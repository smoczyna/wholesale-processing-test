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
public class AltBookingCsvFileDTO implements RawType<String>{
    private String sbid;
    private String altBookType;
    private String glMarketId;
    private String legalEntityId;
    private String glMktMapTyp;

    public String getSbid() {
        return sbid;
    }

    public void setSbid(String sbid) {
        this.sbid = sbid;
    }

    public String getAltBookType() {
        return altBookType;
    }

    public void setAltBookType(String altBookType) {
        this.altBookType = altBookType;
    }

    public String getGlMarketId() {
        return glMarketId;
    }

    public void setGlMarketId(String glMarketId) {
        this.glMarketId = glMarketId;
    }

    public String getLegalEntityId() {
        return legalEntityId;
    }

    public void setLegalEntityId(String legalEntityId) {
        this.legalEntityId = legalEntityId;
    }

    public String getGlMktMapTyp() {
        return glMktMapTyp;
    }

    public void setGlMktMapTyp(String glMktMapTyp) {
        this.glMktMapTyp = glMktMapTyp;
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
