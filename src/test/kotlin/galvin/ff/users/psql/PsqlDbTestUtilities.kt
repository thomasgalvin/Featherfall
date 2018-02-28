package galvin.ff.users.psql

import galvin.ff.*
import galvin.ff.db.ConnectionManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*

object PSQL{
    private val databaseNames = mutableListOf<String>()
    private var connectionAttempted = false
    private var canConnect = false
    private val maxConnections = 1
    private val username = System.getProperty("user.name")
    private val password: String? = null


    fun canConnect(): Boolean{
        if( connectionAttempted ) return canConnect
        connectionAttempted = true

        try{
            val db = createRandom()
            canConnect = true
        }
        catch(t: Throwable){
            println("***********************************************************************");
            println("***                                                                 ***");
            println("*** Unable to connect to PostgreSQL: will not run psql test harness ***");
            println("***                                                                 ***");
            println("***********************************************************************");
            //t.printStackTrace()
            canConnect = false
        }


        return canConnect
    }

    fun randomAuditDB() : AuditDB {
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/" + dbName
        return AuditDB.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
    }

    fun randomUserDB(): UserDB{
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/" + dbName
        return UserDB.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
    }

    fun randomAccountRequestDB(userDB: UserDB ): AccountRequestDB {
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/" + dbName
        return AccountRequestDB.PostgreSQL( userDB = userDB, maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password )
    }

    fun cleanup(){
        val connectionURL = "jdbc:postgresql://localhost:5432/"
        val connectionManager = ConnectionManager.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
        val conn = connectionManager.connect()
        conn.autoCommit = true

        try {
            for (db in databaseNames) {
                val sql = "DROP DATABASE $db;"
                val statement = conn.prepareStatement(sql)
                statement.executeUpdate()
                statement.close()
            }
        }
        finally {
            connectionManager.release(conn)
            databaseNames.clear()
        }
    }

    private fun createRandom(): String{
        val result = "unit_test_" + uuid()
        val connectionURL = "jdbc:postgresql://localhost:5432/"
        val createSQL = "CREATE DATABASE $result;"
        val grantSQL = "GRANT ALL PRIVILEGES ON DATABASE $result to $username;"
        databaseNames.add(result)

        val connectionManager = ConnectionManager.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
        val conn = connectionManager.connect()
        conn.autoCommit = true
        try {
            val createStatement = conn.prepareStatement(createSQL)
            createStatement.executeUpdate()
            createStatement.close()

            val grantStatement = conn.prepareStatement(grantSQL)
            grantStatement.executeUpdate()
            grantStatement.close()

            conn.close()
        }
        finally{
            connectionManager.release(conn)
        }


        return result
    }
}



//fun randomUserDB(): UserDB{
//    return UserDB.SQLite( 1, randomDbFile() )
//}

//fun randomAccountRequestDB(userDB: UserDB ): AccountRequestDB {
//    val userDB = randomUserDB()
//    return AccountRequestDB.SQLite( userDB, 1, randomDbFile(), randomDbFile() )
//}

//fun randomAuditDB() : AuditDB {
//    return AuditDB.SQLite( maxConnections = 1, databaseFile = randomDbFile() )
//}

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
