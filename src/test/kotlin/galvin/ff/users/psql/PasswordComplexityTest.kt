package galvin.ff.users.sqlite

import galvin.ff.PasswordRequirements
import org.junit.Test


class PaswordComplexityTest{
    @Test
    @Throws(Exception::class)
    fun testEmpty() {
        val requirements = PasswordRequirements(validatedAgainstBlacklist = false)

        val badPassword = ""
        val shouldBeInvalid = requirements.validate(badPassword)

        if (!shouldBeInvalid.invalidPassword()) {
            throw Exception("Password requirements failed; password *was* empty")
        }

        if (!shouldBeInvalid.passwordEmpty) {
            throw Exception("Password requirements failed; password *was* empty")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testLength() {
        val password = "1234567890"
        for (i in password.length downTo 0) {
            val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minLength = i)
            val shouldBeValid = requirements.validate(password)
            if (shouldBeValid.invalidPassword() || shouldBeValid.passwordTooShort) {
                throw Exception("Password Requirements didn't allow long password")
            }
        }

        val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minLength = password.length + 1)
        val shouldBeInvalid = requirements.validate(password)
        if (!shouldBeInvalid.invalidPassword() || !shouldBeInvalid.passwordTooShort) {
            throw Exception("Password Requirements didn't catch short password: minLength: " + requirements.minLength + " pass length: " + password.length)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testLowerCase() {
        val password = "12345abcde"
        for (i in 0..4) {
            val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minLowerCase = i)
            val shouldBeValid = requirements.validate(password)
            if (shouldBeValid.invalidPassword()) {
                throw Exception("Password requirements failed; password contained enough lower-case characters")
            }
        }

        val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minLowerCase = 6)
        val shouldBeInvalid = requirements.validate(password)

        if (!shouldBeInvalid.invalidPassword() || !shouldBeInvalid.tooFewLowerCase) {
            throw Exception("Password requirements failed; did *not* contain enough lower-case characters")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testUpperCase() {
        val password = "12345ABCDE"
        for (i in 0..4) {
            val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minUpperCase = i)
            val shouldBeValid = requirements.validate(password)
            if (shouldBeValid.invalidPassword()) {
                throw Exception("Password requirements failed; password contained enough upper-case characters")
            }
        }

        val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minUpperCase = 6)
        val shouldBeInvalid = requirements.validate(password)

        if (!shouldBeInvalid.invalidPassword() || !shouldBeInvalid.tooFewUpperCase) {
            throw Exception("Password requirements failed; did *not* contain enough upper-case characters")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testDigits() {
        val password = "12345ABCDE"
        for (i in 0..4) {
            val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minDigits = i)
            val shouldBeValid = requirements.validate(password)
            if (shouldBeValid.invalidPassword()) {
                throw Exception("Password requirements failed; password contained enough digits")
            }
        }

        val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minDigits = 6)
        val shouldBeInvalid = requirements.validate(password)

        if (!shouldBeInvalid.invalidPassword() || !shouldBeInvalid.tooFewDigits) {
            throw Exception("Password requirements failed; did *not* contain enough digits")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSpecialCharacters() {
        val password = "12345ABCDE~!@#$"
        for (i in 0..4) {
            val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minSpecialCharacters = i)
            val shouldBeValid = requirements.validate(password)
            if (shouldBeValid.invalidPassword()) {
                throw Exception("Password requirements failed; password contained enough special characters")
            }
        }

        val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minSpecialCharacters = 6)
        val shouldBeInvalid = requirements.validate(password)

        if (!shouldBeInvalid.invalidPassword() || !shouldBeInvalid.tooFewSpecialCharacters) {
            throw Exception("Password requirements failed; did *not* contain enough special characters")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSpecialCharacterSet() {
        val password = "`~!@#$%^&*()_+-={}|:\"<>?[]\\;',./"
        val requirements = PasswordRequirements(validatedAgainstBlacklist = false, minSpecialCharacters = password.length)
        val shouldBeValid = requirements.validate(password)
        if (shouldBeValid.invalidPassword()) {
            throw Exception("Password requirements failed; password contained enough special characters")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testRepeatedCharacters() {
        val repeatedCharsAllowed = PasswordRequirements(validatedAgainstBlacklist = false, repeatedCharactersAllowed = true)
        val repeatedCharsNotAllowed = PasswordRequirements(validatedAgainstBlacklist = false, repeatedCharactersAllowed = false)
        val passwords = arrayOf("aa12345", "12345aa", "12345aa67890")

        for (password in passwords) {
            val shouldBeValid = repeatedCharsAllowed.validate(password)
            if (shouldBeValid.invalidPassword()) {
                throw Exception("Password requirements failed; repeated characters should have been allowed")
            }

            val shouldBeInvalid = repeatedCharsNotAllowed.validate(password)
            if (!shouldBeInvalid.invalidPassword() || !shouldBeInvalid.repeatedCharacters) {
                throw Exception("Password requirements failed; repeated characters should *not*have been allowed: " + password)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testOnBlacklist() {
        val requirements = PasswordRequirements()

        val badPasswords = requirements.loadBlacklist()
        if (badPasswords.isEmpty()) {
            throw Exception("Failed to load bad password list")
        }

        for (password in badPasswords) {
            val shouldBeInvalid = requirements.validate(password)
            if (!shouldBeInvalid.invalidPassword() || !shouldBeInvalid.foundOnBlacklist) {
                throw Exception("Password requirements failed; blacklisted password should *not* have been allowed: " + password)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testNotOnBlacklist() {
        val requirements = PasswordRequirements()

        val goodPasswords = arrayOf("*yN9+R7z3==#87", "386/jq4[YZ&2$.", "=8[y26%E9$#4Ek", "(M7E7]8[C4#@4V", "(8@4h3o(i*]Y32", "i@2{)794L;c*8R", "6]L7663@.zY[>J", "6{x=4W/3nR{6>7", "w3P&y?3M;3&6*8", "K+3uU(39{V44=>")

        for (password in goodPasswords) {
            val shouldBeInvalid = requirements.validate(password)
            if (shouldBeInvalid.invalidPassword() || shouldBeInvalid.foundOnBlacklist) {
                throw Exception("Password requirements failed; password should not be found on blacklist: " + password)
            }
        }
    }
}