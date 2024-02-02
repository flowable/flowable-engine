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
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.test.api.event.TestFlowableEventListener;
import org.junit.jupiter.api.Test;

/**
 * Test to verify event-listeners, which are configured in the cfg.xml, are notified.
 *
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class EngineEventsTest {

    @Test
    public void testEngineEventsTest() {
        ProcessEngineConfiguration processEngineConfiguration = StandaloneProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource("org/flowable/standalone/event/flowable-eventlistener.cfg.xml");
        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

        // Fetch the listener to check received events
        TestFlowableEventListener listener = (TestFlowableEventListener) processEngineConfiguration.getBeans().get("eventListener");
        assertThat(listener).isNotNull();

        // Check create-event
        assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.ENGINE_CREATED);
        listener.clearEventsReceived();

        // Check close-event
        processEngine.close();
        assertThat(listener.getEventsReceived().get(listener.getEventsReceived().size() - 1).getType()).isEqualTo(FlowableEngineEventType.ENGINE_CLOSED);
    }

}