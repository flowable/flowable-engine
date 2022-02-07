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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.eventregistry.api.EventConsumerInfo;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventregistry.api.EventRegistryNonMatchingEventConsumer;
import org.flowable.eventregistry.api.EventRegistryProcessingInfo;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.DefaultInboundEventProcessor;
import org.flowable.eventregistry.impl.event.FlowableEventRegistryEvent;
import org.flowable.eventregistry.impl.pipeline.DefaultInboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
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

    protected Map<Object, Object> initialBeans;

    @BeforeEach
    public void setup() {
        testEventConsumer = new TestEventConsumer();
        initialBeans = eventEngineConfiguration.getExpressionManager().getBeans();
        eventEngineConfiguration.getExpressionManager().setBeans(new HashMap<>());

        EventRegistry eventRegistry = eventEngineConfiguration.getEventRegistry();
        eventRegistry.registerEventRegistryEventConsumer(this.testEventConsumer);
        eventRegistry.setInboundEventProcessor(new DefaultInboundEventProcessor(eventRegistry));
    }

    @AfterEach
    public void tearDown() {
        eventEngineConfiguration.getExpressionManager().setBeans(initialBeans);
        List<EventDeployment> eventDeployments = repositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : eventDeployments) {
            repositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    public void testPropertiesPassedToChannelAdapter() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannel();
        assertThat(inboundEventChannelAdapter.inboundChannelModel.getKey()).isEqualTo("test-channel");
        assertThat(inboundEventChannelAdapter.eventRegistry).isEqualTo(eventEngineConfiguration.getEventRegistry());
    }

    @Test
    public void testDefaultInboundEventPipeline() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannel();

        repositoryService.createEventModelBuilder()
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
        assertThat(eventInstance.getEventKey()).isEqualTo("myEvent");

        assertThat(eventInstance.getCorrelationParameterInstances())
                .extracting(EventPayloadInstance::getValue)
                .containsOnly("test");
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
        assertThat(eventInstance.getEventKey()).isEqualTo("myEvent");

        assertThat(eventInstance.getCorrelationParameterInstances())
                .extracting(EventPayloadInstance::getValue)
                .containsOnly("test");
        assertThat(eventInstance.getPayloadInstances())
                .extracting(p -> p.getEventPayloadDefinition().getName(), p -> p.getEventPayloadDefinition().getType(), EventPayloadInstance::getValue)
                .containsOnly(
                        tuple("customerId", EventPayloadTypes.STRING, "test"),
                        tuple("payload1", EventPayloadTypes.STRING, "Hello World"),
                        tuple("payload2", EventPayloadTypes.INTEGER, 123)
                );
    }
    
    @Test
    public void testMissingEventConsumer() {
        eventEngineConfiguration.getEventRegistryEventConsumers().remove(testEventConsumer.getConsumerKey());
        TestNonMatchingEventConsumer testNonMatchingEventConsumer = new TestNonMatchingEventConsumer();
        eventEngineConfiguration.setNonMatchingEventConsumer(testNonMatchingEventConsumer);
        try {
            TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannel();
    
            repositoryService.createEventModelBuilder()
                    .key("myEvent")
                    .resourceName("myEvent.event")
                    .correlationParameter("customerId", EventPayloadTypes.STRING)
                    .payload("payload1", EventPayloadTypes.STRING)
                    .payload("payload2", EventPayloadTypes.INTEGER)
                    .deploy();
    
            inboundEventChannelAdapter.triggerTestEvent();
    
            assertThat(testNonMatchingEventConsumer.eventsReceived).hasSize(1);
            FlowableEventRegistryEvent eventRegistryEvent = (FlowableEventRegistryEvent) testNonMatchingEventConsumer.eventsReceived.get(0);
    
            EventInstance eventInstance = eventRegistryEvent.getEventInstance();
            assertThat(eventInstance.getEventKey()).isEqualTo("myEvent");
    
            assertThat(eventInstance.getCorrelationParameterInstances())
                    .extracting(EventPayloadInstance::getValue)
                    .containsOnly("test");
            assertThat(eventInstance.getPayloadInstances())
                    .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getDefinitionType, EventPayloadInstance::getValue)
                    .containsOnly(
                            tuple("customerId", EventPayloadTypes.STRING, "test"),
                            tuple("payload1", EventPayloadTypes.STRING, "Hello World"),
                            tuple("payload2", EventPayloadTypes.INTEGER, 123)
                    );
            
        } finally {
            eventEngineConfiguration.setNonMatchingEventConsumer(null);
        }
    }
    
    @Test
    public void testMissingEventConsumerNotCalled() {
        TestNonMatchingEventConsumer testNonMatchingEventConsumer = new TestNonMatchingEventConsumer();
        eventEngineConfiguration.setNonMatchingEventConsumer(testNonMatchingEventConsumer);
        try {
            TestInboundEventChannelAdapter inboundEventChannelAdapter = setupTestChannel();
    
            repositoryService.createEventModelBuilder()
                    .key("myEvent")
                    .resourceName("myEvent.event")
                    .correlationParameter("customerId", EventPayloadTypes.STRING)
                    .payload("payload1", EventPayloadTypes.STRING)
                    .payload("payload2", EventPayloadTypes.INTEGER)
                    .deploy();
    
            inboundEventChannelAdapter.triggerTestEvent();
    
            assertThat(testNonMatchingEventConsumer.eventsReceived).hasSize(0);
            assertThat(testEventConsumer.eventsReceived).hasSize(1);
            FlowableEventRegistryEvent eventRegistryEvent = (FlowableEventRegistryEvent) testEventConsumer.eventsReceived.get(0);
    
            EventInstance eventInstance = eventRegistryEvent.getEventInstance();
            assertThat(eventInstance.getEventKey()).isEqualTo("myEvent");
    
            assertThat(eventInstance.getCorrelationParameterInstances())
                    .extracting(EventPayloadInstance::getValue)
                    .containsOnly("test");
            assertThat(eventInstance.getPayloadInstances())
                    .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getDefinitionType, EventPayloadInstance::getValue)
                    .containsOnly(
                            tuple("customerId", EventPayloadTypes.STRING, "test"),
                            tuple("payload1", EventPayloadTypes.STRING, "Hello World"),
                            tuple("payload2", EventPayloadTypes.INTEGER, 123)
                    );
            
        } finally {
            eventEngineConfiguration.setNonMatchingEventConsumer(null);
        }
    }

    protected TestInboundEventChannelAdapter setupTestChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();
        eventEngineConfiguration.getExpressionManager().getBeans()
                .put("inboundEventChannelAdapter", inboundEventChannelAdapter);

        eventEngineConfiguration.getEventRepositoryService().createInboundChannelModelBuilder()
                .key("test-channel")
                .resourceName("test.channel")
                .channelAdapter("${inboundEventChannelAdapter}")
                .jsonDeserializer()
                .detectEventKeyUsingJsonField("type")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();

        return inboundEventChannelAdapter;
    }

    @SuppressWarnings("unchecked")
    protected TestInboundEventChannelAdapter setupTestChannelWithCustomDeserializer() {
        eventEngineConfiguration.getEventRepositoryService().createInboundChannelModelBuilder()
                .key("test-channel")
                .resourceName("test.channel")
                .jmsChannelAdapter("test")
                .eventProcessingPipeline()
                .jsonDeserializer()
                .fixedEventKey("test")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();

        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();
        InboundChannelModel inboundChannelModel = (InboundChannelModel) eventEngineConfiguration.getEventRepositoryService()
                .getChannelModelByKey("test-channel");
        DefaultInboundEventProcessingPipeline<Customer> inboundEventProcessingPipeline = (DefaultInboundEventProcessingPipeline<Customer>) inboundChannelModel
                .getInboundEventProcessingPipeline();
        inboundEventProcessingPipeline.setInboundEventDeserializer(new InboundEventDeserializer<Customer>() {

            @Override
            public Customer deserialize(String rawEvent) {
                try {
                    return new ObjectMapper().readValue(rawEvent, Customer.class);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

        });

        inboundEventProcessingPipeline.setInboundEventKeyDetector(Customer::getType);
        inboundEventProcessingPipeline.setInboundEventPayloadExtractor(new InboundEventPayloadExtractor<Customer>() {

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
        });

        inboundEventChannelAdapter.setInboundChannelModel(inboundChannelModel);
        inboundEventChannelAdapter.setEventRegistry(eventEngineConfiguration.getEventRegistry());

        return inboundEventChannelAdapter;
    }

    private static class TestEventConsumer implements EventRegistryEventConsumer {

        public List<EventRegistryEvent> eventsReceived = new ArrayList<>();

        @Override
        public String getConsumerKey() {
            return "myTestEventConsumer";
        }

        @Override
        public EventRegistryProcessingInfo eventReceived(EventRegistryEvent event) {
            eventsReceived.add(event);
            EventRegistryProcessingInfo eventRegistryProcessingInfo = new EventRegistryProcessingInfo();
            eventRegistryProcessingInfo.addEventConsumerInfo(new EventConsumerInfo());
            return eventRegistryProcessingInfo;
        }

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

        public void triggerTestEvent() {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "myEvent");
            json.put("customerId", "test");
            json.put("payload1", "Hello World");
            json.put("payload2", 123);
            try {
                eventRegistry.eventReceived(inboundChannelModel, objectMapper.writeValueAsString(json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static class TestNonMatchingEventConsumer implements EventRegistryNonMatchingEventConsumer {

        public List<EventRegistryEvent> eventsReceived = new ArrayList<>();

        @Override
        public void handleNonMatchingEvent(EventRegistryEvent event, EventRegistryProcessingInfo eventRegistryProcessingInfo) {
            eventsReceived.add(event);
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
