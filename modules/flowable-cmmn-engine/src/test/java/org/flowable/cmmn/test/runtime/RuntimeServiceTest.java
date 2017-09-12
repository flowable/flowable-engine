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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.history.HistoricMilestoneInstance;
import org.flowable.cmmn.engine.repository.CaseDefinition;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.runtime.MilestoneInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class RuntimeServiceTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testStartSimplePassthroughCase() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertEquals("myCase", caseDefinition.getKey());
        assertNotNull(caseDefinition.getResourceName());
        assertNotNull(caseDefinition.getDeploymentId());
        assertTrue(caseDefinition.getVersion() > 0);
        
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceById(caseDefinition.getId());
        assertNotNull(caseInstance);
        assertEquals(caseDefinition.getId(), caseInstance.getCaseDefinitionId());
        assertNotNull(caseInstance.getStartTime());
        assertEquals(CaseInstanceState.COMPLETED, caseInstance.getState());
        
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
     
        List<HistoricMilestoneInstance> milestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .orderByMilestoneName().asc()
                .list();
        assertEquals(2, milestoneInstances.size());
        assertEquals("PlanItem Milestone One", milestoneInstances.get(0).getName());
        assertEquals("PlanItem Milestone Two", milestoneInstances.get(1).getName());
    }
    
    @Test
    @CmmnDeployment
    public void testStartSimplePassthroughCaseWithBlockingTask() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceById(caseDefinition.getId());
        assertEquals(CaseInstanceState.ACTIVE, caseInstance.getState());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        
        assertEquals(4, cmmnRuntimeService.createPlanItemQuery().caseInstanceId(caseInstance.getId()).count());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task A", planItemInstance.getName());
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        List<MilestoneInstance> mileStones = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertEquals(1, mileStones.size());
        assertEquals("PlanItem Milestone One", mileStones.get(0).getName());
        
        List<HistoricMilestoneInstance> historicMilestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertEquals(1, historicMilestoneInstances.size());
        assertEquals("PlanItem Milestone One", historicMilestoneInstances.get(0).getName());
        
        planItemInstance = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task B", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(2, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstance() {
        
        // Task A active
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 0);
        
        // Task B active
        caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        cmmnRuntimeService.triggerPlanItemInstance(cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult().getId());
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 1);
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        assertEquals(8, cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 0);
    }
    
    @Test
    @CmmnDeployment
    public void testGetVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase", variables);
        
        Map<String, Object> variablesFromGet = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertTrue(variablesFromGet.containsKey("stringVar"));
        assertEquals("Hello World", (String) variablesFromGet.get("stringVar"));
        assertTrue(variablesFromGet.containsKey("intVar"));
        assertEquals(42, ((Integer) variablesFromGet.get("intVar")).intValue());
        
        assertEquals("Hello World", (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar"));
        assertEquals(42, ((Integer) cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar")).intValue());
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "doesNotExist"));
    }
    
    @Test
    @CmmnDeployment
    public void testSetVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        cmmnRuntimeService.setVariables(caseInstance.getId(), variables);
        
        assertEquals("Hello World", (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar"));
        assertEquals(42, ((Integer) cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar")).intValue());
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "doesNotExist"));
    }
    
    @Test
    @CmmnDeployment
    public void testRemoveVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase", variables);
        assertEquals(2, cmmnRuntimeService.getVariables(caseInstance.getId()).size());
        
        cmmnRuntimeService.removeVariable(caseInstance.getId(), "stringVar");
        assertEquals(1, cmmnRuntimeService.getVariables(caseInstance.getId()).size());
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "StringVar"));
        assertNotNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar"));
    }
    
}
