package galvin.dw.sqlite

import galvin.dw.*
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.util.stream.Collectors

class SQLiteAuditDB( databaseFile: File,
                     private val console: Boolean = false) : AuditDB, SQLiteDB(databaseFile) {
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

    private val sqlRetrieveAccessInfoMods = loadSql("/galvin/dw/db/sqlite/audit/retrieve_access_info_mods.sql")
    
    private val sqlCreateTableCurrentSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/create_table_current_system_info.sql")
    private val sqlDeleteCurrentSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/delete_current_system_info.sql")
    private val sqlSetCurrentSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/store_current_system_info.sql")
    private val sqlRetrieveCurrentSystemInfo = loadSql("/galvin/dw/db/sqlite/audit/retrieve_current_system_info.sql")
    private val sqlCurrentSystemInfoExistsByUuid = loadSql("/galvin/dw/db/sqlite/audit/current_system_info_exists_by_uuid.sql")

    init{
        val conn = conn()
        try {
            executeUpdate(conn, sqlCreateTableSystemInfo)
            executeUpdate(conn, sqlCreateTableSystemInfoNetworks)
            executeUpdate(conn, sqlCreateTableAccessInfo)
            executeUpdate(conn, sqlCreateTableAccessInfoMods)
            executeUpdate(conn, sqlCreateTableCurrentSystemInfo)
            commitAndClose(conn)
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun storeSystemInfo(systemInfo: SystemInfo) {
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlStoreSystemInfo)

                statement.setString(1, systemInfo.serialNumber)
                statement.setString(2, systemInfo.name)
                statement.setString(3, systemInfo.version)
                statement.setString(4, systemInfo.maximumClassification)
                statement.setString(5, systemInfo.classificationGuide)
                statement.setString(6, systemInfo.uuid)

                for ((ordinal, network) in systemInfo.networks.withIndex()) {
                    storeNetwork(conn, systemInfo.uuid, network, ordinal)
                }

                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
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

        try {
            val statement = conn.prepareStatement(sqlRetrieveAllSystemInfo)
            val result = mutableListOf<SystemInfo>()

            val resultSet = statement.executeQuery()
            if (resultSet != null) {
                while (resultSet.next()) {
                    result.add(unmarshalSystemInfo(resultSet, conn))
                }
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun retrieveSystemInfo(uuid: String): SystemInfo? {
        val conn = conn()

        try {
            val statement = conn.prepareStatement(sqlRetrieveSystemInfoByUuid)
            var result: SystemInfo? = null

            statement.setString(1, uuid)

            val resultSet = statement.executeQuery()
            if (resultSet != null && resultSet.next()) {
                result = unmarshalSystemInfo(resultSet, conn)
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun storeCurrentSystemInfo(uuid: String) {
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val existsStatement = conn.prepareStatement(sqlCurrentSystemInfoExistsByUuid)
                existsStatement.setString(1, uuid)
                val existsResult = existsStatement.executeQuery()
                if (!existsResult.next()) {
                    conn.close()
                    throw Exception(ERROR_CURRENT_SYSTEM_INFO_UUID_NOT_PRESENT)
                }

                val deleteStatement = conn.prepareStatement(sqlDeleteCurrentSystemInfo)
                executeAndClose(deleteStatement)

                val storeStatement = conn.prepareStatement(sqlSetCurrentSystemInfo)
                storeStatement.setString(1, uuid)
                executeAndClose(storeStatement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun retrieveCurrentSystemInfo(): SystemInfo?{
        val conn = conn()

        try {
            var result: SystemInfo? = null

            val statement = conn.prepareStatement(sqlRetrieveCurrentSystemInfo)
            val resultSet = statement.executeQuery()
            if (resultSet != null && resultSet.next()) {
                val uuid = resultSet.getString("uuid")
                result = retrieveSystemInfo(uuid)
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun retrieveCurrentSystemInfoUuid(): String{
        val csi = retrieveCurrentSystemInfo()
        if( csi != null ){
            return csi.uuid
        }
        return ""
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

    override fun log(access: AccessInfo) {
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlStoreAccessInfo)
                val accessGranted = if (access.permissionGranted) 1 else 0

                statement.setString(1, access.userUuid)
                statement.setString(2, access.loginType.name)
                statement.setString(3, access.loginProxyUuid)
                statement.setString(4, access.ipAddress)
                statement.setLong(5, access.timestamp)
                statement.setString(6, access.resourceUuid)
                statement.setString(7, access.resourceName)
                statement.setString(8, access.classification)
                statement.setString(9, access.resourceType)
                statement.setString(10, access.accessType.name)
                statement.setInt(11, accessGranted)
                statement.setString(12, access.systemInfoUuid)
                statement.setString(13, access.uuid)

                executeAndClose(statement)

                for ((ordinal, mod) in access.modifications.withIndex()) {
                    storeMod(conn, access.uuid, mod, ordinal)
                }

                commitAndClose(conn)
            }
            finally{
                rollbackAndClose(conn)
            }
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

    override fun retrieveAccessInfo( systemInfoUuid: String?,
                                     userUuid: String?,
                                     startTimestamp: Long?,
                                     endTimestamp: Long?,
                                     accessType: AccessType?,
                                     permissionGranted: Boolean? ): List<AccessInfo> {
        val sql = StringBuilder( "select * from AccessInfo" )
        val criteria = mutableListOf<String>()

        if( systemInfoUuid != null ){
            criteria.add("systemInfoUuid = ?")
        }

        if( userUuid != null ){
            criteria.add("userUuid = ?")
        }

        if( startTimestamp != null ){
            criteria.add("timestamp >= ?")
        }

        if( endTimestamp != null ){
            criteria.add("timestamp <= ?")
        }

        if( accessType != null ){
            criteria.add("accessType = ?")
        }

        if( permissionGranted != null ){
            criteria.add("permissionGranted = ?")
        }

        if( !criteria.isEmpty() ){
            sql.append(" where " )
        }

        sql.append( criteria.stream().collect( Collectors.joining( " and " ) ) )
        sql.append( " order by timestamp" )

        val conn = conn()
        try {
            val statement = conn.prepareStatement(sql.toString())

            var index = 1

            if (systemInfoUuid != null) {
                statement.setString(index, systemInfoUuid)
                index++
            }

            if( userUuid != null ){
                statement.setString(index, userUuid)
                index++
            }

            if (startTimestamp != null) {
                statement.setLong(index, startTimestamp)
                index++
            }

            if (endTimestamp != null) {
                statement.setLong(index, endTimestamp)
                index++
            }

            if (accessType != null) {
                statement.setInt(index, accessType.ordinal)
                index++
            }

            if (permissionGranted != null) {
                statement.setInt(index, if (permissionGranted) 1 else 0)
            }

            val result = mutableListOf<AccessInfo>()
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                result.add(unmarshalAccessInfo(resultSet, conn))
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }

    }

    private fun unmarshalAccessInfo(hit: ResultSet, conn: Connection): AccessInfo {
        val loginType = LoginType.valueOf( hit.getString("loginType") )
        val accessType = AccessType.valueOf( hit.getString("accessType") )
        val permissionGranted = hit.getInt("permissionGranted") != 0
        val uuid = hit.getString("uuid")

        val mods = mutableListOf<Modification>()

        val statement = conn.prepareStatement(sqlRetrieveAccessInfoMods)
        statement.setString(1, uuid)

        val modHits = statement.executeQuery()
        if( modHits != null){
            while( modHits.next() ){
                mods.add( Modification(
                        modHits.getString("field"),
                        modHits.getString("oldValue"),
                        modHits.getString("newValue")
                ))
            }
        }

        return AccessInfo(
                hit.getString("userUuid"),
                loginType,
                hit.getString("loginProxyUuid"),
                hit.getString("ipAddress"),
                hit.getLong("timestamp"),
                hit.getString("resourceUuid"),
                hit.getString("resourceName"),
                hit.getString("classification"),
                hit.getString("resourceType"),
                accessType,
                permissionGranted,
                hit.getString("systemInfoUuid"),
                mods,
                uuid
        )
    }
}