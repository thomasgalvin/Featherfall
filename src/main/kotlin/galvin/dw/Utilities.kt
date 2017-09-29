package galvin.dw

import java.io.File
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.*

class Utilities{}

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
    val resource = Utilities::class.java.getResource(classpathEntry)
    if(resource == null) throw IOException( "Unable to load SQL: $classpathEntry" )

    val sql = resource.readText()
    if( isBlank(sql) ) throw IOException( "Loaded empty SQL: $classpathEntry" )

    return sql
}

fun runSql(conn: Connection, sql: String ){
    val statement = conn.prepareStatement(sql)
    executeAndClose(conn, statement)
}

fun executeAndClose(conn: Connection? = null, statement: PreparedStatement){
    statement.executeUpdate()
    statement.close()

    if( conn != null ) {
        conn.commit()
        conn.close()
    }
}

fun close(conn: Connection, statement: PreparedStatement){
    statement.close()
    conn.close()
}

