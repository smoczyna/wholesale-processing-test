/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.processors;

import com.vzw.booking.bg.batch.config.CassandraQueryManager;
import com.vzw.booking.bg.batch.domain.AdminFeeCsvFileDTO;
import com.vzw.booking.bg.batch.domain.AggregateWholesaleReportDTO;
import com.vzw.booking.bg.batch.domain.BilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import com.vzw.booking.bg.batch.domain.casandra.FinancialEventCategory;
import com.vzw.booking.bg.batch.domain.casandra.FinancialMarket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import com.vzw.booking.bg.batch.domain.BaseBookingInputInterface;
import com.vzw.booking.bg.batch.domain.UnbilledCsvFileDTO;
import com.vzw.booking.bg.batch.domain.casandra.DataEvent;
import com.vzw.booking.bg.batch.domain.casandra.WholesalePrice;
import com.vzw.booking.bg.batch.domain.exceptions.CassandraQueryException;
import com.vzw.booking.bg.batch.domain.exceptions.MultipleRowsReturnedException;
import com.vzw.booking.bg.batch.domain.exceptions.NoResultsReturnedException;
import com.vzw.booking.bg.batch.utils.ProcessingUtils;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author smorcja
 * @param <T> - payload of the processor, 
 *              it must be a class implementing BaseBookingInputInterface or AdminFeeCsvFileDTO
 */
public class WholesaleReportProcessor<T> implements ItemProcessor<T, AggregateWholesaleReportDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WholesaleReportProcessor.class);
     
    @Autowired
    SubLedgerProcessor tempSubLedgerOuput;
    
    @Autowired
    CassandraQueryManager queryManager;

    String searchServingSbid;
    String searchHomeSbid;
    boolean homeEqualsServingSbid;
    String financialMarket;
    String fileSource;
    int tmpProdId;
    double tmpChargeAmt;
    final Set<Integer> PROD_IDS_TOLL = new HashSet(Arrays.asList(new Integer[]{95, 12872, 12873, 36201}));
    final Set<Integer> PROD_IDS = new HashSet(Arrays.asList(new Integer[]{95, 12872, 12873, 13537, 13538, 36201}));

    /**
     * this method is a working version of booking logic, it is exact representation of the spec and need to be tuned when finished
     * @param inRec
     * @return
     * @throws Exception 
     */
    @Override
    public AggregateWholesaleReportDTO process(T inRec) throws Exception {
        this.searchServingSbid = null;
        this.searchHomeSbid = null;
        this.financialMarket = "";
        this.homeEqualsServingSbid = false;
        this.fileSource = null;
        this.tmpProdId = 0;
        this.tmpChargeAmt = 0;

        if (inRec instanceof BilledCsvFileDTO) 
            return processBilledRecord((BilledCsvFileDTO) inRec);
        else if (inRec instanceof UnbilledCsvFileDTO)
            return processUnbilledRecord((UnbilledCsvFileDTO) inRec);
        else if (inRec instanceof AdminFeeCsvFileDTO)
            return processAdminFeesRecord((AdminFeeCsvFileDTO) inRec); 
        else
            return null;
    }

    private void createSubLedgerRecord(Double tmpChargeAmt, FinancialEventCategory financialEventCategory, String financialMarket) {
        if (tmpChargeAmt==null || financialEventCategory==null || financialMarket==null)
            return;
        
        SummarySubLedgerDTO subLedgerOutput = this.tempSubLedgerOuput.addSubledger();
        
        if (financialEventCategory.getFinancialeventnormalsign().equals("DR")) {
            if (financialEventCategory.getDebitcreditindicator().equals("DR")) {
                if (financialEventCategory.getBillingaccrualindicator().equals("Y")) {
                    if (tmpChargeAmt > 0) {
                        subLedgerOutput.setSubledgerTotalDebitAmount(tmpChargeAmt);
                        subLedgerOutput.setSubledgerTotalCreditAmount(0d);
                    } else {
                        subLedgerOutput.setSubledgerTotalCreditAmount(tmpChargeAmt);
                        subLedgerOutput.setSubledgerTotalDebitAmount(0d);
                    }
                } else if (financialEventCategory.getBillingaccrualindicator().equals("N")) {
                    if (tmpChargeAmt > 0) {
                        subLedgerOutput.setSubledgerTotalCreditAmount(tmpChargeAmt);
                        subLedgerOutput.setSubledgerTotalDebitAmount(0d);
                    } else {
                        subLedgerOutput.setSubledgerTotalDebitAmount(tmpChargeAmt);
                        subLedgerOutput.setSubledgerTotalDebitAmount(0d);
                    }
                }
            }
        }
        else if (financialEventCategory.getFinancialeventnormalsign().equals("CR")) {
            if (financialEventCategory.getDebitcreditindicator().equals("CR")) {
                if (tmpChargeAmt > 0) {
                    subLedgerOutput.setSubledgerTotalCreditAmount(tmpChargeAmt);
                    subLedgerOutput.setSubledgerTotalDebitAmount(0d);
                } else {
                    subLedgerOutput.setSubledgerTotalDebitAmount(tmpChargeAmt);
                    subLedgerOutput.setSubledgerTotalCreditAmount(0d);
                }
            }
            else if (financialEventCategory.getDebitcreditindicator().equals("DR")) {
                if (tmpChargeAmt > 0) {
                    subLedgerOutput.setSubledgerTotalDebitAmount(tmpChargeAmt);
                    subLedgerOutput.setSubledgerTotalCreditAmount(0d);
                } else {
                    subLedgerOutput.setSubledgerTotalCreditAmount(tmpChargeAmt);
                    subLedgerOutput.setSubledgerTotalDebitAmount(0d);
                }
            }
        }
        
        subLedgerOutput.setFinancialEventNumber(financialEventCategory.getFinancialeventnumber());
        subLedgerOutput.setFinancialCategory(financialEventCategory.getFinancialcategory());
        subLedgerOutput.setFinancialmarketId(financialMarket);
        subLedgerOutput.setBillCycleMonthYear(ProcessingUtils.getYearAndMonthFromStrDate(this.tempSubLedgerOuput.getDates().getRptPerEndDate()));
        subLedgerOutput.setBillAccrualIndicator(financialEventCategory.getBillingaccrualindicator());
        this.tempSubLedgerOuput.incrementCounter("sub");
        this.createOffsetBooking(subLedgerOutput);
    }

    private void createOffsetBooking(SummarySubLedgerDTO subLedgerOutput) {
        Integer offsetFinCat = this.tempSubLedgerOuput.findOffsetFinCat(subLedgerOutput.getFinancialEventNumber());
        if (offsetFinCat==null)
            LOGGER.error("Offset fin cat value not found !!!");
        else {            
            Double debitAmt = subLedgerOutput.getSubledgerTotalDebitAmount();
            Double creditAmt = subLedgerOutput.getSubledgerTotalCreditAmount();
            
            if (debitAmt==0d && creditAmt==0d)
                LOGGER.info("Zero amounts in sub ledger record found !!!");
            
            SummarySubLedgerDTO clone = null;
            try {
                clone = subLedgerOutput.clone();
            } catch (CloneNotSupportedException ex) {
                LOGGER.error("Failed to create offset booking !!!");
            } 
            clone.setFinancialCategory(offsetFinCat);
            clone.setSubledgerTotalDebitAmount(creditAmt);
            clone.setSubledgerTotalCreditAmount(debitAmt);
            this.tempSubLedgerOuput.addOffsetSubledger(clone);
            this.tempSubLedgerOuput.incrementCounter("sub");
        }            
    }

    private boolean isAlternateBookingApplicable(BaseBookingInputInterface inRec) {
        boolean altBookingInd = false;
        String homeGlMarketId = " ";
        String servingGlMarketId;
        
        searchHomeSbid = inRec.getHomeSbid();
        if (inRec.getServingSbid().trim().isEmpty()) {
            searchServingSbid = searchHomeSbid;
        } else {
            searchServingSbid = inRec.getServingSbid();
        }
        if (searchHomeSbid.equals(searchServingSbid)) {
            homeEqualsServingSbid = true;
        }

        FinancialMarket finMarket = this.getFinancialMarketFromDb(this.financialMarket);
                
        String homeLegalEntityId = " ";
        String servingLegalEntityId;

        if (finMarket.getSidbid().equals(searchHomeSbid) && finMarket.getAlternatebookingtype().equals("P")) {
            homeLegalEntityId = finMarket.getGllegalentityid();
            if (homeEqualsServingSbid) {
                altBookingInd = true;
            }            
        }

        if (!homeEqualsServingSbid && (finMarket.getSidbid().equals(searchServingSbid) && finMarket.getAlternatebookingtype().equals("P"))) {
            servingLegalEntityId = finMarket.getGllegalentityid();
            if (homeLegalEntityId.equals(servingLegalEntityId)) {
                altBookingInd = true;
            }
        }

        if (finMarket.getSidbid().equals(searchHomeSbid) && finMarket.getAlternatebookingtype().equals("M")) {
            homeGlMarketId = finMarket.getGlmarketid();
            if (homeEqualsServingSbid) {
                altBookingInd = true;
            }
        }

        if (!homeEqualsServingSbid && (finMarket.getSidbid().equals(searchServingSbid) && finMarket.getAlternatebookingtype().equals("M"))) {
            servingGlMarketId = finMarket.getGlmarketid();
            if (homeGlMarketId.equals(servingGlMarketId)) {
                altBookingInd = true;
            }
        }
        altBookingInd = false; // temporary fix, will be reviewed later
        return altBookingInd;
    }
    
    private boolean bypassBooking(FinancialEventCategory financialEventCategory, boolean altBookingInd) {        
        boolean bypassBooking = false;
        
        // bypass booking check stage 1
        if (!financialEventCategory.getBamsaffiliateindicator().equals("N") || !financialEventCategory.getCompanycode().trim().isEmpty()) {
            bypassBooking = true;
        }
        
        // bypass booking check stage 2
        if (this.fileSource.equals("M") && financialEventCategory.getHomesidequalsservingsidindicator().trim().isEmpty())
            bypassBooking = false;
               
        if (this.fileSource.equals("U") && financialEventCategory.getBillingaccrualindicator().equals("Y"))
            bypassBooking = false;

        if (this.fileSource.equals("B")) {
            if (financialEventCategory.getHomesidequalsservingsidindicator().equals("Y") && searchHomeSbid.equals(searchServingSbid))
                bypassBooking = false;
            
            if (financialEventCategory.getHomesidequalsservingsidindicator().equals("N") && !searchHomeSbid.equals(searchServingSbid)) {
                if ((financialEventCategory.getAlternatebookingindicator().equals("N") && !altBookingInd)
                     || (financialEventCategory.getAlternatebookingindicator().equals("Y") && altBookingInd))
                    bypassBooking = false;
            }                         
        }
        
        // bypass booking check stage 3
        if (bypassBooking && (searchHomeSbid.equals(searchServingSbid))) {
            if (!financialEventCategory.getForeignservedindicator().trim().isEmpty() || financialEventCategory.getForeignservedindicator().equals("N"))
                bypassBooking = true;
        }
        
        return bypassBooking;
    }
    
    private AggregateWholesaleReportDTO processBilledRecord(BilledCsvFileDTO billedRec) {
        AggregateWholesaleReportDTO outRec = new AggregateWholesaleReportDTO();
        int tmpInterExchangeCarrierCode = 0;
        boolean bypassBooking;
        boolean altBookingInd;
        boolean zeroAirCharge = false;
        boolean zeroTollCharge = false;
        
        outRec.setBilledInd("Y");
        this.fileSource = "B";

        if (billedRec.getFinancialMarket().trim().isEmpty())
            this.financialMarket = "HUB";
        else
            this.financialMarket = billedRec.getFinancialMarket();
        
        if (billedRec.getWholesalePeakAirCharge()==0 && billedRec.getWholesaleOffpeakAirCharge()==0 && billedRec.getTollCharge()==0) {
            LOGGER.info("Record skipped due to zero charges !!!");
            this.tempSubLedgerOuput.incrementCounter("zero");
            return null;
        }
        
        if (billedRec.getAirProdId() > 0 && (billedRec.getWholesalePeakAirCharge() > 0 || billedRec.getWholesaleOffpeakAirCharge() > 0)) {
            outRec.setPeakDollarAmt(billedRec.getWholesalePeakAirCharge());
            outRec.setOffpeakDollarAmt(billedRec.getWholesaleOffpeakAirCharge());
            outRec.setDollarAmtOther(0d);
            outRec.setVoiceMinutes(billedRec.getAirBillSeconds() * 60);
            tmpChargeAmt = billedRec.getWholesalePeakAirCharge() + billedRec.getWholesaleOffpeakAirCharge();
            if (billedRec.getAirProdId().equals(190))
                this.tmpProdId = 1;
            else
                this.tmpProdId = billedRec.getAirProdId();
            
            outRec.setPeakDollarAmt(0d);
            this.makeBookings(billedRec, tmpInterExchangeCarrierCode);
        } else {    
            zeroAirCharge = true;
        }
        
        if (billedRec.getTollProductId() > 0 && billedRec.getTollCharge() > 0) {
            if (billedRec.getInterExchangeCarrierCode().equals(5050)
                || billedRec.getIncompleteInd().equals("D")
                || !billedRec.getHomeSbid().equals(billedRec.getServingSbid())
                || (billedRec.getAirProdId().equals(190) && billedRec.getWholesaleTollChargeLDPeak() > 0 && billedRec.getWholesaleTollChargeLDOther() > 0)) {
                
                if (billedRec.getAirProdId().equals(190)) {
                    this.tmpProdId = 95;
                } else {
                    this.tmpProdId = billedRec.getTollProductId();
                }
                if (billedRec.getInterExchangeCarrierCode().equals(5050)) {
                    this.tmpChargeAmt = billedRec.getTollCharge();
                    outRec.setDollarAmtOther(this.tmpChargeAmt);
                }
                else if (billedRec.getIncompleteInd().equals("D")) {
                    this.tmpChargeAmt = billedRec.getTollCharge();
                    outRec.setDollarAmtOther(this.tmpChargeAmt);

                    DataEvent dataEvent = this.getDataEventFromDb(this.tmpProdId);
                   
                    /* compute data usage */
                    if (dataEvent.getDataeventsubtype().equals("DEFLT")) {
                        outRec.setDollarAmt3G(this.tmpChargeAmt);
                        outRec.setUsage3G(Math.round(billedRec.getWholesaleUsageBytes().doubleValue() / 1024));
                    }    
                    else if (dataEvent.getDataeventsubtype().equals("DEF4G")) {
                        outRec.setDollarAmt4G(this.tmpChargeAmt);
                        outRec.setUsage4G(Math.round(billedRec.getWholesaleUsageBytes().doubleValue() / 1024));
                    }
                } else {
                    tmpChargeAmt = billedRec.getWholesaleTollChargeLDPeak() + billedRec.getWholesaleTollChargeLDOther();
                    outRec.setTollDollarsAmt(this.tmpChargeAmt);
                    outRec.setTollMinutes(this.tmpProdId);
                    outRec.setTollMinutes(Math.round(billedRec.getTollBillSeconds() / 60));
                }
                if (!billedRec.getInterExchangeCarrierCode().equals(5050)) 
                    tmpInterExchangeCarrierCode = 0;
                else
                    tmpInterExchangeCarrierCode = billedRec.getInterExchangeCarrierCode();
                
                outRec.setPeakDollarAmt(0d);
                this.makeBookings(billedRec, tmpInterExchangeCarrierCode);
            }
            else {
                LOGGER.info("Gap in the code encountered !!!");
                this.tempSubLedgerOuput.incrementCounter("gap");
                return null;
            }
        } else {
            zeroTollCharge = true;
        }
        if (zeroAirCharge && zeroTollCharge) {
            LOGGER.info("Invalid input record encountered !!!");
            this.tempSubLedgerOuput.incrementCounter("error");
            return null;
        }
        
        return outRec;
    }
    
    private void makeBookings(BilledCsvFileDTO billedRec, int iecCode) {
        boolean altBookingInd = this.isAlternateBookingApplicable(billedRec);
        FinancialEventCategory financialEventCategory = this.getEventCategoryFromDb(this.tmpProdId, this.homeEqualsServingSbid ? "Y" : "N", altBookingInd, iecCode);
        boolean bypassBooking = this.bypassBooking(financialEventCategory, altBookingInd);

        if (bypassBooking) {
            LOGGER.warn("Booking bypass detected, record skipped for sub ledger file ...");
            this.tempSubLedgerOuput.incrementCounter("bypass");
        } else {
            this.createSubLedgerRecord(tmpChargeAmt, financialEventCategory, financialMarket);
        }
    }
    
    private AggregateWholesaleReportDTO processUnbilledRecord(UnbilledCsvFileDTO unbilledRec) {        
        if (unbilledRec.getAirProdId() > 0 && (unbilledRec.getWholesalePeakAirCharge() > 0 || unbilledRec.getWholesaleOffpeakAirCharge() > 0)) {
            AggregateWholesaleReportDTO outRec = new AggregateWholesaleReportDTO();
            boolean altBookingInd;
            outRec.setBilledInd("N");
            this.fileSource = "U";
            financialMarket = unbilledRec.getFinancialMarket();
            
            this.searchHomeSbid = unbilledRec.getHomeSbid();
            if (unbilledRec.getServingSbid().trim().isEmpty())
                this.searchServingSbid = unbilledRec.getHomeSbid();
            else
                this.searchServingSbid = unbilledRec.getServingSbid();

            if (this.searchHomeSbid.equals(this.searchServingSbid))
                this.homeEqualsServingSbid = true;

            if (unbilledRec.getAirProdId().equals(190))
                this.tmpProdId = 1;
            else
                this.tmpProdId = unbilledRec.getAirProdId();
            
            this.tmpChargeAmt = unbilledRec.getWholesalePeakAirCharge() + unbilledRec.getWholesaleOffpeakAirCharge();
            if (unbilledRec.getMessageSource().trim().isEmpty()) {
                outRec.setPeakDollarAmt(unbilledRec.getWholesalePeakAirCharge());
                outRec.setDollarAmtOther(0d);
                outRec.setVoiceMinutes(Math.round(unbilledRec.getAirBillSeconds() / 60));
            }
            else if (unbilledRec.getMessageSource().equals("D")) {
                outRec.setDollarAmtOther(unbilledRec.getWholesalePeakAirCharge());
                outRec.setPeakDollarAmt(0d);
            }
            outRec.setOffpeakDollarAmt(unbilledRec.getWholesaleOffpeakAirCharge());
            
            if (unbilledRec.getSource().equals("D")) {            
                DataEvent dataEvent = this.getDataEventFromDb(this.tmpProdId);

                if (dataEvent.getDataeventsubtype().equals("DEFLT")) {
                    outRec.setDollarAmt3G(this.tmpChargeAmt);
                    outRec.setUsage3G(Math.round(unbilledRec.getTotalWholesaleUsage().doubleValue() / 1024));
                }    
                else if (dataEvent.getDataeventsubtype().equals("DEF4G")) {
                    outRec.setDollarAmt4G(this.tmpChargeAmt);
                    outRec.setUsage4G(Math.round(unbilledRec.getTotalWholesaleUsage().doubleValue() / 1024));
                }
            }
            altBookingInd = this.isAlternateBookingApplicable(unbilledRec);
            FinancialEventCategory financialEventCategory = this.getEventCategoryFromDb(this.tmpProdId, this.homeEqualsServingSbid ? "Y" : "N", altBookingInd, 0); 
            this.createSubLedgerRecord(tmpChargeAmt, financialEventCategory, financialMarket);            
            return outRec;
        }
        else
            return null;
    }
    
    private AggregateWholesaleReportDTO processAdminFeesRecord(AdminFeeCsvFileDTO adminFeesRec) {
        AggregateWholesaleReportDTO outRec = new AggregateWholesaleReportDTO();
        outRec.setBilledInd("Y");
        this.fileSource = "M";
        this.searchHomeSbid = adminFeesRec.getSbid(); // there is no check if both home and serving bids are equal ???
        this.tmpProdId = adminFeesRec.getProductId();
        this.financialMarket = adminFeesRec.getFinancialMarket();
        
        // call cassandra wholesale price table
        WholesalePrice wholesalePrice = this.getWholesalePriceFromDb(this.tmpProdId, this.searchHomeSbid);
        
        this.tmpChargeAmt = wholesalePrice.getProductwholesaleprice().multiply(BigDecimal.valueOf(adminFeesRec.getAdminCount()).movePointLeft(2)).floatValue();
        outRec.setDollarAmtOther(this.tmpChargeAmt);
        
        boolean altBookingInd = false; // alternate booking cannot be checked here due incompatible payload (adminfees file doesn't fit the interface as it has no all required fields)
        FinancialEventCategory financialEventCategory = this.getEventCategoryFromDb(this.tmpProdId, " ", altBookingInd, 0);
                
        // this is pointless - it's false by default
        //if (financialEventCategory.getHomesidequalsservingsidindicator().trim().isEmpty())
        //    bypassBooking = false;
        //else
              
        boolean bypassBooking = this.bypassBooking(financialEventCategory, altBookingInd);
        
        if (bypassBooking)
            LOGGER.warn("Booking bypass detected, record skipped for sub ledger file ...");
        else
            this.createSubLedgerRecord(tmpChargeAmt, financialEventCategory, financialMarket);
        
        return outRec;
    }
    
    protected FinancialMarket getFinancialMarketFromDb(String financialMarketId) {
        FinancialMarket result = null;
        try {
            List<FinancialMarket> dbResult = queryManager.getFinancialMarketRecord(financialMarketId);
            if (dbResult.size()==1)
                result = dbResult.get(0);
        } catch (MultipleRowsReturnedException | NoResultsReturnedException | CassandraQueryException ex) {
            LOGGER.error(ex.getLocalizedMessage());
        }
        return result;
    }
    
    protected FinancialEventCategory getEventCategoryFromDb(Integer tmpProdId, String homeEqualsServingSbid, boolean altBookingInd, int interExchangeCarrierCode) {
        FinancialEventCategory result = null;
        try {
            List<FinancialEventCategory> dbResult = queryManager.getFinancialEventCategoryNoClusteringRecord(
                    tmpProdId, homeEqualsServingSbid, altBookingInd ? "Y" : "N", interExchangeCarrierCode);
            if (dbResult.size()==1)
                result = dbResult.get(0);
        } catch (MultipleRowsReturnedException | NoResultsReturnedException | CassandraQueryException ex) {
            LOGGER.error(ex.getLocalizedMessage());
        }
        return result;
    }
    
    protected DataEvent getDataEventFromDb(Integer productId) {
        DataEvent result = null;
        try {
            List<DataEvent> dbResult = queryManager.getDataEventRecords(productId);
            if (dbResult.size()==1)
                result = dbResult.get(0);
        } catch (MultipleRowsReturnedException | NoResultsReturnedException | CassandraQueryException ex) {
            LOGGER.error(ex.getLocalizedMessage());
        }
        return result;
    }
    
    protected WholesalePrice getWholesalePriceFromDb(Integer tmpProdId, String searchHomeSbid) {
        WholesalePrice result = null;
        try {
            List<WholesalePrice> dbResult = queryManager.getWholesalePriceRecords(tmpProdId, searchHomeSbid);
            if (dbResult.size()==1)
                result = dbResult.get(0);
        } catch (MultipleRowsReturnedException | NoResultsReturnedException | CassandraQueryException ex) {
            LOGGER.error(ex.getLocalizedMessage());
        }
        return result;
    }
}
