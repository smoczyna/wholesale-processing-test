/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.domain;

/**
 *
 * @author smorcja
 */
public class FinancialEventOffsetDTO implements RawType<String> {
    private Integer financialEvent;
    private Integer offsetFinancialCategory;

    public Integer getFinancialEvent() {
        return financialEvent;
    }

    public void setFinancialEvent(Integer financialEvent) {
        this.financialEvent = financialEvent;
    }

    public Integer getOffsetFinancialCategory() {
        return offsetFinancialCategory;
    }

    public void setOffsetFinancialCategory(Integer offsetFinancialCategory) {
        this.offsetFinancialCategory = offsetFinancialCategory;
    }
    
    private volatile String rowValue="";
    private volatile long lineNumber=0;

	/* (non-Javadoc)
	 * @see com.vzw.booking.bg.batch.domain.RowType#getRowType()
	 */
	@Override
	public String getRowType() {
		// TODO Auto-generated method stub
		return rowValue;
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
	public void setRowType(String t,long lineNumber) {
		rowValue=t;
		this.lineNumber=lineNumber;
	}
}
