package galvin.ff

class DatabaseError :  RuntimeException {
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}

const val LOGIN_EXCEPTION_INVALID_TOKEN = "Login Error: The login token provided was invalid or has expired"
const val LOGIN_EXCEPTION_INVALID_CREDENTIALS = "Login Error: The credentials provided were invalid"
const val LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED = "Login Error: The maximum number of login attempts has been exceeded"

class LoginError :  RuntimeException {
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super("An error occurred logging into the application")

    companion object {
        fun invalidToken() = LoginError(LOGIN_EXCEPTION_INVALID_TOKEN)
        fun invalidCredentials() = LoginError(LOGIN_EXCEPTION_INVALID_CREDENTIALS)
        fun maxAttemptsExpired() = LoginError(LOGIN_EXCEPTION_MAX_ATTEMPTS_EXCEEDED)
    }
}