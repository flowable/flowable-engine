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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.lock.LockManager;
import org.flowable.common.engine.impl.lock.LockManagerImpl;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobNotFoundException;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.cmd.AcquireJobsCmd;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsCmd;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.Assert;
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
        assertThat(timerJob2.getExceptionMessage())
                .contains("This is an exception thrown from scriptTask");

        // Get the full stacktrace using the managementService
        String exceptionStack = managementService.getTimerJobExceptionStacktrace(timerJob2.getId());
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
        assertThat(asyncJob.getCorrelationId()).isNotNull();

        String correlationId = asyncJob.getCorrelationId();
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
        assertThat(asyncJob.getCorrelationId()).isEqualTo(correlationId);

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
        assertThat(asyncJob.getCorrelationId()).isEqualTo(correlationId);

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
        assertThat(asyncJob.getCorrelationId()).isEqualTo(correlationId);

        managementService.moveDeadLetterJobToExecutableJob(asyncJob.getId(), 5);

        asyncJob = managementService.createJobQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(asyncJob.getElementId()).isEqualTo("theScriptTask");
        assertThat(asyncJob.getElementName()).isEqualTo("Execute script");
        assertThat(asyncJob.getCorrelationId()).isEqualTo(correlationId);

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

        // Move the timer to an executable job
        managementService.moveTimerToExecutableJob(timerJob.getId());

        // Acquire job by running the acquire command manually
        AcquireJobsCmd acquireJobsCmd = new AcquireJobsCmd(processEngine.getProcessEngineConfiguration().getAsyncExecutor(), 5,
                processEngineConfiguration.getJobServiceConfiguration().getJobEntityManager());
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(acquireJobsCmd);

        // Try to delete the job. This should fail.
        assertThatThrownBy(() -> managementService.deleteJob(timerJob.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("Cannot delete job when the job is being executed. Try again later.");

        // Clean up
        managementService.executeJob(timerJob.getId());
    }


    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/mgmt/timerOnTask.bpmn20.xml" })
    public void testDeleteTimerJobThatWasAlreadyAcquired() {
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
        assertThatThrownBy(() -> managementService.deleteTimerJob(timerJob.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("Cannot delete timer job when the job is being executed. Try again later.");

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
        try {
            LockManager testLockManager1 = managementService.getLockManager(lockName);

            assertThat(testLockManager1.acquireLock()).isTrue();
            // acquiring the lock from the same lock manager should always return true
            assertThat(testLockManager1.acquireLock()).isTrue();
            Map<String, String> properties = managementService.getProperties();
            assertThat(properties).containsKey(lockName);
            assertThat(properties.get(lockName)).isNotNull();

            LockManager testLockManager2 = managementService.getLockManager(lockName);
            assertThat(testLockManager2.acquireLock()).isFalse();

            testLockManager1.releaseLock();
            properties = managementService.getProperties();
            assertThat(properties).containsEntry(lockName, null);

            assertThat(testLockManager2.acquireLock()).isTrue();
            assertThat(testLockManager1.acquireLock()).isFalse();

            properties = managementService.getProperties();
            assertThat(properties).containsKey(lockName);
            assertThat(properties.get(lockName)).isNotNull();
            testLockManager2.releaseLock();
            properties = managementService.getProperties();
            assertThat(properties).containsEntry(lockName, null);
        } finally {
            deletePropertyIfExists(lockName);
        }
    }

    @Test
    void testWaitForLock() {
        Duration initialLockPollRate = processEngineConfiguration.getLockPollRate();
        String lockName = "testWaitForLock";
        try {
            processEngineConfiguration.setLockPollRate(Duration.ofMillis(100));
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
            assertThat(managementService.getProperties())
                    .containsEntry(lockName, null);
        } finally {
            processEngineConfiguration.setLockPollRate(initialLockPollRate);
            deletePropertyIfExists(lockName);
        }
    }

    @Test
    void testAcquireExpiredAcquiredLock() {
        String lockName = "testLock";
        try {
            Instant startTime = Instant.now();
            LockManager testLockManager1 = new LockManagerImpl(processEngineConfiguration.getCommandExecutor(), lockName, Duration.ofMinutes(1), processEngineConfiguration.getEngineCfgKey());

            assertThat(testLockManager1.acquireLock()).isTrue();
            // acquiring the lock from the same lock manager should always return true
            assertThat(testLockManager1.acquireLock()).isTrue();
            Map<String, String> properties = managementService.getProperties();
            assertThat(properties).containsKey(lockName);
            assertThat(properties.get(lockName)).isNotNull();

            String updatedPropertyValue = startTime.minus(2, ChronoUnit.HOURS).toString();
            updatePropertyValue(lockName, updatedPropertyValue);

            LockManager testLockManager2 = managementService.getLockManager(lockName);
            assertThat(testLockManager2.acquireLock()).isFalse();

            testLockManager2 = new LockManagerImpl(processEngineConfiguration.getCommandExecutor(), lockName, Duration.ofMinutes(1), Duration.ofHours(3), processEngineConfiguration.getEngineCfgKey());
            assertThat(testLockManager2.acquireLock()).isFalse();

            properties = managementService.getProperties();
            assertThat(properties.get(lockName)).isEqualTo(updatedPropertyValue);

            testLockManager2 = new LockManagerImpl(processEngineConfiguration.getCommandExecutor(), lockName, Duration.ofMinutes(1), Duration.ofHours(1), processEngineConfiguration.getEngineCfgKey());
            assertThat(testLockManager2.acquireLock()).isTrue();

            properties = managementService.getProperties();
            assertThat(properties.get(lockName)).isNotEqualTo(updatedPropertyValue);

        } finally {
            deletePropertyIfExists(lockName);
        }
    }

    @Test
    void testAcquireExpiredAcquiredLockWithZInTheHostName() {
        String lockName = "testLock";
        try {
            Instant startTime = Instant.now();
            LockManager testLockManager1 = new LockManagerImpl(processEngineConfiguration.getCommandExecutor(), lockName, Duration.ofMinutes(1), processEngineConfiguration.getEngineCfgKey());

            assertThat(testLockManager1.acquireLock()).isTrue();
            // acquiring the lock from the same lock manager should always return true
            assertThat(testLockManager1.acquireLock()).isTrue();
            Map<String, String> properties = managementService.getProperties();
            assertThat(properties).containsKey(lockName);
            assertThat(properties.get(lockName)).isNotNull();

            String updatedPropertyValue = startTime.minus(2, ChronoUnit.HOURS).toString() + " - Zulu";
            updatePropertyValue(lockName, updatedPropertyValue);

            LockManager testLockManager2 = managementService.getLockManager(lockName);
            assertThat(testLockManager2.acquireLock()).isFalse();

            testLockManager2 = new LockManagerImpl(processEngineConfiguration.getCommandExecutor(), lockName, Duration.ofMinutes(1), Duration.ofHours(3), processEngineConfiguration.getEngineCfgKey());
            assertThat(testLockManager2.acquireLock()).isFalse();

            properties = managementService.getProperties();
            assertThat(properties.get(lockName)).isEqualTo(updatedPropertyValue);

            testLockManager2 = new LockManagerImpl(processEngineConfiguration.getCommandExecutor(), lockName, Duration.ofMinutes(1), Duration.ofHours(1), processEngineConfiguration.getEngineCfgKey());
            assertThat(testLockManager2.acquireLock()).isTrue();

            properties = managementService.getProperties();
            assertThat(properties.get(lockName)).isNotEqualTo(updatedPropertyValue);

        } finally {
            deletePropertyIfExists(lockName);
        }
    }

    @Test
    void testFindJobByCorrelationId() {
        Job asyncJob = managementService.executeCommand(context -> {
            JobService jobService = CommandContextUtil.getJobService(context);
            JobEntity job = jobService.createJob();
            job.setJobType("testAsync");
            jobService.insertJob(job);
            return job;
        });

        Job timerJob = managementService.executeCommand(context -> {
            TimerJobService timerJobService = CommandContextUtil.getTimerJobService(context);
            TimerJobEntity job = timerJobService.createTimerJob();
            job.setJobType("testTimer");
            timerJobService.insertTimerJob(job);
            return job;
        });

        Job deadLetterJob = managementService.executeCommand(context -> {
            JobService jobService = CommandContextUtil.getJobService(context);
            DeadLetterJobEntity job = jobService.createDeadLetterJob();
            job.setJobType("testDeadLetter");
            jobService.insertDeadLetterJob(job);
            return job;
        });

        Job suspendedJob = managementService.executeCommand(context -> {
            SuspendedJobEntity job = processEngineConfiguration.getJobServiceConfiguration()
                    .getSuspendedJobEntityManager()
                    .create();
            job.setJobType("testSuspended");
            processEngineConfiguration.getJobServiceConfiguration().getSuspendedJobEntityManager().insert(job);
            return job;
        });


        Job externalWorkerJob = managementService.executeCommand(context -> {
            JobService jobService = CommandContextUtil.getJobService(context);
            ExternalWorkerJobEntity job = jobService.createExternalWorkerJob();
            job.setJobType("testExternal");
            jobService.insertExternalWorkerJob(job);
            return job;
        });

        Job job = managementService.findJobByCorrelationId(asyncJob.getCorrelationId());
        assertThat(job).isNotNull();
        assertThat(job.getJobType()).isEqualTo("testAsync");
        assertThat(job).isInstanceOf(JobEntity.class);

        job = managementService.findJobByCorrelationId(timerJob.getCorrelationId());
        assertThat(job).isNotNull();
        assertThat(job.getJobType()).isEqualTo("testTimer");
        assertThat(job).isInstanceOf(TimerJobEntity.class);

        job = managementService.findJobByCorrelationId(deadLetterJob.getCorrelationId());
        assertThat(job).isNotNull();
        assertThat(job.getJobType()).isEqualTo("testDeadLetter");
        assertThat(job).isInstanceOf(DeadLetterJobEntity.class);

        job = managementService.findJobByCorrelationId(suspendedJob.getCorrelationId());
        assertThat(job).isNotNull();
        assertThat(job.getJobType()).isEqualTo("testSuspended");
        assertThat(job).isInstanceOf(SuspendedJobEntity.class);

        job = managementService.findJobByCorrelationId(externalWorkerJob.getCorrelationId());
        assertThat(job).isNotNull();
        assertThat(job.getJobType()).isEqualTo("testExternal");
        assertThat(job).isInstanceOf(ExternalWorkerJobEntity.class);

        job = managementService.findJobByCorrelationId("unknown");
        assertThat(job).isNull();

        managementService.deleteJob(asyncJob.getId());
        managementService.deleteTimerJob(timerJob.getId());
        managementService.deleteDeadLetterJob(deadLetterJob.getId());
        managementService.deleteSuspendedJob(suspendedJob.getId());
        managementService.deleteExternalWorkerJob(externalWorkerJob.getId());
    }

    @Test
    void testMoveDeadLetterJobToInvalidHistoryJob() {
        for (String jobType : Arrays.asList(JobEntity.JOB_TYPE_MESSAGE, JobEntity.JOB_TYPE_TIMER, JobEntity.JOB_TYPE_EXTERNAL_WORKER)) {
            Job deadLetterJob = managementService.executeCommand(context -> {
                JobService jobService = CommandContextUtil.getProcessEngineConfiguration(context).getJobServiceConfiguration().getJobService();
                DeadLetterJobEntity job = jobService.createDeadLetterJob();
                job.setJobType(jobType);
                jobService.insertDeadLetterJob(job);
                return job;
            });

            try {
                managementService.moveDeadLetterJobToHistoryJob(deadLetterJob.getId(), 3);
                Assert.fail();
            } catch (FlowableIllegalArgumentException e) { }

            managementService.deleteDeadLetterJob(deadLetterJob.getId());
        }

    }

    protected void deletePropertyIfExists(String propertyName) {
        managementService.executeCommand(commandContext -> {
            PropertyEntityManager propertyEntityManager = CommandContextUtil.getPropertyEntityManager(commandContext);
            PropertyEntity property = propertyEntityManager.findById(propertyName);
            if (property != null) {
                propertyEntityManager.delete(property);
            }
            return null;
        });
    }

    protected void updatePropertyValue(String propertyName, String propertyValue) {
        managementService.executeCommand(commandContext -> {
            PropertyEntityManager propertyEntityManager = CommandContextUtil.getPropertyEntityManager(commandContext);
            PropertyEntity property = propertyEntityManager.findById(propertyName);
            if (property == null) {
                throw new FlowableObjectNotFoundException("Property with name " + propertyName + " does not exist");
            }
            property.setValue(propertyValue);
            propertyEntityManager.update(property);
            return null;
        });
    }

}
