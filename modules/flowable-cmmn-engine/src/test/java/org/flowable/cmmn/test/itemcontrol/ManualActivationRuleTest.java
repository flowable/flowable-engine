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
import static org.junit.Assert.assertNotNull;
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
public class ManualActivationRuleTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSingleHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testManualActivatedHumanTask").start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ENABLED, planItemInstance.getState());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, planItemInstance.getState());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("The Task", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testDisableSingleHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testDisableSingleHumanTask").start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(PlanItemInstanceState.ENABLED, planItemInstance.getState());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Disabling the single plan item will terminate the case
        cmmnRuntimeService.disablePlanItemInstance(planItemInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testDisableHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testDisableHumanTask").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertEquals(PlanItemInstanceState.ENABLED, planItemInstance.getState());
        }
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        PlanItemInstance planItemInstance = planItemInstances.get(0);
        cmmnRuntimeService.disablePlanItemInstance(planItemInstance.getId());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateDisabled().count());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());

        cmmnRuntimeService.enablePlanItemInstance(planItemInstance.getId());
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
    }
    
    @Test
    @CmmnDeployment
    public void testManualActivationWithSentries() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testManualActivationWithSentries").start();
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateAvailable().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        
        cmmnRuntimeService.setVariables(caseInstance.getId(), CollectionUtil.singletonMap("variable", "startStage"));
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().singleResult().getId());
        
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateAvailable().count());
        assertEquals(3, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Completing C should enable the nested stage
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("C").singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateAvailable().count());
        
        // Enabling the nested stage activates task D
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(3, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("B", tasks.get(1).getName());
        assertEquals("D", tasks.get(2).getName());
        
        // Completing all the tasks ends the case instance
        for (Task t : cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()) {
            cmmnTaskService.complete(t.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testExitEnabledPlanItem() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testExitEnabledPlanItem").start();
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("C", tasks.get(1).getName());
        
        // Completing task A will exit the enabled stage
        cmmnTaskService.complete(tasks.get(0).getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("C", task.getName());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testManuallyActivatedServiceTask() {
        // Manual Activation enabled
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testManuallyActivatedServiceTask")
                .variable("manual", true)
                .start();
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().planItemDefinitionType(PlanItemDefinitionType.SERVICE_TASK).singleResult();
        assertNotNull(planItemInstance);
        cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
        assertEquals("test", cmmnRuntimeService.getVariable(caseInstance.getId(), "variable"));
        
        // Manual Activation disabled
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testManuallyActivatedServiceTask")
                .variable("manual", false)
                .start();
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        assertEquals("test", cmmnRuntimeService.getVariable(caseInstance.getId(), "variable"));
    }
    
    @Test
    @CmmnDeployment
    public void testRepeatedManualActivatedHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepeatedManualActivatedHumanTask")
                .variable("stopRepeat", false)
                .start();
        
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().singleResult().getId());
        
        // This can go on forever (but testing 100 here), as it's repeated without stop
        for (int i = 0; i < 100; i++) {
            
            List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
            assertEquals(2, tasks.size());
            assertEquals("Non-repeated task", tasks.get(0).getName());
            assertEquals("Repeated task", tasks.get(1).getName());
            
            // Completing the repeated task should again lead to an enabled task
            cmmnTaskService.complete(tasks.get(1).getId());
            assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
            cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().singleResult().getId());
        }
        
        assertEquals(2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.setVariables(caseInstance.getId(), CollectionUtil.singletonMap("stopRepeat", true));
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testInvalidDisable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testInvalidDisable").start();
        
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        try {
            PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().singleResult();
            cmmnRuntimeService.disablePlanItemInstance(planItemInstance.getId());
            fail();
        } catch (FlowableIllegalArgumentException e) {
            assertEquals("Can only disable a plan item instance which is in state ENABLED", e.getMessage());
        }
    }
    
    @Test
    @CmmnDeployment
    public void testInvalidEnable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testInvalidEnable").start();
        
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        try {
            PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().singleResult();
            cmmnRuntimeService.enablePlanItemInstance(planItemInstance.getId());
            fail();
        } catch (FlowableIllegalArgumentException e) {
            assertEquals("Can only enable a plan item instance which is in state AVAILABLE or DISABLED", e.getMessage());
        }
    }
    
    @Test
    @CmmnDeployment
    public void testInvalidStart() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testInvalidStart").start();
        
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateEnabled().count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        try {
            PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().singleResult();
            cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
            fail();
        } catch (FlowableIllegalArgumentException e) {
            assertEquals("Can only enable a plan item instance which is in state ENABLED", e.getMessage());
        }
    }
    
}
