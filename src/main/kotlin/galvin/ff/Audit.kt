package galvin.ff

import galvin.ff.db.QuietCloser
import galvin.ff.db.ConnectionManager
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.util.stream.Collectors

const val ERROR_CURRENT_SYSTEM_INFO_UUID_NOT_PRESENT = "Audit Error: unable to store current system info: UUID did not match an existing system info"
const val ERROR_CURRENT_SYSTEM_INFO_NOT_PRESENT = "Audit Error: no current System Info exists"
const val RESOURCE_TYPE_USER_ACCOUNT = "user_account"

interface AuditDB{
    fun storeSystemInfo(systemInfo: SystemInfo )
    fun retrieveAllSystemInfo(): List<SystemInfo>
    fun retrieveSystemInfo(uuid: String): SystemInfo?

    fun storeCurrentSystemInfo(uuid: String)
    fun retrieveCurrentSystemInfo(): SystemInfo?
    fun retrieveCurrentSystemInfoUuid(): String


    fun log( access: AccessInfo )
    fun retrieveAccessInfo( systemInfoUuid: String? = null,
                            userUuid: String? = null,
                            startTimestamp: Long? = null,
                            endTimestamp: Long? = null,
                            accessType: AccessType? = null,
                            permissionGranted: Boolean? = null ): List<AccessInfo>

    fun toAuditEvent( userDB: UserDB, accessInfo: List<AccessInfo> ): List<AuditEvent>

    companion object {
        fun SQLite(maxConnections: Int, databaseFile: File, console: Boolean = false, timeout: Long = 60_000 ): AuditDB{
            val connectionManager = ConnectionManager.SQLite(maxConnections, databaseFile, timeout)
            val classpath = "/galvin/ff/db/sqlite/"
            return AuditDBImpl( connectionManager, classpath, console)
        }

        fun PostgreSQL( maxConnections: Int, connectionURL: String, console: Boolean = false, timeout: Long = 60_000, username: String? = null, password: String? = null ): AuditDB{
            val connectionManager = ConnectionManager.PostgreSQL(maxConnections, connectionURL, timeout, username, password)
            val classpath = "/galvin/ff/db/psql/"
            return AuditDBImpl( connectionManager, classpath, console)
        }
    }
}

data class SystemInfo( val serialNumber: String,
                       val name: String,
                       val version: String,
                       val maximumClassification: String,
                       val classificationGuide: String,
                       val networks: List<String> = listOf(),
                       val uuid: String = uuid() )

data class AccessInfo( val userUuid: String,
                       val loginType: LoginType,
                       val loginProxyUuid: String?, //used if a trusted system is logging in on behalf of the user
                       val ipAddress: String?, //used if a trusted system is logging in on behalf of the user

                       val timestamp: Long,
                       val resourceUuid: String,
                       val resourceName: String, // what is being accessed? e.g. "CBP 13Aug16" or "C2WE Login"
                       val classification: String, //level of the data being accessed, e.g. "U//FOUO" or "N/A"
                       val resourceType: String, //e.g. "TPA REPORT", "User Account", "Login" or "Logout". Not an enum, but we should have a definitive list

                       val accessType: AccessType,
                       val permissionGranted: Boolean, // false iff the user attempted an action but was denied by security framework

                       val systemInfoUuid: String,

                       val modifications: List<Modification> = listOf(),
                       val uuid: String = uuid()
)

data class Modification( val field: String,
                         val oldValue: String,
                         val newValue: String )

data class AuditEvent(val systemInfo: SystemInfo? = null,
                      val user: User? = null,
                      val commandLineUserName: String = "",
                      val loginType: LoginType,
                      val accessInfo: AccessInfo )

enum class LoginType{
    PKI,
    USERNAME_PASSWORD,
    LOGIN_TOKEN,
    COMMAND_LINE
}

enum class AccessType {
    CREATE,
    MODIFY,
    RETRIEVE,
    DELETE,

    REJECT,
    APPROVE,

    LOCKED,
    UNLOCKED,

    ACTIVATED,
    DEACTIVATED,

    LOGIN,
    LOGOUT,

    ASSERT_PERMISSION
}

///
/// No-Op implementation
///

class NoOpAuditDB: AuditDB {
    private val dummySystemInfo = galvin.ff.dummySystemInfo()

    override fun storeSystemInfo(systemInfo: SystemInfo ){}

    override fun retrieveAllSystemInfo(): List<SystemInfo>{
        return listOf( dummySystemInfo )
    }

    override fun retrieveSystemInfo(uuid: String): SystemInfo?{
        if( uuid == dummySystemInfo.uuid ){
            return dummySystemInfo
        }

        return null
    }

    override fun storeCurrentSystemInfo(uuid: String){}

    override fun retrieveCurrentSystemInfo(): SystemInfo?{
        return dummySystemInfo
    }

    override fun retrieveCurrentSystemInfoUuid(): String{
        return dummySystemInfo.uuid
    }


    override fun log( access: AccessInfo ){}

    override fun retrieveAccessInfo(systemInfoUuid: String?,
                                    userUuid: String?,
                                    startTimestamp: Long?,
                                    endTimestamp: Long?,
                                    accessType: AccessType?,
                                    permissionGranted: Boolean?): List<AccessInfo> {
        return listOf()
    }

    override fun toAuditEvent( userDB: UserDB, accessInfo: List<AccessInfo> ): List<AuditEvent>{
        return listOf()
    }
}

fun dummySystemInfo(): SystemInfo{
    return SystemInfo(
            serialNumber = "????",
            name = "????",
            version = "????",
            maximumClassification = "U",
            classificationGuide = "N/A",
            uuid = "????"
    )
}

///
/// Working implementation
///

class AuditDBImpl( private val connectionManager: ConnectionManager,
                   sqlClasspath: String,
                   private val console: Boolean = false ): AuditDB {
    private val concurrencyLock = Object()

    private val sqlCreateTableSystemInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/create_table_system_info.sql")
    private val sqlCreateTableSystemInfoNetworks = loadFromClasspathOrThrow("$sqlClasspath/audit/create_table_system_info_networks.sql")
    private val sqlStoreSystemInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/store_system_info.sql")
    private val sqlStoreSystemInfoNetwork = loadFromClasspathOrThrow("$sqlClasspath/audit/store_system_info_network.sql")
    private val sqlRetrieveAllSystemInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/retrieve_all_system_info.sql")
    private val sqlRetrieveSystemInfoByUuid = loadFromClasspathOrThrow("$sqlClasspath/audit/retrieve_system_info_by_uuid.sql")
    private val sqlRetrieveSystemInfoNetworks = loadFromClasspathOrThrow("$sqlClasspath/audit/retrieve_system_info_networks.sql")

    private val sqlCreateTableAccessInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/create_table_access_info.sql")
    private val sqlCreateTableAccessInfoMods = loadFromClasspathOrThrow("$sqlClasspath/audit/create_table_access_info_mods.sql")

    private val sqlStoreAccessInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/store_access_info.sql")
    private val sqlStoreAccessInfoMod = loadFromClasspathOrThrow("$sqlClasspath/audit/store_access_info_mod.sql")

    private val sqlRetrieveAccessInfoMods = loadFromClasspathOrThrow("$sqlClasspath/audit/retrieve_access_info_mods.sql")

    private val sqlCreateTableCurrentSystemInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/create_table_current_system_info.sql")
    private val sqlDeleteCurrentSystemInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/delete_current_system_info.sql")
    private val sqlSetCurrentSystemInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/store_current_system_info.sql")
    private val sqlRetrieveCurrentSystemInfo = loadFromClasspathOrThrow("$sqlClasspath/audit/retrieve_current_system_info.sql")
    private val sqlCurrentSystemInfoExistsByUuid = loadFromClasspathOrThrow("$sqlClasspath/audit/current_system_info_exists_by_uuid.sql")

    fun conn(): Connection = connectionManager.connect()

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
            rollbackCloseAndRelease(conn, connectionManager)
        }
    }

    override fun storeSystemInfo(systemInfo: SystemInfo) {
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlStoreSystemInfo)

                try {
                    statement.setString(1, systemInfo.serialNumber)
                    statement.setString(2, systemInfo.name)
                    statement.setString(3, systemInfo.version)
                    statement.setString(4, systemInfo.maximumClassification)
                    statement.setString(5, systemInfo.classificationGuide)
                    statement.setString(6, systemInfo.uuid)

                    for((ordinal, network) in systemInfo.networks.withIndex()) {
                        storeNetwork(conn, systemInfo.uuid, network, ordinal)
                    }

                    executeUpdateAndClose(statement, conn)
                } finally{ QuietCloser.close(statement) }
            }
            finally{
                rollbackCloseAndRelease(conn, connectionManager)
            }
        }
    }

    private fun storeNetwork(conn: Connection, systemInfoUuid: String, networkName: String, ordinal: Int ){
        val statement = conn.prepareStatement(sqlStoreSystemInfoNetwork )

        statement.setString(1, systemInfoUuid)
        statement.setString(2, networkName)
        statement.setInt(3, ordinal)

        executeUpdateAndClose(statement )
    }

    override fun retrieveAllSystemInfo(): List<SystemInfo> {
        val conn = conn()

        try {
            val statement = conn.prepareStatement(sqlRetrieveAllSystemInfo)
            try {
                val result = mutableListOf<SystemInfo>()

                val resultSet = statement.executeQuery()
                if(resultSet != null) {
                    while(resultSet.next()) {
                        result.add(unmarshalSystemInfo(resultSet, conn))
                    }
                }
                return result
            } finally{ QuietCloser.close(statement) }
        }
        finally{
            rollbackCloseAndRelease(conn, connectionManager)
        }
    }

    override fun retrieveSystemInfo(uuid: String): SystemInfo? {
        val conn = conn()

        try {
            val statement = conn.prepareStatement(sqlRetrieveSystemInfoByUuid)
            try {
                var result: SystemInfo? = null

                statement.setString(1, uuid)

                val resultSet = statement.executeQuery()
                if(resultSet != null && resultSet.next()) {
                    result = unmarshalSystemInfo(resultSet, conn)
                }
                return result
            } finally{ QuietCloser.close(statement) }
        }
        finally{
            rollbackCloseAndRelease(conn, connectionManager)
        }
    }

    override fun storeCurrentSystemInfo(uuid: String) {
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val existsStatement = conn.prepareStatement(sqlCurrentSystemInfoExistsByUuid)
                existsStatement.setString(1, uuid)
                val existsResult = existsStatement.executeQuery()
                if ( !existsResult.next() ) {
                    throw Exception(ERROR_CURRENT_SYSTEM_INFO_UUID_NOT_PRESENT)
                }

                val deleteStatement = conn.prepareStatement(sqlDeleteCurrentSystemInfo)
                val storeStatement = conn.prepareStatement(sqlSetCurrentSystemInfo)

                try {
                    executeUpdateAndClose(deleteStatement)

                    storeStatement.setString(1, uuid)
                    executeUpdateAndClose(storeStatement)

                    commitAndClose(conn)
                } finally{ QuietCloser.close(deleteStatement, storeStatement) }
            }
            finally{
                rollbackCloseAndRelease(conn, connectionManager)
            }
        }
    }

    override fun retrieveCurrentSystemInfo(): SystemInfo?{
        val uuid = retrieveCurrentSystemInfoUuid()
        return retrieveSystemInfo(uuid)
    }


    override fun retrieveCurrentSystemInfoUuid(): String{
        val conn = conn()

        try {
            val statement = conn.prepareStatement(sqlRetrieveCurrentSystemInfo)
            try {
                val resultSet = statement.executeQuery()
                try {
                    if(resultSet != null && resultSet.next()) {
                        return resultSet.getString("uuid")
                    }
                } finally{ QuietCloser.close(resultSet) }
            } finally{ QuietCloser.close(statement) }
        }
        finally{
            rollbackCloseAndRelease(conn, connectionManager)
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
        try{
            statement.setString(1, uuid)

            val networkHits = statement.executeQuery()
            try {
                if(networkHits != null) {
                    while(networkHits.next()) {
                        networks.add(networkHits.getString(1))
                    }
                }
            } finally{ QuietCloser.close(networkHits) }
        } finally{ QuietCloser.close(statement) }

        return result
    }

    override fun log(access: AccessInfo) {
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlStoreAccessInfo)
                try {
                    val accessGranted = if(access.permissionGranted) 1 else 0

                    statement.setString(1, access.userUuid)
                    statement.setString(2, access.loginType.name)
                    statement.setString(3, access.loginProxyUuid)
                    statement.setString(4, access.ipAddress)
                    statement.setLong(5, access.timestamp)
                    statement.setString(6, access.resourceUuid)
                    statement.setString(7, access.resourceName)
                    statement.setString(8, access.classification)
                    statement.setString(9, access.resourceType)
                    statement.setInt(10, access.accessType.ordinal)
                    statement.setInt(11, accessGranted)
                    statement.setString(12, access.systemInfoUuid)
                    statement.setString(13, access.uuid)

                    executeUpdateAndClose(statement)

                    for((ordinal, mod) in access.modifications.withIndex()) {
                        storeMod(conn, access.uuid, mod, ordinal)
                    }

                    commitAndClose(conn)
                } finally { QuietCloser.close(statement) }
            }
            finally{
                rollbackCloseAndRelease(conn, connectionManager)
            }
        }

        if(console) println( access )
    }

    private fun storeMod(conn: Connection, accessInfoUuid: String, mod: Modification, ordinal: Int ){
        val statement = conn.prepareStatement(sqlStoreAccessInfoMod)
        try {

            statement.setString(1, mod.field)
            statement.setString(2, mod.oldValue)
            statement.setString(3, mod.newValue)
            statement.setString(4, accessInfoUuid)
            statement.setInt(5, ordinal)

            executeUpdateAndClose(statement)
        } finally { QuietCloser.close(statement) }
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
            try{

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
                try {
                    while(resultSet.next()) {
                        result.add(unmarshalAccessInfo(resultSet, conn))
                    }
                } finally { QuietCloser.close(resultSet) }

                return result
            } finally { QuietCloser.close(statement) }

        }
        finally{
            rollbackCloseAndRelease(conn, connectionManager)
        }

    }

    private fun unmarshalAccessInfo(hit: ResultSet, conn: Connection): AccessInfo {
        val accessTypeOrdinal = hit.getInt("accessType")

        val loginType = LoginType.valueOf( hit.getString("loginType") )
        val accessType = AccessType.values()[accessTypeOrdinal]
        val permissionGranted = hit.getInt("permissionGranted") != 0
        val uuid = hit.getString("uuid")

        val mods = mutableListOf<Modification>()

        val statement = conn.prepareStatement(sqlRetrieveAccessInfoMods)
        try {
            statement.setString(1, uuid)

            val modHits = statement.executeQuery()
            try {
                if(modHits != null) {
                    while(modHits.next()) {
                        mods.add(Modification(
                                modHits.getString("field"),
                                modHits.getString("oldValue"),
                                modHits.getString("newValue")
                        ))
                    }
                }
            } finally{ QuietCloser.close(modHits) }

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
        } finally{ QuietCloser.close(statement) }
    }

    override fun toAuditEvent( userDB: UserDB, accessInfo: List<AccessInfo> ): List<AuditEvent>{
        val result = mutableListOf<AuditEvent>()

        for( info in accessInfo ){
            val systemInfo = retrieveSystemInfo(info.systemInfoUuid)
            val user = userDB.retrieveUser(info.userUuid)
            val commandLineUserName = if( info.loginType == LoginType.COMMAND_LINE) info.userUuid else ""

            result.add( AuditEvent(
                    systemInfo = systemInfo,
                    user = user,
                    commandLineUserName = commandLineUserName,
                    loginType = info.loginType,
                    accessInfo = info
            ) )
        }

        return result
    }
}