package galvin.ff.demo

import galvin.ff.*
import java.io.File

class Setup{
    companion object {
        private val timeProvider = DefaultTimeProvider()
        
        fun createDefaultUsers(userDB: UserDB, accountRequestDB: AccountRequestDB){
            val adminRole = Role( name = "Admin", permissions = listOf("Admin", "User") )
            userDB.storeRole(adminRole)

            val userRole = Role( name = "User", permissions = listOf("User") )
            userDB.storeRole(userRole)

            val adminUser = User(
                    login = "admin",
                    name = "Administrator",
                    displayName = "Administrator",
                    sortName = "Administrator",
                    created = timeProvider.now(),
                    active = false,
                    contact = listOf(
                            ContactInfo(
                                    type = "Email",
                                    description = "Email",
                                    contact = "admin@dev.null",
                                    primary = true
                            )
                    ),
                    roles = listOf( adminRole.name, userRole.name )
            )

            val adminAccountRequest = AccountRequest(
                    user = adminUser,
                    password = "password",
                    confirmPassword = "password"
            )
            accountRequestDB.storeAccountRequest(adminAccountRequest)
            accountRequestDB.approve( adminAccountRequest.uuid, "", timeProvider.now() )

            val testUser = User(
                    login = "user",
                    name = "Joe User",
                    displayName = "Joe User",
                    sortName = "User, Joe",
                    created = timeProvider.now(),
                    active = false,
                    contact = listOf(
                            ContactInfo(
                                    type = "Email",
                                    description = "Email",
                                    contact = "user@dev.null",
                                    primary = true
                            )
                    ),
                    roles = listOf( userRole.name )
            )

            val userAccountRequest = AccountRequest(
                    user = testUser,
                    password = "password",
                    confirmPassword = "password"
            )
            accountRequestDB.storeAccountRequest(userAccountRequest)
            accountRequestDB.approve( userAccountRequest.uuid, "", timeProvider.now() )
        }

        fun userDB(): UserDB {
            val databaseFile = File( "target/ff_demo_users_" + uuid() )
            return UserDB.SQLite(maxConnections = 1, databaseFile = databaseFile )
        }

        fun accountRequestDB( userDB: UserDB): AccountRequestDB {
            val accountRequestDatabaseFile = File( "target/ff_demo_account_requests_" + uuid() )
            val accountRequestUserDatabaseFile = File( "target/ff_demo_account_request_users_" + uuid() )

            return AccountRequestDB.SQLite(
                    userDB = userDB,
                    maxConnections = 1,
                    databaseFile = accountRequestDatabaseFile,
                    userDatabaseFile = accountRequestUserDatabaseFile )
        }

        fun auditDB(): AuditDB {
            val databaseFile = File( "target/ff_demo_audit_" + uuid() )
            return AuditDB.SQLite(maxConnections = 1, databaseFile = databaseFile )
        }
    }
}