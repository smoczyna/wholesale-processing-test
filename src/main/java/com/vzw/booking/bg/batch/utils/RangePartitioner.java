/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 *
 * @author smorcja
 */
@Component
public class RangePartitioner implements Partitioner {

    private static final String PROPERTY_CSV_SOURCE_FILE_PATH = "csv.to.database.job.source.file.path";
    private final String resourceLocation;
    
    @Autowired
    private ApplicationContext applicationContext;

    public RangePartitioner(Environment environment, String splitFolder) {
        this.resourceLocation = environment.getRequiredProperty(PROPERTY_CSV_SOURCE_FILE_PATH).concat(splitFolder);
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap();
        Resource[] resources;
        try {
            resources = applicationContext.getResources(resourceLocation.concat("*.csv"));
            for (Resource resource : resources) {
                ExecutionContext context = new ExecutionContext();
                String fileName = resource.getFilename();
                int fileNo = Integer.parseInt(fileName.substring(fileName.indexOf("_")));
                context.putString("fileName", fileName);
                result.put("partition" + fileNo, context);
            }
        } catch (IOException ex) {
            Logger.getLogger(RangePartitioner.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
}
