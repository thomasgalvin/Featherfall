package galvin.dw.users.sqlite

import galvin.dw.tools.AuditManager
import galvin.dw.tools.AuditManagerOptions
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test

class AuditManagerTest{
    val UNEXPECTED_OPTIONS = "Unexpected options"

    @Test
    fun parse_args_should_show_verbose(){
        testBooleanOptions( AuditManagerOptions(verbose=true), "-v", "--verbose" )
    }

    @Test
    fun parse_args_should_show_manual(){
        testBooleanOptions( AuditManagerOptions(showManual=true), "-m", "--man" )
    }

    @Test
    fun parse_args_should_show_help(){
        testBooleanOptions( AuditManagerOptions(showHelp=true), "-h", "--help" )
    }

    @Test
    fun parse_args_should_show_deltas(){
        testBooleanOptions( AuditManagerOptions(showDeltas= true), "-d", "--deltas" )
    }

    @Test
    fun parse_args_should_show_success(){
        testBooleanOptions( AuditManagerOptions(showSuccess= true), "-success" )
    }

    @Test
    fun parse_args_should_show_failure(){
        testBooleanOptions( AuditManagerOptions(showFailure= true), "-failure" )
    }

    @Test
    fun parse_args_should_show_system_info(){
        testBooleanOptions( AuditManagerOptions(showSystemInfo= true), "-si", "--system-info" )
    }

    @Test
    fun should_parse_start_dates(){
        testStartDate( DateTime(2017, 4, 7, 0, 0), "2017/04/07", "2017/4/7" )
        testStartDate( DateTime(2017, 4, 7, 1, 37), "2017/04/07-01:37", "2017/4/7-1:37" )
        testStartDate( DateTime(2017, 4, 7, 1, 37, 55), "2017/04/07-01:37:55", "2017/4/7-1:37:55" )
    }

    @Test
    fun should_parse_end_dates(){
        testEndDate( DateTime(2017, 4, 7, 0, 0), "2017/04/07", "2017/4/7" )
        testEndDate( DateTime(2017, 4, 7, 1, 37), "2017/04/07-01:37", "2017/4/7-1:37" )
        testEndDate( DateTime(2017, 4, 7, 1, 37, 55), "2017/04/07-01:37:55", "2017/4/7-1:37:55" )
    }

    ///
    /// utilities
    ///

    private fun testBooleanOptions(expected: AuditManagerOptions,
                                   short: String,
                                   long: String? = null ){
        Assert.assertEquals( UNEXPECTED_OPTIONS, expected, AuditManager().parse( arrayOf( short ) ) )
        if( long != null ){
            Assert.assertEquals( UNEXPECTED_OPTIONS, expected, AuditManager().parse( arrayOf( long ) ) )
        }
    }

    private fun testStartDate( date: DateTime, vararg args: String ){
        for( arg in args ){
            Assert.assertEquals( UNEXPECTED_OPTIONS, AuditManagerOptions(start=date), AuditManager().parse( arrayOf( "-s", arg ) ) )
            Assert.assertEquals( UNEXPECTED_OPTIONS, AuditManagerOptions(start=date), AuditManager().parse( arrayOf( "--start", arg ) ) )
        }
    }

    private fun testEndDate( date: DateTime, vararg args: String ){
        for( arg in args ){
            Assert.assertEquals( UNEXPECTED_OPTIONS, AuditManagerOptions(end=date), AuditManager().parse( arrayOf( "-e", arg ) ) )
            Assert.assertEquals( UNEXPECTED_OPTIONS, AuditManagerOptions(end=date), AuditManager().parse( arrayOf( "--end", arg ) ) )
        }
    }
}