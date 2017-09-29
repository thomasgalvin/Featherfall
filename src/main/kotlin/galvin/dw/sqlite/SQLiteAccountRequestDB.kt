package galvin.dw.sqlite

import galvin.dw.AccountRequest
import galvin.dw.AccountRequestDB
import galvin.dw.loadSql
import galvin.dw.runSql
import java.io.File

class SQLiteAccountRequestDB( private val databaseFile: File) : AccountRequestDB, SQLiteDB(databaseFile){
    private val concurrencyLock = Object()
    private val userDB : SQLiteUserDB

    private val sqlCreateTableAccountRequests = loadSql("/galvin/dw/db/sqlite/requests/create_table_account_requests.sql")
    private val sqlAccountRequestUser = loadSql("/galvin/dw/db/sqlite/requests/store_account_request.sql")

    init{
        userDB = SQLiteUserDB(databaseFile)

        //create tables
        runSql( conn(), sqlCreateTableAccountRequests )
    }

    override fun storeAccountRequest(request: AccountRequest) {
        synchronized(concurrencyLock) {


            userDB.storeUser(request.user);
        }
    }
//
//    override fun retrieveAccountRequest(uuid: String): AccountRequest {
//        synchronized(concurrencyLock) {
//        }
//    }
//
//    override fun retrieveAccountRequests(): List<AccountRequest> {
//        synchronized(concurrencyLock) {
//        }
//    }

}