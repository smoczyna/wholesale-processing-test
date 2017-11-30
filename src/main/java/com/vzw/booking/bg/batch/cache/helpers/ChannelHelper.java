package com.vzw.booking.bg.batch.cache.helpers;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vzw.booking.bg.batch.constants.ErrorSource;
import com.vzw.booking.bg.batch.constants.ErrorType;
import com.vzw.booking.bg.batch.domain.AdminFeeCsvFileDTO;
import com.vzw.booking.bg.batch.domain.AggregateWholesaleReportDTO;
import com.vzw.booking.bg.batch.domain.AltBookingCsvFileDTO;
import com.vzw.booking.bg.batch.domain.BilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.FinancialEventOffsetDTO;
import com.vzw.booking.bg.batch.domain.GenericRowTypeError;
import com.vzw.booking.bg.batch.domain.RawType;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import com.vzw.booking.bg.batch.domain.UnbilledCsvFileDTO;
import com.vzw.booking.bg.batch.streams.Channel;
import com.vzw.booking.bg.batch.streams.BatchCollector;
import com.vzw.booking.bg.batch.streams.exceptions.ChannelIOException;
import com.vzw.booking.bg.batch.streams.exceptions.ChannelNullableAssignementException;
import com.vzw.booking.bg.batch.streams.model.MultipleFileConsumer;

/**
 * @author torelfa
 *
 */
public class ChannelHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelHelper.class);
	
	private ChannelHelper() {
		throw new IllegalStateException("ChannelHelper: Helper Class");
	}
	
	private static Channel<RawType<String>> errorChannel = Channel.create();
	
	private static MultipleFileConsumer<RawType<String>> defaultConsumer = new MultipleFileConsumer<>();

	public static Channel<RawType<String>> getErrorChannel() {
		return errorChannel;
	}

	public static MultipleFileConsumer<RawType<String>> getDefaultConsumer() {
		return defaultConsumer;
	}
	
	public static final void collectProcessError(Object inputRecord) {
    	if(inputRecord!=null && RawType.class.isAssignableFrom(inputRecord.getClass())) {
            LOGGER.warn("skipped: {} ", ((RawType<String>)inputRecord).getRowType() );
        	addObjectToErrorChannel((RawType<String>)inputRecord, ErrorType.SKIPPED_PROCESS);
    	}
	}
	
	public static final void addObjectToErrorChannel(RawType<String> inputRecord, ErrorType type) {
		LOGGER.warn("ChannelHelper.addObjectToErrorChannel [{}] : {}", type, inputRecord.toString());
		try {
			GenericRowTypeError generic = null;
			if (!GenericRowTypeError.class.isAssignableFrom(inputRecord.getClass())) {
				ErrorSource source = ErrorSource.WHOLESALE_OUTPUT;
				if(AdminFeeCsvFileDTO.class.isAssignableFrom(inputRecord.getClass())) {
						source = ErrorSource.ADMIN_FEES_INPUT;
				}
				else if(BilledCsvFileDTO.class.isAssignableFrom(inputRecord.getClass())) {
					source = ErrorSource.BILLED_BOOKING_INPUT;
				}
				else if(UnbilledCsvFileDTO.class.isAssignableFrom(inputRecord.getClass())) {
					source = ErrorSource.UNBILLED_BOOKING_INPUT;
				}
				else if(BookDateCsvFileDTO.class.isAssignableFrom(inputRecord.getClass())) {
					source = ErrorSource.BOOK_DATES_INPUT;
				}
				else if(AltBookingCsvFileDTO.class.isAssignableFrom(inputRecord.getClass())) {
					source = ErrorSource.ALTERNATE_BOOKING_INPUT;
				}
				else if(FinancialEventOffsetDTO.class.isAssignableFrom(inputRecord.getClass())) {
					source = ErrorSource.FINANCIAL_EVENT_OFFSET_INPUT;
				}
				else if(SummarySubLedgerDTO.class.isAssignableFrom(inputRecord.getClass())) {
					source = ErrorSource.SUBLEDGER_OUTPUT;
				}
				else if(!AggregateWholesaleReportDTO.class.isAssignableFrom(inputRecord.getClass())) {
					throw new IllegalStateException("Unknown RawType SubClass : " + inputRecord.getClass().getName());
				}
				generic = new GenericRowTypeError(inputRecord.getRowType(), type, source);
			} else {
				generic = (GenericRowTypeError)inputRecord;
			}
			errorChannel.add(generic);
		} catch (Exception e) {
			LOGGER.error("Error adding raw type to channel: {}", e.getMessage());
		}
	}
	
	public static <T> BatchCollector<T> batchCollector(int batchSize, Consumer<List<T>> batchProcessor) {
		return new BatchCollector<T>(batchSize, batchProcessor);
	}
}
