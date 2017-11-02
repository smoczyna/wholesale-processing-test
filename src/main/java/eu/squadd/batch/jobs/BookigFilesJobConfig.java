/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.squadd.batch.jobs;

import eu.squadd.batch.constants.Constants;
import eu.squadd.batch.domain.AdminFeeCsvFileDTO;
import eu.squadd.batch.domain.BilledCsvFileDTO;
import eu.squadd.batch.domain.BookDateCsvFileDTO;
import eu.squadd.batch.domain.FinancialEventOffsetDTO;
import eu.squadd.batch.domain.UnbilledCsvFileDTO;
import eu.squadd.batch.domain.WholesaleProcessingOutput;
import eu.squadd.batch.listeners.BookingAggregateJobListener;
import eu.squadd.batch.listeners.GenericStepExecutionListener;
import eu.squadd.batch.listeners.WholesaleProcessingListener;
import eu.squadd.batch.processors.BookDateProcessor;
import eu.squadd.batch.processors.FinancialEventOffsetProcessor;
import eu.squadd.batch.processors.WholesaleBookingProcessor;
import eu.squadd.batch.readers.AdminFeesBookingFileReader;
import eu.squadd.batch.readers.BilledBookingFileReader;
import eu.squadd.batch.readers.BookDateCsvFileReader;
import eu.squadd.batch.readers.FinancialEventOffsetReader;
import eu.squadd.batch.readers.UnbilledBookingFileReader;
import eu.squadd.batch.utils.RangePartitioner;
import eu.squadd.batch.validations.CsvFileVerificationSkipper;
import eu.squadd.batch.writers.WholesaleOutputWriter;
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
    WholesaleProcessingListener processingListener() {
        return new WholesaleProcessingListener();
    }
    
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
    BilledBookingFileReader billedFileItemReader(@Value("#{stepExecutionContext[sourceFileName]}") String filename) {
        return new BilledBookingFileReader(filename);
    }
    
    @Bean
    @StepScope
    UnbilledBookingFileReader unbilledFileItemReader(@Value("#{stepExecutionContext[sourceFileName]}") String filename) {
        return new UnbilledBookingFileReader(filename);
    }
    
    @Bean
    @StepScope
    AdminFeesBookingFileReader adminFeesFileItemReader(@Value("#{stepExecutionContext[sourceFileName]}") String filename) {
        return new AdminFeesBookingFileReader(filename);
    }
    
    
//    @Bean
//    @StepScope
//    BilledBookingFileReader billedFileItemReader() {
//        ExecutionContext context = this.processingHelper.getStepExecutionContext();
//        BilledBookingFileReader reader = new BilledBookingFileReader(context.getString("fileName"));
//        reader.open(context);
//        return reader;
//    }
//    
//    @Bean
//    @StepScope
//    UnbilledBookingFileReader unbilledFileItemReader() {
//        ExecutionContext context = this.processingHelper.getStepExecutionContext();
//        UnbilledBookingFileReader reader = new UnbilledBookingFileReader(context.getString("fileName"));
//        reader.open(context);
//        return reader;
//    }
//    
//    @Bean
//    @StepScope
//    AdminFeesBookingFileReader adminFeesFileItemReader() {
//        ExecutionContext context = this.processingHelper.getStepExecutionContext();
//        AdminFeesBookingFileReader reader = new AdminFeesBookingFileReader(context.getString("fileName"));
//        reader.open(context);
//        return reader;
//    }
    
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
    WholesaleOutputWriter wholesaleOutputWriter(Environment environment, @Value("#{stepExecutionContext[destFileNo]}") String fileNo) {
        return new WholesaleOutputWriter(environment, fileNo);
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
    Step billedBookingFileSlaveStep(WholesaleProcessingListener processingListener,
                                    BilledBookingFileReader billedFileItemReader,
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
                .listener(processingListener)
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
    Step unbilledBookingFileSlaveStep(WholesaleProcessingListener processingListener,
                                      UnbilledBookingFileReader unbilledFileItemReader,
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
                .listener(processingListener)
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
    Step adminFeesBookingFileSlaveStep(WholesaleProcessingListener processingListener,
                                       AdminFeesBookingFileReader adminFeesFileItemReader,
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
                .listener(processingListener)
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
