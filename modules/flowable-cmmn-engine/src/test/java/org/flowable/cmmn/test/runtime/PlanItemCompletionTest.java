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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.UNAVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.Test;

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

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", UNAVAILABLE);

        // activate task and complete it the first time will make the user listener available
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getAllPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED, COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", AVAILABLE);

        // trigger user listener to complete stage
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "User Listener A"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testManualActivatedTaskWithRepetitionIgnoreAfterFirstCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("requiredTaskWithRepetitionAndManualActivation").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", AVAILABLE);

        // Completing the task should not complete the case instance, even when set to 'ignoreAfterFirstCompletion',
        // as the event listener is still there in the available state.
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().singleResult().getId());

        planItemInstances = getAllPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED, COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", AVAILABLE);

        // trigger user listener to complete stage
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "User Listener A"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testManualActivatedTaskWithRepetitionIgnoredAfterFirstCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("requiredTaskWithRepetitionAndManualActivation").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "User Listener A", UNAVAILABLE);

        // activate task and complete it the first time will complete the case as it will be ignored after first completion
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(userEventListenerInstance.getState()).isEqualTo(AVAILABLE);

        cmmnRuntimeService.completeGenericEventListenerInstance(userEventListenerInstance.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testNestedComplexCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestingPlanItems").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // start and complete Task A -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // complete Task C -> nothing yet to happen
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // start Task B -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // complete Task B -> Stage B and then Stage A need to complete
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(1);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // start and complete Task E -> case must be completed, as Task E is ignored after first completion
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task E"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task E"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testNestedComplexCompletionAlternatePath() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestingPlanItems").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // start Task D and E -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task D"));
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task E"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);

        // complete Task C and Task D -> still nothing yet to happen
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task D"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);

        // start and complete Task A -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);

        // start Task B -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);

        // complete Task E -> nothing further changes
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task E"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ENABLED);

        // complete Task B -> case must be completed now
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testComplexCompletionWithoutAutocompletion() {
        CaseInstance caseInstance = runComplexCompletionTestScenario(false);

        // because autocompletion is off, we still stay in Stage A
        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
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
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    protected CaseInstance runComplexCompletionTestScenario(boolean autocompleteEnabled) {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCompletionWithConditions")
                .variable("autocompleteEnabled", autocompleteEnabled)
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(9);
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
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(9);
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

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(9);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task H", ENABLED);

        // start Task B and D and complete C
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task D", ENABLED));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(8);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task H", ENABLED);

        // complete Task B and D
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task D", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task H", ENABLED);

        // start and complete Task H
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task H"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task H"));

        // enable Task F through making its condition true and then start it
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskF", true);
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task F"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task F", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ENABLED);

        // complete Task F and start Task G
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task F"));
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task G"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);

        // complete Task G and depending on autocompletion being on or off, we stay in Stage A or the case will be completed
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task G"));

        return caseInstance;
    }
}
