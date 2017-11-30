/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.jobs;

import com.vzw.booking.bg.batch.cache.helpers.ChannelHelper;
import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.constants.ErrorSource;
import com.vzw.booking.bg.batch.constants.ErrorType;
import com.vzw.booking.bg.batch.utils.ProcessingUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StreamUtils;

/**
 *
 * @author smorcja
 */
public class SourceFilesExistanceChecker implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFilesExistanceChecker.class);
    
    @Value("${csv.to.database.job.source.file.path}")
    private String SOURCE_FILES_PATH;
    
    @Value("${csv.to.database.job.source.file.splitSize}")
    private int SOURCE_FILES_SPLIT_SIZE;
    
    @Value("${database.to.csv.job.export.file.path}")
    private String TARGET_FILES_PATH;

    @Value("${com.springbatch.output.executor.archive.output}")
    private boolean ARCHIVE_OUTPUT=false;

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        LOGGER.info(Constants.CHECK_IF_FILES_EXIST);
        if (SOURCE_FILES_PATH==null || SOURCE_FILES_PATH.isEmpty())
            throw new JobInterruptedException(Constants.SOURCE_LOCATION_MISSING_MESSAGE);
        if (TARGET_FILES_PATH==null || TARGET_FILES_PATH.isEmpty())
            throw new JobInterruptedException(Constants.TARGET_LOCATION_MISSING_MESSAGE);
        if (ARCHIVE_OUTPUT) {
        	archiveOutputFiles();
        }
        String errorFolderPath=TARGET_FILES_PATH.concat("quarantine");
        if (!new File(errorFolderPath).exists()) {
        	new File(errorFolderPath).mkdirs();
        }
        //Clear Default Writer instance
        ChannelHelper.getDefaultConsumer().clear();
        
        //Creating Read Skipped Records error files
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.ADMIN_FEES_SKIPPED_ERROR_FILENAME), ErrorType.SKIPPED_INPUT, ErrorSource.ADMIN_FEES_INPUT);
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.BILLED_BOOKING_SKIPPED_ERROR_FILENAME), ErrorType.SKIPPED_INPUT, ErrorSource.BILLED_BOOKING_INPUT);
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.UNBILLED_BOOKING_SKIPPED_ERROR_FILENAME), ErrorType.SKIPPED_INPUT, ErrorSource.UNBILLED_BOOKING_INPUT);
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.ALTERNATE_BOOKING_SKIPPED_ERROR_FILENAME), ErrorType.SKIPPED_INPUT, ErrorSource.ALTERNATE_BOOKING_INPUT);

        //Creating Process Skipped Records error files
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.ADMIN_FEES_PROCESS_ERROR_FILENAME), ErrorType.SKIPPED_PROCESS, ErrorSource.ADMIN_FEES_INPUT);
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.BILLED_BOOKING_PROCESS_ERROR_FILENAME), ErrorType.SKIPPED_PROCESS, ErrorSource.BILLED_BOOKING_INPUT);
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.UNBILLED_BOOKING__PROCESS_ERROR_FILENAME), ErrorType.SKIPPED_PROCESS, ErrorSource.UNBILLED_BOOKING_INPUT);

        //Creating Output Write Skipped Records error files
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.SUBLEDGER_WRITE_ERROR_FILENAME), ErrorType.NOT_OUTPUT_WRITTEN, ErrorSource.SUBLEDGER_OUTPUT);
        createErrorFile(errorFolderPath.concat(File.separator).concat(Constants.WHOLESALE_WRITE_ERROR_FILENAME), ErrorType.NOT_OUTPUT_WRITTEN, ErrorSource.WHOLESALE_OUTPUT);
        
        //Activating Channel and assign listener
        ChannelHelper.getErrorChannel().addChannelConsumer(ChannelHelper.getDefaultConsumer());
        checkErrorFiles();
        ChannelHelper.getErrorChannel().start();

        File f1 = new File(SOURCE_FILES_PATH.concat(Constants.BOOK_DATE_FILENAME));
        File f2 = new File(SOURCE_FILES_PATH.concat(Constants.FINANCIAL_EVENT_OFFSET_FILENAME));
        File f3 = new File(SOURCE_FILES_PATH.concat(Constants.ALT_BOOKING_FILENAME));
        File f4 = new File(SOURCE_FILES_PATH.concat(Constants.BILLED_BOOKING_FILENAME));
        File f5 = new File(SOURCE_FILES_PATH.concat(Constants.UNBILLED_BOOKING_FILENAME));
        File f6 = new File(SOURCE_FILES_PATH.concat(Constants.ADMIN_FEES_FILENAME));
        if ((!f1.exists() || f1.isDirectory()) ||
            (!f2.exists() || f2.isDirectory()) ||
            (!f3.exists() || f3.isDirectory()) ||
            (!f4.exists() || f4.isDirectory()) ||
            (!f5.exists() || f5.isDirectory()) ||
            (!f6.exists() || f6.isDirectory())) {
            LOGGER.error(Constants.FILES_NOT_FOUND_JOB_ABORTED);
            throw new JobInterruptedException(Constants.FILES_NOT_FOUND_MESSAGE);
        } else {
            try {
				splitTextFile(f4, SOURCE_FILES_SPLIT_SIZE);
				splitTextFile(f5, SOURCE_FILES_SPLIT_SIZE);
				splitTextFile(f6, SOURCE_FILES_SPLIT_SIZE);
				return RepeatStatus.FINISHED;
			} catch (Exception e) {
				throw new JobInterruptedException(Constants.FILES_NOT_SPLITTED_MESSAGE);
			}
        }
    }
 
    
    private final void archiveOutputFiles() {
    	File[] files = findOutputFiles(TARGET_FILES_PATH);
    	if (files.length>0) {
           	String archiveFolderPath = TARGET_FILES_PATH.concat("archive").concat(File.separator).concat(ProcessingUtils.dateToString(new Date(), ProcessingUtils.SHORT_DATETIME_FORMAT_NOSPACE)).concat(""+System.nanoTime());
           	for (File file: files) {
           		moveFileToArchive(file, archiveFolderPath);
           	}
           	System.out.println("Old Output files archived to " + archiveFolderPath);
    	} else {
           	System.out.println("No Old Output files to archive");
    	}
    }


    /**
     * moves source file to archive folder
     *
     * @param filename
     */
    private void moveFileToArchive(File file, String archiveFolderPath) {
        try {
         	File baseArchiveFolder = new File(archiveFolderPath);
        	if (!baseArchiveFolder.exists())
        		baseArchiveFolder.mkdirs();
            String archiveFileName = archiveFolderPath.concat(File.separator).concat(file.getName());
            File destFile = new File(archiveFileName);
            file.renameTo(destFile);
            LOGGER.info(String.format(Constants.FILE_ARCHIVED_MESSAGE, file.getName()));
        } catch (Exception e) {
            LOGGER.error("Error moving output file: {} to archive, cause: {}", file.getName(), e.getMessage());
        }
    }

    private static File[] findOutputFiles(String parentDir) {
        File outputDir = new File(parentDir);
        File[] files = outputDir.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File file) {
            	return file != null &&
            			( (! file.isDirectory() && file.getName().matches(".*\\.csv$")) || 
            			  (  file.isDirectory() && file.getName().indexOf("quarantine")>=0)
            			);
//                if (! file.isDirectory() && file.getName().matches(".*\\.csv$")) {
//                    LOGGER.warn("File found: {}", file.getName());
//                    return true;
//                } if (file.isDirectory() && file.getName().indexOf("quarantine") else
//                    return false;
            }
        });
        return files;
    }
    
    public static void checkErrorFiles() {
    	ChannelHelper.getDefaultConsumer().logState();
    }
    
    public static void createErrorFile(String filePath, ErrorType type, ErrorSource source) throws IOException {
    	File f = new File(filePath);
    	if (f.exists()) {
    		f.delete();
    	}
    	f.createNewFile();
    	f.setReadable(true);
    	f.setWritable(true);
    	FileWriter writer = new FileWriter(f);
    	ChannelHelper.getDefaultConsumer().addWriter(type, source, writer);
    }
   
    private static final void save(List<String> lines, Path splitFilePath) {
    	File f = splitFilePath.toFile();
    	if (f.exists()) {
    		boolean removed = f.delete();
			LOGGER.error("Drained existing split file : {}, status: {}", splitFilePath.getFileName(), removed);
    	}
		try {
			f.createNewFile();
			f.setReadable(true);
			f.setWritable(true, true);
		} catch (Exception e) {
			LOGGER.error("Error creating split file : {}, cause: {}", splitFilePath.getFileName(), e.getMessage());
			throw new RuntimeException(e);
		}
		try (FileOutputStream fs = new FileOutputStream(f) ) {
	    	IOUtils.writeLines(lines, System.lineSeparator(), fs);
		} catch (Exception e) {
			LOGGER.error("Error processing split file : {}, cause: {}", splitFilePath.getFileName(), e.getMessage());
			throw new RuntimeException(e);
		}
    }
    
    public static void splitTextFile(File bigFile, int maxRows) throws Exception {
        int i = 1;
        String ext = FilenameUtils.getExtension(bigFile.getName());
        String fileNoExt = bigFile.getName().replace("."+ext, "");        
        File newDir = new File(bigFile.getParent() + "/" + fileNoExt + "_split");
        if (!newDir.exists()) newDir.mkdirs();
        AtomicInteger  counter = new AtomicInteger(0);
        try {
			Files.lines(bigFile.toPath())
			.collect(ChannelHelper.batchCollector(maxRows, xs -> {
				int chunkNumber = counter.incrementAndGet();
			    Path splitFilePath = Paths.get(newDir.getPath() + "/" + fileNoExt + "_" + String.format("%03d", chunkNumber) + "." + ext);
			    SourceFilesExistanceChecker.save(xs, splitFilePath);
			 }));
		} catch (Exception e) {
			LOGGER.error("Error processing split file : {}, cause: {}", bigFile.getName(), e.getMessage());
			throw e;
		}
        LOGGER.info(String.format(Constants.FILE_SPLIT_MESSAGE, bigFile.getName(), i));
    }
}
