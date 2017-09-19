/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.writers;

import com.vzw.booking.bg.batch.domain.AggregateWholesaleReportDTO;
import org.springframework.core.env.Environment;

/**
 *
 * @author smorcja
 */
public class WholesaleReportCsvWriter extends CsvFileGenericWriter<AggregateWholesaleReportDTO> {
        
    public WholesaleReportCsvWriter(Environment environment) {
        super(environment, "wholesale_report.csv", new String[] {"cycleMonthYear",
                                                                 "startDate",
                                                                 "endDate",
                                                                 "homeLegalEntity",
                                                                 "servingLegalEntity",
                                                                 "homeFinancialMarketId",
                                                                 "servingFinancialMarketId",
                                                                 "productDiscountOfferId",
                                                                 "contractTermId",
                                                                 "peakDollarAmt",
                                                                 "offpeakDollarAmt",
                                                                 "voiceMinutes",
                                                                 "tollDollarsAmt",
                                                                 "tollMinutes",
                                                                 "dollarAmt3G",
                                                                 "usage3G",
                                                                 "dollarAmt4G",
                                                                 "usage4G",
                                                                 "dollarAmtOther",
                                                                 "dbCrInd",
                                                                 "billedInd"});
    }
}

/*


*/