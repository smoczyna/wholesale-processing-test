/**
 * 
 */
package com.vzw.booking.bg.batch.streams.exceptions;

/**
 * @author Fabrizio Torelli &lt;hellgate75@gmail.com&gt;
 *
 */
public class ChannelInstanceException extends Exception {

	/**
	 * Exception Serial Version ID
	 */
	private static final long serialVersionUID = 6744378336188957181L;

	/**
	 * Constructor
	 * @param message
	 */
	public ChannelInstanceException(String message) {
		super(message);
	}

	/**
	 * Constructor
	 * @param cause
	 */
	public ChannelInstanceException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor
	 * @param message
	 * @param cause
	 */
	public ChannelInstanceException(String message, Throwable cause) {
		super(message, cause);
	}

}
