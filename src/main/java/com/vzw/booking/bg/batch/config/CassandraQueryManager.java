package com.vzw.booking.bg.batch.config;

import com.datastax.driver.core.AuthProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.exceptions.UnsupportedFeatureException;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.vzw.booking.bg.batch.domain.casandra.DataEvent;
import com.vzw.booking.bg.batch.domain.casandra.FinancialEventCategory;
import com.vzw.booking.bg.batch.domain.casandra.FinancialMarket;
import com.vzw.booking.bg.batch.domain.casandra.Product;
import com.vzw.booking.bg.batch.domain.casandra.WholesalePrice;
import com.vzw.booking.bg.batch.domain.exceptions.CassandraQueryException;
import com.vzw.booking.bg.batch.domain.exceptions.ErrorEnum;
import com.vzw.booking.bg.batch.domain.exceptions.MultipleRowsReturnedException;
import com.vzw.booking.bg.batch.domain.exceptions.NoResultsReturnedException;
import com.vzw.booking.bg.batch.domain.casandra.mappers.DataEventCassandraMapper;
import com.vzw.booking.bg.batch.domain.casandra.mappers.FinancialEventCategoryCassandraMapper;
import com.vzw.booking.bg.batch.domain.casandra.mappers.FinancialMarketCassandraMapper;
import com.vzw.booking.bg.batch.domain.casandra.mappers.ProductCassandraMapper;
import com.vzw.booking.bg.batch.domain.casandra.mappers.WholesalePriceCassandraMapper;
import javax.annotation.PostConstruct;
import org.springframework.cache.annotation.Cacheable;

/**
 *
 * @author khanaas
 */
@Configuration
//@PropertySource("classpath:cassandra.properties")
public class CassandraQueryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraQueryManager.class);
    
//    com.vzw.services.cassandra.FccCgsaMapEndDate=12/31/9999
//    com.vzw.services.cassandra.FinancialMarketMapEndDate=12/31/9999
//    com.vzw.services.cassandra.GLMarketLegalEntityEndDate=12/31/9999
//    com.vzw.services.cassandra.GLMarketMapType=D
//    com.vzw.services.cassandra.GLMarketEndDate=12/31/9999
//    com.vzw.services.cassandra.AlternateBookingType=D
//    com.vzw.services.cassandra.FinancialEventCategoryTable=financialeventcategory
//    com.vzw.services.cassandra.FinancialMarketTable=financialmarket
//    com.vzw.services.cassandra.ProductTable=product
//    com.vzw.services.cassandra.DataEventTable=dataevent
//    com.vzw.services.cassandra.WholesalePriceTable=wholesaleprice
    
    //@Value("${com.vzw.services.cassandra.FccCgsaMapEndDate}")
    private String fcccgsamapenddate = "12/31/9999";

    //@Value("${com.vzw.services.cassandra.FinancialMarketMapEndDate}")
    private String financialmarketmapenddate = "12/31/9999";

    //@Value("${com.vzw.services.cassandra.GLMarketLegalEntityEndDate}")
    private String glmarketlegalentityenddate = "12/31/9999";

    //@Value("${com.vzw.services.cassandra.GLMarketMapType}")
    private String glmarketmaptype = "D";

    //@Value("${com.vzw.services.cassandra.GLMarketEndDate}")
    private String glmarketenddate = "12/31/9999";

    //@Value("${com.vzw.services.cassandra.AlternateBookingType}")
    private String alternatebookingtype = "D";

//    @Value("${com.vzw.services.cassandra.FinancialEventCategoryTable}")
//    private String financialEventCategoryTable;
//
//    @Value("${com.vzw.services.cassandra.FinancialMarketTable}")
//    private String financialMarketTable;
//
//    @Value("${com.vzw.services.cassandra.ProductTable}")
//    private String productTable;
//
//    @Value("${com.vzw.services.cassandra.DataEventTable}")
//    private String dataEventTable;
//
//    @Value("${com.vzw.services.cassandra.WholesalePriceTable}")
//    private String WholesalePriceTable;

    static Logger logger = LoggerFactory.getLogger(CassandraQueryManager.class);

    private Session cassandraSession;
    
    private static final String CASSANDRA_KEYSPACE = "j6_dev";

//    private final String finMarketQuery = "SELECT" + " *" + " FROM financialmarket"
//                + " WHERE financialmarketid=? AND fcccgsamapenddate=? "
//                + " AND financialmarketmapenddate=? AND glmarketlegalentityenddate=? AND glmarketmaptype=? "
//                + " AND glmarketenddate=? ALLOW FILTERING";
    
    private static final String finMarketQuery = "SELECT * FROM financialmarket"
            + " WHERE financialmarketid=? AND financialmarketmapenddate=? AND glmarketlegalentityenddate=? "
            + " AND glmarketmaptype=? AND glmarketenddate=? ALLOW FILTERING";
    
    private final String productQuery = "SELECT * FROM product WHERE productid=?" + " ALLOW FILTERING";
    
    private final String finEventCatQuery = "SELECT * FROM financialeventcategory "
            + "WHERE productid=? AND homesidequalsservingsidindicator=? AND alternatebookingindicator=? ALLOW FILTERING";

    private final String dataEventQuery = "SELECT *  FROM dataevent WHERE productid=? ALLOW FILTERING";
    
    private final String wholesalePriceQuery = "SELECT * FROM WholesalePrice WHERE productid=? AND homesidbid=? AND servesidbid=?";
    
    private PreparedStatement finMarketStatement;
    private PreparedStatement productStatement;
    private PreparedStatement finEventCatStatement;
    private PreparedStatement dataEventStatement;
    private PreparedStatement wholesalePriceStatement;
    
    @PostConstruct
    public void init() {
        AuthProvider authProvider = new PlainTextAuthProvider("j6_dev_user", "Ireland");
        Cluster cluster = Cluster.builder().addContactPoint("170.127.114.154").withAuthProvider(authProvider).build();
        this.cassandraSession = cluster.connect(CASSANDRA_KEYSPACE);
        
        this.finMarketStatement = this.cassandraSession.prepare(finMarketQuery);
        this.productStatement = this.cassandraSession.prepare(productQuery);
        this.finEventCatStatement = this.cassandraSession.prepare(finEventCatQuery);
        this.dataEventStatement = this.cassandraSession.prepare(dataEventQuery);
        this.wholesalePriceStatement = this.cassandraSession.prepare(wholesalePriceQuery);
    }
    
    /**
     * I had to add this to make calls
     *
     * @return
     */
    public Session getCassandraSession() {
        return this.cassandraSession;
    }

    /**
     * Returns matching rows, output List<FinancialMarket> list. If there are
     * multiple rows returned, send an error code to the program.
     * <p>
     * Cassandra Table Name=FinancialMarket
     *
     * @param session
     * @param file2financialmarketid
     * @return List<FinancialMarket>
     * @throws CassandraQueryException
     * @throws NoResultsReturnedException
     * @throws MultipleRowsReturnedException
     */
    @Cacheable("FinancialMarket")
    public List<FinancialMarket> getFinancialMarketRecord( String financialmarketid) throws CassandraQueryException, NoResultsReturnedException, MultipleRowsReturnedException {
        List<FinancialMarket> fms = new ArrayList<>();
        BoundStatement statement = new BoundStatement(this.finMarketStatement);
        statement.bind(financialmarketid, financialmarketmapenddate, glmarketlegalentityenddate, glmarketmaptype, glmarketenddate);
        statement.enableTracing();        
        try {
            Result<FinancialMarket> result = new FinancialMarketCassandraMapper().executeAndMapResults(this.cassandraSession, statement, new MappingManager(this.cassandraSession), false);
            fms = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }

        if (fms.isEmpty()) {
            logger.info("Error message:" + ErrorEnum.NO_ROWS);
            throw new NoResultsReturnedException(ErrorEnum.NO_ROWS);
        }
        if (fms.size() >= 2) {
            logger.info("Error message:" + ErrorEnum.MULTIPLE_ROWS);
            logger.info("Rows returned:" + fms.toString());
            throw new MultipleRowsReturnedException(ErrorEnum.MULTIPLE_ROWS,
                    " rows returned: " + Integer.toString(fms.size()));
        }
        return fms;
    }

    /**
     * Check if the product is Wholesale product or not. Retrieve matching row,
     * output character 'Y' or 'N'
     * <p>
     * Cassandra Table Name=Product
     *
     * @param session
     * @param TmpProdId
     * @return char
     * @throws CassandraQueryException
     * @throws NoResultsReturnedException
     * @throws MultipleRowsReturnedException
     */
    @Cacheable("WholesaleProduct")
    public char isWholesaleProduct(Integer TmpProdId) throws CassandraQueryException {
        char isWholesaleProduct;        
        List<Product> listoffep = new ArrayList<>();        
        BoundStatement statement = new BoundStatement(this.productStatement);
        statement.bind(TmpProdId);
        try {
            Result<Product> result = new ProductCassandraMapper().executeAndMapResults(this.cassandraSession, statement, new MappingManager(this.cassandraSession), false);
            listoffep = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }

        if (listoffep.size() == 1) {
            isWholesaleProduct = listoffep.get(0).getWholesalebillingcode().charAt(0);
        } else {
            isWholesaleProduct = 'N';
        }
        return isWholesaleProduct;
    }

    /**
     * Returns list of FinancialEventCategory records
     * <p>
     * Cassandra Table Name=FinancialEventCategory
     *
     * @param session
     * @param TmpProdId
     * @param File2FinancialMarketId
     * @param InterExchangeCarrierCode
     * @param homesidequalsservingsidindicator
     * @param alternatebookingindicator
     * @return List<FinancialEventCategory>
     * @throws CassandraQueryException
     * @throws NoResultsReturnedException
     * @throws MultipleRowsReturnedException
     */    
//    public List<FinancialEventCategory> getFinancialEventCategoryRecord(Session session, Integer TmpProdId,
//            String File2FinancialMarketId, Integer InterExchangeCarrierCode, String homesidequalsservingsidindicator,
//            String alternatebookingindicator)
//            throws MultipleRowsReturnedException, NoResultsReturnedException, CassandraQueryException {
//
//        List<FinancialEventCategory> listoffec = new ArrayList<>();
//        String cql_selectTest = "SELECT " + "*" + " FROM financialeventcategory"
//                + " WHERE productid=? AND homesidequalsservingsidindicator=? AND alternatebookingindicator=? "
//                + "  AND (financialmarketid, interexchangecarriercode) = (?, ?)  ALLOW FILTERING";
//
//        PreparedStatement preparedStatement = session.prepare(cql_selectTest);
//        BoundStatement statement = new BoundStatement(preparedStatement);
//        statement.bind(TmpProdId, homesidequalsservingsidindicator, alternatebookingindicator, File2FinancialMarketId,
//                InterExchangeCarrierCode);
//        try {
//            Result<FinancialEventCategory> result = new FinancialEventCategoryCassandraMapper()
//                    .executeAndMapResults(session, statement, new MappingManager(session), false);
//            listoffec = result.all();
//        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
//            LOGGER.error(e.getLocalizedMessage());
//            throw new CassandraQueryException("Casandra Query Exception", e);       
//        } catch (NullPointerException | InterruptedException | ExecutionException e) {
//            LOGGER.error(e.getLocalizedMessage());
//            throw new CassandraQueryException("Query Execution exception", e);
//        }
//
//        if (listoffec.isEmpty()) {
//            logger.info("Error message:" + ErrorEnum.NO_ROWS);
//            throw new NoResultsReturnedException(ErrorEnum.NO_ROWS);
//        }
//        if (listoffec.size() >= 2) {
//            logger.info("Error message:" + ErrorEnum.MULTIPLE_ROWS);
//            logger.info("Rows returned:" + listoffec.toString());
//            throw new MultipleRowsReturnedException(ErrorEnum.MULTIPLE_ROWS,
//                    " rows returned: " + Integer.toString(listoffec.size()));
//        }
//        return listoffec;
//    }

    /**
     * Returns list of FinancialEventCategory records
     * <p>
     * Cassandra Table Name=FinancialEventCategory
     *
     * @param session
     * @param TmpProdId
     * @param File2FinancialMarketId
     * @param InterExchangeCarrierCode
     * @param homesidequalsservingsidindicator
     * @param alternatebookingindicator
     * @return List<FinancialEventCategory>
     * @throws CassandraQueryException
     * @throws NoResultsReturnedException
     * @throws MultipleRowsReturnedException
     */
    @Cacheable("FinancialEventCategory")
    public List<FinancialEventCategory> getFinancialEventCategoryNoClusteringRecord(Integer TmpProdId, 
            String homesidequalsservingsidindicator, String alternatebookingindicator)
            throws MultipleRowsReturnedException, NoResultsReturnedException, CassandraQueryException {
        
        List<FinancialEventCategory> listoffec = new ArrayList<>();        
        BoundStatement statement = new BoundStatement(this.finEventCatStatement);
        statement.bind(TmpProdId, homesidequalsservingsidindicator, alternatebookingindicator);
        try {
            Result<FinancialEventCategory> result = new FinancialEventCategoryCassandraMapper().executeAndMapResults(this.cassandraSession, statement, new MappingManager(this.cassandraSession), false);
            listoffec = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }
        
        if (listoffec.isEmpty()) {
            logger.info("Error message:" + ErrorEnum.NO_ROWS);
            throw new NoResultsReturnedException(ErrorEnum.NO_ROWS);
        }
        if (listoffec.size() > 1) {
            logger.info("Error message:" + ErrorEnum.MULTIPLE_ROWS);
            logger.info("Rows returned:" + listoffec.toString());
            throw new MultipleRowsReturnedException(ErrorEnum.MULTIPLE_ROWS,
                    " rows returned: " + Integer.toString(listoffec.size()));
        }
        return listoffec;
    }

    /**
     * Returns first record of the List<DataEvent>
     * <p>
     * Cassandra Table Name=DataEvent
     *
     * @param session
     * @param productid
     * @return DataEvent
     * @throws CassandraQueryException
     * @throws NoResultsReturnedException
     * @throws MultipleRowsReturnedException
     */
    @Cacheable("DataEvent")
    public List<DataEvent> getDataEventRecords(Integer productid) throws MultipleRowsReturnedException, CassandraQueryException, NoResultsReturnedException {

        List<DataEvent> listofde = new ArrayList<>();
        BoundStatement statement = new BoundStatement(this.dataEventStatement);
        statement.bind(productid);
        try {
            Result<DataEvent> result = new DataEventCassandraMapper().executeAndMapResults(this.cassandraSession, statement,
                    new MappingManager(this.cassandraSession), false);
            listofde = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }

        if (listofde.isEmpty()) {
            throw new NoResultsReturnedException(ErrorEnum.NO_ROWS);
        }
        return listofde;
    }

    /**
     * Returns first record of the WholesalePrice object/one record
     * <p>
     * Cassandra Table Name=WholesalePrice
     *
     * @param session
     * @param productid
     * @param homesidbid
     * @return WholesalePrice
     * @throws CassandraQueryException
     * @throws NoResultsReturnedException
     * @throws MultipleRowsReturnedException
     */
    @Cacheable("WholesalePrice")
    public List<WholesalePrice> getWholesalePriceRecords(Integer productid, String homesidbid) throws MultipleRowsReturnedException, CassandraQueryException, NoResultsReturnedException {

        List<WholesalePrice> listofwp = new ArrayList<>();
        BoundStatement statement = new BoundStatement(this.wholesalePriceStatement);
        statement.bind(productid, homesidbid, "00000");
        try {
            Result<WholesalePrice> result = new WholesalePriceCassandraMapper().executeAndMapResults(this.cassandraSession, statement, new MappingManager(this.cassandraSession), false);
            listofwp = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }

        if (listofwp.isEmpty()) {
            logger.info("Error message:" + ErrorEnum.NO_ROWS);
            throw new NoResultsReturnedException(ErrorEnum.NO_ROWS);
        }
        if (listofwp.size() >= 2) {
            logger.info("Error message:" + ErrorEnum.MULTIPLE_ROWS);
            logger.info("Rows returned:" + listofwp.toString());
            throw new MultipleRowsReturnedException(ErrorEnum.MULTIPLE_ROWS,
                    " rows returned: " + Integer.toString(listofwp.size()));
        }
        return listofwp;
    }
}
