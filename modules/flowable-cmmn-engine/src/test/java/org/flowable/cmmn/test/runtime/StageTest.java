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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class StageTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testOneNestedStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
       assertEquals(4, planItemInstances.size());
       String[] expectedNames = new String[] { "Stage One", "Task One", "Task Three", "Task Two"};
       for (int i=0; i<planItemInstances.size(); i++) {
           assertEquals(expectedNames[i], planItemInstances.get(i).getName());
       }
       
       // Finishing task 2 and 3 should complete the nested stage
       cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
       cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
       
       planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
               .caseInstanceId(caseInstance.getId())
               .planItemInstanceState(PlanItemInstanceState.ACTIVE)
               .orderByName().asc()
               .list();
       assertEquals(1, planItemInstances.size());
       expectedNames = new String[] { "Task One" };
       for (int i=0; i<planItemInstances.size(); i++) {
           assertEquals(expectedNames[i], planItemInstances.get(i).getName());
       }
       
       // Finish case instance
       cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
       assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
       assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
       assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testOneNestedStageNonBlocking() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testTwoNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .orderByName().asc()
                .list();
       assertEquals(6, planItemInstances.size());
       String[] expectedNames = new String[] { "Stage One", "Stage Two", "Task Four", "Task One", "Task Three", "Task Two"};
       for (int i=0; i<planItemInstances.size(); i++) {
           assertEquals(expectedNames[i], planItemInstances.get(i).getName());
       }
       
       // Complete inner nested stage
       cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
       planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
               .caseInstanceId(caseInstance.getId())
               .planItemInstanceState(PlanItemInstanceState.ACTIVE)
               .orderByName().asc()
               .list();
      assertEquals(4, planItemInstances.size());
      expectedNames = new String[] { "Stage One", "Task One", "Task Three", "Task Two"};
      for (int i=0; i<planItemInstances.size(); i++) {
          assertEquals(expectedNames[i], planItemInstances.get(i).getName());
      }
    }
    
    @Test
    @CmmnDeployment
    public void testTwoNestedStagesNonBlocking() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testThreeNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .orderByName().asc()
                .list();
        assertEquals(8, planItemInstances.size());
        String[] expectedNames = new String[] { "Stage One", "Stage Three", "Stage Two",
                "Task Five", "Task Four", "Task One", "Task Three", "Task Two" };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }

        // Complete inner nested stage (3th stage)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(6, planItemInstances.size());
        expectedNames = new String[] { "Stage One", "Stage Two", "Task Four", "Task One",
                "Task Three", "Task Two" };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }

        // Commplete inner nested stage (2nd stage)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(4, planItemInstances.size());
        expectedNames = new String[] { "Stage One", "Task One", "Task Three", "Task Two" };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(1, planItemInstances.size());
        expectedNames = new String[] { "Task One"};
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testThreeNestedStagesNonBlocking() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testThreeNestedStagesWithCriteria() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(4, planItemInstances.size());
        String[] expectedNames = new String[] { "Stage A", "Task A", "Task B", "Task C"};
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
        
        // Completing A and B triggers stage 2
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(6, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Stage C", "Task C", "Task D", "Task E"};
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
        
        // Triggering Task C should exit stage 2, which should also exit the inner nested stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(8, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
}
