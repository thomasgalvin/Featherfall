package galvin.dw

interface UserDB {
    //roles
    fun storeRole(role: Role)
    fun deactivate( roleName: String )
    fun activate( roleName: String )
    fun listRoles(): List<Role>
    fun retrieveRole(name: String): Role?

    //users
    fun storeUser(user: User, uuid: String? = null)
    fun retrieveUser(uuid: String): User?
    fun retrieveUsers(): List<User>
}

interface AccountRequestDB {
    fun storeAccountRequest( request: AccountRequest )
    fun retrieveAccountRequest( uuid: String ) : AccountRequest?
    fun retrieveAccountRequests() : List<AccountRequest>

    fun approve( uuid: String, approvedByUuid: String, timestamp: Long = System.currentTimeMillis() )
}

data class User(
        //login credentials
        val login: String, val passwordHash: String?,

        //human name
        val name: String, //full legal name, eg "John Smith"
        val displayName:  String,//on-screen nickname, eg "John" or "Tigerpunch2010"
        val sortName: String, //name in a sortable order, eg "Smith, John"
        val prependToName: String?, // this is used to store stuff like "Mr." or "Dr."
        val appendToName: String?, // used for stuff like rank, eg "Major General of the Fell Armies of Nod"

        //smart card info
        val credential: String?, val serialNumber: String?, val distinguishedName: String?,
        val homeAgency: String?, val agency: String?, val countryCode: String?,
        val citizenship: String?,

        //activation
        val created: Long, val active: Boolean, val locked: Boolean = false,

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
        val approved: Boolean = false, val approvedByUuid: String?, val approvedTimestamp: Long = -1,
        val rejected: Boolean = false, val rejectedByUuid: String?, val rejectedTimestamp: Long = -1,

        //uuid
        val uuid: String = uuid()
)

data class Role( val name: String, val permissions: List<String>, val active: Boolean = true )

data class ContactInfo( val type: String, //eg "Phone", "Email", or "Mattermost"
                        val description: String, //eg "Work Email" or "Cell Phone"
                        val contact: String, //eg spam@devnull.com or 555.555.5555
                        val primary: Boolean //flags this as the best way to contact the user for the given type
)


