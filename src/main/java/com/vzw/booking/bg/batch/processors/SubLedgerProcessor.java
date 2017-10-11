/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.processors;

import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.FinancialEventOffsetDTO;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * this class does input record classification and consolidation it retrieve or
 * creates output record and populates it with all static data which are
 * available at the moment
 *
 * @author smorcja
 */
@Component
public class SubLedgerProcessor {

    private Set<SummarySubLedgerDTO> aggregatedOutput;
    private BookDateCsvFileDTO dates;
    private final Map<Integer, Integer> financialEventOffset;
    private int zeroChargesCounter;
    private int gapsCounter;
    private int dataErrorsCounter;
    private int bypassCounter;
    private int subledgerWriteCounter;

    public SubLedgerProcessor() {
        this.aggregatedOutput = new HashSet();
        this.financialEventOffset = new HashMap();
        this.zeroChargesCounter = 0;
        this.gapsCounter = 0;
        this.dataErrorsCounter = 0;
        this.bypassCounter = 0;
        this.subledgerWriteCounter = 0;
    }

    public Set<SummarySubLedgerDTO> getAggregatedOutput() {
        return aggregatedOutput;
    }

    public void setAggregatedOutput(Set<SummarySubLedgerDTO> aggregatedOutput) {
        this.aggregatedOutput = aggregatedOutput;
    }

    public SummarySubLedgerDTO addSubledger() {
        SummarySubLedgerDTO slRecord = new SummarySubLedgerDTO();
        if (this.dates != null) {
            slRecord.setReportStartDate(this.dates.getRptPerStartDate());
            slRecord.setJemsApplTransactioDate(this.dates.getTransPerEndDate());
        }
        if (this.aggregatedOutput.add(slRecord)) {
            return slRecord;
        } else {
            return null;
        }
    }

    public SummarySubLedgerDTO addOffsetSubledger(SummarySubLedgerDTO subledger) {
        if (this.aggregatedOutput.add(subledger)) {
            return subledger;
        } else {
            return null;
        }
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

    public void incrementCounter(String name) {
        switch (name) {
            case "zero":
                this.zeroChargesCounter++;
                break;
            case "gap":
                this.gapsCounter++;
                break;
            case "error":
                this.dataErrorsCounter++;
                break;
            case "bypass":
                this.bypassCounter++;
                break;
            case "sub":
                this.subledgerWriteCounter++;
                break;
            default:
                break;
        }
    }

    public int getCounter(String name) {
        switch (name) {
            case "zero":
                return this.zeroChargesCounter;
            case "gap":
                return this.gapsCounter;
            case "error":
                return this.dataErrorsCounter;
            case "bypass":
                return this.bypassCounter;
            case "sub":
                return this.subledgerWriteCounter;
            default:
                return -1;
        }
    }

    public void clearCounters() {
        this.zeroChargesCounter = 0;
        this.gapsCounter = 0;
        this.dataErrorsCounter = 0;
        this.bypassCounter = 0;
        this.subledgerWriteCounter=0;
    }
}
