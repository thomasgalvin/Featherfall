package galvin.dw

import java.io.File
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.*

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// String utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun isBlank( string: String? ) :Boolean {
    return string == null || string.isBlank();
}

fun uuid(): String {
    return UUID.randomUUID().toString()
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Database utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

data class ConnectionStatement(val connection: Connection, val statement: PreparedStatement)

fun loadSql( classpathEntry: String ): String{
    val resource = SQLiteAuditDB::class.java.getResource(classpathEntry)
    if(resource == null) throw IOException( "Unable to load SQL: $classpathEntry" )

    val sql = resource.readText()
    if( isBlank(sql) ) throw IOException( "Loaded empty SQL: $classpathEntry" )

    return sql
}

fun runSql(conn: Connection, sql: String ){
    val statement = conn.prepareStatement(sql)
    executeAndClose(conn, statement)
}

fun executeAndClose(conn: Connection, statement: PreparedStatement){
    statement.executeUpdate()
    statement.close()

    conn.commit()
    conn.close()
}

fun close(conn: Connection, statement: PreparedStatement){
    statement.close()
    conn.close()
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
/// SQLite utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

open class SQLiteDB( private val databaseFile: File){
    private val connectionUrl: String = "jdbc:sqlite:" + databaseFile.absolutePath

    init{
        //load driver
        Class.forName( "org.sqlite.JDBC" )
    }

    /**
     * Returns a connection to the current SQLite database (specified in constructor), with
     * auto-commit disabled
     */
    fun conn(): Connection{
        val result = DriverManager.getConnection( connectionUrl )
        result.autoCommit = false
        return result
    }
}