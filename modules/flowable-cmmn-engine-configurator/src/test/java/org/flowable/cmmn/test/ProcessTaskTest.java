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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
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
    public void testOneCallActivityProcessBlocking() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneCallActivityProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();
        
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                            .caseDefinitionKey("myCase")
                            .start();
             
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                            .caseInstanceId(caseInstance.getId())
                            .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                            .list();
             
            assertEquals(1, planItemInstances.size());
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

            List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
            assertEquals(1, processTasks.size());
            Task processTask = processTasks.get(0);
            String subProcessInstanceId = processTask.getProcessInstanceId();
            ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().subProcessInstanceId(subProcessInstanceId).singleResult();
            
            Task task = cmmnTaskService.createTaskQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertEquals(processTask.getId(), task.getId());
            
            task = cmmnTaskService.createTaskQuery().processInstanceIdWithChildren(processInstance.getId()).singleResult();
            assertEquals(processTask.getId(), task.getId());
            
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertEquals(3, entityLinks.size());
            EntityLink processEntityLink = null;
            EntityLink subProcessEntityLink = null;
            EntityLink taskEntityLink = null;
            for (EntityLink entityLink : entityLinks) {
                if (ScopeTypes.BPMN.equals(entityLink.getReferenceScopeType())) {
                    if (processInstance.getId().equals(entityLink.getReferenceScopeId())) {
                        processEntityLink = entityLink;
                    } else {
                        subProcessEntityLink = entityLink;
                    }
                    
                } else if (ScopeTypes.TASK.equals(entityLink.getReferenceScopeType())) {
                    taskEntityLink = entityLink;
                }
            }
            
            assertEquals(EntityLinkType.CHILD, processEntityLink.getLinkType());
            assertNotNull(processEntityLink.getCreateTime());
            assertEquals(caseInstance.getId(), processEntityLink.getScopeId());
            assertEquals(ScopeTypes.CMMN, processEntityLink.getScopeType());
            assertNull(processEntityLink.getScopeDefinitionId());
            assertEquals(processInstance.getId(), processEntityLink.getReferenceScopeId());
            assertEquals(ScopeTypes.BPMN, processEntityLink.getReferenceScopeType());
            assertNull(processEntityLink.getReferenceScopeDefinitionId());
            assertEquals(HierarchyType.ROOT, processEntityLink.getHierarchyType());
            
            assertEquals(EntityLinkType.CHILD, subProcessEntityLink.getLinkType());
            assertNotNull(subProcessEntityLink.getCreateTime());
            assertEquals(caseInstance.getId(), subProcessEntityLink.getScopeId());
            assertEquals(ScopeTypes.CMMN, subProcessEntityLink.getScopeType());
            assertNull(subProcessEntityLink.getScopeDefinitionId());
            assertEquals(subProcessInstanceId, subProcessEntityLink.getReferenceScopeId());
            assertEquals(ScopeTypes.BPMN, subProcessEntityLink.getReferenceScopeType());
            assertNull(subProcessEntityLink.getReferenceScopeDefinitionId());
            assertEquals(HierarchyType.ROOT, subProcessEntityLink.getHierarchyType());
            
            assertEquals(EntityLinkType.CHILD, taskEntityLink.getLinkType());
            assertNotNull(taskEntityLink.getCreateTime());
            assertEquals(caseInstance.getId(), taskEntityLink.getScopeId());
            assertEquals(ScopeTypes.CMMN, taskEntityLink.getScopeType());
            assertNull(taskEntityLink.getScopeDefinitionId());
            assertEquals(processTasks.get(0).getId(), taskEntityLink.getReferenceScopeId());
            assertEquals(ScopeTypes.TASK, taskEntityLink.getReferenceScopeType());
            assertNull(taskEntityLink.getReferenceScopeDefinitionId());
            assertEquals(HierarchyType.ROOT, taskEntityLink.getHierarchyType());
            
            entityLinks = processEngine.getRuntimeService().getEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertEquals(2, entityLinks.size());
            
            entityLinks = processEngine.getRuntimeService().getEntityLinkChildrenForProcessInstance(subProcessInstanceId);
            assertEquals(1, entityLinks.size());
            EntityLink entityLink = entityLinks.get(0);
            assertEquals(EntityLinkType.CHILD, entityLink.getLinkType());
            assertNotNull(entityLink.getCreateTime());
            assertEquals(subProcessInstanceId, entityLink.getScopeId());
            assertEquals(ScopeTypes.BPMN, entityLink.getScopeType());
            assertNull(entityLink.getScopeDefinitionId());
            assertEquals(processTasks.get(0).getId(), entityLink.getReferenceScopeId());
            assertEquals(ScopeTypes.TASK, entityLink.getReferenceScopeType());
            assertNull(entityLink.getReferenceScopeDefinitionId());
            assertEquals(HierarchyType.PARENT, entityLink.getHierarchyType());

            entityLinks = processEngine.getRuntimeService().getEntityLinkParentsForTask(processTask.getId());
            assertEquals(3, entityLinks.size());
            entityLink = entityLinks.get(0);
            assertEquals(EntityLinkType.CHILD, entityLink.getLinkType());
            assertNotNull(entityLink.getCreateTime());

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
            
            List<HistoricEntityLink> historicEntityLinks = cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId());
            assertEquals(3, historicEntityLinks.size());
            
            historicEntityLinks = processEngine.getHistoryService().getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
            assertEquals(2, historicEntityLinks.size());
            
            historicEntityLinks = processEngine.getHistoryService().getHistoricEntityLinkChildrenForProcessInstance(subProcessInstanceId);
            assertEquals(1, historicEntityLinks.size());
            assertEquals(HierarchyType.PARENT, historicEntityLinks.get(0).getHierarchyType());

            historicEntityLinks = processEngine.getHistoryService().getHistoricEntityLinkParentsForTask(processTasks.get(0).getId());
            assertEquals(3, historicEntityLinks.size());
            
            HistoricTaskInstance historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceIdWithChildren(caseInstance.getId()).singleResult();
            assertEquals(processTask.getId(), historicTask.getId());
            
            historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery().processInstanceIdWithChildren(processInstance.getId()).singleResult();
            assertEquals(processTask.getId(), historicTask.getId());
            
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
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
        assertEquals(CallbackTypes.PLAN_ITEM_CHILD_PROCESS, planItemInstances.get(0).getReferenceType());
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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessBlocking.cmmn")
    public void testTwoTaskProcessBlocking() {
        try {
            if (processEngineRepositoryService.createDeploymentQuery().count() == 1) {
                Deployment deployment = processEngineRepositoryService.createDeploymentQuery().singleResult();
                processEngineRepositoryService.deleteDeployment(deployment.getId());
            }
            processEngineRepositoryService.createDeployment().
                    addClasspathResource("org/flowable/cmmn/test/twoTaskProcess.bpmn20.xml").
                    deploy();

            CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess();

            Task task = processEngineTaskService.createTaskQuery().singleResult();
            assertEquals("my task", task.getName());
            
            EntityLink taskEntityLink = null;
            List<EntityLink> entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            for (EntityLink entityLink : entityLinks) {
                if (task.getId().equals(entityLink.getReferenceScopeId())) {
                    taskEntityLink = entityLink;
                }
            }
            
            assertNotNull(taskEntityLink);
            assertEquals(task.getId(), taskEntityLink.getReferenceScopeId());
            assertEquals(ScopeTypes.TASK, taskEntityLink.getReferenceScopeType());
            assertEquals(HierarchyType.ROOT, taskEntityLink.getHierarchyType());
            
            processEngineTaskService.complete(task.getId());
            
            Task task2 = processEngineTaskService.createTaskQuery().singleResult();
            assertEquals("my task2", task2.getName());
            
            EntityLink taskEntityLink2 = null;
            entityLinks = cmmnRuntimeService.getEntityLinkChildrenForCaseInstance(caseInstance.getId());
            for (EntityLink entityLink : entityLinks) {
                if (task2.getId().equals(entityLink.getReferenceScopeId())) {
                    taskEntityLink2 = entityLink;
                }
            }
            
            assertNotNull(taskEntityLink2);
            assertEquals(task2.getId(), taskEntityLink2.getReferenceScopeId());
            assertEquals(ScopeTypes.TASK, taskEntityLink2.getReferenceScopeType());
            assertEquals(HierarchyType.ROOT, taskEntityLink2.getHierarchyType());
            
            processEngineTaskService.complete(task2.getId());
            
            List<ProcessInstance> processInstances = processEngineRuntimeService.createProcessInstanceQuery().processDefinitionKey("oneTask").list();
            assertEquals(0, processInstances.size());

            this.cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
            
        } finally {
            if (processEngineRepositoryService.createDeploymentQuery().count() == 1) {
                Deployment deployment = processEngineRepositoryService.createDeploymentQuery().singleResult();
                processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
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

    @Test
    @CmmnDeployment
    public void testProcessIOParameter() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("processDefinitionKey", "oneTask");
        variables.put("num2", 123);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
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
        assertEquals(123, cmmnRuntimeService.getVariable(caseInstance.getId(), "num3"));
        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());

    }

    @Test
    @CmmnDeployment
    public void testProcessIOParameterExpressions() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionId(cmmnRepositoryService.createCaseDefinitionQuery().singleResult().getId())
            .variable("processDefinitionKey", "oneTask")
            .start();

        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        assertNotNull(task);

        // Completing task will trigger completion of process task plan item
        assertEquals(2L, ((Number) processEngine.getRuntimeService().getVariable(task.getProcessInstanceId(), "numberVariable")).longValue());
        processEngine.getTaskService().complete(task.getId(), Collections.singletonMap("processVariable", "Hello World"));

        assertEquals("Hello World", cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVariable"));

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

    @Test
    @CmmnDeployment(resources = {
        "org/flowable/cmmn/test/ProcesTaskTest.testParentStageTerminatedBeforeProcessStarted.cmmn",
        "org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml"
    })
    public void testParentStageTerminatedBeforeProcessStarted() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testProcessTask").start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());

        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Complete stage", userEventListenerInstance.getName());
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
    }

    @Test
    @CmmnDeployment(
        resources = {"org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessFallbackToDefaultTenant.cmmn"},
        tenantId = "flowable"
    )
    public void testOneTaskProcessFallbackToDefaultTenant() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").
            deploy();
        try {
            CaseInstance caseInstance = startCaseInstanceWithOneTaskProcess("flowable");
            List<Task> processTasks = processEngine.getTaskService().createTaskQuery().list();
            assertEquals(1, processTasks.size());

            // Non-blocking process task, plan item should have been completed
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
            assertEquals(1, planItemInstances.size());
            assertEquals("Task Two", planItemInstances.get(0).getName());

            assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());

            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(
        resources = {"org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessFallbackToDefaultTenantFalse.cmmn"},
        tenantId = "flowable"
    )
    public void testOneTaskProcessFallbackToDefaultTenantFalse() {
        Deployment deployment = this.processEngineRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").
            deploy();
        try {
            startCaseInstanceWithOneTaskProcess("flowable");
            fail();
        } catch (FlowableObjectNotFoundException e) {
            assertThat(e.getMessage(), is("Process definition with key 'oneTask' and tenantId 'flowable' was not found"));
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

}
