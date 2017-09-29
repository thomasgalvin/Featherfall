package galvin.dw.sqlite

import galvin.dw.*
import java.io.File
import java.sql.ResultSet
import java.util.*

class SQLiteAccountRequestDB( private val databaseFile: File) : AccountRequestDB, SQLiteDB(databaseFile){
    private val concurrencyLock = Object()
    private val userDB = SQLiteUserDB(databaseFile)

    private val sqlCreateTableAccountRequests = loadSql("/galvin/dw/db/sqlite/requests/create_table_account_requests.sql")
    private val sqlStoreAccountRequest = loadSql("/galvin/dw/db/sqlite/requests/store_account_request.sql")
    private val sqlRetrieveAccountRequestByUuid = loadSql("/galvin/dw/db/sqlite/requests/retrieve_account_request_by_uuid.sql")
    private val sqlRetrieveAllAccountRequests = loadSql("/galvin/dw/db/sqlite/requests/retrieve_all_account_requests.sql")

    init{
        //create tables
        runSql( conn(), sqlCreateTableAccountRequests )
    }

    override fun storeAccountRequest(request: AccountRequest) {
        if( !Objects.equals(request.password, request.confirmPassword) ){
            throw Exception("Account Request: password mismatch")
        }

        synchronized(concurrencyLock) {
            userDB.storeUser(request.user, request.uuid)

            val conn = conn()
            val statement = conn.prepareStatement(sqlStoreAccountRequest)
            val approved = if( request.approved ) 1 else 0
            val rejected = if( request.rejected ) 1 else 0

            statement.setString(1, request.password)
            statement.setString(2, request.reasonForAccount)
            statement.setString(3, request.vouchName)
            statement.setString(4, request.vouchContactInfo)
            statement.setInt(5, approved)
            statement.setString(6, request.approvedByUuid)
            statement.setLong(7, request.approvedTimestamp)
            statement.setInt(8, rejected)
            statement.setString(9, request.rejectedByUuid)
            statement.setLong(10, request.rejectedTimestamp)
            statement.setString(11, request.uuid)

            statement.executeUpdate()
            conn.commit()
            conn.close()
        }
    }

    override fun retrieveAccountRequest(uuid: String): AccountRequest? {
        var result: AccountRequest? = null

        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveAccountRequestByUuid)
        statement.setString(1, uuid)

        val resultSet = statement.executeQuery()
        if (resultSet != null) {
            if (resultSet.next()) {
                result = unmarshallAccountRequest(resultSet)
            }
        }

        statement.close()
        conn.close()

        return result
    }


    override fun retrieveAccountRequests(): List<AccountRequest> {
        val result = mutableListOf<AccountRequest>()

        val conn = conn()
        val statement = conn.prepareStatement(sqlRetrieveAllAccountRequests)

        val resultSet = statement.executeQuery()
        if (resultSet != null) {
            if (resultSet.next()) {
                result.add(unmarshallAccountRequest(resultSet))
            }
        }

        statement.close()
        conn.close()

        return result
    }

    private fun unmarshallAccountRequest(hit: ResultSet): AccountRequest{
        val uuid = hit.getString("uuid")
        val approved = hit.getInt("approved") == 1
        val rejected = hit.getInt("rejected") == 1

        val user = userDB.retrieveUser(uuid)
        if( user == null ){
            throw Exception( "Account Request error: no user data" )
        }

        return AccountRequest(
                user = user,
                password = hit.getString("password"),
                confirmPassword = hit.getString("password"),
                reasonForAccount = hit.getString("reasonForAccount"),
                vouchName = hit.getString("vouchName"),
                vouchContactInfo = hit.getString("vouchContactInfo"),
                approved = approved,
                approvedByUuid = hit.getString("approvedByUuid"),
                approvedTimestamp = hit.getLong("approvedTimestamp"),
                rejected = rejected,
                rejectedByUuid = hit.getString("rejectedByUuid"),
                rejectedTimestamp = hit.getLong("rejectedTimestamp"),
                uuid = uuid
        )
    }
}