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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTestCase;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class EventRegistryEventSubprocessTest extends FlowableTestCase {
    
    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        inboundEventChannelAdapter = setupTestChannel();

        getEventRepositoryService().createEventModelBuilder()
            .inboundChannelKey("test-channel")
            .key("myEvent")
            .resourceName("myEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .correlationParameter("orderId", EventPayloadTypes.STRING)
            .payload("payload1", EventPayloadTypes.STRING)
            .payload("payload2", EventPayloadTypes.INTEGER)
            .deploy();
    }
    
    protected TestInboundEventChannelAdapter setupTestChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();

        getEventRegistry().newInboundChannelModel()
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
        getEventRegistry().removeChannelModel("test-channel");
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }

        super.tearDown();
    }

    @Test
    @Deployment
    public void testNonInterruptingSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
        
        inboundEventChannelAdapter.triggerTestEvent("notexisting");
        
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        
        assertEquals(6, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(2, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertEquals(9, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(3, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        assertEquals(0, createEventSubscriptionQuery().count());

        // we still have 7 executions:
        assertEquals(7, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // now let's complete the first task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        assertEquals(4, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // complete the second task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testInterruptingSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "gonzo");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        inboundEventChannelAdapter.triggerTestEvent("notexisting");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
        
        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertEquals(5, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(0, createEventSubscriptionQuery().count());

        // now let's complete the task in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor());
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

        public void triggerTestEvent(String customerId) {
            triggerTestEvent(customerId, null);
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
