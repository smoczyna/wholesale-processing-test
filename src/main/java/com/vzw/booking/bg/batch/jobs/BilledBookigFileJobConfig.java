/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.jobs;

import com.vzw.booking.bg.batch.domain.AggregateWholesaleReportDTO;
import com.vzw.booking.bg.batch.domain.BilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import com.vzw.booking.bg.batch.listeners.BilledBookingFileJobListener;
import com.vzw.booking.bg.batch.listeners.BilledBookingFileStepExecutionListener;
import com.vzw.booking.bg.batch.processors.BookDateProcessor;
import com.vzw.booking.bg.batch.processors.SubLedgerProcessor;
import com.vzw.booking.bg.batch.processors.WholesaleReportProcessor;
import com.vzw.booking.bg.batch.readers.BilledBookingFileReader;
import com.vzw.booking.bg.batch.readers.BookDateCsvFileReader;
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
public class BilledBookigFileJobConfig {
    
    @Bean
    JobExecutionListener billedFileJobListener() {
        return new BilledBookingFileJobListener();
    }

    @Bean
    Tasklet sourceFilesExistanceChecker() {
        return new SourceFilesExistanceChecker();
    }

    @Bean
    StepExecutionListener billedFileStepListener() {
        return new BilledBookingFileStepExecutionListener();
    }

    @Bean
    SubLedgerProcessor tempSubLedgerOuput() {
        return new SubLedgerProcessor();
    }

    @Bean
    ItemReader<BilledCsvFileDTO> billedFileItemReader(Environment environment) {        
        return new BilledBookingFileReader(environment);
    }

    @Bean
    public SkipPolicy fileVerificationSkipper() {
        return new CsvFileVerificationSkipper();
    }

    @Bean
    ItemReader<BookDateCsvFileDTO> bookDateItemReader(Environment environment) {
        String[] fieldNames = new String[]{"rptPerStartDate", "rptPerEndDate", "transPerStartDate", "transPerEndDate", "monthEndCycle"};
        return new BookDateCsvFileReader(environment, "bookdate.txt", fieldNames);
    }

    @Bean
    ItemProcessor<BilledCsvFileDTO, AggregateWholesaleReportDTO> wholesaleBookingProcessor() {
        return new WholesaleReportProcessor();
    }

    @Bean
    ItemProcessor<BookDateCsvFileDTO, Set<SummarySubLedgerDTO>> bookDateProcessor() {
        return new BookDateProcessor();
    }

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
    Step billedBookingFileStep(StepExecutionListener billedFileStepListener,
                               ItemReader<BilledCsvFileDTO> billedFileItemReader,
                               SkipPolicy fileVerificationSkipper,
                               ItemProcessor<BilledCsvFileDTO, AggregateWholesaleReportDTO> wholesaleBookingProcessor,
                               ItemWriter<AggregateWholesaleReportDTO> wholesaleReportWriter,
                               StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("billedBookingFileStep")
                .<BilledCsvFileDTO, AggregateWholesaleReportDTO>chunk(1)
                .reader(billedFileItemReader)
                .faultTolerant()
                .skipPolicy(fileVerificationSkipper)
                .processor(wholesaleBookingProcessor)
                .writer(wholesaleReportWriter)
                .listener(billedFileStepListener)
                .build();
    }

    // other files steps here
    
    @Bean
    Step saveSubLedgerToFile(Tasklet writeAggregatedSubLedger,
                             StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("saveSubLedgerToFile")
                .tasklet(writeAggregatedSubLedger)
                .build();
    }

    @Bean
    Job billedBookingAggregateJob(JobExecutionListener billedFileJobListener,
                                  JobBuilderFactory jobBuilderFactory,
                                  @Qualifier("checkIfSourceFilesExist") Step checkIfSourceFilesExist,
                                  @Qualifier("updateBookingDatesStep") Step updateBookingDatesStep,
                                  @Qualifier("billedBookingFileStep") Step billedBookingFileStep,
                                  @Qualifier("saveSubLedgerToFile") Step saveSubLedgerToFile) {
        return jobBuilderFactory.get("billedBookingAggregateJob")
                .incrementer(new RunIdIncrementer())
                .listener(billedFileJobListener)
                .start(checkIfSourceFilesExist)
                .on("COMPLETED").to(updateBookingDatesStep)
                .on("COMPLETED").to(billedBookingFileStep)
                //other files steps
                .on("COMPLETED").to(saveSubLedgerToFile)
                .end()
                .build();
    }
}
