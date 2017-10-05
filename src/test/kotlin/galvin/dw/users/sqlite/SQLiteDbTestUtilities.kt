package galvin.dw.users.sqlite

import galvin.dw.*
import galvin.dw.sqlite.SQLiteAccountRequestDB
import galvin.dw.sqlite.SQLiteAuditDB
import galvin.dw.sqlite.SQLiteUserDB
import java.io.File
import java.util.*

fun randomDbFile(): File {
    return File( "target/" + uuid() + ".dat" )
}

fun randomUserDB(): UserDB{
    return SQLiteUserDB( randomDbFile() )
}

fun randomAccountRequestDB(userDB: UserDB ): AccountRequestDB {
    return SQLiteAccountRequestDB( randomDbFile(), userDB )
}

fun randomAuditDB() : AuditDB {
    return SQLiteAuditDB( randomDbFile() )
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

fun generateUser( systemRoles: List<Role>, uuid: String = uuid(), password: String = uuid() ): User {
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
            true,
            false,
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
    val isActive = if(active == null) random.nextBoolean() else active

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
            "version" + uuid(),
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