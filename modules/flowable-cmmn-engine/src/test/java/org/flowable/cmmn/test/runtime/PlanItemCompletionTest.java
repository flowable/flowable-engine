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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Objects;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;
import org.opentest4j.AssertionFailedError;

/**
 * Adds testing around plan item completion evaluation.
 *
 * @author Micha Kiener
 */
public class PlanItemCompletionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testManualActivatedTaskWithRepetition() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("requiredTaskWithRepetitionAndManualActivation").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", UNAVAILABLE);

        // activate task and complete it the first time will make the user listener available
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .orderByEndTime().asc()
            .includeEnded()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED, COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", AVAILABLE);

        // trigger user listener to complete stage
        cmmnRuntimeService.completeUserEventListenerInstance(planItemInstances.get(3).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testManualActivatedTaskWithRepetitionIgnoreAfterFirstCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("requiredTaskWithRepetitionAndManualActivation").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", AVAILABLE);

        // Completing the task should not complete the case instance, even when set to 'ignoreAfterFirstCompletion',
        // as the event listener is still there in the available state.
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(1).getId());
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().singleResult().getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .orderByEndTime().asc()
            .includeEnded()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED, COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", AVAILABLE);

        // trigger user listener to complete stage
        cmmnRuntimeService.completeUserEventListenerInstance(planItemInstances.get(3).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testManualActivatedTaskWithRepetitionIgnoredAfterFirstCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("requiredTaskWithRepetitionAndManualActivation").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", UNAVAILABLE);

        // activate task and complete it the first time will complete the case as it will be ignored after first completion
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testNestedComplexCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestingPlanItems").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(7, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // start and complete Task A -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // complete Task C -> nothing yet to happen
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(5, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // start Task B -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(5, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // complete Task B -> Stage B and then Stage A need to complete
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(1, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // start and complete Task E -> case must be completed, as Task E is ignored after first completion
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(0).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testNestedComplexCompletionAlternatePath() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestingPlanItems").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(7, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // start Task D and E -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(5).getId());
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(6).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(7, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);

        // complete Task C and Task D -> still nothing yet to happen
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(4).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(5).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(5, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);

        // start and complete Task A -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);

        // start Task B -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);

        // complete Task E -> nothing further changes
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // complete Task B -> case must be completed now
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testComplexCompletionWithoutAutocompletion() {
        CaseInstance caseInstance = runComplexCompletionTestScenario(false);

        // because autocompletion is off, we still stay in Stage A
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
    }

    @Test
    @CmmnDeployment
    public void testComplexCompletionWithAutocompletion() {
        runComplexCompletionTestScenario(true);

        // because autocompletion is on, the case will be completed
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    protected CaseInstance runComplexCompletionTestScenario(boolean autocompleteEnabled) {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testCompletionWithConditions")
            .variable("autocompleteEnabled", autocompleteEnabled)
            .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(9, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task H", ENABLED);

        // complete Task A -> will enable B and D, C stays in available as it has a condition
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(9, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task H", ENABLED);

        // activate Task C by setting the flag making its condition true
        cmmnRuntimeService.setVariable(caseInstance.getId(), "activateTaskC", true);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(9, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task H", ENABLED);

        // start Task B and D and complete C
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        // Task D has 2 states (enabled and waiting for repetition),
        // so we should explicitly get the enabled state for starting
        PlanItemInstance taskD = planItemInstances.stream()
            .filter(pi -> Objects.equals("Task D", pi.getName()))
            .filter(pi -> Objects.equals(ENABLED, pi.getState()))
            .findFirst()
            .orElseThrow(() -> new AssertionFailedError("Could not find enabled Task D"));
        cmmnRuntimeService.startPlanItemInstance(taskD.getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(8, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task H", ENABLED);

        // complete Task B and D
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        // Task D has 2 states (active and waiting for repetition),
        // so we should explicitly get the enabled state for triggering
        taskD = planItemInstances.stream()
            .filter(pi -> Objects.equals("Task D", pi.getName()))
            .filter(pi -> Objects.equals(ACTIVE, pi.getState()))
            .findFirst()
            .orElseThrow(() -> new AssertionFailedError("Could not find active Task D"));
        cmmnRuntimeService.triggerPlanItemInstance(taskD.getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(6, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task H", ENABLED);

        // start and complete Task H
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(5).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(5).getId());

        // enable Task F through making its condition true and then start it
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskF", true);
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(5, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);

        // complete Task F and start Task G
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(4).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);

        // complete Task G and depending on autocompletion being on or off, we stay in Stage A or the case will be completed
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        return caseInstance;
    }
}
