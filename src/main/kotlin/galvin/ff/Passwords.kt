package galvin.ff

import org.mindrot.jbcrypt.BCrypt
import java.io.IOException
import java.security.cert.X509Certificate
import java.security.cert.CertificateFactory
import java.io.FileInputStream
import java.io.File

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
                                val minSpecialCharacters: Int = 0,
                                val repeatedCharactersAllowed: Boolean = true,
                                val validatedAgainstBlacklist: Boolean = true ){
    private val badPasswordFile = "galvin/ff/bad-passwords.txt"
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
        val count = chars.count { Character.isLowerCase(it) }
        return count < minLowerCase
    }

    private fun tooFewUpperCase(chars: CharArray): Boolean {
        val count = chars.count { Character.isUpperCase(it) }
        return count < minUpperCase
    }

    private fun tooFewDigits(chars: CharArray): Boolean {
        val count = chars.count { Character.isDigit(it) }
        return count < minDigits
    }

    private fun tooFewSpecialCharacters(chars: CharArray): Boolean {
        val count = chars.count { !Character.isAlphabetic(it.toInt()) && !Character.isDigit(it) }
        return count < minSpecialCharacters
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
                        badPasswords[line] = java.lang.Boolean.TRUE
                    }
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                    println("Unable to load password blacklist" )
                }

            }
        }
    }

    fun loadBlacklist(): List<String> {
        val list = loadResourceAndReadLines(badPasswordFile)
        val badPasswords = list.filter { !isBlank(it) && !it.startsWith("#") }
        return badPasswords
    }
}

fun parsePKI( x509: X509Certificate ): CertificateData{
    val serialNumber = if(x509.serialNumber == null) "" else "${x509.serialNumber}"
    val tokens = getDistinguishedNameTokens(x509.subjectX500Principal.name)

    return CertificateData(
            credential = getCredentialID(x509),
            serialNumber = serialNumber,
            distinguishedName = x509.subjectX500Principal.name,
            countryCode = neverNull( tokens["countryName"] ),
            citizenship = neverNull( tokens["COUNTRY_OF_CITIZENSHIP"] )
    )
}

fun getCredentialID( x509: X509Certificate ): String{
    val hash = x509.issuerX500Principal.hashCode()
    val serial = x509.serialNumber
    val credentialName = getCredentialName( x509 )
    return "$hash::$serial::$credentialName"
}

fun getCredentialName( x509: X509Certificate ): String{
    val distinguishedName = x509.subjectX500Principal.name

    val  nameIndex = distinguishedName.indexOf( "CN=" )
    if( nameIndex < 0 )  return distinguishedName

    val separatorIndex = distinguishedName.indexOf(',', nameIndex)
    return if (separatorIndex < 0) {
        distinguishedName.substring(nameIndex + 3)
    } else {
        distinguishedName.substring(nameIndex + 3, separatorIndex)
    }
}

fun getDistinguishedNameTokens( dn: String ): Map<String, String>{
    val result = mutableMapOf<String, String>()
    val tokens = dn.split( "," )
    for( token in tokens ){
        if( token.contains("=") ){
            val nameValue = token.split(",")
            if( nameValue.size == 2){
                result[ nameValue[0] ] = nameValue[1]
            }
        }
    }
    return result
}

fun loadCertificateFromFile(file: File): X509Certificate {
    val stream = FileInputStream(file)
    val certFactory = CertificateFactory.getInstance("X.509")

    val cert = certFactory.generateCertificate(stream) as X509Certificate
    stream.close()

    return cert
}

data class CertificateData( val credential: String = "", val serialNumber: String = "", val distinguishedName: String = "",
                            val countryCode: String = "", val citizenship: String = "" )

