/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.domain.casandra.mappers;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.vzw.booking.bg.batch.domain.dump.BatchJobExecution;
import com.vzw.booking.bg.batch.utils.AbstractMapper;

/**
 *
 * @author smorcja
 */
public class BatchJobExecutionMapper extends AbstractMapper<BatchJobExecution> {

    @Override
    protected Mapper<BatchJobExecution> getMapper(MappingManager manager) {
        return manager.mapper(BatchJobExecution.class);
    }
}
