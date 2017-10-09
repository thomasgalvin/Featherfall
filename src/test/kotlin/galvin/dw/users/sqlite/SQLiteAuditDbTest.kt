package galvin.dw.users.sqlite

import galvin.dw.*
import org.junit.Assert
import org.junit.Test
import java.util.*

class SQLiteAuditDbTest {
    @Test
    fun should_not_create_tables_twice() {
        val audit = randomAuditDB()
        val audit2 = randomAuditDB()

        Assert.assertNotNull( "Audit database was null", audit )
        Assert.assertNotNull( "Audit database was null", audit2 )
    }

    @Test
    fun should_retrieve_system_info_list() {
        val audit = randomAuditDB()

        val expectedCount = 10
        val map = HashMap<String, SystemInfo>()

        for (i in 0 until expectedCount) {
            val system = randomSystemInfo()
            map.put(system.uuid, system)
            audit.storeSystemInfo(system)
        }

        val allSystemInfo = audit.retrieveAllSystemInfo()
        Assert.assertEquals("Unexpected system info count", expectedCount, allSystemInfo.size )

        for (loaded in allSystemInfo) {
            val expected = map[loaded.uuid]
            Assert.assertEquals("Loaded system info did not match expected", expected, loaded)
        }
    }

    @Test
    fun should_retrieve_system_info_by_uuid() {
        val audit = randomAuditDB()

        val expectedCount = 10
        val map = HashMap<String, SystemInfo>()

        for (i in 0 until expectedCount) {
            val system = randomSystemInfo()
            map.put(system.uuid, system)
            audit.storeSystemInfo(system)
        }

        for (key in map.keys) {
            val expected = map[key]
            val loaded = audit.retrieveSystemInfo(key)
            Assert.assertEquals("Loaded system info did not match expected", expected, loaded)
        }
    }

    @Test
    fun should_retrieve_current_system_info() {
        val audit = randomAuditDB()
        val system = randomSystemInfo()
        audit.storeSystemInfo(system)
        audit.storeCurrentSystemInfo(system.uuid)

        val loaded = audit.retrieveCurrentSystemInfo()
        Assert.assertEquals("Loaded current system info did not match expected", system, loaded)

        val uuid = audit.retrieveCurrentSystemInfoUuid()
        Assert.assertEquals("Loaded current system info uuiddid not match expected", system.uuid, uuid)
    }

    @Test
    fun should_store_system_info_list_and_retrieve_current() {
        val audit = randomAuditDB()

        val map = HashMap<String, SystemInfo>()
        val expectedCount = 11

        val current = randomSystemInfo()
        map.put(current.uuid, current)
        audit.storeSystemInfo(current)
        audit.storeCurrentSystemInfo(current.uuid)

        for (i in 1..10) {
            val system = randomSystemInfo()
            map.put(system.uuid, system)
            audit.storeSystemInfo(system)
        }

        val allSystemInfo = audit.retrieveAllSystemInfo()
        Assert.assertEquals("Unexpected system info count", expectedCount, allSystemInfo.size )

        for (loaded in allSystemInfo) {
            val expected = map[loaded.uuid]
            Assert.assertEquals("Loaded system info did not match expected", expected, loaded)
        }

        val loadedCurrent = audit.retrieveCurrentSystemInfo()
        Assert.assertEquals("Loaded current system info did not match expected", current, loadedCurrent)

    }

    @Test
    fun should_throw_when_current_system_info_uuid_not_present(){
        val audit = randomAuditDB()
        try{
            audit.storeCurrentSystemInfo( uuid() )
            throw Exception( "Audit DB should have thrown" )
        }
        catch( ex: Exception ){
            Assert.assertEquals( "Unexpected error", ex.message, ERROR_CURRENT_SYSTEM_INFO_UUID_NOT_PRESENT )
        }
    }

    @Test
    fun should_retrieve_access_info_by_dates() {
        val now = System.currentTimeMillis()
        val then = now - 10000
        val later = now + 10000
        val expectedCount = 10

        val audit = randomAuditDB()

        val system = randomSystemInfo()
        audit.storeSystemInfo(system)

        val expectedEntries = generateAccessInfo(system, expectedCount, audit)
        val loadedEntries = audit.retrieveAccessInfo(startTimestamp = then, endTimestamp = later)

        Assert.assertEquals("Unexpected system info count", expectedCount.toLong(), loadedEntries.size.toLong())

        for (i in expectedEntries.indices) {
            val expected = expectedEntries[i]
            val loaded = loadedEntries[i]
            Assert.assertEquals("Loaded access info did not match expected", expected, loaded)
        }
    }

    @Test
    fun should_retrieve_access_info_by_user_uuid(){
        val audit = randomAuditDB()
        val system = randomSystemInfo()

        val count = 10
        val myUserUuid = uuid()

        generateAccessInfo(system, count, audit )
        val mine = generateAccessInfo(system, count, audit, myUserUuid)
        generateAccessInfo(system, count, audit )

        val loadedMine = audit.retrieveAccessInfo(userUuid = myUserUuid)
        Assert.assertEquals("Loaded access info did not match expected", mine, loadedMine)
    }

    @Test
    fun should_retrieve_access_info_by_user_uuid_and_permission_granted(){
        val audit = randomAuditDB()
        val system = randomSystemInfo()

        val count = 10
        val myUserUuid = uuid()

        generateAccessInfo(system, count, audit )
        val mineSuccess = generateAccessInfo(system, count, audit, myUserUuid, true)
        generateAccessInfo(system, count, audit )
        val mineFail = generateAccessInfo(system, count, audit, myUserUuid, false)
        generateAccessInfo(system, count, audit )

        val loadedMineSuccess = audit.retrieveAccessInfo(userUuid = myUserUuid, permissionGranted = true)
        Assert.assertEquals("Loaded access info did not match expected", mineSuccess, loadedMineSuccess)

        val loadedMineFail = audit.retrieveAccessInfo(userUuid = myUserUuid, permissionGranted = false)
        Assert.assertEquals("Loaded access info did not match expected", mineFail, loadedMineFail)
    }

    @Test
    fun should_retrieve_access_info_by_permission_granted(){
        val audit = randomAuditDB()
        val system = randomSystemInfo()

        val count = 10
        val success = generateAccessInfo(system, count, audit, uuid(), true)
        val fail = generateAccessInfo(system, count, audit, uuid(),  false)

        val loadedSuccess = audit.retrieveAccessInfo(permissionGranted = true)
        Assert.assertEquals("Loaded access info did not match expected", success, loadedSuccess)

        val loadedFail = audit.retrieveAccessInfo(permissionGranted = false)
        Assert.assertEquals("Loaded access info did not match expected", fail, loadedFail)
    }

    @Test
    fun should_retrieve_access_info_by_dates_and_system_info_uuid() {
        val now = System.currentTimeMillis()
        val then = now - 10000
        val later = now + 10000
        val expectedCount = 10

        val audit = randomAuditDB()

        val systems = ArrayList<SystemInfo>()
        val map = HashMap<String, List<AccessInfo>>()

        for (i in 0 until expectedCount) {
            val system = randomSystemInfo()
            audit.storeSystemInfo(system)
            systems.add(system)

            val expectedEntries = generateAccessInfo(system, expectedCount, audit)
            map.put(system.uuid, expectedEntries)
        }

        for (system in systems) {
            val systemUuid = system.uuid
            val expectedEntries = map[systemUuid]
            val loadedEntries = audit.retrieveAccessInfo(systemUuid, null, then, later)

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
    fun should_not_retrieve_access_info_by_dates_before() {
        val now = System.currentTimeMillis()
        val muchEarlier = now - 15000
        val earlier = now - 10000
        val expectedCount = 10

        val audit = randomAuditDB()

        val system = randomSystemInfo()
        audit.storeSystemInfo(system)

        generateAccessInfo(system, expectedCount, audit)
        val loadedEntries = audit.retrieveAccessInfo( startTimestamp = muchEarlier, endTimestamp = earlier)

        Assert.assertEquals("Unexpected system info count", 0, loadedEntries.size.toLong())
    }

    @Test
    fun should_not_retrieve_access_info_by_dates_after() {
        val now = System.currentTimeMillis()
        val later = now + 10000
        val muchLater = now + 20000
        val expectedCount = 10

        val audit = randomAuditDB()

        val system = randomSystemInfo()
        audit.storeSystemInfo(system)

        generateAccessInfo(system, expectedCount, audit)
        val loadedEntries = audit.retrieveAccessInfo(startTimestamp = later, endTimestamp = muchLater)

        Assert.assertEquals("Unexpected system info count", 0, loadedEntries.size.toLong())
    }

    @Test
    fun should_not_retrieve_access_info_by_uuid() {
        val now = System.currentTimeMillis()
        val then = now - 10000
        val later = now + 10000
        val expectedCount = 10

        val audit = randomAuditDB()

        val system = randomSystemInfo()
        generateAccessInfo(system, expectedCount, audit)

        val loadedEntries = audit.retrieveAccessInfo( startTimestamp = then, endTimestamp = later)
        Assert.assertEquals("Unexpected system info count", expectedCount.toLong(), loadedEntries.size.toLong())

        val randomSystemUuid = uuid()
        val nonExistentSystemEntries = audit.retrieveAccessInfo(systemInfoUuid = randomSystemUuid, startTimestamp = then, endTimestamp = later)
        Assert.assertEquals("Unexpected system info count", 0, nonExistentSystemEntries.size.toLong())
    }

    ///
    /// Utility code
    ///

    private fun generateAccessInfo(system: SystemInfo,
                                   expectedCount: Int,
                                   audit: AuditDB,
                                   userUuid: String = "user" + uuid(),
                                   permissionGranted: Boolean? = null ): List<AccessInfo> {
        val result = ArrayList<AccessInfo>()

        for (i in 0 until expectedCount) {
            val info = randomAccessInfo(system.uuid, userUuid, permissionGranted)
            audit.log(info)
            result.add(info)

            Thread.sleep(1)
        }

        return result
    }

    private fun randomAccessInfo(systemInfoUuid: String, userUuid: String, permissionGranted: Boolean? ): AccessInfo {
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

        val permission = if(permissionGranted!=null) permissionGranted else random.nextBoolean()

        return AccessInfo(
                userUuid,
                loginType,
                "proxy:" + uuid(),
                "ipAddress:" + uuid(),
                System.currentTimeMillis(),
                "resourceUUID:" + uuid(),
                "resourceName:" + uuid(),
                "classification" + uuid(),
                "resource" + uuid(),
                accessType,
                permission,
                systemInfoUuid,
                mods,
                "uuid:" + uuid()
        )
    }
}