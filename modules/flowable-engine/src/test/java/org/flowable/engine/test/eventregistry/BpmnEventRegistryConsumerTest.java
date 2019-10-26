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
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTestCase;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.definition.EventPayloadTypes;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BpmnEventRegistryConsumerTest extends FlowableTestCase {

    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        inboundEventChannelAdapter = setupTestChannel();

        processEngineConfiguration.getEventRegistry().newEventDefinition()
            .inboundChannelKey("test-channel")
            .key("myEvent")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .correlationParameter("orderId", EventPayloadTypes.STRING)
            .payload("payload1", EventPayloadTypes.STRING)
            .payload("payload2", EventPayloadTypes.INTEGER)
            .register();
    }
    
    protected TestInboundEventChannelAdapter setupTestChannel() {
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
        processEngineConfiguration.getEventRegistry().removeChannelDefinition("test-channel");
        processEngineConfiguration.getEventRegistry().removeEventDefinition("myEvent");

        super.tearDown();
    }

    @Test
    @Deployment
    public void testGenericEventListenerNoCorrelation() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        inboundEventChannelAdapter.triggerTestEvent();
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        
        eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNull();
    }
    
    @Test
    @Deployment
    public void testGenericEventListenerNoCorrelationNoTrigger() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

        taskService.complete(task.getId());
        
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNull();
    }

    @Test
    @Deployment
    public void testGenericEventListenerWithCorrelation() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        ProcessInstance kermitProcessInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
        
        variableMap.clear();
        variableMap.put("customerIdVar", "gonzo");
        ProcessInstance gonzoProcessInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
 
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("task");
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("task");

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("task");

        inboundEventChannelAdapter.triggerTestEvent("fozzie");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("task");

        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
    }

    @Test
    @Deployment
    public void testProcessStartNoCorrelationParameter() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
        assertThat(processDefinition).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery()
            .processDefinitionId(processDefinition.getId())
            .scopeType(ScopeTypes.BPMN)
            .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(0);

        for (int i = 1; i <= 5; i++) {
            inboundEventChannelAdapter.triggerTestEvent();
            assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(i);
        }
    }
    
    @Test
    @Deployment
    public void testProcessStartSimpleCorrelationParameter() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
        assertThat(processDefinition).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery()
            .processDefinitionId(processDefinition.getId())
            .scopeType(ScopeTypes.BPMN)
            .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(0);
        
        inboundEventChannelAdapter.triggerTestEvent("anotherCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(0);

        for (int i = 1; i <= 5; i++) {
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(i);
        }
    }
    
    @Test
    @Deployment
    public void testProcessStartWithPayload() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
        assertThat(processDefinition).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery()
            .processDefinitionId(processDefinition.getId())
            .scopeType(ScopeTypes.BPMN)
            .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(0);
        
        inboundEventChannelAdapter.triggerTestEvent("payloadStartCustomer");
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").singleResult();
        assertThat(runtimeService.getVariables(processInstance.getId()))
            .containsOnly(
                entry("customerIdVar", "payloadStartCustomer"),
                entry("payload1", "Hello World")
            );
    }

    private static class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

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

        public void triggerTestEvent() {
            triggerTestEvent(null);
        }

        public void triggerTestEvent(String customerId) {
            triggerTestEvent(customerId, null);
        }

        public void triggerOrderTestEvent(String orderId) {
            triggerTestEvent(null, orderId);
        }

        public void triggerTestEvent(String customerId, String orderId) {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "myEvent");
            if (customerId != null) {
                json.put("customerId", customerId);
            }

            if (orderId != null) {
                json.put("orderId", orderId);
            }
            json.put("payload1", "Hello World");
            json.put("payload2", new Random().nextInt());
            try {
                eventRegistry.eventReceived(channelKey, objectMapper.writeValueAsString(json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
