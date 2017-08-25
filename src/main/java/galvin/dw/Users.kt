package galvin.dw

import java.io.File
import java.sql.Connection
import java.sql.ResultSet

interface UserDB {
    //roles
    fun storeRole(role: Role)
    fun deactivate( roleName: String )
    fun activate( roleName: String )
    fun listRoles(): List<Role>
    fun retrieveRole(name: String): Role?

    //users
    fun storeUser(user: User)
    fun retrieveUser(uuid: String): User?
}

data class User(
        //login credentials
        val login: String, val passwordHash: String?,

        //human name
        val name: String, val displayName: String, val sortName: String,
        val prependToName: String?, // this is used to store stuff like "Mr." or "Dr."
        val appendToName: String?, // used for stuff like rank, eg "Major General of the Fell Armies of Nod"

        //smart card info
        val credential: String?, val serialNumber: String?, val distinguishedName: String?,
        val homeAgency: String?, val agency: String?, val countryCode: String?,
        val citizenship: String?,

        //activation
        val created: Long, val active: Boolean,

        //uuid
        val uuid: String = uuid(),

        //contact info
        val contact: List<ContactInfo> = listOf<ContactInfo>(),

        //roles
        val roles: List<String> = listOf<String>()
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
    private val sqlCreateTableUsers = loadSql("/galvin/dw/db/sqlite/users/create_table_users.sql")
    private val sqlCreateTableContactInfo = loadSql("/galvin/dw/db/sqlite/users/create_table_contact_info.sql")
    private val sqlCreateTableUserRoles = loadSql("/galvin/dw/db/sqlite/users/create_table_user_roles.sql")

    private val sqlDeleteRole = loadSql("/galvin/dw/db/sqlite/users/delete_role.sql")
    private val sqlDeleteRolePermissions = loadSql("/galvin/dw/db/sqlite/users/delete_role_permissions.sql")

    private val sqlStoreRole = loadSql("/galvin/dw/db/sqlite/users/store_role.sql")
    private val sqlStoreRolePermission = loadSql("/galvin/dw/db/sqlite/users/store_role_permission.sql")
    private val sqlRetrieveAllRoles = loadSql("/galvin/dw/db/sqlite/users/retrieve_all_roles.sql")
    private val sqlRetrieveRoleByUuid = loadSql("/galvin/dw/db/sqlite/users/retrieve_role_by_uuid.sql")
    private val sqlRetrieveRolePermissions = loadSql("/galvin/dw/db/sqlite/users/retrieve_role_permissions.sql")

    private val sqlSetRoleActive = loadSql("/galvin/dw/db/sqlite/users/set_role_active.sql")

    private val sqlStoreUser = loadSql("/galvin/dw/db/sqlite/users/store_user.sql")
    private val sqlStoreUserContactInfo = loadSql("/galvin/dw/db/sqlite/users/store_user_contact_info.sql")
    private val sqlStoreUserRole = loadSql("/galvin/dw/db/sqlite/users/store_user_roles.sql")

    private val sqlRetrieveUserByUuid = loadSql("/galvin/dw/db/sqlite/users/retrieve_user_by_uuid.sql")
    private val sqlRetrieveContactInfoForUser = loadSql("/galvin/dw/db/sqlite/users/retrieve_contact_info_for_user.sql")
    private val sqlRetrieveRolesForUser = loadSql("/galvin/dw/db/sqlite/users/retrieve_roles_for_user.sql")

    private val sqlDeleteContactInfoForUser = loadSql("/galvin/dw/db/sqlite/users/delete_contact_info_for_user.sql")
    private val sqlDeleteRolesForUser = loadSql("/galvin/dw/db/sqlite/users/delete_roles_for_user.sql")

    init{
        //load driver
        Class.forName( "org.sqlite.JDBC" )

        //create tables
        runSql( conn(), sqlCreateTableRoles )
        runSql( conn(), sqlCreateTableRolePermissions )
        runSql( conn(), sqlCreateTableUsers )
        runSql( conn(), sqlCreateTableContactInfo )
        runSql( conn(), sqlCreateTableUserRoles )
    }

    //
    // Roles
    //

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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Users
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun storeUser(user: User){
        val conn = conn()

        val delContactStatement = conn.prepareStatement(sqlDeleteContactInfoForUser)
        delContactStatement.setString(1, user.uuid)
        delContactStatement.executeUpdate()
        delContactStatement.close()

        val delRolesStatement = conn.prepareStatement(sqlDeleteRolesForUser)
        delRolesStatement.setString(1, user.uuid)
        delRolesStatement.executeUpdate()
        delRolesStatement.close()

        val statement = conn.prepareStatement(sqlStoreUser)
        val active = if(user.active) 1 else 0

        statement.setString(1, user.login)
        statement.setString(2, user.passwordHash)
        statement.setString(3, user.name)
        statement.setString(4, user.displayName)
        statement.setString(5, user.sortName)
        statement.setString(6, user.prependToName)
        statement.setString(7, user.appendToName)
        statement.setString(8, user.credential)
        statement.setString(9, user.serialNumber)
        statement.setString(10, user.distinguishedName)
        statement.setString(11, user.homeAgency)
        statement.setString(12, user.agency)
        statement.setString(13, user.countryCode)
        statement.setString(14, user.citizenship)
        statement.setLong(15, user.created)
        statement.setInt(16, active)
        statement.setString(17, user.uuid)

        statement.executeUpdate()
        statement.close()

        for( (ordinal, contact) in user.contact.withIndex() ){
            val isPrimary = if(contact.primary) 1 else 0

            val contactStatement = conn.prepareStatement(sqlStoreUserContactInfo)
            contactStatement.setString(1, contact.type)
            contactStatement.setString(2, contact.description)
            contactStatement.setString(3, contact.contact)
            contactStatement.setInt(4, isPrimary)
            contactStatement.setString(5, user.uuid)
            contactStatement.setInt(6, ordinal)

            contactStatement.executeUpdate()
            contactStatement.close()
        }

        for( (ordinal, role) in user.roles.withIndex() ){
            val roleStatement = conn.prepareStatement(sqlStoreUserRole)
            roleStatement.setString(1, role)
            roleStatement.setString(2, user.uuid)
            roleStatement.setInt(3, ordinal)

            roleStatement.executeUpdate()
            roleStatement.close()
        }

        conn.commit()
        conn.close()
    }

    override fun retrieveUser(uuid: String): User?{
        var result: User? = null;

        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveUserByUuid)
        statement.setString(1, uuid)

        val resultSet = statement.executeQuery()
        if( resultSet != null ){
            if( resultSet.next() ){
                result = unmarshallUser(resultSet, conn)
            }
        }

        statement.close()
        conn.close()

        return result;
    }

    private fun unmarshallUser( hit: ResultSet, conn: Connection ): User{
        val uuid = hit.getString(17)

        val contact: MutableList<ContactInfo> = mutableListOf<ContactInfo>()
        val roles: MutableList<String> = mutableListOf<String>()
        val active = hit.getInt(16) != 0

        val contactStatement = conn.prepareStatement(sqlRetrieveContactInfoForUser )
        contactStatement.setString(1, uuid)
        val conHits = contactStatement.executeQuery()
        if( conHits != null ){
            while( conHits.next() ){
                contact.add( unmarshallContact(conHits) )
            }
        }
        contactStatement.close()

        val roleStatement = conn.prepareStatement(sqlRetrieveRolesForUser)
        roleStatement.setString(1, uuid)
        val roleHits = roleStatement.executeQuery()
        if( roleHits != null ){
            while( roleHits.next() ){
                roles.add( roleHits.getString(1) )
            }
        }
        roleStatement.close()

        return User(
                hit.getString(1),
                hit.getString(2),
                hit.getString(3),
                hit.getString(4),
                hit.getString(5),
                hit.getString(6),
                hit.getString(7),
                hit.getString(8),
                hit.getString(9),
                hit.getString(10),
                hit.getString(11),
                hit.getString(12),
                hit.getString(13),
                hit.getString(14),
                hit.getLong(15),
                active,
                uuid,
                contact,
                roles
        )
    }

    private fun unmarshallContact( hit: ResultSet ): ContactInfo{
        val primary = hit.getInt(4) != 0
        return ContactInfo(
                hit.getString(1),
                hit.getString(2),
                hit.getString(3),
                primary
        )
    }
}