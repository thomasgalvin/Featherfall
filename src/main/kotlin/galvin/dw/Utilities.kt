package galvin.dw

import java.io.*
import java.nio.charset.Charset
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.*


class Utilities{}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// String utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun isBlank( string: String? ) :Boolean {
    return string == null || string.isBlank()
}

fun uuid(): String {
    return UUID.randomUUID().toString()
}

fun neverNull( string: String? ): String{
    if( string == null ){
        return ""
    }
    else{
        return string
    }
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

fun executeUpdate(conn: Connection, sql: String ){
    val statement = conn.prepareStatement(sql)
    statement.executeUpdate()
}

fun executeAndClose(statement: PreparedStatement? = null, conn: Connection? = null){
    if( statement != null ) {
        statement.executeUpdate()
        statement.close()
    }

    if( conn != null ) {
        conn.commit()
        conn.close()
    }
}

fun commitAndClose(conn: Connection){
    conn.commit()
    conn.close()
}

fun close( conn: Connection, statement: PreparedStatement){
    statement.close()
    conn.close()
}

/**
 * If the connection is not null and is still open,
 * calls rollback() and then close()
 */
fun rollbackAndClose( conn: Connection? ){
    if( conn != null && !conn.isClosed ){
        conn.rollback()
        conn.close()
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
/// IO utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun loadResourceAndReadLines( resource: String ): List<String>{
    val input = Utilities::class.java.classLoader.getResourceAsStream(resource)
    val lines = readlines(input)
    closeQuietly(input)
    return lines
}

fun readlines( stream: InputStream, charset: Charset = Charset.defaultCharset() ): List<String>{
    val r = InputStreamReader( stream, charset )
    val reader = BufferedReader(r)
    val list = mutableListOf<String>()

    var line = reader.readLine()
    while( line != null ){
        list.add(line)
        line = reader.readLine()
    }

    closeQuietly(reader)
    closeQuietly(r)
    closeQuietly(stream)
    return list
}

fun closeQuietly( stream: Closeable? ){
    try {
        if (stream != null) {
            stream.close()
        }
    }
    catch( t: Throwable ){}
}

