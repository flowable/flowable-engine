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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Date;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.lock.LockManager;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
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
        assertThat(metaData).isNull();
    }

    @Test
    public void testGetMetaDataNullTableName() {
        assertThatThrownBy(() -> managementService.getTableMetaData(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("tableName is null");
    }

    @Test
    public void testExecuteJobNullJobId() {
        assertThatThrownBy(() -> managementService.executeJob(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("JobId is null");
    }

    @Test
    public void testExecuteJobUnexistingJob() {
        assertThatThrownBy(() -> managementService.executeJob("unexistingjob"))
                .isExactlyInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("No job found with id");
    }

    @Test
    @Deployment
    public void testGetJobExceptionStacktrace() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // The execution is waiting in the first usertask. This contains a boundary
        // timer event which we will execute manual for testing purposes.
        final Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(timerJob).as("No job found for process instance").isNotNull();

        assertThatThrownBy(() -> {
            managementService.moveTimerToExecutableJob(timerJob.getId());
            managementService.executeJob(timerJob.getId());
        })
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("This is an exception thrown from scriptTask");

        // Fetch the task to see that the exception that occurred is persisted
        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(timerJob2).isNotNull();
        assertThat(timerJob2.getExceptionMessage()).isNotNull();
        assertThat(timerJob2.getExceptionMessage())
                .contains("This is an exception thrown from scriptTask");

        // Get the full stacktrace using the managementService
        String exceptionStack = managementService.getTimerJobExceptionStacktrace(timerJob2.getId());
        assertThat(exceptionStack).isNotNull();
        assertThat(exceptionStack)
                .contains("This is an exception thrown from scriptTask");
    }

    @Test
    public void testgetJobExceptionStacktraceUnexistingJobId() {
        assertThatThrownBy(() -> managementService.getJobExceptionStacktrace("unexistingjob"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("No job found with id unexistingjob");
    }

    @Test
    public void testgetJobExceptionStacktraceNullJobId() {
        assertThatThrownBy(() -> managementService.getJobExceptionStacktrace(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("jobId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testGetJobExceptionStacktrace.bpmn20.xml" })
    public void testSetJobRetries() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // The execution is waiting in the first usertask. This contains a boundary timer event.
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        Date duedate = timerJob.getDuedate();

        assertThat(timerJob).as("No job found for process instance").isNotNull();
        assertThat(timerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());

        managementService.setTimerJobRetries(timerJob.getId(), 5);

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob.getRetries()).isEqualTo(5);
        assertThat(timerJob.getDuedate()).isEqualTo(duedate);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/ManagementServiceTest.testFailingAsyncJob.bpmn20.xml" })
    public void testAsyncJobWithNoRetriesLeft() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exceptionInJobExecution");

        // The execution is waiting in the first async script task.
        Job asyncJob = managementService.createJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(asyncJob).as("No job found for process instance").isNotNull();
        assertThat(asyncJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());

        final String asyncId = asyncJob.getId();
        assertThatThrownBy(() -> managementService.executeJob(asyncId))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("problem evaluating script");

        asyncJob = managementService.createTimerJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(asyncJob.getRetries()).isEqualTo(2);
        assertThat(asyncJob.getElementId()).isEqualTo("theScriptTask");
        assertThat(asyncJob.getElementName()).isEqualTo("Execute script");

        final String jobId = asyncJob.getId();
        assertThatThrownBy(() -> {
            Job job = managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(job.getId());
        })
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("problem evaluating script");

        asyncJob = managementService.createTimerJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(asyncJob.getElementId()).isEqualTo("theScriptTask");
        assertThat(asyncJob.getElementName()).isEqualTo("Execute script");

        final String jobId2 = asyncJob.getId();
        assertThatThrownBy(() -> {
            Job job = managementService.moveTimerToExecutableJob(jobId2);
            managementService.executeJob(jobId2);
        })
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("problem evaluating script");

        asyncJob = managementService.createDeadLetterJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(asyncJob.getElementId()).isEqualTo("theScriptTask");
        assertThat(asyncJob.getElementName()).isEqualTo("Execute script");

        managementService.moveDeadLetterJobToExecutableJob(asyncJob.getId(), 5);

        asyncJob = managementService.createJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(asyncJob.getElementId()).isEqualTo("theScriptTask");
        assertThat(asyncJob.getElementName()).isEqualTo("Execute script");

        assertThat(asyncJob.getRetries()).isEqualTo(5);
    }

    @Test
    public void testSetJobRetriesUnexistingJobId() {
        assertThatThrownBy(() -> managementService.setJobRetries("unexistingjob", 5))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("No job found with id 'unexistingjob'.");
    }

    @Test
    public void testSetJobRetriesEmptyJobId() {
        assertThatThrownBy(() -> managementService.setJobRetries("", 5))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("The job id is mandatory, but '' has been provided.");
    }

    @Test
    public void testSetJobRetriesJobIdNull() {
        assertThatThrownBy(() -> managementService.setJobRetries(null, 5))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("The job id is mandatory, but 'null' has been provided.");
    }

    @Test
    public void testSetJobRetriesNegativeNumberOfRetries() {
        assertThatThrownBy(() -> managementService.setJobRetries("unexistingjob", -1))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("The number of job retries must be a non-negative Integer, but '-1' has been provided.");
    }

    @Test
    public void testDeleteJobNullJobId() {
        assertThatThrownBy(() -> managementService.deleteJob(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("jobId is null");
    }

    @Test
    public void testDeleteJobUnexistingJob() {
        assertThatThrownBy(() -> managementService.deleteJob("unexistingjob"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("No job found with id");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/timerOnTask.bpmn20.xml" })
    public void testDeleteJobDeletion() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerOnTask");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(timerJob).as("Task timer should be there").isNotNull();
        managementService.deleteTimerJob(timerJob.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).as("There should be no job now. It was deleted").isNull();
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
        assertThatThrownBy(() -> managementService.deleteJob(timerJob.getId()))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("No job found with id");

        // Clean up
        managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(timerJob.getId());
    }

    // https://jira.codehaus.org/browse/ACT-1816:
    // ManagementService doesn't seem to give actual table Name for EventSubscriptionEntity.class
    @Test
    public void testGetTableName() {
        String table = managementService.getTableName(EventSubscriptionEntity.class, false);
        assertThat(table).isEqualTo("ACT_RU_EVENT_SUBSCR");
    }

    @Test
    void testAcquireAlreadyAcquiredLock() {
        String lockName = "testLock";
        LockManager testLockManager1 = managementService.getLockManager(lockName);

        assertThat(testLockManager1.acquireLock()).isTrue();
        // acquiring the lock from the same lock manager should always return true
        assertThat(testLockManager1.acquireLock()).isTrue();
        assertThat(managementService.getProperties()).containsKey(lockName);

        LockManager testLockManager2 = managementService.getLockManager(lockName);
        assertThat(testLockManager2.acquireLock()).isFalse();

        testLockManager1.releaseLock();
        assertThat(managementService.getProperties()).doesNotContainKey(lockName);

        assertThat(testLockManager2.acquireLock()).isTrue();
        assertThat(testLockManager1.acquireLock()).isFalse();

        assertThat(managementService.getProperties()).containsKey(lockName);
        testLockManager2.releaseLock();
        assertThat(managementService.getProperties()).doesNotContainKey(lockName);
    }

    @Test
    void testWaitForLock() {
        Duration initialLockPollRate = processEngineConfiguration.getLockPollRate();
        try {
            processEngineConfiguration.setLockPollRate(Duration.ofMillis(100));
            String lockName = "testWaitForLock";
            LockManager testLockManager1 = managementService.getLockManager(lockName);

            testLockManager1.waitForLock(Duration.ofMillis(100));
            assertThat(testLockManager1.acquireLock()).isTrue();
            // acquiring the lock from the same lock manager should always return true

            LockManager testLockManager2 = managementService.getLockManager(lockName);
            assertThatThrownBy(() -> testLockManager2.waitForLock(Duration.ofMillis(250)))
                    .isExactlyInstanceOf(FlowableException.class)
                    .hasMessageContaining("Could not acquire lock testWaitForLock. Current lock value:");
            assertThat(testLockManager2.acquireLock()).isFalse();

            testLockManager1.releaseLock();
            testLockManager2.waitForLock(Duration.ofSeconds(1));
            assertThat(testLockManager2.acquireLock()).isTrue();
            testLockManager2.releaseLock();
            assertThat(managementService.getProperties()).doesNotContainKey(lockName);
        } finally {
            processEngineConfiguration.setLockPollRate(initialLockPollRate);
        }
    }
}
