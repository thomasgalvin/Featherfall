package galvin.ff.users

import galvin.ff.*
import galvin.ff.tools.AccountManager
import galvin.ff.tools.AccountManagerOptions
import org.junit.Assert
import org.junit.Test
import java.io.File

class AccountManagerQueryTest{
    @Test fun should_list_all_accounts() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = testObjects(database = database)
            val users = objects.accountManager.retrieveAllUsers(options = objects.accountManagerOptions)
            Assert.assertEquals("Unexpected user count", objects.allUsers.size, users.size)
        }
    }

    @Test fun should_list_locked_accounts() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val unlockedCount = 10
            val lockedCount = 5
            val objects = testObjects(database = database, count = 0, unlockedCount = unlockedCount, lockedCount = lockedCount)
            val users = objects.accountManager.retrieveLockedUsers(options = objects.accountManagerOptions)
            Assert.assertEquals("Unexpected locked user count", lockedCount, objects.lockedUsers.size)
            Assert.assertEquals("Unexpected locked user count", lockedCount, users.size)

            for (user in users) {
                val expected = objects.lockedUsersMap[user.uuid]
                Assert.assertEquals("Unexpected user", expected, user)
            }
        }
    }

    @Test fun should_list_unlocked_accounts() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val unlockedCount = 5
            val lockedCount = 10
            val objects = testObjects(database = database, count = 0, unlockedCount = unlockedCount, lockedCount = lockedCount)
            val users = objects.accountManager.retrieveUnlockedUsers(options = objects.accountManagerOptions)
            Assert.assertEquals("Unexpected unlocked user count", unlockedCount, objects.unlockedUsers.size)
            Assert.assertEquals("Unexpected unlocked user count", unlockedCount, users.size)

            for (user in users) {
                val expected = objects.unlockedUsersMap[user.uuid]
                Assert.assertEquals("Unexpected user", expected, user)
            }
        }
    }

    @Test fun should_lock_account() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val unlockedCount = 5
            val lockedCount = 10
            val objects = testObjects(database = database, count = 0, unlockedCount = unlockedCount, lockedCount = lockedCount)
            for (user in objects.unlockedUsers) {
                Assert.assertFalse("User should not have been locked", user.locked)
                objects.accountManager.lockUser(objects.accountManagerOptions, user.login)
                val loaded = objects.userDB.retrieveUser(user.uuid)
                Assert.assertEquals("User should have been locked", true, loaded?.locked)
            }
        }
    }

    @Test fun should_unlock_account() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val unlockedCount = 5
            val lockedCount = 10
            val objects = testObjects(database = database, count = 0, unlockedCount = unlockedCount, lockedCount = lockedCount)
            for (user in objects.lockedUsers) {
                Assert.assertTrue("User should have been locked", user.locked)
                objects.accountManager.unlockUser(objects.accountManagerOptions, user.login)
                val loaded = objects.userDB.retrieveUser(user.uuid)
                Assert.assertEquals("User should not have been locked", false, loaded?.locked)
            }
        }
    }

    @Test fun should_list_inactive_accounts() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val activeCount = 7
            val inactiveCount = 3
            val objects = testObjects(database = database, count = 0, activeCount = activeCount, inactiveCount = inactiveCount)
            val users = objects.accountManager.retrieveInactiveUsers(options = objects.accountManagerOptions)
            Assert.assertEquals("Unexpected inactive user count", inactiveCount, objects.inactiveUsers.size)
            Assert.assertEquals("Unexpected inactive user count", inactiveCount, users.size)

            for (user in users) {
                val expected = objects.inactiveUsersMap[user.uuid]
                Assert.assertEquals("Unexpected user", expected, user)
            }
        }
    }

    @Test fun should_list_active_accounts() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val activeCount = 7
            val inactiveCount = 3
            val objects = testObjects(database = database, count = 0, activeCount = activeCount, inactiveCount = inactiveCount)
            val users = objects.accountManager.retrieveActiveUsers(options = objects.accountManagerOptions)
            Assert.assertEquals("Unexpected inactive user count", activeCount, objects.activeUsers.size)
            Assert.assertEquals("Unexpected inactive user count", activeCount, users.size)

            for (user in users) {
                val expected = objects.activeUsersMap[user.uuid]
                Assert.assertEquals("Unexpected user", expected, user)
            }
        }
    }

    @Test fun should_deactivate_account() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val inactiveCount = 3
            val activeCount = 7
            val objects = testObjects(database = database, count = 0, inactiveCount = inactiveCount, activeCount = activeCount)
            for (user in objects.activeUsers) {
                Assert.assertTrue("User should have been active", user.active)
                objects.accountManager.deactivateUser(objects.accountManagerOptions, user.login)
                val loaded = objects.userDB.retrieveUser(user.uuid)
                Assert.assertEquals("User should not have been active", false, loaded?.active)
            }
        }
    }

    @Test fun should_activate_account() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val inactiveCount = 3
            val activeCount = 7
            val objects = testObjects(database = database, count = 0, inactiveCount = inactiveCount, activeCount = activeCount)
            for (user in objects.inactiveUsers) {
                Assert.assertFalse("User should not have been active", user.active)
                objects.accountManager.activateUser(objects.accountManagerOptions, user.login)
                val loaded = objects.userDB.retrieveUser(user.uuid)
                Assert.assertEquals("User should have been active", true, loaded?.active)
            }
        }
    }

    @Test fun should_update_password() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val count = 5
            val objects = testObjects(database = database, count = count)
            for (user in objects.allUsers) {
                val newPassword = uuid()
                objects.accountManager.updatePassword(objects.accountManagerOptions, user.login, newPassword)
                objects.userDB.validatePassword(user.uuid, newPassword)
            }
        }
    }

    @Test fun should_update_credentials() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val count = 5
            val objects = testObjects(database = database, count = count)
            for (user in objects.allUsers) {
                val expectedCredentials = CertificateData(
                        credential = uuid(),
                        serialNumber = uuid(),
                        distinguishedName = uuid(),
                        countryCode = uuid(),
                        citizenship = uuid()
                )
                objects.accountManager.updateCredentials(
                        objects.accountManagerOptions,
                        user.login,
                        expectedCredentials.credential,
                        expectedCredentials.serialNumber,
                        expectedCredentials.distinguishedName,
                        expectedCredentials.countryCode,
                        expectedCredentials.citizenship
                )

                val expected = user.withCredentials(expectedCredentials)
                val loaded = objects.userDB.retrieveUser(user.uuid)
                Assert.assertEquals("Unexpected credentials", expected, loaded)

                val loadedCredentials = objects.userDB.retrieveCredentials(user.uuid)
                Assert.assertEquals("Unexpected credentials", expectedCredentials, loadedCredentials)
            }
        }
    }

    @Test fun should_print_short(){
        val users = listOf(
                printableUser(1),
                printableUser(2, active=false),
                printableUser(3, locked = true),
                printableUser(4),
                printableUser(5)
        )
        val accountManager = AccountManager()

        ConsoleGrabber.grabConsole()
        accountManager.printShort(users)
        val result = ConsoleGrabber.releaseConsole(false)

        val expected = loadResourceAndReadString("galvin/ff/print_short.txt")
        Assert.assertEquals( "Unexpected user info", expected, result )
    }

    @Test fun should_print_long(){
        val created = 1510759774477
        val users = listOf(
                printableUser(1, created=created),
                printableUser(2, created=created, active=false),
                printableUser(3, created=created, locked = true),
                printableUser(4, created=created),
                printableUser(5, created=created)
        )
        val accountManager = AccountManager()

        ConsoleGrabber.grabConsole()
        accountManager.printLong(users)
        val result = ConsoleGrabber.releaseConsole(false)

        val expected = loadResourceAndReadString("galvin/ff/print_long.txt")
        Assert.assertEquals( "Unexpected user info", expected, result )
    }

    @Test fun should_print_roles() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = true)

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager

            ConsoleGrabber.grabConsole()
            accountManager.printRoles(accountManagerOptions)
            val result = ConsoleGrabber.releaseConsole(false)

            val expected = loadResourceAndReadString("galvin/ff/print_roles.txt")
            Assert.assertEquals("Unexpected role info", expected, result)
        }
    }

    @Test fun should_create_roles() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = false)

            val names = listOf("Role 1", "Role 2", "Role 3")
            val permissions = listOf("Perm 1", "Perm 2", "Perm 3")

            val expectedOne = Role(name = "Role 1", permissions = permissions, active = true)
            val expectedTwo = Role(name = "Role 2", permissions = permissions, active = true)
            val expectedThree = Role(name = "Role 3", permissions = permissions, active = true)

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager
            accountManager.createRoles(accountManagerOptions, names, permissions)

            val loadedRoles = objects.userDB.listRoles()
            Assert.assertEquals("Unexpected role count", 3, loadedRoles.size)
            Assert.assertEquals("Unexpected role", expectedOne, loadedRoles[0])
            Assert.assertEquals("Unexpected role", expectedTwo, loadedRoles[1])
            Assert.assertEquals("Unexpected role", expectedThree, loadedRoles[2])
        }
    }

    @Test fun should_not_create_duplicate_roles() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = false)

            val names = listOf("Role 1", "Role 1", "Role 1")
            val permissions = listOf("Perm 1", "Perm 2", "Perm 3")

            val expectedOne = Role(name = "Role 1", permissions = permissions, active = true)

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager
            accountManager.createRoles(accountManagerOptions, names, permissions)

            val loadedRoles = objects.userDB.listRoles()
            Assert.assertEquals("Unexpected role count", 1, loadedRoles.size)
            Assert.assertEquals("Unexpected role", expectedOne, loadedRoles[0])
        }
    }

    @Test fun should_add_permissions_to_roles() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = false)

            val names = listOf("Role 1", "Role 2", "Role 3")
            val permissions = listOf("Perm 1", "Perm 2", "Perm 3")
            val toAdd = listOf("Perm 4", "Perm 5")
            val addedPermissions = listOf("Perm 1", "Perm 2", "Perm 3", "Perm 4", "Perm 5")

            val expectedOne = Role(name = "Role 1", permissions = addedPermissions, active = true)
            val expectedTwo = Role(name = "Role 2", permissions = addedPermissions, active = true)
            val expectedThree = Role(name = "Role 3", permissions = addedPermissions, active = true)

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager
            accountManager.createRoles(accountManagerOptions, names, permissions)
            accountManager.addPermissions(accountManagerOptions, names, toAdd)

            val loadedRoles = objects.userDB.listRoles()
            Assert.assertEquals("Unexpected role count", 3, loadedRoles.size)
            Assert.assertEquals("Unexpected role", expectedOne, loadedRoles[0])
            Assert.assertEquals("Unexpected role", expectedTwo, loadedRoles[1])
            Assert.assertEquals("Unexpected role", expectedThree, loadedRoles[2])
        }
    }

    @Test fun should_add_permissions_to_role() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = false)

            val roleOne = Role(name = "Role 1", permissions = listOf("Perm 1", "Perm 2", "Perm 3"))
            objects.userDB.storeRole(roleOne)

            val roleTwo = Role(name = "Role 2", permissions = listOf("Perm 1", "Perm 2", "Perm 3"))
            objects.userDB.storeRole(roleTwo)

            val expectedPermissions = listOf("Perm 1", "Perm 2", "Perm 3", "Perm 4", "Perm 5")
            val expectedRole = roleTwo.copy(permissions = expectedPermissions)

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager
            accountManager.addPermissions(accountManagerOptions, listOf("Role 2"), listOf("Perm 4", "Perm 5"))

            val loadedRoles = objects.userDB.listRoles()
            Assert.assertEquals("Unexpected role count", 2, loadedRoles.size)

            val loaded = objects.userDB.retrieveRole("Role 2")
            Assert.assertEquals("Unexpected role", expectedRole, loaded)

            val unchangedLoaded = objects.userDB.retrieveRole("Role 1")
            Assert.assertEquals("Unexpected role", roleOne, unchangedLoaded)
        }
    }

    @Test fun should_remove_permissions_from_roles() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = false)

            val names = listOf("Role 1", "Role 2", "Role 3")
            val permissions = listOf("Perm 1", "Perm 2", "Perm 3", "Perm 4", "Perm 5")
            val toRemove = listOf("Perm 4", "Perm 5")
            val finalPermissions = listOf("Perm 1", "Perm 2", "Perm 3")

            val expectedOne = Role(name = "Role 1", permissions = finalPermissions, active = true)
            val expectedTwo = Role(name = "Role 2", permissions = finalPermissions, active = true)
            val expectedThree = Role(name = "Role 3", permissions = finalPermissions, active = true)

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager
            accountManager.createRoles(accountManagerOptions, names, permissions)
            accountManager.removePermissions(accountManagerOptions, names, toRemove)

            val loadedRoles = objects.userDB.listRoles()
            Assert.assertEquals("Unexpected role count", 3, loadedRoles.size)
            Assert.assertEquals("Unexpected role", expectedOne, loadedRoles[0])
            Assert.assertEquals("Unexpected role", expectedTwo, loadedRoles[1])
            Assert.assertEquals("Unexpected role", expectedThree, loadedRoles[2])
        }
    }

    @Test fun should_remove_permissions_from_role() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = false)

            val roleOne = Role(name = "Role 1", permissions = listOf("Perm 1", "Perm 2", "Perm 3"))
            objects.userDB.storeRole(roleOne)

            val roleTwo = Role(name = "Role 2", permissions = listOf("Perm 1", "Perm 2", "Perm 3"))
            objects.userDB.storeRole(roleTwo)

            val expectedPermissions = listOf("Perm 1", "Perm 2")
            val expectedRole = roleTwo.copy(permissions = expectedPermissions)

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager
            accountManager.removePermissions(accountManagerOptions, listOf("Role 2"), listOf("Perm 3"))

            val loadedRoles = objects.userDB.listRoles()
            Assert.assertEquals("Unexpected role count", 2, loadedRoles.size)

            val loaded = objects.userDB.retrieveRole("Role 2")
            Assert.assertEquals("Unexpected role", expectedRole, loaded)

            val unchangedLoaded = objects.userDB.retrieveRole("Role 1")
            Assert.assertEquals("Unexpected role", roleOne, unchangedLoaded)
        }
    }

    @Test fun should_activate_accounts() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = true)

            val inactiveNames = mutableListOf<String>()
            for (role in objects.roles) {
                if (!role.active) {
                    inactiveNames.add(role.name)
                }
            }
            Assert.assertFalse("No inactive accounts found", inactiveNames.isEmpty())

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager
            accountManager.activateRoles(accountManagerOptions, inactiveNames)

            val loadedRoles = objects.userDB.listRoles()
            for (role in loadedRoles) {
                Assert.assertTrue("Role should have been active: ${role.name}", role.active)
            }

            for (role in objects.roles) {
                val expected = role.copy(active = true)
                val loaded = objects.userDB.retrieveRole(expected.name)
                Assert.assertEquals("Role was improperly modified", expected, loaded)
            }
        }
    }

    @Test fun should_deactivate_accounts() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val objects = roleTestObjects(database = database, createRoles = true)

            val activeNames = mutableListOf<String>()
            for (role in objects.roles) {
                if (role.active) {
                    activeNames.add(role.name)
                }
            }
            Assert.assertFalse("No active accounts found", activeNames.isEmpty())

            val accountManagerOptions = objects.accountManagerOptions
            val accountManager = objects.accountManager
            accountManager.deactivateRoles(accountManagerOptions, activeNames)

            val loadedRoles = objects.userDB.listRoles()
            for (role in loadedRoles) {
                Assert.assertFalse("Role should have been inactive: ${role.name}", role.active)
            }

            for (role in objects.roles) {
                val expected = role.copy(active = false)
                val loaded = objects.userDB.retrieveRole(expected.name)
                Assert.assertEquals("Role was improperly modified", expected, loaded)
            }
        }
    }

    //
    // Utilities
    //

    private fun printableUser(count: Int,
                              active: Boolean = true,
                              locked: Boolean = false,
                              created: Long = System.currentTimeMillis()): User{
        val roles = listOf(
                "Role $count:A",
                "Role $count:B",
                "Role $count:C"
        )

        val contact = listOf(
                ContactInfo("Email", "Work Email", "user$count@dev.null", true),
                ContactInfo("Email", "Home Email", "user$count@gmail.com", false),
                ContactInfo("Phone", "Work Phone", "1.800.555.5555", true),
                ContactInfo("Phone", "Home Phone", "555.5555", false)
        )

        return User(
                login="user $count",
                name = "name $count",
                displayName = "display name $count",
                sortName = "sort Name $count",
                prependToName = "prepend $count",
                appendToName = "append $count",
                credential = "credential $count",
                serialNumber = "serial $count",
                distinguishedName = "distinguished name $count",
                homeAgency = "home agency $count",
                agency = "agency $count",
                countryCode = "country code $count",
                citizenship = "citizenship $count",
                created = created,
                active = active,
                locked = locked,
                roles = roles,
                contact = contact
        )
    }

    private fun testObjects(database: DbConnection,
                            count: Int = 10,
                            activeCount: Int = 0, inactiveCount: Int = 0,
                            unlockedCount: Int = 0, lockedCount: Int = 0 ): AccountManagerQueryTestObjects {
        val userDB: UserDB = database.randomUserDB()
        val roles: List<Role> = generateRoles(5)

        val allUsers = mutableListOf<User>()
        val activeUsers = mutableListOf<User>()
        val inactiveUsers = mutableListOf<User>()
        val unlockedUsers = mutableListOf<User>()
        val lockedUsers = mutableListOf<User>()

        val allUsersMap: MutableMap<String, User> = mutableMapOf()
        val activeUsersMap: MutableMap<String, User> = mutableMapOf()
        val inactiveUsersMap: MutableMap<String, User> = mutableMapOf()
        val lockedUsersMap: MutableMap<String, User> = mutableMapOf()
        val unlockedUsersMap: MutableMap<String, User> = mutableMapOf()

        for( i in 1..count){
            val user = generateUser(roles)
            userDB.storeUser(user)
            allUsersMap[user.uuid] = user
            allUsers.add(user)
        }

        for( i in 1..activeCount){
            val user = generateUser(systemRoles = roles, active = true)
            userDB.storeUser(user)
            allUsersMap[user.uuid] = user
            activeUsersMap[user.uuid] = user
            allUsers.add(user)
            activeUsers.add(user)
        }

        for( i in 1..inactiveCount){
            val user = generateUser(systemRoles = roles, active = false)
            userDB.storeUser(user)
            allUsersMap[user.uuid] = user
            inactiveUsersMap[user.uuid] = user
            allUsers.add(user)
            inactiveUsers.add(user)
        }

        for( i in 1..unlockedCount){
            val user = generateUser(systemRoles = roles, locked = false)
            userDB.storeUser(user)
            allUsersMap[user.uuid] = user
            unlockedUsersMap[user.uuid] = user
            allUsers.add(user)
            unlockedUsers.add(user)
        }

        for( i in 1..lockedCount){
            val user = generateUser(systemRoles = roles, locked = true)
            userDB.storeUser(user)
            allUsersMap[user.uuid] = user
            lockedUsersMap[user.uuid] = user
            allUsers.add(user)
            lockedUsers.add(user)
        }

        return AccountManagerQueryTestObjects(
                userDB = userDB,
                roles = roles,
                allUsers = allUsers,
                activeUsers = activeUsers,
                inactiveUsers = inactiveUsers,
                unlockedUsers = unlockedUsers,
                lockedUsers = lockedUsers,
                allUsersMap = allUsersMap,
                activeUsersMap = activeUsersMap,
                inactiveUsersMap = inactiveUsersMap,
                unlockedUsersMap = unlockedUsersMap,
                lockedUsersMap = lockedUsersMap
        )
    }

    private fun roleTestObjects( database: DbConnection, createRoles: Boolean = false ): AccountManagerQueryTestObjects {
        val userDB: UserDB = database.randomUserDB()
        val roles: MutableList<Role> = mutableListOf()

        if( createRoles ){
            for( i in 1..10 ){
                val name = "Name $i"
                val active = i % 2 == 0
                val permissions = listOf( "Permission $i A", "Permission $i B", "Permission $i C" )
                val role = Role( name = name, permissions = permissions, active = active )
                userDB.storeRole(role)
                roles.add(role)
            }
        }

        return AccountManagerQueryTestObjects(userDB = userDB, roles = roles)
    }

}



data class AccountManagerQueryTestObjects(
        val userDB: UserDB,
        val roles: List<Role>,
        val allUsers: List<User> = listOf(),
        val activeUsers: List<User> = listOf(),
        val inactiveUsers: List<User> = listOf(),
        val unlockedUsers: List<User> = listOf(),
        val lockedUsers: List<User> = listOf(),
        val allUsersMap: Map<String, User> = mapOf(),
        val activeUsersMap: Map<String, User> = mapOf(),
        val inactiveUsersMap: Map<String, User> = mapOf(),
        val unlockedUsersMap: Map<String, User> = mapOf(),
        val lockedUsersMap: Map<String, User> = mapOf(),
        val accountManager: AccountManager = AccountManager(),
        val accountManagerOptions: AccountManagerOptions = AccountManagerOptions(userDB = userDB)
)