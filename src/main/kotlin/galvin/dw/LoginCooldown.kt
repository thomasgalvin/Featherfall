package galvin.dw

const val MAX_UNHINDERED_ATTEMPTS = 10
const val MAX_FAILED_ATTEMPTS = 15
const val ATTEMPTS_EXPIRE_AFTER: Long = 1000 * 60 * 60 //one hour in ms

/**
 * Tracks login attempts by a key, eg username or IP address.
 *
 * This class will sleep for a progressively longer period of
 * time for each failed login attempt, unless doSleep is set
 * to false.
 */
class LoginCooldown(
        val maxUnhinderedAttempts: Int = MAX_UNHINDERED_ATTEMPTS,
        val maxFailedLoginAttempts: Int = MAX_FAILED_ATTEMPTS,
        val attemptsExpireAfter: Long = ATTEMPTS_EXPIRE_AFTER,
        val doSleep: Boolean = true){
    private val attempts = mutableMapOf<String, LoginAttempt>()

    fun clearLoginAttempts( key: String ){
        attempts.remove(key)
    }

    fun incrementLoginAttempts( key: String ){
        val lastLoginAttempt = getAttempts(key)
        val attemptCount = lastLoginAttempt.count + 1
        val newLoginAttempt = LoginAttempt(attemptCount)
        attempts[key] = newLoginAttempt

        if( doSleep ) {
            val badAttemptCount = attemptCount - maxUnhinderedAttempts
            sleep(badAttemptCount)
        }
    }

    fun maxFailedLoginAttemptsExceeded(key: String): Boolean{
        val lastLoginAttempt = getAttempts(key)
        return lastLoginAttempt.count > maxFailedLoginAttempts
    }

    private fun getAttempts( key: String ): LoginAttempt{
        val result = attempts[key]
        if( result != null ){
            val now = System.currentTimeMillis()
            val expires = result.timestamp + attemptsExpireAfter
            if( now <= expires ){
                attempts.remove(key)
            }
            else{
                return result
            }
        }

        return LoginAttempt()
    }

    private fun sleep( badAttemptCount: Int ){
        val sleep = getSleepTime(badAttemptCount)
        Thread.sleep(sleep)
    }

    private fun getSleepTime( badAttemptCount: Int ): Long{
        if( badAttemptCount <= 0 ){
            return 0
        }

        when(badAttemptCount){
            1 -> return 1_000
            2 -> return 2_000
            3 -> return 5_000
            4 -> return 10_000
            else -> return 15_000
        }
    }
}

data class LoginAttempt(val count: Int = 0,
                        val timestamp: Long = System.currentTimeMillis())