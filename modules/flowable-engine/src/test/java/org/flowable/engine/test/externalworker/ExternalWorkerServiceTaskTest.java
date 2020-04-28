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
package org.flowable.engine.test.externalworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.task.api.TaskInfo;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerServiceTaskTest extends PluggableFlowableTestCase {

    private boolean asyncExecutorActivated;

    /*
        Need to disable the async executor during this test, as otherwise jobs will be picked up
        which will make it impossible to test the lock releasing logic.
     */

    @BeforeEach
    public void disableAsyncExecutorIfNeeded() {
        asyncExecutorActivated = processEngineConfiguration.getAsyncExecutor().isActive();

        if (asyncExecutorActivated) {
            processEngineConfiguration.getAsyncExecutor().shutdown();
        }
    }

    @AfterEach
    public void enabledAsyncExecutorIfNeeded() {
        if (asyncExecutorActivated) {
            processEngineConfiguration.getAsyncExecutor().start();
        }
    }

    @Test
    @Deployment
    void testSimple() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(1);
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(1);
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(1);
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        runtimeService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = managementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs(5000, 300);

        assertThat(taskService.createTaskQuery().list()).hasSize(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleWithVariables() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "kermit")
                );

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        runtimeService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker")
                .variable("name", "gonzo")
                .complete();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();
        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "kermit")
                );

        waitForJobExecutorToProcessAllJobs(5000, 300);

        assertThat(taskService.createTaskQuery().list()).hasSize(1);

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "gonzo")
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testExternalWorkerVariablesShouldBeDeletedWhenProcessInstancesIsCanceled() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "kermit")
                );

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        runtimeService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker")
                .variable("name", "gonzo")
                .complete();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();
        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "kermit")
                );

        runtimeService.deleteProcessInstance(processInstance.getId(), "deletion for test");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricVariableInstanceQuery().list())
                    .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                    .containsExactlyInAnyOrder(
                            tuple("name", "kermit")
                    );
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleAcquireMultipleTimes() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(1);
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");
        assertThat(acquiredJobs).isEmpty();

        runtimeService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = managementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs(5000, 300);

        assertThat(taskService.createTaskQuery().list()).hasSize(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleCompleteByDifferentWorker() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> runtimeService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "otherWorker").complete())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("otherWorker does not hold a lock on the requested job");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleFailureByDifferentWorker() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> runtimeService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "otherWorker")
                .failure(3, Duration.of(10, ChronoUnit.MINUTES)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("otherWorker does not hold a lock on the requested job");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleBpmnErrorByDifferentWorker() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> runtimeService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "otherWorker").bpmnError("testError"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("otherWorker does not hold a lock on the requested job");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleFailure() {
        Instant startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        setTime(startTime);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(1);
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(1);
        assertThat(externalWorkerJob.getLockExpirationTime()).isEqualTo(Date.from(startTime.plus(30, ChronoUnit.MINUTES)));
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getRetries()).isEqualTo(1);
        assertThat(acquiredJob.getLockExpirationTime()).isEqualTo(Date.from(startTime.plus(30, ChronoUnit.MINUTES)));
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        Instant executionTime = startTime.plus(20, ChronoUnit.MINUTES);
        setTime(executionTime);
        runtimeService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .failure(4, Duration.ofHours(1));

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(4);
        assertThat(externalWorkerJob.getLockExpirationTime()).isEqualTo(Date.from(executionTime.plus(1, ChronoUnit.HOURS)));
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(managementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId())).isEqualTo("Some complex error details");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNotNull();

        acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        Instant resetTime = executionTime.plus(2, ChronoUnit.HOURS);
        setTime(resetTime);

        waitForJobExecutorOnCondition(5000, 300, () -> managementService.createExternalWorkerJobQuery().singleResult().getLockExpirationTime() == null);

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(4);
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Failed to run job");

        acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getRetries()).isEqualTo(4);
        assertThat(acquiredJob.getLockExpirationTime()).isEqualTo(Date.from(resetTime.plus(10, ChronoUnit.MINUTES)));
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        runtimeService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker").complete();

        Job executableJob = managementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs(5000, 300);

        assertThat(taskService.createTaskQuery().list()).hasSize(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleFailureWithZeroRetries() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        runtimeService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .failure(0, null);

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNull();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job deadLetterJob = managementService.createDeadLetterJobQuery().singleResult();

        assertThat(deadLetterJob).isNotNull();
        assertThat(deadLetterJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(deadLetterJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(managementService.getDeadLetterJobExceptionStacktrace(deadLetterJob.getId()))
                .isEqualTo("Some complex error details");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleFailureMoveFromDeadLetter() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        runtimeService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .failure(0, null);

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNull();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job deadLetterJob = managementService.createDeadLetterJobQuery().singleResult();

        assertThat(deadLetterJob).isNotNull();
        assertThat(deadLetterJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(deadLetterJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(managementService.getDeadLetterJobExceptionStacktrace(deadLetterJob.getId()))
                .isEqualTo("Some complex error details");

        Job movedJob = managementService.moveDeadLetterJobToExecutableJob(deadLetterJob.getId(), 4);

        assertThat(movedJob).isNotNull();
        assertThat(movedJob).isInstanceOf(ExternalWorkerJob.class);
        assertThat(movedJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(movedJob.getRetries()).isEqualTo(4);

        assertThat(managementService.createJobQuery().list()).isEmpty();

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getId()).isEqualTo(movedJob.getId());

    }

    @Test
    @Deployment
    void testSimpleWithBoundaryError() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorkerWithError")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        runtimeService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker").bpmnError("errorOne");

        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();

        waitForJobExecutorToProcessAllJobs(5000, 300);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    void testSimpleWithBoundaryErrorAndVariables() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorkerWithError")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "kermit")
                );

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = runtimeService.createExternalWorkerProvider()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        runtimeService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker")
                .variable("name", "gonzo")
                .bpmnError("errorOne");

        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();
        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "kermit")
                );

        waitForJobExecutorToProcessAllJobs(5000, 300);

        assertThat(taskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskAfterError");

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "gonzo")
                );
    }

    @Test
    void testAcquireWithInvalidArguments() {
        assertThatThrownBy(() -> runtimeService.createExternalWorkerProvider().acquireAndLock(10, "someWorker"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("topic must not be empty");

        assertThatThrownBy(() -> runtimeService.createExternalWorkerProvider().topic("simple", Duration.ofMinutes(10)).acquireAndLock(0, "someWorker"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("requested number of jobs must not be smaller than 1");

        assertThatThrownBy(() -> runtimeService.createExternalWorkerProvider().topic("simple", Duration.ofMinutes(10)).acquireAndLock(10, null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("workerId must not be empty");
    }

    protected List<ByteArrayEntity> allByteArrays() {
        return managementService.executeCommand(commandContext -> {
            return CommandContextUtil.getByteArrayEntityManager(commandContext).findAll();
        });
    }

    protected void setTime(Instant time) {
        processEngineConfiguration.getClock().setCurrentTime(Date.from(time));
    }
}
