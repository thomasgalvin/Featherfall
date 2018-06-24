package galvin.ff.users

import galvin.ff.*
import galvin.ff.db.ConnectionManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///
/// DB connections
///
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

val databases = listOf(
        SqliteDbConnection(),
        PsqlDbConnection()
)

interface DbConnection{
    fun canConnect(): Boolean
    fun randomAuditDB() : AuditDB
    fun randomUserDB(): UserDB
    fun randomAccountRequestDB(userDB: UserDB ): AccountRequestDB
    fun randomLoginTokenManager(): LoginTokenManager

    fun userDbConnection(): UserDbConnection
    fun auditDbConnection(): AuditDbConnection

    fun cleanup()
}

data class UserDbConnection(val userDB: UserDB, val connectionURL: String, val userName: String? = null, val password: String? = null)
data class AuditDbConnection(val auditDB: AuditDB, val connectionURL: String, val userName: String? = null, val password: String? = null)

class SqliteDbConnection: DbConnection{
    private val prefix = "featherfall_unit_test_"

    override fun canConnect(): Boolean = true

    override fun randomUserDB(): UserDB = UserDB.SQLite( 1, randomDbFile() )

    override fun randomAccountRequestDB(userDB: UserDB ): AccountRequestDB  = AccountRequestDB.SQLite( userDB, 1, randomDbFile(), randomDbFile() )

    override fun randomAuditDB() : AuditDB  = AuditDB.SQLite( maxConnections = 1, databaseFile = randomDbFile() )

    override fun randomLoginTokenManager(): LoginTokenManager = LoginTokenManager.SQLite( maxConnections = 1, databaseFile = randomDbFile() )

    override fun userDbConnection(): UserDbConnection{
        val file = randomDbFile()
        val userDB = UserDB.SQLite(databaseFile = file, maxConnections = 1)
        return UserDbConnection(userDB = userDB, connectionURL = file.absolutePath)
    }

    override fun auditDbConnection(): AuditDbConnection{
        val file = randomDbFile()
        val auditDB = AuditDB.SQLite(databaseFile = file, maxConnections = 1)
        return AuditDbConnection(auditDB = auditDB, connectionURL = file.absolutePath)
    }

    override fun cleanup(){}

    fun randomDbFile(): File = File( "target/$prefix${uuid()}.dat" )
}

class PsqlDbConnection: DbConnection{
    private val prefix = "featherfall_unit_test_"
    private var connectionAttempted = false
    private var canConnect = false
    private val maxConnections = 1
    private val username = System.getProperty("user.name")
    private val password: String? = null

    override fun canConnect(): Boolean{
        if( connectionAttempted ) return canConnect
        connectionAttempted = true

        try{
            createRandom()
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

    override fun randomAuditDB() : AuditDB {
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/" + dbName
        return AuditDB.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
    }

    override fun randomUserDB(): UserDB{
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/$dbName"
        return UserDB.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
    }

    override fun randomLoginTokenManager(): LoginTokenManager{
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/$dbName"
        return LoginTokenManager.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
    }

    override fun randomAccountRequestDB(userDB: UserDB ): AccountRequestDB {
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/" + dbName
        return AccountRequestDB.PostgreSQL( userDB = userDB, maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password )
    }

    override fun userDbConnection(): UserDbConnection{
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/$dbName"
        val userDB = UserDB.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
        return UserDbConnection(userDB = userDB, connectionURL = connectionURL)
    }

    override fun auditDbConnection(): AuditDbConnection{
        val dbName = createRandom()
        val connectionURL = "jdbc:postgresql://localhost:5432/$dbName"
        val auditDB = AuditDB.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
        return AuditDbConnection(auditDB = auditDB, connectionURL = connectionURL, userName = username, password = password)
    }

    fun createRandom(): String{
        val result = prefix + uuid()
        val connectionURL = "jdbc:postgresql://localhost:5432/"
        val createSQL = "CREATE DATABASE $result;"
        val grantSQL = "GRANT ALL PRIVILEGES ON DATABASE $result to $username;"

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

    override fun cleanup(){
        if( !canConnect() ) return

        val databaseNames = mutableListOf<String>()

        val connectionURL = "jdbc:postgresql://localhost:5432/"
        val connectionManager = ConnectionManager.PostgreSQL(maxConnections = maxConnections, connectionURL = connectionURL, username = username, password = password)
        val conn = connectionManager.connect()
        conn.autoCommit = true

        try {
            val sql = "SELECT datname FROM pg_database WHERE datistemplate = false;"
            val statement = conn.prepareStatement(sql)
            val resultSet = statement.executeQuery()
            if( resultSet != null ){
                while (resultSet.next()) {
                    val db = resultSet.getString(1)
                    if( db.startsWith(prefix) ) {
                        databaseNames.add(db)
                    }
                }
            }
            resultSet.close()
            statement.close()

            for (db in databaseNames) {
                try{
                    val dropSQL = "DROP DATABASE $db;"
                    val dropStatement = conn.prepareStatement(dropSQL)
                    dropStatement.executeUpdate()
                    dropStatement.close()
                }
                catch(t:Throwable){
                    t.printStackTrace()
                }
            }
        }
        finally {
            connectionManager.release(conn)
            databaseNames.clear()
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///
/// Test data generation
///
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

fun generateUser(systemRoles: List<Role>,
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