package galvin.ff.resources

import galvin.ff.*

class UserResource( userDB: UserDB,
                    loginManager: LoginManager,
                    passwordRequirements: PasswordRequirements = PasswordRequirements(),
                    auditDB: AuditDB = NoOpAuditDB() )