/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.writers;

import com.vzw.booking.bg.batch.domain.WholesaleProcessingOutput;
import java.util.List;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author smorcja
 */
@Component
public class WholesaleOutputWriter implements ItemStreamWriter<WholesaleProcessingOutput> {
    
    private final WholesaleReportCsvWriter wholesaleReportWriter;    
    private final SubledgerCsvFileWriter subledgerWriter;
    
    public WholesaleOutputWriter(Environment environment, String fileNo) {
        this.wholesaleReportWriter = new WholesaleReportCsvWriter(environment, fileNo);
        this.subledgerWriter = new SubledgerCsvFileWriter(environment, fileNo);
    }
    
    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        wholesaleReportWriter.open(ec);
        subledgerWriter.open(ec);
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
        wholesaleReportWriter.update(ec);
        subledgerWriter.update(ec);
    }

    @Override
    public void close() throws ItemStreamException {
        wholesaleReportWriter.close();
        subledgerWriter.close();
    }

    @Override
    public void write(List<? extends WholesaleProcessingOutput> list) throws Exception {
        for (WholesaleProcessingOutput item : list) {
            wholesaleReportWriter.write(item.getWholesaleReportRecords());
            subledgerWriter.write(item.getSubledgerRecords());
        }        
    }    
}