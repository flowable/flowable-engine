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
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.definition.EventPayloadTypes;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SendEventTaskTest extends FlowableTestCase {

    protected TestOutboundEventChannelAdapter outboundEventChannelAdapter;
    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        outboundEventChannelAdapter = setupTestChannel();
        inboundEventChannelAdapter = setupTestInboundChannel();

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
        
        processEngineConfiguration.getEventRegistry().newEventDefinition()
            .inboundChannelKey("test-channel")
            .key("myTriggerEvent")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .payload("customerId", EventPayloadTypes.STRING)
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

    protected TestInboundEventChannelAdapter setupTestInboundChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();

        processEngineConfiguration.getEventRegistry().newInboundChannelDefinition()
            .key("test-channel")
            .channelAdapter(inboundEventChannelAdapter)
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("type")
            .jsonFieldsMapDirectlyToPayload()
            .register();

        return inboundEventChannelAdapter;
    }

    @Override
    protected void tearDown() throws Exception {
        processEngineConfiguration.getEventRegistry().removeChannelDefinition("out-channel");
        processEngineConfiguration.getEventRegistry().removeChannelDefinition("test-channel");
        processEngineConfiguration.getEventRegistry().removeEventDefinition("myEvent");
        processEngineConfiguration.getEventRegistry().removeEventDefinition("anotherEvent");
        processEngineConfiguration.getEventRegistry().removeEventDefinition("myTriggerEvent");
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
    
    @Test
    @Deployment
    public void testTriggerableSendEvent() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(0);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get("eventProperty").asText()).isEqualTo("test");
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myTriggerEvent");
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "testId");
        processEngineConfiguration.getEventRegistry().eventReceived("test-channel", objectMapper.writeValueAsString(json));
        
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();
        
        assertThat(runtimeService.getVariable(processInstance.getId(), "anotherVariable")).isEqualTo("testId");
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfter");
    }
    
    @Test
    @Deployment
    public void testTriggerableSendEventWithCorrelation() throws Exception {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .variable("customerIdVar", "someId")
                .start();
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(0);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get("eventProperty").asText()).isEqualTo("test");
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myTriggerEvent");
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "testId");
        processEngineConfiguration.getEventRegistry().eventReceived("test-channel", objectMapper.writeValueAsString(json));
        
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNull();
        
        json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "someId");
        processEngineConfiguration.getEventRegistry().eventReceived("test-channel", objectMapper.writeValueAsString(json));
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();
        
        assertThat(runtimeService.getVariable(processInstance.getId(), "anotherVariable")).isEqualTo("someId");
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfter");
    }

    public static class TestOutboundEventChannelAdapter implements OutboundEventChannelAdapter {

        public List<String> receivedEvents = new ArrayList<>();

        @Override
        public void sendEvent(String rawEvent) {
            receivedEvents.add(rawEvent);
        }
    }
    
    public static class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

        public String channelKey;
        public EventRegistry eventRegistry;

        @Override
        public void setChannelKey(String channelKey) {
            this.channelKey = channelKey;
        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
            this.eventRegistry = eventRegistry;
        }
        
    }
}
