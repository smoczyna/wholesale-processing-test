package com.vzw.booking.bg.batch;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.vzw.booking.bg.batch.util.CustomIdGenerator;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;

/**
 * <h1>WholesaleBookingProcessorApplicationInit</h1>
 * This is initialization class for the application.
 * <p>
 * 1. Inject the application config classes. 
 * 2. Define beans to initialize
 * 3. Can create the logger based on the application specific configurations
 * </p>
 *
 */
@Component
public class BookingWholesaleApplicationInit {

//    @Autowired
//    private LoggerConfig loggerConfig;

    /**
     * Stateful bean providing generated IDs
     */
    public CustomIdGenerator idGenerator = new CustomIdGenerator();
    
    /**
     * Define a Bean to initialize the logger.
     *
     * @return Verizon Logger
     */
//    @Bean
//    public VlfLogger vlfLogger() {
//        VlfLogger logger = new VlfLogger();
//        logger.setAppName(this.loggerConfig.getAppname());
//        logger.setServiceName(this.loggerConfig.getServicename());
//        logger.setRegion(this.loggerConfig.getRegion());
//        logger.setZone(this.loggerConfig.getZone());
//        VlfLogger.setLogLevel(LogLevel.INFO);
//        return logger;
//    }

    /**
     * This is meta data source
     * this source is used by Spring Batch internally to control processing
     * connection and credentials details are configured in application.properties
     * @return - ready to use data source 
     */
    @Bean(name = "metaDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource metaDataSource() {
        return DataSourceBuilder.create().build();
    }

}
