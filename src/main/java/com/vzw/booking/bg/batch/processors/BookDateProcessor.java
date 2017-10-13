/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.processors;

import com.vzw.booking.bg.batch.utils.WholesaleBookingProcessorHelper;
import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author smorcja
 */
public class BookDateProcessor implements ItemProcessor<BookDateCsvFileDTO, Boolean> {
    
    @Autowired
    WholesaleBookingProcessorHelper tempSubLedgerOuput;
    
    @Override
    public Boolean process(BookDateCsvFileDTO dates) throws Exception {
        this.tempSubLedgerOuput.setDates(dates);
        return true;
    }
}
