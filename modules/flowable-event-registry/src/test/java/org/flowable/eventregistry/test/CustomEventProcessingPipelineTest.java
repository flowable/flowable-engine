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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.InboundEventTenantDetector;
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.pipeline.DefaultInboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.pipeline.DefaultOutboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class CustomEventProcessingPipelineTest extends AbstractFlowableEventTest {

    @AfterEach
    public void cleanup () {
        eventRegistryEngine.getEventRepositoryService().createDeploymentQuery().list()
            .forEach(eventDeployment -> eventRegistryEngine.getEventRepositoryService().deleteDeployment(eventDeployment.getId()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCustomInboundPipelineInvoked() {

        TestInboundChannelAdapter testInboundChannelAdapter = new TestInboundChannelAdapter();
        TestInboundEventDeserializer testInboundEventDeserializer = new TestInboundEventDeserializer();
        TestInboundEventKeyDetector testInboundEventKeyDetector = new TestInboundEventKeyDetector();
        TestInboundEventTenantDetector testInboundEventTenantDetector = new TestInboundEventTenantDetector();
        TestInboundEventPayloadExtractor testInboundEventPayloadExtractor = new TestInboundEventPayloadExtractor();
        TestInboundEventTransformer testInboundEventTransformer = new TestInboundEventTransformer();

        eventRegistryEngine.getEventRepositoryService().createInboundChannelModelBuilder()
            .key("customTestChannel")
            .resourceName("customTest.channel")
            .jmsChannelAdapter("test")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .fixedEventKey("test")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();
        
        InboundChannelModel inboundChannelModel = (InboundChannelModel) eventRegistryEngine.getEventRepositoryService().getChannelModelByKey("customTestChannel");
        inboundChannelModel.setInboundEventChannelAdapter(testInboundChannelAdapter);
        DefaultInboundEventProcessingPipeline<String> inboundEventProcessingPipeline = (DefaultInboundEventProcessingPipeline<String>) inboundChannelModel.getInboundEventProcessingPipeline();
        inboundEventProcessingPipeline.setInboundEventDeserializer(testInboundEventDeserializer);
        inboundEventProcessingPipeline.setInboundEventKeyDetector(testInboundEventKeyDetector);
        inboundEventProcessingPipeline.setInboundEventTenantDetector(testInboundEventTenantDetector);
        inboundEventProcessingPipeline.setInboundEventPayloadExtractor(testInboundEventPayloadExtractor);
        inboundEventProcessingPipeline.setInboundEventTransformer(testInboundEventTransformer);
        
        testInboundChannelAdapter.setEventRegistry(eventRegistryEngine.getEventRegistry());
        testInboundChannelAdapter.setInboundChannelModel(inboundChannelModel);

        eventRegistryEngine.getEventRepositoryService().createEventModelBuilder()
            .key("testKey")
            .deploymentTenantId("testTenantId")
            .resourceName("myEvent.event")
            .deploy();

        testInboundChannelAdapter.trigger("testEvent");

        assertThat(testInboundEventDeserializer.counter.get()).isEqualTo(1);
        assertThat(testInboundEventKeyDetector.counter.get()).isEqualTo(1);
        assertThat(testInboundEventTenantDetector.counter.get()).isEqualTo(1);
        assertThat(testInboundEventPayloadExtractor.correlationCounter.get()).isEqualTo(1);
        assertThat(testInboundEventPayloadExtractor.payloadCounter.get()).isEqualTo(1);
        assertThat(testInboundEventTransformer.counter.get()).isEqualTo(1);

    }

    @Test
    public void testCustomOutboundPipelineInvoked() {

        TestOutboundChannelAdapter testOutboundChannelAdapter = new TestOutboundChannelAdapter();
        TestOutboundEventSerializer testOutboundEventSerializer = new TestOutboundEventSerializer();

        eventRegistryEngine.getEventRepositoryService().createOutboundChannelModelBuilder()
            .key("customTestOutboundChannel")
            .resourceName("customOutboundTest.channel")
            .jmsChannelAdapter("testOut")
            .eventProcessingPipeline()
            .jsonSerializer()
            .deploy();
        
        OutboundChannelModel outboundChannelModel = (OutboundChannelModel) eventRegistryEngine.getEventRepositoryService().getChannelModelByKey("customTestOutboundChannel");
        outboundChannelModel.setOutboundEventChannelAdapter(testOutboundChannelAdapter);
        DefaultOutboundEventProcessingPipeline outboundEventProcessingPipeline = (DefaultOutboundEventProcessingPipeline) outboundChannelModel.getOutboundEventProcessingPipeline();
        outboundEventProcessingPipeline.setOutboundEventSerializer(testOutboundEventSerializer);

        eventRegistryEngine.getEventRepositoryService().createEventModelBuilder()
            .key("testKey")
            .deploymentTenantId("testTenantId")
            .outboundChannelKey("customTestOutboundChannel")
            .resourceName("myEvent.event")
            .deploy();
        
        EventModel eventModel = eventRegistryEngine.getEventRepositoryService().getEventModelByKey("testKey", "testTenantId", false);

        EventInstanceImpl eventInstance = new EventInstanceImpl();
        eventInstance.setEventModel(eventModel);
        eventRegistryEngine.getEventRegistry().sendEventOutbound(eventInstance);

        assertThat(testOutboundChannelAdapter.counter.get()).isEqualTo(1);
        assertThat(testOutboundEventSerializer.counter.get()).isEqualTo(1);
    }

    private static class TestInboundChannelAdapter implements InboundEventChannelAdapter {

        private InboundChannelModel inboundChannelModel;
        private EventRegistry eventRegistry;

        public void trigger(String event) {
            eventRegistry.eventReceived(inboundChannelModel, event);
        }

        @Override
        public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
            this.inboundChannelModel = inboundChannelModel;
        }
        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
            this.eventRegistry = eventRegistry;
        }
    }

    private static class TestInboundEventDeserializer implements InboundEventDeserializer<String> {

        public AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String deserialize(String rawEvent) {
            counter.incrementAndGet();
            return rawEvent;
        }

        @Override
        public String getType() {
            return "test";
        }
    }

    private static class TestInboundEventKeyDetector implements InboundEventKeyDetector<String> {

        public AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String detectEventDefinitionKey(String event) {
            counter.incrementAndGet();
            return "testKey";
        }
    }

    private static class TestInboundEventTenantDetector implements InboundEventTenantDetector<String> {

        public AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String detectTenantId(String event) {
            counter.incrementAndGet();
            return "testTenantId";
        }
    }

    private static class TestInboundEventPayloadExtractor implements InboundEventPayloadExtractor<String> {

        public AtomicInteger correlationCounter = new AtomicInteger(0);
        public AtomicInteger payloadCounter = new AtomicInteger(0);

        @Override
        public Collection<EventCorrelationParameterInstance> extractCorrelationParameters(EventModel eventDefinition, String event) {
            correlationCounter.incrementAndGet();
            return Collections.emptyList();
        }

        @Override
        public Collection<EventPayloadInstance> extractPayload(EventModel eventDefinition, String event) {
            payloadCounter.incrementAndGet();
            return Collections.emptyList();
        }

    }

    private static class TestInboundEventTransformer implements InboundEventTransformer {

        public AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Collection<EventRegistryEvent> transform(EventInstance eventInstance) {
            counter.incrementAndGet();
            EventRegistryEvent eventRegistryEvent = new EventRegistryEvent() {

                @Override
                public String getType() {
                    return "testType";
                }
                @Override
                public Object getEventObject() {
                    return "test";
                }
            };
            return Collections.singletonList(eventRegistryEvent);
        }
    }

    private static class TestOutboundChannelAdapter implements OutboundEventChannelAdapter {

        public AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void sendEvent(String rawEvent) {
            counter.incrementAndGet();
        }

    }

    private static class TestOutboundEventSerializer implements OutboundEventSerializer {

        public AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String serialize(EventInstance eventInstance) {
            counter.incrementAndGet();
            return "test";
        }

        @Override
        public String getType() {
            return "test";
        }
    }

}
