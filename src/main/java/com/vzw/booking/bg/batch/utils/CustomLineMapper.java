/**
 * 
 */
package com.vzw.booking.bg.batch.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;

import com.vzw.booking.bg.batch.cache.helpers.ChannelHelper;
import com.vzw.booking.bg.batch.constants.ErrorSource;
import com.vzw.booking.bg.batch.constants.ErrorType;
import com.vzw.booking.bg.batch.domain.GenericRowTypeError;
import com.vzw.booking.bg.batch.domain.RawType;

/**
 * @author TORELFA
 *
 */
public class CustomLineMapper<T extends RawType<String>> extends DefaultLineMapper<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomLineMapper.class);
	
	private ErrorSource source=ErrorSource.ADMIN_FEES_INPUT;
	
	public void setErrorSource(ErrorSource source) {
		this.source=source;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.file.mapping.DefaultLineMapper#mapLine(java.lang.String, int)
	 */
	@Override
	public T mapLine(String line, int lineNumber) throws Exception {
		try {
			T t = super.mapLine(line, lineNumber);
			t.setRowType(line, lineNumber);
			return t;
		} catch (Exception e) {
			LOGGER.error("Skipping line ["+lineNumber+"] : <" + line + ">");
			if (!line.isEmpty()) {
				GenericRowTypeError generic = new GenericRowTypeError(line, ErrorType.SKIPPED_INPUT, source);
				ChannelHelper.addObjectToErrorChannel(generic, ErrorType.SKIPPED_INPUT);
			}
			throw e;
		}
	}

}
