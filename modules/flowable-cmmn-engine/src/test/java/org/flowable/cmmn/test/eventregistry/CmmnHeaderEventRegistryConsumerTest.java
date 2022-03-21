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
package org.flowable.cmmn.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.impl.FlowableEventInfoImpl;
import org.flowable.eventregistry.impl.payload.JsonFieldToMapPayloadExtractor;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CmmnHeaderEventRegistryConsumerTest extends FlowableEventRegistryCmmnTestCase {

    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @Before
    public void registerEventDefinition() {
        inboundEventChannelAdapter = setupTestChannel();

        getEventRepositoryService().createEventModelBuilder()
                .key("myEvent")
                .resourceName("myEvent.event")
                .correlationParameter("customerId", EventPayloadTypes.STRING)
                .correlationParameter("orderId", EventPayloadTypes.STRING)
                .header("headerProperty1", EventPayloadTypes.STRING)
                .header("headerProperty2", EventPayloadTypes.INTEGER)
                .payload("payload1", EventPayloadTypes.STRING)
                .payload("payload2", EventPayloadTypes.INTEGER)
                .deploy();
    }

    protected TestInboundEventChannelAdapter setupTestChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();
        Map<Object, Object> beans = getEventRegistryEngineConfiguration().getExpressionManager().getBeans();
        beans.put("inboundEventChannelAdapter", inboundEventChannelAdapter);
        beans.put("testDeserializer", new TestDeserializer());
        beans.put("testInboundEventKeyDetector", new TestInboundEventKeyDetector());
        beans.put("testInboundEventPayloadExtractor", new TestInboundEventPayloadExtractor());

        getEventRepositoryService().createInboundChannelModelBuilder()
                .key("test-channel")
                .resourceName("test.channel")
                .channelAdapter("${inboundEventChannelAdapter}")
                .delegateExpressionDeserializer("${testDeserializer}")
                .delegateExpressionKeyDetector("${testInboundEventKeyDetector}")
                .payloadExtractor("${testInboundEventPayloadExtractor}")
                .deploy();

        return inboundEventChannelAdapter;
    }

    @After
    public void unregisterEventDefinition() {
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseStartWithHeaders() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("testCaseStartEventWithPayload").singleResult();
        assertThat(caseDefinition).isNotNull();

        inboundEventChannelAdapter.triggerTestEventWithHeaders("payloadStartCustomer", "testHeader", 1234);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(1);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("customerIdVar", "payloadStartCustomer"),
                        entry("anotherVarName", "Hello World"),
                        entry("myHeaderValue1", "testHeader"),
                        entry("myHeaderValue2", 1234)
                );
    }

    private static class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

        public InboundChannelModel inboundChannelModel;
        public EventRegistry eventRegistry;
        protected ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
            this.inboundChannelModel = inboundChannelModel;
        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
            this.eventRegistry = eventRegistry;
        }

        public void triggerTestEventWithHeaders(String customerId, String headerValue1, Integer headerValue2) {
            ObjectNode eventNode = createTestEventNode(customerId, null);
            ObjectNode headersNode = eventNode.putObject("headers");
            headersNode.put("headerProperty1", headerValue1);
            headersNode.put("headerProperty2", headerValue2);
            try {
                eventRegistry.eventReceived(inboundChannelModel, objectMapper.writeValueAsString(eventNode));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        protected ObjectNode createTestEventNode(String customerId, String orderId) {
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
            
            return json;
        }

    }

    protected class TestDeserializer implements InboundEventDeserializer<JsonNode> {

        @Override
        public FlowableEventInfo<JsonNode> deserialize(Object rawEvent) {
            try {
                Map<String, Object> headerMap = new HashMap<>();
                JsonNode eventNode = getEventRegistryEngineConfiguration().getObjectMapper().readTree(rawEvent.toString());
                JsonNode headersNode = eventNode.get("headers");
                if (headersNode != null) {
                    Iterator<String> itHeader = headersNode.fieldNames();
                    while (itHeader.hasNext()) {
                        String headerKey = itHeader.next();
                        JsonNode headerValue = headersNode.get(headerKey);
                        if (NumberUtils.isCreatable(headerValue.asText())) {
                            headerMap.put(headerKey, headersNode.get(headerKey).asInt());
                        } else {
                            headerMap.put(headerKey, headersNode.get(headerKey).asText());
                        }
                    }
                }
                
                return new FlowableEventInfoImpl<>(headerMap, eventNode);
                
            } catch (Exception e) {
                throw new FlowableException("Error reading event json", e);
            }
        }
    }
    
    protected class TestInboundEventKeyDetector implements InboundEventKeyDetector<JsonNode> {
        
        @Override
        public String detectEventDefinitionKey(FlowableEventInfo<JsonNode> event) {
            return event.getPayload().get("type").asText();
        }
    }
    
    protected class TestInboundEventPayloadExtractor extends JsonFieldToMapPayloadExtractor {

    }
}
