package galvin.ff

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

    fun retrievePermissions( roleNames: List<String>): List<String>

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
}

data class User(
        //login credentials
        val login: String, val passwordHash: String? = "",

        //human name
        val name: String, //full legal name, eg "John Smith"
        val displayName:  String,//on-screen nickname, eg "John" or "Tigerpunch2010"
        val sortName: String, //name in a sortable order, eg "Smith, John"
        val prependToName: String? = "", // this is used to storeSystemInfo stuff like "Mr." or "Dr."
        val appendToName: String? = "", // used for stuff like rank, eg "Major General of the Fell Armies of Nod"

        //smart card info
        val credential: String? = "", val serialNumber: String? = "",
        val distinguishedName: String? = "", val homeAgency: String? = "",
        val agency: String? = "", val countryCode: String? = "",
        val citizenship: String? = "",

        //activation
        val created: Long, val active: Boolean, val locked: Boolean = false,

        //uuid
        val uuid: String = uuid(),

        //contact info
        val contact: List<ContactInfo> = listOf<ContactInfo>(),

        //roles
        val roles: List<String> = listOf<String>()
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

    private fun copyContact(): List<ContactInfo>{
        val newContact = mutableListOf<ContactInfo>()
        newContact.addAll(contact)
        return newContact
    }

    private fun copyRoles(): List<String>{
        val newRoles = mutableListOf<String>()
        newRoles.addAll(roles)
        return newRoles
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

data class Role( val name: String, val permissions: List<String>, val active: Boolean = true )

data class ContactInfo( val type: String, //eg "Phone", "Email", or "Mattermost"
                        val description: String, //eg "Work Email" or "Cell Phone"
                        val contact: String, //eg spam@devnull.com or 555.555.5555
                        val primary: Boolean //flags this as the best way to contact the user for the given type
)
