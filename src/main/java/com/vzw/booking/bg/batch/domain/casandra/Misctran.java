/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.domain.casandra;

import com.datastax.driver.mapping.annotations.Table;

/**
 *
 * @author smorcja
 */
@Table(name = "misctran")
public class Misctran {
    private Integer miscfinancialtransactionnumber;
    private String miscfinancialtransactiondescription;
    private String billtypecode;
    private String companycode;

    public Integer getMiscfinancialtransactionnumber() {
        return miscfinancialtransactionnumber;
    }

    public void setMiscfinancialtransactionnumber(Integer miscfinancialtransactionnumber) {
        this.miscfinancialtransactionnumber = miscfinancialtransactionnumber;
    }

    public String getMiscfinancialtransactiondescription() {
        return miscfinancialtransactiondescription;
    }

    public void setMiscfinancialtransactiondescription(String miscfinancialtransactiondescription) {
        this.miscfinancialtransactiondescription = miscfinancialtransactiondescription;
    }

    public String getBilltypecode() {
        return billtypecode;
    }

    public void setBilltypecode(String billtypecode) {
        this.billtypecode = billtypecode;
    }

    public String getCompanycode() {
        return companycode;
    }

    public void setCompanycode(String companycode) {
        this.companycode = companycode;
    }

    @Override
    public String toString() {
        return "Misctran{" 
                + "miscfinancialtransactionnumber=" + miscfinancialtransactionnumber 
                + ", miscfinancialtransactiondescription=" + miscfinancialtransactiondescription 
                + ", billtypecode=" + billtypecode 
                + ", companycode=" + companycode + '}';
    }
}
