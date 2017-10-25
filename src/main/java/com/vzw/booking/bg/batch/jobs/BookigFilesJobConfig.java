/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.jobs;

import com.vzw.booking.bg.batch.utils.RangePartitioner;
import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.domain.AdminFeeCsvFileDTO;
import com.vzw.booking.bg.batch.domain.AggregateWholesaleReportDTO;
import com.vzw.booking.bg.batch.domain.BilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.FinancialEventOffsetDTO;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
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
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
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
    RangePartitioner billedFilePartitioner(Environment environment) throws IOException {
        //String PROPERTY_CSV_SOURCE_FILE_PATH = "csv.to.database.job.source.file.path";
        //Resource[] resources = resourceResolver.loadResources(environment.getRequiredProperty(PROPERTY_CSV_SOURCE_FILE_PATH).concat("unbilled_split/*.csv"));
        
        return new RangePartitioner(resources);        
    }
    
    @Bean
    @StepScope
    RangePartitioner unbilledFilePartitioner(Environment environment) throws IOException {
        String PROPERTY_CSV_SOURCE_FILE_PATH = "csv.to.database.job.source.file.path";
        Resource[] resources = applicationContext.getResources(environment.getRequiredProperty(PROPERTY_CSV_SOURCE_FILE_PATH).concat("unbilled_split/*.csv"));
        return new RangePartitioner(resources);
    }

    @Bean
    @StepScope
    RangePartitioner adminFeesFilePartitioner(Environment environment) throws IOException {
        String PROPERTY_CSV_SOURCE_FILE_PATH = "csv.to.database.job.source.file.path";
        Resource[] resources = applicationContext.getResources(environment.getRequiredProperty(PROPERTY_CSV_SOURCE_FILE_PATH).concat("adminfees_split/*.csv"));
        return new RangePartitioner(resources);
    }
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(1000);
        taskExecutor.setCorePoolSize(1000);
        taskExecutor.setQueueCapacity(1000);
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }
    
    /* readers */
    
    @Bean
    ItemReader<BookDateCsvFileDTO> bookDateItemReader(Environment environment) {
        return new BookDateCsvFileReader(environment, Constants.BOOK_DATE_FILENAME);
    }

    @Bean
    ItemReader<FinancialEventOffsetDTO> financialEventOffsetReader(Environment environment) {
        return new FinancialEventOffsetReader(environment, Constants.FINANCIAL_EVENT_OFFSET_FILENAME);
    }

    @Bean
    @StepScope
    ItemReader<BilledCsvFileDTO> billedFileItemReader(ExecutionContext context) {
        return new BilledBookingFileReader(context.getString("fileName"));
    }
    
    @Bean
    @StepScope
    ItemReader<UnbilledCsvFileDTO> unbilledFileItemReader(ExecutionContext context) {
        return new UnbilledBookingFileReader(context.getString("fileName"));
    }
    
    @Bean
    @StepScope
    ItemReader<AdminFeeCsvFileDTO> adminFeesFileItemReader(ExecutionContext context) {
        return new AdminFeesBookingFileReader(context.getString("fileName"));
    }
    
    @Bean
    public SkipPolicy fileVerificationSkipper() {
        return new CsvFileVerificationSkipper();
    }

    /* processors */
    
    @Bean
    ItemProcessor<BookDateCsvFileDTO, Boolean> bookDateProcessor() {
        return new BookDateProcessor();
    }

    @Bean
    ItemProcessor<FinancialEventOffsetDTO, Boolean> financialEventOffsetProcessor() {
        return new FinancialEventOffsetProcessor();
    }

    @Bean
    ItemProcessor<BilledCsvFileDTO, WholesaleProcessingOutput> billedBookingProcessor() {
        return new WholesaleBookingProcessor();
    }

    @Bean
    ItemProcessor<UnbilledCsvFileDTO, WholesaleProcessingOutput> unbilledBookingProcessor() {
        return new WholesaleBookingProcessor();
    }

    @Bean
    ItemProcessor<AdminFeeCsvFileDTO, WholesaleProcessingOutput> adminFeesBookingProcessor() {
        return new WholesaleBookingProcessor();
    }

    /* writers */
    
    @Bean
    ItemWriter<AggregateWholesaleReportDTO> wholesaleReportWriter(Environment environment) {
        return new WholesaleReportCsvWriter(environment);
    }

    @Bean
    ItemWriter<SummarySubLedgerDTO> subledgerItemWriter(Environment environment) {
        return new SubledgerCsvFileWriter(environment);
    }

    @Bean
    ItemWriter<WholesaleProcessingOutput> wholesaleOutputWriter(Environment environment) {
        return new WholesaleOutputWriter();
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
    Step updateBookingDatesStep(ItemReader<BookDateCsvFileDTO> bookDateItemReader,
                                ItemProcessor<BookDateCsvFileDTO, Boolean> bookDateProcessor,
                                StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("updateBookingDatesStep")
                .<BookDateCsvFileDTO, Boolean>chunk(1)
                .reader(bookDateItemReader)
                .processor(bookDateProcessor)
                .build();
    }

    @Bean
    Step readOffsetDataStep(ItemReader<FinancialEventOffsetDTO> financialEventOffsetReader,
                            ItemProcessor<FinancialEventOffsetDTO, Boolean> financialEventOffsetProcessor,
                            StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("readOffsetDataStep")
                .<FinancialEventOffsetDTO, Boolean>chunk(1)
                .reader(financialEventOffsetReader)
                .processor(financialEventOffsetProcessor)
                .build();
    }

    @Bean
    Step billedFilePartitionStep(RangePartitioner billedFilePartitioner,
                                 Step billedBookingFileSlaveStep,
                                 TaskExecutor taskExecutor,
                                 StepBuilderFactory stepBuilderFactory) { // throws UnexpectedInputException, MalformedURLException, ParseException {
        return stepBuilderFactory.get("billedFilePartitionStep")
                .partitioner("billedSlaveStep", billedFilePartitioner)
                .step(billedBookingFileSlaveStep)
                .taskExecutor(taskExecutor)
                .build();
    }
    
    @Bean
    Step billedBookingFileSlaveStep(StepExecutionListener billedFileStepListener,
                                    ItemReader<BilledCsvFileDTO> billedFileItemReader,
                                    SkipPolicy fileVerificationSkipper,
                                    ItemProcessor<BilledCsvFileDTO, WholesaleProcessingOutput> billedBookingProcessor,
                                    ItemWriter<WholesaleProcessingOutput> wholesaleOutputWriter,
                                    StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("billedBookingFileSlaveStep")
                .<BilledCsvFileDTO, WholesaleProcessingOutput>chunk(1)
                .reader(billedFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(billedBookingProcessor)
                .writer(wholesaleOutputWriter)
                .listener(billedFileStepListener)
                .build();
    }

    @Bean
    Step unbilledFilePartitionStep(RangePartitioner unbilledFilePartitioner,
                                   Step unbilledBookingFileSlaveStep,
                                   TaskExecutor taskExecutor,
                                   StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("unbilledFilePartitionStep")
                .partitioner("unbilledSlaveStep", unbilledFilePartitioner)
                .step(unbilledBookingFileSlaveStep)
                .taskExecutor(taskExecutor)
                .build();
    }
    
    @Bean
    Step unbilledBookingFileSlaveStep(StepExecutionListener unbilledFileStepListener,
                                      ItemReader<UnbilledCsvFileDTO> unbilledFileItemReader,
                                      SkipPolicy fileVerificationSkipper,
                                      ItemProcessor<UnbilledCsvFileDTO, WholesaleProcessingOutput> unbilledBookingProcessor,
                                      ItemWriter<WholesaleProcessingOutput> wholesaleOutputWriter,
                                      StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("unbilledBookingFileSlaveStep")
                .<UnbilledCsvFileDTO, WholesaleProcessingOutput>chunk(1)
                .reader(unbilledFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(unbilledBookingProcessor)
                .writer(wholesaleOutputWriter)
                .listener(unbilledFileStepListener)
                .build();
    }

    @Bean
    Step adminFeesFilePartitionStep(RangePartitioner adminFeesFilePartitioner,
                                    Step adminFeesBookingFileSlaveStep,
                                    TaskExecutor taskExecutor,
                                    StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("adminFeesFilePartitionStep")
                .partitioner("adminFeesSlaveStep", adminFeesFilePartitioner)
                .step(adminFeesBookingFileSlaveStep)
                .taskExecutor(taskExecutor)
                .build();
    }
    
    @Bean
    Step adminFeesBookingFileSlaveStep(StepExecutionListener adminFeesFileStepListener,
                                       ItemReader<AdminFeeCsvFileDTO> adminFeesFileItemReader,
                                       SkipPolicy fileVerificationSkipper,
                                       ItemProcessor<AdminFeeCsvFileDTO, WholesaleProcessingOutput> adminFeesBookingProcessor,
                                       ItemWriter<WholesaleProcessingOutput> wholesaleOutputWriter,
                                       StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("adminFeesBookingFileSlaveStep")
                .<AdminFeeCsvFileDTO, WholesaleProcessingOutput>chunk(1)
                .reader(adminFeesFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(adminFeesBookingProcessor)
                .writer(wholesaleOutputWriter)
                .listener(adminFeesFileStepListener)
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
