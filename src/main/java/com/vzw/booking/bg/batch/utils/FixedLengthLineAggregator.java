/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.utils;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.transform.LineAggregator;

import com.vzw.booking.bg.batch.cache.helpers.ChannelHelper;
import com.vzw.booking.bg.batch.constants.ErrorType;
import com.vzw.booking.bg.batch.constants.ExternalizationFormat;
import com.vzw.booking.bg.batch.domain.Externalizable;
import com.vzw.booking.bg.batch.domain.ExternalizationMetadata;
import com.vzw.booking.bg.batch.domain.RawType;
import com.vzw.booking.bg.batch.domain.exceptions.ExternalizationException;

/**
 *
 * @author smorcja
 * @param <T>
 */
public class FixedLengthLineAggregator<T extends Externalizable> implements LineAggregator<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixedLengthLineAggregator.class);

    private ExternalizationMetadata metaData;
    
    private AtomicLong aggregatorCounter = new AtomicLong(0L);
        
    public FixedLengthLineAggregator() {
        super();
    }

    public void setFormat(ExternalizationMetadata metaData) {
        this.metaData = metaData;
    }

    @Override
    public String aggregate(T t) {
    	long lineNumber = aggregatorCounter.incrementAndGet();
    	try {
			return t.dump(metaData);
		} catch (ExternalizationException e) {
			if (t!=null && RawType.class.isAssignableFrom(t.getClass())) {
				try {
					@SuppressWarnings("unchecked")
					RawType<String> inputRecord = (RawType<String>) t;
					try {
						inputRecord.setRowType(t.asStringDescriptor(metaData), lineNumber);
					} catch (Exception e2) {
						LOGGER.error("Unable to enqueue send to channel error : {} ", t!=null ? t.toString() : "<NONE>");
					}
					ChannelHelper.addObjectToErrorChannel(inputRecord, ErrorType.NOT_OUTPUT_WRITTEN);
				} catch (Exception e1) {
					try {
						LOGGER.error("Unable to enqueue output to error : {} ", t.asStringDescriptor(metaData.clone(ExternalizationFormat.COMMA_SEPARATED_FORMAT)));
					} catch (Exception e2) {
						LOGGER.error("Unable to enqueue output to error : <NOT-PARSEABLE> ");
					}
				}
			}
			LOGGER.error("Unable to parse record : {} ", e.getMessage());
			return null;
		}
    }    
}
