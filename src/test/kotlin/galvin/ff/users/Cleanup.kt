package galvin.ff.users

import org.junit.Test

class Cleanup{
    @Test fun cleanup(){
        for (database in databases) {
            if (!database.canConnect()) continue

            database.cleanup()
        }
    }
}