package galvin.ff.db

import galvin.ff.DatabaseError
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class ConnectionManager(val maxConnections: Int,
                        val connectionURL: String,
                        driverName: String,
                        val timeout: Long = 60_000,
                        val user: String? = null,
                        val password: String? = null ) {
    private val logger = LoggerFactory.getLogger(ConnectionManager::class.java)
    private val lock = Object()
    private var connectionCount = 0

    init{
        try {
            Class.forName(driverName)
        } catch (t: Throwable) {
            logger.debug(t.message, t)
            throw DatabaseError("Error creating JDBC driver: " + driverName, t)
        }

    }

    companion object {
        fun SQLite( maxConnections: Int, databaseFile: File, timeout: Long = 60_000): ConnectionManager{
            val connectionURL = "jdbc:sqlite:" + databaseFile.absolutePath
            return ConnectionManager(maxConnections, connectionURL, "org.sqlite.JDBC", timeout)
        }

        fun PostgreSQL( maxConnections: Int, connectionURL: String, timeout: Long = 60_000, username: String? = null, password: String? = null ) = ConnectionManager(maxConnections, connectionURL, "org.postgresql.Driver", timeout, username, password)
    }

    fun execute( sql: String ){
        val conn = connect()
        conn.autoCommit = true
        try {
            val statement = conn.prepareStatement(sql)
            statement.executeUpdate()
            statement.close()
            conn.close()
        }
        finally{
            release(conn)
        }
    }

    fun connect(): Connection {
        try {
            incrementCount()
            waitForAvailableConnection()

            val result = DriverManager.getConnection(connectionURL, user, password)
            result.autoCommit = false
            return result
        } catch (t: Throwable) {
            decrementCount()

            logger.debug(t.message, t)
            throw DatabaseError("Error establishing database connection: " + connectionURL, t)
        }

    }

    fun release(conn: Connection?) {
        try {
            if (conn != null && !conn.isClosed) {
                conn.close()
            }
        } catch (t: Throwable) {
            logger.debug(t.message, t)
            throw DatabaseError("Error returning database connection", t)
        } finally {
            decrementCount()
        }
    }

    private fun incrementCount(){
        synchronized(lock) {
            connectionCount++
        }
    }

    private fun decrementCount(){
        synchronized(lock) {
            connectionCount--
            if( connectionCount < 0 ) connectionCount = 0
        }
    }

    private fun waitForAvailableConnection(){
        val sleepFor = 25.toLong()
        var waitTime = 0.toLong()

        while (connectionCount > maxConnections) {
            try {
                if (logger.isTraceEnabled) {
                    logger.trace("Waiting for available connection: max: $maxConnections current: $connectionCount")
                }

                Thread.sleep(sleepFor)
                waitTime += sleepFor
                if( waitTime > timeout ) throw DatabaseError("Timeout waiting for database connection: $connectionURL")
            } catch (t: Throwable) {
                logger.trace(t.message)
            }

        }
    }

}