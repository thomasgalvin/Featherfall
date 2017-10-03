package galvin.dw.users.sqlite

import galvin.dw.Role
import galvin.dw.User
import org.junit.Assert
import org.junit.Test

class SqliteUserDbTest {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Roles tests
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun should_not_create_tables_twice(){
        val userdb = userDB()
        val userdb2 = userDB()

        Assert.assertNotNull(userdb)
        Assert.assertNotNull(userdb2)
    }

    @Test
    fun should_store_and_retrieve_roles(){
        val userdb = userDB()

        val expectedCount = 10;
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
        val userdb = userDB()

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
        val userdb = userDB()

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
        val userdb = userDB()

        val expectedCount = 10;
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Users tests
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun should_store_and_retrieve_user(){
        val userdb = userDB()
        val roles = generateRoles(userdb = userdb)

        val user = generateUser(roles)
        userdb.storeUser(user)

        val loaded = userdb.retrieveUser(user.uuid)
        Assert.assertEquals("Loaded user did not match expected", user, loaded)
    }

    @Test
    fun should_store_and_retrieve_all_users_by_serial_number(){
        val userdb = userDB()
        val roles = generateRoles(userdb = userdb)
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userdb.storeUser(user)
            val serial = user.serialNumber
            if( serial != null ){
                map.put(serial, user)
            }
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = userdb.retrieveUserBySerialNumber(key)
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }

        val loadedUsers = userdb.retrieveUsers()
        for( loaded in loadedUsers ){
            val expected = map[loaded.serialNumber]
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }
    }

    @Test
    fun should_store_and_retrieve_all_users_by_login(){
        val userdb = userDB()
        val roles = generateRoles(userdb = userdb)
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userdb.storeUser(user)
            val login = user.login
            map.put(login, user)
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = userdb.retrieveUserByLogin(key)
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }

        val loadedUsers = userdb.retrieveUsers()
        for( loaded in loadedUsers ){
            val expected = map[loaded.login]
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }
    }

    @Test
    fun should_store_and_retrieve_all_users(){
        val userdb = userDB()
        val roles = generateRoles(userdb = userdb)
        val expectedCount = 10

        val map = mutableMapOf<String, User>()
        for( i in 1..expectedCount ){
            val user = generateUser(roles)
            userdb.storeUser(user)
            map.put( user.uuid, user )
        }

        for( key in map.keys ){
            val expected = map[key]
            val loaded = userdb.retrieveUser(key)
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }

        val loadedUsers = userdb.retrieveUsers()
        for( loaded in loadedUsers ){
            val expected = map[loaded.uuid]
            Assert.assertEquals("Loaded user did not match expected", expected, loaded)
        }
    }

    @Test
    fun should_update_user(){
        val userdb = userDB()
        val roles = generateRoles(userdb = userdb)

        val user = generateUser(roles)
        userdb.storeUser(user)

        val updateRoles = generateRoles(userdb = userdb)
        val updated = generateUser(updateRoles, user.uuid)
        userdb.storeUser(updated)

        val loaded = userdb.retrieveUser(user.uuid)
        Assert.assertEquals("Loaded user did not match expected", updated, loaded)
    }

    @Test
    fun should_update_multiple_users(){
        val userdb = userDB()
        val roles = generateRoles(userdb = userdb)

        val users = mutableListOf<User>()
        for( i in 1 .. 10 ) {
            users.add(generateUser(roles))
        }

        val map = mutableMapOf<String, User>()
        for( user in users ){
            userdb.storeUser( user )
            map[user.uuid] = user
        }

        for( key in map.keys ){
            val loaded = userdb.retrieveUser(key)
            Assert.assertEquals("Loaded user did not match expected", map[key], loaded)
        }

        for( key in map.keys ){
            val user = generateUser(roles, key)
            map[key] = user
            userdb.storeUser( user )
        }

        for( key in map.keys ){
            val loaded = userdb.retrieveUser(key)
            Assert.assertEquals("Loaded user did not match expected", map[key], loaded)
        }

        for( user in users ){
            val loaded = userdb.retrieveUser(user.uuid)
            Assert.assertNotEquals("Loaded user should have been modified", user, loaded)
        }
    }

    @Test
    fun should_retrieve_uuid_by_login(){
        val userDB = userDB()
        val roles = generateRoles(userdb = userDB)

        val user = generateUser(roles)
        userDB.storeUser(user)

        val user2 = generateUser(roles)
        userDB.storeUser(user2)

        val uuid = userDB.retrieveUuidByLogin(user.login)
        Assert.assertEquals("Unexpected UUID", user.uuid, uuid)

        val uuid2 = userDB.retrieveUuidByLogin(user2.login)
        Assert.assertEquals("Unexpected UUID", user2.uuid, uuid2)
    }

    @Test
    fun should_lock_and_unlock_user_by_uuid(){
        val userDB = userDB()
        val roles = generateRoles(userdb = userDB)

        val toBeLocked = generateUser(roles)
        val neverLocked = generateUser(roles)

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
        val userDB = userDB()
        val roles = generateRoles(userdb = userDB)

        val toBeLocked = generateUser(roles)
        val neverLocked = generateUser(roles)

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
}