package org.flowable.rest.service.api.management;

import java.util.Calendar;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for all REST-operations related to the Job collection and a single job resource.
 * 
 * @author Frederik Heremans
 */
public class JobExceptionStacktraceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting the stacktrace for a failed job
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobExceptionStacktraceResourceTest.testTimerProcess.bpmn20.xml" })
    public void testGetJobStacktrace() throws Exception {
        // Start process, forcing error on job-execution
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess", Collections.singletonMap("error", (Object) Boolean.TRUE));

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Force execution of job
        try {
            managementService.moveTimerToExecutableJob(timerJob.getId());
            managementService.executeJob(timerJob.getId());
            fail();
        } catch (FlowableException expected) {
            // Ignore, we expect the exception
        }

        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        processEngineConfiguration.getClock().setCurrentTime(now.getTime());

        CloseableHttpResponse response = executeRequest(new HttpGet(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_EXCEPTION_STRACKTRACE, timerJob.getId())), HttpStatus.SC_OK);

        String stack = IOUtils.toString(response.getEntity().getContent());
        assertNotNull(stack);
        assertEquals(managementService.getTimerJobExceptionStacktrace(timerJob.getId()), stack);

        // Also check content-type
        assertEquals("text/plain", response.getEntity().getContentType().getValue());
        closeResponse(response);
    }

    /**
     * Test getting the stacktrace for an unexisting job.
     */
    @Test
    public void testGetStackForUnexistingJob() throws Exception {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_EXCEPTION_STRACKTRACE, "unexistingjob")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test getting the stacktrace for an unexisting job.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobExceptionStacktraceResourceTest.testTimerProcess.bpmn20.xml" })
    public void testGetStackForJobWithoutException() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess", Collections.singletonMap("error", (Object) Boolean.FALSE));
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_EXCEPTION_STRACKTRACE, timerJob.getId())), HttpStatus.SC_NOT_FOUND));
    }
}
