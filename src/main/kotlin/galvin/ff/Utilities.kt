package galvin.ff

import galvin.ff.db.QuietCloser
import galvin.ff.db.ConnectionManager
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import java.io.*
import java.nio.charset.Charset
import java.security.cert.X509Certificate
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.*
import javax.servlet.http.HttpServletRequest


class Utilities

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// String utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun isBlank( string: String? ) :Boolean {
    return string == null || string.isBlank()
}

fun uuid(): String {
    val uuid = UUID.randomUUID().toString()
    return uuid.replace("-", "_")
}

fun neverNull( string: String? ): String{
    return if( string == null ){
        ""
    } else{
        string
    }
}

fun createList(firstEntry: String): MutableList<String> {
    val result = mutableListOf<String>()
    result.add(firstEntry)
    return result
}

fun elseIfNull( one: String?, two: String? ): String{
    if( one == null ) return neverNull(two)
    return one
}

fun loadFromClasspathOrThrow(classpathEntry: String ): String{
    val resource = Utilities::class.java.getResource(classpathEntry) ?: throw IOException( "Unable to load classpath entry: $classpathEntry" )

    val result = resource.readText()
    if( isBlank(result) ) throw IOException("Loaded empty classpath entry: $classpathEntry" )
    return result
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Database utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun executeUpdate(conn: Connection, sql: String ){
    val statement = conn.prepareStatement(sql)
    try{ statement.executeUpdate() }
    finally{ QuietCloser.close(statement) }
}

fun executeUpdateAndClose(statement: PreparedStatement? = null, conn: Connection? = null, rollbackOnError: Boolean = true ){
    if( statement != null ) {
        try{
            statement.executeUpdate()
        }
        catch( t: Throwable ){
            if( rollbackOnError ){ conn?.rollback() }
            throw t
        }
        finally{ QuietCloser.close(statement) }
    }

    if( conn != null ) {
        try{ conn.commit() }
        catch( t: Throwable ){
            if( rollbackOnError ){ conn.rollback() }
        }
        finally{ QuietCloser.close(conn) }
    }
}

fun commitAndClose(conn: Connection, rollbackOnError: Boolean = true){
    try{ conn.commit() }
    catch(t: Throwable){
        if(rollbackOnError){ conn.rollback() }
        throw t
    }
    finally{ QuietCloser.close(conn) }
}

fun rollbackAndRelease(conn: Connection?, connectionManager: ConnectionManager? = null ){
    if( conn != null && !conn.isClosed ){
        try{ conn.rollback() }
        finally{ QuietCloser.close(conn) }
    }

    if( connectionManager != null ){
        connectionManager.release(conn)
    }
}

fun closeAndRelease(conn: Connection?, connectionManager: ConnectionManager? = null ){
    QuietCloser.close(conn)

    if( connectionManager != null ){
        connectionManager.release(conn)
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
/// IO utility code: mostly stolen from Apache Commons IOUtils
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun loadResourceAndReadLines( resource: String ): List<String>{
    val input = Utilities::class.java.classLoader.getResourceAsStream(resource)
    val lines = readLines(input)
    closeQuietly(input)
    return lines
}

fun readLines(stream: InputStream, charset: Charset = Charset.defaultCharset() ): List<String>{
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

fun loadResourceAndReadString( resource: String ): String{
    val builder = StringBuilder()
    val lines = loadResourceAndReadLines(resource)
    for( line in lines ){
        builder.append(line)
        builder.append("\n")
    }
    return builder.toString()
}

fun closeQuietly( stream: Closeable? ){
    try {
        stream?.close()
    }
    catch( t: Throwable ){}
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Date parsing utilities
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun parseToDateTime( string: String,
                     dateTimeFormats: List<DateTimeFormatter> ): DateTime?{
    if( isBlank(string) ) return null

    for( format in dateTimeFormats ){
        try{
            return format.parseDateTime(string)
        }
        catch(t: Throwable){}
    }

    return null
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Authentication utilities
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

const val loginTokenParamName: String = "X-Auth-Token"

fun getIpAddress(httpRequest: HttpServletRequest): String {
    var ipAddress = httpRequest.getHeader( "X-FORWARDED-FOR" )
    if( ipAddress == null || isBlank(ipAddress) ) {
        ipAddress = httpRequest.getRemoteAddr()
    }
    else if( ipAddress.contains( "," ) ) {
        ipAddress = ipAddress.split( "," )[0];
    }
    return ipAddress
}

fun getSerialNumber(x509: X509Certificate?): String {
    if (x509 != null) {
        val result = x509.serialNumber.toString(16)
        if( !isBlank(result) ){
            return result
        }
    }

    return ""
}