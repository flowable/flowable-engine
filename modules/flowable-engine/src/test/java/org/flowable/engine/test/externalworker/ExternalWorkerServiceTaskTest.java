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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.cmd.ClearProcessInstanceLockTimesCmd;
import org.flowable.engine.impl.jobexecutor.ExternalWorkerTaskCompleteJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.interceptor.CreateExternalWorkerJobAfterContext;
import org.flowable.engine.interceptor.CreateExternalWorkerJobBeforeContext;
import org.flowable.engine.interceptor.CreateExternalWorkerJobInterceptor;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
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
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
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
        assertThat(acquiredJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker").complete();

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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        managementService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker")
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
    void testExternalWorkerJobDeadLetterWithVariables() {
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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        managementService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker")
                .variable("name", "gonzo")
                .complete();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();
        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "kermit")
                );

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(ExternalWorkerTaskCompleteJobHandler.TYPE);

        managementService.moveJobToDeadLetterJob(job.getId());

        assertThat(managementService.createJobQuery().singleResult()).isNull();

        job = managementService.createDeadLetterJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(ExternalWorkerTaskCompleteJobHandler.TYPE);

        managementService.moveDeadLetterJobToExecutableJob(job.getId(), 3);

        waitForJobExecutorToProcessAllJobs(5000, 300);

        assertThat(taskService.createTaskQuery().list()).hasSize(1);

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("name", "gonzo")
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleSuspendProcessInstance() {
        ProcessInstance processInstance1 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();
        ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(managementService.createExternalWorkerJobQuery().list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(managementService.createSuspendedJobQuery().list()).isEmpty();

        runtimeService.suspendProcessInstanceById(processInstance1.getId());

        assertThat(managementService.createExternalWorkerJobQuery().list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsExactlyInAnyOrder(processInstance2.getId());

        assertThat(managementService.createSuspendedJobQuery().list())
                .extracting(Job::getProcessInstanceId)
                .containsExactlyInAnyOrder(processInstance1.getId());

        runtimeService.activateProcessInstanceById(processInstance1.getId());

        assertThat(managementService.createExternalWorkerJobQuery().list())
                .extracting(ExternalWorkerJob::getProcessInstanceId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(managementService.createSuspendedJobQuery().list()).isEmpty();
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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        managementService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker")
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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
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
        assertThat(acquiredJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");
        assertThat(acquiredJobs).isEmpty();

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker").complete();

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
    void testSimpleAcquireOnlyBpmn() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .onlyCmmn()
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .onlyBpmn()
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");
        assertThat(acquiredJobs).isEmpty();

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();

        waitForJobExecutorToProcessAllJobs(5000, 300);

        assertThat(taskService.createTaskQuery().list()).hasSize(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testSimpleAcquireByScopeType() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .scopeType(ScopeTypes.TASK)
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .scopeType(ScopeTypes.BPMN)
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");
        assertThat(acquiredJobs).isEmpty();

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNull();

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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "otherWorker").complete())
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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> managementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "otherWorker")
                .retries(3)
                .retryTimeout(Duration.ofMinutes(10))
                .fail())
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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "otherWorker").bpmnError("testError"))
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
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isEqualTo(Date.from(startTime.plus(30, ChronoUnit.MINUTES)));
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isEqualTo(Date.from(startTime.plus(30, ChronoUnit.MINUTES)));
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        Instant executionTime = startTime.plus(20, ChronoUnit.MINUTES);
        setTime(executionTime);
        managementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .retries(4)
                .retryTimeout(Duration.ofHours(1))
                .fail();

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(4);
        assertThat(externalWorkerJob.getLockExpirationTime()).isEqualTo(Date.from(executionTime.plus(1, ChronoUnit.HOURS)));
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(managementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId())).isEqualTo("Some complex error details");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNotNull();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
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

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getRetries()).isEqualTo(4);
        assertThat(acquiredJob.getLockExpirationTime()).isEqualTo(Date.from(resetTime.plus(10, ChronoUnit.MINUTES)));
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        managementService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker").complete();

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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        managementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .retries(0)
                .fail();

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
    void testSimpleFailureWithNoRetries() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        managementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .fail();

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries() - 1);
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(managementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId()))
                .isEqualTo("Some complex error details");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
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

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        managementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .retries(0)
                .fail();

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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        managementService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker").bpmnError("errorOne");

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

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        managementService.createExternalWorkerCompletionBuilder(acquiredJob.getId(), "testWorker")
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
        assertThatThrownBy(() -> managementService.createExternalWorkerJobAcquireBuilder().acquireAndLock(10, "someWorker"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("topic must not be empty");

        assertThatThrownBy(() -> managementService.createExternalWorkerJobAcquireBuilder().topic("simple", Duration.ofMinutes(10)).acquireAndLock(0, "someWorker"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("requested number of jobs must not be smaller than 1");

        assertThatThrownBy(() -> managementService.createExternalWorkerJobAcquireBuilder().topic("simple", Duration.ofMinutes(10)).acquireAndLock(10, null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("workerId must not be empty");

        assertThatThrownBy(() -> managementService.createExternalWorkerJobAcquireBuilder().onlyCmmn().onlyBpmn().acquireAndLock(10, null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot combine onlyCmmn() with onlyBpmn() in the same query");

        assertThatThrownBy(() -> managementService.createExternalWorkerJobAcquireBuilder().onlyBpmn().onlyCmmn().acquireAndLock(10, null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot combine onlyBpmn() with onlyCmmn() in the same query");

        assertThatThrownBy(() -> managementService.createExternalWorkerJobAcquireBuilder().scopeType(ScopeTypes.TASK).onlyBpmn().acquireAndLock(10, null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot combine scopeType(String) with onlyBpmn() in the same query");

        assertThatThrownBy(() -> managementService.createExternalWorkerJobAcquireBuilder().scopeType(ScopeTypes.TASK).onlyCmmn().acquireAndLock(10, null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Cannot combine scopeType(String) with onlyCmmn() in the same query");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testProcessInstanceIsCorrectlyLocked() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        ExecutionEntity processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(processInstance.getLockTime()).isEqualToIgnoringMillis(acquiredJob.getLockExpirationTime());

        managementService.executeCommand(new ClearProcessInstanceLockTimesCmd(processEngineConfiguration.getAsyncExecutor().getLockOwner()));

        // Clearing the async executor jobs times should not clear the ones which are locked by the external worker
        processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(processInstance.getLockTime()).isEqualToIgnoringMillis(acquiredJob.getLockExpirationTime());

        managementService.executeCommand(new ClearProcessInstanceLockTimesCmd("worker1"));

        // Clearing the worker1 jobs times should clear the ones which are locked by it
        processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isNull();
        assertThat(processInstance.getLockTime()).isNull();

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleNotExclusive.bpmn20.xml")
    void testProcessInstanceIsNotLockedByNotExclusiveJob() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        ExecutionEntity processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isNull();
        assertThat(processInstance.getLockTime()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.bpmn20.xml")
    void testProcessInstanceIsUnlockedWhenUnacquiringExclusiveJob() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        ExecutionEntity processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(processInstance.getLockTime()).isNotNull();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        assertThat(acquiredJobs).isEmpty();

        managementService.unacquireExternalWorkerJob(acquiredJob.getId(), "worker1");

        processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isNull();
        assertThat(processInstance.getLockTime()).isNull();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(processInstance.getLockTime()).isNotNull();

    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.bpmn20.xml")
    void testProcessInstanceIsUnlockedWhenUnacquiringAllExclusiveJobs() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        ExecutionEntity processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(processInstance.getLockTime()).isNotNull();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        assertThat(acquiredJobs).isEmpty();

        managementService.unacquireAllExternalWorkerJobsForWorker("worker1");

        processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isNull();
        assertThat(processInstance.getLockTime()).isNull();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        processInstance = (ExecutionEntity) runtimeService.createProcessInstanceQuery()
                .processInstanceId(acquiredJob.getProcessInstanceId())
                .singleResult();

        assertThat(processInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(processInstance.getLockTime()).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testCreateExternalWorkerJobInterceptor() {
        TestCreateExternalWorkerJobInterceptor interceptor = new TestCreateExternalWorkerJobInterceptor();
        processEngineConfiguration.setCreateExternalWorkerJobInterceptor(interceptor);

        try {
            runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("simpleExternalWorker")
                    .variable("name", "kermit")
                    .start();

            ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

            assertThat(externalWorkerJob).isNotNull();
            assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simpleTest");

            assertThat(interceptor.beforeCounter).isEqualTo(1);
            assertThat(interceptor.afterCounter).isEqualTo(1);
        } finally {
            processEngineConfiguration.setCreateExternalWorkerJobInterceptor(null);
        }

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    public void testAcquireForUserOrGroups() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();

        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();

        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();

        List<ExternalWorkerJob> jobs = managementService.createExternalWorkerJobQuery().list();
        assertThat(jobs).hasSize(3);

        ExternalWorkerJob onlyUserJob = jobs.get(0);
        ExternalWorkerJob onlyGroupJob = jobs.get(1);
        ExternalWorkerJob userAndGroupJob = jobs.get(2);

        addUserIdentityLinkToJob(onlyUserJob, "gonzo");
        addGroupIdentityLinkToJob(onlyGroupJob, "bears");
        addGroupIdentityLinkToJob(userAndGroupJob, "frogs");
        addUserIdentityLinkToJob(userAndGroupJob, "fozzie");

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups("kermit", Collections.singleton("muppets"))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups("gonzo", Collections.singleton("muppets"))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyUserJob.getId());

        managementService.createExternalWorkerJobFailureBuilder(onlyUserJob.getId(), "testWorker").fail();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups("fozzie", Collections.singleton("bears"))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyGroupJob.getId(), userAndGroupJob.getId());

        managementService.createExternalWorkerJobFailureBuilder(onlyGroupJob.getId(), "testWorker").fail();
        managementService.createExternalWorkerJobFailureBuilder(userAndGroupJob.getId(), "testWorker").fail();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups(null, Collections.singleton("bears"))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyGroupJob.getId());

        managementService.createExternalWorkerCompletionBuilder(onlyGroupJob.getId(), "testWorker").complete();

        assertThat(getJobIdentityLinks(onlyGroupJob)).isEmpty();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups("fozzie", Collections.emptyList())
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(userAndGroupJob.getId());

        managementService.createExternalWorkerCompletionBuilder(userAndGroupJob.getId(), "testWorker").bpmnError("errorCode");
        assertThat(getJobIdentityLinks(userAndGroupJob)).isEmpty();

        assertThat(getJobIdentityLinks(onlyUserJob))
                .extracting(IdentityLink::getUserId)
                .containsOnly("gonzo");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    public void testAcquireByTenantId() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .overrideProcessDefinitionTenantId("flowable")
                .start();

        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .overrideProcessDefinitionTenantId("acme")
                .start();

        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();

        List<ExternalWorkerJob> jobs = managementService.createExternalWorkerJobQuery().list();
        assertThat(jobs).hasSize(3);

        ExternalWorkerJob flowableJob = managementService.createExternalWorkerJobQuery().jobTenantId("flowable").singleResult();
        ExternalWorkerJob acmeJob = managementService.createExternalWorkerJobQuery().jobTenantId("acme").singleResult();
        ExternalWorkerJob noTenantJob = managementService.createExternalWorkerJobQuery().jobWithoutTenantId().singleResult();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .tenantId("megacorp")
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .tenantId("flowable")
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(flowableJob.getId());
        managementService.createExternalWorkerJobFailureBuilder(flowableJob.getId(), "testWorker").fail();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .tenantId("acme")
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(acmeJob.getId());

        managementService.createExternalWorkerJobFailureBuilder(acmeJob.getId(), "testWorker").fail();

        acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .tenantId("")
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(noTenantJob.getId());

        managementService.createExternalWorkerJobFailureBuilder(noTenantJob.getId(), "testWorker").fail();
    }

    @Test
    @Deployment
    void testWithLimitedVariables() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("anotherVar", "secretContent")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("theName", "kermit")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker")
                .variable("theResult", "some result")
                .variable("theSecretResult", "secret result")
                .complete();

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

        assertThat(runtimeService.getVariable(processInstance.getId(), "result")).isEqualTo("some result");
        assertThat(runtimeService.getVariable(processInstance.getId(), "theResult")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "theSecretResult")).isNull();
    }

    @Test
    @Deployment
    void testWithMultipleVariableMapping() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("description", "kermit is king")
                .variable("anotherVar", "secretContent")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("theName", "kermit"),
                        entry("theDescription", "kermit is king")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker")
                .variable("theResult1", "r1")
                .variable("theResult2", "r2")
                .variable("theResult3", "r3")
                .variable("theSecretResult", "secret result")
                .complete();

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

        assertThat(runtimeService.getVariable(processInstance.getId(), "result1")).isEqualTo("r1");
        assertThat(runtimeService.getVariable(processInstance.getId(), "result2")).isEqualTo("r2");
        assertThat(runtimeService.getVariable(processInstance.getId(), "result3")).isEqualTo("r3");
        assertThat(runtimeService.getVariable(processInstance.getId(), "theSecretResult")).isNull();
    }

    @Test
    @Deployment
    void testWithChangingVariables() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("anotherVar", "secretContent")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        runtimeService.setVariable(processInstance.getId(), "name", "ernie");

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("theName", "ernie")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker").complete();

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
    @Deployment
    void testWithNoInputVariables() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("anotherVar", "secretContent")
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        runtimeService.setVariable(processInstance.getId(), "name", "ernie");

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables()).isEmpty();

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker").complete();

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
    @Deployment
    void testWithExpressions() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("anotherVar", 10)
                .variable("anotherVar2", 32)
                .start();

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        runtimeService.setVariable(processInstance.getId(), "name", "ernie");

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables()).containsOnly(
                entry("theSum", 42L),
                entry("theProduct", 320L)
        );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(processEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        managementService.createExternalWorkerCompletionBuilder(externalWorkerJob.getId(), "testWorker")
                .variable("resultVar1", 25)
                .variable("resultVar2", 35)
                .complete();

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

        assertThat(runtimeService.getVariable(processInstance.getId(), "sum")).isEqualTo(60L);
        assertThat(runtimeService.getVariable(processInstance.getId(), "product")).isEqualTo(875L);
    }

    protected void addUserIdentityLinkToJob(Job job, String userId) {
        managementService.executeCommand(commandContext -> {
                    processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                            .createScopeIdentityLink(null, job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER, userId, null, IdentityLinkType.PARTICIPANT);

                    return null;
                });
    }

    protected void addGroupIdentityLinkToJob(Job job, String groupId) {
        managementService.executeCommand(commandContext -> {
                    processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                            .createScopeIdentityLink(null, job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER, null, groupId, IdentityLinkType.PARTICIPANT);
                    return null;
                });
    }

    protected Collection<IdentityLinkEntity> getJobIdentityLinks(Job job) {
        return managementService.executeCommand(commandContext -> processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                        .findIdentityLinksByScopeIdAndType(job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER));
    }

    protected void setTime(Instant time) {
        processEngineConfiguration.getClock().setCurrentTime(Date.from(time));
    }

    protected static class TestCreateExternalWorkerJobInterceptor implements CreateExternalWorkerJobInterceptor {

        protected int beforeCounter = 0;
        protected int afterCounter = 0;

        @Override
        public void beforeCreateExternalWorkerJob(CreateExternalWorkerJobBeforeContext beforeContext) {
            beforeCounter++;
            beforeContext.setJobTopicExpression(beforeContext.getExternalWorkerServiceTask().getTopic() + "Test");
        }

        @Override
        public void afterCreateExternalWorkerJob(CreateExternalWorkerJobAfterContext createExternalWorkerJobAfterContext) {
            afterCounter++;
        }
    }
}
