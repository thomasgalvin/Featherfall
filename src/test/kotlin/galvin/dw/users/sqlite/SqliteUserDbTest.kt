package galvin.dw.users.sqlite

import galvin.dw.Role
import galvin.dw.SQLiteUserDB
import galvin.dw.UserDB
import galvin.dw.uuid
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.*

class SqliteUserDbTest {
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
            var loaded = userdb.retrieveRole(expected.name)
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
    fun should_deactivate_role(){
        val userdb = userDB()

        val activeToDeactive = generateRole( active=true )
        userdb.storeRole(activeToDeactive)

        val alwaysActive= generateRole( active=true )
        userdb.storeRole(alwaysActive)

        userdb.deactivate(activeToDeactive.name)

        val shouldBeDeactive = userdb.retrieveRole(activeToDeactive.name)
        if( shouldBeDeactive == null ) throw Exception("Failed to load role from database")

        val shouldBeActive = userdb.retrieveRole(alwaysActive.name)
        if( shouldBeActive == null ) throw Exception("Failed to load role from database")

        Assert.assertEquals("Role should have been deactivated", false, shouldBeDeactive.active)
        Assert.assertEquals("Role was unintentionally deactivated", true, shouldBeActive.active)
    }

    // Utility code

    private fun userDB(): UserDB{
        return SQLiteUserDB( randomUserDbFile() )
    }

    private fun randomUserDbFile(): File {
        return File( "target/users-" + uuid() + ".dat" )
    }

    private fun generateRoles( count: Int ): List<Role>  {
        val result = mutableListOf<Role>()

        for( i in 1..count ){
            result.add( generateRole(count) )
        }

        return result
    }

    private fun generateRole( permissonCount: Int = 10, active: Boolean? = null ): Role {
        val random = Random()
        val isActive = if(active == null) random.nextBoolean() else active

        val permissions = mutableListOf<String>()

        for (i in 1..permissonCount) {
            for (j in 1..permissonCount) {
                permissions.add("permission:" + uuid())
            }
        }

        return Role(
                "name:" + uuid(),
                permissions,
                isActive
        )
    }
}