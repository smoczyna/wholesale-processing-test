/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.jobs;

import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.domain.AdminFeeCsvFileDTO;
import com.vzw.booking.bg.batch.domain.AggregateWholesaleReportDTO;
import com.vzw.booking.bg.batch.domain.BilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.FinancialEventOffsetDTO;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import com.vzw.booking.bg.batch.domain.UnbilledCsvFileDTO;
import com.vzw.booking.bg.batch.listeners.BookingAggregateJobListener;
import com.vzw.booking.bg.batch.listeners.GenericStepExecutionListener;
import com.vzw.booking.bg.batch.processors.BookDateProcessor;
import com.vzw.booking.bg.batch.processors.FinancialEventOffsetProcessor;
import com.vzw.booking.bg.batch.processors.SubLedgerProcessor;
import com.vzw.booking.bg.batch.processors.WholesaleReportProcessor;
import com.vzw.booking.bg.batch.readers.AdminFeesBookingFileReader;
import com.vzw.booking.bg.batch.readers.BilledBookingFileReader;
import com.vzw.booking.bg.batch.readers.BookDateCsvFileReader;
import com.vzw.booking.bg.batch.readers.FinancialEventOffsetReader;
import com.vzw.booking.bg.batch.readers.UnbilledBookingFileReader;
import com.vzw.booking.bg.batch.validation.CsvFileVerificationSkipper;
import com.vzw.booking.bg.batch.writers.AggregatedSubLedgerWriter;
import com.vzw.booking.bg.batch.writers.SubledgerCsvFileWriter;
import com.vzw.booking.bg.batch.writers.WholesaleReportCsvWriter;
import java.util.Set;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 *
 * @author smorcja
 */
@Configuration
public class BookigFilesJobConfig {
    
    /* listeners and checkers */
    
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
    SubLedgerProcessor tempSubLedgerOuput() {
        return new SubLedgerProcessor();
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
    ItemReader<BilledCsvFileDTO> billedFileItemReader(Environment environment) {
        return new BilledBookingFileReader(environment, Constants.BILLED_BOOKING_FILENAME);
    }

    @Bean
    ItemReader<UnbilledCsvFileDTO> unbilledFileItemReader(Environment environment) {
        return new UnbilledBookingFileReader(environment, Constants.UNBILLED_BOOKING_FILENAME);
    }
    
    @Bean
    ItemReader<AdminFeeCsvFileDTO> adminFeesFileItemReader(Environment environment) {
        return new AdminFeesBookingFileReader(environment, Constants.ADMIN_FEES_FILENAME);
    }
    
    @Bean
    public SkipPolicy fileVerificationSkipper() {
        return new CsvFileVerificationSkipper();
    }

    
    /* processors */
    
    @Bean
    ItemProcessor<BookDateCsvFileDTO, Set<SummarySubLedgerDTO>> bookDateProcessor() {
        return new BookDateProcessor();
    }
    
    @Bean
    ItemProcessor<FinancialEventOffsetDTO, Boolean> financialEventOffsetProcessor() {
        return new FinancialEventOffsetProcessor();
    }

    @Bean
    ItemProcessor<BilledCsvFileDTO, AggregateWholesaleReportDTO> billedBookingProcessor() {
        return new WholesaleReportProcessor();
    }

    @Bean
    ItemProcessor<UnbilledCsvFileDTO, AggregateWholesaleReportDTO> unbilledBookingProcessor() {
        return new WholesaleReportProcessor();
    }
    
    @Bean
    ItemProcessor<AdminFeeCsvFileDTO, AggregateWholesaleReportDTO> adminFeesBookingProcessor() {
        return new WholesaleReportProcessor();
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
    Tasklet writeAggregatedSubLedger() {
        return new AggregatedSubLedgerWriter();
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
                                ItemProcessor<BookDateCsvFileDTO, Set<SummarySubLedgerDTO>> bookDateProcessor,
                                StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("updateBookingDatesStep")
                .<BookDateCsvFileDTO, Set<SummarySubLedgerDTO>>chunk(1)
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
    Step billedBookingFileStep(StepExecutionListener billedFileStepListener,
                               ItemReader<BilledCsvFileDTO> billedFileItemReader,
                               SkipPolicy fileVerificationSkipper,
                               ItemProcessor<BilledCsvFileDTO, AggregateWholesaleReportDTO> billedBookingProcessor,
                               ItemWriter<AggregateWholesaleReportDTO> wholesaleReportWriter,
                               StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("billedBookingFileStep")
                .<BilledCsvFileDTO, AggregateWholesaleReportDTO>chunk(1)
                .reader(billedFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(billedBookingProcessor)
                .writer(wholesaleReportWriter)
                .listener(billedFileStepListener)
                .build();
    }

    @Bean
    Step unbilledBookingFileStep(StepExecutionListener unbilledFileStepListener,
                                 ItemReader<UnbilledCsvFileDTO> unbilledFileItemReader,
                                 SkipPolicy fileVerificationSkipper,
                                 ItemProcessor<UnbilledCsvFileDTO, AggregateWholesaleReportDTO> unbilledBookingProcessor,
                                 ItemWriter<AggregateWholesaleReportDTO> wholesaleReportWriter,
                                 StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("unbilledBookingFileStep")
                .<UnbilledCsvFileDTO, AggregateWholesaleReportDTO>chunk(1)
                .reader(unbilledFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(unbilledBookingProcessor)
                .writer(wholesaleReportWriter)
                .listener(unbilledFileStepListener)
                .build();
    }
    
    @Bean
    Step adminFeesBookingFileStep(StepExecutionListener adminFeesFileStepListener,
                                  ItemReader<AdminFeeCsvFileDTO> adminFeesFileItemReader,
                                  SkipPolicy fileVerificationSkipper,
                                  ItemProcessor<AdminFeeCsvFileDTO, AggregateWholesaleReportDTO> adminFeesBookingProcessor,
                                  ItemWriter<AggregateWholesaleReportDTO> wholesaleReportWriter,
                                  StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("adminFeesBookingFileStep")
                .<AdminFeeCsvFileDTO, AggregateWholesaleReportDTO>chunk(1)
                .reader(adminFeesFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(adminFeesBookingProcessor)
                .writer(wholesaleReportWriter)
                .listener(adminFeesFileStepListener)
                .build();
    }
    
    @Bean
    Step saveSubLedgerToFile(Tasklet writeAggregatedSubLedger,
                             StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("saveSubLedgerToFile")
                .tasklet(writeAggregatedSubLedger)
                .build();
    }

    /* the job */
    
    @Bean
    Job bookingAggregateJob(JobExecutionListener bookingFileJobListener,
                            JobBuilderFactory jobBuilderFactory,
                            @Qualifier("checkIfSourceFilesExist") Step checkIfSourceFilesExist,
                            @Qualifier("updateBookingDatesStep") Step updateBookingDatesStep,
                            @Qualifier("readOffsetDataStep") Step readOffsetDataStep,
                            @Qualifier("billedBookingFileStep") Step billedBookingFileStep,
                            @Qualifier("unbilledBookingFileStep") Step unbilledBookingFileStep,
                            @Qualifier("adminFeesBookingFileStep") Step adminFeesBookingFileStep,
                            @Qualifier("saveSubLedgerToFile") Step saveSubLedgerToFile) {
        return jobBuilderFactory.get("bookingAggregateJob")
                .incrementer(new RunIdIncrementer())
                .listener(bookingFileJobListener)
                .start(checkIfSourceFilesExist)
                .on("COMPLETED").to(updateBookingDatesStep)
                .on("COMPLETED").to(readOffsetDataStep)
                .on("COMPLETED").to(billedBookingFileStep)
                .on("COMPLETED").to(unbilledBookingFileStep)
                .on("COMPLETED").to(adminFeesBookingFileStep)
                .on("COMPLETED").to(saveSubLedgerToFile)
                .end()
                .build();
    }
}
