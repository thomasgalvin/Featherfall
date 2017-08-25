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

        val loadedRoles = userdb.listRoles()
        Assert.assertEquals("Unexpected role count", expectedCount, loadedRoles.size)

        for( loaded in loadedRoles ){
            val expected = map[loaded.name]
            Assert.assertEquals("Loaded role did not match expected", expected, loaded)
        }
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
        val random = Random()

        for( i in 1..count ){
            val permissions = mutableListOf<String>()
            for( j in 1..count ){
                permissions.add( "permission:" + uuid() )
            }

            val role = Role(
                "name:" + uuid(),
                    permissions,
                    random.nextBoolean()
            )
            result.add(role)
        }

        return result
    }
}