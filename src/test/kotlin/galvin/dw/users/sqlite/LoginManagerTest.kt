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
        }

        for( i in 0..count ){
            val user = users[i]
            val password = passwords[i]
            val credentials =  Credentials( username = user.login, password = password )

            val loginToken = loginManager.authenticate(credentials)
            Assert.assertEquals("Unexpected user", user, loginToken.user )
        }

        for( i in 0..count ){
            val user = users[i]
            val credentials =  Credentials( x509SerialNumber = neverNull(user.serialNumber) )

            val loginToken = loginManager.authenticate(credentials)
            Assert.assertEquals("Unexpected user", user, loginToken.user )
        }

        val auditEvents = auditDB.retrieveAccessInfo()
        Assert.assertEquals("Unexpected event count", 22, auditEvents.size )
        for( (i, event) in auditEvents.listIterator().withIndex() ){
            val index = if(i <= 10) i else i - 11
            val user = users[index]
            Assert.assertEquals("Unexpected user at index ${index}", user.uuid, event.userUuid )
            Assert.assertEquals("Unexpected user at index ${index}", user.uuid, event.resourceUuid )
            Assert.assertEquals("Unexpected user at index ${index}", user.login, event.resourceName )
            Assert.assertTrue("Unexpected access granted ${index}", event.permissionGranted )
        }
    }

    @Test
    fun should_fail_login_by_password(){
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
        }

        for( i in 0..count ){
            val user = users[i]
            val credentials =  Credentials( username = user.login, password = uuid() )

            try {
                loginManager.authenticate(credentials)
                throw Exception( "Should have thrown login exception" )
            }
            catch( ex: LoginException ){
                //no-op
            }
        }

        val auditEvents = auditDB.retrieveAccessInfo()
        Assert.assertEquals("Unexpected event count", 11, auditEvents.size )
        for( (i, event) in auditEvents.listIterator().withIndex() ){
            val index = if(i <= 10) i else i - 11
            val user = users[index]
            Assert.assertEquals("Unexpected user at index ${index}", user.uuid, event.resourceUuid )
            Assert.assertEquals("Unexpected user at index ${index}", user.login, event.resourceName )
            Assert.assertFalse("Unexpected access granted ${index}", event.permissionGranted )
        }
    }

    private fun randomAuditDbFile(): File {
        return File("target/audit-" + uuid() + ".dat")
    }

    private fun randomAuditDB() : AuditDB {
        val result = SQLiteAuditDB( randomAuditDbFile(), false )
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