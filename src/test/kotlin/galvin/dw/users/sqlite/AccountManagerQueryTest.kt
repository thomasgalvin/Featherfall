package galvin.dw.users.sqlite

import galvin.dw.Role
import galvin.dw.User
import galvin.dw.UserDB
import galvin.dw.sqlite.SQLiteUserDB
import galvin.dw.tools.AccountManager
import galvin.dw.tools.AccountManagerOptions
import org.junit.Assert
import org.junit.Test
import java.io.File

class AccountManagerQueryTest{
    @Test
    fun should_list_all_accounts(){
        val objects = testObjects()
        val users = objects.accountManager.retrieveAllUsers( options = objects.accountManagerOptions )
        Assert.assertEquals("Unexpected user count", objects.allUsers.size, users.size)
    }

    @Test fun should_list_locked_accounts(){
        val unlockedCount = 10
        val lockedCount = 5
        val objects = testObjects(count = 0, unlockedCount = unlockedCount, lockedCount = lockedCount)
        val users = objects.accountManager.retrieveLockedUsers( options = objects.accountManagerOptions )
        Assert.assertEquals("Unexpected locked user count", lockedCount, objects.lockedUsers.size)
        Assert.assertEquals("Unexpected locked user count", lockedCount, users.size)

        for(user in users){
            val expected = objects.lockedUsersMap[user.uuid]
            Assert.assertEquals("Unexpected user", expected, user)
        }
    }

    @Test fun should_list_unlocked_accounts(){
        val unlockedCount = 5
        val lockedCount = 10
        val objects = testObjects(count = 0, unlockedCount = unlockedCount, lockedCount = lockedCount)
        val users = objects.accountManager.retrieveUnlockedUsers( options = objects.accountManagerOptions )
        Assert.assertEquals("Unexpected unlocked user count", unlockedCount, objects.unlockedUsers.size)
        Assert.assertEquals("Unexpected unlocked user count", unlockedCount, users.size)

        for(user in users){
            val expected = objects.unlockedUsersMap[user.uuid]
            Assert.assertEquals("Unexpected user", expected, user)
        }
    }

    @Test fun should_list_inactive_accounts(){
        val activeCount = 7
        val inactiveCount = 3
        val objects = testObjects(count = 0, activeCount = activeCount, inactiveCount = inactiveCount)
        val users = objects.accountManager.retrieveInactiveUsers( options = objects.accountManagerOptions )
        Assert.assertEquals("Unexpected inactive user count", inactiveCount, objects.inactiveUsers.size)
        Assert.assertEquals("Unexpected inactive user count", inactiveCount, users.size)

        for(user in users){
            val expected = objects.inactiveUsersMap[user.uuid]
            Assert.assertEquals("Unexpected user", expected, user)
        }
    }

    @Test fun should_list_active_accounts(){
        val activeCount = 7
        val inactiveCount = 3
        val objects = testObjects(count = 0, activeCount = activeCount, inactiveCount = inactiveCount)
        val users = objects.accountManager.retrieveActiveUsers( options = objects.accountManagerOptions )
        Assert.assertEquals("Unexpected inactive user count", activeCount, objects.activeUsers.size)
        Assert.assertEquals("Unexpected inactive user count", activeCount, users.size)

        for(user in users){
            val expected = objects.activeUsersMap[user.uuid]
            Assert.assertEquals("Unexpected user", expected, user)
        }
    }

    private fun testObjects(count: Int = 10,
                            activeCount: Int = 0, inactiveCount: Int = 0,
                            unlockedCount: Int = 0, lockedCount: Int = 0 ): AccountManagerQueryTestObjects{
        val userDbFile: File = randomDbFile()
        val userDB: UserDB = SQLiteUserDB(userDbFile)
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
                userDbFile = userDbFile,
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
}



data class AccountManagerQueryTestObjects(
        val userDbFile: File,
        val userDB: UserDB,
        val roles: List<Role>,
        val allUsers: List<User>,
        val activeUsers: List<User>,
        val inactiveUsers: List<User>,
        val unlockedUsers: List<User>,
        val lockedUsers: List<User>,
        val allUsersMap: Map<String, User>,
        val activeUsersMap: Map<String, User>,
        val inactiveUsersMap: Map<String, User>,
        val unlockedUsersMap: Map<String, User>,
        val lockedUsersMap: Map<String, User>,
        val accountManager: AccountManager = AccountManager(),
        val accountManagerOptions: AccountManagerOptions = AccountManagerOptions( sqlite = userDbFile.absolutePath )
)