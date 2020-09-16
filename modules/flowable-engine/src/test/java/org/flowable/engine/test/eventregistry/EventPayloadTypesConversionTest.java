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

import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class EventPayloadTypesConversionTest extends FlowableEventRegistryBpmnTestCase {

    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;
    protected TestOutboundEventChannelAdapter outboundEventChannelAdapter;

    protected TestXmlInboundEventChannelAdapter inboundXmlEventChannelAdapter;
    protected TestOutboundEventChannelAdapter outboundXmlEventChannelAdapter;

    @BeforeEach
    public void setUp() throws Exception {
        inboundEventChannelAdapter = setupTestInboundChannel();
        outboundEventChannelAdapter = setupTestOutboundChannel();

        inboundXmlEventChannelAdapter = setupTestXmlInboundChannel();
        outboundXmlEventChannelAdapter = setupTestXmlOutboundChannel();

        getEventRepositoryService().createEventModelBuilder()
            .key("myEvent")
            .resourceName("myEvent.event")
            .payload("stringPayload", EventPayloadTypes.STRING)
            .payload("integerPayload", EventPayloadTypes.INTEGER)
            .payload("doublePayload", EventPayloadTypes.DOUBLE)
            .payload("longPayload", EventPayloadTypes.LONG)
            .payload("booleanPayload", EventPayloadTypes.BOOLEAN)
            .payload("jsonPayload", EventPayloadTypes.JSON)
            .deploy();
    }

    protected TestInboundEventChannelAdapter setupTestInboundChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
            .put("inboundEventChannelAdapter", inboundEventChannelAdapter);

        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("test-channel")
            .resourceName("testChannel.channel")
            .channelAdapter("${inboundEventChannelAdapter}")
            .jsonDeserializer()
            .fixedEventKey("myEvent")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        return inboundEventChannelAdapter;
    }

    protected TestXmlInboundEventChannelAdapter setupTestXmlInboundChannel() {
        TestXmlInboundEventChannelAdapter inboundEventChannelAdapter = new TestXmlInboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
            .put("xmlInboundEventChannelAdapter", inboundEventChannelAdapter);

        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("test-xml-channel")
            .resourceName("testChannel.channel")
            .channelAdapter("${xmlInboundEventChannelAdapter}")
            .xmlDeserializer()
            .fixedEventKey("myEvent")
            .xmlElementsMapDirectlyToPayload()
            .deploy();

        return inboundEventChannelAdapter;
    }

    protected TestOutboundEventChannelAdapter setupTestOutboundChannel() {
        TestOutboundEventChannelAdapter outboundEventChannelAdapter = new TestOutboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans().put("outboundEventChannelAdapter", outboundEventChannelAdapter);
        getEventRepositoryService().createOutboundChannelModelBuilder()
            .key("out-channel")
            .resourceName("testOut.channel")
            .channelAdapter("${outboundEventChannelAdapter}")
            .jsonSerializer()
            .deploy();

        return outboundEventChannelAdapter;
    }

    protected TestOutboundEventChannelAdapter setupTestXmlOutboundChannel() {
        TestOutboundEventChannelAdapter outboundEventChannelAdapter = new TestOutboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans().put("xmlOutboundEventChannelAdapter", outboundEventChannelAdapter);
        getEventRepositoryService().createOutboundChannelModelBuilder()
            .key("out-xml-channel")
            .resourceName("testOut.channel")
            .channelAdapter("${xmlOutboundEventChannelAdapter}")
            .xmlSerializer()
            .deploy();

        return outboundEventChannelAdapter;
    }

    @AfterEach
    public void tearDown() throws Exception {
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    @Deployment
    public void testPayloadTypesConversionMatchingTypes() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").start();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

        inboundEventChannelAdapter.testTriggerEvent();
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");

        assertThat(runtimeService.getVariable(processInstance.getId(), "stringPayload")).isEqualTo("Hello world");
        assertThat(runtimeService.getVariable(processInstance.getId(), "integerPayload")).isEqualTo(123);
        assertThat(runtimeService.getVariable(processInstance.getId(), "doublePayload")).isEqualTo(99.99);
        assertThat(runtimeService.getVariable(processInstance.getId(), "longPayload")).isEqualTo(123456789L);
        assertThat(runtimeService.getVariable(processInstance.getId(), "booleanPayload")).isEqualTo(true);
        assertThatJson(runtimeService.getVariable(processInstance.getId(), "jsonPayload")).isEqualTo("{ hello: 'world' }");
    }

    @Test
    @Deployment
    public void testPayloadTypesConversionMatchingTypesXmlChannel() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").start();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

        inboundXmlEventChannelAdapter.triggerTestEvent();
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");

        assertThat(runtimeService.getVariable(processInstance.getId(), "stringPayload")).isEqualTo("Hello World from xml");
        assertThat(runtimeService.getVariable(processInstance.getId(), "integerPayload")).isEqualTo(1);
        assertThat(runtimeService.getVariable(processInstance.getId(), "doublePayload")).isEqualTo(2.22);
        assertThat(runtimeService.getVariable(processInstance.getId(), "longPayload")).isEqualTo(33333333L);
        assertThat(runtimeService.getVariable(processInstance.getId(), "booleanPayload")).isEqualTo(false);
    }

    @Test
    @Deployment
    public void testOutgoingPayloadConversion() throws Exception {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("process")
            .variable("stringPayload", "Hello world")
            .variable("integerPayload", 123)
            .variable("doublePayload", 99.99)
            .variable("longPayload", 123456789L)
            .variable("booleanPayload", true)
            .variable("jsonPayload", processEngineConfiguration.getObjectMapper().createObjectNode().put("hello", "world"))
            .start();

        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(6);
        assertThatJson(jsonNode).isEqualTo(
            "{"
           + "  stringPayload: 'Hello world',"
           + "  integerPayload: 123,"
           + "  doublePayload: 99.99,"
           + "  longPayload: 123456789,"
           + "  booleanPayload: true,"
           + "  jsonPayload: { hello : 'world' }"
           + "}"
        );
    }

    @Test
    @Deployment
    public void testOutgoingPayloadConversionAllStrings() throws Exception {

        // Values are set as strings in the process bpmn xml

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("process")
            .start();

        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(6);
        assertThatJson(jsonNode).isEqualTo(
            "{"
                + "  stringPayload: 'hardcoded',"
                + "  integerPayload: 987,"
                + "  doublePayload: 12.34,"
                + "  longPayload: 123456789,"
                + "  booleanPayload: false,"
                + "  jsonPayload: { test : 123 }"
                + "}"
        );

    }

    @Test
    @Deployment
    public void testOutgoingPayloadConversionXml() throws Exception {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("process")
            .variable("stringPayload", "Hello world with xml channel")
            .variable("integerPayload", 123)
            .variable("doublePayload", 99.99)
            .variable("longPayload", 123456789L)
            .variable("booleanPayload", true)
            .start();

        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertThat(outboundXmlEventChannelAdapter.receivedEvents).isEmpty();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(outboundXmlEventChannelAdapter.receivedEvents).hasSize(1);

        String event = outboundXmlEventChannelAdapter.receivedEvents.get(0);
        assertThat(event).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><myEvent><stringPayload>Hello world with xml channel</stringPayload><integerPayload>123</integerPayload><doublePayload>99.99</doublePayload><longPayload>123456789</longPayload><booleanPayload>true</booleanPayload></myEvent>");
    }

    @Test
    @Deployment
    public void testOutgoingPayloadConversionXmlAllStrings() throws Exception {

        // Values are set as strings in the process bpmn xml

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("process")
            .start();

        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertThat(outboundXmlEventChannelAdapter.receivedEvents).isEmpty();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(outboundXmlEventChannelAdapter.receivedEvents).hasSize(1);

        String event = outboundXmlEventChannelAdapter.receivedEvents.get(0);
        assertThat(event).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><myEvent><stringPayload>hardcoded</stringPayload><integerPayload>987</integerPayload><doublePayload>12.34</doublePayload><longPayload>123456789</longPayload><booleanPayload>false</booleanPayload></myEvent>");

    }

    private static class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

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

        public void testTriggerEvent() {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();

            json.put("stringPayload", "Hello world");
            json.put("integerPayload", 123);
            json.put("doublePayload", 99.99);
            json.put("longPayload", 123456789L);
            json.put("booleanPayload", true);
            json.set("jsonPayload", objectMapper.createObjectNode().put("hello", "world"));

            try {
                eventRegistry.eventReceived(inboundChannelModel, objectMapper.writeValueAsString(json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static class TestXmlInboundEventChannelAdapter implements InboundEventChannelAdapter {

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

        public void triggerTestEvent() {
            String event =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<data>" +
            "  <stringPayload>Hello World from xml</stringPayload>" +
            "  <integerPayload>1</integerPayload>" +
            "  <doublePayload>2.22</doublePayload>" +
            "  <longPayload>33333333</longPayload>" +
            "  <booleanPayload>false</booleanPayload>" +
            "</data>";

            eventRegistry.eventReceived(inboundChannelModel, event);
        }

    }

    public static class TestOutboundEventChannelAdapter implements OutboundEventChannelAdapter<String> {

        public List<String> receivedEvents = new ArrayList<>();

        @Override
        public void sendEvent(String rawEvent) {
            receivedEvents.add(rawEvent);
        }
    }

}
