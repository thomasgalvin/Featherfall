package galvin.ff.users.psql

import galvin.ff.*
import org.junit.Assert
import org.junit.Test

class PsqlUserDbTest {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Roles tests
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test fun should_store_and_retrieve_roles(){
        if( !PSQL.canConnect() ) return
        val userdb = PSQL.randomUserDB()

        val expectedCount = 10
        val map = mutableMapOf<String, Role>()

        val roles = generateRoles(expectedCount)
        for( role in roles ){
            userdb.storeRole(role)
            map[role.name] = role
        }

        for( expected in roles ){
            val loaded = userdb.retrieveRole(expected.name)
            Assert.assertEquals("Loaded role did not match expected", expected, loaded)
        }

        val loadedRoles = userdb.listRoles()
        Assert.assertEquals("Unexpected role count", expectedCount, loadedRoles.size)

        for( loaded in loadedRoles ){
            val expected = map[loaded.name]
            Assert.assertEquals("Loaded role did not match expected", expected, loaded)
        }
    }

    @Test fun should_activate_role(){
        if( !PSQL.canConnect() ) return
        val userdb = PSQL.randomUserDB()

        val inactiveToActive = generateRole( active=false )
        val alwaysInactive = generateRole( active=false )

        userdb.storeRole(inactiveToActive)
        userdb.storeRole(alwaysInactive)
        userdb.activate(inactiveToActive.name)

        val shouldBeActive = userdb.retrieveRole(inactiveToActive.name) ?: throw Exception("Failed to load role from database")
        val shouldBeInactive = userdb.retrieveRole(alwaysInactive.name) ?: throw Exception("Failed to load role from database")

        Assert.assertEquals("Role should have been activated", true, shouldBeActive.active)
        Assert.assertEquals("Role was unintentionally activated", false, shouldBeInactive.active)
    }

    @Test fun should_deactivate_role(){
        if( !PSQL.canConnect() ) return
        val userdb = PSQL.randomUserDB()

        val activeToInactive = generateRole( active=true )
        val alwaysActive = generateRole( active=true )

        userdb.storeRole(activeToInactive)
        userdb.storeRole(alwaysActive)
        userdb.deactivate(activeToInactive.name)

        val shouldBeInactive = userdb.retrieveRole(activeToInactive.name) ?: throw Exception("Failed to load role from database")
        val shouldBeActive = userdb.retrieveRole(alwaysActive.name) ?: throw Exception("Failed to load role from database")

        Assert.assertEquals("Role should have been deactivated", false, shouldBeInactive.active)
        Assert.assertEquals("Role was unintentionally deactivated", true, shouldBeActive.active)
    }

    @Test fun should_update_role(){
        if( !PSQL.canConnect() ) return
        val userdb = PSQL.randomUserDB()

        val expectedCount = 10
        val map = mutableMapOf<String, Role>()

        val roles = generateRoles(expectedCount)
        for( role in roles ){
            userdb.storeRole(role)
            map[role.name] = role
        }

        val toBeUpdated = generateRole()
        userdb.storeRole(toBeUpdated)

        val check = userdb.retrieveRole(toBeUpdated.name)
        Assert.assertEquals("Loaded role did not match expected", toBeUpdated, check)

        val update = generateRole(name = toBeUpdated.name)
        userdb.storeRole(update)

        val shouldBeUpdated = userdb.retrieveRole(toBeUpdated.name)
        Assert.assertEquals("Role was not updated", update, shouldBeUpdated)

        for( expected in roles ){
            val loaded = userdb.retrieveRole(expected.name)
            Assert.assertEquals("Loaded role did not match expected", expected, loaded)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Users tests
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test fun should_store_and_retrieve_user(){
        if( !PSQL.canConnect() ) return
        val( userDB, user ) = testObjects()
        userDB.storeUser(user)

        val loaded = userDB.retrieveUser(user.uuid)
        Assert.assertEquals("Loaded user did not match expected", user, loaded)
    }

    @Test fun should_store_and_retrieve_all_users(){
        if( !PSQL.canConnect() ) return
        val( userDB, _, _, roles ) = testObjects()
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userDB.storeUser(user)
            map[user.uuid] = user
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = userDB.retrieveUser(key)
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }

        val loadedUsers = userDB.retrieveUsers()
        Assert.assertEquals("Unexpected user count", expectedCount, loadedUsers.size)

        for( loaded in loadedUsers ){
            val expected = map[loaded.uuid]
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }
    }

    @Test fun should_store_and_retrieve_all_users_by_serial_number(){
        if( !PSQL.canConnect() ) return
        val( userDB, _, _, roles ) = testObjects()
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userDB.storeUser(user)
            val serial = user.serialNumber
            map[serial] = user
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = userDB.retrieveUserBySerialNumber(key)
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }
    }

    @Test fun should_store_and_retrieve_all_users_by_login(){
        if( !PSQL.canConnect() ) return
        val( userDB, _, _, roles ) = testObjects()
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userDB.storeUser(user)
            val login = user.login
            map[login] = user
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = userDB.retrieveUserByLogin(key)
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }
    }

    @Test fun should_update_user(){
        if( !PSQL.canConnect() ) return
        val( userDB, user ) = testObjects()
        userDB.storeUser(user)

        val updateRoles = generateRoles(userdb = userDB)
        val updated = generateUser(updateRoles, user.uuid)
        userDB.storeUser(updated)

        val loaded = userDB.retrieveUser(user.uuid)
        Assert.assertEquals("Loaded user did not match expected", updated, loaded)
    }

    @Test fun should_update_multiple_users(){
        if( !PSQL.canConnect() ) return
        val( userDB, _, _, roles ) = testObjects()

        val users = mutableListOf<User>()
        for( i in 1 .. 10 ) {
            users.add(generateUser(roles))
        }

        val map = mutableMapOf<String, User>()
        for( user in users ){
            userDB.storeUser( user )
            map[user.uuid] = user
        }

        for( key in map.keys ){
            val loaded = userDB.retrieveUser(key)
            Assert.assertEquals("Loaded user did not match expected", map[key], loaded)
        }

        for( key in map.keys ){
            val user = generateUser(roles, key)
            map[key] = user
            userDB.storeUser( user )
        }

        for( key in map.keys ){
            val loaded = userDB.retrieveUser(key)
            Assert.assertEquals("Loaded user did not match expected", map[key], loaded)
        }

        for( user in users ){
            val loaded = userDB.retrieveUser(user.uuid)
            Assert.assertNotEquals("Loaded user should have been modified", user, loaded)
        }
    }

    @Test fun should_retrieve_uuid_by_login(){
        if( !PSQL.canConnect() ) return
        val( userDB, user, user2 ) = testObjects()
        userDB.storeUser(user)
        userDB.storeUser(user2)

        val uuid = userDB.retrieveUuidByLogin(user.login)
        Assert.assertEquals("Unexpected UUID", user.uuid, uuid)

        val uuid2 = userDB.retrieveUuidByLogin(user2.login)
        Assert.assertEquals("Unexpected UUID", user2.uuid, uuid2)
    }

    @Test fun should_retrieve_uuid_by_serial_number(){
        if( !PSQL.canConnect() ) return
        val( userDB, user, user2 ) = testObjects()
        userDB.storeUser(user)
        userDB.storeUser(user2)

        val uuid = userDB.retrieveUuidBySerialNumber( neverNull( user.serialNumber) )
        Assert.assertEquals("Unexpected UUID", user.uuid, uuid)

        val uuid2 = userDB.retrieveUuidBySerialNumber( neverNull(user2.serialNumber) )
        Assert.assertEquals("Unexpected UUID", user2.uuid, uuid2)
    }

    @Test fun should_retrieve_uuid_by_login_or_serial_number(){
        if( !PSQL.canConnect() ) return
        val( userDB, user, user2 ) = testObjects()
        userDB.storeUser(user)
        userDB.storeUser(user2)

        val uuidA = userDB.retrieveUuid(user.login)
        val uuidB = userDB.retrieveUuid( neverNull( user.serialNumber) )
        Assert.assertEquals("Unexpected UUID", user.uuid, uuidA)
        Assert.assertEquals("Unexpected UUID", user.uuid, uuidB)

        val uuid2A = userDB.retrieveUuid(user2.login)
        val uuid2B = userDB.retrieveUuid( neverNull(user2.serialNumber) )
        Assert.assertEquals("Unexpected UUID", user2.uuid, uuid2A)
        Assert.assertEquals("Unexpected UUID", user2.uuid, uuid2B)
    }

    @Test fun should_lock_and_unlock_user_by_uuid(){
        if( !PSQL.canConnect() ) return
        val( userDB, toBeLocked, neverLocked ) = testObjects()

        userDB.storeUser(toBeLocked)
        userDB.storeUser(neverLocked)

        Assert.assertFalse( "Account should not have been locked", toBeLocked.locked )
        Assert.assertFalse( "Account should not have been locked", neverLocked.locked )

        Assert.assertFalse( "Account should not have been locked", userDB.isLocked( toBeLocked.uuid) )
        Assert.assertFalse( "Account should not have been locked", userDB.isLocked( neverLocked.uuid) )

        userDB.setLocked( toBeLocked.uuid, true)

        Assert.assertTrue( "Account should have been locked", userDB.isLocked( toBeLocked.uuid) )
        Assert.assertFalse( "Account should not have been locked", userDB.isLocked( neverLocked.uuid) )

        val shouldBeLocked = userDB.retrieveUser(toBeLocked.uuid)
        if( shouldBeLocked == null ){
            throw Exception( "Loaded account was null" )
        }
        else {
            Assert.assertTrue("Account should have been locked", shouldBeLocked.locked)
        }

        userDB.setLocked( toBeLocked.uuid, false)

        Assert.assertFalse( "Account should not have been locked", userDB.isLocked( toBeLocked.uuid) )
        Assert.assertFalse( "Account should not have been locked", userDB.isLocked( neverLocked.uuid) )

        val shouldBeUnlocked = userDB.retrieveUser(toBeLocked.uuid)
        if( shouldBeUnlocked == null ){
            throw Exception( "Loaded account was null" )
        }
        else {
            Assert.assertFalse( "Account should not have been locked", shouldBeUnlocked.locked)
        }
    }

    @Test fun should_lock_and_unlock_user_by_login(){
        if( !PSQL.canConnect() ) return
        val( userDB, toBeLocked, neverLocked ) = testObjects()

        userDB.storeUser(toBeLocked)
        userDB.storeUser(neverLocked)

        Assert.assertFalse( "Account should not have been locked", toBeLocked.locked )
        Assert.assertFalse( "Account should not have been locked", neverLocked.locked )

        Assert.assertFalse( "Account should not have been locked", userDB.isLocked( toBeLocked.login) )
        Assert.assertFalse( "Account should not have been locked", userDB.isLocked( neverLocked.login) )

        userDB.setLockedByLogin( toBeLocked.login, true)

        Assert.assertTrue( "Account should have been locked", userDB.isLockedByLogin( toBeLocked.login) )
        Assert.assertFalse( "Account should not have been locked", userDB.isLockedByLogin( neverLocked.login) )

        val shouldBeLocked = userDB.retrieveUserByLogin(toBeLocked.login)
        if( shouldBeLocked == null ){
            throw Exception( "Loaded account was null" )
        }
        else {
            Assert.assertTrue("Account should have been locked", shouldBeLocked.locked)
        }

        userDB.setLockedByLogin( toBeLocked.login, false)

        Assert.assertFalse( "Account should not have been locked", userDB.isLockedByLogin( toBeLocked.login) )
        Assert.assertFalse( "Account should not have been locked", userDB.isLockedByLogin( neverLocked.login) )

        val shouldBeUnlocked = userDB.retrieveUserByLogin(toBeLocked.login)
        if( shouldBeUnlocked == null ){
            throw Exception( "Loaded account was null" )
        }
        else {
            Assert.assertFalse( "Account should not have been locked", shouldBeUnlocked.locked)
        }
    }

    @Test fun should_retrive_users_by_locked(){
        if( !PSQL.canConnect() ) return
        val( userDB, _, _, roles ) = testObjects()
        val lockedCount = 10
        val unlockedCount = 5

        val lockedMap = mutableMapOf<String, User>()
        val unlockedMap = mutableMapOf<String, User>()

        for( i in 1..lockedCount ){
            val user = generateUser(roles, locked=true)
            userDB.storeUser(user)
            lockedMap[user.uuid] = user
        }

        for( i in 1..unlockedCount ){
            val user = generateUser(roles, locked=false)
            userDB.storeUser(user)
            unlockedMap[user.uuid] = user
        }

        val loadedLocked = userDB.retrieveUsersByLocked(true)
        Assert.assertEquals("Unexpected unlocked user count", lockedCount, loadedLocked.size)

        val loadedUnlocked = userDB.retrieveUsersByLocked(false)
        Assert.assertEquals("Unexpected unlocked user count", unlockedCount, loadedUnlocked.size)

        for( loaded in loadedLocked ){
            val expected = lockedMap[loaded.uuid]
            Assert.assertEquals("Unexpected user", expected, loaded)
        }

        for( loaded in loadedUnlocked ){
            val expected = unlockedMap[loaded.uuid]
            Assert.assertEquals("Unexpected user", expected, loaded)
        }
    }

    @Test fun should_retrive_users_by_active(){
        if( !PSQL.canConnect() ) return
        val( userDB, _, _, roles ) = testObjects()
        val activeCount = 10
        val inactiveCount = 5

        val activeMap = mutableMapOf<String, User>()
        val inactiveMap = mutableMapOf<String, User>()

        for( i in 1..activeCount ){
            val user = generateUser(roles, active=true)
            userDB.storeUser(user)
            activeMap[user.uuid] = user
        }

        for( i in 1..inactiveCount ){
            val user = generateUser(roles, active=false)
            userDB.storeUser(user)
            inactiveMap[user.uuid] = user
        }

        val loadedActive = userDB.retrieveUsersByActive(true)
        Assert.assertEquals("Unexpected inactive user count", activeCount, loadedActive.size)

        val loadedInactive = userDB.retrieveUsersByActive(false)
        Assert.assertEquals("Unexpected inactive user count", inactiveCount, loadedInactive.size)

        for( loaded in loadedActive ){
            val expected = activeMap[loaded.uuid]
            Assert.assertEquals("Unexpected user", expected, loaded)
        }

        for( loaded in loadedInactive ){
            val expected = inactiveMap[loaded.uuid]
            Assert.assertEquals("Unexpected user", expected, loaded)
        }
    }

    @Test fun should_activate_and_deactivate_user_by_login(){
        if( !PSQL.canConnect() ) return
        val( userDB, _, _, roles ) = testObjects()

        val toBeActive = generateUser(roles, active=false)
        val neverActive = generateUser(roles, active=false)

        userDB.storeUser(toBeActive)
        userDB.storeUser(neverActive)

        userDB.storeUser(toBeActive)
        userDB.storeUser(neverActive)

        Assert.assertFalse( "Account should not have been active", toBeActive.active )
        Assert.assertFalse( "Account should not have been active", neverActive.active )

        Assert.assertFalse( "Account should not have been active", userDB.isActive( toBeActive.login) )
        Assert.assertFalse( "Account should not have been active", userDB.isActive( neverActive.login) )

        userDB.setActiveByLogin( toBeActive.login, true)

        Assert.assertTrue( "Account should have been active", userDB.isActiveByLogin( toBeActive.login) )
        Assert.assertFalse( "Account should not have been active", userDB.isActiveByLogin( neverActive.login) )

        val shouldBeActive = userDB.retrieveUserByLogin(toBeActive.login)
        if( shouldBeActive == null ){
            throw Exception( "Loaded account was null" )
        }
        else {
            Assert.assertTrue("Account should have been active", shouldBeActive.active)
        }

        userDB.setActiveByLogin( toBeActive.login, false)

        Assert.assertFalse( "Account should not have been active", userDB.isActiveByLogin( toBeActive.login) )
        Assert.assertFalse( "Account should not have been active", userDB.isActiveByLogin( neverActive.login) )

        val shouldBeInactive = userDB.retrieveUserByLogin(toBeActive.login)
        if( shouldBeInactive == null ){
            throw Exception( "Loaded account was null" )
        }
        else {
            Assert.assertFalse( "Account should not have been active", shouldBeInactive.active)
        }
    }

    @Test fun should_update_password_by_login(){
        if( !PSQL.canConnect() ) return
        val (userDB, user) = testObjects()
        userDB.storeUser(user)

        val newPassword = "1234567890"
        userDB.setPasswordByLogin(user.login, newPassword)
        Assert.assertTrue( "Password should have worked", userDB.validatePassword(user.uuid, newPassword) )
    }

    @Test fun should_update_password_by_uuid(){
        if( !PSQL.canConnect() ) return
        val (userDB, user) = testObjects()
        userDB.storeUser(user)

        val newPassword = "1234567890"
        userDB.setPasswordByUuid(user.uuid, newPassword)
        Assert.assertTrue( "Password should have worked", userDB.validatePassword(user.uuid, newPassword) )
    }

    @Test fun should_retrieve_credentials(){
        if( !PSQL.canConnect() ) return
        val (userDB, user) = testObjects()
        userDB.storeUser(user)

        val expected = user.getCredentials()
        val loaded = userDB.retrieveCredentials(user.uuid)
        Assert.assertEquals("Unexpected credentials", expected, loaded)
    }

    @Test fun should_update_credentials(){
        if( !PSQL.canConnect() ) return
        val (userDB, user) = testObjects()
        userDB.storeUser(user)

        val expectedCredentials = CertificateData(
                credential = uuid(),
                serialNumber = uuid(),
                distinguishedName = uuid(),
                countryCode = uuid(),
                citizenship = uuid()
        )

        userDB.updateCredentials(user.uuid, expectedCredentials)
        val expected = user.withCredentials(expectedCredentials)
        val loaded = userDB.retrieveUser(user.uuid)
        Assert.assertEquals("Unexpected credentials", expected, loaded)

        val loadedCredentials = userDB.retrieveCredentials(user.uuid)
        Assert.assertEquals("Unexpected credentials", expectedCredentials, loadedCredentials)
    }

    private fun testObjects(): SqliteUserDbTestObjects{
        val userdb = PSQL.randomUserDB()
        val roles = generateRoles(userdb = userdb)
        val user1 = generateUser(roles)
        val user2 = generateUser(roles)

        return SqliteUserDbTestObjects(userdb, user1, user2, roles )
    }

    class SqliteUserDbTestObjects(private val userDB: UserDB, private val user1: User, private val user2: User, private val roles: List<Role> ){
        operator fun component1(): UserDB{ return userDB }
        operator fun component2(): User{ return user1 }
        operator fun component3(): User{ return user2 }
        operator fun component4(): List<Role>{ return roles }
    }
}