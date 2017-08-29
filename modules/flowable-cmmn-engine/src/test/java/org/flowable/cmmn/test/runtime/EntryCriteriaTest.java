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

import org.flowable.cmmn.engine.history.HistoricCaseInstance;
import org.flowable.cmmn.engine.history.HistoricMilestoneInstance;
import org.flowable.cmmn.engine.repository.CaseDefinition;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class EntryCriteriaTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testStartPassthroughCaseWithThreeEntryCriteriaOnParts() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceById(caseDefinition.getId());
     
        List<HistoricMilestoneInstance> mileStones = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .orderByMilestoneName().asc()
                .list();
        assertEquals(1, mileStones.size());
        assertEquals("PlanItem Milestone One", mileStones.get(0).getName());
        
        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertNotNull(historicCaseInstance);
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
    
    @Test
    @CmmnDeployment
    public void testThreeEntryCriteriaOnPartsForWaitStates() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceById(caseDefinition.getId());
        assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(3, planItemInstances.size());
        
        for (PlanItemInstance planItemInstance : planItemInstances) {
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        }
     
        HistoricMilestoneInstance mileStone = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .singleResult();
        assertEquals("PlanItem Milestone One", mileStone.getName());
        assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        
        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertNotNull(historicCaseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceAfterOneOutOfMultipleOnPartsSatisfied() {
        CaseInstance caseInstance = cmmnRuntimeService.startCaseInstanceByKey("myCase");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(3, planItemInstances.size());
        
        // Triggering two plan items = 2 on parts satisfied
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
}
