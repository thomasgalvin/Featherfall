package galvin.dw

import com.sun.corba.se.pept.transport.ContactInfo
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

interface UserDB {
    //roles
    fun storeRole(role: Role)
    fun deactivate( roleName: String )
    fun activate( roleName: String )
    fun listRoles(): List<Role>
    fun retrieveRole(name: String): Role?
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
        val created: Long, val active: Boolean,

        //uuid
        val uuid: String = uuid()
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
        val uuid: String = uuid()
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

class SQLiteUserDB( private val databaseFile: File) :UserDB, SQLiteDB(databaseFile) {
    private val concurrencyLock = Object()

    private val connectionUrl: String = "jdbc:sqlite:" + databaseFile.absolutePath

    private val sqlCreateTableRoles = loadSql("/galvin/dw/db/sqlite/users/create_table_roles.sql")
    private val sqlCreateTableRolePermissions = loadSql("/galvin/dw/db/sqlite/users/create_table_role_permissions.sql")

    private val sqlDeleteRole = loadSql("/galvin/dw/db/sqlite/users/delete_role.sql")
    private val sqlDeleteRolePermissions = loadSql("/galvin/dw/db/sqlite/users/delete_role_permissions.sql")

    private val sqlStoreRole = loadSql("/galvin/dw/db/sqlite/users/store_role.sql")
    private val sqlStoreRolePermission = loadSql("/galvin/dw/db/sqlite/users/store_role_permission.sql")
    private val sqlRetrieveAllRoles = loadSql("/galvin/dw/db/sqlite/users/retrieve_all_roles.sql")
    private val sqlRetrieveRoleByUuid = loadSql("/galvin/dw/db/sqlite/users/retrieve_role_by_uuid.sql")
    private val sqlRetrieveRolePermissions = loadSql("/galvin/dw/db/sqlite/users/retrieve_role_permissions.sql")

    private val sqlSetRoleActive = loadSql("/galvin/dw/db/sqlite/users/set_role_active.sql")

    init{
        //load driver
        Class.forName( "org.sqlite.JDBC" )

        //create tables
        runSql( conn(), sqlCreateTableRoles )
        runSql( conn(), sqlCreateTableRolePermissions )
    }

    private fun getConnection( connectionUrl: String ): Connection {
        val result = DriverManager.getConnection( connectionUrl )
        result.autoCommit = false
        return result
    }

    private fun getConnection(): Connection {
        return getConnection(connectionUrl)
    }

    override fun storeRole(role: Role){
        synchronized(concurrencyLock) {
            val conn = conn()

            val deletePermissionsStatement = conn.prepareStatement(sqlDeleteRolePermissions)
            deletePermissionsStatement.setString(1, role.name)
            deletePermissionsStatement.executeUpdate()
            deletePermissionsStatement.close()

            val deleteRoleStatement = conn.prepareStatement(sqlDeleteRole)
            deleteRoleStatement.setString(1, role.name)
            deleteRoleStatement.executeUpdate()
            deleteRoleStatement.close()

            val statement = conn.prepareStatement(sqlStoreRole)

            val active = if(role.active) 1 else 0
            statement.setString(1, role.name)
            statement.setInt(2, active)

            statement.executeUpdate()
            statement.close()

            for( (ordinal,permission) in role.permissions.withIndex() ){
                val permStatement = conn.prepareStatement(sqlStoreRolePermission)
                permStatement.setString(1, role.name)
                permStatement.setString(2, permission)
                permStatement.setInt(3, ordinal)

                permStatement.executeUpdate()
                permStatement.close()
            }


            conn.commit()
            conn.close()
        }
    }

    override fun deactivate( roleName: String ){
        doSetRoleActive(roleName, false)
    }

    override fun activate( roleName: String ){
        doSetRoleActive(roleName, true)
    }

    private fun doSetRoleActive( roleName: String, active: Boolean ){
        val conn = conn();
        val statement = conn.prepareStatement(sqlSetRoleActive)

        val value = if(active) 1 else 0
        statement.setInt(1, value)
        statement.setString(2, roleName)

        statement.executeUpdate()
        statement.close()

        conn.commit()
        conn.close()
    }

    override fun listRoles(): List<Role>{
        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveAllRoles)
        val result = mutableListOf<Role>()

        val resultSet = statement.executeQuery()
        if(resultSet != null){
            while( resultSet.next() ){
                result.add( unmarshallRole(resultSet, conn) )
            }
        }

        close(conn, statement)
        return result
    }

    override fun retrieveRole(name: String): Role?{
        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveRoleByUuid)
        statement.setString(1, name)

        var result: Role? = null

        val resultSet = statement.executeQuery()
        if(resultSet != null){
            if( resultSet.next() ){
                result = unmarshallRole(resultSet, conn)
            }
        }

        close(conn, statement)
        return result
    }

    private fun unmarshallRole(hit: ResultSet, conn: Connection ): Role{
        val name = hit.getString(1)
        val active = hit.getInt(2) != 0
        val permissions = mutableListOf<String>()

        val statement = conn.prepareStatement(sqlRetrieveRolePermissions)
        statement.setString(1, name)

        val permissionHits = statement.executeQuery()
        if( permissionHits != null ){
            while( permissionHits.next() ){
                permissions.add( permissionHits.getString(1) )
            }
        }

        statement.close()
        return Role(name, permissions, active)
    }
}