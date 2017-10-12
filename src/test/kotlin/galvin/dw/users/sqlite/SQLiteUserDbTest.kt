package galvin.dw.users.sqlite

import galvin.dw.Role
import galvin.dw.User
import galvin.dw.UserDB
import galvin.dw.neverNull
import org.junit.Assert
import org.junit.Test

class SQLiteUserDbTest {

    @Test
    fun should_not_create_tables_twice(){
        val userdb = randomUserDB()
        val userdb2 = randomUserDB()

        Assert.assertNotNull(userdb)
        Assert.assertNotNull(userdb2)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Roles tests
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun should_store_and_retrieve_roles(){
        val userdb = randomUserDB()

        val expectedCount = 10
        val map = mutableMapOf<String, Role>()

        val roles = generateRoles(expectedCount)
        for( role in roles ){
            userdb.storeRole(role)
            map.put(role.name, role)
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

    @Test
    fun should_activate_role(){
        val userdb = randomUserDB()

        val inactiveToActive = generateRole( active=false )
        val alwaysInactive = generateRole( active=false )

        userdb.storeRole(inactiveToActive)
        userdb.storeRole(alwaysInactive)
        userdb.activate(inactiveToActive.name)

        val shouldBeActive = userdb.retrieveRole(inactiveToActive.name)
        if( shouldBeActive == null ) throw Exception("Failed to load role from database")

        val shouldBeInactive = userdb.retrieveRole(alwaysInactive.name)
        if( shouldBeInactive == null ) throw Exception("Failed to load role from database")

        Assert.assertEquals("Role should have been activated", true, shouldBeActive.active)
        Assert.assertEquals("Role was unintentionally activated", false, shouldBeInactive.active)
    }

    @Test
    fun should_deactivate_role(){
        val userdb = randomUserDB()

        val activeToInactive = generateRole( active=true )
        val alwaysActive = generateRole( active=true )

        userdb.storeRole(activeToInactive)
        userdb.storeRole(alwaysActive)
        userdb.deactivate(activeToInactive.name)

        val shouldBeInactive = userdb.retrieveRole(activeToInactive.name)
        if( shouldBeInactive == null ) throw Exception("Failed to load role from database")

        val shouldBeActive = userdb.retrieveRole(alwaysActive.name)
        if( shouldBeActive == null ) throw Exception("Failed to load role from database")

        Assert.assertEquals("Role should have been deactivated", false, shouldBeInactive.active)
        Assert.assertEquals("Role was unintentionally deactivated", true, shouldBeActive.active)
    }

    @Test
    fun should_update_role(){
        val userdb = randomUserDB()

        val expectedCount = 10
        val map = mutableMapOf<String, Role>()

        val roles = generateRoles(expectedCount)
        for( role in roles ){
            userdb.storeRole(role)
            map.put(role.name, role)
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

    @Test
    fun should_store_and_retrieve_user(){
        val( userDB, user ) = testObjects()
        userDB.storeUser(user)

        val loaded = userDB.retrieveUser(user.uuid)
        Assert.assertEquals("Loaded user did not match expected", user, loaded)
    }

    @Test
    fun should_store_and_retrieve_all_users(){
        val( userDB, _, _, roles ) = testObjects()
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userDB.storeUser(user)
            map.put( user.uuid, user )
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

    @Test
    fun should_store_and_retrieve_all_users_by_serial_number(){
        val( userDB, _, _, roles ) = testObjects()
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userDB.storeUser(user)
            val serial = user.serialNumber
            if( serial != null ){
                map.put(serial, user)
            }
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = userDB.retrieveUserBySerialNumber(key)
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }
    }

    @Test
    fun should_store_and_retrieve_all_users_by_login(){
        val( userDB, _, _, roles ) = testObjects()
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userDB.storeUser(user)
            val login = user.login
            map.put(login, user)
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = userDB.retrieveUserByLogin(key)
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }
    }

    @Test
    fun should_update_user(){
        val( userDB, user ) = testObjects()
        userDB.storeUser(user)

        val updateRoles = generateRoles(userdb = userDB)
        val updated = generateUser(updateRoles, user.uuid)
        userDB.storeUser(updated)

        val loaded = userDB.retrieveUser(user.uuid)
        Assert.assertEquals("Loaded user did not match expected", updated, loaded)
    }

    @Test
    fun should_update_multiple_users(){
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

    @Test
    fun should_retrieve_uuid_by_login(){
        val( userDB, user, user2 ) = testObjects()
        userDB.storeUser(user)
        userDB.storeUser(user2)

        val uuid = userDB.retrieveUuidByLogin(user.login)
        Assert.assertEquals("Unexpected UUID", user.uuid, uuid)

        val uuid2 = userDB.retrieveUuidByLogin(user2.login)
        Assert.assertEquals("Unexpected UUID", user2.uuid, uuid2)
    }

    @Test
    fun should_retrieve_uuid_by_serial_number(){
        val( userDB, user, user2 ) = testObjects()
        userDB.storeUser(user)
        userDB.storeUser(user2)

        val uuid = userDB.retrieveUuidBySerialNumber( neverNull( user.serialNumber) )
        Assert.assertEquals("Unexpected UUID", user.uuid, uuid)

        val uuid2 = userDB.retrieveUuidBySerialNumber( neverNull(user2.serialNumber) )
        Assert.assertEquals("Unexpected UUID", user2.uuid, uuid2)
    }

    @Test
    fun should_retrieve_uuid_by_login_or_serial_number(){
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

    @Test
    fun should_lock_and_unlock_user_by_uuid(){
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

    @Test
    fun should_lock_and_unlock_user_by_login(){
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

    @Test
    fun should_retrive_users_by_locked(){
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

    @Test
    fun should_retrive_users_by_active(){
        val( userDB, _, _, roles ) = testObjects()
        val activeCount = 10
        val unactiveCount = 5

        val activeMap = mutableMapOf<String, User>()
        val unactiveMap = mutableMapOf<String, User>()

        for( i in 1..activeCount ){
            val user = generateUser(roles, active=true)
            userDB.storeUser(user)
            activeMap[user.uuid] = user
        }

        for( i in 1..unactiveCount ){
            val user = generateUser(roles, active=false)
            userDB.storeUser(user)
            unactiveMap[user.uuid] = user
        }

        val loadedActive = userDB.retrieveUsersByActive(true)
        Assert.assertEquals("Unexpected unactive user count", activeCount, loadedActive.size)

        val loadedUnactive = userDB.retrieveUsersByActive(false)
        Assert.assertEquals("Unexpected unactive user count", unactiveCount, loadedUnactive.size)

        for( loaded in loadedActive ){
            val expected = activeMap[loaded.uuid]
            Assert.assertEquals("Unexpected user", expected, loaded)
        }

        for( loaded in loadedUnactive ){
            val expected = unactiveMap[loaded.uuid]
            Assert.assertEquals("Unexpected user", expected, loaded)
        }
    }

    @Test
    fun should_activate_and_deactivate_user_by_login(){
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

        val shouldBeUnactive = userDB.retrieveUserByLogin(toBeActive.login)
        if( shouldBeUnactive == null ){
            throw Exception( "Loaded account was null" )
        }
        else {
            Assert.assertFalse( "Account should not have been active", shouldBeUnactive.active)
        }
    }

    private fun testObjects(): SqliteUserDbTestObjects{
        val userDB = randomUserDB()
        val roles = generateRoles(userdb = userDB)
        val user1 = generateUser(roles)
        val user2 = generateUser(roles)

        return SqliteUserDbTestObjects(userDB, user1, user2, roles )
    }

    class SqliteUserDbTestObjects(val userDB: UserDB, val user1: User, val user2: User, val roles: List<Role> ){
        operator fun component1(): UserDB{ return userDB }
        operator fun component2(): User{ return user1 }
        operator fun component3(): User{ return user2 }
        operator fun component4(): List<Role>{ return roles }
    }
}