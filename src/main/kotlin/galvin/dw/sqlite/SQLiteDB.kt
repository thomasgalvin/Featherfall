package galvin.dw.sqlite

import java.io.File
import java.sql.Connection
import java.sql.DriverManager


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