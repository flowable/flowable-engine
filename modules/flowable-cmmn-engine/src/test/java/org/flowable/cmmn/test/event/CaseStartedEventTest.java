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

import org.flowable.cmmn.api.event.FlowableCaseStartedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
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
 * @author Filip Hrisafov
 */
public class CaseStartedEventTest extends FlowableCmmnTestCase {

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
    public void testCaseInstanceEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        listener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseStartedEvent caseStartedEvent) {
                CaseInstance eventCaseInstance = caseStartedEvent.getEntity();
                assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("business key");
                assertThat(eventCaseInstance.getName()).isEqualTo("name");

                assertThat(caseStartedEvent.getProcessInstanceId()).isNull();
                assertThat(caseStartedEvent.getExecutionId()).isNull();
                assertThat(caseStartedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseStartedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseStartedEvent.getScopeId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getId());
                assertThat(caseStartedEvent.getSubScopeId()).isNull();
                assertThat(caseStartedEvent.getScopeDefinitionId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getCaseDefinitionId());
                events.add(flowableEvent);
            }
        };

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("business key")
                .name("name")
                .start();

        assertThat(events)
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.CASE_STARTED);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/event/CaseInstanceEventsTest.testSimpleSubCase.cmmn",
            "org/flowable/cmmn/test/one-human-task-model.cmmn"
    })
    public void testSubCaseStartedEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        listener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseStartedEvent caseStartedEvent) {
                CaseInstance eventCaseInstance = caseStartedEvent.getEntity();
                assertThat(caseStartedEvent.getProcessInstanceId()).isNull();
                assertThat(caseStartedEvent.getExecutionId()).isNull();
                assertThat(caseStartedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseStartedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseStartedEvent.getScopeId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getId());
                assertThat(caseStartedEvent.getSubScopeId()).isNull();
                assertThat(caseStartedEvent.getScopeDefinitionId())
                        .isNotNull()
                        .isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("main key");
                    assertThat(eventCaseInstance.getName()).isEqualTo("name");

                } else {
                    assertThat(eventCaseInstance.getBusinessKey()).isEqualTo("child key");
                    assertThat(eventCaseInstance.getName()).isNull();
                }
                events.add(flowableEvent);
            }
        };

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("mainCase")
                .businessKey("main key")
                .name("name")
                .transientVariable("childBusinessKey", "child key")
                .start();

        assertThat(events)
                .extracting(FlowableEvent::getType)
                .containsExactly(
                        FlowableEngineEventType.CASE_STARTED,
                        FlowableEngineEventType.CASE_STARTED
                );
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
