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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.cmmn.api.event.FlowableCaseEndedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testing the case ended events.
 *
 * @author Micha Kiener
 */
public class CaseEndedEventTests extends FlowableCmmnTestCase {
    protected TestEventListener listener;

    @BeforeEach
    public void setUp() {
        listener = new TestEventListener();
        cmmnEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    public void tearDown() {
        if (listener != null) {
            cmmnEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testCaseInstanceEndedEvent() {
        List<FlowableCaseEndedEvent> events = new ArrayList<>();
        listener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseEndedEvent caseEndedEvent) {
                CaseInstance eventCaseInstance = caseEndedEvent.getEntity();
                assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("business key");
                assertThat(eventCaseInstance.getName()).isEqualTo("name");

                assertThat(caseEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseEndedEvent.getExecutionId()).isNull();
                assertThat(caseEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseEndedEvent.getScopeId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getId());
                assertThat(caseEndedEvent.getSubScopeId()).isNull();
                assertThat(caseEndedEvent.getScopeDefinitionId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getCaseDefinitionId());
                events.add((FlowableCaseEndedEvent) flowableEvent);
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .businessKey("business key")
            .name("name")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(1);
        assertPlanItemInstanceState(planItemInstances, "The Task", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "The Task"));

        assertThat(events)
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.CASE_ENDED);

        assertThat(events)
                .extracting(FlowableCaseEndedEvent::getEndingState)
                .containsExactly(FlowableCaseEndedEvent.ENDING_STATE_COMPLETED);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testCaseInstanceEndWithTerminationEvent() {
        List<FlowableCaseEndedEvent> events = new ArrayList<>();
        listener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseEndedEvent caseEndedEvent) {
                CaseInstance eventCaseInstance = caseEndedEvent.getEntity();
                assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("business key");
                assertThat(eventCaseInstance.getName()).isEqualTo("name");

                assertThat(caseEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseEndedEvent.getExecutionId()).isNull();
                assertThat(caseEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseEndedEvent.getScopeId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getId());
                assertThat(caseEndedEvent.getSubScopeId()).isNull();
                assertThat(caseEndedEvent.getScopeDefinitionId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getCaseDefinitionId());
                events.add((FlowableCaseEndedEvent) flowableEvent);
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .businessKey("business key")
            .name("name")
            .start();

        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        assertThat(events)
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.CASE_ENDED);

        assertThat(events)
                .extracting(FlowableCaseEndedEvent::getEndingState)
                .containsExactly(FlowableCaseEndedEvent.ENDING_STATE_TERMINATED);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/event/CaseInstanceEventsTest.testSimpleSubCase.cmmn",
            "org/flowable/cmmn/test/one-human-task-model.cmmn"
    })
    public void testSubCaseEndedEvents() {
        List<FlowableCaseEndedEvent> events = new ArrayList<>();
        listener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseEndedEvent caseEndedEvent) {
                CaseInstance eventCaseInstance = caseEndedEvent.getEntity();
                assertThat(caseEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseEndedEvent.getExecutionId()).isNull();
                assertThat(caseEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseEndedEvent.getScopeId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getId());
                assertThat(caseEndedEvent.getSubScopeId()).isNull();
                assertThat(caseEndedEvent.getScopeDefinitionId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("child key");
                    assertThat(eventCaseInstance.getName()).isNull();
                } else {
                    assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("main key");
                    assertThat(eventCaseInstance.getName()).isEqualTo("name");
                }
                events.add((FlowableCaseEndedEvent) flowableEvent);
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("mainCase")
            .businessKey("main key")
            .name("name")
            .transientVariable("childBusinessKey", "child key")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("Case Task");
        assertThat(planItemInstances).extracting(PlanItemInstance::getState).containsExactly(ACTIVE);
        assertThat(planItemInstances).extracting(PlanItemInstance::getReferenceType).containsExactly(ReferenceTypes.PLAN_ITEM_CHILD_CASE);
        String childCaseId = planItemInstances.get(0).getReferenceId();

        planItemInstances = getPlanItemInstances(childCaseId);
        assertThat(planItemInstances).hasSize(1);
        assertPlanItemInstanceState(planItemInstances, "The Task", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "The Task"));

        assertThat(events)
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.CASE_ENDED, FlowableEngineEventType.CASE_ENDED);

        assertThat(events)
                .extracting(FlowableCaseEndedEvent::getEndingState)
                .containsExactly(FlowableCaseEndedEvent.ENDING_STATE_COMPLETED, FlowableCaseEndedEvent.ENDING_STATE_COMPLETED);
    }


    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/event/CaseInstanceEventsTest.testSimpleSubCase.cmmn",
            "org/flowable/cmmn/test/one-human-task-model.cmmn"
    })
    public void testSubCaseEndedWithManualTriggerEvents() {
        List<FlowableCaseEndedEvent> events = new ArrayList<>();
        listener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseEndedEvent caseEndedEvent) {
                CaseInstance eventCaseInstance = caseEndedEvent.getEntity();
                assertThat(caseEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseEndedEvent.getExecutionId()).isNull();
                assertThat(caseEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseEndedEvent.getScopeId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getId());
                assertThat(caseEndedEvent.getSubScopeId()).isNull();
                assertThat(caseEndedEvent.getScopeDefinitionId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("child key");
                    assertThat(eventCaseInstance.getName()).isNull();
                } else {
                    assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("main key");
                    assertThat(eventCaseInstance.getName()).isEqualTo("name");
                }
                events.add((FlowableCaseEndedEvent) flowableEvent);
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("mainCase")
            .businessKey("main key")
            .name("name")
            .transientVariable("childBusinessKey", "child key")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("Case Task");
        assertThat(planItemInstances).extracting(PlanItemInstance::getState).containsExactly(ACTIVE);
        assertThat(planItemInstances).extracting(PlanItemInstance::getReferenceType).containsExactly(ReferenceTypes.PLAN_ITEM_CHILD_CASE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Case Task"));

        assertThat(events)
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.CASE_ENDED, FlowableEngineEventType.CASE_ENDED);

        assertThat(events)
                .extracting(FlowableCaseEndedEvent::getEndingState)
                .containsExactly(FlowableCaseEndedEvent.ENDING_STATE_TERMINATED, FlowableCaseEndedEvent.ENDING_STATE_COMPLETED);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/event/CaseInstanceEventsTest.testSimpleSubCase.cmmn",
            "org/flowable/cmmn/test/one-human-task-model.cmmn"
    })
    public void testSubCaseEndedWithTerminationEvents() {
        List<FlowableCaseEndedEvent> events = new ArrayList<>();
        listener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseEndedEvent caseEndedEvent) {
                CaseInstance eventCaseInstance = caseEndedEvent.getEntity();
                assertThat(caseEndedEvent.getProcessInstanceId()).isNull();
                assertThat(caseEndedEvent.getExecutionId()).isNull();
                assertThat(caseEndedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseEndedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseEndedEvent.getScopeId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getId());
                assertThat(caseEndedEvent.getSubScopeId()).isNull();
                assertThat(caseEndedEvent.getScopeDefinitionId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("child key");
                    assertThat(eventCaseInstance.getName()).isNull();
                } else {
                    assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("main key");
                    assertThat(eventCaseInstance.getName()).isEqualTo("name");
                }
                events.add((FlowableCaseEndedEvent) flowableEvent);
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("mainCase")
            .businessKey("main key")
            .name("name")
            .transientVariable("childBusinessKey", "child key")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("Case Task");
        assertThat(planItemInstances).extracting(PlanItemInstance::getState).containsExactly(ACTIVE);
        assertThat(planItemInstances).extracting(PlanItemInstance::getReferenceType).containsExactly(ReferenceTypes.PLAN_ITEM_CHILD_CASE);
        String childCaseId = planItemInstances.get(0).getReferenceId();

        cmmnRuntimeService.terminateCaseInstance(childCaseId);

        assertThat(events)
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.CASE_ENDED, FlowableEngineEventType.CASE_ENDED);

        assertThat(events)
                .extracting(FlowableCaseEndedEvent::getEndingState)
                .containsExactly(FlowableCaseEndedEvent.ENDING_STATE_TERMINATED, FlowableCaseEndedEvent.ENDING_STATE_COMPLETED);
    }

    private static class TestEventListener extends AbstractFlowableEventListener {
        private Consumer<FlowableEvent> eventConsumer;

        @Override
        public void onEvent(FlowableEvent event) {
            if (eventConsumer != null) {
                eventConsumer.accept(event);
            }
        }

        @Override
        public boolean isFailOnException() {
            return true;
        }
    }
}
