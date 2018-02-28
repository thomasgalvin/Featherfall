package galvin.ff

import galvin.ff.db.ConnectionManager
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

const val ERROR_PASSWORD_MISMATCH = "Account Request error: password mismatch"
const val ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID = "Account Request error: no account request with that UUID"
const val ERROR_USER_WITH_THIS_UUID_ALREADY_EXISTS = "Account Request error: user with this UUID already exists"
const val ERROR_NO_USER_WITH_THIS_UUID_EXISTS = "Account Request error: no user with this UUID exists"
const val ERROR_ALREADY_APPROVED = "Account Request error: this request was previously approved"

interface UserDB {
    //roles
    fun storeRole(role: Role)
    fun deactivate( roleName: String )
    fun activate( roleName: String )
    fun listRoles(): List<Role>
    fun retrieveRole(name: String): Role?

    fun retrievePermissions( roleNames: List<String> ): List<String>

    //users
    fun storeUser(user: User, uuid: String? = null)
    fun retrieveUsers(): List<User>
    fun retrieveUser(uuid: String): User?
    fun retrieveUserBySerialNumber(serialNumber: String): User?
    fun retrieveUserByLogin(login: String): User?
    fun retrieveUserByLoginAndPassword(login: String, password: String): User?
    fun retrieveUuidByLogin(login: String): String?
    fun retrieveUuidBySerialNumber(serial: String): String?
    fun retrieveUuid(key: String): String?

    fun userExists(uuid: String): Boolean

    fun retrieveUsersByLocked( locked: Boolean = true): List<User>
    fun isLocked( uuid: String ): Boolean
    fun isLockedByLogin( login: String ): Boolean
    fun setLocked( uuid: String, locked: Boolean )
    fun setLockedByLogin( login: String, locked: Boolean )

    fun retrieveUsersByActive( active: Boolean = true): List<User>
    fun isActive( uuid: String ): Boolean
    fun isActiveByLogin( login: String ): Boolean
    fun setActive( uuid: String, active: Boolean )
    fun setActiveByLogin( login: String, active: Boolean )

    fun setPasswordByUuid( uuid: String, plainTextPassword: String )
    fun setPasswordByLogin( login: String, plainTextPassword: String )

    fun retrievePasswordHash(uuid: String): String
    fun validatePassword( uuid: String, plainTextPassword: String ): Boolean

    fun updateCredentials( uuid: String, credentials: CertificateData )
    fun retrieveCredentials( uuid: String ): CertificateData?

    companion object {
        fun SQLite( maxConnections: Int, databaseFile: File, timeout: Long = 60_000 ): UserDB{
            val connectionManager = ConnectionManager.SQLite(maxConnections, databaseFile, timeout)
            val classpath = "/galvin/ff/db/sqlite/"
            return UserDBImpl(connectionManager, classpath)
        }

        fun PostgreSQL( maxConnections: Int, connectionURL: String, timeout: Long = 60_000, username: String? = null, password: String? = null ): UserDB{
            val connectionManager = ConnectionManager.PostgreSQL(maxConnections, connectionURL, timeout, username, password)
            val classpath = "/galvin/ff/db/psql/"
            return UserDBImpl(connectionManager, classpath)
        }
    }
}

interface AccountRequestDB {
    fun storeAccountRequest( request: AccountRequest )
    fun retrieveAccountRequest( uuid: String ) : AccountRequest?
    fun retrieveAccountRequests() : List<AccountRequest>

    fun retrievePendingAccountRequests() : List<AccountRequest>
    fun retrieveApprovedAccountRequests() : List<AccountRequest>
    fun retrieveRejectedAccountRequests() : List<AccountRequest>

    fun approve( uuid: String, approvedByUuid: String, timestamp: Long = System.currentTimeMillis() )
    fun reject( uuid: String, rejectedByUuid: String, reason: String = "", timestamp: Long = System.currentTimeMillis() )

    companion object {
        fun SQLite( userDB: UserDB, maxConnections: Int, databaseFile: File, userDatabaseFile: File, timeout: Long = 60_000 ): AccountRequestDB{
            val connectionManager = ConnectionManager.SQLite(maxConnections, databaseFile, timeout)
            val accountRequestUserInfoDB = UserDB.SQLite(maxConnections, userDatabaseFile, timeout)
            val classpath = "/galvin/ff/db/sqlite/"
            return AccountRequestDBImpl(connectionManager, userDB, accountRequestUserInfoDB, classpath)
        }

        fun PostgreSQL( userDB: UserDB, maxConnections: Int, connectionURL: String, timeout: Long = 60_000, username: String? = null, password: String? = null ): AccountRequestDB{
            val connectionManager = ConnectionManager.PostgreSQL(maxConnections, connectionURL, timeout, username, password)
            val accountRequestUserInfoDB = UserDB.PostgreSQL(maxConnections, connectionURL, timeout, username, password)
            val classpath = "/galvin/ff/db/psql/"
            return AccountRequestDBImpl(connectionManager, userDB, accountRequestUserInfoDB, classpath)
        }
    }
}

data class User(
        //login credentials
        val login: String, val passwordHash: String? = "",

        //human name
        val name: String, //full legal name, eg "John Smith"
        val displayName:  String,//on-screen nickname, eg "John" or "Tigerpunch2010"
        val sortName: String, //name in a sortable order, eg "Smith, John"
        val prependToName: String = "", // this is used to storeSystemInfo stuff like "Mr." or "Dr."
        val appendToName: String? = "", // used for stuff like rank, eg "Major General of the Fell Armies of Nod"

        //smart card info
        val credential: String = "", val serialNumber: String = "",
        val distinguishedName: String = "", val homeAgency: String = "",
        val agency: String = "", val countryCode: String = "",
        val citizenship: String = "",

        //activation
        val created: Long, val active: Boolean, val locked: Boolean = false,

        //uuid
        val uuid: String = uuid(),

        //contact info
        val contact: List<ContactInfo> = listOf(),

        //roles
        val roles: List<String> = listOf()
){
    fun withoutPasswordHash(): User{
        return copy( passwordHash = null )
    }

    fun withCredentials(credentials: CertificateData): User{
        return copy(
                credential = credentials.credential,
                serialNumber = credentials.serialNumber,
                distinguishedName = credentials.distinguishedName,
                countryCode = credentials.countryCode,
                citizenship = credentials.citizenship
        )
    }

    fun getCredentials(): CertificateData{
        return CertificateData(
                credential = credential,
                serialNumber = serialNumber,
                distinguishedName = distinguishedName,
                countryCode = countryCode,
                citizenship = citizenship
        )
    }
}

data class AccountRequest(
        //user details
        val user: User,
        val password: String,
        val confirmPassword: String,

        //confirmation some accounts will only be approved for people with need-to-know or
        // some other compelling reason to have access to the system.
        val reasonForAccount: String?, //an explanation for why this account is being requested
        val vouchName: String?, //the name of someone who can vouch for you
        val vouchContactInfo: String?, //how to get ahold of the person who can vouch for you

        //approved / rejected
        val approved: Boolean = false, val approvedByUuid: String = "", val approvedTimestamp: Long = -1,
        val rejected: Boolean = false, val rejectedByUuid: String = "", val rejectedTimestamp: Long = -1,
        val rejectedReason: String = "",

        //uuid
        val uuid: String = uuid()
)

data class Role( val name: String, val permissions: List<String> = listOf(), val active: Boolean = true )

data class ContactInfo( val type: String, //eg "Phone", "Email", or "Mattermost"
                        val description: String, //eg "Work Email" or "Cell Phone"
                        val contact: String, //eg spam@devnull.com or 555.555.5555
                        val primary: Boolean //flags this as the best way to contact the user for the given type
)

///
/// No-Op implementation
///

class NoOpUserDB: UserDB {
    //roles
    override fun storeRole(role: Role){}
    override fun deactivate( roleName: String ){}
    override fun activate( roleName: String ){}
    override fun listRoles(): List<Role> {return listOf()}
    override fun retrieveRole(name: String): Role? {return null}

    override fun retrievePermissions( roleNames: List<String> ): List<String> {return listOf()}

    //users
    override fun storeUser(user: User, uuid: String?){}
    override fun retrieveUsers(): List<User> {return listOf() }
    override fun retrieveUser(uuid: String): User? {return null}
    override fun retrieveUserBySerialNumber(serialNumber: String): User? {return null}
    override fun retrieveUserByLogin(login: String): User? {return null}
    override fun retrieveUserByLoginAndPassword(login: String, password: String): User? {return null}
    override fun retrieveUuidByLogin(login: String): String? {return null}
    override fun retrieveUuidBySerialNumber(serial: String): String? {return null}
    override fun retrieveUuid(key: String): String? {return null}

    override fun userExists(uuid: String): Boolean {return false}

    override fun retrieveUsersByLocked( locked: Boolean): List<User> {return listOf() }
    override fun isLocked( uuid: String ): Boolean {return false}
    override fun isLockedByLogin( login: String ): Boolean {return false}
    override fun setLocked( uuid: String, locked: Boolean ){}
    override fun setLockedByLogin( login: String, locked: Boolean ){}

    override fun retrieveUsersByActive( active: Boolean ): List<User> {return listOf() }
    override fun isActive( uuid: String ): Boolean {return false}
    override fun isActiveByLogin( login: String ): Boolean {return false}
    override fun setActive( uuid: String, active: Boolean ){}
    override fun setActiveByLogin( login: String, active: Boolean ){}

    override fun setPasswordByUuid( uuid: String, plainTextPassword: String ){}
    override fun setPasswordByLogin( login: String, plainTextPassword: String ){}

    override fun retrievePasswordHash(uuid: String): String {return ""}
    override fun validatePassword( uuid: String, plainTextPassword: String ): Boolean {return false}

    override fun updateCredentials( uuid: String, credentials: CertificateData ){}
    override fun retrieveCredentials( uuid: String ): CertificateData? {return null}
}

class NoOpAccountRequestDB: AccountRequestDB {
    override fun storeAccountRequest( request: AccountRequest ){}
    override fun retrieveAccountRequest( uuid: String ) : AccountRequest? {return null}
    override fun retrieveAccountRequests() : List<AccountRequest> {return listOf() }

    override fun retrievePendingAccountRequests() : List<AccountRequest> {return listOf() }
    override fun retrieveApprovedAccountRequests() : List<AccountRequest> {return listOf() }
    override fun retrieveRejectedAccountRequests() : List<AccountRequest> {return listOf() }

    override fun approve( uuid: String, approvedByUuid: String, timestamp: Long ){}
    override fun reject( uuid: String, rejectedByUuid: String, reason: String, timestamp: Long ){}
}

///
/// Working implementation
///

class UserDBImpl(private val connectionManager: ConnectionManager,
                 sqlClasspath: String) : UserDB {
    private val concurrencyLock = Object()

    private val sqlCreateTableRoles = loadSql("$sqlClasspath/roles/create_table_roles.sql")
    private val sqlCreateTableRolePermissions = loadSql("$sqlClasspath/roles/create_table_role_permissions.sql")
    private val sqlCreateTableUsers = loadSql("$sqlClasspath/users/create_table_users.sql")
    private val sqlCreateTableContactInfo = loadSql("$sqlClasspath/users/create_table_contact_info.sql")
    private val sqlCreateTableUserRoles = loadSql("$sqlClasspath/users/create_table_user_roles.sql")

    private val sqlDeleteRole = loadSql("$sqlClasspath/roles/delete_role.sql")
    private val sqlDeleteRolePermissions = loadSql("$sqlClasspath/roles/delete_role_permissions.sql")

    private val sqlStoreRole = loadSql("$sqlClasspath/roles/store_role.sql")
    private val sqlStoreRolePermission = loadSql("$sqlClasspath/roles/store_role_permission.sql")
    private val sqlRetrieveAllRoles = loadSql("$sqlClasspath/roles/retrieve_all_roles.sql")
    private val sqlRetrieveRoleByUuid = loadSql("$sqlClasspath/roles/retrieve_role_by_uuid.sql")
    private val sqlRetrieveRolePermissions = loadSql("$sqlClasspath/roles/retrieve_role_permissions.sql")

    private val sqlSetRoleActive = loadSql("$sqlClasspath/roles/set_role_active.sql")

    private val sqlStoreUser = loadSql("$sqlClasspath/users/store_user.sql")
    private val sqlStoreUserContactInfo = loadSql("$sqlClasspath/users/store_user_contact_info.sql")
    private val sqlStoreUserRole = loadSql("$sqlClasspath/users/store_user_roles.sql")

    private val sqlUserExistsByUuid = loadSql("$sqlClasspath/users/user_exists_by_uuid.sql")
    private val sqlRetrieveUserByUuid = loadSql("$sqlClasspath/users/retrieve_user_by_uuid.sql")
    private val sqlRetrieveUserBySerialNumber = loadSql("$sqlClasspath/users/retrieve_user_by_serial_number.sql")
    private val sqlRetrieveUserByLogin= loadSql("$sqlClasspath/users/retrieve_user_by_login.sql")
    private val sqlRetrieveAllUsers = loadSql("$sqlClasspath/users/retrieve_all_users.sql")

    private val sqlRetrieveContactInfoForUser = loadSql("$sqlClasspath/users/retrieve_contact_info_for_user.sql")
    private val sqlRetrieveRolesForUser = loadSql("$sqlClasspath/users/retrieve_roles_for_user.sql")

    private val sqlRetrieveUuidByLogin = loadSql("$sqlClasspath/users/retrieve_uuid_by_login.sql")
    private val sqlRetrieveUuidBySerialNumber = loadSql("$sqlClasspath/users/retrieve_uuid_by_serial_number.sql")

    private val sqlRetrieveUsersByLocked = loadSql("$sqlClasspath/users/retrieve_users_by_locked.sql")
    private val sqlSetLockedByUuid = loadSql("$sqlClasspath/users/set_locked_by_uuid.sql")
    private val sqlSetLockedByLogin = loadSql("$sqlClasspath/users/set_locked_by_login.sql")
    private val sqlIsLockedByUuid = loadSql("$sqlClasspath/users/is_locked_by_uuid.sql")
    private val sqlIsLockedByLogin = loadSql("$sqlClasspath/users/is_locked_by_login.sql")

    private val sqlRetrieveUsersByActive = loadSql("$sqlClasspath/users/retrieve_users_by_active.sql")
    private val sqlSetActiveByUuid = loadSql("$sqlClasspath/users/set_active_by_uuid.sql")
    private val sqlSetActiveByLogin = loadSql("$sqlClasspath/users/set_active_by_login.sql")
    private val sqlIsActiveByUuid = loadSql("$sqlClasspath/users/is_active_by_uuid.sql")
    private val sqlIsActiveByLogin = loadSql("$sqlClasspath/users/is_active_by_login.sql")

    private val sqlDeleteContactInfoForUser = loadSql("$sqlClasspath/users/delete_contact_info_for_user.sql")
    private val sqlDeleteRolesForUser = loadSql("$sqlClasspath/users/delete_roles_for_user.sql")

    private val sqlSetPasswordByUuid = loadSql("$sqlClasspath/users/set_password_by_uuid.sql")
    private val sqlSetPasswordByLogin = loadSql("$sqlClasspath/users/set_password_by_login.sql")
    private val sqlRetrievePasswordHashByUuid = loadSql("$sqlClasspath/users/get_password_hash_by_uuid.sql")

    private val sqlUpdateCredentialsByUuid = loadSql("$sqlClasspath/users/update_credentials_by_uuid.sql")
    private val sqlRetrieveCredentialsByUuid = loadSql("$sqlClasspath/users/retrieve_credentials_by_uuid.sql")

    fun conn(): Connection = connectionManager.connect()

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
            rollbackAndClose(conn, connectionManager)
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
                rollbackAndClose(conn, connectionManager)
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
                rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
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

    //
    // Users
    //

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
                rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
        }
    }

    private fun unmarshalUser(hit: ResultSet, conn: Connection): User {
        val uuid = hit.getString("uuid")

        val contact: MutableList<ContactInfo> = mutableListOf()
        val roles: MutableList<String> = mutableListOf()
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
            rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
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
                rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
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
                rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
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
                rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
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
                rollbackAndClose(conn, connectionManager)
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
            rollbackAndClose(conn, connectionManager)
        }
    }
}

class AccountRequestDBImpl( private val connectionManager: ConnectionManager,
                            private val userDB: UserDB,
                            private val accountRequestUserInfoDB: UserDB,
                            sqlClasspath: String ) : AccountRequestDB {
    private val concurrencyLock = Object()
    
    private val sqlCreateTableAccountRequests = loadSql("$sqlClasspath/requests/create_table_account_requests.sql")
    private val sqlStoreAccountRequest = loadSql("$sqlClasspath/requests/store_account_request.sql")
    private val sqlRetrieveAccountRequestByUuid = loadSql("$sqlClasspath/requests/retrieve_account_request_by_uuid.sql")
    private val sqlRetrieveAllAccountRequests = loadSql("$sqlClasspath/requests/retrieve_all_account_requests.sql")
    private val sqlApproveAccountRequest = loadSql("$sqlClasspath/requests/approve_account_request.sql")
    private val sqlRejectAccountRequest = loadSql("$sqlClasspath/requests/reject_account_request.sql")

    private val sqlRetrievePendingAccountRequests = loadSql("$sqlClasspath/requests/retrieve_pending_account_requests.sql")
    private val sqlRetrieveApprovedAccountRequests = loadSql("$sqlClasspath/requests/retrieve_approved_account_requests.sql")
    private val sqlRetrieveRejectedAccountRequests = loadSql("$sqlClasspath/requests/retrieve_rejected_account_requests.sql")

    fun conn(): Connection = connectionManager.connect()

    init{
        val conn = conn()
        try {
            executeUpdate(conn, sqlCreateTableAccountRequests)
            commitAndClose(conn)
        }
        finally{
            rollbackAndClose(conn, connectionManager)
        }
    }

    override fun storeAccountRequest(request: AccountRequest) {
        if( !Objects.equals(request.password, request.confirmPassword) ){
            throw Exception(ERROR_PASSWORD_MISMATCH)
        }

        verifyNoUserExists(request.uuid)

        synchronized(concurrencyLock) {
            accountRequestUserInfoDB.storeUser(request.user, request.uuid)

            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlStoreAccountRequest)
                val approved = if (request.approved) 1 else 0
                val rejected = if (request.rejected) 1 else 0

                statement.setString(1, request.password)
                statement.setString(2, request.reasonForAccount)
                statement.setString(3, request.vouchName)
                statement.setString(4, request.vouchContactInfo)
                statement.setInt(5, approved)
                statement.setString(6, request.approvedByUuid)
                statement.setLong(7, request.approvedTimestamp)
                statement.setInt(8, rejected)
                statement.setString(9, request.rejectedByUuid)
                statement.setLong(10, request.rejectedTimestamp)
                statement.setString(11, request.rejectedReason)
                statement.setString(12, request.uuid)

                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn, connectionManager)
            }
        }
    }

    override fun retrieveAccountRequest(uuid: String): AccountRequest? {
        val conn = conn()

        try {
            var result: AccountRequest? = null
            val statement = conn.prepareStatement(sqlRetrieveAccountRequestByUuid)
            statement.setString(1, uuid)

            val resultSet = statement.executeQuery()
            if (resultSet != null) {
                if (resultSet.next()) {
                    result = unmarshalAccountRequest(resultSet)
                }
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn, connectionManager)
        }
    }


    override fun retrieveAccountRequests(): List<AccountRequest> {
        return retrieveAccountRequests(sqlRetrieveAllAccountRequests)
    }

    override fun retrievePendingAccountRequests() : List<AccountRequest>{
        return retrieveAccountRequests(sqlRetrievePendingAccountRequests)
    }

    override fun retrieveApprovedAccountRequests() : List<AccountRequest>{
        return retrieveAccountRequests(sqlRetrieveApprovedAccountRequests)
    }

    override fun retrieveRejectedAccountRequests() : List<AccountRequest>{
        return retrieveAccountRequests(sqlRetrieveRejectedAccountRequests)
    }

    private fun retrieveAccountRequests( sql: String ): List<AccountRequest> {
        val conn = conn()

        try {
            val result = mutableListOf<AccountRequest>()
            val statement = conn.prepareStatement(sql)

            val resultSet = statement.executeQuery()
            if (resultSet != null) {
                while (resultSet.next()) {
                    result.add(unmarshalAccountRequest(resultSet))
                }
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn, connectionManager)
        }
    }

    override fun approve( uuid: String, approvedByUuid: String, timestamp: Long ){
        verifyNoUserExists(uuid)

        val accountRequest = retrieveAccountRequest(uuid) ?: throw Exception( ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID )
        if( accountRequest.approved ){
            return
        }

        synchronized(concurrencyLock) {
            userDB.storeUser(accountRequest.user)

            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlApproveAccountRequest)
                statement.setString(1, approvedByUuid)
                statement.setLong(2, timestamp)
                statement.setString(3, uuid)

                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn, connectionManager)
            }
        }
    }

    override fun reject( uuid: String, rejectedByUuid: String, reason: String, timestamp: Long ){
        val accountRequest = retrieveAccountRequest(uuid) ?: throw Exception( ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID )

        if( accountRequest.rejected ){
            return
        }

        if( accountRequest.approved ){
            throw Exception(ERROR_ALREADY_APPROVED)
        }

        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlRejectAccountRequest)
                statement.setString(1, rejectedByUuid)
                statement.setLong(2, timestamp)
                statement.setString(3, reason)
                statement.setString(4, uuid)

                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn, connectionManager)
            }
        }
    }

    private fun verifyNoUserExists( uuid: String ){
        if( userDB.userExists(uuid) ){
            throw Exception( ERROR_USER_WITH_THIS_UUID_ALREADY_EXISTS )
        }
    }

    private fun unmarshalAccountRequest(hit: ResultSet): AccountRequest{
        val uuid = hit.getString("uuid")
        val approved = hit.getInt("approved") == 1
        val rejected = hit.getInt("rejected") == 1

        val user = accountRequestUserInfoDB.retrieveUser(uuid) ?: throw Exception( ERROR_NO_USER_WITH_THIS_UUID_EXISTS )

        return AccountRequest(
                user = user,
                password = hit.getString("password"),
                confirmPassword = hit.getString("password"),
                reasonForAccount = hit.getString("reasonForAccount"),
                vouchName = hit.getString("vouchName"),
                vouchContactInfo = hit.getString("vouchContactInfo"),
                approved = approved,
                approvedByUuid = hit.getString("approvedByUuid"),
                approvedTimestamp = hit.getLong("approvedTimestamp"),
                rejected = rejected,
                rejectedByUuid = hit.getString("rejectedByUuid"),
                rejectedTimestamp = hit.getLong("rejectedTimestamp"),
                rejectedReason = hit.getString("rejectedReason"),
                uuid = uuid
        )
    }
}