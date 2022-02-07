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
package org.flowable.eventregistry.spring.test.rabbit;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Filip Hrisafov
 */
@RabbitEventTest
@TestPropertySource(properties = {
        "application.test.rabbit-queue=test-expression-customer"
})
class RabbitChannelDefinitionProcessorTest {

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected EventRegistry eventRegistry;

    @Autowired
    protected EventRepositoryService eventRepositoryService;

    @Autowired
    protected RabbitAdmin rabbitAdmin;

    protected TestEventConsumer testEventConsumer;

    protected Collection<String> queuesToDelete = new HashSet<>();

    @BeforeEach
    void setUp() {
        testEventConsumer = new TestEventConsumer();
        eventRegistry.registerEventRegistryEventConsumer(testEventConsumer);
    }

    @AfterEach
    void tearDown() {
        testEventConsumer.clear();
        
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }

        eventRegistry.removeFlowableEventRegistryEventConsumer(testEventConsumer);

        queuesToDelete.forEach(rabbitAdmin::deleteQueue);
    }

    @Test
    void eventShouldBeReceivedWhenChannelDefinitionIsRegistered() {
        rabbitAdmin.declareQueue(new Queue("test-customer"));
        queuesToDelete.add("test-customer");

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .rabbitChannelAdapter("test-customer")
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

        rabbitTemplate.convertAndSend("test-customer", "{"
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
    void rabbitQueueIsCorrectlyResolvedFromExpression() {
        rabbitAdmin.declareQueue(new Queue("test-expression-customer"));
        queuesToDelete.add("test-expression-customer");

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .rabbitChannelAdapter("${application.test.rabbit-queue}")
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

        rabbitTemplate.convertAndSend("test-expression-customer", "{"
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
    void rabbitQueueIsCorrectlyResolvedFromExpressionUsingEnvironmentAsBean() {
        rabbitAdmin.declareQueue(new Queue("test-expression-customer-environment"));
        queuesToDelete.add("test-expression-customer-environment");

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .rabbitChannelAdapter("#{environment.getProperty('application.test.rabbit-queue')}-environment")
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

        rabbitTemplate.convertAndSend("test-expression-customer-environment", "{"
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
    void rabbitQueueIsCorrectlyResolvedFromExpressionUsingCombinationForProperty() {
        rabbitAdmin.declareQueue(new Queue("combination-test-expression-customer"));
        queuesToDelete.add("combination-test-expression-customer");

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .rabbitChannelAdapter("combination-${application.test.rabbit-queue}")
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

        rabbitTemplate.convertAndSend("combination-test-expression-customer", "{"
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
    void eventShouldBeReceivedAfterChannelDefinitionIsRegistered() {
        rabbitAdmin.declareQueue(new Queue("test-customer"));
        queuesToDelete.add("test-customer");

        rabbitTemplate.convertAndSend("test-customer", "{"
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
            .rabbitChannelAdapter("test-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        rabbitTemplate.convertAndSend("test-customer", "{"
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
    void eventShouldBeSendAfterOutboundChannelDefinitionIsRegisteredWithDefinedExchange() {
        TopicExchange exchange = new TopicExchange("flowable-test");
        rabbitAdmin.declareExchange(exchange);
        Queue queue = new Queue("outbound-customer", false);
        rabbitAdmin.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("customer");
        rabbitAdmin.declareBinding(binding);

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("customer")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
            .key("outboundCustomer")
            .resourceName("outbound.channel")
            .rabbitChannelAdapter("customer")
            .exchange("flowable-test")
            .eventProcessingPipeline()
            .jsonSerializer()
            .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = rabbitTemplate.receiveAndConvert("outbound-customer");
        assertThat(message).isNotNull();
        assertThatJson(message)
            .isEqualTo("{"
                + "  customer: 'kermit',"
                + "  name: 'Kermit the Frog'"
                + "}");

        rabbitAdmin.removeBinding(binding);
        rabbitAdmin.deleteExchange(exchange.getName());
    }

    @Test
    void eventShouldBeSendAfterOutboundChannelDefinitionIsRegisteredWithUndefinedExchange() {
        rabbitAdmin.declareQueue(new Queue("outbound-customer"));
        queuesToDelete.add("outbound-customer");

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("customer")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
            .key("outboundCustomer")
            .resourceName("outboundCustomer.channel")
            .rabbitChannelAdapter("outbound-customer")
            .eventProcessingPipeline()
            .jsonSerializer()
            .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = rabbitTemplate.receiveAndConvert("outbound-customer");
        assertThat(message).isNotNull();
        assertThatJson(message)
            .isEqualTo("{"
                + "  customer: 'kermit',"
                + "  name: 'Kermit the Frog'"
                + "}");
    }

    @Test
    void eventShouldBeReceivedWhenChannelModelIsDeployed() {
        rabbitAdmin.declareQueue(new Queue("test-customer"));
        queuesToDelete.add("test-customer");

        EventDeployment deployment = eventRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/rabbitEvent.event")
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/rabbitChannel.channel")
            .deploy();

        try {
            rabbitTemplate.convertAndSend("test-customer", "{"
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
    void eventShouldBeSendAfterOutboundChannelModelIsDeployed() {
        rabbitAdmin.declareQueue(new Queue("outbound-customer"));
        queuesToDelete.add("outbound-customer");

        EventDeployment deployment = eventRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/rabbitOutboundEvent.event")
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/rabbitOutboundChannel.channel")
            .deploy();

        try {

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            Object message = rabbitTemplate.receiveAndConvert("outbound-customer");
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
    void rabbitOutboundChannelShouldResolveRoutingKeyFromExpression() {
        rabbitAdmin.declareQueue(new Queue("test-expression-customer"));
        queuesToDelete.add("test-expression-customer");

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("customer")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
                .key("outboundCustomer")
                .resourceName("outboundCustomer.channel")
                .rabbitChannelAdapter("${application.test.rabbit-queue}")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = rabbitTemplate.receiveAndConvert("test-expression-customer", 10_000);
        assertThat(message).isNotNull();
        assertThatJson(message)
                .isEqualTo("{"
                        + "  customer: 'kermit',"
                        + "  name: 'Kermit the Frog'"
                        + "}");
    }

    @Test
    void rabbitOutboundChannelShouldResolveRoutingKeyFromExpressionUsingCombinationForProperty() {
        rabbitAdmin.declareQueue(new Queue("combination-test-expression-customer"));
        queuesToDelete.add("combination-test-expression-customer");

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("customer")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
                .key("outboundCustomer")
                .resourceName("outboundCustomer.channel")
                .rabbitChannelAdapter("combination-${application.test.rabbit-queue}")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = rabbitTemplate.receiveAndConvert("combination-test-expression-customer", 10_000);
        assertThat(message).isNotNull();
        assertThatJson(message)
                .isEqualTo("{"
                        + "  customer: 'kermit',"
                        + "  name: 'Kermit the Frog'"
                        + "}");
    }

    @Test
    void rabbitOutboundChannelShouldResolveRoutingKeyFromExpressionUsingEnvironmentAsBean() {
        rabbitAdmin.declareQueue(new Queue("test-expression-customer-environment"));
        queuesToDelete.add("test-expression-customer-environment");

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("customer")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createOutboundChannelModelBuilder()
                .key("outboundCustomer")
                .resourceName("outboundCustomer.channel")
                .rabbitChannelAdapter("#{environment.getProperty('application.test.rabbit-queue')}-environment")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();

        ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

        Object message = rabbitTemplate.receiveAndConvert("test-expression-customer-environment", 10_000);
        assertThat(message).isNotNull();
        assertThatJson(message)
                .isEqualTo("{"
                        + "  customer: 'kermit',"
                        + "  name: 'Kermit the Frog'"
                        + "}");
    }

    @Test
    void rabbitOutboundChannelShouldResolveExchangeFromExpression() {
        TopicExchange exchange = new TopicExchange("test-expression-customer");
        rabbitAdmin.declareExchange(exchange);
        Queue queue = new Queue("outbound-customer", false);
        rabbitAdmin.declareQueue(queue);
        queuesToDelete.add("outbound-customer");
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("customer");
        rabbitAdmin.declareBinding(binding);

        try {
            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .payload("name", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createOutboundChannelModelBuilder()
                    .key("outboundCustomer")
                    .resourceName("outbound.channel")
                    .rabbitChannelAdapter("customer")
                    .exchange("${application.test.rabbit-queue}")
                    .eventProcessingPipeline()
                    .jsonSerializer()
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            Object message = rabbitTemplate.receiveAndConvert("outbound-customer", 10_000);
            assertThat(message).isNotNull();
            assertThatJson(message)
                    .isEqualTo("{"
                            + "  customer: 'kermit',"
                            + "  name: 'Kermit the Frog'"
                            + "}");
        } finally {
            rabbitAdmin.removeBinding(binding);
            rabbitAdmin.deleteExchange(exchange.getName());
        }

    }
}
