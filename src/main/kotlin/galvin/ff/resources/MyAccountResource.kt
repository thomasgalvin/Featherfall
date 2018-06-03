package galvin.ff.resources

import galvin.ff.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("my-account")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MyAccountResource( val userDB: UserDB,
                         val loginManager: LoginManager,
                         val auditDB: AuditDB = NoOpAuditDB() ) {

    @GET
    fun getMyData(@Context httpRequest: HttpServletRequest, credentials: Credentials ): User? {
        val populatedCredentials = Credentials.copy(httpRequest, credentials)
        val loginToken = loginManager.authenticate(populatedCredentials)


        //auditDB.log( accessInfo )

        return loginToken.user
                         .withoutPasswordHash()
    }
}