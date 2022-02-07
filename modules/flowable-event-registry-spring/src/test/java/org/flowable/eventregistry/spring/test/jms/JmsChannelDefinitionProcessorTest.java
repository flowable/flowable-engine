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
package org.flowable.eventregistry.spring.test.jms;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventregistry.spring.test.TestEventConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Filip Hrisafov
 */
@JmsEventTest
@TestPropertySource(properties = {
        "application.test.jms-queue=test-expression-customer"
})
class JmsChannelDefinitionProcessorTest {

    @Autowired
    protected JmsTemplate jmsTemplate;

    @Autowired
    protected EventRegistry eventRegistry;

    @Autowired
    protected EventRepositoryService eventRepositoryService;

    protected TestEventConsumer testEventConsumer;

    @BeforeEach
    void setUp() {
        testEventConsumer = new TestEventConsumer();
        eventRegistry.registerEventRegistryEventConsumer(testEventConsumer);
    }

    @AfterEach
    void tearDown() {
        testEventConsumer.clear();

        List<EventDeployment> eventDeployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : eventDeployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
        
        eventRegistry.removeFlowableEventRegistryEventConsumer(testEventConsumer);
    }

    @Test
    void eventShouldBeReceivedWhenChannelDefinitionIsRegistered() {
        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .jmsChannelAdapter("test-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        jmsTemplate.convertAndSend("test-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}");

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsExactlyInAnyOrder("test"));

        EventInstance eventInstance = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(eventInstance).isNotNull();
        assertThat(eventInstance.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit"),
                tuple("name", "Kermit the Frog")
            );
        assertThat(eventInstance.getCorrelationParameterInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit")
            );
    }

    @Test
    void jmsDestinationIsCorrectlyResolvedFromExpression() {
        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .jmsChannelAdapter("${application.test.jms-queue}")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        jmsTemplate.convertAndSend("test-expression-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}");

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsExactlyInAnyOrder("test"));

        EventInstance eventInstance = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(eventInstance).isNotNull();
        assertThat(eventInstance.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit"),
                tuple("name", "Kermit the Frog")
            );
        assertThat(eventInstance.getCorrelationParameterInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit")
            );
    }

    @Test
    void jmsDestinationIsCorrectlyResolvedFromExpressionUsingEnvironmentAsBean() {
        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .jmsChannelAdapter("test.#{environment.getProperty('application.test.jms-queue')}")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        jmsTemplate.convertAndSend("test.test-expression-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}");

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsExactlyInAnyOrder("test"));

        EventInstance eventInstance = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(eventInstance).isNotNull();
        assertThat(eventInstance.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit"),
                tuple("name", "Kermit the Frog")
            );
        assertThat(eventInstance.getCorrelationParameterInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit")
            );
    }

    @Test
    void jmsDestinationIsCorrectlyResolvedFromExpressionUsingCombinationForProperty() {
        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .jmsChannelAdapter("test-combination.${application.test.jms-queue}")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        jmsTemplate.convertAndSend("test-combination.test-expression-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}");

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsExactlyInAnyOrder("test"));

        EventInstance eventInstance = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(eventInstance).isNotNull();
        assertThat(eventInstance.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit"),
                tuple("name", "Kermit the Frog")
            );
        assertThat(eventInstance.getCorrelationParameterInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit")
            );
    }
    
    @Test
    void eventShouldBeReceivedWhenChannelModelIsDeployed() {
        EventDeployment deployment = eventRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/jmsEvent.event")
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/jmsChannel.channel")
            .deploy();

        try {
            jmsTemplate.convertAndSend("test-customer", "{"
                + "    \"eventKey\": \"test\","
                + "    \"customer\": \"kermit\","
                + "    \"name\": \"Kermit the Frog\""
                + "}");
    
            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                    .extracting(EventRegistryEvent::getType)
                    .containsExactlyInAnyOrder("test"));
    
            EventInstance eventInstance = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();
    
            assertThat(eventInstance).isNotNull();
            assertThat(eventInstance.getPayloadInstances())
                .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
                .containsExactlyInAnyOrder(
                    tuple("customer", "kermit"),
                    tuple("name", "Kermit the Frog")
                );
            assertThat(eventInstance.getCorrelationParameterInstances())
                .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
                .containsExactlyInAnyOrder(
                    tuple("customer", "kermit")
                );
            
        } finally {
            eventRepositoryService.deleteDeployment(deployment.getId());
        }
    }

    @Test
    void eventShouldBeReceivedAfterChannelDefinitionIsRegistered() {
        jmsTemplate.convertAndSend("test-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}");
        
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .jmsChannelAdapter("test-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        jmsTemplate.convertAndSend("test-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"fozzie\","
            + "    \"name\": \"Fozzie Bear\""
            + "}");

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsExactlyInAnyOrder("test", "test"));

        EventInstance kermitEvent = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(kermitEvent).isNotNull();
        assertThat(kermitEvent.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit"),
                tuple("name", "Kermit the Frog")
            );
        assertThat(kermitEvent.getCorrelationParameterInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit")
            );

        EventInstance fozzieEvent = (EventInstance) testEventConsumer.getEvents().get(1).getEventObject();

        assertThat(fozzieEvent).isNotNull();
        assertThat(fozzieEvent.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "fozzie"),
                tuple("name", "Fozzie Bear")
            );
        assertThat(fozzieEvent.getCorrelationParameterInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "fozzie")
            );
    }

    @Test
    void eventShouldBeSendAfterOutboundChannelDefinitionIsRegistered() {
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("customer")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
            .key("outboundCustomer")
            .resourceName("outbound.channel")
            .jmsChannelAdapter("outbound-customer")
            .eventProcessingPipeline()
            .jsonSerializer()
            .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = jmsTemplate.receiveAndConvert("outbound-customer");
        assertThat(message).isNotNull();
        assertThatJson(message)
            .isEqualTo("{"
                + "  customer: 'kermit',"
                + "  name: 'Kermit the Frog'"
                + "}");
    }

    @Test
    void eventShouldBeSendAfterOutboundChannelModelIsDeployed() {
        EventDeployment deployment = eventRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/jmsOutboundEvent.event")
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/jmsOutboundChannel.channel")
            .deploy();

        try {

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            Object message = jmsTemplate.receiveAndConvert("outbound-customer");
            assertThat(message).isNotNull();
            assertThatJson(message)
                .isEqualTo("{"
                    + "  customer: 'kermit',"
                    + "  name: 'Kermit the Frog'"
                    + "}");
        } finally {
            eventRepositoryService.deleteDeployment(deployment.getId());
        }
    }

    @Test
    void jmsOutboundChannelShouldResolveDestinationFromExpression() {
        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("customer")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
                .key("outboundCustomer")
                .resourceName("outbound.channel")
                .jmsChannelAdapter("${application.test.jms-queue}")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = await("receive message")
                .atMost(Duration.ofSeconds(10))
                .until(() -> jmsTemplate.receiveAndConvert("test-expression-customer"), Objects::nonNull);
        assertThat(message).isNotNull();
        assertThatJson(message)
                .isEqualTo("{"
                        + "  customer: 'kermit',"
                        + "  name: 'Kermit the Frog'"
                        + "}");
    }

    @Test
    void jmsOutboundChannelShouldResolveDestinationFromExpressionUsingCombinationForProperty() {
        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("customer")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
                .key("outboundCustomer")
                .resourceName("outbound.channel")
                .jmsChannelAdapter("test-combination.${application.test.jms-queue}")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = await("receive message")
                .atMost(Duration.ofSeconds(10))
                .until(() -> jmsTemplate.receiveAndConvert("test-combination.test-expression-customer"), Objects::nonNull);;
        assertThat(message).isNotNull();
        assertThatJson(message)
                .isEqualTo("{"
                        + "  customer: 'kermit',"
                        + "  name: 'Kermit the Frog'"
                        + "}");
    }

    @Test
    void jmsOutboundChannelShouldResolveDestinationFromExpressionUsingEnvironmentAsBean() {
        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("customer")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
                .key("outboundCustomer")
                .resourceName("outbound.channel")
                .jmsChannelAdapter("test.#{environment.getProperty('application.test.jms-queue')}")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = await("receive message")
                .atMost(Duration.ofSeconds(10))
                .until(() -> jmsTemplate.receiveAndConvert("test.test-expression-customer"), Objects::nonNull);
        assertThat(message).isNotNull();
        assertThatJson(message)
                .isEqualTo("{"
                        + "  customer: 'kermit',"
                        + "  name: 'Kermit the Frog'"
                        + "}");
    }
}
