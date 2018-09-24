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
package org.flowable.cmmn.test.itemcontrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class RequiredRuleTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testOneRequiredHumanTask() {
        
        // The required task is made active, the non-required not.
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testOneRequiredHumanTask")
                .variable("required", true)
                .start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertEquals(2, planItemInstances.size());
        assertEquals("Non-required task", planItemInstances.get(0).getName());
        assertEquals(PlanItemInstanceState.AVAILABLE, planItemInstances.get(0).getState());
        assertEquals("Required task", planItemInstances.get(1).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(1).getState());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Required task", task.getName());
        
        // Completing the task should autocomplete the plan model, as the plan model is autoComplete enabled
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
        
        // Both required and non-required task are made active.
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testOneRequiredHumanTask")
                .variable("required", true)
                .variable("nonRequired", true)
                .start();
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertEquals(2, planItemInstances.size());
        assertEquals("Non-required task", planItemInstances.get(0).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(0).getState());
        assertEquals("Required task", planItemInstances.get(1).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(1).getState());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("Non-required task", tasks.get(0).getName());
        assertEquals("Required task", tasks.get(1).getName());
        
        // Completing the required task should not autocomplete the plan model 
        cmmnTaskService.complete(tasks.get(1).getId());
        assertCaseInstanceNotEnded(caseInstance);
        
        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testOneRequiredHumanTaskInStage() {
        
        // The required task is made active, the non-required not.
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testOneRequiredHumanTaskInStage")
                .variable("required", true)
                .start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertEquals(4, planItemInstances.size());
        assertEquals("Non-required task", planItemInstances.get(0).getName());
        assertEquals(PlanItemInstanceState.AVAILABLE, planItemInstances.get(0).getState());
        assertEquals("Other task", planItemInstances.get(1).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(1).getState());
        assertEquals("Required task", planItemInstances.get(2).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(2).getState());
        assertEquals("The Stage", planItemInstances.get(3).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(3).getState());
        
        Task task = cmmnTaskService.createTaskQuery().taskName("Required task").singleResult();
        assertEquals("Required task", task.getName());
        
        // Completing the task should autocomplete the stage
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceNotEnded(caseInstance);
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertCaseInstanceEnded(caseInstance);
        
        // Both required and non-required task are made active.
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testOneRequiredHumanTaskInStage")
                .variable("required", true)
                .variable("nonRequired", true)
                .start();
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertEquals(4, planItemInstances.size());
        assertEquals("Non-required task", planItemInstances.get(0).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(0).getState());
        assertEquals("Other task", planItemInstances.get(1).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(1).getState());
        assertEquals("Required task", planItemInstances.get(2).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(2).getState());
        assertEquals("The Stage", planItemInstances.get(3).getName());
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstances.get(3).getState());
        
        Task otherTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Other task").singleResult();
        cmmnTaskService.complete(otherTask.getId());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("Non-required task", tasks.get(0).getName());
        assertEquals("Required task", tasks.get(1).getName());
        
        cmmnTaskService.complete(tasks.get(1).getId());
        assertCaseInstanceNotEnded(caseInstance);
        
        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
        
    }
    
    @Test
    @CmmnDeployment
    public void testNonAutoCompleteStageManualCompleteable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testNonAutoCompleteStageManualCompleteable")
                .variable("required", true)
                .start();
        
        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stagePlanItemInstance.getState());
        assertFalse(stagePlanItemInstance.isCompleteable());
        
        // Completing the one task should mark the stage as completeable 
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Required task", task.getName());
        cmmnTaskService.complete(task.getId());
        
        stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(stagePlanItemInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stagePlanItemInstance.getState());
        assertTrue(stagePlanItemInstance.isCompleteable());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Making the other task active, should disable the completeable flag again
        cmmnRuntimeService.setVariables(caseInstance.getId(), CollectionUtil.singletonMap("nonRequired", true));
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(stagePlanItemInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stagePlanItemInstance.getState());
        assertFalse(stagePlanItemInstance.isCompleteable());
    }
    
    @Test
    @CmmnDeployment
    public void testCompleteStageManually() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testNonAutoCompleteStageManualCompleteable")
                .variable("required", true)
                .start();
        
        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stagePlanItemInstance.getState());
        assertFalse(stagePlanItemInstance.isCompleteable());
        
        try {
            cmmnRuntimeService.completeStagePlanItemInstance(stagePlanItemInstance.getId());
            fail();
        } catch (FlowableIllegalArgumentException e) {
            assertEquals("Can only complete a stage plan item instance that is marked as completeable (there might still be active plan item instance).", e.getMessage());
        }
        
        // Completing the one task should mark the stage as completeable 
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Required task", task.getName());
        cmmnTaskService.complete(task.getId());
        
        stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(stagePlanItemInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stagePlanItemInstance.getState());
        assertTrue(stagePlanItemInstance.isCompleteable());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());

        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemCompleteable().singleResult());
        cmmnRuntimeService.completeStagePlanItemInstance(stagePlanItemInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testCompleteCaseInstanceManually() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCompleteCaseInstanceManually")
                .variable("required", true)
                .start();
        
        assertFalse(caseInstance.isCompleteable());
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateAvailable().count());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals("Other task", tasks.get(0).getName());
        assertEquals("Required task", tasks.get(1).getName());
        
        // Case should not be completeale
        try {
            cmmnRuntimeService.completeCaseInstance(caseInstance.getId());
            fail();
        } catch (FlowableIllegalArgumentException e) {
            assertEquals("Can only complete a case instance which is marked as completeable. Check if there are active plan item instances.", e.getMessage());
        }
        
        // Completing both tasks should not auto complete the case, as the plan model is not auto complete
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertTrue(caseInstance.isCompleteable());
        cmmnRuntimeService.completeCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testComplexCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("dRequired", false)
                .variable("enableSubStage", true)
                .start();
        
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(taskA);
        cmmnTaskService.complete(taskA.getId());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(1, tasks.size());
        assertEquals("D", tasks.get(0).getName());
        
        // D is required. So completing D will auto complete the stage
        cmmnTaskService.complete(tasks.get(0).getId());
        assertEquals(2, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()); // M1 is never reached. M2 and M3 are
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(1, tasks.size());
        assertEquals("G", tasks.get(0).getName());
        
        // G is the only required task. Completing it should complete the stage and case instance
        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testComplexCase02() {
        
        // Same as testComplexCase, but now B and E are manually enabled
        
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("dRequired", false)
                .variable("enableSubStage", false)
                .variable("booleanVar", true)
                .variable("subStageRequired", false)
                .start();
        
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(taskA);
        cmmnTaskService.complete(taskA.getId());
        
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertEquals(PlanItemInstanceState.ENABLED, planItemInstanceB.getState());
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("B", tasks.get(0).getName());
        assertEquals("D", tasks.get(1).getName());
        
        // D is required. But B is still active
        cmmnTaskService.complete(tasks.get(1).getId());
        assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        cmmnTaskService.complete(tasks.get(0).getId());
        assertEquals(1, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(1, tasks.size());
        assertEquals("C", tasks.get(0).getName());
        
        // There are no active tasks in the second stage (as the nested stage is not active and not required).
        // Stage should autocomplete immediately after task completion
        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }
      
}
