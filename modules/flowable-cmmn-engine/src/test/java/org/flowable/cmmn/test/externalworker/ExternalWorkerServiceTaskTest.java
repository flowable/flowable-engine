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
package org.flowable.cmmn.test.externalworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.cmd.ClearCaseInstanceLockTimesCmd;
import org.flowable.cmmn.engine.impl.job.ExternalWorkerTaskCompleteJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.interceptor.CreateCmmnExternalWorkerJobAfterContext;
import org.flowable.cmmn.engine.interceptor.CreateCmmnExternalWorkerJobBeforeContext;
import org.flowable.cmmn.engine.interceptor.CreateCmmnExternalWorkerJobInterceptor;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.task.api.TaskInfo;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerServiceTaskTest extends FlowableCmmnTestCase {

    private boolean asyncExecutorActivated;

    /*
        Need to disable the async executor during this test, as otherwise jobs will be picked up
        which will make it impossible to test the lock releasing logic.
     */

    @BeforeEach
    public void disableAsyncExecutorIfNeeded() {
        asyncExecutorActivated = cmmnEngineConfiguration.getAsyncExecutor().isActive();

        if (asyncExecutorActivated) {
            cmmnEngineConfiguration.getAsyncExecutor().shutdown();
        }
    }

    @AfterEach
    public void enabledAsyncExecutorIfNeeded() {
        if (asyncExecutorActivated) {
            cmmnEngineConfiguration.getAsyncExecutor().start();
        }
    }

    @Test
    @CmmnDeployment
    public void testSimple() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(acquiredJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = cmmnManagementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(executableJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleSimpleWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("var", "Initial")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Initial")
                );

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(acquiredJob.getId(), "testWorker")
                .variable("var", "Complete")
                .complete();

        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Initial")
                );
        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Complete")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testExternalWorkerJobDeadLetterWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("var", "Initial")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Initial")
                );

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(acquiredJob.getId(), "testWorker")
                .variable("var", "Complete")
                .complete();

        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Initial")
                );

        Job job = cmmnManagementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(ExternalWorkerTaskCompleteJobHandler.TYPE);

        cmmnManagementService.moveJobToDeadLetterJob(job.getId());

        assertThat(cmmnManagementService.createJobQuery().singleResult()).isNull();

        job = cmmnManagementService.createDeadLetterJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(ExternalWorkerTaskCompleteJobHandler.TYPE);

        cmmnManagementService.moveDeadLetterJobToExecutableJob(job.getId(), 3);

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Complete")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleAcquireMultipleTimes() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(acquiredJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");
        assertThat(acquiredJobs).isEmpty();

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = cmmnManagementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(executableJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleAcquireOnlyCmmn() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .onlyBpmn()
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .onlyCmmn()
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");
        assertThat(acquiredJobs).isEmpty();

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleAcquireByScopeType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .scopeType(ScopeTypes.TASK)
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .scopeType(ScopeTypes.CMMN)
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");
        assertThat(acquiredJobs).isEmpty();

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleCompleteByDifferentWorker() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "otherWorker").complete())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("otherWorker does not hold a lock on the requested job");

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleFailureByDifferentWorker() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> cmmnManagementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "otherWorker")
                .retries(3)
                .retryTimeout(Duration.ofMinutes(10))
                .fail())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("otherWorker does not hold a lock on the requested job");

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleTerminateByDifferentWorker() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        assertThatThrownBy(() -> cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "otherWorker").terminate())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("otherWorker does not hold a lock on the requested job");

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleFailure() {
        Instant startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        setTime(startTime);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isEqualTo(Date.from(startTime.plus(30, ChronoUnit.MINUTES)));
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        assertThat(acquiredJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isEqualTo(Date.from(startTime.plus(30, ChronoUnit.MINUTES)));
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        Instant executionTime = startTime.plus(20, ChronoUnit.MINUTES);
        setTime(executionTime);
        cmmnManagementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .retries(4)
                .retryTimeout(Duration.ofHours(1))
                .fail();

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(4);
        assertThat(externalWorkerJob.getLockExpirationTime()).isEqualTo(Date.from(executionTime.plus(1, ChronoUnit.HOURS)));
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(cmmnManagementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId())).isEqualTo("Some complex error details");

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNotNull();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        Instant resetTime = executionTime.plus(2, ChronoUnit.HOURS);
        setTime(resetTime);

        waitForJobExecutorOnCondition(() -> cmmnManagementService.createExternalWorkerJobQuery().singleResult().getLockExpirationTime() == null);

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(4);
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Failed to run job");

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getRetries()).isEqualTo(4);
        assertThat(acquiredJob.getLockExpirationTime()).isEqualTo(Date.from(resetTime.plus(10, ChronoUnit.MINUTES)));
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(acquiredJob.getId(), "testWorker").complete();

        Job executableJob = cmmnManagementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(executableJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleFailureWithZeroRetries() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        cmmnManagementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .retries(0)
                .fail();

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNull();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job deadLetterJob = cmmnManagementService.createDeadLetterJobQuery().singleResult();

        assertThat(deadLetterJob).isNotNull();
        assertThat(deadLetterJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(deadLetterJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(cmmnManagementService.getDeadLetterJobExceptionStacktrace(deadLetterJob.getId()))
                .isEqualTo("Some complex error details");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleFailureWithNoRetries() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("name", "kermit")
                );

        cmmnManagementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .fail();

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries() - 1);
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(cmmnManagementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId()))
                .isEqualTo("Some complex error details");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleFailureMoveFromDeadLetter() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        cmmnManagementService.createExternalWorkerJobFailureBuilder(externalWorkerJob.getId(), "testWorker")
                .errorMessage("Failed to run job")
                .errorDetails("Some complex error details")
                .retries(0)
                .fail();

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNull();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job deadLetterJob = cmmnManagementService.createDeadLetterJobQuery().singleResult();

        assertThat(deadLetterJob).isNotNull();
        assertThat(deadLetterJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(deadLetterJob.getExceptionMessage()).isEqualTo("Failed to run job");
        assertThat(cmmnManagementService.getDeadLetterJobExceptionStacktrace(deadLetterJob.getId()))
                .isEqualTo("Some complex error details");

        Job movedJob = cmmnManagementService.moveDeadLetterJobToExecutableJob(deadLetterJob.getId(), 4);

        assertThat(movedJob).isInstanceOf(ExternalWorkerJob.class);
        assertThat(movedJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(movedJob.getRetries()).isEqualTo(4);

        assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getId()).isEqualTo(movedJob.getId());

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleTerminate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(acquiredJob.getId(), "testWorker").terminate();

        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerTerminateTask");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testSimpleTerminateWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("var", "Initial")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Initial")
                );

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(acquiredJob.getId(), "testWorker")
                .variable("var", "Terminate")
                .terminate();

        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Initial")
                );
        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(TaskInfo::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerTerminateTask");

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Terminate")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testExternalWorkerVariablesShouldBeDeletedWhenCaseInstanceIsTerminated() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("var", "Initial")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Initial")
                );

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceId(externalWorkerJob.getSubScopeId())
                .singleResult();

        assertThat(planItemInstance).isNotNull();

        assertThat(getExternalWorkerVariablesForPlanItemInstance(planItemInstance.getId())).isEmpty();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(acquiredJob.getId(), "testWorker")
                .variable("var", "Terminate")
                .terminate();

        assertThat(getExternalWorkerVariablesForPlanItemInstance(planItemInstance.getId()))
                .containsOnly(
                        entry("var", "Terminate")
                );

        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var", "Initial")
                );
        cmmnRuntimeService.terminateCaseInstance(planItemInstance.getCaseInstanceId());
        assertThat(getExternalWorkerVariablesForPlanItemInstance(planItemInstance.getId())).isEmpty();
    }

    @Test
    public void testAcquireWithInvalidArguments() {
        assertThatThrownBy(() -> cmmnManagementService.createExternalWorkerJobAcquireBuilder().acquireAndLock(10, "someWorker"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("topic must not be empty");

        assertThatThrownBy(() -> cmmnManagementService.createExternalWorkerJobAcquireBuilder().topic("simple", Duration.ofMinutes(10)).acquireAndLock(0, "someWorker"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("requested number of jobs must not be smaller than 1");

        assertThatThrownBy(() -> cmmnManagementService.createExternalWorkerJobAcquireBuilder().topic("simple", Duration.ofMinutes(10)).acquireAndLock(10, null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("workerId must not be empty");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testCaseInstanceIsCorrectlyLocked() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        CaseInstanceEntity caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(caseInstance.getLockTime()).isEqualToIgnoringMillis(acquiredJob.getLockExpirationTime());

        cmmnEngineConfiguration.getCommandExecutor().execute(new ClearCaseInstanceLockTimesCmd(
                cmmnEngineConfiguration.getAsyncExecutor().getLockOwner(), cmmnEngineConfiguration));

        // Clearing the async executor jobs times should not clear the ones which are locked by the external worker
        caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(caseInstance.getLockTime()).isEqualToIgnoringMillis(acquiredJob.getLockExpirationTime());

        cmmnEngineConfiguration.getCommandExecutor().execute(new ClearCaseInstanceLockTimesCmd("worker1", cmmnEngineConfiguration));

        // Clearing the worker1 jobs times should not clear the ones which are locked by the external worker
        caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isNull();
        assertThat(caseInstance.getLockTime()).isNull();

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleNotExclusive.cmmn")
    public void testCaseInstanceIsNotLockedByNotExclusiveJob() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        CaseInstanceEntity caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isNull();
        assertThat(caseInstance.getLockTime()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.cmmn")
    public void testCaseInstanceIsUnlockedWhenUnacquiringExclusiveJob() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        CaseInstanceEntity caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(caseInstance.getLockTime()).isNotNull();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        assertThat(acquiredJobs).isEmpty();

        cmmnManagementService.unacquireExternalWorkerJob(acquiredJob.getId(), "worker1");

        caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isNull();
        assertThat(caseInstance.getLockTime()).isNull();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(caseInstance.getLockTime()).isNotNull();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimpleExclusive.cmmn")
    public void testCaseInstanceIsUnlockedWhenUnacquiringAllExclusiveJobs() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .start();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        CaseInstanceEntity caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(caseInstance.getLockTime()).isNotNull();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        assertThat(acquiredJobs).isEmpty();

        cmmnManagementService.unacquireAllExternalWorkerJobsForWorker("worker1");

        caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isNull();
        assertThat(caseInstance.getLockTime()).isNull();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofHours(1))
                .acquireAndLock(1, "worker1");

        acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob.getLockOwner()).isEqualTo("worker1");
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();

        caseInstance = (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(acquiredJob.getScopeId())
                .singleResult();

        assertThat(caseInstance.getLockOwner()).isEqualTo("worker1");
        assertThat(caseInstance.getLockTime()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testCreateCmmnExternalWorkerJobInterceptor() {
        TestCreateCmmnExternalWorkerJobInterceptor interceptor = new TestCreateCmmnExternalWorkerJobInterceptor();
        cmmnEngineConfiguration.setCreateCmmnExternalWorkerJobInterceptor(interceptor);

        try {
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("simpleExternalWorker")
                    .variable("name", "kermit")
                    .start();

            ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

            assertThat(externalWorkerJob).isNotNull();
            assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simpleTest");

            assertThat(interceptor.beforeCounter).isEqualTo(1);
            assertThat(interceptor.afterCounter).isEqualTo(1);
        } finally {
            cmmnEngineConfiguration.setCreateCmmnExternalWorkerJobInterceptor(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testAcquireForUserOrGroups() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        List<ExternalWorkerJob> jobs = cmmnManagementService.createExternalWorkerJobQuery().list();
        assertThat(jobs).hasSize(3);

        ExternalWorkerJob onlyUserJob = jobs.get(0);
        ExternalWorkerJob onlyGroupJob = jobs.get(1);
        ExternalWorkerJob userAndGroupJob = jobs.get(2);

        addUserIdentityLinkToJob(onlyUserJob, "gonzo");
        addGroupIdentityLinkToJob(onlyGroupJob, "bears");
        addGroupIdentityLinkToJob(userAndGroupJob, "frogs");
        addUserIdentityLinkToJob(userAndGroupJob, "fozzie");

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups("kermit", Collections.singleton("muppets"))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups("gonzo", Collections.singleton("muppets"))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyUserJob.getId());

        cmmnManagementService.createExternalWorkerJobFailureBuilder(onlyUserJob.getId(), "testWorker").fail();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups("fozzie", Collections.singleton("bears"))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyGroupJob.getId(), userAndGroupJob.getId());

        cmmnManagementService.createExternalWorkerJobFailureBuilder(onlyGroupJob.getId(), "testWorker").fail();
        cmmnManagementService.createExternalWorkerJobFailureBuilder(userAndGroupJob.getId(), "testWorker").fail();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups(null, Collections.singleton("bears"))
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(onlyGroupJob.getId());

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(onlyGroupJob.getId(), "testWorker").terminate();

        assertThat(getJobIdentityLinks(onlyGroupJob)).isEmpty();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .forUserOrGroups("fozzie", Collections.emptyList())
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(userAndGroupJob.getId());

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(userAndGroupJob.getId(), "testWorker").complete();
        assertThat(getJobIdentityLinks(userAndGroupJob)).isEmpty();

        assertThat(getJobIdentityLinks(onlyUserJob))
                .extracting(IdentityLink::getUserId)
                .containsOnly("gonzo");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testAcquireByTenantId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .overrideCaseDefinitionTenantId("flowable")
                .start();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .overrideCaseDefinitionTenantId("acme")
                .start();

        List<ExternalWorkerJob> jobs = cmmnManagementService.createExternalWorkerJobQuery().list();
        assertThat(jobs).hasSize(2);

        ExternalWorkerJob flowableJob = cmmnManagementService.createExternalWorkerJobQuery().jobTenantId("flowable").singleResult();
        ExternalWorkerJob acmeJob = cmmnManagementService.createExternalWorkerJobQuery().jobTenantId("acme").singleResult();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .tenantId("megacorp")
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs).isEmpty();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .tenantId("flowable")
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(flowableJob.getId());
        cmmnManagementService.createExternalWorkerJobFailureBuilder(flowableJob.getId(), "testWorker").fail();

        acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .tenantId("acme")
                .acquireAndLock(4, "testWorker");

        assertThat(acquiredJobs)
                .extracting(AcquiredExternalWorkerJob::getId)
                .containsExactlyInAnyOrder(acmeJob.getId());

        cmmnManagementService.createExternalWorkerJobFailureBuilder(acmeJob.getId(), "testWorker").fail();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testUnaquireWithJobId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
            .topic("simple", Duration.ofMinutes(10))
            .acquireAndLock(1, "testWorker1");
        
        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
            .extracting(ExternalWorkerJob::getElementId)
            .containsExactlyInAnyOrder("externalWorkerTask");

        ExternalWorkerJob job = query.singleResult();

        cmmnManagementService.unacquireExternalWorkerJob(job.getId(), "testWorker1");
        
        assertThat(query.count()).isEqualTo(0);

        query = cmmnManagementService.createExternalWorkerJobQuery().jobId(job.getId());
        job = query.singleResult();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testUnaquireWithJobIdWrongWorkerId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
            .topic("simple", Duration.ofMinutes(10))
            .acquireAndLock(1, "testWorker1");
        
        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1");
        assertThat(query.count()).isEqualTo(1);
        
        final ExternalWorkerJob job = query.singleResult();

        assertThatThrownBy(() -> {
            cmmnManagementService.unacquireExternalWorkerJob(job.getId(), "testWorker2");

        }).isInstanceOf(FlowableException.class)
                .hasMessage("ExternalWorkerJobEntity[id=" + job.getId()
                        + ", jobHandlerType=cmmn-external-worker-complete, jobType=externalWorker, elementId=externalWorkerTask, correlationId="
                        + job.getCorrelationId() + ", scopeId=" + job.getScopeId()
                        + ", subScopeId=" + job.getSubScopeId() + ", scopeType=cmmn, scopeDefinitionId=" + job.getScopeDefinitionId()
                        + "] is locked with a different worker id");

        cmmnManagementService.unacquireExternalWorkerJob(job.getId(), "testWorker1");
        assertThat(query.count()).isEqualTo(0);

        query = cmmnManagementService.createExternalWorkerJobQuery().jobId(job.getId());
        ExternalWorkerJob unacquiredJob = query.singleResult();
        assertThat(unacquiredJob.getLockOwner()).isNull();
        assertThat(unacquiredJob.getLockExpirationTime()).isNull();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testUnaquireWithWorkerId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
            .topic("simple", Duration.ofMinutes(10))
            .acquireAndLock(1, "testWorker1");
        
        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list())
            .extracting(ExternalWorkerJob::getElementId)
            .containsExactlyInAnyOrder("externalWorkerTask");

        ExternalWorkerJob job = query.singleResult();

        cmmnManagementService.unacquireAllExternalWorkerJobsForWorker("testWorker1");
        
        assertThat(query.count()).isEqualTo(0);

        query = cmmnManagementService.createExternalWorkerJobQuery().jobId(job.getId());
        job = query.singleResult();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.cmmn")
    public void testUnaquireWithWorkerIdAndTenantId() {
        CaseInstance tenant1Instance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .overrideCaseDefinitionTenantId("tenant1")
                .start();
        
        CaseInstance tenant2Instance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .overrideCaseDefinitionTenantId("tenant2")
                .start();

        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
            .topic("simple", Duration.ofMinutes(10))
            .acquireAndLock(2, "testWorker1");
        
        ExternalWorkerJobQuery query = cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1");
        assertThat(query.count()).isEqualTo(2);
        assertThat(query.list())
            .extracting(ExternalWorkerJob::getElementId)
            .containsExactlyInAnyOrder("externalWorkerTask", "externalWorkerTask");
        
        assertThatThrownBy(() -> {
            cmmnManagementService.unacquireAllExternalWorkerJobsForWorker("testWorker1", "tenant1");
            
        }).isInstanceOf(FlowableException.class)
          .hasMessageContaining("provided worker id has external worker jobs from different tenant");
        
        ExternalWorkerJobEntity worker1Tenant2Job = (ExternalWorkerJobEntity) cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").jobTenantId("tenant2").singleResult();

        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                ExternalWorkerJobEntityManager externalWorkerJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getExternalWorkerJobEntityManager();
                worker1Tenant2Job.setTenantId("tenant1");
                externalWorkerJobEntityManager.update(worker1Tenant2Job);
                
                return null;
            }
        });

        cmmnManagementService.unacquireAllExternalWorkerJobsForWorker("testWorker1");
        
        assertThat(query.count()).isEqualTo(0);

        query = cmmnManagementService.createExternalWorkerJobQuery().scopeId(tenant1Instance.getId());
        ExternalWorkerJob job = query.singleResult();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
        
        query = cmmnManagementService.createExternalWorkerJobQuery().scopeId(tenant2Instance.getId());
        job = query.singleResult();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
    }


    @Test
    @CmmnDeployment
    public void testWithLimitedVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("anotherVar", "secretContent")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("theName", "kermit")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(acquiredJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker")
                .variable("theResult", "some result")
                .variable("theSecretResult", "secret result")
                .complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = cmmnManagementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(executableJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list()).hasSize(1);

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "result")).isEqualTo("some result");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "theResult")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "theSecretResult")).isNull();
    }

    @Test
    @CmmnDeployment
    public void testWithMultipleVariableMapping() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("description", "kermit is king")
                .variable("anotherVar", "secretContent")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
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
        assertThat(acquiredJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(acquiredJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker")
                .variable("theResult1", "r1")
                .variable("theResult2", "r2")
                .variable("theResult3", "r3")
                .variable("theSecretResult", "secret result")
                .complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = cmmnManagementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(executableJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list()).hasSize(1);

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "result1")).isEqualTo("r1");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "result2")).isEqualTo("r2");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "result3")).isEqualTo("r3");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "theSecretResult")).isNull();
    }

    @Test
    @CmmnDeployment
    public void testWithChangingVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("anotherVar", "secretContent")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "name", "ernie");

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables())
                .containsOnly(
                        entry("theName", "ernie")
                );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(acquiredJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = cmmnManagementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(executableJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list()).hasSize(1);
    }

    @Test
    @CmmnDeployment
    public void testWithNoInputVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("anotherVar", "secretContent")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "name", "ernie");

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables()).isEmpty();

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(acquiredJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker").complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = cmmnManagementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(executableJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list()).hasSize(1);
    }

    @Test
    @CmmnDeployment
    public void testWithExpressions() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("name", "kermit")
                .variable("anotherVar", 10)
                .variable("anotherVar2", 32)
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getCorrelationId()).isNotNull();
        assertThat(externalWorkerJob.getJobType()).isEqualTo(Job.JOB_TYPE_EXTERNAL_WORKER);
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "name", "ernie");

        List<AcquiredExternalWorkerJob> acquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(30))
                .acquireAndLock(4, "testWorker");

        externalWorkerJob = cmmnManagementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(externalWorkerJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(externalWorkerJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(externalWorkerJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(externalWorkerJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(externalWorkerJob.getLockExpirationTime()).isNotNull();
        assertThat(externalWorkerJob.getLockOwner()).isEqualTo("testWorker");

        assertThat(acquiredJobs).hasSize(1);

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);
        assertThat(acquiredJob.getVariables()).containsOnly(
                entry("theSum", 42L),
                entry("theProduct", 320L)
        );

        assertThat(acquiredJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(acquiredJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(acquiredJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(acquiredJob.getJobHandlerConfiguration()).isEqualTo("simple");
        assertThat(acquiredJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(acquiredJob.getLockExpirationTime()).isNotNull();
        assertThat(acquiredJob.getLockOwner()).isEqualTo("testWorker");

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(externalWorkerJob.getId(), "testWorker")
                .variable("resultVar1", 25)
                .variable("resultVar2", 35)
                .complete();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().singleResult()).isNull();

        Job executableJob = cmmnManagementService.createJobQuery().singleResult();

        assertThat(executableJob).isNotNull();
        assertThat(executableJob.getJobType()).isEqualTo(Job.JOB_TYPE_MESSAGE);
        assertThat(executableJob.getElementId()).isEqualTo("externalWorkerTask");
        assertThat(executableJob.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(executableJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(executableJob.getJobHandlerConfiguration()).isNull();
        assertThat(executableJob.getRetries()).isEqualTo(cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());
        assertThat(((JobEntity) executableJob).getLockExpirationTime()).isNull();
        assertThat(((JobEntity) executableJob).getLockOwner()).isNull();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnTaskService.createTaskQuery().list()).hasSize(1);

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "sum")).isEqualTo(60L);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "product")).isEqualTo(875L);
    }


    protected void addUserIdentityLinkToJob(Job job, String userId) {
        cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> {
                    cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                            .createScopeIdentityLink(null, job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER, userId, null, IdentityLinkType.PARTICIPANT);

                    return null;
                });
    }

    protected void addGroupIdentityLinkToJob(Job job, String groupId) {
        cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> {
                    cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                            .createScopeIdentityLink(null, job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER, null, groupId, IdentityLinkType.PARTICIPANT);
                    return null;
                });
    }

    protected Collection<IdentityLinkEntity> getJobIdentityLinks(Job job) {
        return cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                        .findIdentityLinksByScopeIdAndType(job.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER));
    }

    protected Map<String, Object> getExternalWorkerVariablesForPlanItemInstance(String planItemInstanceId) {
        return cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> {
                    List<VariableInstanceEntity> variables = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService()
                            .findVariableInstanceBySubScopeIdAndScopeType(planItemInstanceId, ScopeTypes.CMMN_EXTERNAL_WORKER);

                    Map<String, Object> variableMap = new HashMap<>();
                    for (VariableInstanceEntity variable : variables) {
                        variableMap.put(variable.getName(), variable.getValue());
                    }

                    return variableMap;
                });
    }

    protected void setTime(Instant time) {
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(time));
    }


    protected static class TestCreateCmmnExternalWorkerJobInterceptor implements CreateCmmnExternalWorkerJobInterceptor {
        protected int beforeCounter = 0;
        protected int afterCounter = 0;

        @Override
        public void beforeCreateExternalWorkerJob(CreateCmmnExternalWorkerJobBeforeContext beforeContext) {
            beforeCounter++;
            beforeContext.setJobTopicExpression(beforeContext.getJobTopicExpression() + "Test");
        }

        @Override
        public void afterCreateExternalWorkerJob(CreateCmmnExternalWorkerJobAfterContext afterContext) {
            afterCounter++;
        }
    }
}
