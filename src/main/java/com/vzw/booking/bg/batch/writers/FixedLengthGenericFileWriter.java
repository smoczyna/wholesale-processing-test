/*
 * To change this license header, choose License Headers
 in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.writers;

import com.vzw.booking.bg.batch.domain.Externalizable;
import com.vzw.booking.bg.batch.domain.ExternalizationMetadata;
import com.vzw.booking.bg.batch.utils.FixedLengthLineAggregator;
import java.util.Map;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;
/**
 *
 * @author smorcja
 * @param <T>
 */
public class FixedLengthGenericFileWriter<T extends Externalizable> extends FlatFileItemWriter<T> {
    private final Class<T> payloadClass;
    private FixedLengthLineAggregator<T> aggregator;
    
    public FixedLengthGenericFileWriter(Class<T> payloadClass, String fileName, Map<String, Integer> columns) {
        this.payloadClass = payloadClass;
        super.setAppendAllowed(true);
        this.setResource(new FileSystemResource(fileName));
        aggregator = new FixedLengthLineAggregator<T>(this.payloadClass, columns);
        this.setLineAggregator(aggregator);
    }
    
    public void setUpLineAggregator(ExternalizationMetadata metaData) {
    	this.aggregator.setFormat(metaData);
    }
}
