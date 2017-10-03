package galvin.dw

import java.io.IOException
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.*
import java.security.cert.X509Certificate
import org.mindrot.jbcrypt.BCrypt;

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
    executeAndClose(statement, conn)
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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
/// Password hashing utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun validate( password: String?, hash: String ): Boolean{
    if( password == null || isBlank(password) ){
        return false
    }
    return BCrypt.checkpw( password, hash )
}

fun hash( password: String): String{
    return BCrypt.hashpw( password, BCrypt.gensalt() )
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
/// X509 Certificate utility code
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun getSerialNumber(x509Certificate: X509Certificate): String {
    return x509Certificate.serialNumber.toString(16)

}