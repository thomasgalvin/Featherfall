package galvin.dw.resources

import galvin.dw.*

class UserResource( userDB: UserDB,
                    loginManager: LoginManager,
                    passwordRequirements: PasswordRequirements = PasswordRequirements(),
                    auditDB: AuditDB = NoOpAuditDB() )