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

import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
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
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .includeStagePlanItemInstances()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
       assertEquals(5, planItemInstances.size());
       String[] expectedNames = new String[] { "My CasePlanModel", "Stage One", "Task One", "Task Three", "Task Two"};
       for (int i=0; i<planItemInstances.size(); i++) {
           assertEquals(expectedNames[i], planItemInstances.get(i).getName());
       }
       
       // Finishing task 2 and 3 should complete the nesteds stage
       cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
       cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(4).getId());
       
       planItemInstances = cmmnRuntimeService.createPlanItemQuery()
               .caseInstanceId(caseInstance.getId())
               .includeStagePlanItemInstances()
               .planItemInstanceState(PlanItemInstanceState.ACTIVE)
               .orderByName().asc()
               .list();
       assertEquals(2, planItemInstances.size());
       expectedNames = new String[] { "My CasePlanModel", "Task One"};
       for (int i=0; i<planItemInstances.size(); i++) {
           assertEquals(expectedNames[i], planItemInstances.get(i).getName());
       }
       
       // Finish case instance
       cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
       assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
       assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
       assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testOneNestedStageNonBlocking() {
        cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testTwoNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .includeStagePlanItemInstances()
                .orderByName().asc()
                .list();
       assertEquals(7, planItemInstances.size());
       String[] expectedNames = new String[] { "My CasePlanModel", "Stage One", "Stage Two", "Task Four", "Task One", "Task Three", "Task Two"};
       for (int i=0; i<planItemInstances.size(); i++) {
           assertEquals(expectedNames[i], planItemInstances.get(i).getName());
       }
       
       // Complete inner nested stage
       cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
       planItemInstances = cmmnRuntimeService.createPlanItemQuery()
               .caseInstanceId(caseInstance.getId())
               .includeStagePlanItemInstances()
               .planItemInstanceState(PlanItemInstanceState.ACTIVE)
               .orderByName().asc()
               .list();
      assertEquals(5, planItemInstances.size());
      expectedNames = new String[] { "My CasePlanModel", "Stage One", "Task One", "Task Three", "Task Two"};
      for (int i=0; i<planItemInstances.size(); i++) {
          assertEquals(expectedNames[i], planItemInstances.get(i).getName());
      }
    }
    
    @Test
    @CmmnDeployment
    public void testTwoNestedStagesNonBlocking() {
        cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testThreeNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .includeStagePlanItemInstances()
                .orderByName().asc()
                .list();
        assertEquals(9, planItemInstances.size());
        String[] expectedNames = new String[] { "My CasePlanModel", "Stage One", "Stage Three", "Stage Two",
                "Task Five", "Task Four", "Task One", "Task Three", "Task Two" };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }

        // Complete inner nested stage (3th stage)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(4).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemQuery().caseInstanceId(caseInstance.getId())
                .includeStagePlanItemInstances()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(7, planItemInstances.size());
        expectedNames = new String[] { "My CasePlanModel", "Stage One", "Stage Two", "Task Four", "Task One",
                "Task Three", "Task Two" };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }

        // Commplete inner nested stage (2nd stage)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemQuery().caseInstanceId(caseInstance.getId())
                .includeStagePlanItemInstances()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(5, planItemInstances.size());
        expectedNames = new String[] { "My CasePlanModel", "Stage One", "Task One", "Task Three", "Task Two" };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(4).getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .includeStagePlanItemInstances()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(2, planItemInstances.size());
        expectedNames = new String[] { "My CasePlanModel", "Task One"};
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testThreeNestedStagesNonBlocking() {
        cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testThreeNestedStagesWithCriteria() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(3, planItemInstances.size());
        String[] expectedNames = new String[] { "Task A", "Task B", "Task C"};
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
        
        // Completing A and B triggers stage 2
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(3, planItemInstances.size());
        expectedNames = new String[] { "Task C", "Task D", "Task E"};
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
        
        // Triggering C should exit stage 2, which should also exit the inner nested stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        
        assertEquals(0, cmmnRuntimeService.createPlanItemQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
}
