package galvin.dw.tools

import galvin.dw.*
import galvin.dw.sqlite.SQLiteAuditDB
import galvin.dw.sqlite.SQLiteUserDB
import org.apache.commons.cli.Options
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import java.io.File
import javax.jws.soap.SOAPBinding


val programName = "audit-manager.sh"
val helpFile = "galvin/dw/manual/audit-manager.txt"

val verbose = Opt( short = "v", long = "verbose", desc = "Show extra debugging info" )
val manual = Opt( short = "m", long = "man", desc = "Show the complete help manual" )

val systemInfo = Opt( short = "si", long="system-info", desc = "Shows the system information, including classification and network info" )

val help = Opt( short = "h", long = "help", desc = "Show the concise help summary" )
val start = Opt( short = "s", long = "start", desc = "Specify the start date of the query", argName = "<yyyy.MM.dd.HH.mm.ss>" )
val end = Opt( short = "e", long = "end", desc = "Specify the end date of the query", argName = "<yyyy.MM.dd.HH.mm.ss>" )
val user = Opt( short = "u", long = "user", desc = "Query for audit events from a user", argName = "<user>" )
val success = Opt( short = "success", desc = "Show events where permission was granted" )
val fail = Opt( short = "failure", desc = "Show events where permission was denied" )

val deltas = Opt( short = "d", long = "deltas", desc = "Show the complete modification history for each event" )

val sqlite = Opt( short = "sqlite",  desc = "Connect to an SQLite audit database", argName = "<sqlite filepath>" )
val userdb = Opt( short = "sqliteuserdb",  desc = "Connect to an SQLite user database", argName = "<sqlite userdb filepath>" )



class AuditManager(){
    val options = Options()
    var isVerbose: Boolean = false
    val dateTimeFormats: List<DateTimeFormatter>

    init{
        initOptions()



        dateTimeFormats = initDateTimeFormats()
    }

    private fun initOptions(){
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
        options.addOption( sqlite.build() )
        options.addOption( userdb.build() )
    }

    private fun initDateTimeFormats(): List<DateTimeFormatter>{
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

        return listOf(dtf1, dtf2, dtf3)
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
                showSystemInfo = systemInfo.on(cmd),
                sqlite = sqlite.get(cmd),
                sqliteUserdb = userdb.get(cmd)
        )
    }

    fun main(args: Array<String>) {
        val auditManager = AuditManager()
        val options = auditManager.parse(args)
        auditManager.run(options)
    }

    private fun run(options: AuditManagerOptions) {
        if( options.showHelp ){
            help()
        }
        if( options.showManual ){
            manual()
        }

        val result = runQuery(options)

        for( hit in result ){

        }
    }

    private fun runQuery(options: AuditManagerOptions): List<AccessInfo>{
        isVerbose = options.verbose

        if( options.shouldQuery() ) {
            val auditDB = connect(options)

            val start = options.start?.millis
            val end = options.end?.millis
            val permission = options.calculatePermission()
            val userUuid = getUserUuid(options)

            val result =  auditDB.retrieveAccessInfo(
                    userUuid = userUuid,
                    startTimestamp = start,
                    endTimestamp = end,
                    permissionGranted = permission)

            if( result.isEmpty() ){
                verbose("<No results match query>")
            }

            return result
        }
        else{
            verbose( "No query specified: must specify at least one of [start | end | user]" )
        }

        return listOf()
    }

    private fun getUserUuid( options: AuditManagerOptions ): String?{
        if( isBlank(options.username) ) return null

        val userDB = connectUserDB(options)
        return userDB.retrieveUuidByLogin(options.username)
    }

    private fun connect( options: AuditManagerOptions ): AuditDB {
        if( isBlank(options.sqlite) ){
            throw Exception("Unable to connect to audit DB: no filepath specified")
        }

        val file = File(options.sqlite)
        if( !file.exists() ){
            throw Exception( "Unable to connect to audit DB: ${file.absolutePath} does not exist" )
        }
        else if( !file.canRead() ){
            throw Exception( "Unable to connect to audit DB: ${file.absolutePath} cannot be read" )
        }

        return SQLiteAuditDB(file)
    }

    private fun connectUserDB(options: AuditManagerOptions): UserDB{
        if( isBlank(options.sqliteUserdb) ){
            throw Exception("Unable to connect to user DB: no filepath specified")
        }

        val file = File(options.sqliteUserdb)
        if( !file.exists() ){
            throw Exception( "Unable to connect to user DB: ${file.absolutePath} does not exist" )
        }
        else if( !file.canRead() ){
            throw Exception( "Unable to connect to user DB: ${file.absolutePath} cannot be read" )
        }

        return SQLiteUserDB(file)
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
                                val showSystemInfo: Boolean = false,
                                val sqlite: String = "",
                                val sqliteUserdb: String = ""){
    fun shouldQuery(): Boolean{
        return start != null ||
                end != null ||
                !isBlank(username)
    }
    fun calculatePermission(): Boolean?{
        if( showSuccess != showFailure ){
            if(showSuccess) return true
            if(showFailure) return false
        }
        return null
    }
}