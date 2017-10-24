/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.listeners;

import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.utils.WholesaleBookingProcessorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author smorcja
 */
public class GenericStepExecutionListener implements StepExecutionListener, SkipListener, ChunkListener {

    @Autowired
    WholesaleBookingProcessorHelper processingHelper;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericStepExecutionListener.class);
    
    @Override
    public void beforeStep(StepExecution se) {
    }

    @Override
    public ExitStatus afterStep(StepExecution se) {
        LOGGER.info(String.format(Constants.JOB_EXECUTION_FINISHED, se.getReadCount(), se.getWriteCount()));        
        LOGGER.info(String.format(Constants.WHOLESALE_REPORT_NO, this.processingHelper.getCounter(Constants.WHOLESALES_REPORT)));
        LOGGER.info(String.format(Constants.SUBLEDGER_REPORD_NO, this.processingHelper.getCounter(Constants.SUBLEDGER)));
        this.processingHelper.clearCounters();
        return se.getExitStatus();
    }

    @Override
    public void onSkipInRead(Throwable exception) {
        LOGGER.error(String.format(Constants.READER_EXCEPTION, exception.getMessage()));
    }

    @Override
    public void onSkipInWrite(Object s, Throwable exception) {
        LOGGER.error(String.format(Constants.WRITER_EXCEPTION, exception.getMessage()));
    }

    @Override
    public void onSkipInProcess(Object inputRecord, Throwable exception) {
        LOGGER.error(inputRecord.toString());
    }

    @Override
    public void beforeChunk(ChunkContext cc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void afterChunk(ChunkContext cc) {
        int count = cc.getStepContext().getStepExecution().getReadCount();
        LOGGER.info("Records processed so far: "+count);
    }

    @Override
    public void afterChunkError(ChunkContext cc) {        
    }
}
