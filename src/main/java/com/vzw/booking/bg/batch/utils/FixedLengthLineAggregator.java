/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.utils;

import com.vzw.booking.bg.batch.domain.Externalizable;
import com.vzw.booking.bg.batch.domain.ExternalizationMetadata;
import com.vzw.booking.bg.batch.domain.exceptions.ContentTooLongException;
import com.vzw.booking.bg.batch.domain.exceptions.ExternalizationException;
import com.vzw.booking.bg.batch.writers.WholesaleOutputWriter;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.transform.Alignment;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author smorcja
 * @param <T>
 */
public class FixedLengthLineAggregator<T extends Externalizable> implements LineAggregator<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(FixedLengthLineAggregator.class);

    private ExternalizationMetadata metaData;
        
    public FixedLengthLineAggregator() {
    	super();
    }
    
    public void setFormat(ExternalizationMetadata metaData) {
        this.metaData=metaData;
    }

    @Override
    public String aggregate(T t) {
    	try {
			return t.dump(metaData);
		} catch (ExternalizationException e) {
			LOGGER.error("Unable to parse record : {} ", e.getMessage());
			return null;
		}
//        StringBuilder line = new StringBuilder();
//        Field[] declaredFields = payloadClass.getDeclaredFields();
//        for (Field field : declaredFields) {
//            try {
//                String strValue = null;
//                char padding = '0';
//                Alignment alignment = Alignment.RIGHT;
//                Object value = new PropertyDescriptor(field.getName(), payloadClass).getReadMethod().invoke(t);
//                if (value != null) {
//                    //check only types we can have in our input files
//                    if (value.getClass() == String.class) {
//                        strValue = (String) value;
//                        //padding = ' ';
//                        //alignment = Alignment.LEFT;
//                    } else {
//                        if (value.getClass() == java.lang.Integer.class) strValue = Integer.toString((Integer) value);
//                        else if (value.getClass() == Long.class) strValue = Long.toString((Long) value);
//                        else if (value.getClass() == java.lang.Short.class) strValue = Short.toString((Short) value);
//                        else if (value.getClass() == java.lang.Double.class) strValue = String.format("%.2f", (Double) value);
//                        else throw new IllegalAccessException("Unrecognized field type"); //that should never happen actually 
//                    }
//                }
//                if (fieldsDefinition.containsKey(field.getName())) {
//                    StringBuilder sb = new StringBuilder();
//                    int fieldLength = fieldsDefinition.get(field.getName());
//                    char[] pad = new char[fieldLength];
//                    Arrays.fill(pad, padding);
//                    sb.append(pad);
//                    if (strValue!=null) {
//                        int start=0;
//                        if (alignment == Alignment.RIGHT) {                            
//                            start += (fieldLength - strValue.length());
//                        } else if (alignment == Alignment.CENTER) {
//                            start += ((fieldLength - strValue.length()) / 2);
//                        }
//                        if (start < 0) {
//                            throw new ContentTooLongException("Contents to long to fit defined output field");
//                        }
//                        sb.replace(start, start + strValue.length(), strValue);
//                    }
//                    line.append(sb.toString());
//                }
//            } catch (IllegalAccessException | InvocationTargetException | IntrospectionException | ContentTooLongException ex) {
//                Logger.getLogger(FixedLengthLineAggregator.class.getName()).log(Level.SEVERE, null, ex);
//                return null;
//            }
//        }
//        return line.toString();
    }    
}