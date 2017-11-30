/**
 * 
 */
package com.vzw.booking.bg.batch.streams.exceptions;

/**
 * @author Fabrizio Torelli &lt;hellgate75@gmail.com&gt;
 *
 */
public class DuplicatedObjectException extends Exception {

	/**
	 * Exception Serial Version ID
	 */
	private static final long serialVersionUID = 8731299673833514267L;

	/**
	 * Constructor
	 * @param message
	 */
	public DuplicatedObjectException(String message) {
		super(message);
	}

	/**
	 * Constructor
	 * @param cause
	 */
	public DuplicatedObjectException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor
	 * @param message
	 * @param cause
	 */
	public DuplicatedObjectException(String message, Throwable cause) {
		super(message, cause);
	}

}
