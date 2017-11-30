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
import com.vzw.booking.bg.batch.domain.AltBookingCsvFileDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
    private final Map<String, AltBookingCsvFileDTO> altBooking;
    private AtomicLong zeroChargesCounter;
    private AtomicLong gapsCounter;
    private AtomicLong dataErrorsCounter;
    private AtomicLong bypassCounter;
    private AtomicLong subledgerWriteCounter;
    private AtomicLong wholesaleReportCounter;
    private AtomicLong recordCount;

    public WholesaleBookingProcessorHelper() {
        this.financialEventOffset = new HashMap();
        this.altBooking = new HashMap();
        this.zeroChargesCounter=new AtomicLong(0L);
        this.gapsCounter=new AtomicLong(0L);
        this.dataErrorsCounter=new AtomicLong(0L);
        this.bypassCounter=new AtomicLong(0L);
        this.subledgerWriteCounter=new AtomicLong(0L);
        this.wholesaleReportCounter=new AtomicLong(0L);
        this.recordCount=new AtomicLong(0L);
    }

    public BookDateCsvFileDTO getDates() {
        return this.dates;
    }

    public void setDates(BookDateCsvFileDTO dates) {
        this.dates = dates;
    }

    public boolean addOffset(FinancialEventOffsetDTO offset) {
        this.financialEventOffset.put(offset.getFinancialEvent(), offset.getOffsetFinancialCategory());
        return true;
    }
    
    public Integer findOffsetFinCat(Integer finCat) {
        return this.financialEventOffset.get(finCat);
    }

    
    public void addAltBooking(AltBookingCsvFileDTO altBooking) {
        this.altBooking.put(altBooking.getSbid(), altBooking);
    }
    
    public AltBookingCsvFileDTO getAltBooking(String sbid) {
        return this.altBooking.get(sbid);
    }
    
    public SummarySubLedgerDTO addSubledger() {
        SummarySubLedgerDTO slRecord = new SummarySubLedgerDTO();
        if (this.dates != null) {
            slRecord.setReportStartDate(dates.getRptPerStartDate());
            slRecord.setJemsApplTransactioDate(dates.getTransPerEndDate());
        }
        this.subledgerWriteCounter.incrementAndGet();
        return slRecord;                
    }
    
    public AggregateWholesaleReportDTO addWholesaleReport() {
        AggregateWholesaleReportDTO report = new AggregateWholesaleReportDTO();
        this.wholesaleReportCounter.incrementAndGet();
        return report;
    }
    
    public void incrementCounter(String name) {
        switch (name) {
            case Constants.RECORD_COUNT:
                this.recordCount.incrementAndGet();
                break;
            case Constants.ZERO_CHARGES:
                this.zeroChargesCounter.incrementAndGet();
                break;
            case Constants.GAPS:
                this.gapsCounter.incrementAndGet();
                break;
            case Constants.DATA_ERRORS:
                this.dataErrorsCounter.incrementAndGet();
                break;
            case Constants.BYPASS:
                this.bypassCounter.incrementAndGet();
                break;
            case Constants.SUBLEDGER:
                this.subledgerWriteCounter.incrementAndGet();
                break;
            case Constants.WHOLESALES_REPORT:
                this.wholesaleReportCounter.incrementAndGet();
                break;
            default:
                break;
        }
    }

    public long getCounter(String name) {
        switch (name) {
            case Constants.RECORD_COUNT:
                return this.recordCount.get();
            case Constants.ZERO_CHARGES:
                return this.zeroChargesCounter.get();
            case Constants.GAPS:
                return this.gapsCounter.get();
            case Constants.DATA_ERRORS:
                return this.dataErrorsCounter.get();
            case Constants.BYPASS:
                return this.bypassCounter.get();
            case Constants.SUBLEDGER:
                return this.subledgerWriteCounter.get();
            case Constants.WHOLESALES_REPORT:
                return this.wholesaleReportCounter.get();
            default:
                return -1;
        }
    }

    public void clearCounters() {
        this.recordCount.set(0L);
        this.zeroChargesCounter.set(0L);
        this.gapsCounter.set(0L);
        this.dataErrorsCounter.set(0L);
        this.bypassCounter.set(0L);
        this.subledgerWriteCounter.set(0L);
        this.wholesaleReportCounter.set(0L);
    }
}
