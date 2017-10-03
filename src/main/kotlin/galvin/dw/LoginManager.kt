package galvin.dw

import java.security.cert.X509Certificate

const val TOKEN_LIFESPAN: Long = 1000 * 60 * 60 * 24 * 5 //five days in miliseconds

const val MAX_UNHINDERED_LOGIN_ATTEMPTS = 0
const val MAX_FAILED_LOGIN_ATTEMPTS = 15
const val ATTEMPTS_EXPIRE_AFTER: Long = 1000 * 60 * 60 //one hour in ms

const val MAX_FAILED_LOGIN_ATTEMPTS_PER_USER = 5
const val MAX_FAILED_LOGIN_ATTEMPTS_PER_IP_ADDRESS = 15

const val LOGIN_EXCEPTION_INVALID_CREDENTIALS = "Login Exception: the provided credentials were invalid"
const val LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED = "Login Exception: maximum login attempts exceeded"

class LoginManager(private val userDB: UserDB,
                   private val auditDB: AuditDB? = null,
                   private val maxFailedLoginAttemptsPerUser: Int = MAX_FAILED_LOGIN_ATTEMPTS_PER_USER,
                   private val maxFailedLoginAttemptsPerIpAddress: Int = MAX_FAILED_LOGIN_ATTEMPTS_PER_IP_ADDRESS,
                   val allowConcurrentLogins: Boolean = true,
                   val tokenLifespan: Long = TOKEN_LIFESPAN,
                   systemInfoUuid: String = "" ) {
    private val currentSystemInfo: SystemInfo?
    private val currentSystemInfoUuid: String
    private val classification: String

    private val loginAttemptsByLogin = LoginCooldown(maxFailedLoginAttempts = maxFailedLoginAttemptsPerUser)
    private val loginAttemptsByIpAddress = LoginCooldown(maxFailedLoginAttempts = maxFailedLoginAttemptsPerIpAddress)

    private val loginTokens = LoginTokenManager()

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
            val username = getUsername(credentials)
            val userUuid = neverNull( userDB.retrieveUuidByLogin(username) )

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
                return userDB.retrieveUser(loginToken.user.uuid)
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
        if( !isBlank( credentials.ipAddress ) ){
            loginAttemptsByIpAddress.clearLoginAttempts(credentials.ipAddress)
        }

        loginAttemptsByLogin.clearLoginAttempts( user.login )

        if( auditDB != null ){
            val accessInfo = generateAccessInfo(user.uuid, loginType, credentials.loginProxyUuid, credentials.ipAddress, user.uuid, user.login, true )
            auditDB.log(accessInfo)
        }

        val permissions = userDB.retrievePermissions(user.roles)
        val loginToken = LoginToken(
                tokenLifespan = tokenLifespan,
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
                 val tokenLifespan: Long = TOKEN_LIFESPAN,
                 val timestamp: Long = System.currentTimeMillis(),
                 val expires: Long = timestamp + tokenLifespan,
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

internal class LoginTokenManager{
    private val concurrencyLock = Object()
    private val loginTokens = mutableMapOf<String, LoginToken>()

    /**
     * Returns a login token iff a token with that
     * uuid exists, and the token has not expired
     */
    operator fun get( key: String ): LoginToken?{
        purgeExpired()

        synchronized(concurrencyLock) {
            return loginTokens[key]
        }
    }

    operator fun set( key: String, loginToken: LoginToken ){
        synchronized(concurrencyLock) {
            if (!loginToken.hasExpired()) {
                loginTokens[key] = loginToken
            }
        }
    }

    private fun purgeExpired(){
        synchronized(concurrencyLock){
            val keys = loginTokens.keys
            for( key in keys ){
                val loginToken = loginTokens[key]
                if( loginToken != null ){
                    if( loginToken.hasExpired() ){
                        loginTokens.remove(key)
                    }
                }
            }
        }
    }
}




/**
 * Tracks login attempts by a key, eg username or IP address.
 *
 * This class will sleep for a progressively longer period of
 * time for each failed login attempt, unless doSleep is set
 * to false.
 */
internal class LoginCooldown(
        val maxUnhinderedAttempts: Int = MAX_UNHINDERED_LOGIN_ATTEMPTS,
        val maxFailedLoginAttempts: Int = MAX_FAILED_LOGIN_ATTEMPTS,
        val attemptsExpireAfter: Long = ATTEMPTS_EXPIRE_AFTER,
        val doSleep: Boolean = true){
    private val attempts = mutableMapOf<String, LoginAttempt>()

    fun clearLoginAttempts( key: String ){
        attempts.remove(key)
    }

    fun incrementLoginAttempts( key: String ){
        val lastLoginAttempt = getAttempts(key)
        val attemptCount = lastLoginAttempt.count + 1
        val newLoginAttempt = LoginAttempt(attemptCount)
        attempts[key] = newLoginAttempt

        if( doSleep ) {
            val badAttemptCount = attemptCount - maxUnhinderedAttempts
            sleep(badAttemptCount)
        }
    }

    fun maxFailedLoginAttemptsExceeded(key: String): Boolean{
        val lastLoginAttempt = getAttempts(key)
        return lastLoginAttempt.count > maxFailedLoginAttempts
    }

    private fun getAttempts( key: String ): LoginAttempt{
        val result = attempts[key]
        if( result != null ){
            val now = System.currentTimeMillis()
            val expires = result.timestamp + attemptsExpireAfter
            if( now <= expires ){
                attempts.remove(key)
            }
            else{
                return result
            }
        }

        return LoginAttempt()
    }

    private fun sleep( badAttemptCount: Int ){
        val sleep = getSleepTime(badAttemptCount)
        Thread.sleep(sleep)
    }

    private fun getSleepTime( badAttemptCount: Int ): Long{
        if( badAttemptCount <= 0 ){
            return 0
        }

        when(badAttemptCount){
            1 -> return 1_000
            2 -> return 2_000
            3 -> return 5_000
            4 -> return 10_000
            else -> return 15_000
        }
    }
}

data class LoginAttempt(val count: Int = 0,
                        val timestamp: Long = System.currentTimeMillis())