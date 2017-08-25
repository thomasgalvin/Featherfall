package galvin.dw.users.sqlite

import galvin.dw.*
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.*

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
        val contact = generateContactInfo()
        val roles = generateRoles(userdb = userdb)


        val userRoles = mutableListOf<String>()
        for( role in roles.subList(3, 5) ){
            userRoles.add( role.name )
        }

        val user = User(
                "login:" + uuid(),
                "passwordHash:" + uuid(),
                "name:" + uuid(),
                "displayName:" + uuid(),
                "sortName:" + uuid(),
                "prependToName:" + uuid(),
                "appendToName:" + uuid(),
                "credential:" + uuid(),
                "serialNumber:" + uuid(),
                "distinguishedName:" + uuid(),
                "homeAgency:" + uuid(),
                "agency:" + uuid(),
                "countryCode:" + uuid(),
                "citizenship:" + uuid(),
                System.currentTimeMillis(),
                true,
                uuid(),
                contact,
                userRoles
        )

        userdb.storeUser(user)

        val loaded = userdb.retrieveUser(user.uuid)
        Assert.assertEquals("Loaded user did not match expected", user, loaded)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Utility code
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun userDB(): UserDB{
        return SQLiteUserDB( randomUserDbFile() )
    }

    private fun randomUserDbFile(): File {
        return File( "target/users-" + uuid() + ".dat" )
    }

    private fun generateRoles( count: Int = 10, userdb: UserDB? = null ): List<Role>  {
        val result = mutableListOf<Role>()

        for( i in 1..count ){
            result.add( generateRole(count) )
        }

        if( userdb != null ){
            for( role in result ){
                userdb.storeRole(role)
            }
        }

        return result
    }

    private fun generateRole( permissonCount: Int = 10, active: Boolean? = null, name: String = "name:" + uuid() ): Role {
        val random = Random()
        val isActive = if(active == null) random.nextBoolean() else active

        val permissions = mutableListOf<String>()

        for (i in 1..permissonCount) {
            for (j in 1..permissonCount) {
                permissions.add("permission:" + uuid())
            }
        }

        return Role(
                name,
                permissions,
                isActive
        )
    }

    private fun generateContactInfo(): List<ContactInfo>{
        val result = mutableListOf<ContactInfo>()
        for( i in 1..5 ){
            result.add(
                    ContactInfo(
                            "type:" + uuid(),
                            "description:" + uuid(),
                            "contact:" + uuid(),
                            i == 1
                    )
            )
        }
        return result
    }
}