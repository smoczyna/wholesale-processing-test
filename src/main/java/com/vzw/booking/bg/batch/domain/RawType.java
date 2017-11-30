/**
 * 
 */
package com.vzw.booking.bg.batch.domain;

/**
 * Type that retain Original Raw Record Type behavior
 * @author TORELFA
 *
 */
public interface RawType<T> {

	/**
	 * Standard way to recover Row Type from Object instance
	 * @return
	 */
	T getRowType();

	/**
	 * Standard way to recover Row Type from Object instance line number in file
	 * @return
	 */
	long getLineNumber();
	
	/**
	 * Standard way to set Row Type to Object instance
	 * @param t Row Type value
	 */
	void setRowType(T t, long lineNumber);
	
}
