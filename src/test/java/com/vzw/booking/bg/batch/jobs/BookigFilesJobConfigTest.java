/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.booking.bg.batch.jobs;

import com.vzw.booking.bg.batch.readers.BilledBookingFileReader;
import com.vzw.booking.bg.batch.readers.BookDateCsvFileReader;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 *
 * @author smorcja
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, StepScopeTestExecutionListener.class})
//@PropertySource("classpath:application.properties")
//@SpringBootTest(classes = BookingWholesaleApplication.class)
//@TestPropertySource(properties = {"csv.to.database.job.source.file.path", "./src/main/resources/data"})
public class BookigFilesJobConfigTest {
    
    MockEnvironment environment;
    
//    @Mock
//    BilledBookingFileReader billedFileItemReader;
//    
//    @Mock
//    BookDateCsvFileReader bookdateFileItemReader;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        environment = new MockEnvironment();
        environment.setProperty("csv.to.database.job.source.file.path", "./src/main/resources/data");
        //reader = new BookDateCsvFileReader(environment, "bookdate.csv");
    }

//    @Mock
//    protected StepExecution stepExecution;
//
//    @Mock
//    protected JobParameters jobParams;

//JobLauncherTestUtil jobLauncherTestUtil = new JobLauncherTestUtil();
//jobLauncherTestUtil.setJobLauncher(jobLauncher);
//jobLauncherTestUtil.setJob(job);
//jobLauncherTestUtil.setJobRepository(jobRepository);
//Map<String, JobParameter> params = Maps.newHashMap();
////determine job params here:
//params.put(....);
//JobParameters jobParams = new JobParameters(params);
//ExecutionContext context = new ExecutionContext();
////put something to job context, if you need.
//context.put(...);
//JobExecution jobExecution = jobLauncherTestUtil.launchStep("stepId",jobParams,context);
//
//Assert.assertEquals("Step stepId failed", ExitStatus.COMPLETED, execution.getExitStatus())
    
    @Before
    public void setup() {        
    }
    
    @Test
    public void launchJob() throws Exception {
        ApplicationContext context = new AnnotationConfigApplicationContext(BookigFilesJobConfig.class);
        assertNotNull(context);
        
        //JobLauncherTestUtils jobLauncherTestUtils = context.getBean(JobLauncherTestUtils.class);
        
//        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
//        assertNotNull(jobExecution);
        
//        Assert.assertEquals(jobExecution.getStatus(), BatchStatus.COMPLETED);
        
        //assertNotNull(jobLauncherTestUtils);

    }
    
}
