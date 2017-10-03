package galvin.dw.users.sqlite

import galvin.dw.*
import galvin.dw.sqlite.SQLiteAuditDB
import org.junit.Assert
import org.junit.Test

import java.io.File
import java.util.*

import galvin.dw.uuid

class LoginManagerTest{
    @Test
    fun should_login_successfully_by_password(){
        val auditDB = randomAuditDB()
        val userDB = userDB()
        val loginManager = LoginManager( userDB = userDB, auditDB = auditDB )

        val roles = generateRoles(userdb = userDB)
        val count = 10;

        val passwords = mutableListOf<String>()
        for( i in 0..count ){
            passwords.add( uuid() )
        }

        val users = mutableListOf<User>()
        for( i in 0..count ){
            users.add( generateUser(roles, uuid(), passwords[i]) )
            userDB.storeUser( users[i] )
            //println("Created user: " + users[i].login + " pass: " + passwords[i])
        }

        for( i in 1..count ){
            val user = users[i]
            val password = passwords[i]
            val credentials =  Credentials( username = user.login, password = password )

            val loginToken = loginManager.authenticate(credentials)
            Assert.assertEquals("Unexpected user", user, loginToken.user )
        }

        for( i in 1..count ){
            val user = users[i]
            val credentials =  Credentials( x509SerialNumber = neverNull(user.serialNumber) )

            val loginToken = loginManager.authenticate(credentials)
            Assert.assertEquals("Unexpected user", user, loginToken.user )
        }
    }

    private fun randomAuditDbFile(): File {
        return File("target/audit-" + uuid() + ".dat")
    }

    private fun randomAuditDB() : AuditDB {
        val result = SQLiteAuditDB( randomAuditDbFile() )
        val systemInfo = randomSystemInfo()
        result.storeSystemInfo(systemInfo)
        result.storeCurrentSystemInfo(systemInfo.uuid)
        return result
    }

    private fun randomSystemInfo(): SystemInfo {
        return SystemInfo(
                "serial:" + uuid(),
                "name:" + uuid(),
                "version" + uuid(),
                "Unclassified-" + uuid(),
                "guide:" + uuid(),
                Arrays.asList(
                        uuid(),
                        uuid(),
                        uuid(),
                        uuid(),
                        uuid()
                ),
                "uuid:" + uuid()
        )
    }
}