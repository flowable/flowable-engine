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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.StageResponse;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
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

    @Test
    @CmmnDeployment
    public void testStageFlagSet() {
        Date now = new Date();
        setClockTo(now);
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testCase")
            .variable("showInStage", true)
            .start();

        CaseInstance testStagesOnly = cmmnRuntimeService.createCaseInstanceQuery()
            .caseDefinitionKey("testCase")
            .singleResult();
        assertThat(testStagesOnly).isNotNull();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(testStagesOnly.getId()).planItemDefinitionType("stage").count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(testStagesOnly.getId()).onlyStages().count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(testStagesOnly.getId()).onlyStages().list()).extracting(PlanItemInstance::isStage).containsOnly(true);
        
        List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().onlyStages().list();
        assertEquals(3, historicPlanItemInstances.size());
        Map<String, Boolean> stageIncludeInOverviewMap = new HashMap<>();
        for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
            stageIncludeInOverviewMap.put(historicPlanItemInstance.getName(), historicPlanItemInstance.isShowInOverview());
        }
        
        assertTrue(stageIncludeInOverviewMap.get("Stage 1"));
        assertFalse(stageIncludeInOverviewMap.get("Stage 1.1"));
        assertTrue(stageIncludeInOverviewMap.get("Stage 2"));
        
        cmmnRuntimeService.completeStagePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 1").singleResult().getId());
        
        historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().onlyStages().list();
        assertEquals(4, historicPlanItemInstances.size());
        stageIncludeInOverviewMap = new HashMap<>();
        for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
            stageIncludeInOverviewMap.put(historicPlanItemInstance.getName(), historicPlanItemInstance.isShowInOverview());
        }
        
        assertTrue(stageIncludeInOverviewMap.get("Stage 1"));
        assertFalse(stageIncludeInOverviewMap.get("Stage 1.1"));
        assertTrue(stageIncludeInOverviewMap.get("Stage 2"));
        assertTrue(stageIncludeInOverviewMap.get("Stage 2.1"));
    }
    
    @Test
    @CmmnDeployment
    public void testGetStageOverview() {
        Date now = new Date();
        setClockTo(now);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testCase")
            .variable("showInStage", true)
            .start();

        List<StageResponse> stages = cmmnRuntimeService.getStageOverview(caseInstance.getId());
        assertEquals(3, stages.size());
        
        Map<String, StageResponse> stageMap = new HashMap<>();
        for (StageResponse stageResponse : stages) {
            stageMap.put(stageResponse.getName(), stageResponse);
        }
        
        assertEquals("Stage 1", stageMap.get("Stage 1").getName());
        assertTrue(stageMap.get("Stage 1").isCurrent());
        assertFalse(stageMap.get("Stage 1").isEnded());
        assertNull(stageMap.get("Stage 1").getEndTime());
        assertEquals("Stage 2", stageMap.get("Stage 2").getName());
        assertFalse(stageMap.get("Stage 2").isCurrent());
        assertFalse(stageMap.get("Stage 2").isEnded());
        assertNull(stageMap.get("Stage 2").getEndTime());
        assertEquals("Stage 2.1", stageMap.get("Stage 2.1").getName());
        assertFalse(stageMap.get("Stage 2.1").isCurrent());
        assertFalse(stageMap.get("Stage 2.1").isEnded());
        assertNull(stageMap.get("Stage 2.1").getEndTime());
        
        cmmnRuntimeService.completeStagePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 1").singleResult().getId());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        
        stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
        assertEquals(3, stages.size());
        
        stageMap = new HashMap<>();
        for (StageResponse stageResponse : stages) {
            stageMap.put(stageResponse.getName(), stageResponse);
        }
        
        assertEquals("Stage 1", stageMap.get("Stage 1").getName());
        assertFalse(stageMap.get("Stage 1").isCurrent());
        assertTrue(stageMap.get("Stage 1").isEnded());
        assertNotNull(stageMap.get("Stage 1").getEndTime());
        assertEquals("Stage 2", stageMap.get("Stage 2").getName());
        assertFalse(stageMap.get("Stage 2").isCurrent());
        assertTrue(stageMap.get("Stage 2").isEnded());
        assertNotNull(stageMap.get("Stage 2").getEndTime());
        assertEquals("Stage 2.1", stageMap.get("Stage 2.1").getName());
        assertFalse(stageMap.get("Stage 2.1").isCurrent());
        assertTrue(stageMap.get("Stage 2.1").isEnded());
        assertNotNull(stageMap.get("Stage 2.1").getEndTime());
    }
    
}
