package galvin.dw.audit.sqlite;

import galvin.dw.AuditDB;
import galvin.dw.SQLiteAuditDB;
import galvin.dw.SystemInfo;
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

    private static File randomAuditDbFile(){
        return new File( "target/audit-" + UUID.randomUUID().toString() + ".dat" );
    }

    private static SystemInfo randomSystemInfo(){
        return new SystemInfo(
                "serial:" + UUID.randomUUID().toString(),
                "test system info",
                "v0.1.0",
                "Unclassified",
                "N/A",
                Arrays.asList( "Public Internet", "Private VPN", "Home Wireless"),
                "uuid:" + UUID.randomUUID().toString()
        );
    }
}