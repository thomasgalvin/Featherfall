package galvin.dw

import org.mindrot.jbcrypt.BCrypt
import java.io.IOException

const val SPECIAL_CHARACTER_SET = "`~!@#$%^&*()_+-={}|:\"<>?[]\\;',./"

fun validate( password: String?, hash: String? ): Boolean{
    if( password == null || isBlank(password) ){
        return false
    }

    if( hash == null || isBlank(hash) ){
        return false
    }

    return BCrypt.checkpw( password, hash )
}

fun hash( password: String): String{
    return BCrypt.hashpw( password, BCrypt.gensalt() )
}

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
