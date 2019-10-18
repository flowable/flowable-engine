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
package org.flowable.common.engine.impl.eventregistry;

import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.flowable.common.engine.api.eventbus.FlowableEventBus;
import org.flowable.common.engine.api.eventbus.FlowableEventBusConsumer;
import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.InboundEventChannelAdapter;
import org.flowable.common.engine.api.eventregistry.InboundEventDeserializer;
import org.flowable.common.engine.api.eventregistry.InboundEventPayloadExtractor;
import org.flowable.common.engine.api.eventregistry.definition.EventCorrelationParameterDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventPayloadDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventPayloadTypes;
import org.flowable.common.engine.api.eventregistry.runtime.EventCorrelationParameterInstance;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;
import org.flowable.common.engine.api.eventregistry.runtime.EventPayloadInstance;
import org.flowable.common.engine.impl.eventbus.BasicFlowableEventBus;
import org.flowable.common.engine.impl.eventregistry.event.EventRegistryEvent;
import org.flowable.common.engine.impl.eventregistry.runtime.EventCorrelationParameterInstanceImpl;
import org.flowable.common.engine.impl.eventregistry.runtime.EventPayloadInstanceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class DefaultEventRegistryTest {

    private EventRegistry eventRegistry;
    private TestEventConsumer testEventConsumer;

    @BeforeEach
    public void setup() {
        FlowableEventBus eventBus = new BasicFlowableEventBus();

        testEventConsumer = new TestEventConsumer();
        eventBus.addFlowableEventConsumer(this.testEventConsumer);

        eventRegistry = new DefaultEventRegistry(eventBus);
        eventRegistry.setInboundEventProcessor(new DefaultInboundEventProcessor(eventRegistry, eventBus));
    }

    @Test
    public void testPropertiesPassedToChannelAdapter() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannel();
        assertThat(inboundEventChannelAdapter.channelKey).isEqualTo("test-channel");
        assertThat(inboundEventChannelAdapter.eventRegistry).isEqualTo(eventRegistry);
    }

    @Test
    public void testDefaultInboundEventPipeline() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannel();

        eventRegistry.newEventDefinition()
            .channelKey("test-channel")
            .key("myEvent")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .payload("payload1", EventPayloadTypes.STRING)
            .payload("payload2", EventPayloadTypes.INTEGER)
            .register();

        inboundEventChannelAdapter.triggerTestEvent();

        assertThat(testEventConsumer.eventsReceived).hasSize(1);
        EventRegistryEvent eventRegistryEvent = (EventRegistryEvent) testEventConsumer.eventsReceived.get(0);

        EventInstance eventInstance = eventRegistryEvent.getEventInstance();
        assertThat(eventInstance.getEventDefinition().getKey()).isEqualTo("myEvent");

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

        eventRegistry.newEventDefinition()
            .channelKey("test-channel")
            .key("myEvent")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .payload("payload1", EventPayloadTypes.STRING)
            .payload("payload2", EventPayloadTypes.INTEGER)
            .register();

        inboundEventChannelAdapter.triggerTestEvent();

        assertThat(testEventConsumer.eventsReceived).hasSize(1);
        EventRegistryEvent eventRegistryEvent = (EventRegistryEvent) testEventConsumer.eventsReceived.get(0);

        EventInstance eventInstance = eventRegistryEvent.getEventInstance();
        assertThat(eventInstance.getEventDefinition().getKey()).isEqualTo("myEvent");

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

        eventRegistry.newChannelDefinition()
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

        eventRegistry.newChannelDefinition()
            .key("test-channel")
            .channelAdapter(inboundEventChannelAdapter)
            .deserializer(new InboundEventDeserializer<Customer>() {

                @Override
                public String getType() {
                    return "customer";
                }

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
                public Collection<EventCorrelationParameterInstance> extractCorrelationParameters(EventDefinition eventDefinition, Customer event) {
                    EventCorrelationParameterDefinition correlationParameterDefinition = eventDefinition.getCorrelationParameterDefinitions()
                        .stream()
                        .filter(parameterDefinition -> Objects.equals("customerId", parameterDefinition.getName()))
                        .findAny()
                        .orElse(null);
                    return Collections.singleton(new EventCorrelationParameterInstanceImpl(correlationParameterDefinition, event.getCustomerId()));
                }

                @Override
                public Collection<EventPayloadInstance> extractPayload(EventDefinition eventDefinition, Customer event) {
                    Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
                    for (EventPayloadDefinition eventPayloadDefinition : eventDefinition.getEventPayloadDefinitions()) {
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

    private static class TestEventConsumer implements FlowableEventBusConsumer {

        public List<FlowableEventBusEvent> eventsReceived = new ArrayList<>();

        @Override
        public Collection<String> getSupportedTypes() {
            return Collections.singletonList("myEvent");
        }

        @Override
        public void eventReceived(FlowableEventBusEvent event) {
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
