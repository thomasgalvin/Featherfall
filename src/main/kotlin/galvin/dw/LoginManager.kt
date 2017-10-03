package galvin.dw

import java.security.cert.X509Certificate

const val TOKEN_LIFESPAN: Long = 1000 * 60 * 60 * 24 * 5 //five days in miliseconds
const val MAX_FAILED_LOGIN_ATTEMPTS_PER_USER = 5
const val MAX_FAILED_LOGIN_ATTEMPTS_PER_IP_ADDRESS = 15
const val LOGIN_EXCEPTION_NO_CREDENTIALS = "Login Exception: no credentials provided"
const val LOGIN_EXCEPTION_INVALID_CREDENTIALS = "Login Exception: the credentials provided were invalid"
const val LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED = "Login Exception: maximum login attempts exceeded"

class LoginManager(val userDB: UserDB,
                   val auditDB: AuditDB? = null,
                   val allowConcurrentLogins: Boolean = true,
                   val tokenLifespan: Long = TOKEN_LIFESPAN,
                   val maxFailedLoginAttemptsPerUser: Int = MAX_FAILED_LOGIN_ATTEMPTS_PER_USER,
                   val maxFailedLoginAttemptsPerIpAddress: Int = MAX_FAILED_LOGIN_ATTEMPTS_PER_IP_ADDRESS) {
    private val loginAttemptsByLogin = LoginAttemptCounter(maxFailedLoginAttemptsPerUser)
    private val loginAttemptsByIpAddress = LoginAttemptCounter(maxFailedLoginAttemptsPerIpAddress)

    fun authenticate( credentials: Credentials ): LoginToken{
        val ipAddress = credentials.ipAddress
        if( ipAddress != null && !isBlank( ipAddress ) ){
            if( loginAttemptsByIpAddress.maxFailedLoginAttemptsExceeded(ipAddress) ){
                throw LoginException(LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED)
            }
        }

        var user: User?
        var loginType = LoginType.USERNAME_PASSWORD

        if( credentials.x509 != null ){
            val serialNumber = getSerialNumber( credentials.x509)
            user = userDB.retrieveUserBySerialNumber(serialNumber)
            loginType = LoginType.PKI
        }
        else if( credentials.x509SerialNumber != null && !isBlank(credentials.x509SerialNumber ) ){
            user = userDB.retrieveUserBySerialNumber(credentials.x509SerialNumber)
            loginType = LoginType.PKI
        }
        else if( credentials.username != null && !isBlank(credentials.username) &&
                 credentials.password!= null && !isBlank(credentials.password) ){
            user = userDB.retrieveUserByLoginAndPassword(credentials.username, credentials.password)
        }
        else{
            throw LoginException(LOGIN_EXCEPTION_NO_CREDENTIALS)
        }

        if( user == null ){
            badLoginAttempt(credentials)
            throw LoginException(LOGIN_EXCEPTION_INVALID_CREDENTIALS)
        }
        else{
            successfulLoginAttempt(credentials, user)

            val permissions = userDB.retrievePermissions(user.roles)
            return LoginToken(
                    user = user,
                    permissions = permissions,
                    loginType = loginType,
                    loginProxyUuid = credentials.loginProxyUuid,
                    loginProxyName = credentials.loginProxyName
            )
        }
    }

    private fun badLoginAttempt( credentials: Credentials ){
        val username = credentials.username
        if( username != null && !isBlank(username) ){
            loginAttemptsByLogin.incrementLoginAttempts( username)

            if( loginAttemptsByLogin.maxFailedLoginAttemptsExceeded(username) ){
                userDB.setLockedByLogin(username, true)
            }
        }

        val ipAddress = credentials.ipAddress
        if( ipAddress != null && !isBlank( ipAddress ) ){
            loginAttemptsByIpAddress.incrementLoginAttempts( ipAddress )
        }
    }

    private fun successfulLoginAttempt( credentials: Credentials, user: User ){
        val username = credentials.username
        if( username != null && !isBlank(username) ){
            loginAttemptsByLogin.clearLoginAttempts( username )
        }

        val ipAddress = credentials.ipAddress
        if( ipAddress != null && !isBlank( ipAddress ) ){
            loginAttemptsByIpAddress.clearLoginAttempts(ipAddress)
        }
    }
}

class LoginAttemptCounter(val maxFailedLoginAttempts: Int){
    private val loginAttempts = mutableMapOf<String, Int>()

    fun getLoginAttempts( key: String ): Int{
        val attempts = loginAttempts[key]
        if( attempts == null ){
            return 0
        }
        return attempts
    }

    fun incrementLoginAttempts( key: String ){
        val attempts = getLoginAttempts(key)
        loginAttempts.put( key, attempts + 1)
    }

    fun maxFailedLoginAttemptsExceeded(key: String): Boolean{
        return getLoginAttempts(key) > maxFailedLoginAttempts
    }

    fun clearLoginAttempts( key: String ){
        loginAttempts.put( key, 0)
    }
}

data class Credentials(val ipAddress: String? = null,
                       val x509: X509Certificate? = null,
                       val x509SerialNumber: String? = null,
                       val username: String? = null,
                       val password: String? = null,
                       val loginProxyUuid: String? = null,
                       val loginProxyName: String? = null
)

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

class LoginException( message: String = "Login Exception",
                      cause: Throwable? = null   ): Exception(message, cause)