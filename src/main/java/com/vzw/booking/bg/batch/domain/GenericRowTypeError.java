/**
 * 
 */
package com.vzw.booking.bg.batch.domain;

import com.vzw.booking.bg.batch.constants.ErrorSource;
import com.vzw.booking.bg.batch.constants.ErrorType;

/**
 * DEfine Generic Output Element
 * @author TORELFA
 *
 */
public class GenericRowTypeError implements RawType<String> {

	private String rowString;
    private long lineNumber=0;
	private ErrorType errorType;
	private ErrorSource errorSource;
	
	/**
	 * Constructor
	 */
	public GenericRowTypeError(String rowString, ErrorType errorType, ErrorSource errorSource) {
		super();
		this.rowString=rowString;
		this.errorType=errorType;
		this.errorSource=errorSource;
	}

	/* (non-Javadoc)
	 * @see com.vzw.booking.bg.batch.domain.RowType#getRowType()
	 */
	@Override
	public String getRowType() {
		return rowString;
	}

	/* (non-Javadoc)
	 * @see com.vzw.booking.bg.batch.domain.RawType#getLineNumber()
	 */
	@Override
	public long getLineNumber() {
		return lineNumber;
	}

	/* (non-Javadoc)
	 * @see com.vzw.booking.bg.batch.domain.RowType#serRowType(java.lang.Object)
	 */
	@Override
	public void setRowType(String t, long lineNumber) {
		rowString=t;
		this.lineNumber = lineNumber;
	}

	/**
	 * Error Type Class
	 * @return the errorType
	 */
	public final ErrorType getErrorType() {
		return errorType;
	}

	/**
	 * Error Source Class
	 * @return the errorSource
	 */
	public final ErrorSource getErrorSource() {
		return errorSource;
	}

}
