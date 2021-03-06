package galvin.ff.tools

import galvin.ff.*
import galvin.ff.PadTo.paddedLayout
import org.joda.time.format.DateTimeFormat
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

    fun printLong( users: List<User>, out: PrintStream = System.out ){
        val dateTimeFormat = DateTimeFormat.forPattern("yyyy/MM/dd kk:mm")
        val text = StringBuilder()
        for( (index,user) in users.withIndex() ){
            val created = dateTimeFormat.print(user.created)

            text.append( "Sort By Name:       " )
            text.append( user.sortName )

            text.append("\n")
            text.append( "Login:              " )
            text.append( user.login )

            text.append("\n")
            text.append( "Name:               " )
            text.append( user.name )

            text.append("\n")
            text.append( "Display Name:       " )
            text.append( user.displayName )

            text.append("\n")
            text.append( "Prepend to Name:    " )
            text.append( user.prependToName )

            text.append("\n")
            text.append( "Append to Name:     " )
            text.append( user.appendToName )

            text.append("\n")
            text.append( "Credential:         " )
            text.append( user.credential )

            text.append("\n")
            text.append( "Serial Number:      " )
            text.append( user.serialNumber )

            text.append("\n")
            text.append( "Distinguished Name: " )
            text.append( user.distinguishedName )

            text.append("\n")
            text.append( "Agency:             " )
            text.append( user.agency )

            text.append("\n")
            text.append( "Country Code:       " )
            text.append( user.countryCode )

            text.append("\n")
            text.append( "Citizenship:        " )
            text.append( user.citizenship )

            text.append("\n")
            text.append( "Created On:         " )
            text.append( created )

            text.append("\n")
            text.append( "Active:             " )
            text.append( if(user.active){ "Active" } else{ "Inactive" } )

            text.append("\n")
            text.append( "Locked:             " )
            text.append( if(user.locked){ "Locked" } else{ "Unlocked" } )

            text.append("\n")
            text.append( "Contact Info:\n" )
            for( contact in user.contact ){
                text.append( "    - " )
                text.append( contact.contact )

                if( !isBlank(contact.description) ) {
                    text.append(" (")
                    text.append(contact.description)
                    text.append(")")
                }

                if( contact.primary ){
                    text.append(" * Primary")
                }
                text.append("\n")
            }


            text.append( "Roles:\n" )
            for( roleName in user.roles){
                text.append( "    - " )
                text.append( roleName )
                text.append("\n")
            }

            if( index < users.size-1) {
                text.append("------------------------------\n")
            }
        }

        out.println( text.toString().trim() )
    }

    fun retrieveAllUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsers()
    }

    // locked / unlocked users

    fun retrieveLockedUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsersByLocked(true)
    }

    fun retrieveUnlockedUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsersByLocked(false)
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

    // active / inactive users

    fun retrieveInactiveUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsersByActive(false)
    }

    fun retrieveActiveUsers(options: AccountManagerOptions): List<User>{
        val userDB = connectUserDB(options)
        return userDB.retrieveUsersByActive(true)
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

    // passwords and credentials

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
            if( current != null ) {
                val updateCredentials = CertificateData(
                        credential = elseIfNull(credential, current.credential),
                        serialNumber = elseIfNull(serialNumber, current.serialNumber),
                        distinguishedName = elseIfNull(distinguishedName, current.distinguishedName),
                        countryCode = elseIfNull(countryCode, current.countryCode),
                        citizenship = elseIfNull(citizenship, current.citizenship)
                )
                userDB.updateCredentials(uuid, updateCredentials)
            }
            else{
                println("No such user: $login")
            }
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

    // roles

    fun printRoles( options: AccountManagerOptions, out: PrintStream = System.out ){
        val userDB = connectUserDB(options)
        val roles = userDB.listRoles()

        val text = StringBuilder()
        for( role in roles ){
            text.append( role.name )
            text.append( " (" )
            text.append( if(role.active){ "Active" } else{ "Inactive" } )
            text.append( ")\n" )

            for( permission in role.permissions ){
                text.append("    - ")
                text.append( permission )
                text.append("\n")
            }
        }

        out.println( text.toString().trim() )
    }

    fun createRoles( options: AccountManagerOptions, roleNames: List<String>, permissions: List<String>  ){
        val userDB = connectUserDB(options)
        for( roleName in roleNames ) {
            val role = userDB.retrieveRole(roleName)
            if( role == null ){
                userDB.storeRole( Role( name = roleName, permissions = permissions ) )
            }
        }
    }

    fun addPermissions(options: AccountManagerOptions, roleNames: List<String>, addedPermissions: List<String> ){
        val userDB = connectUserDB(options)
        for( roleName in roleNames ) {
            val role = userDB.retrieveRole(roleName)
            if( role != null ){
                val newPermissions = mutableListOf<String>()
                newPermissions.addAll( role.permissions )
                for( permission in addedPermissions){
                    if( !newPermissions.contains(permission) ){
                        newPermissions.add(permission)
                    }
                }
                userDB.storeRole( role.copy(permissions = newPermissions) )
            }
        }
    }

    fun removePermissions(options: AccountManagerOptions, roleNames: List<String>, deletedPermissions: List<String> ){
        val userDB = connectUserDB(options)
        for( roleName in roleNames ) {
            val role = userDB.retrieveRole(roleName)
            if( role != null ){
                val newPermissions = mutableListOf<String>()
                for( permission in role.permissions ){
                    if( !deletedPermissions.contains(permission) ){
                        newPermissions.add(permission)
                    }
                }
                userDB.storeRole( role.copy(permissions = newPermissions) )
            }
        }
    }

    fun activateRoles(options: AccountManagerOptions, roleNames: List<String> ){
        val userDB = connectUserDB(options)
        for( roleName in roleNames ) {
            userDB.activate(roleName)
        }
    }

    fun deactivateRoles(options: AccountManagerOptions, roleNames: List<String> ){
        val userDB = connectUserDB(options)
        for( roleName in roleNames ) {
            userDB.deactivate(roleName)
        }
    }

    //
    // utilities
    //

    private fun connectUserDB(options: AccountManagerOptions): UserDB {
        if( options.userDB != null ) return options.userDB

        if( !isBlank(options.sqlite) ){
            val file = File(options.sqlite)
            return UserDB.SQLite(maxConnections = options.maxConnections, databaseFile = file )
        }
        else if( !isBlank(options.psql) ){
            return UserDB.PostgreSQL(maxConnections = options.maxConnections, connectionURL = options.psql, username = options.dbuser, password = options.dbpass )
        }

        throw Exception("Unable to connect to user DB: no connection criteria specified")
    }
}

data class AccountManagerOptions(
        val verbose: Boolean = false,
        val showHelp: Boolean = false,
        val showManual: Boolean = false,
        val maxConnections: Int = 10,
        val timeout: Long = 60_000,
        val userDB: UserDB? = null,
        val sqlite: String = "",
        val psql: String = "",
        val dbuser: String? = null,
        val dbpass: String? = null
)