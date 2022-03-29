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
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This test is using the event support on the CMMN runtime service level.
 *
 * @author Micha Kiener
 */
public class CmmnRuntimeEventListenerSupportTest extends FlowableCmmnTestCase {

    protected TestEventListener allEventListener;
    protected TestEventListener caseStartedEventListener;

    @Before
    public void setUp() {
        allEventListener = new TestEventListener();
        caseStartedEventListener = new TestEventListener();
        cmmnRuntimeService.addEventListener(allEventListener);
        cmmnRuntimeService.addEventListener(caseStartedEventListener, FlowableEngineEventType.CASE_STARTED);
    }

    @After
    public void tearDown() {
        if (allEventListener != null) {
            cmmnRuntimeService.removeEventListener(allEventListener);
            cmmnRuntimeService.removeEventListener(caseStartedEventListener);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testCaseInstanceStartEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        allEventListener.eventConsumer = (flowableEvent) -> {
            // we need to check for a case started event, as this listener will receive all events
            if (flowableEvent instanceof FlowableCaseStartedEvent) {
                events.add(flowableEvent);
            }
        };
        caseStartedEventListener.eventConsumer = (flowableEvent) -> {
            // this listener will only receive case started events as it was registered using the event type
            events.add(flowableEvent);
        };

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("business key")
                .name("name")
                .start();

        assertThat(events)
            .extracting(FlowableEvent::getType)
            .containsExactly(FlowableEngineEventType.CASE_STARTED, FlowableEngineEventType.CASE_STARTED);
    }

    @Test
    public void testDispatchingEvent() {
        List<FlowableEvent> events = new ArrayList<>();
        allEventListener.eventConsumer = (flowableEvent) -> {
            events.add(flowableEvent);
        };
        caseStartedEventListener.eventConsumer = (flowableEvent) -> {
            // this listener must not catch the event, as it was registered using the case started event type
            events.add(flowableEvent);
        };

        cmmnRuntimeService.dispatchEvent(new TestEvent());

        assertThat(events).singleElement().isInstanceOf(TestEvent.class);
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

    private static class TestEvent implements FlowableEngineEvent {

        @Override
        public String getExecutionId() {
            return null;
        }
        @Override
        public String getProcessInstanceId() {
            return null;
        }
        @Override
        public String getProcessDefinitionId() {
            return null;
        }
        @Override
        public String getScopeType() {
            return null;
        }
        @Override
        public String getScopeId() {
            return null;
        }
        @Override
        public String getSubScopeId() {
            return null;
        }
        @Override
        public String getScopeDefinitionId() {
            return null;
        }
        @Override
        public FlowableEventType getType() {
            return () -> "TestEvent";
        }
    }

}
