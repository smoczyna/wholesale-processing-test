package eu.squadd.batch;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import eu.squadd.batch.utils.WholesaleBookingProcessorHelper;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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
public class BookingProcessorApplicationInit {

    /**
     * This is meta data source
     * this source is used by Spring Batch internally to control processing
     * connection and credentials details are configured in application.properties
     * @return - ready to use data source 
     */
    @Bean(name = "metaDataSource")
    @Primary
    public static DataSource dataSource() {
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        EmbeddedDatabase db = builder
                .setType(EmbeddedDatabaseType.H2) // .HSQL or .DERBY
                .addScript("classpath:db/meta/schema-h2.sql")
                .build();
        return db;
    }
    
    @Bean
    public WholesaleBookingProcessorHelper wholesaleBookingProcessorHelper() {
        return new WholesaleBookingProcessorHelper();
    }
    
//    @Bean(name = "metaDataSource")
//    @Primary
//    @ConfigurationProperties(prefix = "spring.datasource")
//    public DataSource metaDataSource() {
//        return DataSourceBuilder.create().build();
//    }
    
    @Bean(name = "jobScheduler")
    public TaskScheduler jobScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("poolScheduler");
        scheduler.setPoolSize(10);
        return scheduler;
    }
}
