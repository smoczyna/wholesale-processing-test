/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.processors;

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
import com.vzw.booking.bg.batch.utils.ProcessingUtils;

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

    private void createSubLedgerRecord(double tmpChargeAmt, FinancialEventCategory financialEventCategory, String financialMarket) {
        SummarySubLedgerDTO subLedgerOutput = this.tempSubLedgerOuput.addSubledger();
        
        // second cassandra call goes here, it will check if product is wholesale product        
        String wholesaleBillingCode = null; // this object comes from db as response 
        //if (wholesaleBillingCode != null) it is
        //else it is not
        // what is that for ??? It's never used anywhere after 
        
        if (financialEventCategory.getFinancialeventnormalsign().equals("DR")) {
            if (financialEventCategory.getDebitcreditindicator().equals("DR")) {
                if (financialEventCategory.getBillingaccrualindicator().equals("Y")) {
                    if (tmpChargeAmt > 0) {
                        subLedgerOutput.setSubledgerTotalDebitAmount(tmpChargeAmt);
                    } else {
                        subLedgerOutput.setSubledgerTotalCreditAmount(tmpChargeAmt);
                    }
                } else if (financialEventCategory.getBillingaccrualindicator().equals("N")) {
                    if (tmpChargeAmt > 0) {
                        subLedgerOutput.setSubledgerTotalCreditAmount(tmpChargeAmt);
                    } else {
                        subLedgerOutput.setSubledgerTotalDebitAmount(tmpChargeAmt);
                    }
                }
            }
        }
        else if (financialEventCategory.getFinancialeventnormalsign().equals("CR")) {
            if (financialEventCategory.getDebitcreditindicator().equals("CR")) {
                if (tmpChargeAmt > 0) {
                    subLedgerOutput.setSubledgerTotalCreditAmount(tmpChargeAmt);
                } else {
                    subLedgerOutput.setSubledgerTotalDebitAmount(tmpChargeAmt);
                }
            }
            else if (financialEventCategory.getDebitcreditindicator().equals("DR")) {
                if (tmpChargeAmt > 0) {
                    subLedgerOutput.setSubledgerTotalDebitAmount(tmpChargeAmt);
                } else {
                    subLedgerOutput.setSubledgerTotalCreditAmount(tmpChargeAmt);
                }
            }
        }
        
        subLedgerOutput.setFinancialEventNumber(financialEventCategory.getFinancialeventnumber());
        subLedgerOutput.setFinancialCategory(financialEventCategory.getFinancialcategory());
        //subLedgerOutput.setFinancialmarketId(financialEventCategory.getFinancialmarketid());
        subLedgerOutput.setFinancialmarketId(financialMarket);
        subLedgerOutput.setBillCycleMonthYear(ProcessingUtils.getYearAndMonthFromStrDate(this.tempSubLedgerOuput.getDates().getRptPerEndDate()));
        subLedgerOutput.setBillAccrualIndicator(financialEventCategory.getBillingaccrualindicator());
        
        if (subLedgerOutput.getSubledgerTotalDebitAmount()==null) {
            LOGGER.info("No subledger total debit amount, cannot determine booking side (DR/CR) !!!");            
        }
        
        if (subLedgerOutput.getSubledgerTotalDebitAmount() > 0) {
            subLedgerOutput.setSubledgerTotalCreditAmount(subLedgerOutput.getSubledgerTotalDebitAmount());
            subLedgerOutput.setSubledgerTotalDebitAmount(0d);
        } else {
            subLedgerOutput.setSubledgerTotalDebitAmount(subLedgerOutput.getSubledgerTotalDebitAmount());
            subLedgerOutput.setSubledgerTotalCreditAmount(0d);
        }
        
        // balance check
        if (isBookingBalanced(subLedgerOutput)) {
            LOGGER.info("Sub ledger record is balanced");
        } else {
            LOGGER.info("Sub ledger record is unbalanced - rebalancing is not yet ready");
            rebalanceBooking(subLedgerOutput);
        }        
    }

    private boolean isBookingBalanced(SummarySubLedgerDTO subLedgerOutput) {
        boolean result = false;
        if (subLedgerOutput.getSubledgerTotalCreditAmount().equals(subLedgerOutput.getSubledgerTotalDebitAmount()))
            result = true;
        else {
            if ((subLedgerOutput.getSubledgerTotalCreditAmount().equals(0d) && subLedgerOutput.getSubledgerTotalDebitAmount() < 0.01d)
                || (subLedgerOutput.getSubledgerTotalDebitAmount().equals(0d) && subLedgerOutput.getSubledgerTotalCreditAmount() < 0.01d))
                result = true;
        }    
        return result;
    }
    
    private void rebalanceBooking(SummarySubLedgerDTO subLedgerOutpu) {
        // ths logic is a little bit unclear and has been left over for further discution
    }

    private boolean isAlternateBookingApplicable(BaseBookingInputInterface inRec) {
        boolean altBookingInd = false;
        String homeGlMarketId = null;
        String servingGlMarketId;
        
        searchHomeSbid = inRec.getHomeSbid();
        if (inRec.getServingSbid().trim().isEmpty()) {
            searchServingSbid = searchHomeSbid;
        } else {
            searchHomeSbid = inRec.getServingSbid();
        }
        if (searchHomeSbid.equals(searchServingSbid)) {
            homeEqualsServingSbid = true;
        }

        /*
        call cassandra finacial market with bind params: fileRecord.getFinancialMarket() and homeEqServSbid (what column?)
        this call has to retrieve ust 1 record, if more records get back throw an error and move record to fail list
         */
        FinancialMarket financialMarket = new FinancialMarket(); // this object come from database as a response of above call
        financialMarket.setSidbid("VZHUB");
        financialMarket.setAlternatebookingtype("Z");
        financialMarket.setGllegalentityid("1");
        financialMarket.setGlmarketid("1");
        
        String homeLegalEntityId = null;
        String servingLegalEntityId = null;

        if (financialMarket.getSidbid().equals(searchHomeSbid) && financialMarket.getAlternatebookingtype().equals("P")) {
            homeLegalEntityId = financialMarket.getGllegalentityid();
            if (homeEqualsServingSbid) {
                altBookingInd = true;
            }
        }

        if (!homeEqualsServingSbid && (financialMarket.getSidbid().equals(searchServingSbid) && financialMarket.getAlternatebookingtype().equals("P"))) {
            servingLegalEntityId = financialMarket.getGllegalentityid();
            if (homeLegalEntityId.equals(servingLegalEntityId)) {
                altBookingInd = true;
            }
        }

        if (financialMarket.getSidbid().equals(searchHomeSbid) && financialMarket.getAlternatebookingtype().equals("M")) {
            homeGlMarketId = financialMarket.getGlmarketid();
            if (homeEqualsServingSbid) {
                altBookingInd = true;
            }
        }

        if (!homeEqualsServingSbid && (financialMarket.getSidbid().equals(searchServingSbid) && financialMarket.getAlternatebookingtype().equals("M"))) {
            servingGlMarketId = financialMarket.getGlmarketid();
            if (homeGlMarketId.equals(servingGlMarketId)) {
                altBookingInd = true;
            }
        }
        return altBookingInd;
    }
    
    private boolean bypassBooking(FinancialEventCategory financialEventCategory, BaseBookingInputInterface inputRec) {        
        boolean altBookingInd = this.isAlternateBookingApplicable(inputRec);
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
        //int tmpInterExchangeCarrierCode = 0;
        boolean bypassBooking;
        //boolean altBookingInd;
        //boolean defaultBooking;
        
        outRec.setBilledInd("Y");
        this.fileSource = "B";
        
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
        }
        
        if (billedRec.getDeviceType().trim().isEmpty())                
            financialMarket = billedRec.getFinancialMarket();

        if ((billedRec.getTollProductId() > 0 && billedRec.getTollCharge() > 0)
            || billedRec.getInterExchangeCarrierCode().equals(5050)
            || billedRec.getIncompleteInd().equals("D")
            || !billedRec.getHomeSbid().equals(billedRec.getServingSbid())
            || (billedRec.getAirProdId().equals(190) && billedRec.getWholesaleTollChargeLDPeak() > 0 && billedRec.getWholesaleTollChargeLDOther() > 0)) {

            if (billedRec.getAirProdId().equals(190)) {
                this.tmpProdId = 95;
            } else {
                this.tmpProdId = billedRec.getAirProdId();
            }

            if (billedRec.getIncompleteInd().equals("D")) {
                this.tmpChargeAmt = billedRec.getTollCharge();
                outRec.setDollarAmtOther(this.tmpChargeAmt);

                DataEvent dataEvent = this.getDataEventFromDb();
                
                /* compute data usage */
                if (dataEvent.getDataEventSubType().equals("DEFLT")) {
                    outRec.setDollarAmt3G(this.tmpChargeAmt);
                    outRec.setUsage3G(Math.round(billedRec.getWholesaleUsageBytes().doubleValue() / 1024));
                }    
                else if (dataEvent.getDataEventSubType().equals("DEF4G")) {
                    outRec.setDollarAmt4G(this.tmpChargeAmt);
                    outRec.setUsage4G(Math.round(billedRec.getWholesaleUsageBytes().doubleValue() / 1024));
                }
            } else {
                tmpChargeAmt = billedRec.getWholesaleTollChargeLDPeak() + billedRec.getWholesaleTollChargeLDOther();
                outRec.setTollDollarsAmt(this.tmpChargeAmt);
                outRec.setTollMinutes(this.tmpProdId);
                outRec.setTollMinutes(Math.round(billedRec.getTollBillSeconds() / 60));
            }
        }
        outRec.setPeakDollarAmt(0d);
        
//        if (PROD_IDS_TOLL.contains(this.tmpProdId))
//            tmpInterExchangeCarrierCode = billedRec.getInterExchangeCarrierCode();
        
        
        /* do events & book record */        
        FinancialEventCategory financialEventCategory = this.getEventCategoryFromDb();
        bypassBooking = this.bypassBooking(financialEventCategory, billedRec);

        /* default booking check - basically it means population of sub leadger record */   
        /* this rule here works only for billed booking */
        
// this is dummy code, never used anywhere after
//        if (this.tmpProdId == 0 || (PROD_IDS.contains(this.tmpProdId) && billedRec.getInterExchangeCarrierCode() == 0)) {
//            defaultBooking = true;
//        } else {
//            tmpInterExchangeCarrierCode = 0; // it is already intiialized with 0, no other values used
//        }
        if (bypassBooking)
            LOGGER.warn("Booking bypass detected, record skipped for sub ledger file ...");
        else
            this.createSubLedgerRecord(tmpChargeAmt, financialEventCategory, financialMarket);
                
        return outRec;
    }
    
    private AggregateWholesaleReportDTO processUnbilledRecord(UnbilledCsvFileDTO unbilledRec) {
        if (unbilledRec.getAirProdId() > 0 && (unbilledRec.getWholesalePeakAirCharge() > 0 || unbilledRec.getWholesaleOffpeakAirCharge() > 0)) {
            AggregateWholesaleReportDTO outRec = new AggregateWholesaleReportDTO();

            outRec.setBilledInd("N");
            this.fileSource = "U";
            this.searchHomeSbid = unbilledRec.getHomeSbid();
            if (unbilledRec.getServingSbid().trim().isEmpty())
                this.searchServingSbid = unbilledRec.getHomeSbid();
            else
                this.searchServingSbid = unbilledRec.getServingSbid();

            if (this.searchHomeSbid.equals(this.searchServingSbid))
                this.homeEqualsServingSbid = true;

            financialMarket = unbilledRec.getFinancialMarket();
            
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
            
            DataEvent dataEvent = this.getDataEventFromDb();
            
            if (dataEvent.getDataEventSubType().equals("DEFLT")) {
                outRec.setDollarAmt3G(this.tmpChargeAmt);
                outRec.setUsage3G(Math.round(unbilledRec.getTotalWholesaleUsage().doubleValue() / 1024));
            }    
            else if (dataEvent.getDataEventSubType().equals("DEF4G")) {
                outRec.setDollarAmt4G(this.tmpChargeAmt);
                outRec.setUsage4G(Math.round(unbilledRec.getTotalWholesaleUsage().doubleValue() / 1024));
            }
            
            FinancialEventCategory financialEventCategory = this.getEventCategoryFromDb();
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
        this.tmpProdId = adminFeesRec.getProductId();
        this.financialMarket = adminFeesRec.getFinancialMarket();
        
        // call cassandra wholesale price table
        WholesalePrice wholesalePrice = this.getWholesalePriceFromDb();
        this.tmpChargeAmt = wholesalePrice.getProductWholesalePrice() * adminFeesRec.getAdminCount();
        outRec.setDollarAmtOther(this.tmpChargeAmt);
        
        boolean bypassBooking = false;
        
        FinancialEventCategory financialEventCategory = this.getEventCategoryFromDb();
        //financialEventCategory.setHomesidequalsservingsidindicator(" "); // this ensures the record will be bypassed
                
        if (financialEventCategory.getHomesidequalsservingsidindicator().trim().isEmpty())
            bypassBooking = false;
        
        if (bypassBooking)
            LOGGER.warn("Booking bypass detected, record skipped for sub ledger file ...");
        else
            this.createSubLedgerRecord(tmpChargeAmt, financialEventCategory, financialMarket);
        
        return outRec;
    }
    
    private FinancialEventCategory getEventCategoryFromDb() {
        FinancialEventCategory financialEventCategory = new FinancialEventCategory();
        financialEventCategory.setBamsaffiliateindicator("N");
        //financialEventCategory.setCompanycode("CDN");         // that causes booking bypass for 'B' file
        financialEventCategory.setCompanycode(" ");
        financialEventCategory.setForeignservedindicator("Y");
        financialEventCategory.setHomesidequalsservingsidindicator("Y");
        financialEventCategory.setFinancialeventnormalsign("DR");
        financialEventCategory.setDebitcreditindicator("DR");
        financialEventCategory.setBillingaccrualindicator("Y");
        financialEventCategory.setFinancialeventnumber(4756);
        financialEventCategory.setFinancialcategory(678);
        financialEventCategory.setFinancialmarketid("FM1");
        financialEventCategory.setAlternatebookingindicator("Z");
        
        return financialEventCategory;
    }
    
    private DataEvent getDataEventFromDb() {
        DataEvent event = new DataEvent();
        event.setProductId(100);
        event.setDataEventSubType("DEFLT"); // 3G
        
        return event;
    }
    
    private WholesalePrice getWholesalePriceFromDb() {
        WholesalePrice wholesalePrice = new WholesalePrice();
        wholesalePrice.setProductWholesalePrice(351.45);
        return wholesalePrice;
    }
}
