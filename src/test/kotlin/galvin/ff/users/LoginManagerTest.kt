package galvin.ff.users

import galvin.ff.*
import galvin.ff.db.ConnectionManager
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test

class LoginManagerTest{
    private enum class LoginTokenManagerType{ inmem, sqlite, psql }

    companion object {
        @AfterClass @JvmStatic fun cleanup(){
            for (database in databases) {
                database.cleanup()
            }
        }
    }

    @Test fun should_login_successfully_by_password() {
        for( type in LoginTokenManagerType.values() ){
            val (auditDB, userDB, loginManager, roles, count) = testObjects(type)

            val passwords = mutableListOf<String>()
            for (i in 0..count) {
                passwords.add(uuid())
            }

            val users = mutableListOf<User>()
            for (i in 0..count) {
                users.add(generateUser(roles, uuid(), passwords[i]))
                userDB.storeUser(users[i])
            }

            for (i in 0..count) {
                val user = users[i]
                val password = passwords[i]
                val credentials = Credentials(username = user.login, password = password)

                val loginToken = loginManager.authenticate(credentials)
                Assert.assertEquals("Unexpected user", user.uuid, loginToken.user.uuid)
            }

            for (i in 0..count) {
                val user = users[i]
                val credentials = Credentials(x509SerialNumber = neverNull(user.serialNumber))

                val loginToken = loginManager.authenticate(credentials)
                Assert.assertEquals("Unexpected user", user.uuid, loginToken.user.uuid)
            }

            val auditEvents = auditDB.retrieveAccessInfo()
            Assert.assertEquals("Unexpected event count", 22, auditEvents.size)
            for ((i, event) in auditEvents.listIterator().withIndex()) {
                val index = if (i <= 10) i else i - 11
                val user = users[index]
                Assert.assertEquals("Unexpected user at index $index", user.uuid, event.userUuid)
                Assert.assertEquals("Unexpected user at index $index", user.uuid, event.resourceUuid)
                Assert.assertEquals("Unexpected user at index $index", user.login, event.resourceName)
                Assert.assertTrue("Unexpected access granted $index", event.permissionGranted)
            }
        }
    }

    @Test fun should_login_successfully_by_serial_number() {
        for( type in LoginTokenManagerType.values() ){
            val (_, userDB, loginManager, roles, count) = testObjects(type)

            val passwords = mutableListOf<String>()
            for (i in 0..count) {
                passwords.add(uuid())
            }

            val users = mutableListOf<User>()
            for (i in 0..count) {
                users.add(generateUser(roles, uuid(), passwords[i]))
                userDB.storeUser(users[i])
            }

            for (i in 0..count) {
                val user = users[i]
                val credentials = Credentials(x509SerialNumber = neverNull(user.serialNumber))

                val loginToken = loginManager.authenticate(credentials)
                Assert.assertEquals("Unexpected user", user.uuid, loginToken.user.uuid)
            }
        }
    }

    @Test fun should_login_successfully_by_login_token() {
        for( type in LoginTokenManagerType.values() ){
            val (_, userDB, loginManager, roles, count) = testObjects(type)

            val passwords = mutableListOf<String>()
            for (i in 0..count) {
                passwords.add(uuid())
            }

            val users = mutableListOf<User>()
            for (i in 0..count) {
                users.add(generateUser(roles, uuid(), passwords[i]))
                userDB.storeUser(users[i])
            }

            val loginTokens = mutableListOf<LoginToken>()

            for (i in 0..count) {
                val user = users[i]
                val password = passwords[i]
                val credentials = Credentials(username = user.login, password = password)

                val loginToken = loginManager.authenticate(credentials)
                loginTokens.add(loginToken)
            }

            for (loginToken in loginTokens) {
                val credentials = Credentials(tokenUuid = loginToken.uuid)
                val newLoginToken = loginManager.authenticate(credentials)
                Assert.assertEquals("Unexpected user", loginToken.user.uuid, newLoginToken.user.uuid)
            }

            try {
                val credentials = Credentials(tokenUuid = uuid())
                loginManager.authenticate(credentials)
                throw Exception("Login manager should have thrown")
            } catch (ex: LoginError) {
            }
        }
    }

    @Test
    fun should_log_out() {
        for( type in LoginTokenManagerType.values() ){
            val (_, userDB, loginManager, roles, count) = testObjects(type)

            val passwords = mutableListOf<String>()
            for (i in 0..count) {
                passwords.add(uuid())
            }

            val users = mutableListOf<User>()
            for (i in 0..count) {
                users.add(generateUser(roles, uuid(), passwords[i]))
                userDB.storeUser(users[i])
            }

            val loginTokens = mutableListOf<LoginToken>()

            for (i in 0..count) {
                val user = users[i]
                val password = passwords[i]
                val credentials = Credentials(username = user.login, password = password)

                val loginToken = loginManager.authenticate(credentials)
                loginTokens.add(loginToken)
            }

            for (loginToken in loginTokens) {
                loginManager.logout(loginToken.uuid)
            }

            for (loginToken in loginTokens) {
                try {
                    val credentials = Credentials(tokenUuid = loginToken.uuid)
                    loginManager.authenticate(credentials)
                    throw Exception("Login manager should have thrown")
                } catch (ex: LoginError) {
                    //no-op
                }
            }
        }
    }

    @Test
    fun should_purge_expired() {
        for( type in LoginTokenManagerType.values() ){
            val (_, userDB, loginManager, roles, count) = testObjects(type = type, tokenLifespan = 1, addressMaxUnhindered = 10_000)

            val passwords = mutableListOf<String>()
            for (i in 0..count) {
                passwords.add(uuid())
            }

            val users = mutableListOf<User>()
            for (i in 0..count) {
                users.add(generateUser(roles, uuid(), passwords[i]))
                userDB.storeUser(users[i])
            }

            val loginTokens = mutableListOf<LoginToken>()

            for (i in 0..count) {
                val user = users[i]
                val password = passwords[i]
                val credentials = Credentials(username = user.login, password = password)

                val loginToken = loginManager.authenticate(credentials)
                loginTokens.add(loginToken)
            }

            //let the tokens expire
            Thread.sleep(100)

            for (loginToken in loginTokens) {
                try {
                    val credentials = Credentials(tokenUuid = loginToken.uuid)
                    loginManager.authenticate(credentials)
                    throw Exception("Login manager should have thrown")
                } catch (ex: LoginError) {
                    //no-op
                }
            }

//            for (loginToken in loginTokens) {
//                Assert.assertTrue("Token should have expired", loginToken.hasExpired(timeProvider.now()))
//            }
        }
    }

    @Test
    fun should_log_out_concurrent_sessions() {
        for( type in LoginTokenManagerType.values() ){
            val (_, userDB, loginManager, roles, count) = testObjects(type = type, allowConcurrentLogins = false, loginMaxFailed = 100)
            val timeProvider = DefaultTimeProvider()

            val user = generateUser(roles)
            userDB.storeUser(user)

            val badLogins = mutableListOf<LoginToken>()
            val goodLogins = mutableListOf<LoginToken>()

            val address1 = "127.0.0.1"
            for (i in 0..count) {
                val credentials = Credentials(ipAddress = address1,
                        x509SerialNumber = neverNull(user.serialNumber))
                val loginToken = loginManager.authenticate(credentials)
                badLogins.add(loginToken)
            }

            val address2 = "127.0.0.2"
            for (i in 0..count) {
                val credentials = Credentials( ipAddress = address2, x509SerialNumber = neverNull(user.serialNumber) )
                val loginToken = loginManager.authenticate(credentials)
                goodLogins.add(loginToken)
            }

            for (token in badLogins) {
                try {
                    loginManager.authenticate( Credentials(ipAddress = token.ipAddress, tokenUuid = token.uuid) )
                    throw Exception("Token should have been logged out")
                }catch( loginError: LoginError ){}
            }

            for (token in goodLogins) {
                val loaded = loginManager.authenticate( Credentials(ipAddress = token.ipAddress, tokenUuid = token.uuid) )
                Assert.assertFalse("Token should not have been logged out", loaded.hasExpired(timeProvider.now()))
            }
        }
    }

    @Test
    fun should_fail_login_by_password() {
        for( type in LoginTokenManagerType.values() ){
            val (auditDB, userDB, loginManager, roles, count) = testObjects(type)

            val passwords = mutableListOf<String>()
            for (i in 0..count) {
                passwords.add(uuid())
            }

            val users = mutableListOf<User>()
            for (i in 0..count) {
                users.add(generateUser(roles, uuid(), passwords[i]))
                userDB.storeUser(users[i])
            }

            for (i in 0..count) {
                val user = users[i]
                val credentials = Credentials(username = user.login, password = uuid())

                try {
                    loginManager.authenticate(credentials)
                    throw Exception("Should have thrown login exception")
                } catch (ex: LoginError) {
                    //no-op
                }
            }

            val auditEvents = auditDB.retrieveAccessInfo()
            Assert.assertEquals("Unexpected event count", 11, auditEvents.size)
            for ((i, event) in auditEvents.listIterator().withIndex()) {
                val index = if (i <= 10) i else i - 11
                val user = users[index]
                Assert.assertEquals("Unexpected user at index $index", user.uuid, event.resourceUuid)
                Assert.assertEquals("Unexpected user at index $index", user.login, event.resourceName)
                Assert.assertFalse("Unexpected access granted $index", event.permissionGranted)
            }
        }
    }

    @Test
    fun should_lock_and_throw_on_max_failures() {
        for( type in LoginTokenManagerType.values() ){
            val (_, userDB, loginManager, roles, _) = testObjects(type)

            val user = generateUser(roles)
            userDB.storeUser(user)

            val attemptCount = MAX_FAILED_LOGIN_ATTEMPTS_PER_USER + 1
            for (i in 0..attemptCount) {
                val credentials = Credentials(username = user.login, password = uuid())

                try {
                    loginManager.authenticate(credentials)
                    throw Exception("Should have thrown login exception")
                } catch (ex: LoginError) {
                    //no-op
                }
            }

            try {
                val credentials = Credentials(username = user.login, password = uuid())
                loginManager.authenticate(credentials)
                throw Exception("Should have thrown login exception")
            } catch (ex: LoginError) {
                Assert.assertEquals("Unexpected error message", LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED, ex.message)
            }

            Assert.assertTrue("User should have been locked", userDB.isLocked(user.uuid))
        }
    }

    ///
    /// utilities
    ///

    class LoginManagerTestObjects(private val auditDB: AuditDB,
                                  private val userDB: UserDB,
                                  private val loginManager: LoginManager,
                                  private val roles: List<Role>,
                                  private val count: Int = 10 ){
        operator fun component1(): AuditDB{ return auditDB }
        operator fun component2(): UserDB{ return userDB }
        operator fun component3(): LoginManager{ return loginManager }
        operator fun component4(): List<Role>{ return roles }
        operator fun component5(): Int{ return count }
    }

    private fun testObjects(type: LoginTokenManagerType, allowConcurrentLogins: Boolean = true, tokenLifespan: Long = 1_000_000, loginMaxFailed: Int = 1, addressMaxUnhindered: Int = MAX_UNHINDERED_LOGIN_ATTEMPTS_IP_ADDRESS): LoginManagerTestObjects {
        val dbConnection = SqliteDbConnection()
        val auditDB = dbConnection.randomAuditDB()
        val userDB = dbConnection.randomUserDB()
        val roles = generateRoles(userdb = userDB)

        val config = LoginManagerConfig( tokenLifespan = tokenLifespan,
                                         sleepBetweenAttempts = false,
                                         allowConcurrentLogins = allowConcurrentLogins,
                                         loginMaxFailed = loginMaxFailed,
                                         addressMaxUnhindered = addressMaxUnhindered)

        val tokens = when(type){
            LoginTokenManagerType.inmem -> InMemLoginTokenManager()
            LoginTokenManagerType.sqlite -> LoginTokenManager.SQLite( userDB, 1, SqliteDbConnection.randomDbFile() )
            LoginTokenManagerType.psql -> LoginTokenManager.PostgreSQL( userDB, 1, "jdbc:postgresql://localhost:5432/" + PsqlDbConnection().createRandom() )
        }

        val loginManager = LoginManager( userDB = userDB, auditDB = auditDB, config = config, loginTokens = tokens )
        return LoginManagerTestObjects(auditDB, userDB, loginManager, roles)
    }
}