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
package org.flowable.cmmn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.flowable.cmmn.engine.history.HistoricMilestoneInstance;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.task.service.Task;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class ProcessTaskTest extends AbstractProcessEngineIntegrationTest {
    
    @Before
    public void deployOneTaskProcess() {
        if (processEngineRepositoryService.createDeploymentQuery().count() == 0) {
            processEngineRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").deploy();
        }
    }
    
    @Test
    @CmmnDeployment
    public void testOneTaskProcessNonBlocking() {
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess();
        List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
        assertEquals(1, processTasks.size());
        
        // Non-blocking process task, plan item should have been completed
        List<PlanItemInstance>  planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(1, planItemInstances.size());
        assertEquals("Task Two", planItemInstances.get(0).getName());
        
        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());
        
        processEngine.getTaskService().complete(processTasks.get(0).getId());
        assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
    }

    @Test
    @CmmnDeployment
    public void testOneTaskProcessBlocking() {
        
        CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess();
        
        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        
        // Blocking process task, plan item should be in state ACTIVE
        List<PlanItemInstance>  planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(1, planItemInstances.size());
        assertEquals("The Process", planItemInstances.get(0).getName());
        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());
        
        // Completing task will trigger completion of process task plan item
        processEngine.getTaskService().complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(1, planItemInstances.size());
        assertEquals("Task Two", planItemInstances.get(0).getName());
        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());
    }
    
    protected CaseInstance startCaseInstanceWithOneTaskProcess() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceById(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId());
        
        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());
        assertEquals(0L, processEngineRuntimeService.createProcessInstanceQuery().count());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(1, planItemInstances.size());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals("No process instance started", 1L, processEngineRuntimeService.createProcessInstanceQuery().count());
        return caseInstance;
    }
    
    @Test
    @CmmnDeployment
    public void testTransactionRollback() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceById(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("Task One", planItemInstance.getName());
        assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
        
        /*
         * Triggering the plan item will lead to the plan item that starts the one task process in a non-blocking way.
         * Due to the non-blocking, the plan item completes and the new task, mile stone and service task are called.
         * The service task throws an exception. The process should also roll back now and never have been inserted. 
         */
        try {
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
            fail();
        } catch (Exception e) { }
        
        // Without shared transaction, following would be 1
        assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
        
        planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        
        // Both case and process should have rolled back
        assertEquals("Task One", planItemInstance.getName());
    }
    
    @Test
    @CmmnDeployment
    public void testTriggerUnfinishedProcessPlanItem() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("The Process", planItemInstance.getName());
        assertEquals("my task", processEngine.getTaskService().createTaskQuery().singleResult().getName());
        
        // Triggering the process plan item should cancel the process instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(0, processEngine.getTaskService().createTaskQuery().count());
        assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());

        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertEquals("Process planitem done", historicMilestoneInstance.getName());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testStartProcessInstanceNonBlockingAndCaseInstanceFinished() {
        cmmnRuntimeService.startCaseInstanceByKey("myCase");
        
        assertEquals(1, processEngine.getTaskService().createTaskQuery().count());
        assertEquals(1, processEngineRuntimeService.createProcessInstanceQuery().count());

        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertEquals("Process planitem done", historicMilestoneInstance.getName());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testStartMultipleProcessInstancesBlocking() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("Task One", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        assertEquals(4, processEngine.getTaskService().createTaskQuery().count());
        assertEquals(4, processEngineRuntimeService.createProcessInstanceQuery().count());
        
        // Completing all the tasks should lead to the milestone
        for (Task task : processEngineTaskService.createTaskQuery().list()) {
            processEngineTaskService.complete(task.getId());
        }
        
        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertEquals("Processes done", historicMilestoneInstance.getName());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithBlockingProcessTask() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(8, cmmnRuntimeService.createPlanItemQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(3, cmmnRuntimeService.createPlanItemQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemInstanceName("Task One")
                .singleResult();
        assertNotNull(planItemInstance);
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        assertEquals(4, processEngine.getTaskService().createTaskQuery().count());
        assertEquals(4, processEngineRuntimeService.createProcessInstanceQuery().count());
        
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        
        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        assertEquals(0, processEngine.getTaskService().createTaskQuery().count());
        assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
}
