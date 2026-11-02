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
package org.flowable.cmmn.test.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.cmmn.test.delegate.TestJavaDelegateThrowsException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.job.api.FlowableUnrecoverableJobException;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class AsyncTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testAsyncServiceTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task before service task");
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getElementId()).isEqualTo("asyncServiceTask");
        assertThat(job.getElementName()).isEqualTo("Async service task");

        waitForJobExecutorToProcessAllJobs();

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task after service task");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate")).isEqualTo("executed");
    }

    @Test
    @CmmnDeployment
    public void testAsyncServiceTaskCurrentTenant() {
        CaseInstance noTenantCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testAsyncServiceTask")
                .start();
        CaseInstance flowableCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testAsyncServiceTask")
                .overrideCaseDefinitionTenantId("flowable")
                .start();
        CaseInstance muppetsCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testAsyncServiceTask")
                .overrideCaseDefinitionTenantId("muppets")
                .start();
        assertThat(cmmnRuntimeService.hasVariable(noTenantCase.getId(), "currentTenantVar")).isFalse();
        assertThat(cmmnRuntimeService.hasVariable(flowableCase.getId(), "currentTenantVar")).isFalse();
        assertThat(cmmnRuntimeService.hasVariable(muppetsCase.getId(), "currentTenantVar")).isFalse();

        waitForJobExecutorToProcessAllJobs();

        VariableInstance variableInstance = cmmnRuntimeService.getVariableInstance(noTenantCase.getId(), "currentTenantVar");
        assertThat(variableInstance).isNotNull();
        assertThat((String) variableInstance.getValue()).isNullOrEmpty();
        assertThat(cmmnRuntimeService.getVariable(flowableCase.getId(), "currentTenantVar")).isEqualTo("flowable");
        assertThat(cmmnRuntimeService.getVariable(muppetsCase.getId(), "currentTenantVar")).isEqualTo("muppets");
    }
    
    @Test
    @CmmnDeployment
    public void testAsyncServiceTaskWithCategory() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job.getCategory()).isEqualTo("cmmnCategory");

        waitForJobExecutorToProcessAllJobs();

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task after service task");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate")).isEqualTo("executed");
    }
    
    @Test
    @CmmnDeployment
    public void testAsyncServiceTaskWithCategoryExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testAsyncServiceTask")
                .variable("categoryValue", "testValue")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job.getCategory()).isEqualTo("testValue");

        waitForJobExecutorToProcessAllJobs();

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task after service task");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate")).isEqualTo("executed");
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testAsyncServiceTaskWithCategoryEnabledConfigurationSet() {
        try {
            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("myCategory");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());
    
            Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(job.getCategory()).isEqualTo("cmmnCategory");
    
            assertThatThrownBy(() -> CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true))
                    .isInstanceOf(Exception.class);

            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("cmmnCategory");
            
            waitForJobExecutorToProcessAllJobs();
    
            task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("Task after service task");
            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate")).isEqualTo("executed");
            
        } finally {
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @CmmnDeployment
    public void testAsyncServiceTaskWithManagementService() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task before service task");
        cmmnTaskService.complete(task.getId());

        // There should be an async job created now
        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(job.getScopeDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
        assertThat(job.getSubScopeId()).isNotNull();
        assertThat(job.getScopeType()).isEqualTo(ScopeTypes.CMMN);

        cmmnManagementService.executeJob(job.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task after service task");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate")).isEqualTo("executed");
    }

    @Test
    @CmmnDeployment
    public void testAsyncServiceTaskWithFailure() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task before service task");
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getElementId()).isEqualTo("asyncServiceTask");
        assertThat(job.getElementName()).isEqualTo("Async service task");
        assertThat(job.getRetries()).isEqualTo(3);
        String correlationId = job.getCorrelationId();

        assertThatThrownBy(() -> cmmnManagementService.executeJob(job.getId()));

        Job timerJob = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        assertThat(timerJob.getCorrelationId()).isEqualTo(correlationId);
        assertThat(timerJob.getRetries()).isEqualTo(2);
        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        assertThat(cmmnManagementService.createDeadLetterJobQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithFailure.cmmn")
    public void testAsyncServiceTaskWithUnrecoverableFailure() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task before service task");
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getElementId()).isEqualTo("asyncServiceTask");
        assertThat(job.getElementName()).isEqualTo("Async service task");
        assertThat(job.getRetries()).isEqualTo(3);
        String correlationId = job.getCorrelationId();

        try {
            TestJavaDelegateThrowsException.setExceptionSupplier(() -> new FlowableUnrecoverableJobException("Test exception"));
            assertThatThrownBy(() -> cmmnManagementService.executeJob(job.getId()));
        } finally {
            TestJavaDelegateThrowsException.resetExceptionSupplier();
        }

        Job deadLetterJob = cmmnManagementService.createDeadLetterJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(deadLetterJob).isNotNull();
        assertThat(deadLetterJob.getCorrelationId()).isEqualTo(correlationId);
        assertThat(deadLetterJob.getRetries()).isZero();
        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        assertThat(cmmnManagementService.createTimerJobQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithFailure.cmmn")
    public void testAsyncServiceTaskWithUnrecoverableFailureAsCause() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task before service task");
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getElementId()).isEqualTo("asyncServiceTask");
        assertThat(job.getElementName()).isEqualTo("Async service task");
        assertThat(job.getRetries()).isEqualTo(3);
        String correlationId = job.getCorrelationId();

        try {
            TestJavaDelegateThrowsException.setExceptionSupplier(() -> new FlowableException("Test", new FlowableUnrecoverableJobException("Test exception")));
            assertThatThrownBy(() -> cmmnManagementService.executeJob(job.getId()))
                    .isExactlyInstanceOf(FlowableException.class)
                    .hasMessage("Test")
                    .cause()
                    .isInstanceOf(FlowableUnrecoverableJobException.class)
                    .hasMessage("Test exception");
        } finally {
            TestJavaDelegateThrowsException.resetExceptionSupplier();
        }

        Job deadLetterJob = cmmnManagementService.createDeadLetterJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(deadLetterJob).isNotNull();
        assertThat(deadLetterJob.getCorrelationId()).isEqualTo(correlationId);
        assertThat(deadLetterJob.getRetries()).isZero();
        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        assertThat(cmmnManagementService.createTimerJobQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testMultipleAsyncHumanTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMultipleAsyncHumanTasks").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");
        cmmnTaskService.complete(task.getId());

        // Now 3 async jobs should be created for the 3 async human tasks
        assertThat(cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(3);

        List<Job> jobs = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(jobs).hasSize(3);
        for (Job job : jobs) {
            assertThat(job.getElementId()).startsWith("sid-");
            assertThat(job.getElementName()).isIn("B", "C", "D");
        }

        waitForJobExecutorToProcessAllJobs();

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B", "C", "D");

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("B", "C", "D");
        assertThat(planItemInstances).extracting(PlanItemInstance::getCreateTime).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testSingleAsyncTask() {

        // Evaluation should not complete the case instance when the async task hasn't been processed yet
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSingleAsyncTask").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(job.getScopeDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
        assertThat(job.getSubScopeId()).isNotNull();
        assertThat(job.getScopeType()).isEqualTo(ScopeTypes.CMMN);

        // Special state 'async-active' expected
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceStateAsyncActive().singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ASYNC_ACTIVE);

        waitForJobExecutorToProcessAllJobs();

        // Complete the case
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testAsyncServiceTaskCompletesCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTaskCompletesCaseInstance").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");
        cmmnTaskService.complete(task.getId());
        assertThat(cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        waitForJobExecutorToProcessAllJobs();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTerminateCaseInstance").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Non-async");
        assertThat(cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult().getState())
                .isEqualTo(PlanItemInstanceState.ASYNC_ACTIVE);

        waitForJobExecutorToProcessAllJobs();

        // Triggering A should async-activate the three tasks after it
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getName()).isEqualTo("Non-async");

        // Terminating the case should delete also the jobs
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testAsyncExclusiveServiceTaskAfterStageExit() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncExclusive").start();

        // Trigger the user event listener first. This will execute the first exclusive async service task
        cmmnRuntimeService.completeUserEventListenerInstance(cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult().getId());

        // There should be one job and it needs to be exclusive
        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job.isExclusive()).isTrue();

        waitForJobExecutorToProcessAllJobs();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "serviceTaskVar1")).isEqualTo("firstST");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "serviceTaskVar2")).isNull();

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).contains("Task in stage", "Task outside stage");

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Second service task outside stage")
                .singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        // Completing 'Task in stage' exits the stage, which will start the service task.
        // The service task is async and exclusive. It will set the 'serviceTaskVar2' variable.
        cmmnTaskService.complete(tasks.get(0).getId());

        // There should be one job and it needs to be exclusive
        job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job.isExclusive()).isTrue();

        waitForJobExecutorToProcessAllJobs();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "serviceTaskVar1")).isEqualTo("firstST");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "serviceTaskVar2")).isEqualTo("secondST");
    }

    @Test
    @CmmnDeployment
    public void testEndUserIdWhenCaseInstanceEndsAsynchronously() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("endAsynchronouslyCase").start();

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());

        // There should be one job and it needs to be exclusive
        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job.isExclusive()).isTrue();

        waitForJobExecutorToProcessAllJobs();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getEndTime()).isNotNull();
            assertThat(historicCaseInstance.getEndUserId()).isEqualTo(null);
        }

    }

}
