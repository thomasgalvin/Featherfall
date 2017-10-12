package galvin.dw.sqlite

import galvin.dw.*
import java.io.File
import java.sql.Connection
import java.sql.ResultSet

class SQLiteUserDB( databaseFile: File) : UserDB, SQLiteDB(databaseFile) {
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
    private val sqlRetrieveUserBySerialNumber = loadSql("/galvin/dw/db/sqlite/users/retrieve_user_by_serial_number.sql")
    private val sqlRetrieveUserByLogin= loadSql("/galvin/dw/db/sqlite/users/retrieve_user_by_login.sql")
    private val sqlRetrieveAllUsers = loadSql("/galvin/dw/db/sqlite/users/retrieve_all_users.sql")

    private val sqlRetrieveContactInfoForUser = loadSql("/galvin/dw/db/sqlite/users/retrieve_contact_info_for_user.sql")
    private val sqlRetrieveRolesForUser = loadSql("/galvin/dw/db/sqlite/users/retrieve_roles_for_user.sql")

    private val sqlRetrieveUuidByLogin = loadSql("/galvin/dw/db/sqlite/users/retrieve_uuid_by_login.sql")
    private val sqlRetrieveUuidBySerialNumber = loadSql("/galvin/dw/db/sqlite/users/retrieve_uuid_by_serial_number.sql")

    private val sqlRetrieveUsersByLocked = loadSql("/galvin/dw/db/sqlite/users/retrieve_users_by_locked.sql")
    private val sqlSetLockedByUuid = loadSql("/galvin/dw/db/sqlite/users/set_locked_by_uuid.sql")
    private val sqlSetLockedByLogin = loadSql("/galvin/dw/db/sqlite/users/set_locked_by_login.sql")
    private val sqlIsLockedByUuid = loadSql("/galvin/dw/db/sqlite/users/is_locked_by_uuid.sql")
    private val sqlIsLockedByLogin = loadSql("/galvin/dw/db/sqlite/users/is_locked_by_login.sql")

    private val sqlRetrieveUsersByActive = loadSql("/galvin/dw/db/sqlite/users/retrieve_users_by_active.sql")
    private val sqlSetActiveByUuid = loadSql("/galvin/dw/db/sqlite/users/set_active_by_uuid.sql")
    private val sqlSetActiveByLogin = loadSql("/galvin/dw/db/sqlite/users/set_active_by_login.sql")
    private val sqlIsActiveByUuid = loadSql("/galvin/dw/db/sqlite/users/is_active_by_uuid.sql")
    private val sqlIsActiveByLogin = loadSql("/galvin/dw/db/sqlite/users/is_active_by_login.sql")

    private val sqlDeleteContactInfoForUser = loadSql("/galvin/dw/db/sqlite/users/delete_contact_info_for_user.sql")
    private val sqlDeleteRolesForUser = loadSql("/galvin/dw/db/sqlite/users/delete_roles_for_user.sql")

    private val sqlSetPasswordByUuid = loadSql("/galvin/dw/db/sqlite/users/set_password_by_uuid.sql")
    private val sqlSetPasswordByLogin = loadSql("/galvin/dw/db/sqlite/users/set_password_by_login.sql")
    private val sqlRetrievePasswordHashByUuid = loadSql("/galvin/dw/db/sqlite/users/get_password_hash_by_uuid.sql")

    private val sqlUpdateCredentialsByUuid = loadSql("/galvin/dw/db/sqlite/users/update_credentials_by_uuid.sql")
    private val sqlRetrieveCredentialsByUuid = loadSql("/galvin/dw/db/sqlite/users/retrieve_credentials_by_uuid.sql")

    init{
        val conn = conn()
        try {
            executeUpdate(conn, sqlCreateTableRoles)
            executeUpdate(conn, sqlCreateTableRolePermissions)
            executeUpdate(conn, sqlCreateTableUsers)
            executeUpdate(conn, sqlCreateTableContactInfo)
            executeUpdate(conn, sqlCreateTableUserRoles)
            commitAndClose(conn)
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    //
    // Roles
    //

    override fun storeRole(role: Role){
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val deletePermissionsStatement = conn.prepareStatement(sqlDeleteRolePermissions)
                deletePermissionsStatement.setString(1, role.name)
                executeAndClose(deletePermissionsStatement)

                val deleteRoleStatement = conn.prepareStatement(sqlDeleteRole)
                deleteRoleStatement.setString(1, role.name)
                executeAndClose(deleteRoleStatement)

                val active = if (role.active) 1 else 0

                val statement = conn.prepareStatement(sqlStoreRole)
                statement.setString(1, role.name)
                statement.setInt(2, active)
                executeAndClose(statement)

                for ((ordinal, permission) in role.permissions.withIndex()) {
                    val permStatement = conn.prepareStatement(sqlStoreRolePermission)
                    permStatement.setString(1, role.name)
                    permStatement.setString(2, permission)
                    permStatement.setInt(3, ordinal)

                    executeAndClose(permStatement)
                }

                commitAndClose(conn)
            }
            finally{
                rollbackAndClose(conn)
            }
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

            try {
                val statement = conn.prepareStatement(sqlSetRoleActive)

                val value = if (active) 1 else 0
                statement.setInt(1, value)
                statement.setString(2, roleName)

                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun listRoles(): List<Role>{
        val conn = conn()

        try{
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
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun retrieveRole(name: String): Role?{
        val conn = conn()

        try{
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
        finally{
            rollbackAndClose(conn)
        }
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

    override fun retrievePermissions( roleNames: List<String>): List<String>{
        val result = mutableListOf<String>()

        for( roleName in roleNames ){
            val role = retrieveRole(roleName)
            if( role != null ){
                for( permission in role.permissions ){
                    if( !isBlank(permission) ){
                        if( !result.contains(permission) ){
                            result.add(permission)
                        }
                    }
                }
            }
        }

        return result
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

            try {
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

                for ((ordinal, contact) in user.contact.withIndex()) {
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
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun retrieveUsers(): List<User>{
        return retrieveUsersBy(sqlRetrieveAllUsers)
    }

    override fun retrieveUsersByLocked( locked: Boolean): List<User>{
        val flag = if(locked) 1 else 0
        return retrieveUsersBy(sqlRetrieveUsersByLocked, flag)
    }

    override fun retrieveUsersByActive( active: Boolean): List<User>{
        val flag = if(active) 1 else 0
        return retrieveUsersBy(sqlRetrieveUsersByActive, flag)
    }

    private fun retrieveUsersBy( sql: String, intFlag: Int? = null ): List<User>{
        val conn = conn()

        try {
            val result = mutableListOf<User>()

            val statement = conn.prepareStatement(sql)
            if( intFlag != null){
                statement.setInt(1, intFlag)
            }

            val resultSet = statement.executeQuery()
            if (resultSet != null) {
                while (resultSet.next()) {
                    result.add(unmarshalUser(resultSet, conn))
                }
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun retrieveUser(uuid: String): User?{
        return retrieveUserBy(sqlRetrieveUserByUuid, uuid)
    }

    override fun retrieveUserBySerialNumber(serialNumber: String): User? {
        return retrieveUserBy(sqlRetrieveUserBySerialNumber, serialNumber)
    }

    override fun retrieveUserByLogin(login: String): User?{
        return retrieveUserBy(sqlRetrieveUserByLogin, login)
    }

    override fun retrieveUserByLoginAndPassword(login: String, password: String): User?{
        val user = retrieveUserByLogin(login)
        if( user != null ){
            if( !validate(password, user.passwordHash) ){
                return null
            }
        }

        return user
    }

    private fun retrieveUserBy(sql: String, value: String): User? {
        val conn = conn()

        try {
            var result: User? = null
            val statement = conn.prepareStatement(sql)
            statement.setString(1, value)

            val resultSet = statement.executeQuery()
            if (resultSet != null) {
                if (resultSet.next()) {
                    result = unmarshalUser(resultSet, conn)
                }
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
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

    override fun retrieveUuid(key: String): String?{
        var uuid = neverNull( retrieveUuidByLogin(key) )
        if( isBlank(uuid) ){
            uuid = neverNull( retrieveUuidBySerialNumber(key) )
        }
        return uuid
    }

    override fun retrieveUuidByLogin(login: String): String?{
        return retrieveUuidBy(sqlRetrieveUuidByLogin, login)
    }

    override fun retrieveUuidBySerialNumber(serial: String): String?{
        return retrieveUuidBy(sqlRetrieveUuidBySerialNumber, serial)
    }

    private fun retrieveUuidBy(sql: String, key: String): String?{
        val conn = conn()

        try {
            var result: String? = null

            val statement = conn.prepareStatement(sql)
            statement.setString(1, key)

            val results = statement.executeQuery()
            if (results.next()) {
                result = results.getString("uuid")
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
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

        try {
            val statement = conn.prepareStatement(sqlUserExistsByUuid)
            statement.setString(1, uuid)
            val resultSet = statement.executeQuery()
            val exists = resultSet.next()
            close(conn, statement)
            return exists
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun setLocked( uuid: String, locked: Boolean ){
        setLockedBy(sqlSetLockedByUuid, uuid, locked)
    }

    override fun setLockedByLogin( login: String, locked: Boolean ){
        setLockedBy(sqlSetLockedByLogin, login, locked)
    }

    private fun setLockedBy(sql: String, key: String, locked: Boolean){
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sql)
                val lockedValue = if (locked) 1 else 0
                statement.setInt(1, lockedValue)
                statement.setString(2, key)
                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun isLocked( uuid: String ): Boolean{
        return isLockedBy( sqlIsLockedByUuid, uuid )
    }

    override fun isLockedByLogin( login: String ): Boolean{
        return isLockedBy( sqlIsLockedByLogin, login )
    }

    private fun isLockedBy( sql: String, key: String): Boolean{
        val conn = conn()

        try {
            var result = false
            val statement = conn.prepareStatement(sql)
            statement.setString(1, key)
            val results = statement.executeQuery()
            if (results.next()) {
                val locked = results.getInt("locked")
                result = locked != 0
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun setActive( uuid: String, active: Boolean ){
        setActiveBy(sqlSetActiveByUuid, uuid, active)
    }

    override fun setActiveByLogin( login: String, active: Boolean ){
        setActiveBy(sqlSetActiveByLogin, login, active)
    }

    private fun setActiveBy(sql: String, key: String, active: Boolean){
        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sql)
                val activeValue = if (active) 1 else 0
                statement.setInt(1, activeValue)
                statement.setString(2, key)
                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun isActive( uuid: String ): Boolean{
        return isActiveBy( sqlIsActiveByUuid, uuid )
    }

    override fun isActiveByLogin( login: String ): Boolean{
        return isActiveBy( sqlIsActiveByLogin, login )
    }

    private fun isActiveBy( sql: String, key: String): Boolean{
        val conn = conn()

        try {
            var result = false
            val statement = conn.prepareStatement(sql)
            statement.setString(1, key)
            val results = statement.executeQuery()
            if (results.next()) {
                val active = results.getInt("active")
                result = active != 0
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun setPasswordByUuid( uuid: String, plainTextPassword: String ){
        setPasswordBy(sqlSetPasswordByUuid, uuid, plainTextPassword)
    }

    override fun setPasswordByLogin( login: String, plainTextPassword: String ){
        setPasswordBy(sqlSetPasswordByLogin, login, plainTextPassword)
    }

    private fun setPasswordBy( sql: String, uuid: String, plainTextPassword: String ){
        synchronized(concurrencyLock) {
            val conn = conn()
            val hash = hash(plainTextPassword)

            try {
                val statement = conn.prepareStatement(sql)
                statement.setString(1, hash)
                statement.setString(2, uuid)
                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun retrievePasswordHash(uuid: String): String{
        val conn = conn()

        try {
            var result = ""
            val statement = conn.prepareStatement(sqlRetrievePasswordHashByUuid)
            statement.setString(1, uuid)
            val results = statement.executeQuery()
            if (results.next()) {
                result = results.getString("passwordHash")
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun validatePassword( uuid: String, plainTextPassword: String ): Boolean{
        val hash = retrievePasswordHash(uuid)
        if( isBlank(hash) ) return false

        return validate(plainTextPassword, hash)
    }

    override fun updateCredentials( uuid: String, credentials: CertificateData ){
        if( !userExists(uuid) ) return

        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlUpdateCredentialsByUuid)
                statement.setString(1, credentials.credential)
                statement.setString(2, credentials.serialNumber)
                statement.setString(3, credentials.distinguishedName)
                statement.setString(4, credentials.countryCode)
                statement.setString(5, credentials.citizenship)
                statement.setString(6, uuid)
                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun retrieveCredentials( uuid: String ): CertificateData?{
        val conn = conn()

        try {
            var result: CertificateData? = null

            val statement = conn.prepareStatement(sqlRetrieveCredentialsByUuid)
            statement.setString(1, uuid)
            val results = statement.executeQuery()
            if (results.next()) {
                result = CertificateData(
                        credential = results.getString("credential"),
                        serialNumber = results.getString("serialNumber"),
                        distinguishedName = results.getString("distinguishedName"),
                        countryCode = results.getString("countryCode"),
                        citizenship = results.getString("citizenship")
                )
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }
}