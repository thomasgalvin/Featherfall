package galvin.dw.audit.sqlite;

import galvin.dw.*;
import galvin.dw.sqlite.SQLiteAuditDB;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static galvin.dw.UtilitiesKt.uuid;

public class SQLiteAuditDbTest{
    private static final boolean console = false;

    @Test
    public void should_not_create_tables_twice() throws Exception{
        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);
        final AuditDB audit2 = new SQLiteAuditDB(auditFile);
    }

    @Test
    public void should_retrieve_system_info_list() throws Exception{
        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);

        final int expectedCount = 10;
        Map<String, SystemInfo> map = new HashMap<>();

        for( int i = 0; i < expectedCount; i++ ){
            final SystemInfo system = randomSystemInfo();
            map.put( system.getUuid(), system );
            audit.store(system);
        }

        final List<SystemInfo> allSystemInfo = audit.retrieveAllSystemInfo();
        Assert.assertEquals( "Unexpected system info count", expectedCount, allSystemInfo.size() );

        for( final SystemInfo loaded: allSystemInfo ){
            final SystemInfo expected = map.get( loaded.getUuid() );
            Assert.assertEquals( "Loaded system info did not match expected", expected, loaded );
        }
    }

    @Test
    public void should_retrieve_system_info_by_uuid() throws Exception{
        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);

        final int expectedCount = 10;
        final Map<String, SystemInfo> map = new HashMap<>();

        for( int i = 0; i < expectedCount; i++ ){
            final SystemInfo system = randomSystemInfo();
            map.put( system.getUuid(), system );
            audit.store(system);
        }


        for( final String key: map.keySet() ){
            final SystemInfo expected = map.get(key);
            final SystemInfo loaded = audit.retrieveSystemInfo(key);
            Assert.assertEquals( "Loaded system info did not match expected", expected, loaded );
        }
    }

    @Test
    public void should_retrieve_current_system_info() throws Exception{
        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);
        final SystemInfo system = randomSystemInfo();
        audit.store(system);
        audit.storeCurrentSystemInfo( system.getUuid() );
        final SystemInfo loaded = audit.retrieveCurrentSystemInfo();
        Assert.assertEquals( "Loaded current system info did not match expected", system, loaded );
    }

    @Test
    public void should_retrieve_access_info_by_dates() throws Exception{
        final long now = System.currentTimeMillis();
        final long then = now - 10_000;
        final long later = now + 10_000;
        final int expectedCount = 10;

        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);

        final SystemInfo system = randomSystemInfo();
        audit.store(system);

        final List<AccessInfo> expectedEntries = generateAccessInfo(system, expectedCount, audit);
        final List<AccessInfo> loadedEntries = audit.retrieveAccessInfo(then, later);

        Assert.assertEquals( "Unexpected system info count", expectedCount, loadedEntries.size() );

        for( int i = 0; i < expectedEntries.size(); i++ ){
            final AccessInfo expected = expectedEntries.get(i);
            final AccessInfo loaded = loadedEntries.get(i);
            Assert.assertEquals( "Loaded access info did not match expected", expected, loaded );
        }
    }

    @Test
    public void should_retrieve_access_info_by_dates_and_uuid() throws Exception{
        final long now = System.currentTimeMillis();
        final long then = now - 10_000;
        final long later = now + 10_000;
        final int expectedCount = 10;

        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);

        final List<SystemInfo> systems = new ArrayList();
        final Map< String, List<AccessInfo> > map = new HashMap<>();

        for( int i = 0; i < expectedCount; i++ ){
            final SystemInfo system = randomSystemInfo();
            audit.store(system);
            systems.add(system);

            final List<AccessInfo> expectedEntries = generateAccessInfo(system, expectedCount, audit);
            map.put( system.getUuid(), expectedEntries );
        }

        for( SystemInfo system : systems ){
            final String systemUuid = system.getUuid();
            final List<AccessInfo> expectedEntries = map.get( systemUuid );
            final List<AccessInfo> loadedEntries = audit.retrieveAccessInfo( systemUuid, then, later );

            Assert.assertEquals( "Unexpected system info count", expectedEntries.size(), loadedEntries.size() );

            for( int i = 0; i < expectedEntries.size(); i++ ){
                final AccessInfo expected = expectedEntries.get(i);
                final AccessInfo loaded = loadedEntries.get(i);
                Assert.assertEquals( "Loaded access info did not match expected", expected, loaded );
            }
        }
    }

    @Test
    public void should_not_retrieve_access_info_by_dates_before() throws Exception{
        final long now = System.currentTimeMillis();
        final long muchEarlier = now - 15_000;
        final long earlier = now - 10_000;
        final int expectedCount = 10;

        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);

        final SystemInfo system = randomSystemInfo();
        audit.store(system);

        generateAccessInfo(system, expectedCount, audit);
        final List<AccessInfo> loadedEntries = audit.retrieveAccessInfo(muchEarlier, earlier);

        Assert.assertEquals( "Unexpected system info count", 0, loadedEntries.size() );
    }

    @Test
    public void should_not_retrieve_access_info_by_dates_after() throws Exception{
        final long now = System.currentTimeMillis();
        final long later = now + 10_000;
        final long muchLater = now + 20_000;
        final int expectedCount = 10;

        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);

        final SystemInfo system = randomSystemInfo();
        audit.store(system);

        generateAccessInfo(system, expectedCount, audit);
        final List<AccessInfo> loadedEntries = audit.retrieveAccessInfo(later, muchLater);

        Assert.assertEquals( "Unexpected system info count", 0, loadedEntries.size() );
    }

    @Test
    public void should_not_retrieve_access_info_by_uuid() throws Exception{
        final long now = System.currentTimeMillis();
        final long then = now - 10_000;
        final long later = now + 10_000;
        final int expectedCount = 10;

        final File auditFile = randomAuditDbFile();
        final AuditDB audit = new SQLiteAuditDB(auditFile);

        final SystemInfo system = randomSystemInfo();
        generateAccessInfo( system, expectedCount, audit );

        final List<AccessInfo> loadedEntries = audit.retrieveAccessInfo(then, later);
        Assert.assertEquals( "Unexpected system info count", expectedCount, loadedEntries.size() );

        final String randomSystemUuid = uuid();
        final List<AccessInfo> nonExistentSystemEntries = audit.retrieveAccessInfo( randomSystemUuid , then, later);
        Assert.assertEquals( "Unexpected system info count", 0, nonExistentSystemEntries.size() );
    }

    private static File randomAuditDbFile(){
        return new File( "target/audit-" + uuid() + ".dat" );
    }

    private static SystemInfo randomSystemInfo(){
        return new SystemInfo(
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
        );
    }

    private List<AccessInfo> generateAccessInfo( SystemInfo system, int expectedCount, AuditDB audit ) throws Exception{
        final List<AccessInfo> result = new ArrayList<>();

        for( int i = 0; i < expectedCount; i++ ) {
            final AccessInfo info = randomAccessInfo(system.getUuid());
            audit.log(info, console);
            result.add(info);

            Thread.sleep(1);
        }

        return result;
    }

    private static AccessInfo randomAccessInfo(String systemInfoUuid){
        final List<Modification> mods = new ArrayList();
        for( int i = 0; i < 10; i++ ){
            mods.add( new Modification(
                    "field:" + uuid(),
                    "old:" + uuid(),
                    "new:" + uuid()
            ) );
        }

        final Random random = new Random();
        final LoginType[] loginTypes = LoginType.values();
        final AccessType[] accessTypes = AccessType.values();

        final LoginType loginType = loginTypes[ random.nextInt( loginTypes.length ) ];
        final AccessType accessType = accessTypes[ random.nextInt( accessTypes.length ) ];
        final boolean permissionGranted = random.nextBoolean();

        return new AccessInfo(
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
        );
    }
}