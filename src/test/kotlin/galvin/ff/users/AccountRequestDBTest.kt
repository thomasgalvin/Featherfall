package galvin.ff.users

import galvin.ff.*
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test


class AccountRequestDBTest{
    companion object {
        @AfterClass @JvmStatic fun cleanup(){
            for (database in databases) {
                database.cleanup()
            }
        }
    }

    @Test fun should_store_and_retrieve_account_request(){
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, _) = testObjects(database)

            val request = generateAccountRequest(roles)
            accountRequestDB.storeAccountRequest(request)

            val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
            Assert.assertEquals("Loaded account request did not match expected", request, loaded)
        }
    }

    @Test fun should_store_and_retrieve_all_account_requests(){
        for( database in databases ){
            if( !database.canConnect() ) continue

            val( _, accountRequestDB, roles, count ) = testObjects(database)

            val map = mutableMapOf<String, AccountRequest>()
            for( i in 1..count ){
                val request = generateAccountRequest(roles)
                accountRequestDB.storeAccountRequest(request)
                map[request.uuid] = request
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

    @Test fun should_update_and_retrieve_account_request(){
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, _) = testObjects(database)

            val request = generateAccountRequest(roles)
            accountRequestDB.storeAccountRequest(request)

            val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
            Assert.assertEquals("Loaded account request did not match expected", request, loaded)

            val updatedAccountRequest = generateAccountRequest(roles, request.uuid)
            accountRequestDB.storeAccountRequest(updatedAccountRequest)
            val loadedUpdate = accountRequestDB.retrieveAccountRequest(request.uuid)
            Assert.assertEquals("Loaded account request did not match expected", updatedAccountRequest, loadedUpdate)
        }
    }

    @Test fun should_update_multiple_account_requests(){
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, count) = testObjects(database)

            val requests = mutableListOf<AccountRequest>()
            for (i in 1..count) {
                requests.add(generateAccountRequest(roles))
            }

            val map = mutableMapOf<String, AccountRequest>()
            for (request in requests) {
                accountRequestDB.storeAccountRequest(request)
                map[request.uuid] = request
            }

            for (key in map.keys) {
                val loaded = accountRequestDB.retrieveAccountRequest(key)
                Assert.assertEquals("Loaded account request did not match expected", map[key], loaded)
            }

            for (key in map.keys) {
                val request = generateAccountRequest(roles, key)
                map[key] = request
                accountRequestDB.storeAccountRequest(request)
            }

            for (key in map.keys) {
                val loaded = accountRequestDB.retrieveAccountRequest(key)
                Assert.assertEquals("Loaded account request did not match expected", map[key], loaded)
            }

            for (request in requests) {
                val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
                Assert.assertNotEquals("Loaded account request should have been modified", request, loaded)
            }
        }
    }

    @Test fun should_approve_account_requests(){
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, _) = testObjects(database)

            val request = generateAccountRequest(roles)
            accountRequestDB.storeAccountRequest(request)

            val approvedBy = uuid()
            val timestamp = System.currentTimeMillis()
            accountRequestDB.approve(request.uuid, approvedBy, timestamp)

            val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
            Assert.assertTrue("Unexpected value for approved", loaded!!.approved)
            Assert.assertEquals("Unexpected value for approved by", approvedBy, loaded.approvedByUuid)
            Assert.assertEquals("Unexpected value for approved timestamp", timestamp, loaded.approvedTimestamp)
            Assert.assertEquals("Unexpected value for approved user", request.user, loaded.user)
        }
    }

    @Test fun should_approve_multiple_account_requests(){
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, count) = testObjects(database)

            val requests = mutableListOf<AccountRequest>()
            for (i in 1..count) {
                requests.add(generateAccountRequest(roles))
            }

            val map = mutableMapOf<String, AccountRequest>()
            for (request in requests) {
                accountRequestDB.storeAccountRequest(request)
                map[request.uuid] = request
            }

            for (key in map.keys) {
                val loaded = accountRequestDB.retrieveAccountRequest(key)
                Assert.assertEquals("Loaded account request did not match expected", map[key], loaded)
            }

            for (key in map.keys) {
                accountRequestDB.approve(key, uuid(), System.currentTimeMillis())
            }

            for (key in map.keys) {
                val loaded = accountRequestDB.retrieveAccountRequest(key)
                Assert.assertTrue("Unexpected value for approved", loaded!!.approved)
            }

            for (request in requests) {
                val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
                Assert.assertNotEquals("Loaded account request should have been modified", request, loaded)
            }
        }
    }

    @Test fun should_reject_account_requests(){
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, _) = testObjects(database)

            val request = generateAccountRequest(roles)
            accountRequestDB.storeAccountRequest(request)

            val rejectedBy = uuid()
            val reason = uuid()
            val timestamp = System.currentTimeMillis()
            accountRequestDB.reject(request.uuid, rejectedBy, reason, timestamp)

            val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
            Assert.assertTrue("Unexpected value for rejected", loaded!!.rejected)
            Assert.assertEquals("Unexpected value for rejected by", rejectedBy, loaded.rejectedByUuid)
            Assert.assertEquals("Unexpected value for rejected timestamp", timestamp, loaded.rejectedTimestamp)
            Assert.assertEquals("Unexpected value for rejected reason", reason, loaded.rejectedReason)
            Assert.assertEquals("Unexpected value for rejected user", request.user, loaded.user)
        }
    }

    @Test fun should_reject_multiple_account_requests(){
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, count) = testObjects(database)

            val requests = mutableListOf<AccountRequest>()
            for (i in 1..count) {
                requests.add(generateAccountRequest(roles))
            }

            val map = mutableMapOf<String, AccountRequest>()
            for (request in requests) {
                accountRequestDB.storeAccountRequest(request)
                map[request.uuid] = request
            }

            for (key in map.keys) {
                val loaded = accountRequestDB.retrieveAccountRequest(key)
                Assert.assertEquals("Loaded account request did not match expected", map[key], loaded)
            }

            for (key in map.keys) {
                accountRequestDB.reject(key, uuid(), uuid(), System.currentTimeMillis())
            }

            for (key in map.keys) {
                val loaded = accountRequestDB.retrieveAccountRequest(key)
                Assert.assertFalse("Unexpected value for approved", loaded!!.approved)
            }

            for (request in requests) {
                val loaded = accountRequestDB.retrieveAccountRequest(request.uuid)
                Assert.assertNotEquals("Loaded account request should have been modified", request, loaded)
            }
        }
    }

    @Test fun should_throw_when_passwords_mismatch() {
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, _) = testObjects(database)

            try {
                val request = generateAccountRequest(roles, uuid(), uuid(), uuid())
                accountRequestDB.storeAccountRequest(request)
            } catch (e: Exception) {
                Assert.assertEquals("UnexpectedException", e.message, ERROR_PASSWORD_MISMATCH)
            }
        }
    }

    @Test fun should_throw_when_no_account_request_exists_to_approve() {
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, _, _) = testObjects(database)

            try {
                accountRequestDB.approve(uuid(), uuid(), System.currentTimeMillis())
                throw Exception("Error: Account Request database should have thrown an exception")
            } catch (e: Exception) {
                Assert.assertEquals("UnexpectedException", e.message, ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID)
            }
        }
    }

    @Test fun should_throw_when_no_account_request_exists_to_reject() {
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, _, _) = testObjects(database)

            try {
                accountRequestDB.reject(uuid(), uuid(), uuid(), System.currentTimeMillis())
                throw Exception("Error: Account Request database should have thrown an exception")
            } catch (e: Exception) {
                Assert.assertEquals("UnexpectedException", e.message, ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID)
            }
        }
    }

    @Test fun should_throw_when_user_already_exists_by_login() {
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (userDB, accountRequestDB, roles, _) = testObjects(database)

            val user = generateUser(roles)
            userDB.storeUser(user)

            val request = generateAccountRequest(roles, uuid()).withLogin(user.login)

            try {
                accountRequestDB.storeAccountRequest(request)
                throw Exception("Error: Account Request database should have thrown an exception")
            } catch (e: Exception) {
                Assert.assertEquals("UnexpectedException", e.message, ERROR_USER_WITH_THIS_LOGIN_ALREADY_EXISTS)
            }
        }
    }

    @Test fun should_throw_when_user_already_exists_by_uuid() {
        for( database in databases ) {
            if (!database.canConnect()) continue

            val (userDB, accountRequestDB, roles, _) = testObjects(database)

            val user = generateUser(roles)
            userDB.storeUser(user)

            val request = generateAccountRequest(roles, user.uuid)

            try {
                accountRequestDB.storeAccountRequest(request)
                throw Exception("Error: Account Request database should have thrown an exception")
            } catch (e: Exception) {
                Assert.assertEquals("UnexpectedException", e.message, ERROR_USER_WITH_THIS_UUID_ALREADY_EXISTS)
            }
        }
    }

    @Test fun should_throw_when_rejecting_an_approved_account() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, _) = testObjects(database)

            val request = generateAccountRequest(roles)
            accountRequestDB.storeAccountRequest(request)
            accountRequestDB.approve(request.uuid, uuid(), System.currentTimeMillis())

            try {
                accountRequestDB.reject(request.uuid, uuid(), uuid(), System.currentTimeMillis())
                throw Exception("Error: Account Request database should have thrown an exception")
            } catch (e: Exception) {
                Assert.assertEquals("UnexpectedException", e.message, ERROR_ALREADY_APPROVED)
            }
        }
    }

    @Test fun should_retrieve_by_status() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val (_, accountRequestDB, roles, _) = testObjects(database)

            val pending = mutableMapOf<String, AccountRequest>()
            val approved = mutableMapOf<String, AccountRequest>()
            val rejected = mutableMapOf<String, AccountRequest>()

            populate(pending, roles, accountRequestDB)
            populate(approved, roles, accountRequestDB)
            populate(rejected, roles, accountRequestDB)

            for (uuid in approved.keys) {
                accountRequestDB.approve(uuid, uuid(), System.currentTimeMillis())
            }

            for (uuid in rejected.keys) {
                accountRequestDB.reject(uuid, uuid(), uuid(), System.currentTimeMillis())
            }

            val loadedPending = accountRequestDB.retrievePendingAccountRequests()
            Assert.assertEquals("Unexpected number of pending users", pending.size, loadedPending.size)
            for (loaded in loadedPending) {
                val original = pending[loaded.uuid]
                Assert.assertEquals("Unexpected value for pending user", original!!.user, loaded.user)
            }

            val loadedApproved = accountRequestDB.retrieveApprovedAccountRequests()
            Assert.assertEquals("Unexpected number of approved users", approved.size, loadedApproved.size)
            for (loaded in loadedApproved) {
                val original = approved[loaded.uuid]
                Assert.assertEquals("Unexpected value for approved user", original!!.user, loaded.user)
            }

            val loadedRejected = accountRequestDB.retrieveRejectedAccountRequests()
            Assert.assertEquals("Unexpected number of rejected users", rejected.size, loadedRejected.size)
            for (loaded in loadedRejected) {
                val original = rejected[loaded.uuid]
                Assert.assertEquals("Unexpected value for rejected user", original!!.user, loaded.user)
            }
        }
    }

    private fun populate( map: MutableMap<String, AccountRequest>, roles: List<Role>, accountRequestDB : AccountRequestDB ){
        for( i in 1 .. 10 ){
            val request = generateAccountRequest(roles)
            map[request.uuid] = request
            accountRequestDB.storeAccountRequest(request)
        }
    }

    class AccountRequestDBTestObjects(private val userDB: UserDB,
                                      private val accountRequestDB: AccountRequestDB,
                                      private val roles: List<Role>,
                                      private val count: Int = 10) {
        operator fun component1(): UserDB{ return userDB }
        operator fun component2(): AccountRequestDB{ return accountRequestDB }
        operator fun component3(): List<Role>{ return roles }
        operator fun component4(): Int{ return count }
    }

    private fun testObjects(database: DbConnection): AccountRequestDBTestObjects {
        val userDB = database.randomUserDB()
        val accountRequestDB = database.randomAccountRequestDB(userDB)
        val roles = generateRoles(userdb = userDB)
        val count = 10

        return AccountRequestDBTestObjects(userDB, accountRequestDB, roles, count)
    }
}