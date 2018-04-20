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

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dennis Federico
 */
public class PlanItemInstanceHistoryServiceTest extends FlowableCmmnTestCase {

    private static Consumer<HistoricPlanItemInstance> assertStartTimeHistoricPlanItemInstance = h -> {
        assertNotNull(h.getStartTime());
        assertTrue(h.getStartTime().getTime() >= 0L);
    };

    private static Consumer<HistoricPlanItemInstance> assertActivationTimeHistoricPlanItemInstance = h -> {
        assertNotNull(h.getActivationTime());
        assertTrue(h.getActivationTime().getTime() >= h.getStartTime().getTime());
    };

    private static Consumer<HistoricPlanItemInstance> assertEndTimeHistoricPlanItemInstance = h -> {
        assertNotNull(h.getEndTime());
        assertTrue(h.getEndTime().getTime() >= h.getActivationTime().getTime());
    };

    private static Consumer<HistoricPlanItemInstance> assertEndStateHistoricPlanItemInstance = h -> {
        assertNotNull(h.getState());
        assertTrue(PlanItemInstanceState.END_STATES.contains(h.getState()));
    };

    private static Consumer<HistoricPlanItemInstance> assertActiveStateHistoricPlanItemInstance = h -> {
        assertNotNull(h.getState());
        assertTrue(PlanItemInstanceState.ACTIVE.contains(h.getState()) ||
                PlanItemInstanceState.ENABLED.contains(h.getState()) ||
                PlanItemInstanceState.ASYNC_ACTIVE.contains(h.getState()));
    };

    @Test
    @CmmnDeployment
    public void testSimpleCaseFlow() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimplePlanItem").start();

        //one Task, one Stage, one Milestone
        List<PlanItemInstance> currentPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertNotNull(currentPlanItems);
        assertEquals(3, currentPlanItems.size());
        assertTrue(currentPlanItems.stream().map(PlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.STAGE::equalsIgnoreCase));
        assertTrue(currentPlanItems.stream().map(PlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.MILESTONE::equalsIgnoreCase));
        assertTrue(currentPlanItems.stream().map(PlanItemInstance::getPlanItemDefinitionType).anyMatch("task"::equalsIgnoreCase));

        //Milestone are just another planItem too, so it will appear in the planItemInstance History
        List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
        assertNotNull(historicPlanItems);
        assertEquals(3, historicPlanItems.size());
        assertTrue(historicPlanItems.stream().map(HistoricPlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.STAGE::equalsIgnoreCase));
        assertTrue(historicPlanItems.stream().map(HistoricPlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.MILESTONE::equalsIgnoreCase));
        assertTrue(historicPlanItems.stream().anyMatch(h -> "task".equalsIgnoreCase(h.getPlanItemDefinitionType()) && "planItemTaskA".equalsIgnoreCase(h.getElementId())));

        //Check Start timeStamp within the second of its original creation
        historicPlanItems.forEach(assertStartTimeHistoricPlanItemInstance);
        checkHistoryStartTimestamp(currentPlanItems, historicPlanItems, 1000L);

        //Check activation timestamp... for those "live" instances not in "Waiting" state (i.e. AVAILABLE)
        List<String> nonWaitingPlanIntanceIds = getIdsOfNonWaitingPlanItemInstances(currentPlanItems);
        assertFalse(nonWaitingPlanIntanceIds.isEmpty());
        List<HistoricPlanItemInstance> filteredHistoricPlanItemInstances = historicPlanItems.stream().filter(h -> nonWaitingPlanIntanceIds.contains(h.getId())).collect(Collectors.toList());
        assertFalse(filteredHistoricPlanItemInstances.isEmpty());
        filteredHistoricPlanItemInstances.forEach(assertStartTimeHistoricPlanItemInstance
                .andThen(assertActivationTimeHistoricPlanItemInstance)
                .andThen(assertActiveStateHistoricPlanItemInstance)
                .andThen(assertActiveStateHistoricPlanItemInstance));

        //No planItemInstance has "ended" yet, so no historicPlanItemInstance should have endTime timestamp
        historicPlanItems.forEach(h -> assertNull(h.getEndTime()));

        //Milestone history is only filled when the milestone occurs
        List<HistoricMilestoneInstance> historicMilestones = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
        assertNotNull(historicMilestones);
        assertTrue(historicMilestones.isEmpty());

        //////////////////////////////////////////////////////////////////
        //Trigger the task to reach the milestone and activate the stage//
        assertCaseInstanceNotEnded(caseInstance);
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertNotNull(task);
        cmmnRuntimeService.triggerPlanItemInstance(task.getId());
        assertCaseInstanceNotEnded(caseInstance);

        //Now there are 2 plan items in a non-final state, a Stage and its containing task (only 1 new planItem)
        currentPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertNotNull(currentPlanItems);
        assertEquals(2, currentPlanItems.size());
        assertTrue(currentPlanItems.stream().map(PlanItemInstance::getPlanItemDefinitionType).anyMatch(PlanItemDefinitionType.STAGE::equalsIgnoreCase));
        assertTrue(currentPlanItems.stream().anyMatch(p -> "task".equalsIgnoreCase(p.getPlanItemDefinitionType()) && "planItemTaskB".equalsIgnoreCase(p.getElementId())));

        historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
        assertNotNull(historicPlanItems);
        assertEquals(4, historicPlanItems.size());
        //Check start timestamps of newly added timeStamp within the second of its original creation
        checkHistoryStartTimestamp(currentPlanItems, historicPlanItems, 1000L);

        //Check activationTime if applies and the endTime for not "live" instances
        List<String> livePlanItemInstanceIds = currentPlanItems.stream().map(PlanItemInstance::getId).collect(Collectors.toList());
        assertFalse(livePlanItemInstanceIds.isEmpty());
        filteredHistoricPlanItemInstances = historicPlanItems.stream().filter(h -> !livePlanItemInstanceIds.contains(h.getId())).collect(Collectors.toList());
        filteredHistoricPlanItemInstances.forEach(assertStartTimeHistoricPlanItemInstance
                .andThen(assertActivationTimeHistoricPlanItemInstance)
                .andThen(assertEndTimeHistoricPlanItemInstance)
                .andThen(assertEndStateHistoricPlanItemInstance));

        //Milestone appears now in the MilestoneHistory
        historicMilestones = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
        assertNotNull(historicMilestones);
        assertEquals(1, historicMilestones.size());

        //////////////////////////////////////////////////
        //Trigger the last planItem to complete the Case//
        assertCaseInstanceNotEnded(caseInstance);
        task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskB").singleResult();
        assertNotNull(task);
        cmmnRuntimeService.triggerPlanItemInstance(task.getId());
        assertCaseInstanceEnded(caseInstance);

        //History remains
        historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
        assertNotNull(historicPlanItems);
        assertEquals(4, historicPlanItems.size());
        historicPlanItems.forEach(assertStartTimeHistoricPlanItemInstance
                .andThen(assertActivationTimeHistoricPlanItemInstance)
                .andThen(assertEndTimeHistoricPlanItemInstance)
                .andThen(assertEndStateHistoricPlanItemInstance)
        );

        historicMilestones = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
        assertNotNull(historicMilestones);
        assertEquals(1, historicMilestones.size());
    }

    @Test
    @CmmnDeployment
    public void testSimpleRepetitionHistory() {
        int totalRepetitions = 5;
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSimpleRepetitionHistory")
                .variable("totalRepetitions", totalRepetitions)
                .start();

        for (int i = 1; i <= totalRepetitions; i++) {
            PlanItemInstance repeatingTaskPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("repeatingTaskPlanItem").singleResult();
            assertNotNull(repeatingTaskPlanItemInstance);
            assertEquals(PlanItemInstanceState.ACTIVE, repeatingTaskPlanItemInstance.getState());

            //History Before task execution
            List<HistoricPlanItemInstance> historyBefore = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
            Map<String, List<HistoricPlanItemInstance>> historyBeforeByState = historyBefore.stream().collect(Collectors.groupingBy(HistoricPlanItemInstance::getState));
            assertEquals(1, historyBeforeByState.get(PlanItemInstanceState.ACTIVE).size());
            assertEquals(i - 1, historyBeforeByState.getOrDefault(PlanItemInstanceState.COMPLETED, Collections.EMPTY_LIST).size());

            //Sanity check Active planItemInstance
            assertEquals(repeatingTaskPlanItemInstance.getId(), historyBeforeByState.get(PlanItemInstanceState.ACTIVE).get(0).getId());
            //Sanity check repetition counter
            HistoricVariableInstance historicRepetitionCounter = cmmnHistoryService.createHistoricVariableInstanceQuery().planItemInstanceId(repeatingTaskPlanItemInstance.getId()).singleResult();
            assertNotNull(historicRepetitionCounter);
            assertEquals(i, historicRepetitionCounter.getValue());

            //Execute the repetition
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).subScopeId(repeatingTaskPlanItemInstance.getId()).singleResult();
            assertNotNull(task);
            cmmnTaskService.complete(task.getId());

            //History Before task execution
            List<HistoricPlanItemInstance> historyAfter = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
            Map<String, List<HistoricPlanItemInstance>> historyAfterByState = historyAfter.stream().collect(Collectors.groupingBy(HistoricPlanItemInstance::getState));
            assertEquals(i == totalRepetitions ? 0 : 1, historyAfterByState.getOrDefault(PlanItemInstanceState.ACTIVE, Collections.EMPTY_LIST).size());
            assertEquals(i, historyAfterByState.getOrDefault(PlanItemInstanceState.COMPLETED, Collections.EMPTY_LIST).size());
        }

        //Check history in sequence
        List<HistoricPlanItemInstance> history = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        history.sort((o1, o2) -> {
            int order1 = (int) cmmnHistoryService.createHistoricVariableInstanceQuery().planItemInstanceId(o1.getId()).singleResult().getValue();
            int order2 = (int) cmmnHistoryService.createHistoricVariableInstanceQuery().planItemInstanceId(o2.getId()).singleResult().getValue();
            return Integer.compare(order1, order2);
        });

        long previousStartTime = 0L;
        long previousActivateTime = 0L;
        long previousEndTime = 0L;
        for (HistoricPlanItemInstance h : history) {
            assertStartTimeHistoricPlanItemInstance
                    .andThen(assertActivationTimeHistoricPlanItemInstance)
                    .andThen(assertEndTimeHistoricPlanItemInstance)
                    .andThen(assertEndStateHistoricPlanItemInstance)
                    .accept(h);
            assertTrue(previousStartTime <= h.getStartTime().getTime());
            assertTrue(previousActivateTime <= h.getActivationTime().getTime());
            assertTrue(previousEndTime <= h.getEndTime().getTime());
            previousStartTime = h.getStartTime().getTime();
            previousActivateTime = h.getActivationTime().getTime();
            previousEndTime = h.getEndTime().getTime();
        }

        assertCaseInstanceEnded(caseInstance);
    }

    private List<String> getIdsOfNonWaitingPlanItemInstances(List<PlanItemInstance> currentPlanItems) {
        return currentPlanItems.stream()
                .filter(p -> !PlanItemInstanceState.AVAILABLE.equals(p.getState()))
                .map(PlanItemInstance::getId)
                .collect(Collectors.toList());
    }

    private void checkHistoryStartTimestamp(final List<PlanItemInstance> currentPlanItems, final List<HistoricPlanItemInstance> historicPlanItems1, long threshold) {
        currentPlanItems.forEach(p -> {
            Optional<Long> startTimestamp = historicPlanItems1.stream().filter(h -> h.getId().equals(p.getId())).findFirst().map(HistoricPlanItemInstance::getStartTime).map(Date::getTime);
            assertTrue(startTimestamp.isPresent());
            long delta = startTimestamp.orElse(Long.MAX_VALUE) - p.getStartTime().getTime();
            assertTrue(delta <= threshold);
        });
    }

}
