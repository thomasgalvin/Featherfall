package galvin.dw

import com.sun.corba.se.pept.transport.ContactInfo
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

interface UserDB {
    fun storeRole(role: Role)
    fun deactivate( roleName: String )
    fun activate( roleName: String )
    fun listRoles(): List<Role>
}

data class User(
        //login credentials
        val login: String, val passwordHash: String?,

        //human name
        val name: String, val displayName: String, val sortName: String,
        val prepend: String?, // this is used to store stuff like "Mr." or "Dr."
        val append: String?, // used for stuff like rank, eg "Major General of the Fell Armies of Nod"

        //smart card info
        val credential: String?, val serialNumber: String?, val distinguishedName: String?,
        val homeAgency: String?, val agency: String?, val countryCode: String?,
        val citizenship: String?,

        //contact info
        val contact: List<ContactInfo> = listOf<ContactInfo>(),

        //roles
        val roles: List<String> = listOf<String>(),

        //activation
        val active: Boolean, val created: Long,

        //uuid
        val uuid: String = UUID.randomUUID().toString()
)

data class AccountRequest(
        //user details
        val user: User,
        val password: String,
        val confirmPassword: String,

        //confirmation; some accounts will only be approved for people with need-to-know or
        // some other compelling reason to have access to the system.
        val reasonForAccount: String?, //an explanation for why this account is being requested
        val vouchName: String?, //the name of someone who can vouch for you
        val vouchContactInfo: String?, //how to get ahold of the person who can vouch for you

        //approved / rejected
        val approved: Boolean = false, val approvedByUuid: String?, val approvedTimestamp: Long?,
        val rejected: Boolean = false, val rejectedByUuid: String?, val rejectedTimestamp: Long?,

        //uuid
        val uuid: String = UUID.randomUUID().toString()
)

data class Role( val name: String, val permissions: List<String>, val active: Boolean = true )

data class ContactInfo( val type: String, //eg "Phone", "Email", or "Mattermost"
                        val description: String, //eg "Work Email" or "Cell Phone"
                        val contact: String, //eg spam@devnull.com or 555.555.5555
                        val primary: Boolean //flags this as the best way to contact the user for the given type
)

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// SQLite implementation
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class SQLiteUserDB( private val databaseFile: File) :UserDB {
    private val concurrencyLock = Object()

    private val connectionUrl: String = "jdbc:sqlite:" + databaseFile.absolutePath

    private val sqlCreateTableRoles = loadSql("/galvin/dw/db/sqlite/users/create_table_roles.sql")
    private val sqlCreateTableRolePermissions = loadSql("/galvin/dw/db/sqlite/users/create_table_role_permissions.sql")

    init{
        //create tables
        runSql( getConnection(), sqlCreateTableRoles )
        runSql( getConnection(), sqlCreateTableRolePermissions )
    }

    private fun getConnection( connectionUrl: String ): Connection {
        Class.forName( "org.sqlite.JDBC" )
        val result = DriverManager.getConnection( connectionUrl )
        result.autoCommit = false
        return result
    }

    private fun getConnection(): Connection {
        return getConnection(connectionUrl)
    }

    override fun storeRole(role: Role){
    }

    override fun deactivate( roleName: String ){
    }

    override fun activate( roleName: String ){
    }

    override fun listRoles(): List<Role>{
        return listOf()
    }
}