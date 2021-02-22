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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class HistoryServiceTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testStartSimplePassthroughCase() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var1", "test").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var1", "test2").count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var1", "TEST").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var1", "TEST2").count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLike("var1", "te%").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLike("var1", "te2%").count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLikeIgnoreCase("var1", "TE%").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLikeIgnoreCase("var1", "TE2%").count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("var2", 5).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("var2", 11).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThan("var2", 11).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThan("var2", 8).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableExists("var1").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableExists("var3").count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableNotExists("var3").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableNotExists("var1").count()).isZero();

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        }
    }

    @Test
    @CmmnDeployment
    public void testStartSimplePassthroughCaseWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("startVar", "test")
                .variable("changeVar", 10)
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("startVar", "test").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("changeVar", 10).count()).isEqualTo(1);
        }

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task A");

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("newVar", "test");
        varMap.put("changeVar", 11);
        cmmnRuntimeService.setVariables(caseInstance.getId(), varMap);

        Map<String, Object> localVarMap = new HashMap<>();
        localVarMap.put("localVar", "test");
        localVarMap.put("localNumberVar", 2);
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), localVarMap);

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("startVar", "test").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("changeVar", 11).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("changeVar", 10).count()).isZero();
        }

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task B");
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testPlanItemInstancesStateChangesWithFixedTime() {
        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Date fixTime = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(823));
        cmmnEngineConfiguration.getClock().setCurrentTime(fixTime);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("allStates")
                .start();

        List<PlanItemInstance> runtimePlanItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(runtimePlanItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .as("planItemDefinitionId, state")
                .containsExactlyInAnyOrder(
                        tuple("eventListenerAvailable", PlanItemInstanceState.AVAILABLE),
                        tuple("eventListenerUnavailable", PlanItemInstanceState.UNAVAILABLE),
                        tuple("serviceTaskAvailableEnabled", PlanItemInstanceState.ENABLED),
                        tuple("serviceTaskAvailableAsyncActive", PlanItemInstanceState.ASYNC_ACTIVE)
                );

        Map<String, PlanItemInstance> runtimePlanItemInstancesByDefinitionId = runtimePlanItemInstances.stream()
                .collect(Collectors.toMap(PlanItemInstance::getPlanItemDefinitionId, Function.identity()));

        PlanItemInstance eventListenerAvailable = runtimePlanItemInstancesByDefinitionId.get("eventListenerAvailable");

        assertThat(eventListenerAvailable).extracting(
                PlanItemInstance::getCompletedTime,
                PlanItemInstance::getEndedTime,
                PlanItemInstance::getOccurredTime,
                PlanItemInstance::getTerminatedTime,
                PlanItemInstance::getExitTime,
                PlanItemInstance::getLastEnabledTime,
                PlanItemInstance::getLastDisabledTime,
                PlanItemInstance::getLastStartedTime,
                PlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(eventListenerAvailable).extracting(
                PlanItemInstance::getCreateTime,
                PlanItemInstance::getLastAvailableTime
        ).containsOnly(fixTime);

        PlanItemInstance eventListenerUnavailable = runtimePlanItemInstancesByDefinitionId.get("eventListenerUnavailable");

        assertThat(eventListenerUnavailable).extracting(
                PlanItemInstance::getCompletedTime,
                PlanItemInstance::getEndedTime,
                PlanItemInstance::getOccurredTime,
                PlanItemInstance::getTerminatedTime,
                PlanItemInstance::getExitTime,
                PlanItemInstance::getLastEnabledTime,
                PlanItemInstance::getLastAvailableTime,
                PlanItemInstance::getLastDisabledTime,
                PlanItemInstance::getLastStartedTime,
                PlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(eventListenerUnavailable).extracting(
                PlanItemInstance::getCreateTime
        ).isEqualTo(fixTime);

        PlanItemInstance serviceTaskAvailableEnabled = runtimePlanItemInstancesByDefinitionId.get("serviceTaskAvailableEnabled");

        assertThat(serviceTaskAvailableEnabled).extracting(
                PlanItemInstance::getCompletedTime,
                PlanItemInstance::getEndedTime,
                PlanItemInstance::getOccurredTime,
                PlanItemInstance::getTerminatedTime,
                PlanItemInstance::getExitTime,
                PlanItemInstance::getLastDisabledTime,
                PlanItemInstance::getLastStartedTime,
                PlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(serviceTaskAvailableEnabled).extracting(
                PlanItemInstance::getCreateTime,
                PlanItemInstance::getLastEnabledTime,
                PlanItemInstance::getLastAvailableTime
        ).containsOnly(fixTime);

        PlanItemInstance serviceTaskAvailableAsyncActive = runtimePlanItemInstancesByDefinitionId.get("serviceTaskAvailableAsyncActive");

        assertThat(serviceTaskAvailableAsyncActive).extracting(
                PlanItemInstance::getCompletedTime,
                PlanItemInstance::getEndedTime,
                PlanItemInstance::getOccurredTime,
                PlanItemInstance::getTerminatedTime,
                PlanItemInstance::getExitTime,
                PlanItemInstance::getLastEnabledTime,
                PlanItemInstance::getLastDisabledTime,
                PlanItemInstance::getLastSuspendedTime
        ).containsOnlyNulls();

        assertThat(serviceTaskAvailableAsyncActive).extracting(
                PlanItemInstance::getCreateTime,
                PlanItemInstance::getLastAvailableTime,
                PlanItemInstance::getLastStartedTime
        ).containsOnly(fixTime);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .list();

            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                    tuple("serviceTaskAvailableActiveCompleted", PlanItemInstanceState.COMPLETED),
                    tuple("stageAvailableActiveTerminated", PlanItemInstanceState.TERMINATED),
                    tuple("humanTaskAvailableActiveTerminatedAndWaitingForRepetition", PlanItemInstanceState.TERMINATED),
                    tuple("eventListenerAvailable", PlanItemInstanceState.AVAILABLE),
                    tuple("eventListenerUnavailable", PlanItemInstanceState.UNAVAILABLE),
                    tuple("serviceTaskAvailableEnabled", PlanItemInstanceState.ENABLED),
                    tuple("serviceTaskAvailableAsyncActive", PlanItemInstanceState.ASYNC_ACTIVE)
                );

            Map<String, HistoricPlanItemInstance> historicPlanItemInstancesByDefinitionId = historicPlanItemInstances.stream()
                .collect(Collectors.toMap(HistoricPlanItemInstance::getPlanItemDefinitionId, Function.identity()));

            HistoricPlanItemInstance historicEventListenerAvailable = historicPlanItemInstancesByDefinitionId.get("eventListenerAvailable");

            assertThat(historicEventListenerAvailable).extracting(
                HistoricPlanItemInstance::getCompletedTime,
                HistoricPlanItemInstance::getEndedTime,
                HistoricPlanItemInstance::getOccurredTime,
                HistoricPlanItemInstance::getTerminatedTime,
                HistoricPlanItemInstance::getExitTime,
                HistoricPlanItemInstance::getLastEnabledTime,
                HistoricPlanItemInstance::getLastDisabledTime,
                HistoricPlanItemInstance::getLastStartedTime,
                HistoricPlanItemInstance::getLastSuspendedTime
            ).containsOnlyNulls();

            assertThat(historicEventListenerAvailable).extracting(
                HistoricPlanItemInstance::getCreateTime,
                HistoricPlanItemInstance::getLastAvailableTime
            ).containsOnly(fixTime);

            HistoricPlanItemInstance historicEventListenerUnavailable = historicPlanItemInstancesByDefinitionId.get("eventListenerUnavailable");

            assertThat(historicEventListenerUnavailable).extracting(
                HistoricPlanItemInstance::getCompletedTime,
                HistoricPlanItemInstance::getEndedTime,
                HistoricPlanItemInstance::getOccurredTime,
                HistoricPlanItemInstance::getTerminatedTime,
                HistoricPlanItemInstance::getExitTime,
                HistoricPlanItemInstance::getLastEnabledTime,
                HistoricPlanItemInstance::getLastAvailableTime,
                HistoricPlanItemInstance::getLastDisabledTime,
                HistoricPlanItemInstance::getLastStartedTime,
                HistoricPlanItemInstance::getLastSuspendedTime
            ).containsOnlyNulls();

            assertThat(historicEventListenerUnavailable).extracting(
                HistoricPlanItemInstance::getCreateTime
            ).isEqualTo(fixTime);

            HistoricPlanItemInstance historicServiceTaskAvailableEnabled = historicPlanItemInstancesByDefinitionId.get("serviceTaskAvailableEnabled");

            assertThat(historicServiceTaskAvailableEnabled).extracting(
                HistoricPlanItemInstance::getCompletedTime,
                HistoricPlanItemInstance::getEndedTime,
                HistoricPlanItemInstance::getOccurredTime,
                HistoricPlanItemInstance::getTerminatedTime,
                HistoricPlanItemInstance::getExitTime,
                HistoricPlanItemInstance::getLastDisabledTime,
                HistoricPlanItemInstance::getLastStartedTime,
                HistoricPlanItemInstance::getLastSuspendedTime
            ).containsOnlyNulls();

            assertThat(historicServiceTaskAvailableEnabled).extracting(
                HistoricPlanItemInstance::getCreateTime,
                HistoricPlanItemInstance::getLastEnabledTime,
                HistoricPlanItemInstance::getLastAvailableTime
            ).containsOnly(fixTime);

            HistoricPlanItemInstance historicServiceTaskAvailableActiveCompleted = historicPlanItemInstancesByDefinitionId
                .get("serviceTaskAvailableActiveCompleted");

            assertThat(historicServiceTaskAvailableActiveCompleted).extracting(
                HistoricPlanItemInstance::getOccurredTime,
                HistoricPlanItemInstance::getTerminatedTime,
                HistoricPlanItemInstance::getExitTime,
                HistoricPlanItemInstance::getLastEnabledTime,
                HistoricPlanItemInstance::getLastDisabledTime,
                HistoricPlanItemInstance::getLastSuspendedTime
            ).containsOnlyNulls();

            assertThat(historicServiceTaskAvailableActiveCompleted).extracting(
                HistoricPlanItemInstance::getCreateTime,
                HistoricPlanItemInstance::getCompletedTime,
                HistoricPlanItemInstance::getEndedTime,
                HistoricPlanItemInstance::getLastAvailableTime,
                HistoricPlanItemInstance::getLastStartedTime
            ).containsOnly(fixTime);

            HistoricPlanItemInstance historicStageAvailableActiveTerminated = historicPlanItemInstancesByDefinitionId.get("stageAvailableActiveTerminated");

            assertThat(historicStageAvailableActiveTerminated).extracting(
                HistoricPlanItemInstance::getCompletedTime,
                HistoricPlanItemInstance::getOccurredTime,
                HistoricPlanItemInstance::getTerminatedTime,
                HistoricPlanItemInstance::getLastEnabledTime,
                HistoricPlanItemInstance::getLastDisabledTime,
                HistoricPlanItemInstance::getLastSuspendedTime
            ).containsOnlyNulls();

            assertThat(historicStageAvailableActiveTerminated).extracting(
                HistoricPlanItemInstance::getCreateTime,
                HistoricPlanItemInstance::getEndedTime,
                HistoricPlanItemInstance::getExitTime,
                HistoricPlanItemInstance::getLastAvailableTime,
                HistoricPlanItemInstance::getLastStartedTime
            ).containsOnly(fixTime);

            HistoricPlanItemInstance historicHumanTaskAvailableActiveTerminatedAndWaitingForRepetition = historicPlanItemInstancesByDefinitionId
                .get("humanTaskAvailableActiveTerminatedAndWaitingForRepetition");

            assertThat(historicHumanTaskAvailableActiveTerminatedAndWaitingForRepetition).extracting(
                HistoricPlanItemInstance::getCompletedTime,
                HistoricPlanItemInstance::getOccurredTime,
                HistoricPlanItemInstance::getTerminatedTime,
                HistoricPlanItemInstance::getLastEnabledTime,
                HistoricPlanItemInstance::getLastDisabledTime,
                HistoricPlanItemInstance::getLastSuspendedTime
            ).containsOnlyNulls();

            assertThat(historicHumanTaskAvailableActiveTerminatedAndWaitingForRepetition).extracting(
                HistoricPlanItemInstance::getCreateTime,
                HistoricPlanItemInstance::getEndedTime,
                HistoricPlanItemInstance::getExitTime,
                HistoricPlanItemInstance::getLastAvailableTime,
                HistoricPlanItemInstance::getLastStartedTime
            ).containsOnly(fixTime);
        }
    }
}
