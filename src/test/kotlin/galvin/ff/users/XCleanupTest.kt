package galvin.ff.users

import org.junit.Test

class XCleanupTest{
    @Test fun cleanup(){
        for (database in databases) {
            if (!database.canConnect()) continue

            database.cleanup()
        }
    }
}