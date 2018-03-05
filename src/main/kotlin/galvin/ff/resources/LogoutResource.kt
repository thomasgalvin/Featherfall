package galvin.ff.resources

import galvin.ff.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path( "logout" )
@Produces( MediaType.APPLICATION_JSON )
@Consumes( MediaType.APPLICATION_JSON )
class LogoutResource( private val loginManager: LoginManager ){

    @POST fun logoutPost( @Context httpRequest: HttpServletRequest, @HeaderParam( loginTokenParamName )loginTokenUuid: String ): String {
        return logout(httpRequest, loginTokenUuid)
    }

    @GET fun logoutGet( @Context httpRequest: HttpServletRequest, @HeaderParam( loginTokenParamName )loginTokenUuid: String ): String {
        return logout(httpRequest, loginTokenUuid)
    }

    private fun logout(httpRequest: HttpServletRequest, loginTokenUuid: String): String {
        loginManager.logout(loginTokenUuid)
        return ""
    }
}