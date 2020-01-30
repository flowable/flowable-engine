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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.TERMINATED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.UNAVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
public class StageCompletionExitSentryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/StageCompletionExitSentryTest.testCompleteStageThroughExitSentry.cmmn")
    public void testCompleteStageThroughExitSentryWithAvailableUserListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseOne").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete stage if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);

        // trigger the user event listener to manually complete the stage (not forcing it though)
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Complete stage if completable"));

        // the stage must be in completion state
        planItemInstances = getCompletedPlanItemInstances(caseInstance.getId());

        assertEquals(2, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete stage if completable", COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED);

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(1, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        // complete Task B and the case will be completed
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/StageCompletionExitSentryTest.testCompleteStageThroughExitSentry.cmmn")
    public void testCompleteStageThroughExitSentryWithException() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseOne").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete stage if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);

        // manually start Task A to have an active plan item, making the stage not completable
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete stage if completable", UNAVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);

        try {
            // trigger the user event listener to manually complete the stage, which should lead into an exception
            cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Complete stage"));
            Assert.fail("Must lead into an exception");
        } catch (FlowableIllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Cannot exit stage with 'complete' event type"));
        }

        // now complete Task A to make the stage completable
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        // trigger the user event listener again as the stage should not be completable
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Complete stage"));

        // the stage must be in completion state
        planItemInstances = getCompletedPlanItemInstances(caseInstance.getId());

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete stage", COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED);

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(1, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        // complete Task B and the case will be completed
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/StageCompletionExitSentryTest.testCompleteStageThroughExitSentry.cmmn")
    public void testCompleteStageThroughExitSentryWithForceComplete() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseOne").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete stage if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);

        // manually start Task A to have an active plan item, making the stage not completable
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete stage if completable", UNAVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete stage", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);

        // trigger the user event listener to manually complete the stage with a force to complete
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Force complete stage"));

        // the stage must be in completion state
        planItemInstances = getCompletedPlanItemInstances(caseInstance.getId());

        assertEquals(2, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Force complete stage", COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED);

        planItemInstances = getTerminatedPlanItemInstances(caseInstance.getId());

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Complete stage", TERMINATED);
        assertPlanItemInstanceState(planItemInstances, "Complete stage if completable", TERMINATED);
        assertPlanItemInstanceState(planItemInstances, "Task A", TERMINATED);

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(1, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        // complete Task B and the case will be completed
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
}
