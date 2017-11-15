package galvin.ff.tools

import galvin.ff.*
import galvin.ff.PadTo.paddedLayout
import galvin.ff.sqlite.SQLiteUserDB
import java.io.File
import java.io.PrintStream
import java.util.stream.Collectors

class AccountManager{
    fun printShort( users: List<User>, out: PrintStream = System.out  ){
        val logins = createList( "Login" )
        val legal = createList( "Legal Name" )
        val active = createList( "Active" )
        val locked = createList( "Locked" )
        val roles = createList( "Roles" )

        for( user in users ){
            logins.add( user.login )
            legal.add( user.sortName )
            active.add( if(user.active) "Active" else "Inactive" )
            locked.add( if(user.locked) "Locked" else "" )
            roles.add( user.roles.stream().collect(Collectors.joining(", ")) )
        }

        val table = paddedLayout( '-', logins, legal, active, locked, roles )
        out.println(table)
    }

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

    fun updateCredentialsFromFile( options: AccountManagerOptions, login: String, file: File ){
        if( !file.exists() || !file.canRead() ){
            println("Unable to read file: ${file.absolutePath}")
            return
        }

        val x509 = loadCertificateFromFile(file)
        val certData = parsePKI(x509)

        val userDB = connectUserDB(options)
        val uuid = userDB.retrieveUuidByLogin(login)
        if( uuid != null && !isBlank(uuid) ){
            userDB.updateCredentials(uuid, certData)
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