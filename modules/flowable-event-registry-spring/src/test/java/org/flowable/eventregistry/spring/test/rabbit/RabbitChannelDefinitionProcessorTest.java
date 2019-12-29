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

import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
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

/**
 * @author Filip Hrisafov
 */
@RabbitEventTest
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
        eventRegistry.registerEventRegistryEventBusConsumer(testEventConsumer);
    }

    @AfterEach
    void tearDown() {
        testEventConsumer.clear();
        eventRegistry.removeFlowableEventConsumer(testEventConsumer);
        queuesToDelete.forEach(rabbitAdmin::deleteQueue);
    }

    @Test
    void eventShouldBeReceivedWhenChannelDefinitionIsRegistered() {
        rabbitAdmin.declareQueue(new Queue("test-customer"));
        queuesToDelete.add("test-customer");

        eventRegistry.newInboundChannelModel()
            .key("testChannel")
            .rabbitChannelAdapter("test-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .register();

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .inboundChannelKey("testChannel")
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
            .extracting(EventCorrelationParameterInstance::getDefinitionName, EventCorrelationParameterInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit")
            );

        eventRegistry.removeChannelModel("testChannel");
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
            .inboundChannelKey("testChannel")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRegistry.newInboundChannelModel()
            .key("testChannel")
            .rabbitChannelAdapter("test-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .register();

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
            .extracting(EventCorrelationParameterInstance::getDefinitionName, EventCorrelationParameterInstance::getValue)
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
            .extracting(EventCorrelationParameterInstance::getDefinitionName, EventCorrelationParameterInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "fozzie")
            );

        eventRegistry.removeChannelModel("testChannel");
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
            .outboundChannelKey("outboundCustomer")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRegistry.newOutboundChannelModel()
            .key("outboundCustomer")
            .rabbitChannelAdapter("customer")
            .exchange("flowable-test")
            .eventProcessingPipeline()
            .jsonSerializer()
            .register();

        EventModel customerModel = eventRepositoryService.getEventModelByKey("customer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl(customerModel, Collections.emptyList(), payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent);

        Object message = rabbitTemplate.receiveAndConvert("outbound-customer");
        assertThat(message).isNotNull();
        assertThatJson(message)
            .isEqualTo("{"
                + "  customer: 'kermit',"
                + "  name: 'Kermit the Frog'"
                + "}");

        eventRegistry.removeChannelModel("outboundCustomer");
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
            .outboundChannelKey("outboundCustomer")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRegistry.newOutboundChannelModel()
            .key("outboundCustomer")
            .rabbitChannelAdapter("outbound-customer")
            .eventProcessingPipeline()
            .jsonSerializer()
            .register();

        EventModel customerModel = eventRepositoryService.getEventModelByKey("customer");

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
        payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
        EventInstance kermitEvent = new EventInstanceImpl(customerModel, Collections.emptyList(), payloadInstances);

        eventRegistry.sendEventOutbound(kermitEvent);

        Object message = rabbitTemplate.receiveAndConvert("outbound-customer");
        assertThat(message).isNotNull();
        assertThatJson(message)
            .isEqualTo("{"
                + "  customer: 'kermit',"
                + "  name: 'Kermit the Frog'"
                + "}");

        eventRegistry.removeChannelModel("outboundCustomer");
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
                .extracting(EventCorrelationParameterInstance::getDefinitionName, EventCorrelationParameterInstance::getValue)
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

            EventModel customerModel = eventRepositoryService.getEventModelByKey("customer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
            EventInstance kermitEvent = new EventInstanceImpl(customerModel, Collections.emptyList(), payloadInstances);

            eventRegistry.sendEventOutbound(kermitEvent);

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
}
