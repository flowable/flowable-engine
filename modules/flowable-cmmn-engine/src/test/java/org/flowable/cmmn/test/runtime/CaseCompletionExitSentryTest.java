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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.UNAVAILABLE;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.Test;

/**
 * Testing the exit sentry on case scenarios.
 *
 * @author Micha Kiener
 */
public class CaseCompletionExitSentryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseCompletionExitSentryTest.testCompleteCaseThroughExitSentry.cmmn")
    public void testCompleteCaseThroughExitSentryWithAvailableUserListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete case if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);

        // trigger the user event listener to manually complete the case (not forcing it though)
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Complete case if completable"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().finished().singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(COMPLETED);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseCompletionExitSentryTest.testCompleteCaseThroughExitSentry.cmmn")
    public void testCompleteCaseThroughExitSentryWithException() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseTwo").start();

        List<PlanItemInstance> planItemInstances1 = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances1).hasSize(4);
        assertPlanItemInstanceState(planItemInstances1, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances1, "Complete case if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances1, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances1, "Task A", ENABLED);

        // manually start Task A to have an active plan item, making the case not completable
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances1, "Task A"));

        final List<PlanItemInstance> planItemInstances2 = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances2).hasSize(4);
        assertPlanItemInstanceState(planItemInstances2, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances2, "Complete case if completable", UNAVAILABLE);
        assertPlanItemInstanceState(planItemInstances2, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances2, "Task A", ACTIVE);

        // trigger the user event listener to manually complete the case, which should lead into an exception
        assertThatThrownBy(() -> cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances2, "Complete case")))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageStartingWith("Cannot exit case with 'complete' event type");

        // now complete Task A to make the stage completable
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances2, "Task A"));

        // trigger the user event listener again as the stage should not be completable
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances2, "Complete case"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().finished().singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(COMPLETED);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CaseCompletionExitSentryTest.testCompleteCaseThroughExitSentry.cmmn")
    public void testCompleteCaseThroughExitSentryWithForceComplete() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("exitSentryTestCaseTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete case if completable", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);

        // manually start Task A to have an active plan item, making the case not completable
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete case if completable", UNAVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Force complete case", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        // trigger the user event listener to manually complete the case with a force to complete
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Force complete case"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().finished().singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(COMPLETED);
        }
    }
}
