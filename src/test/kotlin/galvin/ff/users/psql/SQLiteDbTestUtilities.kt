package galvin.ff.users.sqlite

import galvin.ff.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*

fun randomDbFile(): File {
    return File( "target/" + uuid() + ".dat" )
}

fun randomUserDB(): UserDB{
    return UserDB.SQLite( 1, randomDbFile() )
}

fun randomAccountRequestDB(userDB: UserDB ): AccountRequestDB {
    val userDB = randomUserDB()
    return AccountRequestDB.SQLite( userDB, 1, randomDbFile(), randomDbFile() )
}

fun randomAuditDB() : AuditDB {
    return AuditDB.SQLite( maxConnections = 1, databaseFile = randomDbFile() )
}

fun generateAccountRequest(systemRoles: List<Role>, uuid: String = uuid(), password: String = uuid(), confirmPassword: String = password ) : AccountRequest {
    return AccountRequest(
            generateUser(systemRoles, uuid),
            password,
            confirmPassword,
            "reason" + uuid(),
            "vouchName" + uuid(),
            "vouchContactInfo" + uuid(),
            false, "", -1,
            false, "", -1,
            "rejectedReason" + uuid(),
            uuid
    )
}

fun generateUser( systemRoles: List<Role>,
                  uuid: String = uuid(), password: String = uuid(),
                  active: Boolean = true, locked: Boolean = false ): User {
    val contact = generateContactInfo()

    val userRoles = mutableListOf<String>()
    for( role in systemRoles.subList(3, 5) ){
        userRoles.add( role.name )
    }

    val passwordHash = hash(password)

    return User(
            "login:" + uuid(),
            passwordHash,
            "name:" + uuid(),
            "displayName:" + uuid(),
            "sortName:" + uuid(),
            "prependToName:" + uuid(),
            "appendToName:" + uuid(),
            "credential:" + uuid(),
            "serialNumber:" + uuid(),
            "distinguishedName:" + uuid(),
            "homeAgency:" + uuid(),
            "agency:" + uuid(),
            "countryCode:" + uuid(),
            "citizenship:" + uuid(),
            System.currentTimeMillis(),
            active,
            locked,
            uuid,
            contact,
            userRoles
    )
}

fun generateContactInfo(): List<ContactInfo>{
    val result = mutableListOf<ContactInfo>()
    for( i in 1..5 ){
        result.add(
                ContactInfo(
                        "type:" + uuid(),
                        "description:" + uuid(),
                        "contact:" + uuid(),
                        i == 1
                )
        )
    }
    return result
}

fun generateRoles( count: Int = 10, userdb: UserDB? = null ): List<Role>  {
    val result = mutableListOf<Role>()

    for( i in 1..count ){
        result.add( generateRole(count) )
    }

    if( userdb != null ){
        for( role in result ){
            userdb.storeRole(role)
        }
    }

    return result
}

fun generateRole( permissionCount: Int = 10, active: Boolean? = null, name: String = "name:" + uuid() ): Role {
    val random = Random()
    val isActive = active ?: random.nextBoolean()

    val permissions = mutableListOf<String>()

    for (i in 1..permissionCount) {
        for (j in 1..permissionCount) {
            permissions.add("permission:" + uuid())
        }
    }

    return Role(
            name,
            permissions,
            isActive
    )
}

fun randomSystemInfo(): SystemInfo {
    return SystemInfo(
            "serial:" + uuid(),
            "name:" + uuid(),
            "version:" + uuid(),
            "Unclassified-" + uuid(),
            "guide:" + uuid(),
            Arrays.asList(
                    uuid(),
                    uuid(),
                    uuid(),
                    uuid(),
                    uuid()
            ),
            "uuid:" + uuid()
    )
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Console Grabber
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

object ConsoleGrabber {
    private val original = System.out
    private val bytes = ByteArrayOutputStream()
    private val printStream = PrintStream(bytes)

    fun grabConsole() {
        bytes.reset()
        System.setOut(printStream)
    }

    fun releaseConsole(print: Boolean = true): String {
        printStream.flush()
        System.setOut(original)

        val result = bytes.toString()
        if(print){ println(result) }
        return result
    }
}