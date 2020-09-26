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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.jobexecutor.AsyncSendEventJobHandler;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SendEventTaskTest extends FlowableEventRegistryBpmnTestCase {

    protected TestOutboundEventChannelAdapter outboundEventChannelAdapter;
    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @BeforeEach
    protected void setUp() throws Exception {
        outboundEventChannelAdapter = setupTestChannel();
        inboundEventChannelAdapter = setupTestInboundChannel();

        getEventRepositoryService().createEventModelBuilder()
            .key("myEvent")
            .resourceName("myEvent.event")
            .payload("eventProperty", EventPayloadTypes.STRING)
            .deploy();
        
        getEventRepositoryService().createEventModelBuilder()
            .key("anotherEvent")
            .resourceName("anotherEvent.event")
            .payload("nameProperty", EventPayloadTypes.STRING)
            .payload("numberProperty", EventPayloadTypes.INTEGER)
            .deploy();
        
        getEventRepositoryService().createEventModelBuilder()
            .key("myTriggerEvent")
            .resourceName("myTriggerEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .deploy();
    }

    protected TestOutboundEventChannelAdapter setupTestChannel() {
        TestOutboundEventChannelAdapter outboundEventChannelAdapter = new TestOutboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
            .put("outboundEventChannelAdapter", outboundEventChannelAdapter);
        getEventRepositoryService().createOutboundChannelModelBuilder()
            .key("out-channel")
            .resourceName("testOut.channel")
            .channelAdapter("${outboundEventChannelAdapter}")
            .jsonSerializer()
            .deploy();

        return outboundEventChannelAdapter;
    }

    protected TestInboundEventChannelAdapter setupTestInboundChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
            .put("inboundEventChannelAdapter", inboundEventChannelAdapter);

        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("test-channel")
            .resourceName("testIn.channel")
            .channelAdapter("${inboundEventChannelAdapter}")
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("type")
            .detectEventTenantUsingJsonPointerExpression("/tenantId")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        return inboundEventChannelAdapter;
    }

    @AfterEach
    protected void tearDown() throws Exception {
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    @Deployment
    public void testSendEvent() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncSendEventJobHandler.TYPE);
        assertThat(job.getElementId()).isEqualTo("sendEventTask");

        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "   eventProperty: 'test'"
                        + " }");
    }
    
    @Test
    @Deployment
    public void testSendEventSynchronously() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "   eventProperty: 'test'"
                        + " }");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfter");
    }

    @Test
    @Deployment
    public void testSendEventWithExpressions() throws Exception {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("process")
                        .variable("name", "someName")
                        .variable("accountNumber", 123)
                        .start();
        
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncSendEventJobHandler.TYPE);
        assertThat(job.getElementId()).isEqualTo("sendEventTask");
        
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "   nameProperty: 'someName',"
                        + "   numberProperty: 123"
                        + " }");
    }
    
    @Test
    @Deployment
    public void testTriggerableSendEvent() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myTriggerEvent");
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncSendEventJobHandler.TYPE);
        assertThat(job.getElementId()).isEqualTo("sendEventTask");
        
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "   eventProperty: 'test'"
                        + " }");

        ObjectMapper objectMapper = new ObjectMapper();

        InboundChannelModel inboundChannel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("test-channel");
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "testId");
        getEventRegistry().eventReceived(inboundChannel, objectMapper.writeValueAsString(json));
        
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();
        
        assertThat(runtimeService.getVariable(processInstance.getId(), "anotherVariable")).isEqualTo("testId");
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfter");
    }

    @Test
    @Deployment
    public void testTriggerableSendEventTransientVariable() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        ObjectMapper objectMapper = new ObjectMapper();

        InboundChannelModel inboundChannel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("test-channel");
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "testId");
        getEventRegistry().eventReceived(inboundChannel, objectMapper.writeValueAsString(json));

        assertThat(runtimeService.getVariable(processInstance.getId(), "anotherVariable")).isNull(); // should not have been stored, as it's transient

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("testId");
    }
    
    @Test
    @Deployment
    public void testTriggerableSendEventSynchronously() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myTriggerEvent");
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNull();

        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get("eventProperty").asText()).isEqualTo("test");

        ObjectMapper objectMapper = new ObjectMapper();

        InboundChannelModel inboundChannel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("test-channel");
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "testId");
        getEventRegistry().eventReceived(inboundChannel, objectMapper.writeValueAsString(json));

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
        
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myTriggerEvent");
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncSendEventJobHandler.TYPE);
        assertThat(job.getElementId()).isEqualTo("sendEventTask");
        
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "   eventProperty: 'test'"
                        + " }");

        ObjectMapper objectMapper = new ObjectMapper();

        InboundChannelModel inboundChannel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("test-channel");
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "testId");
        getEventRegistry().eventReceived(inboundChannel, objectMapper.writeValueAsString(json));
        
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNull();
        
        json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "someId");
        getEventRegistry().eventReceived(inboundChannel, objectMapper.writeValueAsString(json));
        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNull();
        
        assertThat(runtimeService.getVariable(processInstance.getId(), "anotherVariable")).isEqualTo("someId");
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfter");
    }

    public static class TestOutboundEventChannelAdapter implements OutboundEventChannelAdapter<String> {

        public List<String> receivedEvents = new ArrayList<>();

        @Override
        public void sendEvent(String rawEvent) {
            receivedEvents.add(rawEvent);
        }
    }
    
    public static class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

        public InboundChannelModel inboundChannelModel;
        public EventRegistry eventRegistry;

        @Override
        public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
            this.inboundChannelModel = inboundChannelModel;
        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
            this.eventRegistry = eventRegistry;
        }
        
    }

    @Override
    protected EventRepositoryService getEventRepositoryService() {
        return getEventRegistryEngineConfiguration().getEventRepositoryService();
    }

    @Override
    protected EventRegistry getEventRegistry() {
        return getEventRegistryEngineConfiguration().getEventRegistry();
    }

    @Override
    protected EventRegistryEngineConfiguration getEventRegistryEngineConfiguration() {
        return (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
            .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
    }
}
