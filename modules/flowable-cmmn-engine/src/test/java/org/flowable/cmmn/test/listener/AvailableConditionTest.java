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
package org.flowable.cmmn.test.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class AvailableConditionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testAvailableConditionInPlanModelPlanItemInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAvailableConditionInPlanModelPlanItemInstance").start();

        // The plan item instance for the event listener should have been created in the unavailable state, as the condition is not true.
        PlanItemInstance eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
            .singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceId(eventListenerPlanItemInstance.getId()).singleResult().getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);
        }

        // The event listener query should not return them as they are unavailable
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).list()).isEmpty();
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).stateUnavailable().list()).hasSize(1);

        // After case instance start human task A should be active, human taskA B should be enabled
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskA.getName()).isEqualTo("A");

        PlanItemInstance planItemInstanceForB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        assertThat(planItemInstanceForB.getName()).isEqualTo("B");

        // Completing the human task A should mark the stage as completable
        cmmnTaskService.complete(taskA.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult()).isNotNull();

        // The stage being completable, this should make the user event listener available
        PlanItemInstance userEventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
            .singleResult();
        assertThat(userEventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceId(userEventListenerPlanItemInstance.getId()).singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        }

        // Completing the user event listener should terminate the case
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .singleResult();
        assertThat(userEventListenerInstance.getId()).isEqualTo(userEventListenerPlanItemInstance.getId());
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testAvailableConditionWithEventListenerInStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAvailableCondition").start();

        // The plan item instance for the event listener should have been created in the unavailable state, as the condition is not true.
        PlanItemInstance eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
            .singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);

        // After case instance start human task A should be active, human taskA B should be enabled
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskA.getName()).isEqualTo("A");

        PlanItemInstance planItemInstanceForB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        assertThat(planItemInstanceForB.getName()).isEqualTo("B");

        // Completing the human task A should mark the stage as completable
        cmmnTaskService.complete(taskA.getId());
        PlanItemInstance stage1PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 1").singleResult();
        assertThat(stage1PlanItemInstance.isCompletable()).isTrue();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().stageInstanceId(stage1PlanItemInstance.getId()).list()).isNotEmpty();

        // The stage being completable, this should make the user event listener available
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list()).hasSize(1);

        // Completing the user event listener should exit stage 1 and activate Stage 2
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        Task taskC = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskC.getName()).isEqualTo("C");
    }

    @Test
    @CmmnDeployment
    public void testAvailableConditionDismisses() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAvailableCondition").start();

        // The plan item instance for the event listener should have been created in the unavailable state, as the condition is not true.
        PlanItemInstance eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);

        // After case instance start human task A should be active, human taskA B should be enabled
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskA.getName()).isEqualTo("A");

        PlanItemInstance planItemInstanceForB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        assertThat(planItemInstanceForB.getName()).isEqualTo("B");

        // Completing the human task A should mark the stage as completable
        cmmnTaskService.complete(taskA.getId());

        // The stage being completable, this should make the event listener available
        eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceId(eventListenerPlanItemInstance.getId()).singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        }

        // By starting task B now however, the available condition is now false and the event listener is now unavailable again
        cmmnRuntimeService.startPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult().getId());
        eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceId(eventListenerPlanItemInstance.getId()).singleResult().getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);
        }

        // Completing b makes the stage completable, which makes the event listener available again
        eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(eventListenerPlanItemInstance.getEndedTime()).isNull();
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("B").singleResult().getId());

        eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(eventListenerPlanItemInstance.getEndedTime()).isNull();

        // Completing the event listener instance, completes the stage
        cmmnRuntimeService.completeGenericEventListenerInstance(eventListenerPlanItemInstance.getId());
        eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).includeEnded().singleResult();
        assertThat(eventListenerPlanItemInstance.getEndedTime()).isNotNull();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();

        Task taskC = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskC.getName()).isEqualTo("C");
    }

    @Test
    @CmmnDeployment
    public void testStageCompletionDependingOnAvailableEventListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testStageCompleteWithAvailableEventListener")
            .start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateAvailable().count()).isZero();

        // Completing the task should make the available condition (cmmn:isStageCompletable()) true and make the eventlistener available)
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateAvailable().count()).isEqualTo(1);
        assertCaseInstanceNotEnded(caseInstance);

        cmmnRuntimeService.completeUserEventListenerInstance(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertCaseInstanceEnded(caseInstance);
    }

}
