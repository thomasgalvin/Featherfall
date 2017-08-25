package galvin.dw

import java.io.IOException
import java.sql.Connection
import java.sql.PreparedStatement

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// String utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

internal fun isBlank( string: String? ) :Boolean {
    return string == null || string.isBlank();
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
    var (_, statement) = prepareStatement( conn, sql )
    executeAndClose(conn, statement)
}

fun prepareStatement(conn: Connection, sql: String ): ConnectionStatement {
    val statement = conn.prepareStatement(sql)
    return ConnectionStatement(conn, statement)

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