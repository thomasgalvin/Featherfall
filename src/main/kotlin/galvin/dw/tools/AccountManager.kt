package galvin.dw.tools

import galvin.dw.*
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

    fun lockUser( options: AccountManagerOptions, login: String ){
        val userDB = connectUserDB(options)
        val uuid = userDB.retrieveUuidByLogin(login)
        if( uuid != null && !isBlank(uuid) ){
            userDB.setLocked(uuid, true)

            if( options.verbose ){
                println("User locked: $login")
            }
        }
        else{
            println("No such user: $login")
        }
    }

    fun unlockUser( options: AccountManagerOptions, login: String ){
        val userDB = connectUserDB(options)
        val uuid = userDB.retrieveUuidByLogin(login)
        if( uuid != null && !isBlank(uuid) ){
            userDB.setLocked(uuid, false)

            if( options.verbose ){
                println("User locked: $login")
            }
        }
        else{
            println("No such user: $login")
        }
    }

    fun deactivateUser( options: AccountManagerOptions, login: String ){
        val userDB = connectUserDB(options)
        val uuid = userDB.retrieveUuidByLogin(login)
        if( uuid != null && !isBlank(uuid) ){
            userDB.setActive(uuid, false)

            if( options.verbose ){
                println("User deactivated: $login")
            }
        }
        else{
            println("No such user: $login")
        }
    }

    fun activateUser( options: AccountManagerOptions, login: String ){
        val userDB = connectUserDB(options)
        val uuid = userDB.retrieveUuidByLogin(login)
        if( uuid != null && !isBlank(uuid) ){
            userDB.setActive(uuid, true)

            if( options.verbose ){
                println("User deactivated: $login")
            }
        }
        else{
            println("No such user: $login")
        }
    }

    fun updatePassword( options: AccountManagerOptions, login: String, plainTextPassword: String ){
        val userDB = connectUserDB(options)
        userDB.setPasswordByLogin(login, plainTextPassword)
    }

    fun updateCredentials( options: AccountManagerOptions,
                           login: String,
                           credential: String? = null,
                           serialNumber: String? = null,
                           distinguishedName: String? = null,
                           countryCode: String? = null,
                           citizenship: String? = null ){
        val userDB = connectUserDB(options)
        val uuid = userDB.retrieveUuidByLogin(login)
        if( uuid != null && !isBlank(uuid) ){
            val current = userDB.retrieveCredentials(uuid)
            val updateCredentials = CertificateData(
                    credential = elseIfNull( credential, current?.credential ),
                    serialNumber = elseIfNull( serialNumber, current?.serialNumber ),
                    distinguishedName = elseIfNull( distinguishedName, current?.distinguishedName),
                    countryCode = elseIfNull( countryCode, current?.countryCode ),
                    citizenship = elseIfNull( citizenship, current?.citizenship )
            )
            userDB.updateCredentials(uuid, updateCredentials)
        }
        else{
            println("No such user: $login")
        }
    }

    //
    // utilities
    //

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