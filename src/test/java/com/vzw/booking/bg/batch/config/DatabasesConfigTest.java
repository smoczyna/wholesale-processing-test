/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.config;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ColumnDefinitions.Definition;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.vzw.booking.bg.batch.domain.casandra.FinancialEventCategory;
import com.vzw.booking.bg.batch.domain.casandra.FinancialMarket;
import com.vzw.booking.bg.batch.domain.casandra.Misctran;
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author smorcja
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class DatabasesConfigTest {

    public static Session getCasandraSession(String keyspace) {
        AuthProvider authProvider = new PlainTextAuthProvider("j6_dev_user", "Ireland");
        Cluster cluster = Cluster.builder().addContactPoint("170.127.114.154").withAuthProvider(authProvider).build();
        return cluster.connect(keyspace);
    }

    @Test
    public void testCasandraConnectivity() throws Exception {
        System.out.println("*** Checkin Casandra native connectivity using Datastax driver ***");
        //Session session = DatabasesConfig.getCasandraSession();
        AuthProvider authProvider = new PlainTextAuthProvider("j6_dev_user", "Ireland");
        Cluster cluster = Cluster.builder().addContactPoint("170.127.114.154").withAuthProvider(authProvider).build();    
        assertNotNull(cluster);
        Metadata meta = cluster.getMetadata();
        List<KeyspaceMetadata> spaces = meta.getKeyspaces();
        assertNotNull(spaces);
        System.out.println("Keyspaces found: "+spaces.size());
        spaces.forEach((keyspace) -> {
            System.out.println("    "+keyspace.getName());
        });
        Session session = cluster.connect("j6_dev");
        assertNotNull(session);
        System.out.println("*** End of connectivity test ***");
    }
    
    @Test //(expected = NullPointerException.class)
    public void testCassandraFinancialMarketTable() throws Throwable {
        System.out.println("*** Checking Casandra Financial Market table ***");
        AbstractMapper<FinancialMarket> financialMarketMapper = new AbstractMapper() {
            @Override
            protected Mapper<FinancialMarket> getMapper(MappingManager manager) {
                return manager.mapper(FinancialMarket.class);
            }
        };

        CassandraQueryBuilder<FinancialMarket> builder = new CassandraQueryBuilder();
        String cql = "select * from financialmarket";
        builder = builder.withConDetails("170.127.114.154", "j6_dev", "j6_dev_user", "Ireland")
                .openNewSession(true)
                .withCql(cql)
                .withMapper(financialMarketMapper);
        builder.build();
        assertNotNull(builder);

        List<FinancialMarket> markets = builder.getResults();
        assertNotNull(markets);
        System.out.println("*** Query Output ***");
        for (FinancialMarket market : markets) {
            System.out.println(market.toString());
        }
        System.out.println("*** End of output ***");
    }

    /**
     * Test of getCasandraBasicDs method, of class DatabasesConfig.
     *
     * @throws java.lang.Exception
     */
//    @Test
//    public void testGetCasandraBasicDs() throws Exception {
//        System.out.println("Check Casandra DEV space connection");
//        String user = "j6_dev_user";
//        String password = "Ireland";
//        DataSource result = DatabasesConfig.getCasandraBasicDs(user, password);
//        assertNotNull(result);
//    }

    @Test
    public void testSystemSchemaAccess() throws Exception {
        System.out.println("*** Checking Casandra System Schema ***");
        Session session = getCasandraSession("system_schema");
        assertNotNull(session);
        
        ResultSet result = session.execute("select * from tables");
        assertTrue(result.all().size()>0);
        System.out.println("Tables found: "+result.all().size());
        for (Row row : result) {
            System.out.println(row.getString("table_name"));
        }
        System.out.println("*** End of System Schema check ***");
    }
    
//    @Test
//    public void testCassandraUserTablesAccess() throws Exception {
//        Session session = DatabasesConfig.getCasandraSession("j6_dev");
//        assertNotNull(session);
//        
//        ResultSet result1 = session.execute("select * from users_test");
//        int userCount = result1.all().size();
//        System.out.println("Number of users found: "+userCount);
//        assertTrue(userCount>0);
//        
////        session.execute("insert into users_test(userid, name) values(1, 'test user')");
////        ResultSet result2 = session.execute("select * from users_test");
////        int userCount2 = result2.all().size();
////        System.out.println("Users found in second call: "+userCount2);
////        assertTrue(userCount2==userCount+1);
////        
////        session.execute("delete from users_test where userid = 1");
////        ResultSet result3 = session.execute("select * from users_test");
////        int userCount3 = result3.all().size();
////        System.out.println("Users found in third call: "+userCount3);
////        assertEquals(userCount, userCount3);        
//    }
//  
//        MappingManager manager = new MappingManager(session);
//        Mapper<Misctran> mapper = manager.mapper(Misctran.class);
//        Misctran records = mapper.get(""); //get’s arguments must match the partition key components (number of arguments and their types).    
    
    @Test
    public void testFinancialMarketTable() throws Exception {
        System.out.println("*** Checking Casandra Misctran table ***");
        Session session = getCasandraSession("j6_dev");
        assertNotNull(session);
        System.out.println("Misctran table call without mapper, EXTRACTING structure:");
        
        ResultSet result1 = session.execute("select * from misctran");
        List<ColumnDefinitions.Definition> cols = result1.getColumnDefinitions().asList();
        assertNotNull(cols);
        assertEquals(4, cols.size());
        
        System.out.println("Looks like the TABLE_NAME is all I need to know about the table? So what is the issue actually ???");
        
        for (Definition def : cols) {
            System.out.println("Column: " + def.getName() + "   type: " + def.getType());
        }
        System.out.println("select * from misctran");
        System.out.println("All records in the table: "+result1.all().size());
        
        System.out.println("select * from misctran where companycode = 'C'");
        ResultSet result2 = session.execute("select * from misctran where companycode = 'C' ALLOW FILTERING");
        System.out.println("Filtered records retreived: "+result2.all().size());
        
        System.out.println("select * from misctran where miscfinancialtransactionnumber > 100");
        ResultSet result3 = session.execute("select * from misctran where miscfinancialtransactionnumber > 100 ALLOW FILTERING");
        System.out.println("Filtered records retreived: "+result3.all().size());
        
        System.out.println("let's print first 5 records then:");
        int i=0;
        Iterator<Row> it = result3.all().iterator();
        while(it.hasNext()) {
            Row row = it.next();
            System.out.println("printing row: " + row);
            i++;
            if (i>5) break;
        }
        System.out.println("It seeme that the only issue I have here is that I cannot retrieve rows from result set without a mapper !!!");
        System.out.println("Query never fails, regardless there is an output or not");
            
        System.out.println("*** End of Misctran check ***");
        
//        System.out.println("First column value: "+row.getInt("miscfinancialtransactionnumber"));
//        System.out.println("Second column value: "+row.getInt("miscfinancialtransactiondescription"));
//        System.out.println("Thrid column value: "+row.getInt("billtypecode"));
//        System.out.println("Fiurth column value: "+row.getInt("companycode"));
    }
    
    @Test
    public void testMisctanTable() throws Throwable  {
        System.out.println("*** Checking Casandra Misctran table with CassandraQueryBuilder ***");
        AbstractMapper<Misctran> misctranMapper = new AbstractMapper() {
            @Override
            protected Mapper<Misctran> getMapper(MappingManager manager) {
                return manager.mapper(Misctran.class);
            }
        };
        
        CassandraQueryBuilder<Misctran> builder = new CassandraQueryBuilder();
        String cql = "select * from misctran where miscfinancialtransactionnumber > 100 ALLOW FILTERING";
        builder = builder.withConDetails("170.127.114.154", "j6_dev", "j6_dev_user", "Ireland")
                .openNewSession(true)
                .withCql(cql)
                .withMapper(misctranMapper);
        builder.build();
        assertNotNull(builder);

        List<Misctran> records = builder.getResults();
        assertNotNull(records);
        System.out.println("*** Query Output ***");
        for (Misctran rec : records) {
            System.out.println(rec.toString());
        }
        System.out.println("*** End of output ***");
    }
    
    @Test
    public void testFinancialEventCategoryTable() throws Throwable  {
        System.out.println("*** Cheking Financial Event Category table ***");
        AbstractMapper<FinancialEventCategory> misctranMapper = new AbstractMapper() {
            @Override
            protected Mapper<FinancialEventCategory> getMapper(MappingManager manager) {
                return manager.mapper(FinancialEventCategory.class);
            }
        };
//        CassandraQueryBuilder<FinancialEventCategory> builder = new CassandraQueryBuilder();
//        String cql = "select * from financialeventcategory";
//        builder = builder.withConDetails("170.127.114.154", "j6_dev", "j6_dev_user", "Ireland")
//                .openNewSession(true)
//                .withCql(cql)
//                .withMapper(misctranMapper);
//        builder.build();
//        assertNotNull(builder);
//        
//        List<FinancialEventCategory> records = builder.getResults();
//        assertNotNull(records);
//        System.out.println("*** Query Output ***");
//        for (FinancialEventCategory rec : records) {
//            System.out.println(rec.toString());
//        }
//        System.out.println("*** End of output ***");
        
        System.out.println("*** Running conditianal query ***");
        CassandraQueryBuilder<FinancialEventCategory> builder = new CassandraQueryBuilder();
        String cql = "select * from financialeventcategory where billtypecode = 'something' ALLOW FILTERING";
        builder = builder.withConDetails("170.127.114.154", "j6_dev", "j6_dev_user", "Ireland")
                .openNewSession(true)
                .withCql(cql)
                .withMapper(misctranMapper);
        builder.build();
        assertNotNull(builder);
        
        List<FinancialEventCategory> records = builder.getResults();
        assertNotNull(records);
    }
}
