package galvin.dw.sqlite

import galvin.dw.*
import java.io.File
import java.sql.ResultSet
import java.util.*

class SQLiteAccountRequestDB( private val databaseFile: File, private val userDB: UserDB ) : AccountRequestDB, SQLiteDB(databaseFile){
    private val concurrencyLock = Object()
    private val accountRequestUserInfoDB = SQLiteUserDB(databaseFile)

    private val sqlCreateTableAccountRequests = loadSql("/galvin/dw/db/sqlite/requests/create_table_account_requests.sql")
    private val sqlStoreAccountRequest = loadSql("/galvin/dw/db/sqlite/requests/store_account_request.sql")
    private val sqlRetrieveAccountRequestByUuid = loadSql("/galvin/dw/db/sqlite/requests/retrieve_account_request_by_uuid.sql")
    private val sqlRetrieveAllAccountRequests = loadSql("/galvin/dw/db/sqlite/requests/retrieve_all_account_requests.sql")
    private val sqlApproveAccountRequest = loadSql("/galvin/dw/db/sqlite/requests/approve_account_request.sql")
    private val sqlRejectAccountRequest = loadSql("/galvin/dw/db/sqlite/requests/reject_account_request.sql")

    private val sqlRetrievePendingAccountRequests = loadSql("/galvin/dw/db/sqlite/requests/retrieve_pending_account_requests.sql")
    private val sqlRetrieveApprovedAccountRequests = loadSql("/galvin/dw/db/sqlite/requests/retrieve_approved_account_requests.sql")
    private val sqlRetrieveRejectedAccountRequests = loadSql("/galvin/dw/db/sqlite/requests/retrieve_rejected_account_requests.sql")

    init{
        //create tables
        runSql( conn(), sqlCreateTableAccountRequests )
    }

    override fun storeAccountRequest(request: AccountRequest) {
        if( !Objects.equals(request.password, request.confirmPassword) ){
            throw Exception(ERROR_PASSWORD_MISMATCH)
        }

        verifyNoUserExists(request.uuid)

        synchronized(concurrencyLock) {
            accountRequestUserInfoDB.storeUser(request.user, request.uuid)

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
            statement.setString(11, request.rejectedReason)
            statement.setString(12, request.uuid)

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
        return retrieveAccountRequests(sqlRetrieveAllAccountRequests)
    }

    override fun retrievePendingAccountRequests() : List<AccountRequest>{
        return retrieveAccountRequests(sqlRetrievePendingAccountRequests)
    }

    override fun retrieveApprovedAccountRequests() : List<AccountRequest>{
        return retrieveAccountRequests(sqlRetrieveApprovedAccountRequests)
    }

    override fun retrieveRejectedAccountRequests() : List<AccountRequest>{
        return retrieveAccountRequests(sqlRetrieveRejectedAccountRequests)
    }

    private fun retrieveAccountRequests( sql: String ): List<AccountRequest> {
        val result = mutableListOf<AccountRequest>()

        val conn = conn()
        val statement = conn.prepareStatement(sql)

        val resultSet = statement.executeQuery()
        if (resultSet != null) {
            while (resultSet.next()) {
                result.add(unmarshallAccountRequest(resultSet))
            }
        }

        statement.close()
        conn.close()

        return result
    }

    override fun approve( uuid: String, approvedByUuid: String, timestamp: Long ){
        verifyNoUserExists(uuid)

        val accountRequest = retrieveAccountRequest(uuid)
        if( accountRequest == null ){
            throw Exception( ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID )
        }

        if( accountRequest.approved ){
            return
        }

        synchronized(concurrencyLock) {
            userDB.storeUser(accountRequest.user)

            val conn = conn()
            val statement = conn.prepareStatement(sqlApproveAccountRequest)
            statement.setString(1, approvedByUuid)
            statement.setLong(2, timestamp)
            statement.setString(3, uuid)

            statement.executeUpdate()
            statement.close()

            conn.commit()
            conn.close()
        }
    }

    override fun reject( uuid: String, rejectedByUuid: String, timestamp: Long ){
        val accountRequest = retrieveAccountRequest(uuid)
        if( accountRequest == null ){
            throw Exception( ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID )
        }

        if( accountRequest.rejected ){
            return
        }

        if( accountRequest.approved ){
            throw Exception(ERROR_ALREADY_APPROVED)
        }

        synchronized(concurrencyLock) {
            val conn = conn()
            val statement = conn.prepareStatement(sqlRejectAccountRequest)
            statement.setString(1, rejectedByUuid)
            statement.setLong(2, timestamp)
            statement.setString(3, uuid)

            statement.executeUpdate()
            statement.close()

            conn.commit()
            conn.close()
        }
    }

    private fun verifyNoUserExists( uuid: String ){
        val user = userDB.retrieveUser(uuid)
        if( user != null ){
            throw Exception( ERROR_USER_WITH_THIS_UUID_ALREADY_EXISTS )
        }
    }

    private fun unmarshallAccountRequest(hit: ResultSet): AccountRequest{
        val uuid = hit.getString("uuid")
        val approved = hit.getInt("approved") == 1
        val rejected = hit.getInt("rejected") == 1

        val user = accountRequestUserInfoDB.retrieveUser(uuid)
        if( user == null ){
            throw Exception( ERROR_NO_USER_WITH_THIS_UUID_EXISTS )
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
                rejectedReason = hit.getString("rejectedReason"),
                uuid = uuid
        )
    }
}