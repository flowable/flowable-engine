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
package org.flowable.eventregistry.test;

import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.DefaultInboundEventProcessor;
import org.flowable.eventregistry.impl.event.FlowableEventRegistryEvent;
import org.flowable.eventregistry.impl.runtime.EventCorrelationParameterInstanceImpl;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventCorrelationParameter;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class DefaultEventRegistryTest extends AbstractFlowableEventTest {

    protected TestEventConsumer testEventConsumer;

    @BeforeEach
    public void setup() {
        testEventConsumer = new TestEventConsumer();
        EventRegistry eventRegistry = eventEngineConfiguration.getEventRegistry();
        eventRegistry.registerEventRegistryEventConsumer(this.testEventConsumer);
        eventRegistry.setInboundEventProcessor(new DefaultInboundEventProcessor(eventRegistry));
    }
    
    @AfterEach
    public void tearDown() {
        Map<String, InboundChannelModel> inboundChannelDefinitionMap = eventEngineConfiguration.getEventRegistry().getInboundChannelModels();
        for (String key : inboundChannelDefinitionMap.keySet()) {
            eventEngineConfiguration.getEventRegistry().removeChannelModel(key);
        }
        
        List<EventDeployment> eventDeployments = repositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : eventDeployments) {
            repositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    public void testPropertiesPassedToChannelAdapter() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannel();
        assertThat(inboundEventChannelAdapter.channelKey).isEqualTo("test-channel");
        assertThat(inboundEventChannelAdapter.eventRegistry).isEqualTo(eventEngineConfiguration.getEventRegistry());
    }

    @Test
    public void testDefaultInboundEventPipeline() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannel();

        repositoryService.createEventModelBuilder()
            .inboundChannelKey("test-channel")
            .key("myEvent")
            .resourceName("myEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .payload("payload1", EventPayloadTypes.STRING)
            .payload("payload2", EventPayloadTypes.INTEGER)
            .deploy();

        inboundEventChannelAdapter.triggerTestEvent();

        assertThat(testEventConsumer.eventsReceived).hasSize(1);
        FlowableEventRegistryEvent eventRegistryEvent = (FlowableEventRegistryEvent) testEventConsumer.eventsReceived.get(0);

        EventInstance eventInstance = eventRegistryEvent.getEventInstance();
        assertThat(eventInstance.getEventModel().getKey()).isEqualTo("myEvent");

        assertThat(eventInstance.getCorrelationParameterInstances())
            .extracting(EventCorrelationParameterInstance::getValue).containsOnly("test");
        assertThat(eventInstance.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getDefinitionType, EventPayloadInstance::getValue)
            .containsOnly(
                tuple("customerId", EventPayloadTypes.STRING, "test"),
                tuple("payload1", EventPayloadTypes.STRING, "Hello World"),
                tuple("payload2", EventPayloadTypes.INTEGER, 123)
            );
    }

    @Test
    public void testDefaultInboundEventPipelineWithCustomDeserializerAndExtractor() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannelWithCustomDeserializer();

        repositoryService.createEventModelBuilder()
            .inboundChannelKey("test-channel")
            .key("myEvent")
            .resourceName("myEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .payload("payload1", EventPayloadTypes.STRING)
            .payload("payload2", EventPayloadTypes.INTEGER)
            .deploy();

        inboundEventChannelAdapter.triggerTestEvent();

        assertThat(testEventConsumer.eventsReceived).hasSize(1);
        FlowableEventRegistryEvent eventRegistryEvent = (FlowableEventRegistryEvent) testEventConsumer.eventsReceived.get(0);

        EventInstance eventInstance = eventRegistryEvent.getEventInstance();
        assertThat(eventInstance.getEventModel().getKey()).isEqualTo("myEvent");

        assertThat(eventInstance.getCorrelationParameterInstances())
            .extracting(EventCorrelationParameterInstance::getValue).containsOnly("test");
        assertThat(eventInstance.getPayloadInstances())
            .extracting(p -> p.getEventPayloadDefinition().getName(), p -> p.getEventPayloadDefinition().getType(), EventPayloadInstance::getValue)
            .containsOnly(
                tuple("customerId", EventPayloadTypes.STRING, "test"),
                tuple("payload1", EventPayloadTypes.STRING, "Hello World"),
                tuple("payload2", EventPayloadTypes.INTEGER, 123)
            );
    }

    protected TestInboundEventChannelAdapter setupTestChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();

        eventEngineConfiguration.getEventRegistry().newInboundChannelModel()
            .key("test-channel")
            .channelAdapter(inboundEventChannelAdapter)
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("type")
            .jsonFieldsMapDirectlyToPayload()
            .register();

        return inboundEventChannelAdapter;
    }

    protected TestInboundEventChannelAdapter setupTestChannelWithCustomDeserializer() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();

        eventEngineConfiguration.getEventRegistry().newInboundChannelModel()
            .key("test-channel")
            .channelAdapter(inboundEventChannelAdapter)
            .deserializer(new InboundEventDeserializer<Customer>() {

                @Override
                public Customer deserialize(String rawEvent) {
                    try {
                        return new ObjectMapper().readValue(rawEvent, Customer.class);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            })
            .detectEventKeyUsingKeyDetector(Customer::getType)
            .payloadExtractor(new InboundEventPayloadExtractor<Customer>() {

                @Override
                public Collection<EventCorrelationParameterInstance> extractCorrelationParameters(EventModel eventDefinition, Customer event) {
                    EventCorrelationParameter correlationParameterDefinition = eventDefinition.getCorrelationParameters()
                        .stream()
                        .filter(parameterDefinition -> Objects.equals("customerId", parameterDefinition.getName()))
                        .findAny()
                        .orElse(null);
                    return Collections.singleton(new EventCorrelationParameterInstanceImpl(correlationParameterDefinition, event.getCustomerId()));
                }

                @Override
                public Collection<EventPayloadInstance> extractPayload(EventModel eventDefinition, Customer event) {
                    Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
                    for (EventPayload eventPayloadDefinition : eventDefinition.getPayload()) {
                        switch (eventPayloadDefinition.getName()) {
                            case "payload1":
                                payloadInstances.add(new EventPayloadInstanceImpl(eventPayloadDefinition, event.getPayload1()));
                                break;
                            case "payload2":
                                payloadInstances.add(new EventPayloadInstanceImpl(eventPayloadDefinition, event.getPayload2()));
                                break;
                            case "customerId":
                                payloadInstances.add(new EventPayloadInstanceImpl(eventPayloadDefinition, event.getCustomerId()));
                                break;
                        }
                    }

                    return payloadInstances;
                }
            })
            .register();

        return inboundEventChannelAdapter;
    }

    private static class TestEventConsumer implements EventRegistryEventConsumer {

        public List<EventRegistryEvent> eventsReceived = new ArrayList<>();

        @Override
        public String getConsumerKey() {
            return "myTestEventConsumer";
        }
        
        @Override
        public void eventReceived(EventRegistryEvent event) {
            eventsReceived.add(event);
        }

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
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "myEvent");
            json.put("customerId", "test");
            json.put("payload1", "Hello World");
            json.put("payload2", 123);
            try {
                eventRegistry.eventReceived(channelKey, objectMapper.writeValueAsString(json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static class Customer {

        protected String type;
        protected String customerId;
        protected String payload1;
        protected Integer payload2;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getPayload1() {
            return payload1;
        }

        public void setPayload1(String payload1) {
            this.payload1 = payload1;
        }

        public Integer getPayload2() {
            return payload2;
        }

        public void setPayload2(Integer payload2) {
            this.payload2 = payload2;
        }
    }

}
