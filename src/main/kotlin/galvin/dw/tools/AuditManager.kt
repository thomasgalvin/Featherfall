package galvin.dw.tools

import galvin.dw.loadResourceAndReadString
import org.apache.commons.cli.Options
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter


val programName = "audit-manager.sh"
val helpFile = "galvin/dw/manual/audit-manager.txt"

val dateTimeFormats: Array<String> = arrayOf( "yyyy.MM.dd.HH.mm.ss", "yyyy.MM.dd.HH.mm", "yyyy.MM.dd" )

val verbose = Opt( short = "v", long = "verbose", desc = "Show extra debugging info" )
val manual = Opt( short = "m", long = "man", desc = "Show the complete help manual" )
val help = Opt( short = "h", long = "help", desc = "Show the concise help summary" )
val deltas = Opt( short = "d", long = "deltas", desc = "Show the complete modification history for each event" )

val start = Opt( short = "s", long = "start", desc = "Specify the start date of the query", argName = "<yyyy.MM.dd.HH.mm.ss>" )
val end = Opt( short = "e", long = "end", desc = "Specify the end date of the query", argName = "<yyyy.MM.dd.HH.mm.ss>" )
val user = Opt( short = "u", long = "user", desc = "Query for audit events from a user", argName = "<user>" )
val success = Opt( short = "success", desc = "Show events where permission was granted" )
val fail = Opt( short = "fail", desc = "Show events where permission was denied" )

val systemInfo = Opt( short = "si", long="system-info", desc = "Shows the system information, including classification and network info" )

class AuditManager( args: Array<String> ){
    val options = Options()
    val cmd: CommandLine
    val isVerbose: Boolean

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

        cmd = DefaultParser().parse(options, args)
        isVerbose = verbose.on(cmd)
    }

    fun main(args: Array<String>) {
        AuditManager(args).run()
    }

    fun run(){

        if( help.on(cmd) ){
            help()
        }
        if( manual.on(cmd) ){
            manual()
        }
    }

    fun help(){
        val formatter = HelpFormatter()
        formatter.printHelp( programName, options );
    }

    fun manual(){
        val text = loadResourceAndReadString(helpFile)
        println(text)
    }

    fun verbose( message: String ){
        if( isVerbose ){
            println(message)
        }
    }
}