package galvin.dw.users.sqlite

import galvin.dw.*
import galvin.dw.sqlite.SQLiteAuditDB
import galvin.dw.sqlite.SQLiteUserDB
import galvin.dw.tools.AuditManager
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.time.format.DateTimeFormatter

class AuditManagerQueryTest{
    @Test
    fun should_execute_query_by_user_uuid(){
        val setup = AuditManagerQueryTestObjects()

        val expected = mutableListOf<AccessInfo>()
        for( i in 1..10 ){
            val info = accessInfo(setup.user.uuid )
            setup.auditDB.log(info)
            expected.add(info)
            Thread.sleep(10)
        }

        val manager = AuditManager()
        val options = manager.parse( args( setup, "-u", setup.user.uuid ) )
        val results = manager.executeQuery(options)
        for( (index,result) in results.withIndex() ){
            val expectedInfo = expected[index]
            Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
        }
    }

    @Test
    fun should_execute_query_by_access_type(){
        val setup = AuditManagerQueryTestObjects()

        val expectedMap = mutableMapOf<AccessType, MutableList<AccessInfo>>()
        for( type in AccessType.values() ){
            val list = mutableListOf<AccessInfo>()
            expectedMap.put( type, list )

            for( i in 1..10 ){
                val info = accessInfo(accessType = type)
                list.add(info)

                setup.auditDB.log(info)
                Thread.sleep(10)
            }
        }

        val manager = AuditManager()
        for( type in AccessType.values() ) {
            val expected = expectedMap[type]
            if( expected == null ) throw Exception("No test data generated for type: ${type.name}")

            val options = manager.parse(args(setup, "-a", type.name))
            val results = manager.executeQuery(options)
            for ((index, result) in results.withIndex()) {
                val expectedInfo = expected[index]
                Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
            }
        }
    }

    @Test
    fun should_execute_query_by_success(){
        val setup = AuditManagerQueryTestObjects()

        val expectedAll = mutableListOf<AccessInfo>()

        val expectedSuccess = mutableListOf<AccessInfo>()
        for( i in 1..10 ){
            val info = accessInfo(permissionGranted = true)
            setup.auditDB.log(info)
            expectedSuccess.add(info)
            expectedAll.add(info)
            Thread.sleep(10)
        }

        val expectedFailure = mutableListOf<AccessInfo>()
        for( i in 1..10 ){
            val info = accessInfo(permissionGranted = false)
            setup.auditDB.log(info)
            expectedFailure.add(info)
            expectedAll.add(info)
            Thread.sleep(10)
        }

        val manager = AuditManager()

        val successOptions = manager.parse( args(setup, "-success" ) )
        val successResults = manager.executeQuery(successOptions)
        for( (index,result) in successResults.withIndex() ){
            val expectedInfo = expectedSuccess[index]
            Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
        }

        val failureOptions = manager.parse( args(setup, "-failure" ) )
        val failureResults = manager.executeQuery(failureOptions)
        for( (index,result) in failureResults.withIndex() ){
            val expectedInfo = expectedFailure[index]
            Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
        }

        val allOptions = manager.parse( args(setup, "-success", "-failure" ) )
        val allResults = manager.executeQuery(allOptions)
        for( (index,result) in allResults.withIndex() ){
            val expectedInfo = expectedAll[index]
            Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
        }
    }

    @Test
    fun should_execute_query_by_timestamp() {
        val setup = AuditManagerQueryTestObjects()
        val pattern = DateTimeFormat.forPattern("yyyy/MM/dd-kk:mm:ss")

        val allStart = pattern.print( DateTime( System.currentTimeMillis() ) )
        val allExpected = mutableListOf<AccessInfo>()

        val before = mutableListOf<AccessInfo>()
        for( i in 1..10 ){
            val info = accessInfo(permissionGranted = true)
            setup.auditDB.log(info)
            before.add(info)
            allExpected.add(info)
            Thread.sleep(10)
        }

        Thread.sleep(1_000)
        val timestamp = System.currentTimeMillis()
        val dt = DateTime( timestamp )
        val start = pattern.print(dt)

        val after = mutableListOf<AccessInfo>()
        for( i in 1..10 ){
            val info = accessInfo(permissionGranted = true)
            setup.auditDB.log(info)
            after.add(info)
            allExpected.add(info)
            Thread.sleep(10)
        }

        Thread.sleep(1_000)
        val allEnd = pattern.print( DateTime( System.currentTimeMillis() ) )

        val manager = AuditManager()

        val startOptions = manager.parse( args(setup, "-start", start ) )
        val startResults = manager.executeQuery(startOptions)
        for( (index,result) in startResults.withIndex() ){
            val expectedInfo = after[index]
            Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
        }

        val endOptions = manager.parse( args(setup, "-end", start ) )
        val endResults = manager.executeQuery(endOptions)
        for( (index,result) in endResults.withIndex() ){
            val expectedInfo = before[index]
            Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
        }

        val allOptions = manager.parse( args(setup, "-start", allStart, "-end", allEnd ) )
        val allResults = manager.executeQuery(allOptions)
        for( (index,result) in allResults.withIndex() ){
            val expectedInfo = allExpected[index]
            Assert.assertEquals("Unexpected access info", expectedInfo, result.accessInfo)
        }
    }

    private fun accessInfo( userUuid: String = uuid(),
                            accessType: AccessType = AccessType.RETRIEVE,
                            mods: List<Modification> = listOf(),
                            timestamp: Long = System.currentTimeMillis(),
                            permissionGranted: Boolean = true): AccessInfo{
        return AccessInfo(
                userUuid,
                LoginType.LOGIN_TOKEN,
                "proxy:" + uuid(),
                "ipAddress:" + uuid(),
                timestamp,
                "resourceUUID:" + uuid(),
                "resourceName:" + uuid(),
                "classification" + uuid(),
                "resource" + uuid(),
                accessType,
                permissionGranted,
                "system:" + uuid(),
                mods,
                "uuid:" + uuid()

        )
    }

    private fun args( setup: AuditManagerQueryTestObjects, vararg args: String ): Array<String>{
        val list = mutableListOf<String>()
        list.addAll( arrayOf("-sqlite", setup.auditFile.absolutePath, "-userdb", setup.userFile.absolutePath ) )
        list.addAll( args )
        return list.toTypedArray()
    }
}

data class AuditManagerQueryTestObjects(
        val auditFile: File = randomDbFile(),
        val auditDB: AuditDB = SQLiteAuditDB( auditFile ),
        val userFile: File = randomDbFile(),
        val userDB: UserDB = SQLiteUserDB( userFile ),
        val systemInfo: SystemInfo = randomSystemInfo(),
        val roles: List<Role> = generateRoles(5),
        val user: User = generateUser(roles)
){
    init{
        auditDB.storeSystemInfo(systemInfo)
        auditDB.storeCurrentSystemInfo(systemInfo.uuid)
        userDB.storeUser(user)
    }
}