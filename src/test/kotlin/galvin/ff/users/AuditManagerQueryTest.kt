package galvin.ff.users

import galvin.ff.*
import galvin.ff.tools.AuditManager
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.Assert
import org.junit.Test

class AuditManagerQueryTest{
    @Test fun should_execute_query_by_user_uuid() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val setup = AuditManagerQueryTestObjects(database)

            val expected = mutableListOf<AccessInfo>()
            for (i in 1..10) {
                val info = accessInfo(setup.user.uuid, systemInfoUuid = setup.systemInfo.uuid)
                setup.auditDB.auditDB.log(info)
                expected.add(info)
                Thread.sleep(10)
            }

            val manager = AuditManager()
            val options = manager.parse( args(setup, "-u", setup.user.uuid) )
            val results = manager.executeQuery(options)
            for ((index, result) in results.withIndex()) {
                val expectedInfo = expected[index]
                Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
            }
        }
    }

    @Test fun should_print_query_results_by_user_uuid() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val setup = AuditManagerQueryTestObjects(database)

            val expected = mutableListOf<AccessInfo>()
            for (i in 1..10) {
                val info = accessInfo(setup.user.uuid, systemInfoUuid = setup.systemInfo.uuid)
                setup.auditDB.auditDB.log(info)
                expected.add(info)
                Thread.sleep(10)
            }

            val manager = AuditManager()
            val options = manager.parse(args(setup, "-u", setup.user.uuid))
            val results = manager.executeQuery(options)

            ConsoleGrabber.grabConsole()
            manager.print(results, options)
            ConsoleGrabber.releaseConsole(false)
        }
    }

    @Test fun should_print_query_results_with_mods_as_flag_by_user_uuid() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val setup = AuditManagerQueryTestObjects(database)

            val expected = mutableListOf<AccessInfo>()
            for (i in 1..10) {
                val mods = mutableListOf<Modification>()
                for (j in 1..2) {
                    mods.add(
                            Modification("field:" + uuid(), "old:" + uuid(), "new:" + uuid())
                    )
                }
                val info = accessInfo(userUuid = setup.user.uuid, systemInfoUuid = setup.systemInfo.uuid, mods = mods)
                setup.auditDB.auditDB.log(info)
                expected.add(info)
                Thread.sleep(10)
            }

            val manager = AuditManager()
            val options = manager.parse(args(setup, "-u", setup.user.uuid))
            val results = manager.executeQuery(options)

            ConsoleGrabber.grabConsole()
            manager.print(results, options)
            ConsoleGrabber.releaseConsole(false)
        }
    }

    @Test fun should_print_query_results_with_mods_as_deltas_by_user_uuid() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val setup = AuditManagerQueryTestObjects(database)

            val expected = mutableListOf<AccessInfo>()
            for (i in 1..10) {
                val mods = mutableListOf<Modification>()
                for (j in 1..2) {
                    mods.add(
                            Modification("field:" + uuid(), "old:" + uuid(), "new:" + uuid())
                    )
                }
                val info = accessInfo(userUuid = setup.user.uuid, systemInfoUuid = setup.systemInfo.uuid, mods = mods)
                setup.auditDB.auditDB.log(info)
                expected.add(info)
                Thread.sleep(10)
            }

            val manager = AuditManager()
            val options = manager.parse(args(setup, "-u", setup.user.uuid, "-d"))
            val results = manager.executeQuery(options)

            ConsoleGrabber.grabConsole()
            manager.print(results, options)
            ConsoleGrabber.releaseConsole(false)
        }
    }

    @Test fun should_execute_query_by_access_type() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val setup = AuditManagerQueryTestObjects(database)

            val expectedMap = mutableMapOf<AccessType, MutableList<AccessInfo>>()
            for (type in AccessType.values()) {
                val list = mutableListOf<AccessInfo>()
                expectedMap[type] = list

                for (i in 1..10) {
                    val info = accessInfo(accessType = type)
                    list.add(info)

                    setup.auditDB.auditDB.log(info)
                    Thread.sleep(10)
                }
            }

            val manager = AuditManager()
            for (type in AccessType.values()) {
                val expected = expectedMap[type] ?: throw Exception("No test data generated for type: ${type.name}")
                val options = manager.parse(args(setup, "-a", type.name))
                val results = manager.executeQuery(options)
                for ((index, result) in results.withIndex()) {
                    val expectedInfo = expected[index]
                    Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
                }
            }
        }
    }

    @Test fun should_execute_query_by_success() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val setup = AuditManagerQueryTestObjects(database)

            val expectedAll = mutableListOf<AccessInfo>()

            val expectedSuccess = mutableListOf<AccessInfo>()
            for (i in 1..10) {
                val info = accessInfo(permissionGranted = true)
                setup.auditDB.auditDB.log(info)
                expectedSuccess.add(info)
                expectedAll.add(info)
                Thread.sleep(10)
            }

            val expectedFailure = mutableListOf<AccessInfo>()
            for (i in 1..10) {
                val info = accessInfo(permissionGranted = false)
                setup.auditDB.auditDB.log(info)
                expectedFailure.add(info)
                expectedAll.add(info)
                Thread.sleep(10)
            }

            val manager = AuditManager()

            val successOptions = manager.parse(args(setup, "-success"))
            val successResults = manager.executeQuery(successOptions)
            for ((index, result) in successResults.withIndex()) {
                val expectedInfo = expectedSuccess[index]
                Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
            }

            val failureOptions = manager.parse(args(setup, "-failure"))
            val failureResults = manager.executeQuery(failureOptions)
            for ((index, result) in failureResults.withIndex()) {
                val expectedInfo = expectedFailure[index]
                Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
            }

            val allOptions = manager.parse(args(setup, "-success", "-failure"))
            val allResults = manager.executeQuery(allOptions)
            for ((index, result) in allResults.withIndex()) {
                val expectedInfo = expectedAll[index]
                Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
            }
        }
    }

    @Test fun should_execute_query_by_timestamp() {
        for (database in databases) {
            if (!database.canConnect()) continue

            val setup = AuditManagerQueryTestObjects(database)
            val pattern = DateTimeFormat.forPattern("yyyy/MM/dd-kk:mm:ss")

            val allStart = pattern.print(DateTime(System.currentTimeMillis()))
            val allExpected = mutableListOf<AccessInfo>()

            val before = mutableListOf<AccessInfo>()
            for (i in 1..10) {
                val info = accessInfo(permissionGranted = true)
                setup.auditDB.auditDB.log(info)
                before.add(info)
                allExpected.add(info)
                Thread.sleep(10)
            }

            Thread.sleep(1_000)
            val timestamp = System.currentTimeMillis()
            val dt = DateTime(timestamp)
            val start = pattern.print(dt)

            val after = mutableListOf<AccessInfo>()
            for (i in 1..10) {
                val info = accessInfo(permissionGranted = true)
                setup.auditDB.auditDB.log(info)
                after.add(info)
                allExpected.add(info)
                Thread.sleep(10)
            }

            Thread.sleep(1_000)
            val allEnd = pattern.print(DateTime(System.currentTimeMillis()))

            val manager = AuditManager()

            val startOptions = manager.parse(args(setup, "-start", start))
            val startResults = manager.executeQuery(startOptions)
            for ((index, result) in startResults.withIndex()) {
                val expectedInfo = after[index]
                Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
            }

            val endOptions = manager.parse(args(setup, "-end", start))
            val endResults = manager.executeQuery(endOptions)
            for ((index, result) in endResults.withIndex()) {
                val expectedInfo = before[index]
                Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
            }

            val allOptions = manager.parse(args(setup, "-start", allStart, "-end", allEnd))
            val allResults = manager.executeQuery(allOptions)
            for ((index, result) in allResults.withIndex()) {
                val expectedInfo = allExpected[index]
                Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
            }
        }
    }

    private fun accessInfo( userUuid: String = uuid(),
                            accessType: AccessType = AccessType.RETRIEVE,
                            mods: List<Modification> = listOf(),
                            timestamp: Long = System.currentTimeMillis(),
                            permissionGranted: Boolean = true,
                            systemInfoUuid: String = "system:" + uuid() ): AccessInfo{
        return AccessInfo(
                userUuid,
                LoginType.LOGIN_TOKEN,
                "proxy:" + uuid(),
                "ipAddress:" + uuid(),
                timestamp,
                "resourceUUID:" + uuid(),
                "resourceName:" + uuid(),
                "classification:" + uuid(),
                "type:" + uuid(),
                accessType,
                permissionGranted,
                systemInfoUuid,
                mods,
                "uuid:" + uuid()

        )
    }

    private fun args(setup: AuditManagerQueryTestObjects, vararg args: String ): Array<String>{
        val list = mutableListOf<String>()

        if( setup.auditDB.connectionURL.endsWith(".dat") ) {
            list.addAll( arrayOf("-sqlite", setup.auditDB.connectionURL, "-userdb", setup.userDB.connectionURL) )
            list.addAll( arrayOf( "-userdbsqlite", setup.userDB.connectionURL ) )
        }
        else{
            list.addAll( arrayOf("-psql", setup.auditDB.connectionURL, "-userdb", setup.userDB.connectionURL) )
            list.addAll( arrayOf( "-userdbpsql", setup.userDB.connectionURL ) )
        }

        if( setup.auditDB.userName != null ){
            list.addAll( arrayOf( "-dbuser", setup.auditDB.userName ) )
            list.addAll( arrayOf( "-useruser", setup.auditDB.userName ) )
        }

        if( setup.auditDB.password != null ){
            list.addAll( arrayOf( "-dbpass", setup.auditDB.password ) )
            list.addAll( arrayOf( "-userpass", setup.auditDB.password ) )
        }




        list.addAll( args )
        return list.toTypedArray()
    }
}

data class AuditManagerQueryTestObjects(
        val database: DbConnection,
        val maxConnections: Int = 1,
        val auditDB: AuditDbConnection = database.auditDbConnection(),
        val userDB: UserDbConnection = database.userDbConnection(),
        val systemInfo: SystemInfo = randomSystemInfo(),
        private val roles: List<Role> = generateRoles(5),
        val user: User = generateUser(roles)
){
    init{
        auditDB.auditDB.storeSystemInfo(systemInfo)
        auditDB.auditDB.storeCurrentSystemInfo(systemInfo.uuid)
        userDB.userDB.storeUser(user)
    }
}