/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.squadd.batch.writers;

import eu.squadd.batch.constants.Constants;
import eu.squadd.batch.domain.SummarySubLedgerDTO;
import org.springframework.stereotype.Component;

/**
 *
 * @author smorcja
 */
@Component
public class SubledgerCsvFileWriter extends CsvFileGenericWriter<SummarySubLedgerDTO> {    
    private static final String[] COLUMN_NAMES = new String[] {
        "jemsApplId",
        "reportStartDate",
        "jemsApplTransactioDate",
        "financialEventNumber",
        "financialCategory", 
        "financialmarketId",
        "subledgerSequenceNumber",
        "subledgerTotalDebitAmount",
        "subledgerTotalCreditAmount",
        "jurnalEventNumber",
        "jurnalEventExceptionCode",
        "jurnalEventReadInd",
        "generalLedgerTransactionNumber",
        "billCycleNumber",
        "billTypeCode",
        "billCycleMonthYear",
        "billPhaseType",
        "billMonthInd",
        "billAccrualIndicator",
        "paymentSourceCode",
        "discountOfferId",
        "updateUserId",
        "updateTimestamp"};

    public SubledgerCsvFileWriter(String filename) {        
        super(filename, COLUMN_NAMES, Constants.DEFAULT_CSV_FIELDS_DELIMITER);
    }    
}
