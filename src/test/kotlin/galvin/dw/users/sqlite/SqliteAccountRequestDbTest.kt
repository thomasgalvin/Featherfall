package galvin.dw.users.sqlite

import org.junit.Test


class SqliteAccountRequestDBTest {

    @Test
    fun should_not_create_tables_twice(){
        val accountRequestDB = accountRequestDB()
        val accountRequestDB2 = accountRequestDB()
    }

    @Test
    fun should_store_and_retrieve_account_request(){
        val accountRequestDB = accountRequestDB()
        val userDB = userDB()
        val roles = generateRoles(userdb = userDB)
        val request = generateAccountRequest(roles)

        accountRequestDB.storeAccountRequest(request)
    }
}

