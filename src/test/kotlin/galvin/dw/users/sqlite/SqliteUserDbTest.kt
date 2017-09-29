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
}