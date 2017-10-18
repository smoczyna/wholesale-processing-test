/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.readers;

import com.vzw.booking.bg.batch.domain.UnbilledCsvFileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 *
 * @author smorcja
 */
public class UnbilledBookingFileReader extends CsvFileGenericReader<UnbilledCsvFileDTO> {
    private static final String[] COLUMN_NAMES = new String[] {
        "homeSbid",
        "servingSbid",
        "messageSource",
        "airProdId",
        "wholesalePeakAirCharge",
        "wholesaleOffpeakAirCharge",
        "source",
        "financialMarket",
        "airBillSeconds",
        "totalWholesaleUsage",
        "debitcreditindicator"};
    
    @Autowired
    public UnbilledBookingFileReader(Environment environment, String filename) {
        super(UnbilledCsvFileDTO.class, environment, filename, COLUMN_NAMES, ",");
    }
    
    public UnbilledBookingFileReader(String filePath, String delimiter) {
        super(UnbilledCsvFileDTO.class, filePath, COLUMN_NAMES, delimiter, 0);
    }
}
