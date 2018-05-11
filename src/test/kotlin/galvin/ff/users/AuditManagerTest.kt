package galvin.ff.users

import galvin.ff.AccessType
import galvin.ff.tools.AuditManager
import galvin.ff.tools.AuditManagerOptions
import galvin.ff.tools.dbuser
import galvin.ff.uuid
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test

class AuditManagerTest{
    val unexpectedOptions = "Unexpected options"

    @Test fun parse_args_should_show_verbose(){
        testBooleanOptions( AuditManagerOptions(verbose=true), "-v", "--verbose" )
    }

    @Test fun parse_args_should_show_manual(){
        testBooleanOptions( AuditManagerOptions(showManual=true), "-m", "--man" )
    }

    @Test fun parse_args_should_show_help(){
        testBooleanOptions( AuditManagerOptions(showHelp=true), "-h", "--help" )
    }

    @Test fun parse_args_should_show_deltas(){
        testBooleanOptions( AuditManagerOptions(showDeltas= true), "-d", "--deltas" )
    }

    @Test fun parse_args_should_show_success(){
        testBooleanOptions( AuditManagerOptions(showSuccess= true), "-success" )
    }

    @Test fun parse_args_should_show_failure(){
        testBooleanOptions( AuditManagerOptions(showFailure= true), "-failure" )
    }

    @Test fun parse_args_should_show_system_info(){
        testBooleanOptions( AuditManagerOptions(showSystemInfo= true), "-si", "--system-info" )
    }

    @Test fun should_parse_start_dates(){
        testStartDate( DateTime(2017, 1, 1, 0, 0), "2017", "2017" )
        testStartDate( DateTime(2017, 4, 1, 0, 0), "2017/04", "2017/4" )
        testStartDate( DateTime(2017, 4, 7, 0, 0), "2017/04/07", "2017/4/7" )
        testStartDate( DateTime(2017, 4, 7, 1, 37), "2017/04/07-01:37", "2017/4/7-1:37" )
        testStartDate( DateTime(2017, 4, 7, 1, 37, 55), "2017/04/07-01:37:55", "2017/4/7-1:37:55" )
    }

    @Test fun should_parse_end_dates(){
        testEndDate( DateTime(2017, 1, 1, 0, 0), "2017", "2017" )
        testEndDate( DateTime(2017, 4, 1, 0, 0), "2017/04", "2017/4" )
        testEndDate( DateTime(2017, 4, 7, 0, 0), "2017/04/07", "2017/4/7" )
        testEndDate( DateTime(2017, 4, 7, 1, 37), "2017/04/07-01:37", "2017/4/7-1:37" )
        testEndDate( DateTime(2017, 4, 7, 1, 37, 55), "2017/04/07-01:37:55", "2017/4/7-1:37:55" )
    }

    @Test fun should_parse_user_name(){
        testUsername("thomas")
    }

    @Test fun should_parse_sqlite_filepath(){
        val filepath = "target/" + uuid()
        val expected = AuditManagerOptions(sqliteAuditDB = filepath)
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-sqlite", filepath ) ) )
    }

    @Test fun should_parse_psql_connection_url(){
        val connectionURL = "jdbc:postgresql://localhost:5432/" + uuid()
        val expected = AuditManagerOptions(psqlAuditDB = connectionURL)
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-psql", connectionURL ) ) )
    }

    @Test fun should_parse_sqlite_userdb_filepath(){
        val filepath = "target/" + uuid()
        val expected = AuditManagerOptions(sqliteUserdb = filepath)
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-userdbsqlite", filepath ) ) )
    }

    @Test fun should_parse_psql_userdb_connection_url(){
        val connectionURL = "jdbc:postgresql://localhost:5432/" + uuid()
        val expected = AuditManagerOptions(psqlUserdb = connectionURL)
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-userdbpsql", connectionURL ) ) )
    }

    @Test fun should_parse_access_type(){
        for( type in AccessType.values() ){
            testAccessType(type)
        }
    }

    @Test fun should_parse_db_user_and_pass(){
        val user = uuid()
        val pass = uuid()
        val expected = AuditManagerOptions(auditUser = user, auditPass = pass)
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-dbuser", user, "-dbpass", pass ) ) )
    }

    @Test fun should_parse_userdb_user_and_pass(){
        val user = uuid()
        val pass = uuid()
        val expected = AuditManagerOptions(usersUser = user, usersPass = pass)
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-useruser", user, "-userpass", pass ) ) )
    }

    ///
    /// utilities
    ///

    private fun testBooleanOptions(expected: AuditManagerOptions,
                                   short: String,
                                   long: String? = null ){
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( short ) ) )
        if( long != null ){
            Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( long ) ) )
        }
    }

    private fun testStartDate( date: DateTime, vararg args: String ){
        val expected = AuditManagerOptions(start=date)
        val audit = AuditManager()
        for( arg in args ){
            Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-s", arg ) ) )
            Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "--start", arg ) ) )
        }
    }

    private fun testEndDate( date: DateTime, vararg args: String ){
        val expected = AuditManagerOptions(end=date)
        val audit = AuditManager()
        for( arg in args ){
            Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-e", arg ) ) )
            Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "--end", arg ) ) )
        }
    }

    private fun testUsername(username: String){
        val expected = AuditManagerOptions(username = username)
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-u", username ) ) )
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "--user", username ) ) )
    }

    private fun testAccessType( type: AccessType ){
        val name = type.name
        val expected = AuditManagerOptions(accessType = name)
        val audit = AuditManager()
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "-a", name ) ) )
        Assert.assertEquals(unexpectedOptions, expected, audit.parse( arrayOf( "--access", name ) ) )
    }
}