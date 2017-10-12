package galvin.dw.tools

import galvin.dw.User
import galvin.dw.UserDB
import galvin.dw.isBlank
import galvin.dw.sqlite.SQLiteUserDB
import java.io.File

class AccountManager{
    fun retrieveAllUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsers()
    }

    fun retrieveLockedUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsersByLocked(true)
    }

    fun retrieveUnlockedUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsersByLocked(false)
    }

    fun retrieveInactiveUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsersByActive(false)
    }

    fun retrieveActiveUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsersByActive(true)
    }

    private fun connectUserDB(options: AccountManagerOptions): UserDB {
        if( isBlank(options.sqlite) ){
            throw Exception("Unable to connect to user DB: no filepath specified")
        }

        val file = File(options.sqlite)
        if( !file.exists() ){
            throw Exception( "Unable to connect to user DB: ${file.absolutePath} does not exist" )
        }
        else if( !file.canRead() ){
            throw Exception( "Unable to connect to user DB: ${file.absolutePath} cannot be read" )
        }

        return SQLiteUserDB(file)
    }
}

data class AccountManagerOptions(
        val verbose: Boolean = false,
        val showHelp: Boolean = false,
        val showManual: Boolean = false,
        val sqlite: String = ""
)