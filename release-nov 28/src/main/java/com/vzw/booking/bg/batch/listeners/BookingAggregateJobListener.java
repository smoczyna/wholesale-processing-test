/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.listeners;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.vzw.booking.bg.batch.cache.helpers.ChannelHelper;
import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.utils.ProcessingUtils;
import com.vzw.booking.bg.batch.utils.WholesaleBookingProcessorHelper;

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

    @Value("${database.to.csv.job.export.file.path}")
    private String OUTPUT_CSV_SOURCE_FILE_PATH;

    @Value("${com.springbatch.output.executor.archive.input}")
    private boolean ARCHIVE_INPUT=false;
    
    @Override
    public void beforeJob(JobExecution je) {
        this.startTIme = new Date();
        LOGGER.info(String.format(Constants.JOB_STARTED_MESSAGE, ProcessingUtils.dateToString(this.startTIme, ProcessingUtils.SHORT_DATETIME_FORMAT)));
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
            	
                //Activating Channel and assign listener
                try {
                	long totalChannelErrors = ChannelHelper.getErrorChannel().getTotalCount();
                	LOGGER.error("Total reported errors : {} ", totalChannelErrors);
					LOGGER.warn("Stopping error channel...");
					ChannelHelper.getErrorChannel().stop();
					while (System.nanoTime() - ChannelHelper.getDefaultConsumer().getLatestChange() < 2000) {
						try {
							LOGGER.warn("Waiting for errors to be completely flushed in files...");
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
					LOGGER.warn("Closing error files...");
					for (FileWriter writer: ChannelHelper.getDefaultConsumer().getAllWriters()) {
						if (writer!=null) {
							writer.flush();
							writer.close();
						}
					}
					LOGGER.warn("Closing error channel...");
					ChannelHelper.getDefaultConsumer().clear();
					ChannelHelper.getErrorChannel().removeChannelConsumer(ChannelHelper.getDefaultConsumer());
				} catch (Exception e) {
					LOGGER.error("Error closing and flushing errors in output files...");
				}
            	
                if (ARCHIVE_INPUT) {
                    this.moveFileToArchive(Constants.BOOK_DATE_FILENAME);
                    this.moveFileToArchive(Constants.ALT_BOOKING_FILENAME);
                    this.moveFileToArchive(Constants.FINANCIAL_EVENT_OFFSET_FILENAME);
                    this.moveFileToArchive(Constants.BILLED_BOOKING_FILENAME);
                    this.moveFileToArchive(Constants.UNBILLED_BOOKING_FILENAME);
                    this.moveFileToArchive(Constants.ADMIN_FEES_FILENAME);
                }

                File d1 = new File(INPUT_CSV_SOURCE_FILE_PATH.concat("billed_split"));
                if (d1.exists()) FileUtils.cleanDirectory(d1);
                File d2 = new File(INPUT_CSV_SOURCE_FILE_PATH.concat("unbilled_split"));
                if (d1.exists()) FileUtils.cleanDirectory(d2);
                File d3 = new File(INPUT_CSV_SOURCE_FILE_PATH.concat("adminfees_split"));
                if (d3.exists()) FileUtils.cleanDirectory(d3);
                d1.deleteOnExit();
                d2.deleteOnExit();
                d3.deleteOnExit();
                 
                File[] files = findOutputFiles(OUTPUT_CSV_SOURCE_FILE_PATH, Constants.WHOLESALE_REPORT_FILENAME_PATTERN);
                consolidateOutputFiles(files, OUTPUT_CSV_SOURCE_FILE_PATH, Constants.WHOLESALE_REPORT_FILENAME);

                files = findOutputFiles(OUTPUT_CSV_SOURCE_FILE_PATH, Constants.SUBLEDGER_SUMMARY_FILENAME_PATTERN);
                consolidateOutputFiles(files, OUTPUT_CSV_SOURCE_FILE_PATH, Constants.SUBLEDGER_SUMMARY_FILENAME);
                
                Date endTime = new Date();
                System.out.println(String.format(Constants.JOB_FINISHED_MESSAGE, ProcessingUtils.dateToString(endTime, ProcessingUtils.SHORT_DATETIME_FORMAT)));
                System.out.println(String.format(Constants.JOB_PROCESSIG_TIME_MESSAGE, ((endTime.getTime() - this.startTIme.getTime()) / 1000)));
            } catch (Exception ex) {
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
        	String archiveFolderPath = INPUT_CSV_SOURCE_FILE_PATH.concat("archive");
        	File baseArchiveFolder = new File(archiveFolderPath);
        	if (!baseArchiveFolder.exists())
        		baseArchiveFolder.mkdirs();
            File srcFile = new File(INPUT_CSV_SOURCE_FILE_PATH.concat(filename));
            String archiveFileName = filename.concat(".").concat(ProcessingUtils.dateToString(new Date(), ProcessingUtils.SHORT_DATETIME_FORMAT_NOSPACE)).concat(".bak");
            File destFile = new File(archiveFolderPath.concat(File.separator + archiveFileName));
            srcFile.renameTo(destFile);
            LOGGER.info(String.format(Constants.FILE_ARCHIVED_MESSAGE, filename));
        } catch (Exception e) {
            LOGGER.error("Error moving input file: {} to archive, cause: {}", filename, e.getMessage());
        }
    }

    private static File[] findOutputFiles(String parentDir, String namePattern) {
        File outputDir = new File(parentDir);
        File[] files = outputDir.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File file) {
                if (! file.isDirectory() && file.getName().matches(namePattern + "_.*[0-9]\\.csv$")) {
                    LOGGER.warn("File found: {}", file.getName());
                    return true;
                } else
                    return false;
            }
        });
        return files;
    }
    
    private static void appendFile(File file, OutputStream output) throws IOException {
        try (InputStream input = new FileInputStream(file.getAbsolutePath())) {
        	IOUtils.copy(input, output);
        } catch (IOException e) {
        	throw e;
        } catch (NullPointerException e) {
        	throw e;
        }
    }
    
    private static void consolidateOutputFiles(File[] files, String destinationDir, String filename) throws Exception {
        File master = new File(destinationDir.concat(filename));
        master.setReadable(true);
        master.setWritable(true, true);
        try ( OutputStream output = new FileOutputStream(master) ) {
			Arrays.asList(files).forEach(
					f -> {
						LOGGER.warn("Consolidating file : " + f.getName());
						try {
							if (!f.canRead())
								f.setReadable(true);
							if (!f.canWrite())
								f.setWritable(true);
							appendFile(f, output);
							f.deleteOnExit();
						} catch (Exception e) {
							throw new RuntimeException("Error concatenating file: "+f.getAbsolutePath(),e);
						}
					});
		} catch (Exception e) {
			LOGGER.error("Error consolidating file: {}, reason: ", filename, e.getMessage());
			throw e;
		}
    }
}
