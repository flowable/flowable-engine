/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.test.api.mgmt;

import java.util.Date;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobNotFoundException;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsCmd;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Saeid Mizaei
 * @author Joram Barrez
 */
public class ManagementServiceTest extends PluggableFlowableTestCase {

    @Test
    public void testGetMetaDataForUnexistingTable() {
        TableMetaData metaData = managementService.getTableMetaData("unexistingtable");
        assertNull(metaData);
    }

    @Test
    public void testGetMetaDataNullTableName() {
        try {
            managementService.getTableMetaData(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException re) {
            assertTextPresent("tableName is null", re.getMessage());
        }
    }

    @Test
    public void testExecuteJobNullJobId() {
        try {
            managementService.executeJob(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException re) {
            assertTextPresent("JobId is null", re.getMessage());
        }
    }

    @Test
    public void testExecuteJobUnexistingJob() {
        try {
            managementService.executeJob("unexistingjob");
            fail("ActivitiException expected");
        } catch (JobNotFoundException jnfe) {
            assertTextPresent("No job found with id", jnfe.getMessage());
            assertEquals(Job.class, jnfe.getObjectClass());
        }
    }

    @Test
    @Deployment
    public void testGetJobExceptionStacktrace() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // The execution is waiting in the first usertask. This contains a boundary
        // timer event which we will execute manual for testing purposes.
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertNotNull("No job found for process instance", timerJob);

        try {
            managementService.moveTimerToExecutableJob(timerJob.getId());
            managementService.executeJob(timerJob.getId());
            fail("RuntimeException from within the script task expected");
        } catch (RuntimeException re) {
            assertTextPresent("This is an exception thrown from scriptTask", re.getCause().getMessage());
        }

        // Fetch the task to see that the exception that occurred is persisted
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertNotNull(timerJob);
        assertNotNull(timerJob.getExceptionMessage());
        assertTextPresent("This is an exception thrown from scriptTask", timerJob.getExceptionMessage());

        // Get the full stacktrace using the managementService
        String exceptionStack = managementService.getTimerJobExceptionStacktrace(timerJob.getId());
        assertNotNull(exceptionStack);
        assertTextPresent("This is an exception thrown from scriptTask", exceptionStack);
    }

    @Test
    public void testgetJobExceptionStacktraceUnexistingJobId() {
        try {
            managementService.getJobExceptionStacktrace("unexistingjob");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException re) {
            assertTextPresent("No job found with id unexistingjob", re.getMessage());
            assertEquals(Job.class, re.getObjectClass());
        }
    }

    @Test
    public void testgetJobExceptionStacktraceNullJobId() {
        try {
            managementService.getJobExceptionStacktrace(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException re) {
            assertTextPresent("jobId is null", re.getMessage());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
    public void testSetJobRetries() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // The execution is waiting in the first usertask. This contains a boundary timer event.
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        Date duedate = timerJob.getDuedate();

        assertNotNull("No job found for process instance", timerJob);
        assertEquals(processEngineConfiguration.getAsyncExecutorNumberOfRetries(), timerJob.getRetries());

        managementService.setTimerJobRetries(timerJob.getId(), 5);

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals(5, timerJob.getRetries());
        assertEquals(duedate, timerJob.getDuedate());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testFailingAsyncJob.bpmn20.xml" })
    public void testAsyncJobWithNoRetriesLeft() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // The execution is waiting in the first async script task.
        Job asyncJob = managementService.createJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertNotNull("No job found for process instance", asyncJob);
        assertEquals(processEngineConfiguration.getAsyncExecutorNumberOfRetries(), asyncJob.getRetries());

        try {
            managementService.executeJob(asyncJob.getId());
            fail("Exception expected");
        } catch (Exception e) {
            // expected exception
        }

        asyncJob = managementService.createTimerJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertEquals(2, asyncJob.getRetries());

        try {
            asyncJob = managementService.moveTimerToExecutableJob(asyncJob.getId());
            managementService.executeJob(asyncJob.getId());
            fail("Exception expected");
        } catch (Exception e) {
            // expected exception
        }

        asyncJob = managementService.createTimerJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        try {
            asyncJob = managementService.moveTimerToExecutableJob(asyncJob.getId());
            managementService.executeJob(asyncJob.getId());
            fail("Exception expected");
        } catch (Exception e) {
            // expected exception
        }

        asyncJob = managementService.createDeadLetterJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        managementService.moveDeadLetterJobToExecutableJob(asyncJob.getId(), 5);

        asyncJob = managementService.createJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertEquals(5, asyncJob.getRetries());
    }

    @Test
    public void testSetJobRetriesUnexistingJobId() {
        try {
            managementService.setJobRetries("unexistingjob", 5);
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException re) {
            assertTextPresent("No job found with id 'unexistingjob'.", re.getMessage());
            assertEquals(Job.class, re.getObjectClass());
        }
    }

    @Test
    public void testSetJobRetriesEmptyJobId() {
        try {
            managementService.setJobRetries("", 5);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException re) {
            assertTextPresent("The job id is mandatory, but '' has been provided.", re.getMessage());
        }
    }

    @Test
    public void testSetJobRetriesJobIdNull() {
        try {
            managementService.setJobRetries(null, 5);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException re) {
            assertTextPresent("The job id is mandatory, but 'null' has been provided.", re.getMessage());
        }
    }

    @Test
    public void testSetJobRetriesNegativeNumberOfRetries() {
        try {
            managementService.setJobRetries("unexistingjob", -1);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException re) {
            assertTextPresent("The number of job retries must be a non-negative Integer, but '-1' has been provided.", re.getMessage());
        }
    }

    @Test
    public void testDeleteJobNullJobId() {
        try {
            managementService.deleteJob(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException re) {
            assertTextPresent("jobId is null", re.getMessage());
        }
    }

    @Test
    public void testDeleteJobUnexistingJob() {
        try {
            managementService.deleteJob("unexistingjob");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("No job found with id", ae.getMessage());
            assertEquals(Job.class, ae.getObjectClass());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/timerOnTask.bpmn20.xml" })
    public void testDeleteJobDeletion() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnTask");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertNotNull("Task timer should be there", timerJob);
        managementService.deleteTimerJob(timerJob.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull("There should be no job now. It was deleted", timerJob);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/timerOnTask.bpmn20.xml" })
    public void testDeleteJobThatWasAlreadyAcquired() {
        processEngineConfiguration.getClock().setCurrentTime(new Date());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnTask");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        // We need to move time at least one hour to make the timer executable
        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + 7200000L));

        // Acquire job by running the acquire command manually
        ProcessEngineImpl processEngineImpl = (ProcessEngineImpl) processEngine;
        AcquireTimerJobsCmd acquireJobsCmd = new AcquireTimerJobsCmd(processEngine.getProcessEngineConfiguration().getAsyncExecutor());
        CommandExecutor commandExecutor = processEngineImpl.getProcessEngineConfiguration().getCommandExecutor();
        commandExecutor.execute(acquireJobsCmd);

        // Try to delete the job. This should fail.
        try {
            managementService.deleteJob(timerJob.getId());
            fail();
        } catch (FlowableException e) {
            // Exception is expected
        }

        // Clean up
        managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(timerJob.getId());
    }

    // https://jira.codehaus.org/browse/ACT-1816:
    // ManagementService doesn't seem to give actual table Name for EventSubscriptionEntity.class
    @Test
    public void testGetTableName() {
        String table = managementService.getTableName(EventSubscriptionEntity.class);
        assertEquals("ACT_RU_EVENT_SUBSCR", table);
    }
}
