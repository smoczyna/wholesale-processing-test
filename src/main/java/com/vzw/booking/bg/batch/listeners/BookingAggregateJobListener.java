/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.listeners;

import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.utils.ProcessingUtils;
import com.vzw.booking.bg.batch.utils.WholesaleBookingProcessorHelper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author smorcja
 */
public class BookingAggregateJobListener implements JobExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingAggregateJobListener.class);
    private Date startTIme;

    @Autowired
    private WholesaleBookingProcessorHelper helper;

    @Value("${csv.to.database.job.source.file.path}")
    private String INPUT_CSV_SOURCE_FILE_PATH;

    @Override
    public void beforeJob(JobExecution je) {
        this.startTIme = new Date();
        LOGGER.info(String.format(Constants.JOB_STARTED_MESSAGE, ProcessingUtils.dateToString(this.startTIme, ProcessingUtils.SHORT_DATETIME_FORMAT)));
        this.helper.setMaxSkippedRecords(je.getJobParameters().getLong("maxSkippedRecords"));
        this.helper.setNumberOfChunks(je.getJobParameters().getLong("numberOfChunks").intValue());
    }

    /**
     * moves all source files to archive folder to avoid duplicate processing
     *
     * @param je
     */
    @Override
    public void afterJob(JobExecution je) {
        if (je.getStatus() == BatchStatus.COMPLETED) {
            try {
                this.moveFileToArchive(Constants.BOOK_DATE_FILENAME);
                this.moveFileToArchive(Constants.FINANCIAL_EVENT_OFFSET_FILENAME);
                this.moveFileToArchive(Constants.BILLED_BOOKING_FILENAME);
                this.moveFileToArchive(Constants.UNBILLED_BOOKING_FILENAME);
                this.moveFileToArchive(Constants.ADMIN_FEES_FILENAME);
                
                File d1 = new File(INPUT_CSV_SOURCE_FILE_PATH.concat("billed_split"));
                if (d1.exists()) FileUtils.cleanDirectory(d1);
                File d2 = new File(INPUT_CSV_SOURCE_FILE_PATH.concat("unbilled_split"));
                if (d1.exists()) FileUtils.cleanDirectory(d2);
                File d3 = new File(INPUT_CSV_SOURCE_FILE_PATH.concat("adminfees_split"));
                if (d3.exists()) FileUtils.cleanDirectory(d3);
                
                Date endTime = new Date();
                LOGGER.info(String.format(Constants.JOB_FINISHED_MESSAGE, ProcessingUtils.dateToString(endTime, ProcessingUtils.SHORT_DATETIME_FORMAT)));
                LOGGER.info(String.format(Constants.JOB_PROCESSIG_TIME_MESSAGE, ((endTime.getTime() - this.startTIme.getTime()) / 1000)));
            } catch (IOException ex) {
                LOGGER.error(ex.getMessage());
            }
        } else {
            LOGGER.info(Constants.JOB_EXCEPTIONS_ENCOUNTERED);
            List<Throwable> exceptionList = je.getAllFailureExceptions();
            exceptionList.forEach((th) -> {
                LOGGER.error(String.format(Constants.EXCEPTION_MESSAGE, th.getLocalizedMessage()));
            });
        }
    }

    /**
     * moves source file to archive folder
     *
     * @param filename
     */
    private void moveFileToArchive(String filename) {
        try {
            File srcFile = new File(INPUT_CSV_SOURCE_FILE_PATH.concat(filename));
            String archiveFileName = filename.concat(".").concat(ProcessingUtils.dateToString(new Date(), ProcessingUtils.SHORT_DATETIME_FORMAT_NOSPACE)).concat(".bak");
            File destFile = new File(INPUT_CSV_SOURCE_FILE_PATH.concat("archive/").concat(archiveFileName));

            OutputStream outStream;
            try (InputStream inStream = new FileInputStream(srcFile)) {
                outStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, length);
                }
            }
            outStream.close();
            srcFile.delete();
            LOGGER.info(String.format(Constants.FILE_ARCHIVED_MESSAGE, filename));
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
    
//    @Value("${database.to.csv.job.export.file.path}")
//    private String OUTPUT_CSV_SOURCE_FILE_PATH;
    
//    private void consolidateOutputFiles() {
//        
//        OutputStream out = null;
//        try {
//            File master = new File("subledger_summary.csv");
//            out = new FileOutputStream(master);
//            byte[] buf = new byte[n];
//            for (String file : files) {
//                InputStream in = new FileInputStream(file);
//                int b = 0;
//                while ((b = in.read(buf)) >= 0) {
//                    out.write(buf, 0, b);
//                    out.flush();
//                }
//            }   out.close();
//        } catch (FileNotFoundException ex) {
//            LOGGER.error(ex.getMessage());
//        } finally {
//            try {
//                out.close();
//            } catch (IOException ex) {
//                LOGGER.error(ex.getMessage());
//            }
//        }
//    }
//    
//    private File[] findOutputFiles(String parentLocation) {
//        File outputDir = new File(parentLocation);
//        File[] files = outputDir.listFiles(new FileFilter() {
//            
//            @Override
//            public boolean accept(File pathname) {
//                return pathname.isDirectory();
//            }
//        });
//        return files;
//    }
}
