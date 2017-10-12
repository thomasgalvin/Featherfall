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
        val objects = AccountManagerQueryTestObjects()
        val users = objects.accountManager.retrieveAllUsers( options = objects.accountManagerOptions )
        Assert.assertEquals("Unexpected user count", objects.users.size, users.size)
    }
}

data class AccountManagerQueryTestObjects(
        val userDbFile: File = randomDbFile(),
        val userDB: UserDB = SQLiteUserDB(userDbFile),
        val roles: List<Role> = generateRoles(5),
        val userCount: Int = 10,
        val users: List<User> = generateUsers(roles, userCount),
        val map: MutableMap<String, User> = mutableMapOf(),
        val accountManager: AccountManager = AccountManager(),
        val accountManagerOptions: AccountManagerOptions = AccountManagerOptions( sqlite = userDbFile.absolutePath )
){
    init{
        //println("UserDB File: ${userDbFile.absolutePath}")
        for( user in users ){
            userDB.storeUser(user)
            map[user.uuid] = user
        }
    }
}