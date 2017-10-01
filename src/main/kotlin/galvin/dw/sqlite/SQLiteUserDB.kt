package galvin.dw.sqlite

import galvin.dw.*
import java.io.File
import java.sql.Connection
import java.sql.ResultSet

class SQLiteUserDB( private val databaseFile: File) : UserDB, SQLiteDB(databaseFile) {
    private val concurrencyLock = Object()

    private val sqlCreateTableRoles = loadSql("/galvin/dw/db/sqlite/roles/create_table_roles.sql")
    private val sqlCreateTableRolePermissions = loadSql("/galvin/dw/db/sqlite/roles/create_table_role_permissions.sql")
    private val sqlCreateTableUsers = loadSql("/galvin/dw/db/sqlite/users/create_table_users.sql")
    private val sqlCreateTableContactInfo = loadSql("/galvin/dw/db/sqlite/users/create_table_contact_info.sql")
    private val sqlCreateTableUserRoles = loadSql("/galvin/dw/db/sqlite/users/create_table_user_roles.sql")

    private val sqlDeleteRole = loadSql("/galvin/dw/db/sqlite/roles/delete_role.sql")
    private val sqlDeleteRolePermissions = loadSql("/galvin/dw/db/sqlite/roles/delete_role_permissions.sql")

    private val sqlStoreRole = loadSql("/galvin/dw/db/sqlite/roles/store_role.sql")
    private val sqlStoreRolePermission = loadSql("/galvin/dw/db/sqlite/roles/store_role_permission.sql")
    private val sqlRetrieveAllRoles = loadSql("/galvin/dw/db/sqlite/roles/retrieve_all_roles.sql")
    private val sqlRetrieveRoleByUuid = loadSql("/galvin/dw/db/sqlite/roles/retrieve_role_by_uuid.sql")
    private val sqlRetrieveRolePermissions = loadSql("/galvin/dw/db/sqlite/roles/retrieve_role_permissions.sql")

    private val sqlSetRoleActive = loadSql("/galvin/dw/db/sqlite/roles/set_role_active.sql")

    private val sqlStoreUser = loadSql("/galvin/dw/db/sqlite/users/store_user.sql")
    private val sqlStoreUserContactInfo = loadSql("/galvin/dw/db/sqlite/users/store_user_contact_info.sql")
    private val sqlStoreUserRole = loadSql("/galvin/dw/db/sqlite/users/store_user_roles.sql")

    private val sqlUserExistsByUuid = loadSql("/galvin/dw/db/sqlite/users/user_exists_by_uuid.sql")
    private val sqlRetrieveUserByUuid = loadSql("/galvin/dw/db/sqlite/users/retrieve_user_by_uuid.sql")
    private val sqlRetrieveAllUsers = loadSql("/galvin/dw/db/sqlite/users/retrieve_all_users.sql")
    private val sqlRetrieveContactInfoForUser = loadSql("/galvin/dw/db/sqlite/users/retrieve_contact_info_for_user.sql")
    private val sqlRetrieveRolesForUser = loadSql("/galvin/dw/db/sqlite/users/retrieve_roles_for_user.sql")

    private val sqlDeleteContactInfoForUser = loadSql("/galvin/dw/db/sqlite/users/delete_contact_info_for_user.sql")
    private val sqlDeleteRolesForUser = loadSql("/galvin/dw/db/sqlite/users/delete_roles_for_user.sql")

    init{
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
            executeAndClose(deletePermissionsStatement)

            val deleteRoleStatement = conn.prepareStatement(sqlDeleteRole)
            deleteRoleStatement.setString(1, role.name)
            executeAndClose(deleteRoleStatement)

            val active = if(role.active) 1 else 0

            val statement = conn.prepareStatement(sqlStoreRole)
            statement.setString(1, role.name)
            statement.setInt(2, active)
            executeAndClose(statement)

            for( (ordinal,permission) in role.permissions.withIndex() ){
                val permStatement = conn.prepareStatement(sqlStoreRolePermission)
                permStatement.setString(1, role.name)
                permStatement.setString(2, permission)
                permStatement.setInt(3, ordinal)

                executeAndClose(permStatement)
            }

            commitAndClose(conn)
        }
    }

    override fun deactivate( roleName: String ){
        doSetRoleActive(roleName, false)
    }

    override fun activate( roleName: String ){
        doSetRoleActive(roleName, true)
    }

    private fun doSetRoleActive( roleName: String, active: Boolean ){
        synchronized(concurrencyLock) {
            val conn = conn()
            val statement = conn.prepareStatement(sqlSetRoleActive)

            val value = if (active) 1 else 0
            statement.setInt(1, value)
            statement.setString(2, roleName)

            executeAndClose(statement, conn)
        }
    }

    override fun listRoles(): List<Role>{
        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveAllRoles)
        val result = mutableListOf<Role>()

        val resultSet = statement.executeQuery()
        if (resultSet != null) {
            while (resultSet.next()) {
                result.add(unmarshalRole(resultSet, conn))
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
        if (resultSet != null) {
            if (resultSet.next()) {
                result = unmarshalRole(resultSet, conn)
            }
        }

        close(conn, statement)
        return result
    }

    private fun unmarshalRole(hit: ResultSet, conn: Connection): Role {
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

    override fun storeUser(user: User, uuid : String?){
        synchronized(concurrencyLock) {
            val theUuid = if( isBlank(uuid) ) user.uuid else uuid
            val conn = conn()

            val delContactStatement = conn.prepareStatement(sqlDeleteContactInfoForUser)
            delContactStatement.setString(1, theUuid)
            executeAndClose(delContactStatement)

            val delRolesStatement = conn.prepareStatement(sqlDeleteRolesForUser)
            delRolesStatement.setString(1, theUuid)
            executeAndClose(delRolesStatement)

            val active = if (user.active) 1 else 0
            val locked = if (user.locked) 1 else 0

            val statement = conn.prepareStatement(sqlStoreUser)
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
            statement.setInt(17, locked)
            statement.setString(18, theUuid)

            executeAndClose(statement)

            for( (ordinal, contact) in user.contact.withIndex() ) {
                val isPrimary = if (contact.primary) 1 else 0

                val contactStatement = conn.prepareStatement(sqlStoreUserContactInfo)
                contactStatement.setString(1, contact.type)
                contactStatement.setString(2, contact.description)
                contactStatement.setString(3, contact.contact)
                contactStatement.setInt(4, isPrimary)
                contactStatement.setString(5, theUuid)
                contactStatement.setInt(6, ordinal)

                executeAndClose(contactStatement)
            }

            for ((ordinal, role) in user.roles.withIndex()) {
                val roleStatement = conn.prepareStatement(sqlStoreUserRole)
                roleStatement.setString(1, role)
                roleStatement.setString(2, theUuid)
                roleStatement.setInt(3, ordinal)

                executeAndClose(roleStatement)
            }

            commitAndClose(conn)
        }
    }

    override fun retrieveUser(uuid: String): User?{
        var result: User? = null

        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveUserByUuid)
        statement.setString(1, uuid)

        val resultSet = statement.executeQuery()
        if (resultSet != null) {
            if (resultSet.next()) {
                result = unmarshalUser(resultSet, conn)
            }
        }

        close(conn, statement)
        return result
    }

    override fun retrieveUsers(): List<User>{
        val result = mutableListOf<User>()

        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveAllUsers)

        val resultSet = statement.executeQuery()
        if (resultSet != null) {
            if (resultSet.next()) {
                result.add(unmarshalUser(resultSet, conn))
            }
        }

        close(conn, statement)
        return result
    }

    private fun unmarshalUser(hit: ResultSet, conn: Connection): User {
        val uuid = hit.getString("uuid")

        val contact: MutableList<ContactInfo> = mutableListOf<ContactInfo>()
        val roles: MutableList<String> = mutableListOf<String>()
        val active = hit.getInt("active") != 0
        val locked = hit.getInt("locked") != 0

        val contactStatement = conn.prepareStatement(sqlRetrieveContactInfoForUser )
        contactStatement.setString(1, uuid)
        val conHits = contactStatement.executeQuery()
        if( conHits != null ){
            while( conHits.next() ){
                contact.add( unmarshalContact(conHits) )
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
                login = hit.getString("login"),
                passwordHash = hit.getString("passwordHash"),
                name = hit.getString("name"),
                displayName = hit.getString("displayName"),
                sortName = hit.getString("sortName"),
                prependToName = hit.getString("prependToName"),
                appendToName = hit.getString("appendToName"),
                credential = hit.getString("credential"),
                serialNumber = hit.getString("serialNumber"),
                distinguishedName = hit.getString("distinguishedName"),
                homeAgency = hit.getString("homeAgency"),
                agency = hit.getString("agency"),
                countryCode = hit.getString("countryCode"),
                citizenship = hit.getString("citizenship"),
                created = hit.getLong("created"),
                active = active,
                locked = locked,
                uuid = uuid,
                contact = contact,
                roles = roles
        )
    }

    private fun unmarshalContact( hit: ResultSet): ContactInfo {
        val primary = hit.getInt(4) != 0
        return ContactInfo(
                hit.getString(1),
                hit.getString(2),
                hit.getString(3),
                primary
        )
    }

    override fun userExists(uuid: String): Boolean{
        val conn = conn()
        val statement = conn.prepareStatement(sqlUserExistsByUuid)
        statement.setString(1, uuid)
        val resultSet = statement.executeQuery()
        val exists = resultSet.next()
        close(conn, statement)
        return exists
    }
}