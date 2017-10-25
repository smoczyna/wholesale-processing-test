/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.utils;

import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

/**
 *
 * @author smorcja
 */
public class RangePartitioner implements Partitioner {

    private static String PROPERTY_CSV_SOURCE_FILE_PATH = "csv.to.database.job.source.file.path";
    private Resource[] resources;

    public RangePartitioner(Environment environment) {
        this.resources = resources;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap();
        Resource[] resources = applicationContext.getResources(environment.getRequiredProperty(PROPERTY_CSV_SOURCE_FILE_PATH).concat("billed_split/*.csv"));
        for (Resource resource : this.resources) {
            ExecutionContext context = new ExecutionContext();
            String fileName = resource.getFilename();
            int fileNo = Integer.parseInt(fileName.substring(fileName.indexOf("_")));
            context.putString("fileName", fileName);
            result.put("partition" + fileNo, context);            
        }
        return result;
    }    
}
