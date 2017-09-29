package galvin.dw.users.sqlite

import galvin.dw.AccountRequest
import galvin.dw.User
import org.junit.Assert
import org.junit.Test


class SqliteAccountRequestDBTest {

    @Test
    fun should_not_create_tables_twice(){
        val accountRequestDB = accountRequestDB()
        val accountRequestDB2 = accountRequestDB()

        Assert.assertNotNull(accountRequestDB)
        Assert.assertNotNull(accountRequestDB2)
    }

    @Test
    fun should_store_and_retrieve_account_request(){
        val accountRequestDB = accountRequestDB()
        val userDB = userDB()
        val roles = generateRoles(userdb = userDB)

        val request = generateAccountRequest(roles)
        accountRequestDB.storeAccountRequest(request)

        val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
        Assert.assertEquals("Loaded account request did not match expected", request, loaded)
    }

    @Test
    fun should_store_and_retrieve_all_users(){
        val accountRequestDB = accountRequestDB()
        val userdb = userDB()
        val roles = generateRoles(userdb = userdb)
        val expectedCount = 10

        val map = mutableMapOf<String, AccountRequest>()
        for( i in 1..expectedCount ){
            val request = generateAccountRequest(roles)
            accountRequestDB.storeAccountRequest(request)
            map.put( request.uuid, request )
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = accountRequestDB.retrieveAccountRequest(key)
            Assert.assertEquals("Loaded account request did not match expected", expected, loaded)
        }

        val loadedAccountRequests = accountRequestDB.retrieveAccountRequests()
        for( loaded in loadedAccountRequests ){
            val expected = map[loaded.uuid]
            Assert.assertEquals("Loaded account request did not match expected", expected, loaded)
        }
    }
}

