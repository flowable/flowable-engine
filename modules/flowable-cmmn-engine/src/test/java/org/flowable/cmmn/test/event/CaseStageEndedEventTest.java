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
package org.flowable.cmmn.test.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.cmmn.api.event.FlowableCaseStageEndedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Micha Kiener
 */
public class CaseStageEndedEventTest extends FlowableCmmnTestCase {
    protected CustomEventListener stageListener;

    @BeforeEach
    public void setUp() {
        stageListener = new CustomEventListener();
        cmmnEngineConfiguration.getEventDispatcher().addEventListener(stageListener, FlowableEngineEventType.STAGE_ENDED);
    }

    @AfterEach
    public void tearDown() {
        if (stageListener != null) {
            cmmnEngineConfiguration.getEventDispatcher().removeEventListener(stageListener);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/event/Case_Stage_Event_Test_Case.cmmn")
    public void testCaseStageCompletedEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        stageListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseStageEndedEvent caseStageEndedEvent) {
                CaseInstance eventCaseInstance = caseStageEndedEvent.getCaseInstance();
                assertThat(caseStageEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseStageEndedEvent.getExecutionId()).isNull();
                assertThat(caseStageEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseStageEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseStageEndedEvent.getScopeId()).isNotNull().isEqualTo(eventCaseInstance.getId());
                assertThat(caseStageEndedEvent.getSubScopeId()).isNotNull().isEqualTo(caseStageEndedEvent.getEntity().getId());
                assertThat(caseStageEndedEvent.getScopeDefinitionId()).isNotNull().isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage B");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.COMPLETED);
                } else {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage A");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.COMPLETED);
                }
                events.add(flowableEvent);
            }
        };

        // start the case which will also need to throw two stage started events (Stage A and embedded child Stage B)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseStageEventTestCase")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", PlanItemInstanceState.ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        assertThat(events).hasSize(1);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
        assertThat(events).hasSize(2);

        assertCaseInstanceEnded(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/event/Case_Stage_Event_Test_Case.cmmn")
    public void testCaseStageForceCompletedEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        stageListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseStageEndedEvent caseStageEndedEvent) {
                CaseInstance eventCaseInstance = caseStageEndedEvent.getCaseInstance();
                assertThat(caseStageEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseStageEndedEvent.getExecutionId()).isNull();
                assertThat(caseStageEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseStageEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseStageEndedEvent.getScopeId()).isNotNull().isEqualTo(eventCaseInstance.getId());
                assertThat(caseStageEndedEvent.getSubScopeId()).isNotNull().isEqualTo(caseStageEndedEvent.getEntity().getId());
                assertThat(caseStageEndedEvent.getScopeDefinitionId()).isNotNull().isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage A");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.COMPLETED);
                } else {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage B");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.TERMINATED);
                }
                events.add(flowableEvent);
            }
        };

        // start the case which will also need to throw two stage started events (Stage A and embedded child Stage B)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseStageEventTestCase")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", PlanItemInstanceState.ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Complete Stage A"));
        assertThat(events).hasSize(2);

        assertCaseInstanceEnded(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/event/Case_Stage_Event_Test_Case.cmmn")
    public void testCaseStageForceCompletedAfterSubStageCompletedEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        stageListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseStageEndedEvent caseStageEndedEvent) {
                CaseInstance eventCaseInstance = caseStageEndedEvent.getCaseInstance();
                assertThat(caseStageEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseStageEndedEvent.getExecutionId()).isNull();
                assertThat(caseStageEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseStageEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseStageEndedEvent.getScopeId()).isNotNull().isEqualTo(eventCaseInstance.getId());
                assertThat(caseStageEndedEvent.getSubScopeId()).isNotNull().isEqualTo(caseStageEndedEvent.getEntity().getId());
                assertThat(caseStageEndedEvent.getScopeDefinitionId()).isNotNull().isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage B");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.COMPLETED);
                } else {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage A");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.COMPLETED);
                }
                events.add(flowableEvent);
            }
        };

        // start the case which will also need to throw two stage started events (Stage A and embedded child Stage B)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseStageEventTestCase")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", PlanItemInstanceState.ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        assertThat(events).hasSize(1);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Complete Stage A"));
        assertThat(events).hasSize(2);

        assertCaseInstanceEnded(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/event/Case_Stage_Event_Test_Case.cmmn")
    public void testCaseStageForceExitEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        stageListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseStageEndedEvent caseStageEndedEvent) {
                CaseInstance eventCaseInstance = caseStageEndedEvent.getCaseInstance();
                assertThat(caseStageEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseStageEndedEvent.getExecutionId()).isNull();
                assertThat(caseStageEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseStageEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseStageEndedEvent.getScopeId()).isNotNull().isEqualTo(eventCaseInstance.getId());
                assertThat(caseStageEndedEvent.getSubScopeId()).isNotNull().isEqualTo(caseStageEndedEvent.getEntity().getId());
                assertThat(caseStageEndedEvent.getScopeDefinitionId()).isNotNull().isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage A");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.TERMINATED);
                } else {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage B");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.TERMINATED);
                }
                events.add(flowableEvent);
            }
        };

        // start the case which will also need to throw two stage started events (Stage A and embedded child Stage B)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseStageEventTestCase")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", PlanItemInstanceState.ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Exit Stage A"));
        assertThat(events).hasSize(2);

        assertCaseInstanceEnded(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/event/Case_Stage_Event_Test_Case.cmmn")
    public void testCaseStageForceExitAfterSubStageCompletedEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        stageListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseStageEndedEvent caseStageEndedEvent) {
                CaseInstance eventCaseInstance = caseStageEndedEvent.getCaseInstance();
                assertThat(caseStageEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseStageEndedEvent.getExecutionId()).isNull();
                assertThat(caseStageEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseStageEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseStageEndedEvent.getScopeId()).isNotNull().isEqualTo(eventCaseInstance.getId());
                assertThat(caseStageEndedEvent.getSubScopeId()).isNotNull().isEqualTo(caseStageEndedEvent.getEntity().getId());
                assertThat(caseStageEndedEvent.getScopeDefinitionId()).isNotNull().isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage B");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.COMPLETED);
                } else {
                    assertThat(caseStageEndedEvent.getEntity().getName()).isEqualTo("Stage A");
                    assertThat(caseStageEndedEvent.getEndingState()).isEqualTo(PlanItemInstanceState.TERMINATED);
                }
                events.add(flowableEvent);
            }
        };

        // start the case which will also need to throw two stage started events (Stage A and embedded child Stage B)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseStageEventTestCase")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", PlanItemInstanceState.ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        assertThat(events).hasSize(1);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Exit Stage A"));
        assertThat(events).hasSize(2);

        assertCaseInstanceEnded(caseInstance.getId());
    }

    public static class CustomEventListener extends AbstractFlowableEventListener {
        private Consumer<FlowableEvent> eventConsumer;

        @Override
        public void onEvent(FlowableEvent event) {
            if (eventConsumer != null) {
                eventConsumer.accept(event);
            }
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }
}
