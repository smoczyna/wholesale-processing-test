package com.vzw.booking.bg.batch.launchers;

import com.vzw.booking.bg.batch.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Migration job launcher, it takes 1 parameter: current date and time displayed when the job starts
 * it starts automatically right after application start and gets executed every minute since then until terminated
 * 
 * @author smorcja
 */
@Component
public class BookingsJobLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingsJobLauncher.class);

    private final Job job;

    private final JobLauncher jobLauncher;

    @Autowired
    BookingsJobLauncher(@Qualifier("bookingAggregateJob") Job job, JobLauncher jobLauncher) {
        this.job = job;
        this.jobLauncher = jobLauncher;
    }

    void launchCsvFileToDatabaseJob() throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        LOGGER.info(Constants.JOB_STARTED);
        jobLauncher.run(job, newExecution());
        LOGGER.info(Constants.JOB_FINISHED);        
    }

    private JobParameters newExecution() {
        Map<String, JobParameter> parameters = new HashMap<>();  
        parameters.put("currentTime", new JobParameter(new Date()));
        parameters.put("maxSkippedRecords", new JobParameter(1000000L));
        parameters.put("numberOfChunks", new JobParameter(50000L));
        return new JobParameters(parameters);
    }
}
