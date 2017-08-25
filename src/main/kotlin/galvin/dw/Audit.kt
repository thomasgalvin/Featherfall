package galvin.dw

import java.io.File
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

interface AuditDB{
    fun store( systemInfo: SystemInfo )
    fun retrieveAllSystemInfo(): List<SystemInfo>
    fun retrieveSystemInfo(uuid: String): SystemInfo?

    fun log( access: AccessInfo, console: Boolean = false )
    fun retrieveAccessInfo(startTimestamp: Long, endTimestamp: Long): List<AccessInfo>
    fun retrieveAccessInfo(systemInfoUuid: String, startTimestamp: Long, endTimestamp: Long): List<AccessInfo>
}

data class SystemInfo( val serialNumber: String,
                       val name: String,
                       val version: String,
                       val maximumClassification: String,
                       val classificationGuide: String,
                       val networks: List<String>,
                       val uuid: String = UUID.randomUUID().toString() )

data class AccessInfo( val userUuid: String,
                       val loginType: LoginType,
                       val loginProxyUuid: String?, //used if a trusted system is logging in on behalf of the user

                       val timestamp: Long,
                       val resourceUuid: String,
                       val resourceName: String, // what is being accessed? e.g. "CBP 13Aug16" or "C2WE Login"
                       val classification: String, //level of the data being accessed, e.g. "U//FOUO" or "N/A"
                       val resourceType: String, //e.g. "TPA REPORT", "User Account", "Login" or "Logout". Not an enum, but we should have a definitive list

                       val accessType: AccessType,
                       val permissionGranted: Boolean, // false iff the user attempted an action but was denied by security framework

                       val systemInfoUuid: String,

                       val modifications: List<Modification> = listOf(),
                       val uuid: String = UUID.randomUUID().toString()
)

data class Modification( val field: String,
                         val oldValue: String,
                         val newValue: String )

enum class LoginType{
    PKI,
    USERNAME_PASSWORD,
    LOGIN_TOKEN
}

enum class AccessType {
    CREATE,
    MODIFY,
    RETRIEVE,
    DELETE,

    REJECT,

    LOGIN,
    LOGOUT,

    ASSERT_PERMISSION
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// SQLite implementation
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class SQLiteAuditDB( private val databaseFile: File) :AuditDB {
    private val concurrencyLock = Object()

    private val connectionUrl: String = "jdbc:sqlite:" + databaseFile.absolutePath

    private val sqlCreateTableSystemInfo = loadSql("/galvin/dw/db/sqlite/audit//create_table_system_info.sql")
    private val sqlCreateTableSystemInfoNetworks = loadSql("/galvin/dw/db/sqlite/audit//create_table_system_info_networks.sql")
    private val sqlStoreSystemInfo = loadSql("/galvin/dw/db/sqlite/audit//store_system_info.sql")
    private val sqlStoreSystemInfoNetwork = loadSql("/galvin/dw/db/sqlite/audit//store_system_info_network.sql")
    private val sqlRetrieveAllSystemInfo = loadSql("/galvin/dw/db/sqlite/audit//retrieve_all_system_info.sql")
    private val sqlRetrieveSystemInfoByUuid = loadSql("/galvin/dw/db/sqlite/audit//retrieve_system_info_by_uuid.sql")
    private val sqlRetrieveSystemInfoNetworks = loadSql("/galvin/dw/db/sqlite/audit//retrieve_system_info_networks.sql")

    private val sqlCreateTableAccessInfo = loadSql("/galvin/dw/db/sqlite/audit//create_table_access_info.sql")
    private val sqlCreateTableAccessInfoMods = loadSql("/galvin/dw/db/sqlite/audit//create_table_access_info_mods.sql")

    private val sqlStoreAccessInfo = loadSql("/galvin/dw/db/sqlite/audit//store_access_info.sql")
    private val sqlStoreAccessInfoMod = loadSql("/galvin/dw/db/sqlite/audit//store_access_info_mod.sql")

    private val sqlRetrieveAccessInfoByDates = loadSql("/galvin/dw/db/sqlite/audit//retrieve_access_info_by_dates.sql")
    private val sqlRetrieveAccessInfoByDatesAndUuid = loadSql("/galvin/dw/db/sqlite/audit//retrieve_access_info_by_dates_and_uuid.sql")
    private val sqlRetrieveAccessInfoMods = loadSql("/galvin/dw/db/sqlite/audit//retrieve_access_info_mods.sql")

    init{
        createTables()
    }

    private fun getConnection( connectionUrl: String ): Connection {
        Class.forName( "org.sqlite.JDBC" )
        val result = DriverManager.getConnection( connectionUrl )
        result.autoCommit = false
        return result
    }

    private fun getConnection(): Connection{
        return getConnection(connectionUrl)
    }

    private fun createTables(){
        runSql( getConnection(), sqlCreateTableSystemInfo )
        runSql( getConnection(), sqlCreateTableSystemInfoNetworks )

        runSql( getConnection(), sqlCreateTableAccessInfo )
        runSql( getConnection(), sqlCreateTableAccessInfoMods )
    }

    override fun store(systemInfo: SystemInfo) {
        synchronized(concurrencyLock) {
            val (conn, statement) = prepareStatement(getConnection(), sqlStoreSystemInfo)

            statement.setString(1, systemInfo.serialNumber)
            statement.setString(2, systemInfo.name)
            statement.setString(3, systemInfo.version)
            statement.setString(4, systemInfo.maximumClassification)
            statement.setString(5, systemInfo.classificationGuide)
            statement.setString(6, systemInfo.uuid)

            statement.executeUpdate()
            statement.close()

            for ((ordinal, network) in systemInfo.networks.withIndex()) {
                storeNetwork(conn, systemInfo.uuid, network, ordinal)
            }

            conn.commit()
            conn.close()
        }
    }

    private fun storeNetwork( conn: Connection, systemInfoUuid: String, networkName: String, ordinal: Int ){
        val (_, statement) = prepareStatement( conn, sqlStoreSystemInfoNetwork )

        statement.setString(1, systemInfoUuid)
        statement.setString(2, networkName)
        statement.setInt(3, ordinal)

        statement.executeUpdate()
        statement.close()
    }

    override fun retrieveAllSystemInfo(): List<SystemInfo> {
        val (conn, statement) = prepareStatement( getConnection(), sqlRetrieveAllSystemInfo )
        val result = mutableListOf<SystemInfo>()

        val resultSet = statement.executeQuery()
        if(resultSet != null){
            while( resultSet.next() ){
                result.add( unmarshallSystemInfo(resultSet, conn) )
            }
        }

        close(conn, statement)
        return result
    }

    override fun retrieveSystemInfo(uuid: String): SystemInfo? {
        val (conn, statement) = prepareStatement( getConnection(), sqlRetrieveSystemInfoByUuid )
        var result: SystemInfo? = null

        statement.setString(1, uuid)

        val resultSet = statement.executeQuery()
        if( resultSet != null && resultSet.next() ){
            result = unmarshallSystemInfo(resultSet, conn)
        }

        close(conn, statement)
        return result
    }

    private fun unmarshallSystemInfo( hit: ResultSet, conn: Connection ): SystemInfo{
        val uuid = hit.getString(6)
        val networks = mutableListOf<String>()

        val result = SystemInfo(
                hit.getString(1),
                hit.getString(2),
                hit.getString(3),
                hit.getString(4),
                hit.getString(5),
                networks,
                uuid
        )

        val (_, statement) = prepareStatement( conn, sqlRetrieveSystemInfoNetworks )
        statement.setString(1, uuid)

        val networkHits = statement.executeQuery()
        if( networkHits != null ){
            while( networkHits.next() ){
                networks.add( networkHits.getString(1) )
            }
        }

        return result
    }

    override fun log(access: AccessInfo, console: Boolean) {
        synchronized(concurrencyLock) {
            val (conn, statement) = prepareStatement(getConnection(), sqlStoreAccessInfo)

            val accessGranted = if(access.permissionGranted) 1 else 0

            statement.setString(1, access.userUuid)
            statement.setString(2, access.loginType.name)
            statement.setString(3, access.loginProxyUuid)
            statement.setLong(4, access.timestamp)
            statement.setString(5, access.resourceUuid)
            statement.setString(6, access.resourceName)
            statement.setString(7, access.classification)
            statement.setString(8, access.resourceType)
            statement.setString(9, access.accessType.name)
            statement.setInt(10, accessGranted )
            statement.setString(11, access.systemInfoUuid)
            statement.setString(12, access.uuid)

            statement.executeUpdate()
            statement.close()

            for ((ordinal, mod) in access.modifications.withIndex()) {
                storeMod(conn, access.uuid, mod, ordinal)
            }

            conn.commit()
            conn.close()
        }

        if(console) println( access )
    }

    private fun storeMod( conn: Connection, accessInfoUuid: String, mod: Modification, ordinal: Int ){
        val (_, statement) = prepareStatement( conn, sqlStoreAccessInfoMod )

        statement.setString(1, mod.field)
        statement.setString(2, mod.oldValue)
        statement.setString(3, mod.newValue)
        statement.setString(4, accessInfoUuid)
        statement.setInt(5, ordinal)

        statement.executeUpdate()
        statement.close()
    }

    override fun retrieveAccessInfo(startTimestamp: Long, endTimestamp: Long): List<AccessInfo> {
        return doRetrieveAccessInfo(null, startTimestamp, endTimestamp)
    }

    override fun retrieveAccessInfo(systemInfoUuid: String, startTimestamp: Long, endTimestamp: Long): List<AccessInfo> {
        return doRetrieveAccessInfo(systemInfoUuid, startTimestamp, endTimestamp)
    }

    private fun doRetrieveAccessInfo(systemInfoUuid: String?, startTimestamp: Long, endTimestamp: Long): List<AccessInfo> {
        val sql = if( isBlank(systemInfoUuid) ) sqlRetrieveAccessInfoByDates else sqlRetrieveAccessInfoByDatesAndUuid
        val (conn, statement) = prepareStatement( getConnection(), sql )
        val result = mutableListOf<AccessInfo>()

        statement.setLong(1, startTimestamp)
        statement.setLong(2, endTimestamp)
        if( !isBlank(systemInfoUuid) ){
            statement.setString(3, systemInfoUuid)
        }

        val resultSet = statement.executeQuery()
        if(resultSet != null){
            while( resultSet.next() ){
                result.add( unmarshallAccessInfo(resultSet, conn) )
            }
        }

        close(conn, statement)
        return result
    }

    private fun unmarshallAccessInfo(hit: ResultSet, conn: Connection): AccessInfo{
        val loginType = LoginType.valueOf( hit.getString(2) )
        val accessType = AccessType.valueOf( hit.getString(9) )
        val permissionGranted = hit.getInt(10) != 0
        val uuid = hit.getString(12)

        val mods = mutableListOf<Modification>()

        val (_, statement) = prepareStatement( conn, sqlRetrieveAccessInfoMods )
        statement.setString(1, uuid)

        val modHits = statement.executeQuery()
        if( modHits != null){
            while( modHits.next() ){
                mods.add( Modification(
                        modHits.getString(1),
                        modHits.getString(2),
                        modHits.getString(3)
                ))
            }
        }

        return AccessInfo(
                hit.getString(1),
                loginType,
                hit.getString(3),
                hit.getLong(4),
                hit.getString(5),
                hit.getString(6),
                hit.getString(7),
                hit.getString(8),
                accessType,
                permissionGranted,
                hit.getString(11),
                mods,
                uuid
        )
    }
}