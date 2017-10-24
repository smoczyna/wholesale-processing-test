/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.utils;

import com.vzw.booking.bg.batch.constants.Constants;
import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.FinancialEventOffsetDTO;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import com.vzw.booking.bg.batch.domain.AggregateWholesaleReportDTO;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * this class does input record classification and consolidation it retrieve or
 * creates output record and populates it with all static data which are
 * available at the moment
 *
 * @author smorcja
 */
@Component
public class WholesaleBookingProcessorHelper {

    private BookDateCsvFileDTO dates;
    private final Map<Integer, Integer> financialEventOffset;
    
    private long subledgerWriteCounter;
    private long wholesaleReportCounter;
    private long maxSkippedRecords;
    private int numberOfChunks;

    public WholesaleBookingProcessorHelper() {
        this.financialEventOffset = new HashMap();
        this.subledgerWriteCounter = 0;
        this.wholesaleReportCounter = 0;
        this.maxSkippedRecords = 0;
    }

    public BookDateCsvFileDTO getDates() {
        return this.dates;
    }

    public void setDates(BookDateCsvFileDTO dates) {
        this.dates = dates;
    }

    public long getMaxSkippedRecords() {
        return this.maxSkippedRecords==0 ? Constants.DEFAULT_MAX_SKIPPED_RECORDS : this.maxSkippedRecords;
    }

    public void setMaxSkippedRecords(long maxSkippedRecords) {
        this.maxSkippedRecords = maxSkippedRecords>0 ? maxSkippedRecords : Constants.DEFAULT_MAX_SKIPPED_RECORDS;
    }

    public int getNumberOfChunks() {
        return this.numberOfChunks==0 ? Constants.DEFAULT_NUMBER_OF_CHUNKS : this.numberOfChunks;
    }

    public void setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks>0 ? numberOfChunks : Constants.DEFAULT_NUMBER_OF_CHUNKS;
    }

    public boolean addOffset(FinancialEventOffsetDTO offset) {
        this.financialEventOffset.put(offset.getFinancialEvent(), offset.getOffsetFinancialCategory());
        return true;
    }

    public Integer findOffsetFinCat(Integer finCat) {
        return this.financialEventOffset.get(finCat);
    }

    public SummarySubLedgerDTO addSubledger() {
        SummarySubLedgerDTO slRecord = new SummarySubLedgerDTO();
        if (this.dates != null) {
            slRecord.setReportStartDate(dates.getRptPerStartDate());
            slRecord.setJemsApplTransactioDate(dates.getTransPerEndDate());
        }
        this.subledgerWriteCounter++;
        return slRecord;                
    }
    
    public AggregateWholesaleReportDTO addWholesaleReport() {
        AggregateWholesaleReportDTO report = new AggregateWholesaleReportDTO();
        this.wholesaleReportCounter++;
        return report;
    }
    
    public long getCounter(String name) {
        switch (name) {
            case Constants.SUBLEDGER:
                return this.subledgerWriteCounter;
            case Constants.WHOLESALES_REPORT:
                return this.wholesaleReportCounter;
            default:
                return -1;
        }
    }

    public void clearCounters() {
        this.subledgerWriteCounter = 0;
        this.wholesaleReportCounter = 0;
    }
}
