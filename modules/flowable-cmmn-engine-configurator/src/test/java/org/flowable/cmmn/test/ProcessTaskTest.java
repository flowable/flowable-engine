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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.PlanItemInstanceCallbackType;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
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
        List<PlanItemInstance>  planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
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
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(1, planItemInstances.size());
        assertEquals("The Process", planItemInstances.get(0).getName());
        assertNotNull(planItemInstances.get(0).getReferenceId());
        assertEquals(PlanItemInstanceCallbackType.CHILD_PROCESS, planItemInstances.get(0).getReferenceType());
        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());
        
        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().singleResult();
        assertNotNull(processInstance);
        assertNotNull(processInstance.getCallbackId());
        assertNotNull(processInstance.getCallbackType());
        
        if (processEngine.getProcessEngineConfiguration().getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstance historicProcessInstance = processEngine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertEquals(processInstance.getCallbackId(), historicProcessInstance.getCallbackId());
            assertEquals(processInstance.getCallbackType(), historicProcessInstance.getCallbackType());
        }
        
        // Completing task will trigger completion of process task plan item
        processEngine.getTaskService().complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(1, planItemInstances.size());
        assertEquals("Task Two", planItemInstances.get(0).getName());
        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());
    }

    @Test
    @CmmnDeployment(tenantId = "flowable",
    resources = "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessBlocking.cmmn")
    public void testOneTaskProcessBlockingWithTenant() {
        try {
            if (processEngineRepositoryService.createDeploymentQuery().count() == 1) {
                Deployment deployment = processEngineRepositoryService.createDeploymentQuery().singleResult();
                processEngineRepositoryService.deleteDeployment(deployment.getId());
            }
            processEngineRepositoryService.createDeployment().
                    addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").
                    tenantId("flowable").
                    deploy();

            CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("flowable");

            ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().singleResult();
            assertEquals("flowable", processInstance.getTenantId());

            Task task = processEngine.getTaskService().createTaskQuery().singleResult();
            assertEquals("flowable", task.getTenantId());

            this.cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        } finally {
            if (processEngineRepositoryService.createDeploymentQuery().count() == 1) {
                Deployment deployment = processEngineRepositoryService.createDeploymentQuery().singleResult();
                processEngineRepositoryService.deleteDeployment(deployment.getId());
            }
            if (processEngine.getProcessEngineConfiguration().getHistoryService().createHistoricTaskInstanceQuery().count() == 1) {
                HistoricTaskInstance historicTaskInstance = processEngine.getProcessEngineConfiguration().getHistoryService().createHistoricTaskInstanceQuery().singleResult();
                processEngine.getProcessEngineConfiguration().getHistoryService().deleteHistoricTaskInstance(historicTaskInstance.getId());
            }
        }
    }

    @Test
    @CmmnDeployment
    public void testProcessRefExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("processDefinitionKey", "oneTask");
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionId(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId())
            .variables(variables)
            .start();
        
        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        assertNotNull(task);
        
        // Completing task will trigger completion of process task plan item
        processEngine.getTaskService().complete(task.getId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateActive()
                .list();
        assertEquals(1, planItemInstances.size());
        assertEquals("Task Two", planItemInstances.get(0).getName());
        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());
    }
    
    protected CaseInstance startCaseInstanceWithOneTaskProcess() {
        return startCaseInstanceWithOneTaskProcess(null);
    }
    
    protected CaseInstance startCaseInstanceWithOneTaskProcess(String tenantId) {
        CaseDefinitionQuery caseDefinitionQuery = cmmnRepositoryService.createCaseDefinitionQuery();
        if (tenantId != null) {
            caseDefinitionQuery.caseDefinitionTenantId(tenantId);
        }
        String caseDefinitionId = caseDefinitionQuery.singleResult().getId();
        CaseInstanceBuilder caseInstanceBuilder = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionId(caseDefinitionId);
        if (tenantId != null) {
            caseInstanceBuilder.tenantId(tenantId);
        }
        CaseInstance caseInstance = caseInstanceBuilder.
                start();

        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());
        assertEquals(0L, processEngineRuntimeService.createProcessInstanceQuery().count());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
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
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId()).start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
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
        
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        
        // Both case and process should have rolled back
        assertEquals("Task One", planItemInstance.getName());
    }
    
    @Test
    @CmmnDeployment
    public void testTriggerUnfinishedProcessPlanItem() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("The Process", planItemInstance.getName());
        assertEquals("my task", processEngine.getTaskService().createTaskQuery().singleResult().getName());
        
        // Triggering the process plan item should cancel the process instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(0, processEngine.getTaskService().createTaskQuery().count());
        assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertEquals("Process planitem done", historicMilestoneInstance.getName());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testStartProcessInstanceNonBlockingAndCaseInstanceFinished() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        
        assertEquals(1, processEngine.getTaskService().createTaskQuery().count());
        assertEquals(1, processEngineRuntimeService.createProcessInstanceQuery().count());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertEquals("Process planitem done", historicMilestoneInstance.getName());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testStartMultipleProcessInstancesBlocking() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
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
        
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult();
        assertEquals("Processes done", historicMilestoneInstance.getName());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithBlockingProcessTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(8, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
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
        
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, processEngine.getTaskService().createTaskQuery().count());
        assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
}
