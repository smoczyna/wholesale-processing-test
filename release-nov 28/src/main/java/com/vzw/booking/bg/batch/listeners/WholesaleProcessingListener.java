/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.listeners;

import com.vzw.booking.bg.batch.cache.helpers.ChannelHelper;
import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.constants.ErrorType;
import com.vzw.booking.bg.batch.domain.RawType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.SkipListener;
/**
 *
 * @author smorcja
 */
public class WholesaleProcessingListener implements ItemProcessListener, SkipListener {
        
    private static final Logger LOGGER = LoggerFactory.getLogger(WholesaleProcessingListener.class);
        
    @Override
    public void onSkipInRead(Throwable exception) {
        LOGGER.warn(String.format(Constants.READER_EXCEPTION, exception.getMessage()));
    }

    @Override
    public void onSkipInWrite(Object s, Throwable exception) {
        LOGGER.warn(String.format(Constants.WRITER_EXCEPTION, exception.getMessage()));
    }

    @SuppressWarnings("unchecked")
	@Override
    public void onSkipInProcess(Object inputRecord, Throwable exception) {
        LOGGER.warn(Constants.RECORD_SKIP_DETECTED);
        if (
        		(exception == null || ! NullPointerException.class.isAssignableFrom(exception.getClass()) ) 
        		&& 
        	RawType.class.isAssignableFrom(inputRecord.getClass())) {
            LOGGER.warn("skipped: {} ", ((RawType<String>)inputRecord).getRowType() );
        	ChannelHelper.addObjectToErrorChannel((RawType<String>)inputRecord, ErrorType.SKIPPED_PROCESS);
        }
        LOGGER.warn(exception.getMessage());
    }
    
    @Override
    public void beforeProcess(Object t) {
        LOGGER.info(String.format(Constants.PROCESSING_RECORD, t.toString()));
    }

    @Override
    public void afterProcess(Object t, Object s) {
    }

    @Override
    public void onProcessError(Object t, Exception excptn) {
    }
}
