package galvin.dw.users.sqlite

import galvin.dw.SQLiteUserDB
import org.junit.Test
import java.io.File
import java.util.*

class UserTest{
    @Test
    fun should_create_tables() {
        val userFile = randomUserDbFile()
        val users = SQLiteUserDB(userFile)
    }

    private fun randomUserDbFile(): File {
        return File( "target/users-" + UUID.randomUUID().toString() + ".dat" )
    }
}