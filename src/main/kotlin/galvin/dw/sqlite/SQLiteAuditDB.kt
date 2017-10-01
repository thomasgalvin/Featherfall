package galvin.dw.sqlite

import galvin.dw.*
import java.io.File
import java.sql.Connection
import java.sql.ResultSet

val ERROR_CURRENT_SYSTEM_INFO_UUID_NOT_PRESENT = "Audit Error: unable to store current system info: UUID did not match an existing system info"

class SQLiteAuditDB( databaseFile: File) : AuditDB, SQLiteDB(databaseFile) {
    private val concurrencyLock = Object()

    private val sqlCreateTableSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/create_table_system_info.sql")
    private val sqlCreateTableSystemInfoNetworks = loadSql("/galvin/dw/db/sqlite/audit/create_table_system_info_networks.sql")
    private val sqlStoreSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/store_system_info.sql")
    private val sqlStoreSystemInfoNetwork = loadSql("/galvin/dw/db/sqlite/audit/store_system_info_network.sql")
    private val sqlRetrieveAllSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/retrieve_all_system_info.sql")
    private val sqlRetrieveSystemInfoByUuid = loadSql("/galvin/dw/db/sqlite/audit/retrieve_system_info_by_uuid.sql")
    private val sqlRetrieveSystemInfoNetworks = loadSql("/galvin/dw/db/sqlite/audit/retrieve_system_info_networks.sql")

    private val sqlCreateTableAccessInfo = loadSql("/galvin/dw/db/sqlite/audit/create_table_access_info.sql")
    private val sqlCreateTableAccessInfoMods = loadSql("/galvin/dw/db/sqlite/audit/create_table_access_info_mods.sql")

    private val sqlStoreAccessInfo = loadSql("/galvin/dw/db/sqlite/audit/store_access_info.sql")
    private val sqlStoreAccessInfoMod = loadSql("/galvin/dw/db/sqlite/audit/store_access_info_mod.sql")

    private val sqlRetrieveAccessInfoByDates = loadSql("/galvin/dw/db/sqlite/audit/retrieve_access_info_by_dates.sql")
    private val sqlRetrieveAccessInfoByDatesAndUuid = loadSql("/galvin/dw/db/sqlite/audit/retrieve_access_info_by_dates_and_uuid.sql")
    private val sqlRetrieveAccessInfoMods = loadSql("/galvin/dw/db/sqlite/audit/retrieve_access_info_mods.sql")
    
    private val sqlCreateTableCurrentSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/create_table_current_system_info.sql")
    private val sqlDeleteCurrentSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/delete_current_system_info.sql")
    private val sqlSetCurrentSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/store_current_system_info.sql")
    private val sqlRetrieveCurrentSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/retrieve_current_system_info.sql")
    private val sqlCurrentSystemInfoExistsByUuid = loadSql("/galvin/dw/db/sqlite/audit/current_system_info_exists_by_uuid.sql")

    init{
        runSql( conn(), sqlCreateTableSystemInfo )
        runSql( conn(), sqlCreateTableSystemInfoNetworks )

        runSql( conn(), sqlCreateTableAccessInfo )
        runSql( conn(), sqlCreateTableAccessInfoMods )

        runSql( conn(), sqlCreateTableCurrentSystemInfo )
    }

    override fun storeSystemInfo(systemInfo: SystemInfo) {
        synchronized(concurrencyLock) {
            val conn = conn();
            val statement = conn.prepareStatement(sqlStoreSystemInfo)

            statement.setString(1, systemInfo.serialNumber)
            statement.setString(2, systemInfo.name)
            statement.setString(3, systemInfo.version)
            statement.setString(4, systemInfo.maximumClassification)
            statement.setString(5, systemInfo.classificationGuide)
            statement.setString(6, systemInfo.uuid)
            executeAndClose( statement )

            for ((ordinal, network) in systemInfo.networks.withIndex()) {
                storeNetwork(conn, systemInfo.uuid, network, ordinal)
            }

            commitAndClose( conn )
        }
    }

    private fun storeNetwork(conn: Connection, systemInfoUuid: String, networkName: String, ordinal: Int ){
        val statement = conn.prepareStatement(sqlStoreSystemInfoNetwork )

        statement.setString(1, systemInfoUuid)
        statement.setString(2, networkName)
        statement.setInt(3, ordinal)

        executeAndClose( statement )
    }

    override fun retrieveAllSystemInfo(): List<SystemInfo> {
        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveAllSystemInfo)
        val result = mutableListOf<SystemInfo>()

        val resultSet = statement.executeQuery()
        if(resultSet != null){
            while( resultSet.next() ){
                result.add( unmarshalSystemInfo(resultSet, conn) )
            }
        }

        close(conn, statement)
        return result
    }

    override fun retrieveSystemInfo(uuid: String): SystemInfo? {
        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveSystemInfoByUuid)
        var result: SystemInfo? = null

        statement.setString(1, uuid)

        val resultSet = statement.executeQuery()
        if( resultSet != null && resultSet.next() ){
            result = unmarshalSystemInfo(resultSet, conn)
        }

        close(conn, statement)
        return result
    }

    override fun retrieveCurrentSystemInfo(): SystemInfo?{
        var result : SystemInfo? = null
        val conn = conn()

        val statement = conn.prepareStatement(sqlRetrieveCurrentSystemInfo)
        val resultSet = statement.executeQuery()
        if( resultSet != null && resultSet.next() ){
            val uuid = resultSet.getString("uuid")
            result = retrieveSystemInfo(uuid)
        }

        close(conn, statement)
        return result
    }

    override fun storeCurrentSystemInfo(uuid: String) {
        synchronized(concurrencyLock) {
            val conn = conn()

            val existsStatement = conn.prepareStatement(sqlCurrentSystemInfoExistsByUuid)
            existsStatement.setString(1, uuid)
            val existsResult = existsStatement.executeQuery()
            if( !existsResult.next() ){
                throw Exception( ERROR_CURRENT_SYSTEM_INFO_UUID_NOT_PRESENT )
            }

            val deleteStatement = conn.prepareStatement(sqlDeleteCurrentSystemInfo)
            executeAndClose( deleteStatement )

            val storeStatement = conn.prepareStatement(sqlSetCurrentSystemInfo)
            storeStatement.setString(1, uuid)
            executeAndClose( storeStatement, conn )
        }
    }

    private fun unmarshalSystemInfo(hit: ResultSet, conn: Connection): SystemInfo {
        val uuid = hit.getString(6)
        val networks = mutableListOf<String>()

        val result = SystemInfo(
                hit.getString("serialNumber"),
                hit.getString("name"),
                hit.getString("version"),
                hit.getString("maximumClassification"),
                hit.getString("classificationGuide"),
                networks,
                uuid
        )

        val statement = conn.prepareStatement(sqlRetrieveSystemInfoNetworks)
        statement.setString(1, uuid)

        val networkHits = statement.executeQuery()
        if( networkHits != null ){
            while( networkHits.next() ){
                networks.add( networkHits.getString(1) )
            }
        }

        statement.close()
        return result
    }

    override fun log(access: AccessInfo, console: Boolean) {
        synchronized(concurrencyLock) {
            val conn = conn()
            val statement = conn.prepareStatement(sqlStoreAccessInfo)

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

            executeAndClose( statement )

            for ((ordinal, mod) in access.modifications.withIndex()) {
                storeMod(conn, access.uuid, mod, ordinal)
            }

            commitAndClose(conn)
        }

        if(console) println( access )
    }

    private fun storeMod(conn: Connection, accessInfoUuid: String, mod: Modification, ordinal: Int ){
        val statement = conn.prepareStatement(sqlStoreAccessInfoMod)

        statement.setString(1, mod.field)
        statement.setString(2, mod.oldValue)
        statement.setString(3, mod.newValue)
        statement.setString(4, accessInfoUuid)
        statement.setInt(5, ordinal)

        executeAndClose( statement )
    }

    override fun retrieveAccessInfo(startTimestamp: Long, endTimestamp: Long): List<AccessInfo> {
        return doRetrieveAccessInfo(null, startTimestamp, endTimestamp)
    }

    override fun retrieveAccessInfo(systemInfoUuid: String, startTimestamp: Long, endTimestamp: Long): List<AccessInfo> {
        return doRetrieveAccessInfo(systemInfoUuid, startTimestamp, endTimestamp)
    }

    private fun doRetrieveAccessInfo(systemInfoUuid: String?, startTimestamp: Long, endTimestamp: Long): List<AccessInfo> {
        val sql = if( isBlank(systemInfoUuid) ) sqlRetrieveAccessInfoByDates else sqlRetrieveAccessInfoByDatesAndUuid

        val conn = conn()
        val statement = conn.prepareStatement(sql)
        val result = mutableListOf<AccessInfo>()

        statement.setLong(1, startTimestamp)
        statement.setLong(2, endTimestamp)
        if( !isBlank(systemInfoUuid) ){
            statement.setString(3, systemInfoUuid)
        }

        val resultSet = statement.executeQuery()
        if(resultSet != null){
            while( resultSet.next() ){
                result.add( unmarshalAccessInfo(resultSet, conn) )
            }
        }

        close(conn, statement)
        return result
    }

    private fun unmarshalAccessInfo(hit: ResultSet, conn: Connection): AccessInfo {
        val loginType = LoginType.valueOf( hit.getString(2) )
        val accessType = AccessType.valueOf( hit.getString(9) )
        val permissionGranted = hit.getInt(10) != 0
        val uuid = hit.getString(12)

        val mods = mutableListOf<Modification>()

        val statement = conn.prepareStatement(sqlRetrieveAccessInfoMods)
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