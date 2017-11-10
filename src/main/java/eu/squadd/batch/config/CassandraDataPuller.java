/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.squadd.batch.config;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.exceptions.UnsupportedFeatureException;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import eu.squadd.batch.domain.casandra.DataEvent;
import eu.squadd.batch.domain.casandra.FinancialEventCategory;
import eu.squadd.batch.domain.casandra.FinancialMarket;
import eu.squadd.batch.domain.casandra.mappers.DataEventCassandraMapper;
import eu.squadd.batch.domain.casandra.mappers.FinancialEventCategoryCassandraMapper;
import eu.squadd.batch.domain.casandra.mappers.FinancialMarketCassandraMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author smorcja
 */
@Configuration
public class CassandraDataPuller {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraDataPuller.class);
    private static Session cassandraSession;    
    private static final String CASSANDRA_KEYSPACE_DEV = "j6_dev";
    private static final String CASSANDRA_KEYSPACE_PROD = "j6_prod";
    
    private PreparedStatement finMarketStatement;
    private PreparedStatement productStatement;
    private PreparedStatement finEventCatStatement;
    private PreparedStatement dataEventStatement;
    private PreparedStatement wholesalePriceStatement;
    
    @PostConstruct
    public void init() {
        AuthProvider authProvider = new PlainTextAuthProvider("j6_dev_user", "Ireland");
        Cluster cluster = Cluster.builder().addContactPoint("170.127.114.154").withAuthProvider(authProvider).build();

//        AuthProvider authProvider = new PlainTextAuthProvider("j6_prod_user", "Ireland");
//        Cluster cluster = Cluster.builder().addContactPoints("170.127.59.152", "170.127.59.153", "170.127.59.154")
//                .withAuthProvider(authProvider).withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc("IDC1").build()).build();
//        
        cassandraSession = cluster.connect(CASSANDRA_KEYSPACE_DEV);
        
        this.finMarketStatement = cassandraSession.prepare("SELECT * FROM financialmarket");
        this.finEventCatStatement = cassandraSession.prepare("SELECT * FROM financialeventcategory");
        this.dataEventStatement = cassandraSession.prepare("SELECT * FROM dataevent");
        this.wholesalePriceStatement = cassandraSession.prepare("SELECT * FROM WholesalePrice");
        
        LOGGER.info("After construction of Cassandra connection");
    }
    
    public Session getCassandraSession() {
        return cassandraSession;
    }

    public List<FinancialMarket> getFinMarkets() {
        List<FinancialMarket> fms = new ArrayList();
        Statement statement = new BoundStatement(this.finMarketStatement);
        try {
            Result<FinancialMarket> result = new FinancialMarketCassandraMapper().executeAndMapResults(this.cassandraSession, statement, new MappingManager(this.cassandraSession), false);
            fms = result.all();
        } catch (NullPointerException | NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException | InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(CassandraDataPuller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fms;
    }
    
    public List<FinancialEventCategory> getFinCats() {
        List<FinancialEventCategory> fec = new ArrayList();
        Statement statement = new BoundStatement(this.finEventCatStatement);
        try {
            Result<FinancialEventCategory> result = new FinancialEventCategoryCassandraMapper().executeAndMapResults(this.cassandraSession, statement, new MappingManager(this.cassandraSession), false);
            fec = result.all();
        } catch (NullPointerException | NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException | InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(CassandraDataPuller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fec;
    }
    
    public List<DataEvent> getDataEvents() {
        List<DataEvent> de = new ArrayList<>();
        Statement statement = new BoundStatement(this.dataEventStatement);
        try {
            Result<DataEvent> result = new DataEventCassandraMapper().executeAndMapResults(this.cassandraSession, statement, new MappingManager(this.cassandraSession), false);
            de = result.all();
        } catch (NullPointerException | NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException | InterruptedException | ExecutionException ex) {
            java.util.logging.Logger.getLogger(CassandraDataPuller.class.getName()).log(Level.SEVERE, null, ex);
        }
        return de;
    }
}
