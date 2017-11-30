/**
 * 
 */
package com.vzw.booking.bg.batch.streams.model;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vzw.booking.bg.batch.constants.ErrorSource;
import com.vzw.booking.bg.batch.constants.ErrorType;
import com.vzw.booking.bg.batch.domain.GenericRowTypeError;
import com.vzw.booking.bg.batch.domain.RawType;

/**
 * @author torelfa
 *
 */
public class MultipleFileConsumer<T extends RawType<?>> implements ChannelConsumer<T> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MultipleFileConsumer.class);

	private Map<ConsumerKey, FileWriter> writersMap = new ConcurrentHashMap<>(0);
	
	private AtomicLong latestChange = new AtomicLong(System.nanoTime());
	
	/**
	 * Constructor
	 */
	public MultipleFileConsumer() {
		super();
	}
	
	/**
	 * @param type
	 * @param source
	 * @param writer
	 * @return
	 */
	public boolean addWriter(ErrorType type, ErrorSource source, FileWriter writer) {
		if (source!=null && type!=null && writer!=null) {
			writersMap.put(new ConsumerKey(type, source), writer);
			return true;
		}
		return false;
	}

	/**
	 * @return
	 */
	public FileWriter getByKey(ErrorType type, ErrorSource source) {
		return writersMap.get(new ConsumerKey(type, source));
	}

	/**
	 * @return
	 */
	public void logState() {
		for(ConsumerKey key: this.getAllKeys()) {
//			LOGGER.info("****** DISCOVERED KEY = " + key);
//			LOGGER.info("****** DISCOVERED VALUE = " + getByKey(key));
			LOGGER.info("****** DISCOVERED VALUE = " + getByKey(key.getType(), key.getSource()));
		}
	}

	/**
	 * @return
	 */
	public FileWriter getByKey(ConsumerKey key) {
		return writersMap.get(key);
	}

	/**
	 * @return
	 */
	public List<FileWriter> getAllWriters() {
		return new ArrayList<>(writersMap.values());
	}

	/**
	 * @return
	 */
	public List<ConsumerKey> getAllKeys() {
		return new ArrayList<>(writersMap.keySet());
	}

	/**
	 * 
	 */
	public void clear() {
		this.writersMap.clear();
	}

	/**
	 * @return the latestChange
	 */
	public final long getLatestChange() {
		return latestChange.get();
	}

	/* (non-Javadoc)
	 * @see com.vzw.booking.bg.batch.streams.model.ChannelConsumer#consume(java.lang.Object)
	 */
	@Override
	public void consume(T t) {
		if (GenericRowTypeError.class.isAssignableFrom(t.getClass())) {
			GenericRowTypeError generic=(GenericRowTypeError)t;
			ConsumerKey key = new ConsumerKey(generic.getErrorType(), generic.getErrorSource());
			LOGGER.warn("Testing key: {} for line: {}", key, t.getRowType().toString());
			FileWriter currentWriter=null;
			if ( (currentWriter = writersMap.get(key)) != null ) {
				this.latestChange.set(System.nanoTime());
				synchronized (currentWriter) {
					try {
						LOGGER.warn("Writing line of key: {} line: {}", key, t.getRowType().toString());
						currentWriter.write(t.getRowType().toString()+System.lineSeparator());
						currentWriter.flush();
					} catch (IOException e) {
						LOGGER.error("For key: "+key+" unable to save line : " + t.getRowType().toString());
					}
				}
			} else {
				LOGGER.error("Key Writer not registered: " + key);
			}
		} else {
			LOGGER.error("Expected type GenericRowTypeError instead found : " + t.getClass().getName());
		}
	}

	private static class ConsumerKey {
		private ErrorType type;
		private ErrorSource source;
		/**
		 * Constructor
		 * @param type
		 * @param source
		 */
		public ConsumerKey(ErrorType type, ErrorSource source) {
			super();
			this.type = type;
			this.source = source;
		}
		/**
		 * @return the type
		 */
		public final ErrorType getType() {
			return type;
		}
		/**
		 * @return the source
		 */
		public final ErrorSource getSource() {
			return source;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object arg0) {
			try {
				LOGGER.warn("key: {} otherKey: {}", this, arg0);
				if ( arg0 == null ) {
					return false;
				}
				ConsumerKey otherKey = (ConsumerKey) arg0;
				LOGGER.warn("key: {} otherKey: {}", this, arg0);
				return source==otherKey.getSource() && type==otherKey.getType();
			} catch (Exception e) {
				return false;
			}
		}
		
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return source.hashCode() + type.hashCode();
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "ConsumerKey [type="+type+",source=" + source + "]";
		}
		
	}
	
}
