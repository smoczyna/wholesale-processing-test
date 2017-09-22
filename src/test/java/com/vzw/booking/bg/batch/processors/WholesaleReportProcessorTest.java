/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.processors;

import com.vzw.booking.bg.batch.domain.AggregateWholesaleReportDTO;
import com.vzw.booking.bg.batch.domain.BilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.BookDateCsvFileDTO;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import java.util.Date;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author smoczyna
 */
@RunWith(SpringRunner.class)
public class WholesaleReportProcessorTest {

    @Mock
    private SubLedgerProcessor tempSubLedgerOuput;

    @InjectMocks
    private final WholesaleReportProcessor wholesaleBookingProcessor = new WholesaleReportProcessor();
    
    BilledCsvFileDTO billedBookingRecord;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SummarySubLedgerDTO subLedgerRecord = new SummarySubLedgerDTO();
        BookDateCsvFileDTO bookDates = new BookDateCsvFileDTO();
        bookDates.setRptPerStartDate("08/01/2017");
        bookDates.setRptPerEndDate("08/31/2017");
        bookDates.setTransPerStartDate("07/26/2017");
        bookDates.setTransPerEndDate("08/25/2017");
        when(tempSubLedgerOuput.add()).thenReturn(subLedgerRecord);
        when(tempSubLedgerOuput.getDates()).thenReturn(bookDates);        
        billedBookingRecord = createInputRecord();        
    }

    private BilledCsvFileDTO createInputRecord() {
        BilledCsvFileDTO record = new BilledCsvFileDTO();
        record.setAirBillSeconds(1235);
        record.setAirProdId(1);
        record.setAirSurcharge(123.45d);
        record.setAirSurchargeProductId(1);
        record.setDeviceType("mobile");
        record.setFinancialMarket("Ireland");
        record.setHomeSbid("dublin");
        record.setIncompleteCallSurcharge(45.67d);
        record.setIncompleteInd("2");
        record.setIncompleteProdId(1);
        record.setInterExchangeCarrierCode(1);
        record.setLocalAirTax(12.23d);
        record.setMessageSource("B");
        record.setServingSbid("dublin");
        record.setSpace("space");
        record.setStateAirTax(34.45d);
        record.setTollBillSeconds(123);
        record.setTollProductId(1);
        record.setTollCharge(465.76d);
        record.setTollLocalTax(11.23d);
        record.setTollSurcharge(876.23d);
        record.setTollSurchargeProductId(2);
        record.setWholesaleOffpeakAirCharge(2345.67d);
        record.setWholesalePeakAirCharge(3456.78d);
        record.setWholesaleTollChargeLDOther(345.65d);
        record.setWholesaleTollChargeLDPeak(765.34d);
        record.setWholesaleUsageBytes(34567l);
        return record;
    }
    
    /**
     * Test of process method, of class WholesaleReportProcessor.
     * @throws java.lang.Exception
     */
    @Test
    public void testProcess() throws Exception {
        AggregateWholesaleReportDTO result = wholesaleBookingProcessor.process(billedBookingRecord);        
        verify(tempSubLedgerOuput, times(1)).add();
        assertNotNull(result);
        //assertTrue(tempSubLedgerOuput.getAggregatedOutput().size()>0);
    }

}
