/**
 * 
 */
package com.vzw.booking.bg.batch.streams;

/**
 * @author torelfa
 *
 */
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vzw.booking.bg.batch.streams.exceptions.ChannelIOException;
import com.vzw.booking.bg.batch.streams.exceptions.ChannelNullableAssignementException;
import com.vzw.booking.bg.batch.streams.exceptions.DuplicatedObjectException;
import com.vzw.booking.bg.batch.streams.model.ChannelConsumer;

/**
 * Realize a Parametric Stream
 * @author Fabrizio Torelli &lt;hellgate75@gmail.com&gt;
 * @param <T> Type for channel
 */
public class Channel<T> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);
	
	private ChannelThread<T> thread = new ChannelThread<>(this);

	private ConcurrentLinkedQueue<T> channel = new ConcurrentLinkedQueue<>();
	
	private ConcurrentLinkedQueue<ChannelConsumer<T>> consumers = new ConcurrentLinkedQueue<>();
	
	private AtomicLong totalCount = new AtomicLong(0L);

	/**
	 * Constructor for stand-alone Channels
	 * @param type Channel Objects type
	 */
	public Channel() {
		super();
	}
	/**
	 * Add a consumer to the channel output operations
	 * @param consumer Consumer to add the queue output operations
	 * @throws ChannelNullableAssignementException Thrown when provided consumer is null
	 * @throws DuplicatedObjectException Throw when the  lister is registered twice
	 * @see ChannelConsumer
	 */
	public void addChannelConsumer(ChannelConsumer<T> consumer) throws ChannelNullableAssignementException, DuplicatedObjectException {
		if (consumer==null) {
			throw new ChannelNullableAssignementException("Forbidden registration of nullable consumers");
		}
		if (consumers.contains(consumer)) {
			throw new DuplicatedObjectException("This consumer has been already registered");
		}
		consumers.add(consumer);
	}
	
	/**
	 * Remove a consumer from the channel output operations
	 * @param consumer Consumer to be registered to output operations
	 * @throws ChannelNullableAssignementException Thrown when provided consumer is null
	 * @see ChannelConsumer
	 */
	public void removeChannelConsumer(ChannelConsumer<T> consumer) throws ChannelNullableAssignementException {
		if (consumer==null) {
			throw new ChannelNullableAssignementException("Forbidden unregistration of nullable consumers");
		}
		consumers.remove(consumer);
	}

	/**
	 * @param t
	 * @return
	 * @throws ChannelNullableAssignementException
	 * @throws ChannelIOException
	 */
	public boolean add(T t) throws ChannelNullableAssignementException, ChannelIOException {
		if (t==null) {
			throw new ChannelNullableAssignementException("Forbidden registration of nullable element");
		}
		try {
			totalCount.incrementAndGet();
			return this.channel.add(t);
		} catch (Exception e) {
			LOGGER.error("Error adding element: {}", e.getMessage());
			throw new ChannelIOException("Unable to add value", e);
		}
	}

	/**
	 * @param t
	 * @return
	 * @throws ChannelNullableAssignementException
	 * @throws ChannelIOException
	 */
	@SuppressWarnings("unchecked")
	public boolean add(T ...t) throws ChannelNullableAssignementException, ChannelIOException {
		if (t==null) {
			throw new ChannelNullableAssignementException("Forbidden registration of nullable element list");
		}
		try {
			return this.channel.addAll(Arrays.asList(t));
		} catch (Exception e) {
			LOGGER.error("Error adding array: {}", e.getMessage());
			throw new ChannelIOException("Unable to add values", e);
		} finally {
			totalCount.addAndGet(t.length);
		}
	}

	/**
	 * @param collection
	 * @return
	 * @throws ChannelNullableAssignementException
	 * @throws ChannelIOException
	 */
	public boolean add(Collection<T> collection) throws ChannelNullableAssignementException, ChannelIOException {
		if (collection==null) {
			throw new ChannelNullableAssignementException("Forbidden registration of nullable collection");
		}
		try {
			return this.channel.addAll(collection);
		} catch (Exception e) {
			LOGGER.error("Error adding collection: {}", e.getMessage());
			throw new ChannelIOException("Unable to add values", e);
		} finally {
			totalCount.addAndGet(collection.size());
		}
	}

	/**
	 * Poll an element from queue
	 * @return
	 * @throws ChannelIOException
	 */
	public T poll() throws ChannelIOException {
		T t = null;
		try {
			if ( ( ( t = this.channel.poll() ) != null) ) {
				return t;
			}
		} catch (Exception e) {
			LOGGER.error("Error polling element: {}", e.getMessage());
			throw new ChannelIOException("Error polling from Channel" + e.getMessage());
		}
		throw new ChannelIOException("Channel is Empty");
	}

	/**
	 * Retrieve Channel is empty
	 * @return Empty state
	 */
	public boolean isEmpty() {
		return this.channel.isEmpty();
	}

	/**
	 * Retrieve Channel processed elements since start
	 * @return Empty state
	 */
	public long getTotalCount() {
		return this.totalCount.get();
	}

	/**
	 * Clear Channel
	 */
	public void clear() {
		this.channel.clear();
	}

	/**
	 * Replace all elements of Channel with existing ones
	 * @param collection Collection of Element to replace in Channel
	 */
	public void retainAll(Collection<T> collection) {
		this.channel.retainAll(collection);
	}


	/**
	 * Start Channel Consume Thread
	 * @throws ChannelIOException Throw if there is no Consumer in Channel output operations
	 */
	public void start() throws ChannelIOException {
		thread.startConsuming();
	}
	
	/**
	 * Retrieve if Channel Consume Thread is Running
	 * @return Channel Consume Thread is Running
	 */
	public boolean running() {
		return thread.isRunning();
	}
	
	/**
	 * Stop Channel Consume Thread if running
	 */
	public void stop() {
		thread.stopConsuming();
		this.totalCount.set(0);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		if (thread!=null && thread.isRunning()) {
			thread.stopConsuming();
		}
		super.finalize();
		System.gc();
	}


	/**
	 * Create a channel
	 * @return Created Channel
	 */
	public static final <T> Channel<T> create() {
		return new Channel<>();
	}
	
	private static class ChannelThread<T> extends Thread {
		private Channel<T> channelInstance;
		
		private boolean running = false;
		
		/**
		 * Constructor
		 * @param channel running channel
		 */
		private ChannelThread(Channel<T> channel) {
			super();
			this.channelInstance = channel;
		}

		public synchronized void startConsuming() throws ChannelIOException {
			if (channelInstance.consumers.isEmpty()) {
				throw new ChannelIOException("No Consumer Defined");
			}
			if (! this.running) {
				this.running = true;
				super.start();
			}
		}

		public boolean isRunning() {
			return running;
		}

		public synchronized void stopConsuming() {
			if (! channelInstance.consumers.isEmpty() && running) {
				this.running = false;
				while (!channelInstance.channel.isEmpty()) {
					this.running = false;
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			while (running || !channelInstance.channel.isEmpty()) {
				if (channelInstance.channel.isEmpty()) {
					// Are 
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						running=false;
						try {
							// Trying to restart thread was interrupted for time machine issue ...
							this.channelInstance.start();
						} catch (ChannelIOException e1) {
							LOGGER.error("Couldn't restart thread: {}", e1.getMessage());
						}
					}
				}
				// Fetch asynchronously channel element stream and
				// Consume asynchronously element in the stream with
				// All registered consumers. If consumer are deregistered
				// during Channel run, all element from channel will be 
				// simply discarded!!
				channelInstance.channel.parallelStream()
				.forEach( n -> {
					channelInstance.consumers.parallelStream()
						.forEach( c -> c.consume(n) );
					channelInstance.channel.remove(n);
				});
			}
		}
		
	}
}
