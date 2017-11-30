/**
 * 
 */
package com.vzw.booking.bg.batch.streams.exceptions;

/**
 * @author Fabrizio Torelli &lt;hellgate75@gmail.com&gt;
 *
 */
public class ChannelIOException extends Exception {

	/**
	 * Exception Serial Version ID
	 */
	private static final long serialVersionUID = 6747025107409224604L;

	/**
	 * Constructor
	 * @param message
	 */
	public ChannelIOException(String message) {
		super(message);
	}

	/**
	 * Constructor
	 * @param cause
	 */
	public ChannelIOException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor
	 * @param message
	 * @param cause
	 */
	public ChannelIOException(String message, Throwable cause) {
		super(message, cause);
	}

}
