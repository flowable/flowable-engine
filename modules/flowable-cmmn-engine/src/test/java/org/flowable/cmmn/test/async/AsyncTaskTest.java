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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class AsyncTaskTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testAsyncServiceTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task before service task", task.getName());
        cmmnTaskService.complete(task.getId());
        
        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals("asyncServiceTask", job.getElementId());
        assertEquals("Async service task", job.getElementName());

        waitForJobExecutorToProcessAllJobs();
      
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task after service task", task.getName());
        assertEquals("executed", cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate"));
    }
    
    @Test
    @CmmnDeployment
    public void testAsyncServiceTaskWithManagementService() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task before service task", task.getName());
        cmmnTaskService.complete(task.getId());
        
        // There should be an async job created now
        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals(caseInstance.getId(), job.getScopeId());
        assertEquals(caseInstance.getCaseDefinitionId(), job.getScopeDefinitionId());
        assertNotNull(job.getSubScopeId());
        assertEquals(ScopeTypes.CMMN, job.getScopeType());
        
        cmmnManagementService.executeJob(job.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task after service task", task.getName());
        assertEquals("executed", cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate"));
    }
    
    @Test
    @CmmnDeployment
    public void testMultipleAsyncHumanTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMultipleAsyncHumanTasks").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        cmmnTaskService.complete(task.getId());
        
        // Now 3 async jobs should be created for the 3 async human tasks
        assertEquals(3L, cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count());
        
        List<Job> jobs = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).list();
        assertEquals(3, jobs.size());
        for (Job job : jobs) {
            assertTrue(job.getElementId().startsWith("sid-"));
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
        assertEquals(0L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(1L, cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count());
        
        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals(caseInstance.getId(), job.getScopeId());
        assertEquals(caseInstance.getCaseDefinitionId(), job.getScopeDefinitionId());
        assertNotNull(job.getSubScopeId());
        assertEquals(ScopeTypes.CMMN, job.getScopeType());
        
        // Special state 'async-active' expected
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceStateAsyncActive().singleResult();
        assertEquals(PlanItemInstanceState.ASYNC_ACTIVE, planItemInstance.getState());
        
        waitForJobExecutorToProcessAllJobs();
        
        // Complete the case
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testAsyncServiceTaskCompletesCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTaskCompletesCaseInstance").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        cmmnTaskService.complete(task.getId());
        assertEquals(1L, cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count());
        
        waitForJobExecutorToProcessAllJobs();
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTerminateCaseInstance").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Non-async", task.getName());
        assertEquals(1L, cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(PlanItemInstanceState.ASYNC_ACTIVE, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult().getState());
        
        waitForJobExecutorToProcessAllJobs();
        
        // Triggering A should async-activate the three tasks after it
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(3L, cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals("Non-async", cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getName());
        
        // Terminating the case should delete also the jobs
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance);
        assertEquals(0L, cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count());
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
    
}
