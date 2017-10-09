package galvin.dw.tools

import galvin.dw.loadResourceAndReadString
import galvin.dw.parseToDateTime
import org.apache.commons.cli.Options
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder


val programName = "audit-manager.sh"
val helpFile = "galvin/dw/manual/audit-manager.txt"

val verbose = Opt( short = "v", long = "verbose", desc = "Show extra debugging info" )
val manual = Opt( short = "m", long = "man", desc = "Show the complete help manual" )
val help = Opt( short = "h", long = "help", desc = "Show the concise help summary" )
val deltas = Opt( short = "d", long = "deltas", desc = "Show the complete modification history for each event" )

val start = Opt( short = "s", long = "start", desc = "Specify the start date of the query", argName = "<yyyy.MM.dd.HH.mm.ss>" )
val end = Opt( short = "e", long = "end", desc = "Specify the end date of the query", argName = "<yyyy.MM.dd.HH.mm.ss>" )
val user = Opt( short = "u", long = "user", desc = "Query for audit events from a user", argName = "<user>" )
val success = Opt( short = "success", desc = "Show events where permission was granted" )
val fail = Opt( short = "failure", desc = "Show events where permission was denied" )

val systemInfo = Opt( short = "si", long="system-info", desc = "Shows the system information, including classification and network info" )



class AuditManager(){
    val options = Options()
    var isVerbose: Boolean = false
    val dateTimeFormats: List<DateTimeFormatter>

    init{
        options.addOption( verbose.build() )
        options.addOption( manual.build() )
        options.addOption( help.build() )
        options.addOption( deltas.build() )
        options.addOption( start.build() )
        options.addOption( end.build() )
        options.addOption( user.build() )
        options.addOption( success.build() )
        options.addOption( fail.build() )
        options.addOption( systemInfo.build() )

        val dtf1 = DateTimeFormatterBuilder()
                .appendYear(4,4)
                .appendLiteral('/')
                .appendMonthOfYear(2)
                .appendLiteral('/')
                .appendDayOfMonth(2)
                .appendLiteral('-')
                .appendHourOfDay(2)
                .appendLiteral(':')
                .appendMinuteOfHour(2)
                .appendLiteral(':')
                .appendSecondOfMinute(2)
                .toFormatter()

        val dtf2 = DateTimeFormatterBuilder()
                .appendYear(4,4)
                .appendLiteral('/')
                .appendMonthOfYear(2)
                .appendLiteral('/')
                .appendDayOfMonth(2)
                .appendLiteral('-')
                .appendHourOfDay(2)
                .appendLiteral(':')
                .appendMinuteOfHour(2)
                .toFormatter()

        val dtf3 = DateTimeFormatterBuilder()
                .appendYear(4,4)
                .appendLiteral('/')
                .appendMonthOfYear(2)
                .appendLiteral('/')
                .appendDayOfMonth(2)
                .toFormatter()

        dateTimeFormats = listOf(dtf1, dtf2, dtf3)
    }

    fun parse( args: Array<String>): AuditManagerOptions{
        val cmd = DefaultParser().parse(options, args)

        val start: DateTime? = parseToDateTime( start.get(cmd), dateTimeFormats )
        val end:DateTime? =  parseToDateTime( end.get(cmd), dateTimeFormats )
        val username = user.get(cmd)

        return AuditManagerOptions(
                verbose = verbose.on(cmd),
                showHelp = help.on(cmd),
                showManual = manual.on(cmd),
                showSuccess = success.on(cmd),
                showFailure = fail.on(cmd),
                start = start,
                end = end,
                username = username,
                showDeltas = deltas.on(cmd),
                showSystemInfo = systemInfo.on(cmd)
        )
    }

    fun main(args: Array<String>) {
        val auditManager = AuditManager()
        val options = auditManager.parse(args)
        auditManager.run(options)
    }

    fun run(options: AuditManagerOptions){
        isVerbose = options.verbose

        if( options.showHelp ){
            help()
        }
        if( options.showManual ){
            manual()
        }
    }

    private fun help(){
        val formatter = HelpFormatter()
        formatter.printHelp( programName, options );
    }

    private fun manual(){
        val text = loadResourceAndReadString(helpFile)
        println(text)
    }

    fun verbose( message: String ){
        if( isVerbose ){
            println(message)
        }
    }
}

data class AuditManagerOptions( val verbose: Boolean = false,
                                val showHelp: Boolean = false,
                                val showManual: Boolean = false,
                                val showSuccess: Boolean = false,
                                val showFailure: Boolean = false,
                                val start: DateTime? = null,
                                val end: DateTime? = null,
                                val username: String = "",
                                val showDeltas: Boolean = false,
                                val showSystemInfo: Boolean = false )