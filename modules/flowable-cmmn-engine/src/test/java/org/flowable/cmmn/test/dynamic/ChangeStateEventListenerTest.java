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
package org.flowable.cmmn.test.dynamic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

public class ChangeStateEventListenerTest extends FlowableCmmnTestCase {
    
    protected String oneTaskCaseDeploymentId;

    @Test
    @CmmnDeployment
    public void testChangeHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task One", task.getName());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .movePlanItemDefinitionIdTo("task1", "task2")
            .changeState();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list();
        assertEquals(3, planItemInstances.size());
        
        boolean planItem1Found = false;
        boolean planItem2Found = false;
        boolean planItem3Found = false;
        for (PlanItemInstance planItemInstance : planItemInstances) {
            if ("planItem1".equals(planItemInstance.getElementId())) {
                planItem1Found = true;
                assertEquals(PlanItemInstanceState.TERMINATED, planItemInstance.getState());
                
            } else if ("planItem2".equals(planItemInstance.getElementId())) {
                planItem2Found = true;
                assertEquals(PlanItemInstanceState.ACTIVE, planItemInstance.getState());
            
            } else if ("planItem3".equals(planItemInstance.getElementId())) {
                planItem3Found = true;
                assertEquals(PlanItemInstanceState.TERMINATED, planItemInstance.getState());
            }
        }
        
        assertTrue(planItem1Found);
        assertTrue(planItem2Found);
        assertTrue(planItem3Found);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task Two", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testChangeHumanTaskAndListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task One", task.getName());
        
        PlanItemInstance singlePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("eventListener").singleResult();
        assertNotNull(singlePlanItemInstance);
        assertEquals(PlanItemInstanceState.AVAILABLE, singlePlanItemInstance.getState());
        
        cmmnRuntimeService.completeUserEventListenerInstance(singlePlanItemInstance.getId());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertEquals(2, tasks.size());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task1").list();
        assertEquals(1, tasks.size());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").list();
        assertEquals(1, tasks.size());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .movePlanItemDefinitionIdTo("task2", "task1")
            .changeState();
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertEquals(2, tasks.size());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task1").list();
        assertEquals(2, tasks.size());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list();
        assertEquals(6, planItemInstances.size());
        
        int planItem1Found = 0;
        boolean planItem2TerminatedFound = false;
        boolean planItem2AvailableFound = false;
        boolean planItem3CompletedFound = false;
        boolean planItem3AvailableFound = false;
        for (PlanItemInstance planItemInstance : planItemInstances) {
            if ("planItem1".equals(planItemInstance.getElementId())) {
                planItem1Found++;
                assertEquals(PlanItemInstanceState.ACTIVE, planItemInstance.getState());
                
            } else if ("planItem2".equals(planItemInstance.getElementId())) {
                if (PlanItemInstanceState.TERMINATED.equals(planItemInstance.getState())) {
                    planItem2TerminatedFound = true;
                } else {
                    assertEquals(PlanItemInstanceState.AVAILABLE, planItemInstance.getState());
                    planItem2AvailableFound = true;
                }
            
            } else if ("planItem3".equals(planItemInstance.getElementId())) {
                if (PlanItemInstanceState.COMPLETED.equals(planItemInstance.getState())) {
                    planItem3CompletedFound = true;
                } else {
                    assertEquals(PlanItemInstanceState.AVAILABLE, planItemInstance.getState());
                    planItem3AvailableFound = true;
                }
            }
        }
        
        assertEquals(2, planItem1Found);
        assertTrue(planItem2TerminatedFound);
        assertTrue(planItem2AvailableFound);
        assertTrue(planItem3CompletedFound);
        assertTrue(planItem3AvailableFound);
        
        // complete task 1 instances
        cmmnTaskService.complete(tasks.get(0).getId());
        cmmnTaskService.complete(tasks.get(1).getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertEquals(1, tasks.size());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").list();
        assertEquals(1, tasks.size());
        
        // complete task 2 instances
        cmmnTaskService.complete(tasks.get(0).getId());
        
        assertCaseInstanceEnded(caseInstance);
    }

}
