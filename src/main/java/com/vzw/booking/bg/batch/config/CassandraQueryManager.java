package com.vzw.booking.bg.batch.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Configuration;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.exceptions.UnsupportedFeatureException;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.vzw.booking.bg.batch.domain.casandra.DataEvent;
import com.vzw.booking.bg.batch.domain.casandra.FinancialEventCategory;
import com.vzw.booking.bg.batch.domain.casandra.FinancialMarket;
import com.vzw.booking.bg.batch.domain.casandra.Product;
import com.vzw.booking.bg.batch.domain.casandra.WholesalePrice;
import com.vzw.booking.bg.batch.domain.casandra.mappers.DataEventCassandraMapper;
import com.vzw.booking.bg.batch.domain.casandra.mappers.FinancialEventCategoryCassandraMapper;
import com.vzw.booking.bg.batch.domain.casandra.mappers.FinancialMarketCassandraMapper;
import com.vzw.booking.bg.batch.domain.casandra.mappers.ProductCassandraMapper;
import com.vzw.booking.bg.batch.domain.casandra.mappers.WholesalePriceCassandraMapper;
import com.vzw.booking.bg.batch.domain.exceptions.CassandraQueryException;
import com.vzw.booking.bg.batch.domain.exceptions.ErrorEnum;
import com.vzw.booking.bg.batch.domain.exceptions.MultipleRowsReturnedException;
import com.vzw.booking.bg.batch.domain.exceptions.NoResultsReturnedException;

/**
 *
 * @author khanaas
 */
@Configuration
public class CassandraQueryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraQueryManager.class);

    private @Value("${com.springbatch.db.cassandra.contactpoints}") String contactpoints="";
	
	private @Value("${com.springbatch.db.cassandra.keyspace}") String keyspace="";
	
	private @Value("${com.springbatch.db.cassandra.username}") String username="";
	
	private @Value("${com.springbatch.db.cassandra.password}") String password="";
	
	private @Value("${com.springbatch.db.cassandra.dcname}") String dcname="";
    
//    @Autowired
//    private CassandraConfiguration cassandraConfiguration;

    
    //private final String fcccgsamapenddate = "12/31/9999";
    
	private @Value("${com.springbatch.db.query.constraints.financialmarketmapenddate}") String financialmarketmapenddate;

    private @Value("${com.springbatch.db.query.constraints.glmarketlegalentityenddate}") String glmarketlegalentityenddate;
    
    private @Value("${com.springbatch.db.query.constraints.glmarketmaptype}") String glmarketmaptype;
    
    private @Value("${com.springbatch.db.query.constraints.glmarketenddate}") String glmarketenddate;

    //private final String alternatebookingtype = "D";

    private static Session cassandraSession;    
    
    private final String finMarketQuery = "SELECT * FROM financialmarket"
            + " WHERE financialmarketid=? AND financialmarketmapenddate=? AND glmarketlegalentityenddate=? "
            + " AND glmarketmaptype=? AND glmarketenddate=? ALLOW FILTERING";
    
    private final String finEventCatQuery = "SELECT * FROM financialeventcategory "
            + "WHERE productid=? AND homesidequalsservingsidindicator=? AND alternatebookingindicator=? AND interexchangecarriercode=? ALLOW FILTERING";

    private final String finEventCatQueryBilled = "SELECT * FROM financialeventcategory "
            + "WHERE productid=? AND homesidequalsservingsidindicator=? AND financialeventnormalsign=? "
            + "AND alternatebookingindicator=? AND interexchangecarriercode=? ALLOW FILTERING";

    private final String dataEventQuery = "SELECT *  FROM dataevent WHERE productid=? ALLOW FILTERING";
    
    private final String wholesalePriceQuery = "SELECT * FROM WholesalePrice WHERE productid=? AND homesidbid=? AND servesidbid=?";
    
    private PreparedStatement finMarketStatement;
    private PreparedStatement productStatement;
    private PreparedStatement finEventCatStatement;
    private PreparedStatement finEventCatStatementBilled;
    private PreparedStatement dataEventStatement;
    private PreparedStatement wholesalePriceStatement;
    
    @PostConstruct
    public void init() {
		LOGGER.info("Cassandra Query Contraints ...");
		LOGGER.info("Constraint : Financial Market Map End Date : " + financialmarketmapenddate);
		LOGGER.info("Constraint : GL Market Legal Entity End Date : " + glmarketlegalentityenddate);
		LOGGER.info("Constraint : GL Market Map Type : " + glmarketmaptype);
		LOGGER.info("Constraint : GL Market End Date : " + glmarketenddate);
		LOGGER.info("Cassandra Connection parameters ...");
		LOGGER.info("Cassandra Contact Points: " + contactpoints);
		LOGGER.info("Cassandra User: " + username);
		LOGGER.info("Cassandra Password: " + password);
		LOGGER.info("Cassandra DataCenter: " + dcname);
		LOGGER.info("Cassandra Keyspace: " + keyspace);
		AuthProvider authProvider = new PlainTextAuthProvider(username, password);
		Cluster.Builder builder = Cluster.builder()
									.addContactPoints(contactpoints.split(","))
		        					.withAuthProvider(authProvider);
		if (dcname!=null && !dcname.trim().isEmpty())
		      builder = builder.withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(dcname).build());
		Cluster cluster = builder.build();
		cassandraSession = cluster.connect(keyspace);
		
		this.finMarketStatement = cassandraSession.prepare(finMarketQuery);
		this.finEventCatStatement = cassandraSession.prepare(finEventCatQuery);
		this.finEventCatStatementBilled = cassandraSession.prepare(finEventCatQueryBilled);
		this.dataEventStatement = cassandraSession.prepare(dataEventQuery);
		this.wholesalePriceStatement = cassandraSession.prepare(wholesalePriceQuery);
		
		LOGGER.info("After construction of Cassandra connection");
    }
    
    /**
     * Pre-ready session used across all queries
     *
     * @return
     */
    public Session getCassandraSession() {
        return cassandraSession;
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
            Result<FinancialMarket> result = new FinancialMarketCassandraMapper().executeAndMapResults(this.getCassandraSession(), statement, new MappingManager(this.cassandraSession), false);
            fms = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }
        if (fms.isEmpty()) {
            LOGGER.info("Error message:" + ErrorEnum.NO_ROWS+"Table: FinancialMarket, Input params[" 
                    + financialmarketid+","+financialmarketmapenddate+","+glmarketlegalentityenddate+","+glmarketmaptype+","+glmarketenddate+"]");
            throw new NoResultsReturnedException(ErrorEnum.NO_ROWS);
        }
        if (fms.size() >= 2) {
            LOGGER.info("Error message:" + ErrorEnum.MULTIPLE_ROWS+"Table: FinancialMarket, Input params[" 
                    + financialmarketid+","+financialmarketmapenddate+","+glmarketlegalentityenddate+","+glmarketmaptype+","+glmarketenddate+"]");
            LOGGER.info("Number of rows returned:" + Integer.toString(fms.size()));
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
            Result<Product> result = new ProductCassandraMapper().executeAndMapResults(this.getCassandraSession(), statement, new MappingManager(this.cassandraSession), false);
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
    @Cacheable("FinancialEventCategory")
    public List<FinancialEventCategory> getFinancialEventCategoryNoClusteringRecord(Integer TmpProdId, 
            String homesidequalsservingsidindicator, String alternatebookingindicator, int interExchangeCarrierCode, String financialeventnormalsign)
            throws MultipleRowsReturnedException, NoResultsReturnedException, CassandraQueryException {
        
        List<FinancialEventCategory> listoffec = new ArrayList<>();   
        BoundStatement statement;
        if (financialeventnormalsign==null) {
            statement = new BoundStatement(this.finEventCatStatement);
            statement.bind(TmpProdId, homesidequalsservingsidindicator, alternatebookingindicator, interExchangeCarrierCode);        
        } else {
            statement = new BoundStatement(this.finEventCatStatementBilled);
            statement.bind(TmpProdId, homesidequalsservingsidindicator, financialeventnormalsign, alternatebookingindicator, interExchangeCarrierCode);
        }
        try {
            Result<FinancialEventCategory> result = new FinancialEventCategoryCassandraMapper().executeAndMapResults(this.getCassandraSession(), statement, new MappingManager(this.getCassandraSession()), false);
            listoffec = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }        
        if (listoffec.isEmpty()) {
            LOGGER.info("Error message:" + ErrorEnum.NO_ROWS+"Table: FinancialEventCategory, Input params[" 
                    + TmpProdId+","+homesidequalsservingsidindicator+","+alternatebookingindicator+","+interExchangeCarrierCode+","+financialeventnormalsign+"]");
            throw new NoResultsReturnedException(ErrorEnum.NO_ROWS);
        }
        if (listoffec.size() > 1) {
            LOGGER.info("Error message:" + ErrorEnum.MULTIPLE_ROWS+"Table: FinancialEventCategory, Input params[" 
                    + TmpProdId+","+homesidequalsservingsidindicator+","+alternatebookingindicator+","+interExchangeCarrierCode+","+financialeventnormalsign+"]");
            LOGGER.info("Number of rows returned:" + Integer.toString(listoffec.size()));
            throw new MultipleRowsReturnedException(ErrorEnum.MULTIPLE_ROWS, " rows returned: " + Integer.toString(listoffec.size()));
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
            Result<DataEvent> result = new DataEventCassandraMapper().executeAndMapResults(this.getCassandraSession(), statement, new MappingManager(this.getCassandraSession()), false);
            listofde = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }
        if (listofde.isEmpty()) {
            LOGGER.info("Error message:" + ErrorEnum.NO_ROWS+"Table: DataEvent, Input params[" +productid+"]");
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
            Result<WholesalePrice> result = new WholesalePriceCassandraMapper().executeAndMapResults(this.getCassandraSession(), statement, new MappingManager(this.getCassandraSession()), false);
            listofwp = result.all();
        } catch (NoHostAvailableException | QueryExecutionException | QueryValidationException | UnsupportedFeatureException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Casandra Query Exception", e);       
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CassandraQueryException("Query Execution exception", e);
        }

        if (listofwp.isEmpty()) {
            LOGGER.info("Error message:" + ErrorEnum.NO_ROWS+"Table: WholesalePrice, Input params[" +productid+","+homesidbid+",00000]");
            throw new NoResultsReturnedException(ErrorEnum.NO_ROWS);
        }
        if (listofwp.size() >= 2) {
            LOGGER.info("Error message:" + ErrorEnum.MULTIPLE_ROWS+"Table: WholesalePrice, Input params["+productid+","+homesidbid+",00000]");
            LOGGER.info("Number of rows returned:" + Integer.toString(listofwp.size()));
            throw new MultipleRowsReturnedException(ErrorEnum.MULTIPLE_ROWS,
                    " rows returned: " + Integer.toString(listofwp.size()));
        }
        return listofwp;
    }
}
