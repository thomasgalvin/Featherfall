package galvin.ff

import galvin.ff.db.ConnectionManager
import galvin.ff.db.QuietCloser
import java.io.File
import java.security.cert.X509Certificate
import java.sql.Connection
import javax.servlet.http.HttpServletRequest

const val TOKEN_LIFESPAN: Long = 1000 * 60 * 60 * 24 * 5 //five days in milliseconds
const val ATTEMPTS_EXPIRE_AFTER: Long = 1000 * 60 * 60 //one hour in ms

const val MAX_UNHINDERED_LOGIN_ATTEMPTS_PER_USER = 0
const val MAX_FAILED_LOGIN_ATTEMPTS_PER_USER = 5

const val MAX_UNHINDERED_LOGIN_ATTEMPTS_IP_ADDRESS = 0
const val MAX_FAILED_LOGIN_ATTEMPTS_PER_IP_ADDRESS = 15

class LoginManager( private val userDB: UserDB,
                    private val auditDB: AuditDB = NoOpAuditDB(),
                    private val timeProvider: TimeProvider = DefaultTimeProvider(),
                    private val config: LoginManagerConfig = LoginManagerConfig(),
                    private val loginTokens: LoginTokenManager = InMemLoginTokenManager(timeProvider)
){
    private val usernameCooldown = Cooldown(
            config.loginMaxUnhindered,
            config.loginMaxFailed,
            config.attemptsExpireAfter,
            config.sleepBetweenAttempts,
            timeProvider
    )
    private val addressCooldown = Cooldown(
            config.addressMaxUnhindered,
            config.addressMaxFailed,
            config.attemptsExpireAfter,
            config.sleepBetweenAttempts,
            timeProvider
    )

    fun authenticate( credentials: Credentials ): LoginToken{
        addressCooldown.doSleep( credentials.ipAddress )

        val loginType = credentials.getLoginType()
        val username = getUsername(credentials)
        val userUuid = neverNull( userDB.retrieveUuid(username) )

        if( usernameCooldown.maxFailedLoginsExceeded(username) ||
            addressCooldown.maxFailedLoginsExceeded(credentials.ipAddress) ) {
            loginFailed(loginType, credentials.loginProxyUuid, credentials.ipAddress, userUuid, username)
            throw LoginError.maxAttemptsExpired()
        }

        val user = getUser(credentials)
        if( user == null ){
            loginFailed(loginType, credentials.loginProxyUuid, credentials.ipAddress, userUuid, username)
            throw LoginError.invalidCredentials()
        }
        else{
            return loginSucceeded(loginType, credentials.loginProxyUuid, credentials.ipAddress, user )
        }
    }

    private fun loginFailed( loginType: LoginType,
                             loginProxyUuid: String,
                             ipAddress: String,
                             userUuid: String,
                             username: String ){
        usernameCooldown.loginFailed(username)
        addressCooldown.loginFailed(ipAddress)

        val accessInfo = accessInfo(loginType, loginProxyUuid, ipAddress, userUuid, username, false )
        auditDB.log(accessInfo)

        if( usernameCooldown.maxFailedLoginsExceeded(username) ){
            userDB.setLockedByLogin( username, true )

            val lockInfo = accessInfo(loginType, loginProxyUuid, ipAddress,
                                      userUuid, username, true, AccessType.LOCKED )
            auditDB.log(lockInfo)
        }


    }

    private fun loginSucceeded(loginType: LoginType,
                               loginProxyUuid: String,
                               ipAddress: String,
                               user: User ): LoginToken{
        usernameCooldown.loginSucceeded(user.login)
        addressCooldown.loginSucceeded(ipAddress)

        val accessInfo = accessInfo(loginType, loginProxyUuid, ipAddress, user.uuid, user.login, true )
        auditDB.log(accessInfo)

        val sanitizedUser = user.withoutPasswordHash()
        val permissions = userDB.retrievePermissions(sanitizedUser.roles)

        val loginToken = LoginToken(
                ipAddress = ipAddress,
                tokenLifespan = config.tokenLifespan,
                user = sanitizedUser,
                permissions = permissions,
                loginType = loginType,
                loginProxyUuid = loginProxyUuid,
                timestamp = timeProvider.now()
        )
        loginTokens.add(loginToken)

        if( !config.allowConcurrentLogins ){
            loginTokens.logoutExcept( ipAddress, user.uuid )
        }

        return loginToken
    }

    fun logout( loginTokenUuid: String ){
        val token = loginTokens[loginTokenUuid]
        if( token != null ){
            token.logout()

            var accessInfo = accessInfo(LoginType.LOGIN_TOKEN, token.loginProxyUuid, token.ipAddress, token.user.uuid, token.username, successful = true, accessType = AccessType.LOGOUT )
            auditDB.log(accessInfo)
        }

        loginTokens.remove(loginTokenUuid)
    }

    ///
    /// utilities
    ///

    private fun getUser( credentials: Credentials ): User?{
        val serialNumber = credentials.x509SerialNumber
        val now = timeProvider.now()

        if( !isBlank(serialNumber) ){
            return userDB.retrieveUserBySerialNumber(serialNumber)
        }
        else if( !isBlank(credentials.username) && !isBlank(credentials.password) ){
            return userDB.retrieveUserByLoginAndPassword(credentials.username, credentials.password)
        }
        else if( !isBlank(credentials.tokenUuid) ){
            val loginToken = loginTokens[credentials.tokenUuid]
            if( loginToken != null && !loginToken.hasExpired( now ) ){
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
            return credentials.x509SerialNumber
        }

        return ""
    }

    private fun accessInfo( loginType: LoginType,
                            loginProxyUuid: String,
                            ipAddress: String,
                            userUuid: String,
                            username: String,
                            successful: Boolean,
                            accessType: AccessType = AccessType.LOGIN): AccessInfo{
        var currentSystemInfo = auditDB.retrieveCurrentSystemInfo()
        if( currentSystemInfo == null ){
            currentSystemInfo = dummySystemInfo()
        }

        val initiatingUuid = if(successful) userUuid else ""
        val classification = currentSystemInfo.maximumClassification
        val systemInfoUuid = currentSystemInfo.uuid

        return AccessInfo(
                userUuid = initiatingUuid,
                loginType = loginType,
                loginProxyUuid = loginProxyUuid,
                ipAddress = ipAddress,
                timestamp = timeProvider.now(),
                resourceUuid = userUuid,
                resourceName = username,
                resourceType = RESOURCE_TYPE_USER_ACCOUNT,
                classification = classification,
                accessType = accessType,
                permissionGranted = successful,
                systemInfoUuid = systemInfoUuid
        )
    }
}

data class Credentials(val ipAddress: String = "",
                       val x509: X509Certificate? = null,
                       val x509SerialNumber: String = getSerialNumber(x509),
                       val username: String = "",
                       val password: String = "",
                       val tokenUuid: String = "",
                       val loginProxyUuid: String = "") {
    fun getLoginType(): LoginType {
        if ( !isBlank( x509SerialNumber ) ) {
            return LoginType.PKI
        }
        else if (!isBlank(tokenUuid)) {
            return LoginType.LOGIN_TOKEN
        }

        return LoginType.USERNAME_PASSWORD
    }

    companion object {
        fun copy(httpRequest: HttpServletRequest, credentials: Credentials? = null ): Credentials{
            var result = if(credentials==null) Credentials() else credentials

            val ipAddress = getIpAddress(httpRequest)
            result = result.copy(ipAddress = ipAddress)

            val certificates = httpRequest.getAttribute("javax.servlet.request.X509Certificate")
            if( certificates is Array<*> ) {
                if (!certificates.isEmpty()) {
                    val x509 = certificates[0]
                    if( x509 is X509Certificate) {
                        val serial = getSerialNumber(x509)
                        result = result.copy(x509 = x509, x509SerialNumber = serial)
                    }
                }
            }

            return result
        }
    }
}

data class LoginToken(val uuid: String = uuid(),
                      val ipAddress: String = "",
                      val tokenLifespan: Long = TOKEN_LIFESPAN,
                      val timestamp: Long,
                      val expires: Long = timestamp + tokenLifespan,
                      val user: User,
                      val username: String = user.login,
                      val permissions: List<String>,
                      val loginType: LoginType,
                      val loginProxyUuid: String = "") {
    private var loggedOut = false

    fun hasExpired(currentTime: Long): Boolean {
        return loggedOut || expires <= currentTime
    }

    fun logout(){
        loggedOut = true
    }
}

data class LoginManagerConfig(val tokenLifespan: Long = TOKEN_LIFESPAN,
                              val loginMaxUnhindered: Int = MAX_UNHINDERED_LOGIN_ATTEMPTS_PER_USER,
                              val loginMaxFailed: Int = MAX_FAILED_LOGIN_ATTEMPTS_PER_USER,
                              val addressMaxUnhindered: Int = MAX_UNHINDERED_LOGIN_ATTEMPTS_IP_ADDRESS,
                              val addressMaxFailed: Int = MAX_FAILED_LOGIN_ATTEMPTS_PER_IP_ADDRESS,
                              val attemptsExpireAfter: Long = ATTEMPTS_EXPIRE_AFTER,
                              val sleepBetweenAttempts: Boolean = true,
                              val allowConcurrentLogins: Boolean = true
)

/**
 * Tracks login attempts by a key, eg username or IP address.
 *
 * The doSleep() method pauses the executing thread for a progressively
 * longer period of time. doSleep() is a no-op if sleepBetweenAttempts is
 * set to `false`.
 */
internal class Cooldown( val maxUnhinderedAttempts: Int,
                         val maxFailedLoginAttempts: Int,
                         val attemptsExpireAfter: Long,
                         val sleepBetweenAttempts: Boolean,
                         val timeProvider: TimeProvider){
    private val attempts = mutableMapOf<String, LoginAttempt>()

    fun loginFailed( key: String? ){
        if( key != null ){
            val attempt = getAttempts(key)
            attempts[key] = attempt.increment( timeProvider.now() )
        }
    }

    fun loginSucceeded( key: String? ){
        if( key != null ){
            attempts.remove(key)
        }
    }

    fun maxFailedLoginsExceeded( key: String? ): Boolean{
        if( key != null ){
            val attempt = getAttempts(key)
            return attempt.count > maxFailedLoginAttempts
        }

        return false
    }

    fun doSleep( key: String? ){
        if( !sleepBetweenAttempts ) return

        if( key != null ){
            val attempt = getAttempts(key)
            val count = attempt.count - maxUnhinderedAttempts
            val sleep = getSleepTime(count)
            Thread.sleep(sleep)
        }
    }

    private fun getAttempts( key: String ): LoginAttempt{
        val result = attempts[key]
        if( result != null ){
            if( result.hasExpired( timeProvider.now(), attemptsExpireAfter ) ){
                attempts.remove(key)
            }
            else{
                return result
            }
        }

        return LoginAttempt( timestamp = timeProvider.now() )
    }

    private fun getSleepTime( badAttemptCount: Int ): Long{
        if( badAttemptCount <= 0 ){
            return 0
        }

        return when(badAttemptCount){
            1 -> 1_000
            2 -> 2_000
            3 -> 5_000
            4 -> 10_000
            else -> 15_000
        }
    }
}

internal data class LoginAttempt(val count: Int = 0,
                                 val timestamp: Long ){
    fun hasExpired( now: Long, expiresAfter: Long ): Boolean{
        return now >= timestamp + expiresAfter
    }

    fun increment( timestamp: Long): LoginAttempt{
        return LoginAttempt( count = count+1, timestamp = timestamp )
    }
}

interface LoginTokenManager{
    /**
     * Returns a login token iff a token with that
     * uuid exists, and the token has not expired
     */
    operator fun get( loginTokenUuid: String ): LoginToken?

    /**
     * Adds a login token to the database.
     */
    fun add( loginToken: LoginToken )

    fun remove( loginTokenUuid: String )

    /**
     * Used to disable concurrent logins. This method will
     * expire and remove any tokens that belong to the user
     * (userUuid) but do *not* correspond to the given ip address.
     */
    fun logoutExcept( ipAddress: String, userUuid: String )

    companion object {
        fun InMem( timeProvider: TimeProvider = DefaultTimeProvider() ): LoginTokenManager = InMemLoginTokenManager(timeProvider)

        fun SQLite(userDB: UserDB, maxConnections: Int, databaseFile: File, timeout: Long = 60_000, timeProvider: TimeProvider = DefaultTimeProvider() ): LoginTokenManager{
            val connectionManager = ConnectionManager.SQLite(maxConnections, databaseFile, timeout)
            val classpath = "/galvin/ff/db/sqlite/"
            return DBLoginTokenManager( connectionManager, classpath, userDB, timeProvider)
        }

        fun PostgreSQL( userDB: UserDB, maxConnections: Int, connectionURL: String, timeout: Long = 60_000, username: String? = null, password: String? = null, timeProvider: TimeProvider = DefaultTimeProvider() ): LoginTokenManager{
            val connectionManager = ConnectionManager.PostgreSQL(maxConnections, connectionURL, timeout, username, password)
            val classpath = "/galvin/ff/db/psql/"
            return DBLoginTokenManager( connectionManager, classpath, userDB, timeProvider)
        }
    }
}

class InMemLoginTokenManager( private val timeProvider: TimeProvider = DefaultTimeProvider() ): LoginTokenManager{
    private val concurrencyLock = Object()
    private val loginTokens = mutableMapOf<String, LoginToken>()

    override operator fun get( loginTokenUuid: String ): LoginToken?{
        purgeExpired()

        synchronized(concurrencyLock) {
            return loginTokens[loginTokenUuid]
        }
    }

    override fun add( loginToken: LoginToken ){
        synchronized(concurrencyLock) {
            if( !loginToken.hasExpired( timeProvider.now() ) ) {
                loginTokens[loginToken.uuid] = loginToken
            }
        }
    }

    override fun remove( loginTokenUuid: String ){
        //in case someone has cached this for some (bad) reason
        val loginToken = get(loginTokenUuid)
        loginToken?.logout()

        synchronized(concurrencyLock) {
            loginTokens.remove(loginTokenUuid)
        }
    }

    override fun logoutExcept( ipAddress: String, userUuid: String ){
        purgeExpired()

        synchronized(concurrencyLock){
            val expiredTokens = mutableListOf<LoginToken>()

            val tokens = loginTokens.values
            for( token in tokens ){
                if( token.user.uuid == userUuid && token.ipAddress != ipAddress ){
                    token.logout()
                    expiredTokens.add(token)
                }
            }

            for( loginToken in expiredTokens ) invalidate(loginToken)
        }
    }

    private fun purgeExpired(){
        synchronized(concurrencyLock){
            val expiredTokens = mutableListOf<LoginToken>()

            val now = timeProvider.now()
            val tokens = loginTokens.values
            for( token in tokens ){
                if( token.hasExpired(now) ){
                    token.logout()
                    expiredTokens.add(token)
                }
            }

            for( loginToken in expiredTokens ) invalidate(loginToken)
        }
    }

    private fun invalidate( loginToken: LoginToken ){
        loginToken.logout()
        loginTokens.remove(loginToken.uuid)
    }
}

class DBLoginTokenManager(private val connectionManager: ConnectionManager,
                          sqlClasspath: String,
                          private val userDB: UserDB,
                          private val timeProvider: TimeProvider = DefaultTimeProvider()
): LoginTokenManager{
    private val concurrencyLock = Object()

    private val sqlCreateTableLoginTokens = loadFromClasspathOrThrow("$sqlClasspath/login/create_table_login_tokens.sql")
    private val sqlDeleteLoginTokenByUuid = loadFromClasspathOrThrow("$sqlClasspath/login/delete_login_token_by_uuid.sql")
    private val sqlStoreLoginToken = loadFromClasspathOrThrow("$sqlClasspath/login/store_login_token.sql")
    private val sqlRetrieveLoginTokenByUuid = loadFromClasspathOrThrow("$sqlClasspath/login/retrieve_login_token_by_uuid.sql")
    private val sqlLogoutExceptIpAddress = loadFromClasspathOrThrow("$sqlClasspath/login/logout_except_ip_address.sql")
    private val sqlPurgeExpired = loadFromClasspathOrThrow("$sqlClasspath/login/purge_expired.sql")

    fun conn(): Connection = connectionManager.connect()

    init{
        val conn = conn()
        try {
            executeUpdate(conn, sqlCreateTableLoginTokens)
            conn.commit()
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }

        purgeExpired()
    }

    override fun get(loginTokenUuid: String): LoginToken? {
        purgeExpired()

        synchronized(concurrencyLock) {
            val conn = conn()
            try {
                val statement = conn.prepareStatement(sqlRetrieveLoginTokenByUuid)
                try {
                    statement.setString(1, loginTokenUuid)
                    val results = statement.executeQuery()
                    try {
                        if(!results.next()) return null

                        val expires = results.getLong("expiresTimestamp")
                        if(expires <= timeProvider.now()) {
                            return null
                        }

                        val userUuid = results.getString("userUuid")
                        val user = userDB.retrieveUser(userUuid) ?: return null
                        val permissions = userDB.retrievePermissions(user.roles)

                        return LoginToken(
                                uuid = results.getString("uuid"),
                                ipAddress = results.getString("ipAddress"),
                                tokenLifespan = results.getLong("tokenLifespan"),
                                timestamp = results.getLong("createdTimestamp"),
                                expires = expires,
                                user = user.withoutPasswordHash(),
                                username = user.login,
                                permissions = permissions,
                                loginType = LoginType.valueOf(results.getString("loginType")),
                                loginProxyUuid = results.getString("loginProxyUuid")
                        )
                    }
                    finally { QuietCloser.close(results) }
                }
                finally { QuietCloser.close(statement) }

            }
            finally { closeAndRelease(conn, connectionManager) }
        }
    }

    override fun add(loginToken: LoginToken) {
        purgeExpired()

        synchronized(concurrencyLock) {
            val conn = conn()
            try {
                val statement = conn.prepareStatement(sqlStoreLoginToken)
                try {
                    statement.setString(1, loginToken.uuid)
                    statement.setString(2, loginToken.ipAddress)
                    statement.setLong(3, loginToken.tokenLifespan)
                    statement.setLong(4, loginToken.timestamp)
                    statement.setLong(5, loginToken.expires)
                    statement.setString(6, loginToken.user.uuid)
                    statement.setString(7, loginToken.loginType.name)
                    statement.setString(8, loginToken.loginProxyUuid)
                    statement.executeUpdate()
                    conn.commit()
                }
                finally {
                    QuietCloser.close(statement)
                }

            }
            catch(t: Throwable) { rollbackAndRelease(conn, connectionManager); throw t }
            finally { closeAndRelease(conn, connectionManager) }
        }
    }

    override fun remove(loginTokenUuid: String) {
        purgeExpired()

        synchronized(concurrencyLock) {
            val conn = conn()
            try {
                val statement = conn.prepareStatement(sqlDeleteLoginTokenByUuid)
                try {
                    statement.setString(1, loginTokenUuid)
                    statement.executeUpdate()
                    conn.commit()
                }
                finally {
                    QuietCloser.close(statement)
                }

            }
            catch(t: Throwable) { rollbackAndRelease(conn, connectionManager); throw t }
            finally { closeAndRelease(conn, connectionManager) }
        }
    }

    override fun logoutExcept(ipAddress: String, userUuid: String) {
        purgeExpired()

        synchronized(concurrencyLock) {
            val conn = conn()
            try {
                val statement = conn.prepareStatement(sqlLogoutExceptIpAddress)
                try {
                    statement.setString(1, userUuid)
                    statement.setString(2, ipAddress)
                    statement.executeUpdate()
                    conn.commit()
                }
                finally {
                    QuietCloser.close(statement)
                }

            }
            catch(t: Throwable) { rollbackAndRelease(conn, connectionManager); throw t }
            finally { closeAndRelease(conn, connectionManager) }
        }
    }

    private fun purgeExpired(){
        synchronized(concurrencyLock) {
            val conn = conn()
            try {
                val statement = conn.prepareStatement(sqlPurgeExpired)
                try {
                    statement.setLong(1, timeProvider.now() )
                    statement.executeUpdate()
                    conn.commit()
                }
                finally {
                    QuietCloser.close(statement)
                }

            }
            catch(t: Throwable) { rollbackAndRelease(conn, connectionManager); throw t }
            finally { closeAndRelease(conn, connectionManager) }
        }
    }
}