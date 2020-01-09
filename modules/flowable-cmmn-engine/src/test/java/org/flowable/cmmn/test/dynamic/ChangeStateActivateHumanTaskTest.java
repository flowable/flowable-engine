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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

public class ChangeStateActivateHumanTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testActivateHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("activateFirstTask", false)
                        .start();
        
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .activatePlanItemDefinitionId("task1")
            .changeState();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task One", task.getName());
        
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task Two", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/dynamic/ChangeStateActivateHumanTaskTest.testActivateHumanTask.cmmn")
    public void testActivateSecondHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("activateFirstTask", true)
                        .start();
        
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .activatePlanItemDefinitionId("task2")
            .changeState();
        
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task1").singleResult();
        assertEquals("Task One", task.getName());
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task Two", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/dynamic/ChangeStateActivateHumanTaskTest.testActivateHumanTask.cmmn")
    public void testActivateSecondHumanTaskWithNoInitialTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("activateFirstTask", false)
                        .start();
        
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .activatePlanItemDefinitionId("task2")
            .changeState();
        
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .activatePlanItemDefinitionId("task1")
            .changeState();
        
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").singleResult();
        assertEquals("Task Two", task.getName());
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task One", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testActivateHumanTaskAndMoveState() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("activateFirstTask", true)
                        .start();
        
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .movePlanItemDefinitionIdTo("task1", "task2")
            .activatePlanItemDefinitionId("task3")
            .changeState();
        
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").singleResult();
        assertEquals("Task Two", task.getName());
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task Three", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testActivateHumanTaskInStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("activateFirstTask", false)
                        .start();
        
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).count());
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstance.getState());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .activatePlanItemDefinitionId("task1")
            .changeState();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task One", task.getName());
        
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).count());
        PlanItemInstance dbPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, dbPlanItemInstance.getState());
        assertEquals(planItemInstance.getId(), dbPlanItemInstance.getId());
        
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task Two", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testActivateHumanTaskInStageWithSentry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("activateStage", false)
                        .start();
        
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).count());
        assertEquals(PlanItemInstanceState.AVAILABLE, cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).singleResult().getState());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .activatePlanItemDefinitionId("task1")
            .changeState();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task One", task.getName());
        
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).count());
        PlanItemInstance dbPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, dbPlanItemInstance.getState());
        
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task Two", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/dynamic/ChangeStateActivateHumanTaskTest.testActivateHumanTask.cmmn")
    public void testChangeHumanTaskStateToAvailable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("activateFirstTask", true)
                        .start();
        
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "activateFirstTask", false);
        
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.createChangePlanItemStateBuilder()
            .caseInstanceId(caseInstance.getId())
            .changePlanItemInstanceToAvailableByPlanItemDefinitionId("task1")
            .changeState();
        
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        PlanItemInstance dbPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemDefinitionId("task1")
                        .singleResult();
        
        assertNotNull(dbPlanItemInstance);
        assertEquals(PlanItemInstanceState.AVAILABLE, dbPlanItemInstance.getState());
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "activateFirstTask", true);
        cmmnRuntimeService.evaluateCriteria(caseInstance.getId());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task One", task.getName());
        
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task Two", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

}
