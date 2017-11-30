/**
 * 
 */
package com.vzw.booking.bg.batch.streams.exceptions;

/**
 * @author Fabrizio Torelli &lt;hellgate75@gmail.com&gt;
 *
 */
public class ChannelOperationNotPermittedException extends Exception {

	/**
	 * Exception Serial Version ID
	 */
	private static final long serialVersionUID = 181301037599078469L;

	/**
	 * Constructor
	 * @param message
	 */
	public ChannelOperationNotPermittedException(String message) {
		super(message);
	}

	/**
	 * Constructor
	 * @param cause
	 */
	public ChannelOperationNotPermittedException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor
	 * @param message
	 * @param cause
	 */
	public ChannelOperationNotPermittedException(String message, Throwable cause) {
		super(message, cause);
	}

}
