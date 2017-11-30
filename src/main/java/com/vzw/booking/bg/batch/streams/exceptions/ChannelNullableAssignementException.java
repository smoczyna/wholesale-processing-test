/**
 * 
 */
package com.vzw.booking.bg.batch.streams.exceptions;

/**
 * @author Fabrizio Torelli &lt;hellgate75@gmail.com&gt;
 *
 */
public class ChannelNullableAssignementException extends Exception {

	/**
	 * Exception Serial Version ID
	 */
	private static final long serialVersionUID = 4433972168635873198L;

	/**
	 * Constructor
	 * @param message
	 */
	public ChannelNullableAssignementException(String message) {
		super(message);
	}

	/**
	 * Constructor
	 * @param cause
	 */
	public ChannelNullableAssignementException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor
	 * @param message
	 * @param cause
	 */
	public ChannelNullableAssignementException(String message, Throwable cause) {
		super(message, cause);
	}

}
