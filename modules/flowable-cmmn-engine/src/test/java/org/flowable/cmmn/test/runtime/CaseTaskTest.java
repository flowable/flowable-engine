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
package org.flowable.cmmn.test.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CaseTaskTest extends FlowableCmmnTestCase {
    
    protected String oneTaskCaseDeploymentId;
    
    @Before
    public void deployOneTaskCaseDefinition() {
        oneTaskCaseDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn").deploy().getId();
    }
    
    @After
    public void deleteOneTaskCaseDefinition() {
        cmmnRepositoryService.deleteDeploymentAndRelatedData(oneTaskCaseDeploymentId);
    }
    
    @Test
    @CmmnDeployment
    public void testBasicBlocking() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        
        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(2, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(2, planItemInstances.size());
        assertEquals("The Case", planItemInstances.get(0).getName());
        assertEquals("The Task", planItemInstances.get(1).getName());
        
        // Triggering the task from the child case instance should complete the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        
        planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("Task Two", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    // Same as testBasicBlocking(), but now with a non-blocking case task
    @Test
    @CmmnDeployment
    public void testBasicNonBlocking() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        
        // Triggering the task should start the case instance (which is non-blocking -> directly go to task two)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        assertEquals(2, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(2, planItemInstances.size());
        assertEquals("Task Two", planItemInstances.get(0).getName());
        assertEquals("The Task", planItemInstances.get(1).getName());
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        
        planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("The Task", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testRuntimeServiceTriggerCasePlanItemInstance() {
        cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(2, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(3, planItemInstances.size());
        assertEquals("Task One", planItemInstances.get(0).getName());
        assertEquals("The Case", planItemInstances.get(1).getName());
        assertEquals("The Task", planItemInstances.get(2).getName());
        
        // Triggering the planitem of the case should terminate the case and go to task two
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(2, planItemInstances.size());
        assertEquals("Task One", planItemInstances.get(0).getName());
        assertEquals("Task Two", planItemInstances.get(1).getName());
    }
    
    @Test
    @CmmnDeployment
    public void testRuntimeServiceTriggerNonBlockingCasePlanItem() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(2, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .singleResult();
        assertEquals("Task One", planItemInstance.getName());
        
        // Triggering the task plan item completes the parent case, but the child case remains
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .singleResult();
        assertEquals("The Task", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithNonBlockingCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertEquals("Task One", planItemInstance.getName());
        
        // Terminating the parent case instance should not terminate the child (it's non-blocking)
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        // Terminate child
        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(caseInstance.getId()).singleResult();
        assertNotNull(childCaseInstance);
        cmmnRuntimeService.terminateCaseInstance(childCaseInstance.getId());
        assertEquals(2, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithNestedCaseTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(4, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(4, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        
    }
    
}
