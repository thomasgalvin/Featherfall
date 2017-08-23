package galvin.dw

import java.util.*

interface AuditDB{
    fun log( system: SystemInfo, access: AccessInfo )
}

data class SystemInfo( val seriealNumber: String,
                       val name: String,
                       val version: String,
                       val maximumClassificaton: String,
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

                       val uuid: String = UUID.randomUUID().toString()
)

data class Modification( val field: String,
                         val oldValue: String,
                         val newValue: String,
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
