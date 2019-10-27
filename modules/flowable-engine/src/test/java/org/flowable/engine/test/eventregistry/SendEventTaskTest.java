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
package org.flowable.engine.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTestCase;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.definition.EventPayloadTypes;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class SendEventTaskTest extends FlowableTestCase {

    protected TestOutboundEventChannelAdapter outboundEventChannelAdapter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        outboundEventChannelAdapter = setupTestChannel();

        processEngineConfiguration.getEventRegistry().newEventDefinition()
            .outboundChannelKey("out-channel")
            .key("myEvent")
            .payload("eventProperty", EventPayloadTypes.STRING)
            .register();
        
        processEngineConfiguration.getEventRegistry().newEventDefinition()
            .outboundChannelKey("out-channel")
            .key("anotherEvent")
            .payload("nameProperty", EventPayloadTypes.STRING)
            .payload("numberProperty", EventPayloadTypes.INTEGER)
            .register();
    }

    protected TestOutboundEventChannelAdapter setupTestChannel() {
        TestOutboundEventChannelAdapter outboundEventChannelAdapter = new TestOutboundEventChannelAdapter();

        processEngineConfiguration.getEventRegistry().newOutboundChannelDefinition()
            .key("out-channel")
            .channelAdapter(outboundEventChannelAdapter)
            .jsonSerializer()
            .register();

        return outboundEventChannelAdapter;
    }


    @Override
    protected void tearDown() throws Exception {
        processEngineConfiguration.getEventRegistry().removeChannelDefinition("out-channel");
        processEngineConfiguration.getEventRegistry().removeEventDefinition("myEvent");
        processEngineConfiguration.getEventRegistry().removeEventDefinition("anotherEvent");
    }

    @Test
    @Deployment
    public void testSendEvent() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(0);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get("eventProperty").asText()).isEqualTo("test");
    }
    
    @Test
    @Deployment
    public void testSendEventWithExpressions() throws Exception {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("process")
                        .variable("name", "someName")
                        .variable("accountNumber", 123)
                        .start();
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(0);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(2);
        assertThat(jsonNode.get("nameProperty").asText()).isEqualTo("someName");
        assertThat(jsonNode.get("numberProperty").asText()).isEqualTo("123");
    }

    public static class TestOutboundEventChannelAdapter implements OutboundEventChannelAdapter {

        public List<String> receivedEvents = new ArrayList<>();

        @Override
        public void sendEvent(String rawEvent) {
            receivedEvents.add(rawEvent);
        }

    }
}
