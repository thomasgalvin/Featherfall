package galvin.dw

import java.security.cert.X509Certificate

const val TOKEN_LIFESPAN = 1000 * 60 * 60 * 24 * 5 //five days in miliseconds
const val LOGIN_EXCEPTION_NO_CREDENTIALS = "Login Exception: no credentials provided"

class LoginManager(val userDB: UserDB,
                   val auditDB: AuditDB? = null,
                   val allowConcurrentLogins: Boolean = true) {
    private val tokens = mutableMapOf<String, LoginToken>()
    private val loginAttempts = mutableMapOf<String, Int>()
    private val proxiedLogins = mutableMapOf<String, String>()

    fun authenticate( credentials: Credentials ){
        if( credentials.x509 != null ){
            authenticateX509(credentials.x509)
        }
        else if( !isBlank(credentials.serialNumber) && credentials.serialNumber != null ){
            authenticateSerialNumber(credentials.serialNumber)
        }
        else if( !isBlank(credentials.username) &&
                 !isBlank(credentials.password) &&
                 credentials.username != null &&
                 credentials.password != null  ){
            authenticatePassword(credentials.username, credentials.password)
        }
        else{
            throw LoginException(LOGIN_EXCEPTION_NO_CREDENTIALS)
        }
    }

    private fun authenticateX509(x509: X509Certificate){
        authenticateSerialNumber( getSerialNumber(x509) )
    }

    private fun authenticateSerialNumber(serialNumber: String){
        val user = userDB.retrieveUserBySerialNumber(serialNumber)
        authenticate(user)
    }

    private fun authenticatePassword(username: String, password: String){
    }

    private fun authenticate(user: User?){

    }
}

data class Credentials(val x509: X509Certificate? = null,
                       val serialNumber: String? = null,
                       val username: String? = null,
                       val password: String? = null)

class LoginToken(val uuid: String = uuid(),
                 val timestamp: Long = System.currentTimeMillis(),
                 val expires: Long = timestamp + TOKEN_LIFESPAN,
                 val user: User,
                 val username: String = user.login,
                 val permissions: List<String>,
                 val loginType: LoginType,
                 val loginProxyUuid: String? = null,
                 val loginProxyName: String? = null) {
    fun hasExpired(currentTime: Long): Boolean {
        return expires >= currentTime
    }

    fun hasExpired(): Boolean {
        return hasExpired(System.currentTimeMillis())
    }
}

class LoginException( val _message: String = "Login Exception",
                      val _cause: Throwable? = null   ): Exception(_message, _cause)