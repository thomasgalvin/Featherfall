package galvin.ff.resources

import galvin.ff.*
import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path( "users" )
@Produces( MediaType.APPLICATION_JSON )
@Consumes( MediaType.APPLICATION_JSON )
class UserResource( userDB: UserDB,
                    loginManager: LoginManager,
                    passwordRequirements: PasswordRequirements = PasswordRequirements(),
                    auditDB: AuditDB = NoOpAuditDB() ){

}