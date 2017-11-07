/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.utils;

import com.vzw.booking.bg.batch.domain.ExternalizationMetadata;
import com.vzw.booking.bg.batch.domain.SummarySubLedgerDTO;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author smorcja
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({StepScopeTestExecutionListener.class})
@ContextConfiguration
public class SubledgerLineAggregatorTest {
    private FixedLengthLineAggregator<SummarySubLedgerDTO> lineAggregator;
	private @Value("${com.wzw.springbatch.processor.writer.format.subledger}") String subLedgerFormat;
   
    @Before
    public void setUp() {
        ExternalizationMetadata metaData = null;
        try {
        	metaData = ReflectionsUtility.getParametersMap(SummarySubLedgerDTO.class, subLedgerFormat);
		} catch (Exception e) {
			System.exit(1);
		}
        this.lineAggregator = new FixedLengthLineAggregator<SummarySubLedgerDTO>();
        this.lineAggregator.setFormat(metaData);
    }

    @Test
    public void aggregateTest() {
        System.out.println("Testing subledger line aggregator - success");
        
        SummarySubLedgerDTO item = new SummarySubLedgerDTO();
        item.setFinancialEventNumber(456789);
        item.setFinancialmarketId("DUB");
        item.setFinancialCategory(789);
        item.setSubledgerTotalCreditAmount(-1234.78);
        item.setSubledgerTotalDebitAmount(86654.89);
        item.setUpdateUserId("WSBTest"); // default value is too long !!!
        
        String result = lineAggregator.aggregate(item);
        
        System.out.println(result);
        System.out.println("Line length: "+result.length());
        assertEquals(170, result.length()); //should it be 171 ???
    }    
}
