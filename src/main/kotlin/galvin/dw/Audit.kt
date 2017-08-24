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

    fun log( access: AccessInfo )
    fun retreiveAccessInfo(startTimestamp: Long, endTimestamp: Long): List<AccessInfo>
    fun retreiveAccessInfo(systemInfoUuid: String, startTimestamp: Long, endTimestamp: Long): List<AccessInfo>
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
                       val loginProxyUuid: String, //used if a trusted system is logging in on behalf of the user

                       val timestamp: Long,
                       val resourceUuid: String,
                       val resourceName: String, // what is being accessed? e.g. "CBP 13Aug16" or "C2WE Login"
                       val classification: String, //level of the data being accessed, e.g. "U//FOUO" or "N/A"
                       val resourceType: String, //e.g. "TPA REPORT", "User Account", "Login" or "Logout". Not an enum, but we should have a definitive list



                       val accessType: AccessType,
                       val permissionGranted: Boolean, // false iff the user attempted an action but was denied by security framework

                       val modifications: List<Modification> = listOf(),

                       val systemInfoUuid: String = UUID.randomUUID().toString(),
                       val uuid: String = UUID.randomUUID().toString()
)

data class Modification( val field: String,
                         val oldValue: String,
                         val newValue: String,
                         val accessInfoUuid: String,
                         val uuid: String = UUID.randomUUID().toString() )

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
// Database utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

data class ConnectionStatement(val connection: Connection, val statement: PreparedStatement)

internal fun isBlank( string: String? ) :Boolean {
    return string == null || string.isBlank();
}

internal fun executeAndClose(conn: Connection, statement: PreparedStatement){
    statement.executeUpdate()
    statement.close()

    conn.commit()
    conn.close()
}

internal fun close(conn: Connection, statement: PreparedStatement){
    statement.close()
    conn.close()
}

internal fun prepareStatement( conn: Connection, sql: String ): ConnectionStatement {
    val statement = conn.prepareStatement(sql)
    return ConnectionStatement(conn, statement)

}

internal fun runSql( conn: Connection, sql: String ){
    var (_, statement) = prepareStatement( conn, sql )
    executeAndClose(conn, statement)
}

internal fun loadSql( classpathEntry: String ): String{
    val resource = SQLiteAuditDB::class.java.getResource(classpathEntry)
    if(resource == null) throw IOException( "Unable to load SQL: $classpathEntry" )

    val sql = resource.readText()
    if( isBlank(sql) ) throw IOException( "Loaded empty SQL: $classpathEntry" )

    return sql
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// SQLite implementation
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class SQLiteAuditDB( private val databaseFile: File) :AuditDB {
    private val concurrencyLock = Object()
    private val connectionUrl: String = "jdbc:sqlite:" + databaseFile.absolutePath
    private val sqlCreateTableSystemInfo = loadSql("/galvin/dw/db/sqlite/audit_create_table_system_info.sql")
    private val sqlCreateTableSystemInfoNetworks = loadSql("/galvin/dw/db/sqlite/audit_create_table_system_info_networks.sql")
    private val sqlStoreSystemInfo = loadSql("/galvin/dw/db/sqlite/audit_store_system_info.sql")
    private val sqlStoreSystemInfoNetwork = loadSql("/galvin/dw/db/sqlite/audit_store_system_info_network.sql")
    private val sqlRetrieveAllSystemInfo = loadSql("/galvin/dw/db/sqlite/audit_retrieve_all_system_info.sql")
    private val sqlRetrieveSystemInfoByUuid = loadSql("/galvin/dw/db/sqlite/audit_retrieve_system_info_by_uuid.sql")
    private val sqlRetrieveSystemInfoNetworks = loadSql("/galvin/dw/db/sqlite/audit_retrieve_system_info_networks.sql")

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

    override fun log(access: AccessInfo) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retreiveAccessInfo(startTimestamp: Long, endTimestamp: Long): List<AccessInfo> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retreiveAccessInfo(systemInfoUuid: String, startTimestamp: Long, endTimestamp: Long): List<AccessInfo> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}