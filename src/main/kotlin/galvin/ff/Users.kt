package galvin.ff

import galvin.ff.db.QuietCloser
import galvin.ff.db.ConnectionManager
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.util.*

const val ERROR_PASSWORD_MISMATCH = "Account Request error: password mismatch"
const val ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID = "Account Request error: no account request with that UUID"
const val ERROR_USER_WITH_THIS_UUID_ALREADY_EXISTS = "Account Request error: user with this UUID already exists"
const val ERROR_USER_WITH_THIS_LOGIN_ALREADY_EXISTS = "Account Request error: user with this login already exists"
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
    fun userExistsByLogin(login: String): Boolean

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

    fun approve( uuid: String, approvedByUuid: String, timestamp: Long )
    fun reject( uuid: String, rejectedByUuid: String, reason: String = "", timestamp: Long )

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
    val credentials: CertificateData get() = doGetCredentials()

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

    fun doGetCredentials(): CertificateData{
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
        val reasonForAccount: String? = null, //an explanation for why this account is being requested
        val vouchName: String? = null, //the name of someone who can vouch for you
        val vouchContactInfo: String? = null, //how to get ahold of the person who can vouch for you

        //approved / rejected
        val approved: Boolean = false, val approvedByUuid: String = "", val approvedTimestamp: Long = -1,
        val rejected: Boolean = false, val rejectedByUuid: String = "", val rejectedTimestamp: Long = -1,
        val rejectedReason: String = "",

        //uuid
        val uuid: String = uuid()
){
    fun withLogin(login: String): AccountRequest{
        return copy( user = user.copy(login = login) )
    }
}

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
    override fun userExistsByLogin(login: String): Boolean {return false}

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

    private val sqlCreateTableRoles = loadFromClasspathOrThrow("$sqlClasspath/roles/create_table_roles.sql")
    private val sqlCreateTableRolePermissions = loadFromClasspathOrThrow("$sqlClasspath/roles/create_table_role_permissions.sql")
    private val sqlCreateTableUsers = loadFromClasspathOrThrow("$sqlClasspath/users/create_table_users.sql")
    private val sqlCreateTableContactInfo = loadFromClasspathOrThrow("$sqlClasspath/users/create_table_contact_info.sql")
    private val sqlCreateTableUserRoles = loadFromClasspathOrThrow("$sqlClasspath/users/create_table_user_roles.sql")

    private val sqlDeleteRole = loadFromClasspathOrThrow("$sqlClasspath/roles/delete_role.sql")
    private val sqlDeleteRolePermissions = loadFromClasspathOrThrow("$sqlClasspath/roles/delete_role_permissions.sql")

    private val sqlStoreRole = loadFromClasspathOrThrow("$sqlClasspath/roles/store_role.sql")
    private val sqlStoreRolePermission = loadFromClasspathOrThrow("$sqlClasspath/roles/store_role_permission.sql")
    private val sqlRetrieveAllRoles = loadFromClasspathOrThrow("$sqlClasspath/roles/retrieve_all_roles.sql")
    private val sqlRetrieveRoleByUuid = loadFromClasspathOrThrow("$sqlClasspath/roles/retrieve_role_by_uuid.sql")
    private val sqlRetrieveRolePermissions = loadFromClasspathOrThrow("$sqlClasspath/roles/retrieve_role_permissions.sql")

    private val sqlSetRoleActive = loadFromClasspathOrThrow("$sqlClasspath/roles/set_role_active.sql")

    private val sqlStoreUser = loadFromClasspathOrThrow("$sqlClasspath/users/store_user.sql")
    private val sqlStoreUserContactInfo = loadFromClasspathOrThrow("$sqlClasspath/users/store_user_contact_info.sql")
    private val sqlStoreUserRole = loadFromClasspathOrThrow("$sqlClasspath/users/store_user_roles.sql")

    private val sqlUserExistsByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/user_exists_by_uuid.sql")
    private val sqlUserExistsByLogin = loadFromClasspathOrThrow("$sqlClasspath/users/user_exists_by_login.sql")
    private val sqlRetrieveUserByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_user_by_uuid.sql")
    private val sqlRetrieveUserBySerialNumber = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_user_by_serial_number.sql")
    private val sqlRetrieveUserByLogin= loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_user_by_login.sql")
    private val sqlRetrieveAllUsers = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_all_users.sql")

    private val sqlRetrieveContactInfoForUser = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_contact_info_for_user.sql")
    private val sqlRetrieveRolesForUser = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_roles_for_user.sql")

    private val sqlRetrieveUuidByLogin = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_uuid_by_login.sql")
    private val sqlRetrieveUuidBySerialNumber = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_uuid_by_serial_number.sql")

    private val sqlRetrieveUsersByLocked = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_users_by_locked.sql")
    private val sqlSetLockedByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/set_locked_by_uuid.sql")
    private val sqlSetLockedByLogin = loadFromClasspathOrThrow("$sqlClasspath/users/set_locked_by_login.sql")
    private val sqlIsLockedByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/is_locked_by_uuid.sql")
    private val sqlIsLockedByLogin = loadFromClasspathOrThrow("$sqlClasspath/users/is_locked_by_login.sql")

    private val sqlRetrieveUsersByActive = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_users_by_active.sql")
    private val sqlSetActiveByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/set_active_by_uuid.sql")
    private val sqlSetActiveByLogin = loadFromClasspathOrThrow("$sqlClasspath/users/set_active_by_login.sql")
    private val sqlIsActiveByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/is_active_by_uuid.sql")
    private val sqlIsActiveByLogin = loadFromClasspathOrThrow("$sqlClasspath/users/is_active_by_login.sql")

    private val sqlDeleteContactInfoForUser = loadFromClasspathOrThrow("$sqlClasspath/users/delete_contact_info_for_user.sql")
    private val sqlDeleteRolesForUser = loadFromClasspathOrThrow("$sqlClasspath/users/delete_roles_for_user.sql")

    private val sqlSetPasswordByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/set_password_by_uuid.sql")
    private val sqlSetPasswordByLogin = loadFromClasspathOrThrow("$sqlClasspath/users/set_password_by_login.sql")
    private val sqlRetrievePasswordHashByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/get_password_hash_by_uuid.sql")

    private val sqlUpdateCredentialsByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/update_credentials_by_uuid.sql")
    private val sqlRetrieveCredentialsByUuid = loadFromClasspathOrThrow("$sqlClasspath/users/retrieve_credentials_by_uuid.sql")

    fun conn(): Connection = connectionManager.connect()

    init{
        val conn = conn()
        try {
            executeUpdate(conn, sqlCreateTableRoles)
            executeUpdate(conn, sqlCreateTableRolePermissions)
            executeUpdate(conn, sqlCreateTableUsers)
            executeUpdate(conn, sqlCreateTableContactInfo)
            executeUpdate(conn, sqlCreateTableUserRoles)
            conn.commit()
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }

    //
    // Roles
    //

    override fun storeRole(role: Role){
        synchronized(concurrencyLock) {
            val conn = conn()
            try {
                val deletePermissionsStatement = conn.prepareStatement(sqlDeleteRolePermissions)
                val deleteRoleStatement = conn.prepareStatement(sqlDeleteRole)
                val storeStatement = conn.prepareStatement(sqlStoreRole)

                try {
                    deletePermissionsStatement.setString(1, role.name)
                    deletePermissionsStatement.executeUpdate()


                    deleteRoleStatement.setString(1, role.name)
                    deleteRoleStatement.executeUpdate()

                    val active = if(role.active) 1 else 0
                    storeStatement.setString(1, role.name)
                    storeStatement.setInt(2, active)
                    storeStatement.executeUpdate()

                    for((ordinal, permission) in role.permissions.withIndex()) {
                        val permStatement = conn.prepareStatement(sqlStoreRolePermission)
                        try {
                            permStatement.setString(1, role.name)
                            permStatement.setString(2, permission)
                            permStatement.setInt(3, ordinal)
                            permStatement.executeUpdate()
                        } finally{ QuietCloser.close(permStatement) }
                    }

                    conn.commit()
                } finally{ QuietCloser.close(deletePermissionsStatement, deleteRoleStatement, storeStatement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
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
                try {
                    val value = if(active) 1 else 0
                    statement.setInt(1, value)
                    statement.setString(2, roleName)
                    statement.executeUpdate()
                    conn.commit()
                } finally{ QuietCloser.close(statement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
        }
    }

    override fun listRoles(): List<Role>{
        val conn = conn()
        try{
            val result = mutableListOf<Role>()
            val statement = conn.prepareStatement(sqlRetrieveAllRoles)
            try {
                val resultSet = statement.executeQuery()
                if(resultSet != null) {
                    while(resultSet.next()) {
                        result.add(unmarshalRole(resultSet, conn))
                    }
                }
                return result
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }

    override fun retrieveRole(name: String): Role?{
        val conn = conn()
        try{
            val statement = conn.prepareStatement(sqlRetrieveRoleByUuid)
            try {
                statement.setString(1, name)

                val resultSet = statement.executeQuery()
                try {
                    if(resultSet != null) {
                        if(resultSet.next()) {
                            return unmarshalRole(resultSet, conn)
                        }
                    }
                } finally{ QuietCloser.close(resultSet) }

                return null
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }

    private fun unmarshalRole(hit: ResultSet, conn: Connection): Role {
        val name = hit.getString(1)
        val active = hit.getInt(2) != 0
        val permissions = mutableListOf<String>()

        val statement = conn.prepareStatement(sqlRetrieveRolePermissions)
        try {
            statement.setString(1, name)

            val permissionHits = statement.executeQuery()
            try {
                if(permissionHits != null) {
                    while(permissionHits.next()) {
                        permissions.add(permissionHits.getString(1))
                    }
                }
            } finally{ QuietCloser.close(permissionHits) }

            return Role(name, permissions, active)
        } finally{ QuietCloser.close(statement) }
    }

    override fun retrievePermissions( roleNames: List<String>): List<String>{
        val result = mutableListOf<String>()

        for( roleName in roleNames ){
            val role = retrieveRole(roleName) //TODO: reuse a single connection for this
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
                val delRolesStatement = conn.prepareStatement(sqlDeleteRolesForUser)
                val storeStatement = conn.prepareStatement(sqlStoreUser)

                try {
                    delContactStatement.setString(1, theUuid)
                    delContactStatement.executeUpdate()

                    delRolesStatement.setString(1, theUuid)
                    delRolesStatement.executeUpdate()

                    val active = if (user.active) 1 else 0
                    val locked = if (user.locked) 1 else 0

                    storeStatement.setString(1, user.login)
                    storeStatement.setString(2, user.login.toLowerCase())
                    storeStatement.setString(3, user.passwordHash)
                    storeStatement.setString(4, user.name)
                    storeStatement.setString(5, user.displayName)
                    storeStatement.setString(6, user.sortName)
                    storeStatement.setString(7, user.prependToName)
                    storeStatement.setString(8, user.appendToName)
                    storeStatement.setString(9, user.credential)
                    storeStatement.setString(10, user.serialNumber)
                    storeStatement.setString(11, user.distinguishedName)
                    storeStatement.setString(12, user.homeAgency)
                    storeStatement.setString(13, user.agency)
                    storeStatement.setString(14, user.countryCode)
                    storeStatement.setString(15, user.citizenship)
                    storeStatement.setLong(16, user.created)
                    storeStatement.setInt(17, active)
                    storeStatement.setInt(18, locked)
                    storeStatement.setString(19, theUuid)
                    storeStatement.executeUpdate()

                    for ((ordinal, contact) in user.contact.withIndex()) {
                        val isPrimary = if (contact.primary) 1 else 0

                        val contactStatement = conn.prepareStatement(sqlStoreUserContactInfo)
                        try {
                            contactStatement.setString(1, contact.type)
                            contactStatement.setString(2, contact.description)
                            contactStatement.setString(3, contact.contact)
                            contactStatement.setInt(4, isPrimary)
                            contactStatement.setString(5, theUuid)
                            contactStatement.setInt(6, ordinal)
                            contactStatement.executeUpdate()
                        } finally{ QuietCloser.close(contactStatement) }
                    }

                    for ((ordinal, role) in user.roles.withIndex()) {
                        val roleStatement = conn.prepareStatement(sqlStoreUserRole)
                        try {
                            roleStatement.setString(1, role)
                            roleStatement.setString(2, theUuid)
                            roleStatement.setInt(3, ordinal)
                            roleStatement.executeUpdate()
                        } finally{ QuietCloser.close(roleStatement) }
                    }

                    conn.commit()
                } finally{ QuietCloser.close(delContactStatement, delRolesStatement, storeStatement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
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
            try {
                if(intFlag != null) {
                    statement.setInt(1, intFlag)
                }

                val resultSet = statement.executeQuery()
                try {
                    if(resultSet != null) {
                        while(resultSet.next()) {
                            result.add(unmarshalUser(resultSet, conn))
                        }
                    }
                    return result
                } finally{ QuietCloser.close(resultSet) }
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }

    override fun retrieveUser(uuid: String): User?{
        return retrieveUserBy(sqlRetrieveUserByUuid, uuid)
    }

    override fun retrieveUserBySerialNumber(serialNumber: String): User? {
        return retrieveUserBy(sqlRetrieveUserBySerialNumber, serialNumber)
    }

    override fun retrieveUserByLogin(login: String): User?{
        return retrieveUserBy( sqlRetrieveUserByLogin, login.toLowerCase() )
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
            val statement = conn.prepareStatement(sql)
            try {
                statement.setString(1, value)

                val resultSet = statement.executeQuery()
                try {
                    if(resultSet != null) {
                        if(resultSet.next()) {
                            return unmarshalUser(resultSet, conn)
                        }
                    }
                } finally{ QuietCloser.close(resultSet) }

                return null
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }

    private fun unmarshalUser(hit: ResultSet, conn: Connection): User {
        val uuid = hit.getString("uuid")

        val contact: MutableList<ContactInfo> = mutableListOf()
        val roles: MutableList<String> = mutableListOf()
        val active = hit.getInt("active") != 0
        val locked = hit.getInt("locked") != 0

        val contactStatement = conn.prepareStatement(sqlRetrieveContactInfoForUser )
        val roleStatement = conn.prepareStatement(sqlRetrieveRolesForUser)
        try {
            contactStatement.setString(1, uuid)
            val conHits = contactStatement.executeQuery()
            try {
                if(conHits != null) {
                    while(conHits.next()) {
                        contact.add(unmarshalContact(conHits))
                    }
                }
            } finally{ QuietCloser.close(conHits) }

            roleStatement.setString(1, uuid)
            val roleHits = roleStatement.executeQuery()
            try {
                if(roleHits != null) {
                    while(roleHits.next()) {
                        roles.add(roleHits.getString(1))
                    }
                }
            } finally{ QuietCloser.close(conHits) }

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
        } finally{ QuietCloser.close(contactStatement, roleStatement ) }
    }

    override fun retrieveUuid(key: String): String?{
        var uuid = neverNull( retrieveUuidByLogin(key) )
        if( isBlank(uuid) ){
            uuid = neverNull( retrieveUuidBySerialNumber(key) )
        }
        return uuid
    }

    override fun retrieveUuidByLogin(login: String): String?{
        return retrieveUuidBy( sqlRetrieveUuidByLogin, login.toLowerCase() )
    }

    override fun retrieveUuidBySerialNumber(serial: String): String?{
        return retrieveUuidBy(sqlRetrieveUuidBySerialNumber, serial)
    }

    private fun retrieveUuidBy(sql: String, key: String): String?{
        val conn = conn()
        try {
            val statement = conn.prepareStatement(sql)
            try {
                statement.setString(1, key)
                val results = statement.executeQuery()
                try {
                    if(results.next()) {
                        return results.getString("uuid")
                    }
                } finally{ QuietCloser.close(results) }

                return null
            }finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
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
        return userExistsBy(sqlUserExistsByUuid, uuid)
    }

    override fun userExistsByLogin(login: String): Boolean{
        return userExistsBy( sqlUserExistsByLogin, login.toLowerCase() )
    }

    private fun userExistsBy(sql: String, value: String): Boolean{
        val conn = conn()
        try {
            val statement = conn.prepareStatement(sql)
            try {
                statement.setString(1, value)
                val resultSet = statement.executeQuery()
                try{ return resultSet.next() }
                finally{ QuietCloser.close(resultSet) }
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
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
                try {
                    val lockedValue = if(locked) 1 else 0
                    statement.setInt(1, lockedValue)
                    statement.setString(2, key)
                    statement.executeUpdate()
                    conn.commit()
                } finally{ QuietCloser.close(statement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
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
            val statement = conn.prepareStatement(sql)
            try {
                statement.setString(1, key)
                val results = statement.executeQuery()
                try {
                    if(results.next()) {
                        val locked = results.getInt("locked")
                        return locked != 0
                    }
                } finally{ QuietCloser.close(results) }

                return false
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
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
                try {
                    val activeValue = if(active) 1 else 0
                    statement.setInt(1, activeValue)
                    statement.setString(2, key)
                    statement.executeUpdate()
                    conn.commit()
                } finally{ QuietCloser.close(statement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
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
            val statement = conn.prepareStatement(sql)
            try {
                statement.setString(1, key)
                val results = statement.executeQuery()
                try {
                    if(results.next()) {
                        val active = results.getInt("active")
                        return active != 0
                    }
                } finally{ QuietCloser.close(results) }

                return false
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }

    override fun setPasswordByUuid( uuid: String, plainTextPassword: String ){
        setPasswordBy(sqlSetPasswordByUuid, uuid, plainTextPassword)
    }

    override fun setPasswordByLogin( login: String, plainTextPassword: String ){
        setPasswordBy(sqlSetPasswordByLogin, login.toLowerCase(), plainTextPassword)
    }

    private fun setPasswordBy( sql: String, uuid: String, plainTextPassword: String ){
        synchronized(concurrencyLock) {
            val hash = hash(plainTextPassword)
            val conn = conn()
            try {
                val statement = conn.prepareStatement(sql)
                try {
                    statement.setString(1, hash)
                    statement.setString(2, uuid)
                    statement.executeUpdate()
                    conn.commit()
                } finally{ QuietCloser.close(statement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
        }
    }

    override fun retrievePasswordHash(uuid: String): String{
        val conn = conn()
        try {
            val statement = conn.prepareStatement(sqlRetrievePasswordHashByUuid)
            try {
                statement.setString(1, uuid)
                val results = statement.executeQuery()
                try {
                    if(results.next()) {
                        return results.getString("passwordHash")
                    }
                } finally{ QuietCloser.close(results) }
            } finally{ QuietCloser.close(statement) }

            return ""
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
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
                try {
                    statement.setString(1, credentials.credential)
                    statement.setString(2, credentials.serialNumber)
                    statement.setString(3, credentials.distinguishedName)
                    statement.setString(4, credentials.countryCode)
                    statement.setString(5, credentials.citizenship)
                    statement.setString(6, uuid)
                    statement.executeUpdate()
                    conn.commit()
                } finally{ QuietCloser.close(statement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
        }
    }

    override fun retrieveCredentials( uuid: String ): CertificateData?{
        val conn = conn()
        try {
            val statement = conn.prepareStatement(sqlRetrieveCredentialsByUuid)
            try {
                statement.setString(1, uuid)
                val results = statement.executeQuery()
                try {
                    if(results.next()) {
                        return CertificateData(
                                credential = results.getString("credential"),
                                serialNumber = results.getString("serialNumber"),
                                distinguishedName = results.getString("distinguishedName"),
                                countryCode = results.getString("countryCode"),
                                citizenship = results.getString("citizenship")
                        )
                    }
                } finally{ QuietCloser.close(results) }

                return null
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }
}

class AccountRequestDBImpl( private val connectionManager: ConnectionManager,
                            private val userDB: UserDB,
                            private val accountRequestUserInfoDB: UserDB,
                            sqlClasspath: String ) : AccountRequestDB {
    private val concurrencyLock = Object()
    
    private val sqlCreateTableAccountRequests = loadFromClasspathOrThrow("$sqlClasspath/requests/create_table_account_requests.sql")
    private val sqlStoreAccountRequest = loadFromClasspathOrThrow("$sqlClasspath/requests/store_account_request.sql")
    private val sqlRetrieveAccountRequestByUuid = loadFromClasspathOrThrow("$sqlClasspath/requests/retrieve_account_request_by_uuid.sql")
    private val sqlRetrieveAllAccountRequests = loadFromClasspathOrThrow("$sqlClasspath/requests/retrieve_all_account_requests.sql")
    private val sqlApproveAccountRequest = loadFromClasspathOrThrow("$sqlClasspath/requests/approve_account_request.sql")
    private val sqlRejectAccountRequest = loadFromClasspathOrThrow("$sqlClasspath/requests/reject_account_request.sql")

    private val sqlRetrievePendingAccountRequests = loadFromClasspathOrThrow("$sqlClasspath/requests/retrieve_pending_account_requests.sql")
    private val sqlRetrieveApprovedAccountRequests = loadFromClasspathOrThrow("$sqlClasspath/requests/retrieve_approved_account_requests.sql")
    private val sqlRetrieveRejectedAccountRequests = loadFromClasspathOrThrow("$sqlClasspath/requests/retrieve_rejected_account_requests.sql")

    fun conn(): Connection = connectionManager.connect()

    init{
        val conn = conn()
        try {
            executeUpdate(conn, sqlCreateTableAccountRequests)
            conn.commit()
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }

    override fun storeAccountRequest(request: AccountRequest) {
        if( !Objects.equals(request.password, request.confirmPassword) ){
            throw Exception(ERROR_PASSWORD_MISMATCH)
        }

        verifyNoUserExists(request)

        synchronized(concurrencyLock) {
            accountRequestUserInfoDB.storeUser(request.user, request.uuid)

            val conn = conn()
            try {
                val statement = conn.prepareStatement(sqlStoreAccountRequest)
                try {
                    val approved = if(request.approved) 1 else 0
                    val rejected = if(request.rejected) 1 else 0

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
                    statement.executeUpdate()
                    conn.commit()
                } finally{ QuietCloser.close(statement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
        }
    }

    override fun retrieveAccountRequest(uuid: String): AccountRequest? {
        val conn = conn()
        try {
            val statement = conn.prepareStatement(sqlRetrieveAccountRequestByUuid)
            try {
                statement.setString(1, uuid)

                val resultSet = statement.executeQuery()
                try {
                    if(resultSet != null) {
                        if(resultSet.next()) {
                            return unmarshalAccountRequest(resultSet)
                        }
                    }
                } finally{ QuietCloser.close(resultSet) }

                return null
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
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
            try {
                val resultSet = statement.executeQuery()
                try {
                    if(resultSet != null) {
                        while(resultSet.next()) {
                            result.add(unmarshalAccountRequest(resultSet))
                        }
                    }
                } finally{ QuietCloser.close(resultSet) }

                return result
            } finally{ QuietCloser.close(statement) }
        }
        catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
        finally{ closeAndRelease(conn, connectionManager) }
    }

    override fun approve( uuid: String, approvedByUuid: String, timestamp: Long ){
        val accountRequest = retrieveAccountRequest(uuid) ?: throw Exception( ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID )
        if( accountRequest.approved ){
            return
        }

        verifyNoUserExists(accountRequest)

        synchronized(concurrencyLock) {
            val passwordHash = hash( accountRequest.password )
            val newUser = accountRequest.user.copy( passwordHash = passwordHash )
            userDB.storeUser(newUser)

            val conn = conn()
            try {
                val statement = conn.prepareStatement(sqlApproveAccountRequest)
                try {
                    statement.setString(1, approvedByUuid)
                    statement.setLong(2, timestamp)
                    statement.setString(3, uuid)
                    statement.executeUpdate()
                    conn.commit()
                } finally{ QuietCloser.close(statement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
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
                try {
                    statement.setString(1, rejectedByUuid)
                    statement.setLong(2, timestamp)
                    statement.setString(3, reason)
                    statement.setString(4, uuid)
                    statement.executeUpdate()
                    conn.commit()
                } finally{ QuietCloser.close(statement) }
            }
            catch( t: Throwable ){ rollbackAndRelease(conn, connectionManager); throw t }
            finally{ closeAndRelease(conn, connectionManager) }
        }
    }

    private fun verifyNoUserExists( accountRequest: AccountRequest ){
        if( userDB.userExists( accountRequest.uuid ) ){
            throw Exception( ERROR_USER_WITH_THIS_UUID_ALREADY_EXISTS )
        }

        if( userDB.userExistsByLogin( accountRequest.user.login ) ){
            throw Exception( ERROR_USER_WITH_THIS_LOGIN_ALREADY_EXISTS )
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