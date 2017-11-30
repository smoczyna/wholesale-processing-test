/**
 * 
 */
package com.vzw.booking.bg.batch.streams.model;

/**
 * Consumer behavior descriptor
 * @author Fabrizio Torelli &lt;hellgate75@gmail.com&gt;
 * 
 * @param <T> Consume Type
 */
public interface ChannelConsumer<T> {
	void consume(T t);
}
