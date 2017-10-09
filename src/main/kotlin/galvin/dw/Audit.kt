package galvin.dw

const val ERROR_CURRENT_SYSTEM_INFO_UUID_NOT_PRESENT = "Audit Error: unable to store current system info: UUID did not match an existing system info"
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
    private val dummySystemInfo = galvin.dw.dummySystemInfo()

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