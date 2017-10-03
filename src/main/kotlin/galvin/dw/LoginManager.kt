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
    private val currentSystemInfo: SystemInfo?
    private val currentSystemInfoUuid: String
    private val classification: String

    private val loginAttemptsByLogin = LoginAttemptCounter(maxFailedLoginAttemptsPerUser)
    private val loginAttemptsByIpAddress = LoginAttemptCounter(maxFailedLoginAttemptsPerIpAddress)

    private val loginTokens = mutableMapOf<String, LoginToken>()


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

        currentSystemInfoUuid = if(currentSystemInfo==null) "N/A" else currentSystemInfo.uuid
        classification = if(currentSystemInfo==null) "N/A" else currentSystemInfo.maximumClassification
    }


    fun authenticate( credentials: Credentials ): LoginToken{
        checkIpAddressCooldown(credentials.ipAddress)

        val loginType = credentials.getLoginType()
        val user = getUser(credentials)

        if( user == null ){
            val userUuid = neverNull( userDB.retrieveUuidByLogin(credentials.username) )
            val username = getUsername(credentials)
            badLoginAttempt(loginType, credentials.loginProxyUuid, credentials.ipAddress, userUuid, username )

            throw LoginException(LOGIN_EXCEPTION_INVALID_CREDENTIALS)
        }
        else{
            return successfulLoginAttempt(loginType, credentials, user)
        }
    }

    private fun checkIpAddressCooldown( ipAddress: String ){
        if( !isBlank( ipAddress ) ){
            if( loginAttemptsByIpAddress.maxFailedLoginAttemptsExceeded( ipAddress ) ){
                throw LoginException(LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED)
            }
        }
    }

    private fun getUser( credentials: Credentials ): User?{
        if( credentials.x509 != null ){
            val serialNumber = getSerialNumber( credentials.x509)
            return userDB.retrieveUserBySerialNumber(serialNumber)
        }
        else if( !isBlank(credentials.x509SerialNumber ) ){
            return userDB.retrieveUserBySerialNumber(credentials.x509SerialNumber)
        }
        else if( !isBlank(credentials.username) && !isBlank(credentials.password) ){
            return userDB.retrieveUserByLoginAndPassword(credentials.username, credentials.password)
        }
        else if( !isBlank(credentials.tokenUuid) ){
            val loginToken = loginTokens[credentials.tokenUuid]
            if( loginToken != null ){
                if( !loginToken.hasExpired() ) {
                    return userDB.retrieveUser(loginToken.user.uuid)
                }
                else{
                    loginTokens.remove(credentials.tokenUuid)
                }
            }
        }

        return null
    }

    private fun getUsername( credentials: Credentials ): String{
        if( !isBlank(credentials.username) ){
            return credentials.username
        }
        else if( !isBlank(credentials.x509SerialNumber ) ){
            return credentials.x509SerialNumber
        }
        else if( credentials.x509 != null ){
            return getSerialNumber( credentials.x509)
        }

        return ""
    }

    private fun badLoginAttempt( loginType: LoginType,
                                 loginProxyUuid: String,
                                 ipAddress: String,
                                 userUuid: String,
                                 username: String ){
        if( !isBlank( username ) ){
            loginAttemptsByLogin.incrementLoginAttempts( username )

            if( loginAttemptsByLogin.maxFailedLoginAttemptsExceeded( username ) ){
                userDB.setLockedByLogin( username, true )
            }
        }

        if( !isBlank( ipAddress ) ){
            loginAttemptsByIpAddress.incrementLoginAttempts( ipAddress )
        }

        if( auditDB != null ){
            val accessInfo = generateAccessInfo("", loginType, loginProxyUuid, ipAddress, userUuid, username, false )
            auditDB.log(accessInfo)
        }
    }

    private fun successfulLoginAttempt( loginType: LoginType,
                                        credentials: Credentials,
                                        user: User ): LoginToken {
        if( isBlank( credentials.username ) ){
            loginAttemptsByLogin.clearLoginAttempts( credentials.username )
        }

        if( !isBlank( credentials.ipAddress ) ){
            loginAttemptsByIpAddress.clearLoginAttempts(credentials.ipAddress)
        }

        if( auditDB != null ){
            val accessInfo = generateAccessInfo(user.uuid, loginType, credentials.loginProxyUuid, credentials.ipAddress, user.uuid, user.login, true )
            auditDB.log(accessInfo)
        }

        val permissions = userDB.retrievePermissions(user.roles)
        val loginToken = LoginToken(
                user = user.withoutPasswordHash(),
                permissions = permissions,
                loginType = loginType,
                loginProxyUuid = credentials.loginProxyUuid,
                loginProxyName = credentials.loginProxyName
        )
        loginTokens[loginToken.uuid] = loginToken

        return loginToken
    }

    private fun generateAccessInfo( actingUserUuid: String,
                                    loginType: LoginType,
                                    loginProxyUuid: String,
                                    ipAddress: String,
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
                loginProxyUuid = loginProxyUuid,
                ipAddress = ipAddress,
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
                  val tokenUuid: String = "",
                  val loginProxyUuid: String = "",
                  val loginProxyName: String = "") {
    fun getLoginType(): LoginType {
        if (x509 != null || !isBlank(x509SerialNumber)) {
            return LoginType.PKI
        }
        else if( !isBlank(tokenUuid) ){
            return LoginType.LOGIN_TOKEN
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
        return expires <= currentTime
    }

    fun hasExpired(): Boolean {
        return hasExpired(System.currentTimeMillis())
    }
}

class LoginException( message: String = "Login Exception",
                      cause: Throwable? = null   ): Exception(message, cause)