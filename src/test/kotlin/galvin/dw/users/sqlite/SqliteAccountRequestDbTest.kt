package galvin.dw.users.sqlite

import galvin.dw.AccountRequest
import galvin.dw.User
import galvin.dw.uuid
import org.junit.Assert
import org.junit.Test


class SqliteAccountRequestDBTest {

    @Test
    fun should_not_create_tables_twice(){
        val userDB = userDB()

        val accountRequestDB = accountRequestDB(userDB)
        val accountRequestDB2 = accountRequestDB(userDB)

        Assert.assertNotNull(accountRequestDB)
        Assert.assertNotNull(accountRequestDB2)
    }

    @Test
    fun should_store_and_retrieve_account_request(){
        val userDB = userDB()
        val accountRequestDB = accountRequestDB(userDB)
        val roles = generateRoles(userdb = userDB)

        val request = generateAccountRequest(roles)
        accountRequestDB.storeAccountRequest(request)

        val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
        Assert.assertEquals("Loaded account request did not match expected", request, loaded)
    }

    @Test
    fun should_update_and_retrieve_account_request(){
        val userDB = userDB()
        val accountRequestDB = accountRequestDB(userDB)
        val roles = generateRoles(userdb = userDB)

        val request = generateAccountRequest(roles)
        accountRequestDB.storeAccountRequest(request)

        val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
        Assert.assertEquals("Loaded account request did not match expected", request, loaded)

        val updatedAccountRequest = generateAccountRequest(roles, request.uuid)
        accountRequestDB.storeAccountRequest(updatedAccountRequest)
        val loadedUpdate = accountRequestDB.retrieveAccountRequest(request.uuid)
        Assert.assertEquals("Loaded account request did not match expected", updatedAccountRequest, loadedUpdate)
    }

    @Test
    fun should_update_multiple_account_requests(){
        val userDB = userDB()
        val accountRequestDB = accountRequestDB(userDB)
        val roles = generateRoles(userdb = userDB)

        val requests = mutableListOf<AccountRequest>()
        for( i in 1 .. 10 ) {
            requests.add(generateAccountRequest(roles))
        }

        val map = mutableMapOf<String, AccountRequest>()
        for( request in requests ){
            accountRequestDB.storeAccountRequest( request )
            map[request.uuid] = request
        }

        for( key in map.keys ){
            val loaded = accountRequestDB.retrieveAccountRequest(key)
            Assert.assertEquals("Loaded account request did not match expected", map[key], loaded)
        }

        for( key in map.keys ){
            val user = generateAccountRequest(roles, key)
            map[key] = user
            accountRequestDB.storeAccountRequest( user )
        }

        for( key in map.keys ){
            val loaded = accountRequestDB.retrieveAccountRequest(key)
            Assert.assertEquals("Loaded account request did not match expected", map[key], loaded)
        }

        for( request in requests ){
            val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
            Assert.assertNotEquals("Loaded account request should have been modified", request, loaded)
        }
    }

    @Test
    fun should_store_and_retrieve_all_users(){
        val userDB = userDB()
        val accountRequestDB = accountRequestDB(userDB)
        val roles = generateRoles(userdb = userDB)
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

    @Test
    fun should_throw_when_no_account_request_exists_to_approve() {
        val userDB = userDB()
        val accountRequestDB = accountRequestDB(userDB)

        try {
            accountRequestDB.approve(uuid(), uuid())
            throw Exception("Error: Account Request database should have thrown an exception")
        }
        catch (e: Exception) {
            val message = e.message;
            if (message == null) {
                throw Exception("Unexpected exception")
            }
            Assert.assertTrue("Unexpected exception", message.startsWith("Account Request error"))
        }
    }

    @Test
    fun should_throw_when_user_alrady_exists_store() {
        val userDB = userDB()
        val accountRequestDB = accountRequestDB(userDB)

        val roles = generateRoles(userdb = userDB)
        val user = generateUser(roles)
        userDB.storeUser(user)

        val request = generateAccountRequest(roles, user.uuid)

        try {
            accountRequestDB.storeAccountRequest(request)
            throw Exception("Error: Account Request database should have thrown an exception")
        }
        catch (e: Exception) {
            val message = e.message;
            if (message == null) {
                throw Exception("Unexpected exception")
            }
            Assert.assertTrue("Unexpected exception", message.startsWith("Account Request error"))
        }
    }
}

