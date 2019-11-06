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

import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.UNAVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testing the exit sentry on stage scenarios.
 *
 * @author Micha Kiener
 */
public class CaseCompletionExitSentryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseCompletionExitSentryTest.testCompleteCaseThroughExitSentry.cmmn")
    public void testCompleteCaseThroughExitSentryWithAvailableUserListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseTwo").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete case if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);

        // trigger the user event listener to manually complete the case (not forcing it though)
        cmmnRuntimeService.completeUserEventListenerInstance(planItemInstances.get(1).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().finished().singleResult();
        assertNotNull(historicCaseInstance);
        assertEquals(COMPLETED, historicCaseInstance.getState());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseCompletionExitSentryTest.testCompleteCaseThroughExitSentry.cmmn")
    public void testCompleteCaseThroughExitSentryWithException() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseTwo").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete case if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);

        // manually start Task A to have an active plan item, making the case not completable
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete case if completable", UNAVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        try {
            // trigger the user event listener to manually complete the case, which should lead into an exception
            cmmnRuntimeService.completeUserEventListenerInstance(planItemInstances.get(0).getId());
            Assert.fail("Must lead into an exception");
        } catch (FlowableIllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Cannot exit case with 'complete' event type"));
        }

        // now complete Task A to make the stage completable
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        // trigger the user event listener again as the stage should not be completable
        cmmnRuntimeService.completeUserEventListenerInstance(planItemInstances.get(0).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().finished().singleResult();
        assertNotNull(historicCaseInstance);
        assertEquals(COMPLETED, historicCaseInstance.getState());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseCompletionExitSentryTest.testCompleteCaseThroughExitSentry.cmmn")
    public void testCompleteStageThroughExitSentryWithForceComplete() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseTwo").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete case if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);

        // manually start Task A to have an active plan item, making the case not completable
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete case if completable", UNAVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        // trigger the user event listener to manually complete the case with a force to complete
        cmmnRuntimeService.completeUserEventListenerInstance(planItemInstances.get(2).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().finished().singleResult();
        assertNotNull(historicCaseInstance);
        assertEquals(COMPLETED, historicCaseInstance.getState());
    }
}
