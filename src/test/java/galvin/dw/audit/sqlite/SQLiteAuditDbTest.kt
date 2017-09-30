package galvin.dw.audit.sqlite

import galvin.dw.*
import galvin.dw.sqlite.SQLiteAuditDB
import org.junit.Assert
import org.junit.Test

import java.io.File
import java.util.*

import galvin.dw.uuid

class SQLiteAuditDbTest {

    @Test
    @Throws(Exception::class)
    fun should_not_create_tables_twice() {
        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)
        val audit2 = SQLiteAuditDB(auditFile)
    }

    @Test
    @Throws(Exception::class)
    fun should_retrieve_system_info_list() {
        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)

        val expectedCount = 10
        val map = HashMap<String, SystemInfo>()

        for (i in 0 until expectedCount) {
            val system = randomSystemInfo()
            map.put(system.uuid, system)
            audit.store(system)
        }

        val allSystemInfo = audit.retrieveAllSystemInfo()
        Assert.assertEquals("Unexpected system info count", expectedCount.toLong(), allSystemInfo.size.toLong())

        for (loaded in allSystemInfo) {
            val expected = map[loaded.uuid]
            Assert.assertEquals("Loaded system info did not match expected", expected, loaded)
        }
    }

    @Test
    @Throws(Exception::class)
    fun should_retrieve_system_info_by_uuid() {
        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)

        val expectedCount = 10
        val map = HashMap<String, SystemInfo>()

        for (i in 0 until expectedCount) {
            val system = randomSystemInfo()
            map.put(system.uuid, system)
            audit.store(system)
        }


        for (key in map.keys) {
            val expected = map[key]
            val loaded = audit.retrieveSystemInfo(key)
            Assert.assertEquals("Loaded system info did not match expected", expected, loaded)
        }
    }

    @Test
    @Throws(Exception::class)
    fun should_retrieve_current_system_info() {
        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)
        val system = randomSystemInfo()
        audit.store(system)
        audit.storeCurrentSystemInfo(system.uuid)
        val loaded = audit.retrieveCurrentSystemInfo()
        Assert.assertEquals("Loaded current system info did not match expected", system, loaded)
    }

    @Test
    @Throws(Exception::class)
    fun should_retrieve_access_info_by_dates() {
        val now = System.currentTimeMillis()
        val then = now - 10000
        val later = now + 10000
        val expectedCount = 10

        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)

        val system = randomSystemInfo()
        audit.store(system)

        val expectedEntries = generateAccessInfo(system, expectedCount, audit)
        val loadedEntries = audit.retrieveAccessInfo(then, later)

        Assert.assertEquals("Unexpected system info count", expectedCount.toLong(), loadedEntries.size.toLong())

        for (i in expectedEntries.indices) {
            val expected = expectedEntries[i]
            val loaded = loadedEntries[i]
            Assert.assertEquals("Loaded access info did not match expected", expected, loaded)
        }
    }

    @Test
    @Throws(Exception::class)
    fun should_retrieve_access_info_by_dates_and_uuid() {
        val now = System.currentTimeMillis()
        val then = now - 10000
        val later = now + 10000
        val expectedCount = 10

        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)

        val systems = ArrayList<SystemInfo>()
        val map = HashMap<String, List<AccessInfo>>()

        for (i in 0 until expectedCount) {
            val system = randomSystemInfo()
            audit.store(system)
            systems.add(system)

            val expectedEntries = generateAccessInfo(system, expectedCount, audit)
            map.put(system.uuid, expectedEntries)
        }

        for (system in systems) {
            val systemUuid = system.uuid
            val expectedEntries = map[systemUuid]
            val loadedEntries = audit.retrieveAccessInfo(systemUuid, then, later)

            Assert.assertNotNull( "Expected entries was null", expectedEntries )
            Assert.assertNotNull( "Loaded entries was null", loadedEntries )

            if( expectedEntries != null ) {
                Assert.assertEquals("Unexpected system info count", expectedEntries.size.toLong(), loadedEntries.size.toLong())
                for (i in expectedEntries.indices) {
                    val expected = expectedEntries.get(i)
                    val loaded = loadedEntries[i]
                    Assert.assertEquals("Loaded access info did not match expected", expected, loaded)
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun should_not_retrieve_access_info_by_dates_before() {
        val now = System.currentTimeMillis()
        val muchEarlier = now - 15000
        val earlier = now - 10000
        val expectedCount = 10

        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)

        val system = randomSystemInfo()
        audit.store(system)

        generateAccessInfo(system, expectedCount, audit)
        val loadedEntries = audit.retrieveAccessInfo(muchEarlier, earlier)

        Assert.assertEquals("Unexpected system info count", 0, loadedEntries.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun should_not_retrieve_access_info_by_dates_after() {
        val now = System.currentTimeMillis()
        val later = now + 10000
        val muchLater = now + 20000
        val expectedCount = 10

        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)

        val system = randomSystemInfo()
        audit.store(system)

        generateAccessInfo(system, expectedCount, audit)
        val loadedEntries = audit.retrieveAccessInfo(later, muchLater)

        Assert.assertEquals("Unexpected system info count", 0, loadedEntries.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun should_not_retrieve_access_info_by_uuid() {
        val now = System.currentTimeMillis()
        val then = now - 10000
        val later = now + 10000
        val expectedCount = 10

        val auditFile = randomAuditDbFile()
        val audit = SQLiteAuditDB(auditFile)

        val system = randomSystemInfo()
        generateAccessInfo(system, expectedCount, audit)

        val loadedEntries = audit.retrieveAccessInfo(then, later)
        Assert.assertEquals("Unexpected system info count", expectedCount.toLong(), loadedEntries.size.toLong())

        val randomSystemUuid = uuid()
        val nonExistentSystemEntries = audit.retrieveAccessInfo(randomSystemUuid, then, later)
        Assert.assertEquals("Unexpected system info count", 0, nonExistentSystemEntries.size.toLong())
    }

    @Throws(Exception::class)
    private fun generateAccessInfo(system: SystemInfo, expectedCount: Int, audit: AuditDB): List<AccessInfo> {
        val result = ArrayList<AccessInfo>()

        for (i in 0 until expectedCount) {
            val info = randomAccessInfo(system.uuid)
            audit.log(info, console)
            result.add(info)

            Thread.sleep(1)
        }

        return result
    }

    companion object {
        private val console = false

        private fun randomAuditDbFile(): File {
            return File("target/audit-" + uuid() + ".dat")
        }

        private fun randomSystemInfo(): SystemInfo {
            return SystemInfo(
                    "serial:" + uuid(),
                    "name:" + uuid(),
                    "version" + uuid(),
                    "Unclassified-" + uuid(),
                    "guide:" + uuid(),
                    Arrays.asList(
                            uuid(),
                            uuid(),
                            uuid(),
                            uuid(),
                            uuid()
                    ),
                    "uuid:" + uuid()
            )
        }

        private fun randomAccessInfo(systemInfoUuid: String): AccessInfo {
            val mods = ArrayList<Modification>()
            for (i in 0..9) {
                mods.add(Modification(
                        "field:" + uuid(),
                        "old:" + uuid(),
                        "new:" + uuid()
                ))
            }

            val random = Random()
            val loginTypes = LoginType.values()
            val accessTypes = AccessType.values()

            val loginType = loginTypes[random.nextInt(loginTypes.size)]
            val accessType = accessTypes[random.nextInt(accessTypes.size)]
            val permissionGranted = random.nextBoolean()

            return AccessInfo(
                    "user" + uuid(),
                    loginType,
                    "proxy:" + uuid(),
                    System.currentTimeMillis(),
                    "resourceUUID:" + uuid(),
                    "resourceName:" + uuid(),
                    "classification" + uuid(),
                    "resource" + uuid(),
                    accessType,
                    permissionGranted,
                    systemInfoUuid,
                    mods,
                    "uuid:" + uuid()
            )
        }
    }
}