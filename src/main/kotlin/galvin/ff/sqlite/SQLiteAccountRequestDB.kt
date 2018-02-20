package galvin.ff.sqlite

import galvin.ff.*
import java.io.File
import java.sql.ResultSet
import java.util.*

class SQLiteAccountRequestDB( databaseFile: File, private val userDB: UserDB ) : AccountRequestDB, SQLiteDB(databaseFile){
    private val concurrencyLock = Object()
    private val accountRequestUserInfoDB = SQLiteUserDB(databaseFile)

    private val sqlCreateTableAccountRequests = loadSql("/galvin/ff/db/sqlite/requests/create_table_account_requests.sql")
    private val sqlStoreAccountRequest = loadSql("/galvin/ff/db/sqlite/requests/store_account_request.sql")
    private val sqlRetrieveAccountRequestByUuid = loadSql("/galvin/ff/db/sqlite/requests/retrieve_account_request_by_uuid.sql")
    private val sqlRetrieveAllAccountRequests = loadSql("/galvin/ff/db/sqlite/requests/retrieve_all_account_requests.sql")
    private val sqlApproveAccountRequest = loadSql("/galvin/ff/db/sqlite/requests/approve_account_request.sql")
    private val sqlRejectAccountRequest = loadSql("/galvin/ff/db/sqlite/requests/reject_account_request.sql")

    private val sqlRetrievePendingAccountRequests = loadSql("/galvin/ff/db/sqlite/requests/retrieve_pending_account_requests.sql")
    private val sqlRetrieveApprovedAccountRequests = loadSql("/galvin/ff/db/sqlite/requests/retrieve_approved_account_requests.sql")
    private val sqlRetrieveRejectedAccountRequests = loadSql("/galvin/ff/db/sqlite/requests/retrieve_rejected_account_requests.sql")

    init{
        //create tables

        val conn = conn()
        try {
            executeUpdate(conn, sqlCreateTableAccountRequests)
            commitAndClose(conn)
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun storeAccountRequest(request: AccountRequest) {
        if( !Objects.equals(request.password, request.confirmPassword) ){
            throw Exception(ERROR_PASSWORD_MISMATCH)
        }

        verifyNoUserExists(request.uuid)

        synchronized(concurrencyLock) {
            accountRequestUserInfoDB.storeUser(request.user, request.uuid)

            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlStoreAccountRequest)
                val approved = if (request.approved) 1 else 0
                val rejected = if (request.rejected) 1 else 0

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

                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun retrieveAccountRequest(uuid: String): AccountRequest? {
        val conn = conn()

        try {
            var result: AccountRequest? = null
            val statement = conn.prepareStatement(sqlRetrieveAccountRequestByUuid)
            statement.setString(1, uuid)

            val resultSet = statement.executeQuery()
            if (resultSet != null) {
                if (resultSet.next()) {
                    result = unmarshalAccountRequest(resultSet)
                }
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
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
        val conn = conn()

        try {
            val result = mutableListOf<AccountRequest>()
            val statement = conn.prepareStatement(sql)

            val resultSet = statement.executeQuery()
            if (resultSet != null) {
                while (resultSet.next()) {
                    result.add(unmarshalAccountRequest(resultSet))
                }
            }

            close(conn, statement)
            return result
        }
        finally{
            rollbackAndClose(conn)
        }
    }

    override fun approve( uuid: String, approvedByUuid: String, timestamp: Long ){
        verifyNoUserExists(uuid)

        val accountRequest = retrieveAccountRequest(uuid) ?: throw Exception( ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID )
        if( accountRequest.approved ){
            return
        }

        synchronized(concurrencyLock) {
            userDB.storeUser(accountRequest.user)

            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlApproveAccountRequest)
                statement.setString(1, approvedByUuid)
                statement.setLong(2, timestamp)
                statement.setString(3, uuid)

                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    override fun reject( uuid: String, rejectedByUuid: String, reason: String, timestamp: Long ){
        val accountRequest = retrieveAccountRequest(uuid) ?: throw Exception( ERROR_NO_ACCOUNT_REQUEST_WITH_THAT_UUID )

        if( accountRequest.rejected ){
            return
        }

        if( accountRequest.approved ){
            throw Exception(ERROR_ALREADY_APPROVED)
        }

        synchronized(concurrencyLock) {
            val conn = conn()

            try {
                val statement = conn.prepareStatement(sqlRejectAccountRequest)
                statement.setString(1, rejectedByUuid)
                statement.setLong(2, timestamp)
                statement.setString(3, reason)
                statement.setString(4, uuid)

                executeAndClose(statement, conn)
            }
            finally{
                rollbackAndClose(conn)
            }
        }
    }

    private fun verifyNoUserExists( uuid: String ){
        if( userDB.userExists(uuid) ){
            throw Exception( ERROR_USER_WITH_THIS_UUID_ALREADY_EXISTS )
        }
    }

    private fun unmarshalAccountRequest(hit: ResultSet): AccountRequest{
        val uuid = hit.getString("uuid")
        val approved = hit.getInt("approved") == 1
        val rejected = hit.getInt("rejected") == 1

        val user = accountRequestUserInfoDB.retrieveUser(uuid) ?: throw Exception( ERROR_NO_USER_WITH_THIS_UUID_EXISTS )

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