/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.jobs;

import com.vzw.booking.bg.batch.utils.RangePartitioner;
import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.domain.AdminFeeCsvFileDTO;
import com.vzw.booking.bg.batch.domain.BilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.FinancialEventOffsetDTO;
import com.vzw.booking.bg.batch.domain.UnbilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.WholesaleProcessingOutput;
import com.vzw.booking.bg.batch.listeners.BookingAggregateJobListener;
import com.vzw.booking.bg.batch.listeners.GenericStepExecutionListener;
import com.vzw.booking.bg.batch.processors.BookDateProcessor;
import com.vzw.booking.bg.batch.processors.FinancialEventOffsetProcessor;
import com.vzw.booking.bg.batch.processors.WholesaleBookingProcessor;
import com.vzw.booking.bg.batch.readers.AdminFeesBookingFileReader;
import com.vzw.booking.bg.batch.readers.BilledBookingFileReader;
import com.vzw.booking.bg.batch.readers.BookDateCsvFileReader;
import com.vzw.booking.bg.batch.readers.FinancialEventOffsetReader;
import com.vzw.booking.bg.batch.readers.UnbilledBookingFileReader;
import com.vzw.booking.bg.batch.validation.CsvFileVerificationSkipper;
import com.vzw.booking.bg.batch.writers.SubledgerCsvFileWriter;
import com.vzw.booking.bg.batch.writers.WholesaleOutputWriter;
import com.vzw.booking.bg.batch.writers.WholesaleReportCsvWriter;
import java.io.IOException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 *
 * @author smorcja
 */
@Configuration
public class BookigFilesJobConfig {
    
    /* listeners and helpers */

    @Bean
    JobExecutionListener bookingFileJobListener() {
        return new BookingAggregateJobListener();
    }

    @Bean
    Tasklet sourceFilesExistanceChecker() {
        return new SourceFilesExistanceChecker();
    }

    @Bean
    StepExecutionListener bookingFileStepListener() {
        return new GenericStepExecutionListener();
    }

    @Bean
    @StepScope
    RangePartitioner billedFilePartitioner(Environment environment) {           
        return new RangePartitioner(environment, "billed_split/");
    }
    
    @Bean
    @StepScope
    RangePartitioner unbilledFilePartitioner(Environment environment) throws IOException {
        return new RangePartitioner(environment, "unbilled_split/");
    }

    @Bean
    @StepScope
    RangePartitioner adminFeesFilePartitioner(Environment environment) throws IOException {
        return new RangePartitioner(environment, "adminfees_split/");
    }
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(30);
        taskExecutor.setCorePoolSize(30);
        taskExecutor.setQueueCapacity(5000);
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }
    
    /* readers */
    
    @Bean
    BookDateCsvFileReader bookDateItemReader(Environment environment) {
        return new BookDateCsvFileReader(environment, Constants.BOOK_DATE_FILENAME);
    }

    @Bean
    FinancialEventOffsetReader financialEventOffsetReader(Environment environment) {
        return new FinancialEventOffsetReader(environment, Constants.FINANCIAL_EVENT_OFFSET_FILENAME);
    }

    @Bean
    @StepScope
    BilledBookingFileReader billedFileItemReader(@Value("#{stepExecutionContext[fileName]}") String filename) {
        return new BilledBookingFileReader(filename);
    }
    
    @Bean
    @StepScope
    UnbilledBookingFileReader unbilledFileItemReader(@Value("#{stepExecutionContext[fileName]}") String filename) {
        return new UnbilledBookingFileReader(filename);
    }
    
    @Bean
    @StepScope
    AdminFeesBookingFileReader adminFeesFileItemReader(@Value("#{stepExecutionContext[fileName]}") String filename) {
        return new AdminFeesBookingFileReader(filename);
    }
    
    @Bean
    public SkipPolicy fileVerificationSkipper() {
        return new CsvFileVerificationSkipper();
    }

    /* processors */
    
    @Bean
    BookDateProcessor bookDateProcessor() {
        return new BookDateProcessor();
    }

    @Bean
    FinancialEventOffsetProcessor financialEventOffsetProcessor() {
        return new FinancialEventOffsetProcessor();
    }

    @Bean
    @StepScope
    WholesaleBookingProcessor billedBookingProcessor() {
        return new WholesaleBookingProcessor();
    }

    @Bean
    @StepScope
    WholesaleBookingProcessor unbilledBookingProcessor() {
        return new WholesaleBookingProcessor();
    }

    @Bean
    @StepScope
    WholesaleBookingProcessor adminFeesBookingProcessor() {
        return new WholesaleBookingProcessor();
    }

    /* writers */

    @Bean
    @StepScope
    WholesaleOutputWriter wholesaleOutputWriter(Environment environment) {
        return new WholesaleOutputWriter(environment);
    }

    /* job steps */
    
    @Bean
    Step checkIfSourceFilesExist(Tasklet sourceFilesExistanceChecker,
                                 StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("checkIfSourceFilesExist")
                .tasklet(sourceFilesExistanceChecker)
                .build();
    }

    @Bean
    Step updateBookingDatesStep(BookDateCsvFileReader bookDateItemReader,
                                BookDateProcessor bookDateProcessor,
                                StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("updateBookingDatesStep")
                .<BookDateCsvFileDTO, Boolean>chunk(1)
                .reader(bookDateItemReader)
                .processor(bookDateProcessor)
                .build();
    }

    @Bean
    Step readOffsetDataStep(FinancialEventOffsetReader financialEventOffsetReader,
                            FinancialEventOffsetProcessor financialEventOffsetProcessor,
                            StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("readOffsetDataStep")
                .<FinancialEventOffsetDTO, Boolean>chunk(1)
                .reader(financialEventOffsetReader)
                .processor(financialEventOffsetProcessor)
                .build();
    }

    @Bean
    Step billedFilePartitionStep(StepExecutionListener billedFileStepListener,
                                 RangePartitioner billedFilePartitioner,
                                 Step billedBookingFileSlaveStep,
                                 TaskExecutor taskExecutor,
                                 StepBuilderFactory stepBuilderFactory) { // throws UnexpectedInputException, MalformedURLException, ParseException {
        return stepBuilderFactory.get("billedFilePartitionStep")
                .partitioner("billedSlaveStep", billedFilePartitioner)
                .step(billedBookingFileSlaveStep)
                .taskExecutor(taskExecutor)
                .listener(billedFileStepListener)
                .build();
    }
    
    @Bean
    Step billedBookingFileSlaveStep(BilledBookingFileReader billedFileItemReader,
                                    SkipPolicy fileVerificationSkipper,
                                    WholesaleBookingProcessor billedBookingProcessor,
                                    WholesaleOutputWriter wholesaleOutputWriter,
                                    StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("billedBookingFileSlaveStep")
                .<BilledCsvFileDTO, WholesaleProcessingOutput>chunk(1)
                .reader(billedFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(billedBookingProcessor)
                .writer(wholesaleOutputWriter)                
                .build();
    }

    @Bean
    Step unbilledFilePartitionStep(StepExecutionListener unbilledFileStepListener,
                                   RangePartitioner unbilledFilePartitioner,
                                   Step unbilledBookingFileSlaveStep,
                                   TaskExecutor taskExecutor,
                                   StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("unbilledFilePartitionStep")
                .partitioner("unbilledSlaveStep", unbilledFilePartitioner)
                .step(unbilledBookingFileSlaveStep)
                .taskExecutor(taskExecutor)
                .listener(unbilledFileStepListener)
                .build();
    }
    
    @Bean
    Step unbilledBookingFileSlaveStep(UnbilledBookingFileReader unbilledFileItemReader,
                                      SkipPolicy fileVerificationSkipper,
                                      WholesaleBookingProcessor unbilledBookingProcessor,
                                      WholesaleOutputWriter wholesaleOutputWriter,
                                      StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("unbilledBookingFileSlaveStep")
                .<UnbilledCsvFileDTO, WholesaleProcessingOutput>chunk(1)
                .reader(unbilledFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(unbilledBookingProcessor)
                .writer(wholesaleOutputWriter)
                .build();
    }

    @Bean
    Step adminFeesFilePartitionStep(StepExecutionListener adminFeesFileStepListener,
                                    RangePartitioner adminFeesFilePartitioner,
                                    Step adminFeesBookingFileSlaveStep,
                                    TaskExecutor taskExecutor,
                                    StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("adminFeesFilePartitionStep")
                .partitioner("adminFeesSlaveStep", adminFeesFilePartitioner)
                .step(adminFeesBookingFileSlaveStep)
                .taskExecutor(taskExecutor)
                .listener(adminFeesFileStepListener)
                .build();
    }
    
    @Bean
    Step adminFeesBookingFileSlaveStep(AdminFeesBookingFileReader adminFeesFileItemReader,
                                       SkipPolicy fileVerificationSkipper,
                                       WholesaleBookingProcessor adminFeesBookingProcessor,
                                       WholesaleOutputWriter wholesaleOutputWriter,
                                       StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("adminFeesBookingFileSlaveStep")
                .<AdminFeeCsvFileDTO, WholesaleProcessingOutput>chunk(1)
                .reader(adminFeesFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(adminFeesBookingProcessor)
                .writer(wholesaleOutputWriter)
                .build();
    }

    /* the job */
    
    @Bean
    Job bookingAggregateJob(JobExecutionListener bookingFileJobListener,
                            JobBuilderFactory jobBuilderFactory,
                            @Qualifier("checkIfSourceFilesExist") Step checkIfSourceFilesExist,
                            @Qualifier("updateBookingDatesStep") Step updateBookingDatesStep,
                            @Qualifier("readOffsetDataStep") Step readOffsetDataStep,
                            @Qualifier("billedFilePartitionStep") Step billedFilePartitionStep,
                            @Qualifier("unbilledFilePartitionStep") Step unbilledFilePartitionStep,
                            @Qualifier("adminFeesFilePartitionStep") Step adminFeesFilePartitionStep) {
        return jobBuilderFactory.get("bookingAggregateJob")
                .incrementer(new RunIdIncrementer())
                .listener(bookingFileJobListener)
                .start(checkIfSourceFilesExist)
                .on("COMPLETED").to(updateBookingDatesStep)
                .on("COMPLETED").to(readOffsetDataStep)
                .on("COMPLETED").to(billedFilePartitionStep)
                .on("COMPLETED").to(unbilledFilePartitionStep)
                .on("COMPLETED").to(adminFeesFilePartitionStep)
                .end()
                .build();
    }
}
