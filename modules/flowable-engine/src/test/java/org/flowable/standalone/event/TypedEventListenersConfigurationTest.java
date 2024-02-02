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
package org.flowable.standalone.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.impl.FlowableProcessEventImpl;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.api.event.TestFlowableEventListener;
import org.junit.jupiter.api.Test;

/**
 * Test to verify event-listeners, which are configured in the cfg.xml, are notified.
 *
 * @author Frederik Heremans
 */
public class TypedEventListenersConfigurationTest extends ResourceFlowableTestCase {

    public TypedEventListenersConfigurationTest() {
        super("org/flowable/standalone/event/flowable-typed-eventlistener.cfg.xml");
    }

    @Test
    public void testEventListenerConfiguration() {
        // Fetch the listener to check received events
        TestFlowableEventListener listener = (TestFlowableEventListener) processEngineConfiguration.getBeans().get("eventListener");
        assertThat(listener).isNotNull();

        // Clear any events received (eg. engine initialisation)
        listener.clearEventsReceived();

        // Dispatch a custom event
        FlowableEvent event = new FlowableProcessEventImpl(FlowableEngineEventType.CUSTOM);
        processEngineConfiguration.getEventDispatcher().dispatchEvent(event, processEngineConfiguration.getEngineCfgKey());

        assertThat(listener.getEventsReceived()).hasSize(1);
        assertThat(listener.getEventsReceived().get(0)).isEqualTo(event);
        listener.clearEventsReceived();

        // Dispatch another event the listener is registered for
        event = new FlowableProcessEventImpl(FlowableEngineEventType.ENTITY_DELETED);
        processEngineConfiguration.getEventDispatcher().dispatchEvent(event, processEngineConfiguration.getEngineCfgKey());
        event = new FlowableProcessEventImpl(FlowableEngineEventType.ENTITY_UPDATED);
        processEngineConfiguration.getEventDispatcher().dispatchEvent(event, processEngineConfiguration.getEngineCfgKey());

        assertThat(listener.getEventsReceived())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ENTITY_DELETED, FlowableEngineEventType.ENTITY_UPDATED);
        listener.clearEventsReceived();

        // Dispatch an event that is NOT part of the types configured
        event = new FlowableProcessEventImpl(FlowableEngineEventType.ENTITY_CREATED);
        processEngineConfiguration.getEventDispatcher().dispatchEvent(event, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.getEventsReceived()).isEmpty();
    }
}
