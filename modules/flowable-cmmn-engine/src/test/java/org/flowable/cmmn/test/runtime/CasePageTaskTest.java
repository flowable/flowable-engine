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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

public class CasePageTaskTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testInStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertEquals(4, planItemInstances.size());
        String[] expectedNames = new String[] { "Case Page Task One", "Stage One", "Task One", "Task Two"};
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
       
        // Finishing task 2 should complete the stage
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
       
        PlanItemInstance pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemDefinitionId("casePageTask1")
                        .includeEnded()
                        .singleResult();
        assertNotNull(pagePlanItemInstance);
        assertEquals(PlanItemInstanceState.COMPLETED, pagePlanItemInstance.getState());
       
        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testTerminateStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        UserEventListenerInstance userEventListener = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListener.getId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
               .caseInstanceId(caseInstance.getId())
               .planItemInstanceState(PlanItemInstanceState.ACTIVE)
               .orderByName().asc()
               .list();
       
        assertEquals(1, planItemInstances.size());
        assertEquals("Task One", planItemInstances.get(0).getName());
       
        PlanItemInstance pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemDefinitionId("casePageTask1")
                        .includeEnded()
                        .singleResult();
        assertNotNull(pagePlanItemInstance);
        assertEquals(PlanItemInstanceState.TERMINATED, pagePlanItemInstance.getState());
       
        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
}
