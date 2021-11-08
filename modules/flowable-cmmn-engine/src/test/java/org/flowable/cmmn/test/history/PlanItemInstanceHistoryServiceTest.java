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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Dennis Federico
 */
public class PlanItemInstanceHistoryServiceTest extends FlowableCmmnTestCase {

    private static Consumer<HistoricPlanItemInstance> assertCreateTimeHistoricPlanItemInstance = h -> {
        assertThat(h.getCreateTime()).isNotNull();
        assertThat(h.getCreateTime().getTime()).isGreaterThanOrEqualTo(0L);
        //        if (!PlanItemInstanceState.WAITING_FOR_REPETITION.equalsIgnoreCase(h.getState())) {
        //            assertThat(h.getLastAvailableTime()).isNotNull();
        //        }
    };

    private static Consumer<HistoricPlanItemInstance> assertStartedTimeHistoricPlanItemInstance = h -> {
        assertThat(h.getLastStartedTime()).isNotNull();
        assertThat(h.getLastStartedTime().getTime()).isGreaterThanOrEqualTo(h.getCreateTime().getTime());
    };

    private static Consumer<HistoricPlanItemInstance> assertEndedTimeHistoricPlanItemInstance = h -> {
        assertThat(h.getEndedTime()).isNotNull();
        assertThat(h.getEndedTime().getTime()).isGreaterThanOrEqualTo(h.getLastStartedTime().getTime());
    };

    private static Consumer<HistoricPlanItemInstance> assertEndStateHistoricPlanItemInstance = h -> {
        assertThat(h.getState()).isNotNull();
        assertThat(PlanItemInstanceState.END_STATES).contains(h.getState());
    };

    private static Consumer<HistoricPlanItemInstance> assertStartedStateHistoricPlanItemInstance = h -> {
        assertThat(h.getState())
                .isIn(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.ENABLED, PlanItemInstanceState.ASYNC_ACTIVE);
    };

    @Test
    @CmmnDeployment
    public void testSimpleCaseFlow() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimpleCaseFlow").start();

        //one Task, one Stage, one Milestone
        List<PlanItemInstance> currentPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(currentPlanItems).hasSize(3);
        assertThat(currentPlanItems.stream().map(PlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.STAGE::equalsIgnoreCase))
                .isTrue();
        assertThat(currentPlanItems.stream().map(PlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.MILESTONE::equalsIgnoreCase))
                .isTrue();
        assertThat(currentPlanItems.stream().map(PlanItemInstance::getPlanItemDefinitionType).anyMatch("task"::equalsIgnoreCase)).isTrue();

        //Milestone are just another planItem too, so it will appear in the planItemInstance History
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItems).hasSize(3);
            assertThat(
                historicPlanItems.stream().map(HistoricPlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.STAGE::equalsIgnoreCase))
                .isTrue();
            assertThat(historicPlanItems.stream().map(HistoricPlanItemInstance::getPlanItemDefinitionType)
                .anyMatch(PlanItemDefinitionType.MILESTONE::equalsIgnoreCase)).isTrue();
            assertThat(historicPlanItems.stream()
                .anyMatch(h -> "task".equalsIgnoreCase(h.getPlanItemDefinitionType()) && "planItemTaskA".equalsIgnoreCase(h.getElementId()))).isTrue();

            //Check Start timeStamp within the second of its original creation
            historicPlanItems.forEach(assertCreateTimeHistoricPlanItemInstance);
            checkHistoryCreateTimestamp(currentPlanItems, historicPlanItems, 1000L);

            //Check activation timestamp... for those "live" instances not in "Waiting" state (i.e. AVAILABLE)
            List<String> nonWaitingPlanInstanceIds = getIdsOfNonWaitingPlanItemInstances(currentPlanItems);
            assertThat(nonWaitingPlanInstanceIds).isNotEmpty();
            List<HistoricPlanItemInstance> filteredHistoricPlanItemInstances = historicPlanItems.stream()
                .filter(h -> nonWaitingPlanInstanceIds.contains(h.getId()))
                .collect(Collectors.toList());
            assertThat(filteredHistoricPlanItemInstances).isNotEmpty();
            filteredHistoricPlanItemInstances.forEach(assertCreateTimeHistoricPlanItemInstance
                .andThen(assertStartedTimeHistoricPlanItemInstance)
                .andThen(assertStartedTimeHistoricPlanItemInstance)
                .andThen(assertStartedStateHistoricPlanItemInstance));

            //No planItemInstance has "ended" yet, so no historicPlanItemInstance should have endTime timestamp
            historicPlanItems.forEach(h -> assertThat(h.getEndedTime()).isNull());

            //Milestone history is only filled when the milestone occurs
            List<HistoricMilestoneInstance> historicMilestones = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicMilestones).isEmpty();

            //////////////////////////////////////////////////////////////////
            //Trigger the task to reach the milestone and activate the stage//
            assertCaseInstanceNotEnded(caseInstance);
            PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
            assertThat(task).isNotNull();
            cmmnRuntimeService.triggerPlanItemInstance(task.getId());
            assertCaseInstanceNotEnded(caseInstance);

            //Now there are 2 plan items in a non-final state, a Stage and its containing task (only 1 new planItem)
            currentPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(currentPlanItems).hasSize(2);
            assertThat(currentPlanItems.stream().map(PlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.STAGE::equalsIgnoreCase))
                .isTrue();
            assertThat(currentPlanItems.stream()
                .anyMatch(p -> "task".equalsIgnoreCase(p.getPlanItemDefinitionType()) && "planItemTaskB".equalsIgnoreCase(p.getElementId()))).isTrue();

            historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItems).hasSize(4);
            assertThat(
                cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(historicPlanItems.get(0).getId()).planItemInstanceWithoutTenantId()
                    .list()).hasSize(1);
            assertThat(
                cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(historicPlanItems.get(0).getId()).planItemInstanceWithoutTenantId()
                    .count()).isEqualTo(1);

            //Check start timestamps of newly added timeStamp within the second of its original creation
            checkHistoryCreateTimestamp(currentPlanItems, historicPlanItems, 1000L);

            //Check activationTime if applies and the endTime for not "live" instances
            List<String> livePlanItemInstanceIds = currentPlanItems.stream().map(PlanItemInstance::getId).collect(Collectors.toList());
            assertThat(livePlanItemInstanceIds).isNotEmpty();
            filteredHistoricPlanItemInstances = historicPlanItems.stream().filter(h -> !livePlanItemInstanceIds.contains(h.getId()))
                .collect(Collectors.toList());
            filteredHistoricPlanItemInstances.forEach(assertCreateTimeHistoricPlanItemInstance
                .andThen(assertStartedTimeHistoricPlanItemInstance)
                .andThen(assertEndedTimeHistoricPlanItemInstance)
                .andThen(assertEndStateHistoricPlanItemInstance));

            //Milestone appears now in the MilestoneHistory
            historicMilestones = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicMilestones).hasSize(1);

            //////////////////////////////////////////////////
            //Trigger the last planItem to complete the Case//
            assertCaseInstanceNotEnded(caseInstance);
            task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskB").singleResult();
            assertThat(task).isNotNull();
            cmmnRuntimeService.triggerPlanItemInstance(task.getId());
            assertCaseInstanceEnded(caseInstance);

            //History remains
            historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItems).hasSize(4);
            historicPlanItems.forEach(assertCreateTimeHistoricPlanItemInstance
                .andThen(assertStartedTimeHistoricPlanItemInstance)
                .andThen(assertEndedTimeHistoricPlanItemInstance)
                .andThen(assertEndStateHistoricPlanItemInstance)
            );

            historicMilestones = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicMilestones).hasSize(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testSimpleStage() {
        setClockFixedToCurrentTime();
        Calendar beforeCaseCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        beforeCaseCalendar.add(Calendar.HOUR, -1);
        Date beforeCaseInstance = beforeCaseCalendar.getTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimpleStage").start();
        Calendar afterCaseCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        afterCaseCalendar.add(Calendar.HOUR, 1);
        Date afterCaseInstance = afterCaseCalendar.getTime();

        //Basic case setup check
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
            assertThat(historicPlanItemInstances).hasSize(2);
            assertThat(historicPlanItemInstances.stream().filter(h -> PlanItemDefinitionType.STAGE.equals(h.getPlanItemDefinitionType())).count()).isEqualTo(1);
            assertThat(historicPlanItemInstances.stream().filter(h -> PlanItemDefinitionType.HUMAN_TASK.equals(h.getPlanItemDefinitionType())).count())
                .isEqualTo(1);

            //Check by different criteria
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .createdBefore(afterCaseInstance)
                .createdAfter(beforeCaseInstance)
                .lastAvailableBefore(afterCaseInstance)
                .lastAvailableAfter(beforeCaseInstance)
                .lastStartedBefore(afterCaseInstance)
                .lastStartedAfter(beforeCaseInstance)
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .count()).isEqualTo(2);
        }

        Calendar beforeCompleteCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        beforeCompleteCalendar.add(Calendar.HOUR, -1);
        Date beforeComplete = beforeCompleteCalendar.getTime();
        PlanItemInstance planItemTask = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        Task task = cmmnTaskService.createTaskQuery().subScopeId(planItemTask.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        Date afterComplete = forwardClock(60_000L);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .completedBefore(afterComplete)
                .completedAfter(beforeComplete)
                .list();
            assertThat(historicPlanItemInstances).hasSize(2);
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getExitTime)
                .containsOnlyNulls();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getTerminatedTime)
                .containsOnlyNulls();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getOccurredTime)
                .containsOnlyNulls();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getLastDisabledTime)
                .containsOnlyNulls();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getLastEnabledTime)
                .containsOnlyNulls();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getLastSuspendedTime)
                .containsOnlyNulls();
            assertThat(historicPlanItemInstances)
                .extracting(HistoricPlanItemInstance::getCreateTime)
                .isNotNull();

            historicPlanItemInstances.forEach(h -> {
                assertThat(h.getCreateTime().getTime()).isLessThanOrEqualTo(h.getLastAvailableTime().getTime());
                assertThat(h.getLastAvailableTime().getTime()).isLessThanOrEqualTo(h.getCompletedTime().getTime());
                assertThat(h.getCompletedTime().getTime()).isLessThanOrEqualTo(h.getEndedTime().getTime());
            });
        }

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testEntryAndExitPropagate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testEntryAndExitPropagate").start();

        List<PlanItemInstance> currentPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(currentPlanItems).hasSize(3);
        assertThat(currentPlanItems.stream()
                .filter(p -> PlanItemDefinitionType.STAGE.equalsIgnoreCase(p.getPlanItemDefinitionType()))
                .filter(p -> PlanItemInstanceState.AVAILABLE.equalsIgnoreCase(p.getState()))
                .count()).isEqualTo(1);
        assertThat(currentPlanItems.stream()
                .filter(p -> PlanItemDefinitionType.USER_EVENT_LISTENER.equalsIgnoreCase(p.getPlanItemDefinitionType()))
                .filter(p -> PlanItemInstanceState.AVAILABLE.equalsIgnoreCase(p.getState()))
                .count()).isEqualTo(2);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItems).hasSize(3);
            checkHistoryCreateTimestamp(currentPlanItems, historicPlanItems, 1000L);
            assertThat(historicPlanItems.stream().filter(h -> PlanItemDefinitionType.STAGE.equalsIgnoreCase(h.getPlanItemDefinitionType()))
                .filter(h -> PlanItemInstanceState.AVAILABLE.equalsIgnoreCase(h.getState()))
                .filter(h -> h.getLastAvailableTime() != null && h.getCreateTime().getTime() <= h.getLastAvailableTime().getTime())
                .count()).isEqualTo(1);

            //QUERY FOR STAGES
            historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
                .list();
            assertThat(historicPlanItems).hasSize(1);
            historicPlanItems.forEach(h -> {
                assertThat(h.getLastAvailableTime()).isNotNull();
                assertThat(h.getCreateTime().getTime()).isLessThanOrEqualTo(h.getLastAvailableTime().getTime());
            });

            //QUERY FOR USER_EVENT_LISTENERS
            historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
                .list();
            assertThat(historicPlanItems).hasSize(2);
            historicPlanItems.forEach(h -> {
                assertThat(h.getLastAvailableTime()).isNotNull();
                assertThat(h.getCreateTime().getTime()).isLessThanOrEqualTo(h.getLastAvailableTime().getTime());
            });
        }

        //Fulfill stages entryCriteria - keep date marks for query criteria

        Date occurredAfter = setClockFixedToCurrentTime();
        forwardClock(TimeUnit.MINUTES.toMillis(1));
        PlanItemInstance event = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemStartStageOneEvent").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(event.getId());
        assertCaseInstanceNotEnded(caseInstance);
        forwardClock(TimeUnit.MINUTES.toMillis(1));
        Date occurredBefore = cmmnEngineConfiguration.getClock().getCurrentTime();

        //A userEventListeners is removed and two human task are instanced
        currentPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(currentPlanItems).hasSize(4);

        //Two more planItemInstances in the history
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItems).hasSize(5);
            checkHistoryCreateTimestamp(currentPlanItems, historicPlanItems, 1000L);

            //Both stages should be active now
            historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
            assertThat(historicPlanItems).hasSize(1);
            historicPlanItems.forEach(h -> {
                assertThat(h.getLastStartedTime()).isNotNull();
                assertThat(h.getCreateTime().getTime()).isLessThanOrEqualTo(h.getLastAvailableTime().getTime());
                assertThat(h.getLastAvailableTime().getTime()).isLessThanOrEqualTo(h.getLastStartedTime().getTime());
            });

            //3 new Human Tasks
            historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list();
            assertThat(historicPlanItems).hasSize(2);
            historicPlanItems.forEach(h -> {
                //These are already started/active, but before that should also have available timestamp
                assertThat(h.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                assertThat(h.getLastAvailableTime()).isNotNull();
                assertThat(h.getLastStartedTime()).isNotNull();
                assertThat(h.getCreateTime().getTime()).isLessThanOrEqualTo(h.getLastAvailableTime().getTime());
                assertThat(h.getLastAvailableTime().getTime()).isLessThanOrEqualTo(h.getLastStartedTime().getTime());
            });

            //There should be 2 eventListeners the history, one of them "occurred" and is completed (user event listener plan item instance) and one should still be available
            historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
            assertThat(
                cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).count())
                .isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE).count()).isEqualTo(1);
            historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .occurredAfter(occurredAfter)
                .occurredBefore(occurredBefore)
                .list();
            assertThat(historicPlanItems).hasSize(1);
            historicPlanItems.forEach(h -> {
                //These are "completed" planItemInstance with occurred timestamp and ended timestamp
                assertThat(h.getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
                assertThat(h.getLastAvailableTime()).isNotNull();
                assertThat(h.getOccurredTime()).isNotNull();
                assertThat(h.getEndedTime()).isNotNull();
                assertThat(h.getCreateTime().getTime()).isLessThanOrEqualTo(h.getLastAvailableTime().getTime());
                assertThat(h.getLastAvailableTime().getTime()).isLessThanOrEqualTo(h.getOccurredTime().getTime());
                assertThat(h.getOccurredTime().getTime()).isLessThanOrEqualTo(h.getEndedTime().getTime());
            });
        }

        //Complete one of the Tasks on stageOne
        Date completedAfter = cmmnEngineConfiguration.getClock().getCurrentTime();
        forwardClock(TimeUnit.MINUTES.toMillis(1));
        PlanItemInstance planItemTask = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        Task task = cmmnTaskService.createTaskQuery().subScopeId(planItemTask.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        forwardClock(TimeUnit.MINUTES.toMillis(1));
        Date completedBefore = cmmnEngineConfiguration.getClock().getCurrentTime();

        //one completed task, fetched with completeTime queryCriteria
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricPlanItemInstance historicPlanItem = cmmnHistoryService.createHistoricPlanItemInstanceQuery().completedBefore(completedBefore)
                .completedAfter(completedAfter).singleResult();
            assertThat(historicPlanItem).isNotNull();
            assertThat(historicPlanItem.getElementId()).isEqualTo("planItemTaskA");
            assertThat(historicPlanItem.getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
            assertThat(historicPlanItem.getLastAvailableTime()).isNotNull();
            assertThat(historicPlanItem.getCompletedTime()).isNotNull();
            assertThat(historicPlanItem.getEndedTime()).isNotNull();
            assertThat(historicPlanItem.getCreateTime().getTime()).isLessThanOrEqualTo(historicPlanItem.getLastAvailableTime().getTime());
            assertThat(historicPlanItem.getLastAvailableTime().getTime()).isLessThanOrEqualTo(historicPlanItem.getCompletedTime().getTime());
            assertThat(historicPlanItem.getCompletedTime().getTime()).isLessThanOrEqualTo(historicPlanItem.getEndedTime().getTime());

            // one task still active
            List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list();
            assertThat(historicPlanItems).hasSize(1);
        }

        //Trigger exit criteria of stage one
        Date endedAfter = cmmnEngineConfiguration.getClock().getCurrentTime();
        forwardClock(TimeUnit.MINUTES.toMillis(1));
        event = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemExitStageOneEvent").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(event.getId());
        forwardClock(TimeUnit.MINUTES.toMillis(1));
        Date endedBefore = cmmnEngineConfiguration.getClock().getCurrentTime();

        //Exit condition should have propagated to the remaining task
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .exitBefore(endedBefore)
                .exitAfter(endedAfter)
                .list();
            //The stage and the remaining containing task
            assertThat(historicPlanItems).hasSize(2);
            assertThat(historicPlanItems.stream().anyMatch(h -> PlanItemDefinitionType.STAGE.equalsIgnoreCase(h.getPlanItemDefinitionType()))).isTrue();
            assertThat(historicPlanItems.stream().anyMatch(h -> PlanItemDefinitionType.HUMAN_TASK.equalsIgnoreCase(h.getPlanItemDefinitionType()))).isTrue();

            historicPlanItems.forEach(h -> {
                assertThat(h.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
                assertThat(h.getLastAvailableTime()).isNotNull();
                assertThat(h.getExitTime()).isNotNull();
                assertThat(h.getEndedTime()).isNotNull();
                assertThat(h.getCreateTime().getTime()).isLessThanOrEqualTo(h.getLastAvailableTime().getTime());
                assertThat(h.getLastAvailableTime().getTime()).isLessThanOrEqualTo(h.getExitTime().getTime());
                assertThat(h.getExitTime().getTime()).isLessThanOrEqualTo(h.getEndedTime().getTime());
            });
        }

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    @SuppressWarnings("unchecked")
    public void testSimpleRepetitionHistory() {
        int totalRepetitions = 5;
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSimpleRepetitionHistory")
                .variable("totalRepetitions", totalRepetitions)
                .start();

        for (int i = 1; i <= totalRepetitions; i++) {
            PlanItemInstance repeatingTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("repeatingTaskPlanItem")
                    .singleResult();
            assertThat(repeatingTaskPlanItemInstance).isNotNull();
            assertThat(repeatingTaskPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

            //History Before task execution
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricPlanItemInstance> historyBefore = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
                Map<String, List<HistoricPlanItemInstance>> historyBeforeByState = historyBefore.stream()
                    .collect(Collectors.groupingBy(HistoricPlanItemInstance::getState));
                assertThat(historyBeforeByState.get(PlanItemInstanceState.ACTIVE)).hasSize(1);
                assertThat(historyBeforeByState.getOrDefault(PlanItemInstanceState.COMPLETED, Collections.EMPTY_LIST)).hasSize(i - 1);

                //Sanity check Active planItemInstance
                assertThat(historyBeforeByState.get(PlanItemInstanceState.ACTIVE).get(0).getId()).isEqualTo(repeatingTaskPlanItemInstance.getId());
                //Sanity check repetition counter
                HistoricVariableInstance historicRepetitionCounter = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .planItemInstanceId(repeatingTaskPlanItemInstance.getId()).singleResult();
                assertThat(historicRepetitionCounter).isNotNull();
                assertThat(historicRepetitionCounter.getValue()).isEqualTo(i);
            }

            //Execute the repetition
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).subScopeId(repeatingTaskPlanItemInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            cmmnTaskService.complete(task.getId());

            //History Before task execution
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricPlanItemInstance> historyAfter = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
                Map<String, List<HistoricPlanItemInstance>> historyAfterByState = historyAfter.stream()
                    .collect(Collectors.groupingBy(HistoricPlanItemInstance::getState));
                assertThat(historyAfterByState.getOrDefault(PlanItemInstanceState.ACTIVE, Collections.EMPTY_LIST)).hasSize(i == totalRepetitions ? 0 : 1);
                assertThat(historyAfterByState.getOrDefault(PlanItemInstanceState.COMPLETED, Collections.EMPTY_LIST)).hasSize(i);
            }
        }

        //Check history in sequence
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> history = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
            history.sort((o1, o2) -> {
                int order1 = (int) cmmnHistoryService.createHistoricVariableInstanceQuery().planItemInstanceId(o1.getId()).singleResult().getValue();
                int order2 = (int) cmmnHistoryService.createHistoricVariableInstanceQuery().planItemInstanceId(o2.getId()).singleResult().getValue();
                return Integer.compare(order1, order2);
            });

            long previousCreateTime = 0L;
            long previousActivateTime = 0L;
            long previousEndTime = 0L;
            for (HistoricPlanItemInstance h : history) {
                assertCreateTimeHistoricPlanItemInstance
                    .andThen(assertStartedTimeHistoricPlanItemInstance)
                    .andThen(assertEndedTimeHistoricPlanItemInstance)
                    .andThen(assertEndStateHistoricPlanItemInstance)
                    .accept(h);
                assertThat(previousCreateTime).isLessThanOrEqualTo(h.getCreateTime().getTime());
                assertThat(previousActivateTime).isLessThanOrEqualTo(h.getLastStartedTime().getTime());
                assertThat(previousEndTime).isLessThanOrEqualTo(h.getEndedTime().getTime());
                previousCreateTime = h.getCreateTime().getTime();
                previousActivateTime = h.getLastStartedTime().getTime();
                previousEndTime = h.getEndedTime().getTime();
            }
        }

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testQueryByUnavailableState() {
        Date startTime = new Date();
        setClockTo(startTime);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAvailableCondition").start();

        PlanItemInstance eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);

        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(taskA.getId());
        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult().getId());

        eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceId(eventListenerPlanItemInstance.getId()).singleResult().getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.UNAVAILABLE).list();
            assertThat(historicPlanItemInstances).extracting(HistoricPlanItemInstance::getName).containsExactly("myEventListener");
        }

        Date afterStartTime = new Date(startTime.getTime() + 10000L);
        setClockTo(afterStartTime);

        // UnavailableAfter

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastUnavailableAfter(startTime).list();
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("myEventListener");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().lastUnavailableAfter(startTime).list();
            assertThat(historicPlanItemInstances).extracting(HistoricPlanItemInstance::getName).containsExactly("myEventListener");
        }

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastUnavailableAfter(afterStartTime).list();
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).isEmpty();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().lastUnavailableAfter(afterStartTime).list();
            assertThat(historicPlanItemInstances).extracting(HistoricPlanItemInstance::getName).isEmpty();
        }

        // UnavailableBefore
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastUnavailableBefore(afterStartTime).list();
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("myEventListener");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().lastUnavailableBefore(afterStartTime).list();
            assertThat(historicPlanItemInstances).extracting(HistoricPlanItemInstance::getName).containsExactly("myEventListener");
        }

        Date beforeStartTime = new Date(startTime.getTime() - 10000);
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceLastUnavailableBefore(beforeStartTime).list();
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).isEmpty();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().lastUnavailableBefore(beforeStartTime).list();
            assertThat(historicPlanItemInstances).extracting(HistoricPlanItemInstance::getName).isEmpty();
        }
    }

    private List<String> getIdsOfNonWaitingPlanItemInstances(List<PlanItemInstance> currentPlanItems) {
        return currentPlanItems.stream()
                .filter(p -> !PlanItemInstanceState.AVAILABLE.equals(p.getState()))
                .map(PlanItemInstance::getId)
                .collect(Collectors.toList());
    }

    private void checkHistoryCreateTimestamp(final List<PlanItemInstance> currentPlanItems, final List<HistoricPlanItemInstance> historicPlanItemInstances,
            long threshold) {
        currentPlanItems.forEach(p -> {
            Optional<Long> createTimestamp = historicPlanItemInstances.stream()
                    .filter(h -> h.getId().equals(p.getId()))
                    .findFirst()
                    .map(HistoricPlanItemInstance::getCreateTime)
                    .map(Date::getTime);
            assertThat(createTimestamp).isPresent();
            long delta = createTimestamp.orElse(Long.MAX_VALUE) - p.getCreateTime().getTime();
            assertThat(delta).isLessThanOrEqualTo(threshold);
        });
    }
}