/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.processors;

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

/**
 *
 * @author smorcja
 * @param <I>
 */
public class WholesaleReportProcessor<I extends BaseBookingInputInterface> implements ItemProcessor<I, AggregateWholesaleReportDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WholesaleReportProcessor.class);
     
    @Autowired
    SubLedgerProcessor tempSubLedgerOuput;

    String searchServingSbid = null;
    String searchHomeSbid = null;
    boolean homeEqualsServingSbid = false;

    /**
     * this method is a working version of booking logic, it is exact representation of the spec and need to be tuned when finished
     * @param inRec
     * @return
     * @throws Exception 
     */
    @Override
    public AggregateWholesaleReportDTO process(I inRec) throws Exception {
        double tmpChargeAmt = 0;
        final Set<Integer> PROD_IDS_TOLL = new HashSet(Arrays.asList(new Integer[]{95, 12872, 12873, 36201}));
        final Set<Integer> PROD_IDS = new HashSet(Arrays.asList(new Integer[]{95, 12872, 12873, 13537, 13538, 36201}));
        int tmpProdId = 0;
        //String messageSource = "B";
        boolean bypassBooking = false;
        boolean defaultBooking = false;
        int tmpInterExchangeCarrierCode = 0;
        String financialMarket = "";

        // check if alternate booking is applicable but what is the consequence of it ???
        AggregateWholesaleReportDTO outRec = new AggregateWholesaleReportDTO();
        outRec.setBilledInd("Y");
        
        // check if wholesale part can be processed for this record at all
        if (inRec.getAirProdId() > 0 && (inRec.getWholesalePeakAirCharge() > 0 || inRec.getWholesaleOffpeakAirCharge() > 0)) {
            outRec.setPeakDollarAmt(inRec.getWholesalePeakAirCharge());
            outRec.setOffpeakDollarAmt(inRec.getWholesaleOffpeakAirCharge());
            outRec.setDollarAmtOther(0d);
            outRec.setVoiceMinutes(inRec.getAirBillSeconds() * 60);
            tmpChargeAmt = inRec.getWholesalePeakAirCharge() + inRec.getWholesaleOffpeakAirCharge();
            if (inRec.getAirProdId().equals(190))
                tmpProdId = 1;
            else
                tmpProdId = inRec.getAirProdId();            
        }
        
        // this part is applicable for billed (checked) file as it uses fields specific for that file
        if (inRec instanceof BilledCsvFileDTO) {
            BilledCsvFileDTO billedRec = (BilledCsvFileDTO) inRec;
            if (billedRec.getDeviceType().trim().isEmpty())                
                financialMarket = inRec.getFinancialMarket();
            
            if ((billedRec.getTollProductId() > 0 && billedRec.getTollCharge() > 0)
                || billedRec.getInterExchangeCarrierCode().equals(5050)
                || billedRec.getIncompleteInd().equals("D")
                || !billedRec.getHomeSbid().equals(billedRec.getServingSbid())
                || (billedRec.getAirProdId().equals(190) && billedRec.getWholesaleTollChargeLDPeak() > 0 && billedRec.getWholesaleTollChargeLDOther() > 0)) {
            
                if (billedRec.getAirProdId().equals(190)) {
                    tmpProdId = 95;
                } else {
                    tmpProdId = billedRec.getAirProdId();
                }
                
                if (billedRec.getIncompleteInd().equals("D")) {
                    tmpChargeAmt = billedRec.getTollCharge();
                    outRec.setDollarAmtOther(tmpChargeAmt);
                    
                    // another cassandra call to DataEvent table
                    // data aggregation - have no clue what is that !!!
                } else {
                    tmpChargeAmt = billedRec.getWholesaleTollChargeLDPeak() + billedRec.getWholesaleTollChargeLDOther();
                    outRec.setTollDollarsAmt(tmpChargeAmt);
                    outRec.setTollMinutes(tmpProdId);
                    outRec.setTollMinutes(billedRec.getTollBillSeconds() * 60); // this is supposed to be re=ounded but how ???
                }
            }
            outRec.setPeakDollarAmt(0d);
            if (PROD_IDS_TOLL.contains(tmpProdId))
                tmpInterExchangeCarrierCode = ((BilledCsvFileDTO) inRec).getInterExchangeCarrierCode();
        }
        
        /* do events & book record */
        
        // second cassandra call goes here, it will check if product is wholesale product        
        String wholesaleBillingCode = null; // this object comes from db as response 
        //if (wholesaleBillingCode != null) it is
        //else it is not
        // what is that for ??? It's never used anywhere after 

        // move tmpChargeAmt to tmpWholesaleCost and tmpWholsaleSettlement ???
        // third cassandra call goes here, should retrieve unique row of FinancialEventCategory
        
        //FinancialEventCategory financialEventCategory = null; // this object comes from db (just one) 
        
        // fake financial category record provided to make the runn successful
        FinancialEventCategory financialEventCategory = new FinancialEventCategory();
        financialEventCategory.setBamsaffiliateindicator("N");
        financialEventCategory.setCompanycode("CDN");
        financialEventCategory.setForeignservedindicator("Y");
        financialEventCategory.setHomesidequalsservingsidindicator("Y");
        financialEventCategory.setFinancialeventnormalsign("DR");
        financialEventCategory.setDebitcreditindicator("DR");
        financialEventCategory.setBillingaccrualindicator("Y");
        financialEventCategory.setFinancialeventnumber(4756);
        financialEventCategory.setFinancialcategory(678);
        financialEventCategory.setFinancialmarketid("FM1");
        // end of fake object - to be removed
        
        boolean altBookingInd = this.isAlternateBookingApplicable(inRec);
        
        if (!financialEventCategory.getBamsaffiliateindicator().equals("N")
            || !financialEventCategory.getCompanycode().trim().isEmpty()
            || (searchHomeSbid.equals(searchServingSbid) && !financialEventCategory.getForeignservedindicator().trim().isEmpty())) {            
            bypassBooking = true;
        }

        if (inRec.getMessageSource().equals("M") && financialEventCategory.getHomesidequalsservingsidindicator().trim().isEmpty())
            bypassBooking = false;
        else if (inRec.getMessageSource().equals("B")) {
            if (financialEventCategory.getBillingaccrualindicator().equals("Y"))
                bypassBooking = false;
            
            if (searchHomeSbid.equals(searchServingSbid)) {
                if (financialEventCategory.getHomesidequalsservingsidindicator().equals("Y"))
                    bypassBooking = false;
            }               
            else {
                if (altBookingInd && financialEventCategory.getAlternatebookingindicator().equals("Y"))
                    bypassBooking = false;
                else if (!altBookingInd && financialEventCategory.getAlternatebookingindicator().equals("N"))
                    bypassBooking = false;
            }
        }
        
        /* default booking check - basically it means population of sub leadger record */   
        /* this rule here works only for billed booking */
        if (tmpProdId == 0 || (inRec instanceof BilledCsvFileDTO && (PROD_IDS.contains(tmpProdId) && ((BilledCsvFileDTO) inRec).getInterExchangeCarrierCode() == 0))) {
            defaultBooking = true;
        } else {
            tmpInterExchangeCarrierCode = 0; // it is already intiialized with 0, no other values used
        }
        // do the booking if no bypass detected
        if (bypassBooking)
            LOGGER.warn("Booking bypass detected, record skipped for sub ledger file ...");
        else
            this.crateSubLedgerRecord(tmpChargeAmt, financialEventCategory, financialMarket);
        
        return outRec;
    }

    private void crateSubLedgerRecord(double tmpChargeAmt, FinancialEventCategory financialEventCategory, String financialMarket) {
        SummarySubLedgerDTO subLedgerOutput = this.tempSubLedgerOuput.add();
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
        subLedgerOutput.setBillCycleMonthYear(this.tempSubLedgerOuput.getDates().getRptPerEndDate()); // need to be in YYYYMM format 
        subLedgerOutput.setBillAccrualIndicator(financialEventCategory.getBillingaccrualindicator());
        // balance check
        if (isBookingBalanced(subLedgerOutput)) {
            LOGGER.info("Sub ledger record is balanced");
        } else {
            LOGGER.info("Sub ledger record is unbalanced - rebalancing...");
            rebalanceBooking(subLedgerOutput);
        }
        
        if (subLedgerOutput.getSubledgerTotalDebitAmount() > 0) {
            subLedgerOutput.setSubledgerTotalCreditAmount(subLedgerOutput.getSubledgerTotalDebitAmount());
            subLedgerOutput.setSubledgerTotalDebitAmount(0d);
        } else {
            subLedgerOutput.setSubledgerTotalDebitAmount(subLedgerOutput.getSubledgerTotalDebitAmount());
            subLedgerOutput.setSubledgerTotalCreditAmount(0d);
        }
        
        // there is some unclear repetitions of rebalancing and cumulation of values but upon what ???
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
        
    }

    private boolean isAlternateBookingApplicable(I inRec) {
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
        FinancialMarket financialMarket = null; // this object come from database as a response of above call
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
}
