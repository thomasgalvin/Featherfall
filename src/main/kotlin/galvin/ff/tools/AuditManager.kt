package galvin.ff.tools

import galvin.ff.*
import galvin.ff.PadTo.paddedLayout
import galvin.ff.sqlite.SQLiteAuditDB
import galvin.ff.sqlite.SQLiteUserDB
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.io.File
import java.io.PrintStream
import java.util.stream.Collectors


val programName = "audit-manager.sh"
val helpFile = "galvin/ff/manual/audit-manager.txt"

val verbose = Opt( short = "v", long = "verbose", desc = "Show extra debugging info" )
val help = Opt( short = "h", long = "help", desc = "Show the concise help summary" )
val manual = Opt( short = "m", long = "man", desc = "Show the complete help manual" )
val systemInfo = Opt( short = "si", long="system-info", desc = "Shows the system information, including classification and network info" )

val start = Opt( short = "s", long = "start", desc = "Specify the start date of the query", argName = "<yyyy.MM.dd.HH.mm.ss>" )
val end = Opt( short = "e", long = "end", desc = "Specify the end date of the query", argName = "<yyyy.MM.dd.HH.mm.ss>" )
val user = Opt( short = "u", long = "user", desc = "Query for audit events from a user", argName = "<user>" )
val success = Opt( short = "success", desc = "Show events where permission was granted" )
val fail = Opt( short = "failure", desc = "Show events where permission was denied" )
val access = Opt( short = "a", long = "access", desc = "Show events with the specified access type", argName = "<access_type>" )

val deltas = Opt( short = "d", long = "deltas", desc = "Show the complete modification history for each event" )

val sqlite = Opt( short = "sqlite",  desc = "Connect to an SQLite audit database", argName = "<sqlite filepath>" )
val userdb = Opt( short = "userdb",  desc = "Connect to an SQLite user database", argName = "<sqlite userdb filepath>" )



class AuditManager(){
    private val options = Options()
    private var isVerbose: Boolean = false
    private val dateTimeFormats: List<DateTimeFormatter>

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
        options.addOption( access.build() )
        options.addOption( systemInfo.build() )
        options.addOption( sqlite.build() )
        options.addOption( userdb.build() )
    }

    private fun initDateTimeFormats(): List<DateTimeFormatter>{
        return listOf(
                DateTimeFormat.forPattern("yyyy/MM/dd-kk:mm:ss"),
                DateTimeFormat.forPattern("yyyy/MM/dd-kk:mm"),
                DateTimeFormat.forPattern("yyyy/MM/dd"),
                DateTimeFormat.forPattern("yyyy/MM"),
                DateTimeFormat.forPattern("yyyy")
        )
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
                accessType = access.get(cmd),
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

    fun run(options: AuditManagerOptions) {
        if( options.showHelp ){
            help()
        }
        if( options.showManual ){
            manual()
        }
        if( options.showSystemInfo ){
            systemInfo(options)
        }

        val result = executeQuery(options)
        print(result, options)
    }

    fun executeQuery(options: AuditManagerOptions): List<AuditEvent>{
        isVerbose = options.verbose

        if( options.shouldQuery() ) {
            val auditDB = connect(options)
            val userDB = connectUserDB(options)

            val start = options.start?.millis
            val end = options.end?.millis
            val permission = options.calculatePermission()
            val userUuid = getUserUuid(options)
            val accessType = getAccessType(options)

            val result =  auditDB.retrieveAccessInfo(
                    userUuid = userUuid,
                    startTimestamp = start,
                    endTimestamp = end,
                    permissionGranted = permission,
                    accessType = accessType
            )

            if( result.isEmpty() ){
                verbose("<No results match query>")
            }

            return auditDB.toAuditEvent(userDB, result)
        }
        else{
            verbose( "No query specified: must specify at least one of [start | end | user]" )
        }

        return listOf()
    }

    fun print( events: List<AuditEvent>, options: AuditManagerOptions, out: PrintStream = System.out ){
        val systemName = createList( "System Name" )
        val systemVersion = createList( "Version" )
        val timestamp = createList( "Timestamp" )
        val userLogin = createList( "User Login" )
        val userLegalName = createList( "User Legal Name" )
        val loginType = createList( "Login Type" )
        val resourceUuid = createList( "Resource UUID" )
        val resourceName = createList( "Resource Name" )
        val resourceClassification = createList( "Classification" )
        val resourceType = createList( "Resource Type" )
        val accessType = createList( "Access Type" )
        val accessGranted = createList( "Access Granted" )
        val modifications = createList( "Modifications" )

        for( event in events ){
            val info = event.accessInfo

            val accessGrantedValue = if (info.permissionGranted) "granted" else "denied"
            val mods = getMods( info.modifications, options )
            var userName: String
            var legalName: String
            val systemInfoName: String
            val systemInfoVersion: String

            val user = event.user
            if (user != null) {
                userName = user.login
                legalName = user.sortName
            } else {
                userName = event.commandLineUserName
                legalName = ""
            }

            if( event.systemInfo != null ){
                systemInfoName = event.systemInfo.name
                systemInfoVersion = event.systemInfo.version
            }
            else{
                systemInfoName = ""
                systemInfoVersion = ""
            }

            systemName.add(systemInfoName)
            systemVersion.add(systemInfoVersion)
            timestamp.add( info.timestamp.toString() )
            userLogin.add(userName)
            userLegalName.add(legalName)
            loginType.add(event.loginType.name)
            resourceUuid.add( info.resourceUuid )
            resourceName.add( info.resourceName )
            resourceClassification.add( info.classification )
            resourceType.add( info.resourceType )
            accessType.add( info.accessType.name )
            accessGranted.add(accessGrantedValue)
            modifications.add(mods)
        }

        val separator = '-'
        val table = paddedLayout(
                separator,
                systemName,
                systemVersion,
                timestamp,
                userLogin,
                userLegalName,
                loginType,
                resourceUuid,
                resourceName,
                resourceClassification,
                resourceType,
                accessType,
                accessGranted,
                modifications )
        out.println(table)
    }

    private fun getMods( mods: List<Modification>, options: AuditManagerOptions ): String{
        if( options.showDeltas ){
            return mods.stream().map({ d -> d.toString() }).collect(Collectors.joining(", "))
        }
        else{
            return if( mods.isEmpty() ) "No modifications" else "Modifications made"
        }
    }

    private fun getUserUuid( options: AuditManagerOptions ): String?{
        if( isBlank(options.username) ) return null

        val userDB = connectUserDB(options)
        return userDB.retrieveUuidByLogin(options.username)
    }

    private fun getAccessType( options: AuditManagerOptions ): AccessType?{
        val name = options.accessType
        if( isBlank(name) ) return null
        return AccessType.valueOf(name)
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

    private fun systemInfo(options: AuditManagerOptions){
        val auditDB = connect(options)
        val current = auditDB.retrieveCurrentSystemInfo()
        if( current != null ){
            print(current)
        }
        else{
            println("<no current System Info has been assigned>")
        }
    }

    private fun print(info: SystemInfo){
        val fields = listOf(
                "Serial Number",
                "System Name",
                "Version",
                "Maximum Classification",
                "Classification Guide",
                "Networks"
        )

        val networkList = info.networks.stream().collect( Collectors.joining( ", " ) );
        val values = listOf(
                info.serialNumber,
                info.name,
                info.version,
                info.maximumClassification,
                info.classificationGuide,
                networkList
        )

        val table = paddedLayout( fields, values );
        println(table)
    }

    private fun verbose( message: String ){
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
                                val accessType: String = "",
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