/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.readers;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.core.io.FileSystemResource;

import com.vzw.booking.bg.batch.constants.ErrorSource;
import com.vzw.booking.bg.batch.domain.RawType;
import com.vzw.booking.bg.batch.utils.CustomLineMapper;

/**
 * Generic reader class for any flat file it is used for all CSV files read from
 * mainframe
 *
 * @author smorcja
 * @param <T>
 */
public class CsvFileGenericReader<T extends RawType<String>> extends FlatFileItemReader<T> {

    private final Class<T> payloadClass;
    
    public CsvFileGenericReader(Class<T> payloadClass, String filePath, String[] fieldNames, String delimiter, int linesToSkip, ErrorSource source) {
        super();
        this.payloadClass = payloadClass;
        this.setResource(new FileSystemResource(filePath));
        this.setLinesToSkip(linesToSkip);
        this.setLineMapper(createLineMapper(fieldNames, delimiter, source));        
    }

	protected final LineMapper<T> createLineMapper(String[] fieldNames, String delimiter, ErrorSource source) {
        CustomLineMapper<T> lineMapper = new CustomLineMapper<>();
        if (fieldNames != null || fieldNames.length > 0) {
            lineMapper.setLineTokenizer(createLineTokenizer(fieldNames, delimiter));
        }
        lineMapper.setErrorSource(source);
        lineMapper.setFieldSetMapper(this.createInformationMapper());
        return lineMapper;
    }

    protected LineTokenizer createLineTokenizer(String[] fieldNames, String delimiter) {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(delimiter);
        lineTokenizer.setNames(fieldNames);
        return lineTokenizer;
    }

    protected FieldSetMapper<T> createInformationMapper() {
        BeanWrapperFieldSetMapper<T> informationMapper = new BeanWrapperFieldSetMapper();
        informationMapper.setTargetType(payloadClass);
        return informationMapper;
    }
    
    
}