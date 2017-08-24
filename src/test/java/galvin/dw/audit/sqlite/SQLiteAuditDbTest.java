package galvin.dw.audit.sqlite;

import galvin.dw.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class SQLiteAuditDbTest{
    @Test
    public void should_create_tables() throws Exception{
        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);
    }

    @Test
    public void should_not_create_tables_twice() throws Exception{
        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);
        AuditDB audit2 = new SQLiteAuditDB(auditFile);
    }

    @Test
    public void should_store_system_info() throws Exception{
        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);

        SystemInfo system = randomSystemInfo();
        audit.store(system);
    }

    @Test
    public void should_retrieve_system_info() throws Exception{
        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);

        SystemInfo system = randomSystemInfo();
        audit.store(system);

        List<SystemInfo> allSystemInfo = audit.retrieveAllSystemInfo();
        Assert.assertEquals( "Unexpected system info count", 1, allSystemInfo.size() );
        Assert.assertEquals( "Loaded system info did not match expected", system, allSystemInfo.get(0) );
    }

    @Test
    public void should_retrieve_system_info_list() throws Exception{
        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);

        final int exepctedCount = 10;
        Map<String, SystemInfo> map = new HashMap<>();

        for( int i = 0; i < exepctedCount; i++ ){
            SystemInfo system = randomSystemInfo();
            map.put( system.getUuid(), system );
            audit.store(system);
        }

        List<SystemInfo> allSystemInfo = audit.retrieveAllSystemInfo();
        Assert.assertEquals( "Unexpected system info count", exepctedCount, allSystemInfo.size() );

        for( SystemInfo loaded: allSystemInfo ){
            SystemInfo expected = map.get( loaded.getUuid() );
            Assert.assertEquals( "Loaded system info did not match expected", expected, loaded );
        }
    }

    @Test
    public void should_retrieve_system_info_by_uuid() throws Exception{
        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);

        final int exepctedCount = 10;
        Map<String, SystemInfo> map = new HashMap<>();

        for( int i = 0; i < exepctedCount; i++ ){
            SystemInfo system = randomSystemInfo();
            map.put( system.getUuid(), system );
            audit.store(system);
        }


        for( String key: map.keySet() ){
            SystemInfo expected = map.get(key);
            SystemInfo loaded = audit.retrieveSystemInfo(key);
            Assert.assertEquals( "Loaded system info did not match expected", expected, loaded );
        }
    }

    @Test
    public void should_store_access_info() throws Exception{
        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);

        SystemInfo system = randomSystemInfo();
        AccessInfo info = randomAccessInfo( system.getUuid() );
        audit.log(info);
    }

    @Test
    public void should_store_access_info_by_dates() throws Exception{
        final long now = System.currentTimeMillis();
        final long then = now - 10_000;
        final long later = now + 10_000;
        final int expectedCount = 10;

        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);

        SystemInfo system = randomSystemInfo();
        audit.store(system);

        List<AccessInfo> expectedEntries = generateAccessInfo(system, expectedCount, audit);
        List<AccessInfo> loadedEntries = audit.retrieveAccessInfo(then, later);

        Assert.assertEquals( "Unexpected system info count", expectedCount, loadedEntries.size() );

        for( int i = 0; i < expectedEntries.size(); i++ ){
            AccessInfo expected = expectedEntries.get(i);
            AccessInfo loaded = loadedEntries.get(i);
            Assert.assertEquals( "Loaded access info did not match expected", expected, loaded );
        }
    }

    @Test
    public void should_store_access_info_by_dates_and_uuid() throws Exception{
        final long now = System.currentTimeMillis();
        final long then = now - 10_000;
        final long later = now + 10_000;
        final int expectedCount = 10;

        File auditFile = randomAuditDbFile();
        AuditDB audit = new SQLiteAuditDB(auditFile);

        List<SystemInfo> systems = new ArrayList();
        Map< String, List<AccessInfo> > map = new HashMap<>();

        for( int i = 0; i < expectedCount; i++ ){
            SystemInfo system = randomSystemInfo();
            audit.store(system);

            List<AccessInfo> expectedEntries = generateAccessInfo(system, expectedCount, audit);
            map.put( system.getUuid(), expectedEntries );
        }

        for( SystemInfo system : systems ){
            List<AccessInfo> expectedEntries = map.get( system.getUuid() );
            List<AccessInfo> loadedEntries = audit.retrieveAccessInfo( system.getUuid(), then, later );

            Assert.assertEquals( "Unexpected system info count", expectedEntries.size(), loadedEntries.size() );

            for( int i = 0; i < expectedEntries.size(); i++ ){
                AccessInfo expected = expectedEntries.get(i);
                AccessInfo loaded = loadedEntries.get(i);
                Assert.assertEquals( "Loaded access info did not match expected", expected, loaded );
            }
        }
    }

    private List<AccessInfo> generateAccessInfo( SystemInfo system, int expectedCount, AuditDB audit ) throws Exception{
        List<AccessInfo> result = new ArrayList<>();

        for( int i = 0; i < expectedCount; i++ ) {
            AccessInfo info = randomAccessInfo(system.getUuid());
            audit.log(info);
            result.add(info);

            Thread.sleep(1);
        }

        return result;
    }

    private static File randomAuditDbFile(){
        return new File( "target/audit-" + UUID.randomUUID().toString() + ".dat" );
    }

    private static SystemInfo randomSystemInfo(){
        return new SystemInfo(
                "serial:" + UUID.randomUUID().toString(),
                "name:" + UUID.randomUUID().toString(),
                "version" + UUID.randomUUID().toString(),
                "Unclassified-" + UUID.randomUUID().toString(),
                "guide:" + UUID.randomUUID().toString(),
                Arrays.asList(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString()
                ),
                "uuid:" + UUID.randomUUID().toString()
        );
    }

    private static AccessInfo randomAccessInfo(String systemInfoUuid){
        List<Modification> mods = new ArrayList();
        for( int i = 0; i < 10; i++ ){
            Modification mod = new Modification(
                    "field:" + UUID.randomUUID().toString(),
                    "old:" + UUID.randomUUID().toString(),
                    "new:" + UUID.randomUUID().toString()
            );
            mods.add(mod);
        }

        return new AccessInfo(
                "user" + UUID.randomUUID().toString(),
                LoginType.USERNAME_PASSWORD,
                null,
                System.currentTimeMillis(),
                "resourceUUID:" + UUID.randomUUID().toString(),
                "resourceName:" + UUID.randomUUID().toString(),
                "U",
                "TPS Report",
                AccessType.CREATE,
                true,
                systemInfoUuid,
                mods,
                "uuid:" + UUID.randomUUID().toString()
        );
    }
}