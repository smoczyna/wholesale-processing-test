package com.vzw.booking.bg.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <h1>WholesaleBookingProcessorApplication</h1>
 * <p>
 * Entry point of the application. It is a standard Spring Boot application
 * class.
 * </p>
 */
@SpringBootApplication(exclude = BatchAutoConfiguration.class)
@EnableBatchProcessing
@EnableCaching
public class BookingWholesaleApplication {

    /**
     * This the main method for the application. It executes the application
     *
     * @param args - arguments passed to the spring boot application.
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BookingWholesaleApplication.class, args);
        // could be nice to have few modes of running (for instance: continuous lister and single run)
        context.registerShutdownHook();
        SpringApplication.exit(context);
        System.exit(0);
    }

}
