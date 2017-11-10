/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.squadd.batch.jobs;

import eu.squadd.batch.config.CassandraDataPuller;
import eu.squadd.batch.domain.casandra.DataEvent;
import eu.squadd.batch.domain.casandra.FinancialEventCategory;
import eu.squadd.batch.domain.casandra.FinancialMarket;
import eu.squadd.batch.utils.GenericSqlConverter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author smorcja
 */
public class PopulateCassandraCache implements Tasklet {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PopulateCassandraCache.class);

    @Autowired
    private CassandraDataPuller queryManager;
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        System.out.println("Financial Market records moved: "+this.moveFinMarkets());
        System.out.println("Financial Event Category records moved: "+this.moveFinEvents());
        System.out.println("Data Event records moved: "+this.moveDataEvents());
        return RepeatStatus.FINISHED;
    }
        
    private Integer moveFinMarkets() throws SQLException {
        List<FinancialMarket> finMarkets = this.queryManager.getFinMarkets();
        GenericSqlConverter converter = new GenericSqlConverter(FinancialMarket.class);
        Connection con = dataSource.getConnection();
        Statement stmt = con.createStatement();
        int counter=0;
        for (FinancialMarket market : finMarkets) {
            String query = converter.createQueryFromModel(market);
            stmt.executeUpdate(query);
            counter++;
        }
        return counter;
    }
    
    private Integer moveFinEvents() throws SQLException {
        List<FinancialEventCategory> finEvents = this.queryManager.getFinCats();
        GenericSqlConverter converter = new GenericSqlConverter(FinancialEventCategory.class);
        Connection con = dataSource.getConnection();
        Statement stmt = con.createStatement();
        int counter=0;
        for (FinancialEventCategory event : finEvents) {
            String query = converter.createQueryFromModel(event);
            stmt.executeUpdate(query);
            counter++;
        }
        return counter;
    }
    
    private Integer moveDataEvents() throws SQLException {
        List<DataEvent> finEvents = this.queryManager.getDataEvents();
        GenericSqlConverter converter = new GenericSqlConverter(DataEvent.class);
        Connection con = dataSource.getConnection();
        Statement stmt = con.createStatement();
        int counter=0;
        for (DataEvent event : finEvents) {
            String query = converter.createQueryFromModel(event);
            stmt.executeUpdate(query);
            counter++;
        }
        return counter;
    }
}

