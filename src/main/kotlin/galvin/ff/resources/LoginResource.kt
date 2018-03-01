package galvin.ff.resources

import galvin.ff.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path( "login" )
@Produces( MediaType.APPLICATION_JSON )
@Consumes( MediaType.APPLICATION_JSON )
class LoginResource(private val loginManager: LoginManager ){
    @POST fun loginPost( @Context httpRequest: HttpServletRequest, credentials: Credentials ): LoginToken{
        return login(httpRequest, credentials)
    }

    @GET fun loginGet( @Context httpRequest: HttpServletRequest, credentials: Credentials ): LoginToken{
        return login(httpRequest, credentials)
    }

    private fun login(httpRequest: HttpServletRequest, credentials: Credentials): LoginToken{
        val populatedCredentials = Credentials.copy(httpRequest, credentials)
        return loginManager.authenticate(populatedCredentials)
    }
}