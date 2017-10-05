package galvin.dw

import java.io.IOException

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

    fun setLocked( uuid: String, locked: Boolean )
    fun setLockedByLogin( login: String, locked: Boolean )

    fun isLocked( uuid: String ): Boolean
    fun isLockedByLogin( login: String ): Boolean
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
        val login: String, val passwordHash: String?,

        //human name
        val name: String, //full legal name, eg "John Smith"
        val displayName:  String,//on-screen nickname, eg "John" or "Tigerpunch2010"
        val sortName: String, //name in a sortable order, eg "Smith, John"
        val prependToName: String?, // this is used to storeSystemInfo stuff like "Mr." or "Dr."
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
){
    fun withoutPasswordHash(): User{
        val newPasswordHash: String? = null

        return User(
                login,
                newPasswordHash,
                name,
                displayName,
                sortName,
                prependToName,
                appendToName,
                credential,
                serialNumber,
                distinguishedName,
                homeAgency,
                agency,
                countryCode,
                citizenship,
                created,
                active,
                locked,
                uuid,
                copyContact(),
                copyRoles()
        )
    }

    fun withPasswordHash( passwordHash: String ): User{
        return User(
                login,
                passwordHash,
                name,
                displayName,
                sortName,
                prependToName,
                appendToName,
                credential,
                serialNumber,
                distinguishedName,
                homeAgency,
                agency,
                countryCode,
                citizenship,
                created,
                active,
                locked,
                uuid,
                copyContact(),
                copyRoles()
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

const val SPECIAL_CHARACTER_SET = "`~!@#$%^&*()_+-={}|:\"<>?[]\\;',./"

data class PasswordValidation( val passwordEmpty: Boolean = false,
                               val passwordTooShort: Boolean = false,
                               val tooFewLowerCase: Boolean = false,
                               val tooFewUpperCase: Boolean = false,
                               val tooFewDigits: Boolean = false,
                               val tooFewSpecialCharacters: Boolean = false,
                               val repeatedCharacters: Boolean = false,
                               val foundOnBlacklist: Boolean = false,
                               val passwordMismatch: Boolean = false ){
    fun invalidPassword(): Boolean{
        return     passwordEmpty
                || passwordTooShort
                || tooFewLowerCase
                || tooFewUpperCase
                || tooFewDigits
                || tooFewSpecialCharacters
                || repeatedCharacters
                || foundOnBlacklist
                || passwordMismatch
    }
}

data class PasswordRequirements(val minLength: Int = 0,
                                val minLowerCase: Int = 0,
                                val minUpperCase: Int = 0,
                                val minDigits: Int = 0,
                                val minSepcialCharacters: Int = 0,
                                val repeatedCharactersAllowed: Boolean = true,
                                val validatedAgainstBlacklist: Boolean = true ){
    private val BAD_PASSWORD_FILE = "galvin/dw/bad-passwords.txt"
    private val badPasswords = mutableMapOf<String, Boolean>()

    fun validate( password: String ): PasswordValidation {
        val chars = password.toCharArray()
        val foundOnBlacklist = if(!validatedAgainstBlacklist) false else foundOnBlacklist(password)
        val repeatedCharacters = if(!repeatedCharactersAllowed) repeatedCharacters(chars) else false

        return PasswordValidation(
                passwordEmpty = isBlank(password),
                passwordTooShort = tooShort(password),
                tooFewLowerCase = tooFewLowerCase(chars),
                tooFewUpperCase = tooFewUpperCase(chars),
                tooFewDigits = tooFewDigits(chars),
                tooFewSpecialCharacters = tooFewSpecialCharacters(chars),
                repeatedCharacters = repeatedCharacters,
                foundOnBlacklist = foundOnBlacklist )
    }

    private fun tooShort( password: String ): Boolean{
        return password.length < minLength
    }

    private fun tooFewLowerCase(chars: CharArray): Boolean {
        var count = 0

        for (c in chars) {
            if (Character.isLowerCase(c)) {
                count++
            }
        }

        return count < minLowerCase
    }

    private fun tooFewUpperCase(chars: CharArray): Boolean {
        var count = 0

        for (c in chars) {
            if (Character.isUpperCase(c)) {
                count++
            }
        }

        return count < minUpperCase
    }

    private fun tooFewDigits(chars: CharArray): Boolean {
        var count = 0

        for (c in chars) {
            if (Character.isDigit(c)) {
                count++
            }
        }

        return count < minDigits
    }

    private fun tooFewSpecialCharacters(chars: CharArray): Boolean {
        var count = 0

        for (c in chars) {
            if (!Character.isAlphabetic(c.toInt()) && !Character.isDigit(c)) {
                count++
            }
        }

        return count < minSepcialCharacters
    }

    private fun repeatedCharacters(chars: CharArray): Boolean {
        if (chars.size < 2) {
            return false
        }

        var previous = chars[0]
        for (i in 1 until chars.size) {
            if (previous == chars[i]) {
                return true
            }
            previous = chars[i]
        }

        return false
    }

    private fun foundOnBlacklist(password: String): Boolean {
        doLoadBlacklist()
        return badPasswords.containsKey(password)
    }

    private fun doLoadBlacklist() {
        synchronized(this) {
            if (badPasswords.isEmpty()) {
                try {
                    val lines = loadBlacklist()
                    for (line in lines) {
                        badPasswords.put(line, java.lang.Boolean.TRUE)
                    }
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                    println("Unable to load password blacklist" )
                }

            }
        }
    }

    fun loadBlacklist(): List<String> {
        val list = loadResourceAndReadLines(BAD_PASSWORD_FILE)
        val badPasswords = mutableListOf<String>()
        for( line in list ){
            if( !isBlank(line) && !line.startsWith("#") ){
                badPasswords.add(line)
            }
        }
        return badPasswords
    }
}

