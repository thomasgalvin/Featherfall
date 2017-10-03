package galvin.dw

import java.security.cert.X509Certificate

const val TOKEN_LIFESPAN: Long = 1000 * 60 * 60 * 24 * 5 //five days in miliseconds
const val MAX_FAILED_LOGIN_ATTEMPTS_PER_USER = 5
const val MAX_FAILED_LOGIN_ATTEMPTS_PER_IP_ADDRESS = 15
const val LOGIN_EXCEPTION_NO_CREDENTIALS = "Login Exception: no credentials provided"
const val LOGIN_EXCEPTION_INVALID_CREDENTIALS = "Login Exception: the credentials provided were invalid"
const val LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED = "Login Exception: maximum login attempts exceeded"

class LoginManager(private val userDB: UserDB,
                   private val auditDB: AuditDB? = null,
                   val systemInfoUuid: String = "",
                   val allowConcurrentLogins: Boolean = true,
                   val tokenLifespan: Long = TOKEN_LIFESPAN,
                   val maxFailedLoginAttemptsPerUser: Int = MAX_FAILED_LOGIN_ATTEMPTS_PER_USER,
                   val maxFailedLoginAttemptsPerIpAddress: Int = MAX_FAILED_LOGIN_ATTEMPTS_PER_IP_ADDRESS) {
    private val loginAttemptsByLogin = LoginAttemptCounter(maxFailedLoginAttemptsPerUser)
    private val loginAttemptsByIpAddress = LoginAttemptCounter(maxFailedLoginAttemptsPerIpAddress)
    private val currentSystemInfo: SystemInfo?
    private val currentSystemInfoUuid: String
    private val classification: String


    init{
        if( auditDB == null ){
            currentSystemInfo = null
        }
        else{
            when( isBlank(systemInfoUuid) ){
                true -> currentSystemInfo = auditDB.retrieveCurrentSystemInfo()
                false -> currentSystemInfo = auditDB.retrieveSystemInfo(systemInfoUuid)
            }
        }

        classification = if(currentSystemInfo==null) "N/A" else currentSystemInfo.maximumClassification
        currentSystemInfoUuid = if(currentSystemInfo==null) "N/A" else currentSystemInfo.uuid
    }


    fun authenticate( credentials: Credentials ): LoginToken{
        if( !isBlank( credentials.ipAddress ) ){
            if( loginAttemptsByIpAddress.maxFailedLoginAttemptsExceeded( credentials.ipAddress ) ){
                throw LoginException(LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED)
            }
        }

        val loginType = credentials.getLoginType()
        var userUuid: String = ""
        var user: User?

        if( credentials.x509 != null ){
            val serialNumber = getSerialNumber( credentials.x509)
            user = userDB.retrieveUserBySerialNumber(serialNumber)
        }
        else if( !isBlank(credentials.x509SerialNumber ) ){
            user = userDB.retrieveUserBySerialNumber(credentials.x509SerialNumber)
        }
        else if( !isBlank(credentials.username) && !isBlank(credentials.password) ){
            user = userDB.retrieveUserByLoginAndPassword(credentials.username, credentials.password)
            userUuid = neverNull( userDB.retrieveUuidByLogin(credentials.username) )
        }
        else{
            throw LoginException(LOGIN_EXCEPTION_NO_CREDENTIALS)
        }

        if( user == null ){

            badLoginAttempt(loginType, credentials, userUuid )
            throw LoginException(LOGIN_EXCEPTION_INVALID_CREDENTIALS)
        }
        else{
            successfulLoginAttempt(loginType, credentials, user)

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

    private fun badLoginAttempt( loginType: LoginType,
                                 credentials: Credentials,
                                 userUuid: String){
        if( !isBlank( credentials.username ) ){
            loginAttemptsByLogin.incrementLoginAttempts( credentials.username )

            if( loginAttemptsByLogin.maxFailedLoginAttemptsExceeded( credentials.username ) ){
                userDB.setLockedByLogin( credentials.username, true )
            }
        }

        if( !isBlank( credentials.ipAddress ) ){
            loginAttemptsByIpAddress.incrementLoginAttempts( credentials.ipAddress )
        }

        if( auditDB != null ){
            val accessInfo = generateAccessInfo("", loginType, credentials, userUuid, credentials.username, false )
            auditDB.log(accessInfo)
        }
    }

    private fun successfulLoginAttempt( loginType: LoginType,
                                        credentials: Credentials,
                                        user: User ){
        if( isBlank( credentials.username ) ){
            loginAttemptsByLogin.clearLoginAttempts( credentials.username )
        }

        if( !isBlank( credentials.ipAddress ) ){
            loginAttemptsByIpAddress.clearLoginAttempts(credentials.ipAddress)
        }

        if( auditDB != null ){
            val accessInfo = generateAccessInfo(user.uuid, loginType, credentials, user.uuid, user.login, true )
            auditDB.log(accessInfo)
        }
    }

    private fun generateAccessInfo( actingUserUuid: String,
                                    loginType: LoginType,
                                    credentials: Credentials,
                                    targetUuid: String,
                                    targetLogin: String,
                                    accessGranted: Boolean ): AccessInfo{
        // the "userUuid" in an access info is the ID of the user
        // who performed the action; if the login succeeded
        // we can get this info from the user object itselfm,
        // but if the login failed there is no user UUID available,
        // se we write an empty string instead

        return AccessInfo(
                userUuid = actingUserUuid,
                loginType = loginType,
                loginProxyUuid = credentials.loginProxyUuid,
                ipAddress = credentials.ipAddress,
                timestamp = System.currentTimeMillis(),
                resourceUuid = targetUuid,
                resourceName = targetLogin,
                resourceType = RESOURCE_TYPE_USER_ACCOUNT,
                classification = classification,
                accessType = AccessType.LOGIN,
                permissionGranted = accessGranted,
                systemInfoUuid = currentSystemInfoUuid
        )
    }
}

class LoginAttemptCounter(val maxFailedLoginAttempts: Int){
    private val loginAttempts = mutableMapOf<String, Int>()

    private fun getLoginAttempts( key: String ): Int{
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

class Credentials(val ipAddress: String = "",
                  val x509: X509Certificate? = null,
                  val x509SerialNumber: String = "",
                  val username: String = "",
                  val password: String = "",
                  val loginProxyUuid: String = "",
                  val loginProxyName: String = "") {
    fun getLoginType(): LoginType {
        if (x509 != null || !isBlank(x509SerialNumber)) {
            return LoginType.PKI
        }

        return LoginType.USERNAME_PASSWORD
    }
}

class LoginToken(val uuid: String = uuid(),
                 val timestamp: Long = System.currentTimeMillis(),
                 val expires: Long = timestamp + TOKEN_LIFESPAN,
                 val user: User,
                 val username: String = user.login,
                 val permissions: List<String>,
                 val loginType: LoginType,
                 val loginProxyUuid: String = "",
                 val loginProxyName: String = "") {
    fun hasExpired(currentTime: Long): Boolean {
        return expires >= currentTime
    }

    fun hasExpired(): Boolean {
        return hasExpired(System.currentTimeMillis())
    }
}

class LoginException( message: String = "Login Exception",
                      cause: Throwable? = null   ): Exception(message, cause)