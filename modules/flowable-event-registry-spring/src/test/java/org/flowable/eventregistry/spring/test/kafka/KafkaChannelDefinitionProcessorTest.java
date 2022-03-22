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
package org.flowable.eventregistry.spring.test.kafka;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
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
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Filip Hrisafov
 */
@KafkaEventTest
@TestPropertySource(properties = {
        "application.test.kafka-topic=test-expression-customer"
})
class KafkaChannelDefinitionProcessorTest {

    @Autowired
    protected KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    protected EventRegistry eventRegistry;

    @Autowired
    protected EventRepositoryService eventRepositoryService;

    @Autowired
    protected AdminClient adminClient;

    @Autowired
    protected ConsumerFactory<Object, Object> consumerFactory;

    protected TestEventConsumer testEventConsumer;

    protected Collection<String> topicsToDelete = new HashSet<>();

    @BeforeEach
    void setUp() {
        testEventConsumer = new TestEventConsumer();
        eventRegistry.registerEventRegistryEventConsumer(testEventConsumer);
    }

    @AfterEach
    void tearDown() throws Exception {
        testEventConsumer.clear();

        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }

        eventRegistry.removeFlowableEventRegistryEventConsumer(testEventConsumer);

        Map<TopicPartition, RecordsToDelete> recordsToDelete = new HashMap<>();
        Map<String, TopicDescription> topicDescriptions = adminClient.describeTopics(topicsToDelete)
            .all()
            .get(10, TimeUnit.SECONDS);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testCleanup")) {

            List<TopicPartition> partitions = new ArrayList<>();
            for (TopicDescription topicDescription : topicDescriptions.values()) {
                for (TopicPartitionInfo partition : topicDescription.partitions()) {
                    partitions.add(new TopicPartition(topicDescription.name(), partition.partition()));
                }
            }

            for (Map.Entry<TopicPartition, Long> entry : consumer.endOffsets(partitions).entrySet()) {
                recordsToDelete.put(entry.getKey(), RecordsToDelete.beforeOffset(entry.getValue()));
            }

        }

        adminClient.deleteRecords(recordsToDelete)
            .all()
            .get(10, TimeUnit.SECONDS);
    }

    @Test
    void eventShouldBeReceivedWhenChannelDefinitionIsRegistered() throws Exception {
        createTopic("test-new-customer");

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("newCustomerChannel")
            .resourceName("customer.channel")
            .kafkaChannelAdapter("test-new-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        kafkaTemplate.send("test-new-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}")
            .get(5, TimeUnit.SECONDS);

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
    void kafkaTopicIsCorrectlyResolvedFromExpression() throws Exception {
        createTopic("test-expression-customer");

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("newCustomerChannel")
            .resourceName("customer.channel")
            .kafkaChannelAdapter("${application.test.kafka-topic}")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        kafkaTemplate.send("test-expression-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}")
            .get(5, TimeUnit.SECONDS);

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
    void kafkaTopicIsCorrectlyResolvedFromExpressionUsingCustomBean() throws Exception {
        createTopic("inbound-custom-bean-customer");

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("newCustomerChannel")
            .resourceName("customer.channel")
            .kafkaChannelAdapter("inbound-#{customPropertiesBean.getProperty('custom-bean-customer')}")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        kafkaTemplate.send("inbound-custom-bean-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}")
            .get(5, TimeUnit.SECONDS);

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
    void eventShouldBeReceivedWhenChannelModelIsDeployed() throws Exception {
        createTopic("test-new-customer");
        
        EventDeployment deployment = eventRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/kafkaEvent.event")
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/kafkaChannel.channel")
            .deploy();
    
        try {

            // Give time for the consumers to register properly in the groups
            // This is linked to the session timeout property for the consumers
            Thread.sleep(600);
    
            kafkaTemplate.send("test-new-customer", "{"
                + "    \"eventKey\": \"test\","
                + "    \"customer\": \"kermit\","
                + "    \"name\": \"Kermit the Frog\""
                + "}")
                .get(5, TimeUnit.SECONDS);
    
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
    void eventShouldBeReceivedWhenMultipleChannelDefinitionsAreRegistered() throws Exception {
        createTopic("test-multi-customer");

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("customer")
            .resourceName("customer.channel")
            .kafkaChannelAdapter("test-multi-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .fixedEventKey("customer")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("newCustomer")
            .resourceName("newCustomer.channel")
            .kafkaChannelAdapter("test-multi-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .fixedEventKey("newCustomer")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("customer")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("newCustomer")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .deploy();

        kafkaTemplate.send("test-multi-customer", "{"
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}")
            .get(5, TimeUnit.SECONDS);

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsExactlyInAnyOrder("customer", "newCustomer"));

        EventInstance eventInstance = (EventInstance) testEventConsumer.getEvent("customer").getEventObject();

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

        eventInstance = (EventInstance) testEventConsumer.getEvent("newCustomer").getEventObject();

        assertThat(eventInstance).isNotNull();
        assertThat(eventInstance.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit")
            );
        assertThat(eventInstance.getCorrelationParameterInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("customer", "kermit")
            );
    }

    @Test
    void eventShouldBeReceivedAfterChannelDefinitionIsRegistered() throws Exception {
        createTopic("test-customer");

        kafkaTemplate.send("test-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"kermit\","
            + "    \"name\": \"Kermit the Frog\""
            + "}")
            .get(5, TimeUnit.SECONDS);
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .correlationParameter("customer", EventPayloadTypes.STRING)
            .payload("name", EventPayloadTypes.STRING)
            .deploy();

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .kafkaChannelAdapter("test-customer")
            .property(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        kafkaTemplate.send("test-customer", "{"
            + "    \"eventKey\": \"test\","
            + "    \"customer\": \"fozzie\","
            + "    \"name\": \"Fozzie Bear\""
            + "}")
            .get(5, TimeUnit.SECONDS);

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
    void eventWithSimpleHeader() throws Exception {
        createTopic("test-customer");
        
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .payload("name", EventPayloadTypes.STRING)
            .header("testStringHeader", EventPayloadTypes.STRING)
            .deploy();

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .kafkaChannelAdapter("test-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();
        
        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        List<Header> headers = Arrays.asList(new RecordHeader("testStringHeader", "123".getBytes()));
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>("test-customer", 0, (Object) null, "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Kermit the Frog\""
                + "}", headers);
        kafkaTemplate.send(producerRecord);

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsExactlyInAnyOrder("test"));

        EventInstance kermitEvent = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(kermitEvent).isNotNull();
        assertThat(kermitEvent.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("name", "Kermit the Frog"),
                tuple("testStringHeader", "123")
            );
        assertThat(kermitEvent.getCorrelationParameterInstances()).isEmpty();
    }
    
    @Test
    void eventWithMultipleHeaders() throws Exception {
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .payload("name", EventPayloadTypes.STRING)
            .header("testStringHeader", EventPayloadTypes.STRING)
            .header("testLongHeader", EventPayloadTypes.LONG)
            .header("testIntHeader", EventPayloadTypes.INTEGER)
            .header("testBooleanHeader", EventPayloadTypes.BOOLEAN)
            .header("testDoubleHeader", EventPayloadTypes.DOUBLE)
            .deploy();

        eventRepositoryService.createInboundChannelModelBuilder()
            .key("testChannel")
            .resourceName("test.channel")
            .kafkaChannelAdapter("test-customer")
            .eventProcessingPipeline()
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("eventKey")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();
        
        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);
        
        Map<String, String> customerObj = new HashMap<>();
        customerObj.put("name", "John Doe");

        List<Header> headers = Arrays.asList(
                new RecordHeader("testStringHeader", "123".getBytes()),
                new RecordHeader("testLongHeader", "123".getBytes()),
                new RecordHeader("testIntHeader", "123".getBytes()),
                new RecordHeader("testBooleanHeader", "true".getBytes()),
                new RecordHeader("testDoubleHeader", "12.3".getBytes())
        );
        
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>("test-customer", 0, (Object) null, "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Kermit the Frog\""
                + "}", headers);
        kafkaTemplate.send(producerRecord);

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsExactlyInAnyOrder("test"));

        EventInstance kermitEvent = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(kermitEvent).isNotNull();
        assertThat(kermitEvent.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("name", "Kermit the Frog"),
                tuple("testStringHeader", "123"),
                tuple("testLongHeader", 123l),
                tuple("testIntHeader", 123),
                tuple("testBooleanHeader", true),
                tuple("testDoubleHeader", 12.3)
            );
        assertThat(kermitEvent.getCorrelationParameterInstances()).isEmpty();
    }

    @Test
    void eventShouldBeSendAfterOutboundChannelDefinitionIsRegistered() throws Exception {
        createTopic("outbound-customer");

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            consumer.subscribe(Collections.singleton("outbound-customer"));

            eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("customer")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

            eventRepositoryService.createOutboundChannelModelBuilder()
                .key("outboundCustomer")
                .resourceName("outboundCustomer.channel")
                .kafkaChannelAdapter("outbound-customer")
                .recordKey("customer")
                .eventProcessingPipeline()
                .jsonSerializer()
                .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));

            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Collections.singleton(new TopicPartition("outbound-customer", 0)));

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                .hasSize(1)
                .first()
                .isNotNull()
                .satisfies(record -> {
                    assertThat(record.key()).isEqualTo("customer");
                    assertThatJson(record.value())
                        .isEqualTo("{"
                            + "  customer: 'kermit',"
                            + "  name: 'Kermit the Frog'"
                            + "}");
                });
        }
    }

    @Test
    void eventShouldBeSendAfterOutboundChannelModelIsDeployed() {
        createTopic("outbound-customer");

        EventDeployment deployment = eventRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/kafkaOutboundEvent.event")
            .addClasspathResource("org/flowable/eventregistry/spring/test/deployment/kafkaOutboundChannel.channel")
            .deploy();


        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            consumer.subscribe(Collections.singleton("outbound-customer"));

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));
            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Collections.singleton(new TopicPartition("outbound-customer", 0)));

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                .hasSize(1)
                .first()
                .isNotNull()
                .satisfies(record -> {
                    assertThat(record.key()).isEqualTo("customer");
                    assertThatJson(record.value())
                        .isEqualTo("{"
                            + "  customer: 'kermit',"
                            + "  name: 'Kermit the Frog'"
                            + "}");
                });
        } finally {
            eventRepositoryService.deleteDeployment(deployment.getId());
        }

    }

    @Test
    void kafkaOutboundChannelShouldResolveTopicFromExpression() {
        createTopic("test-expression-customer");

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testExpression", "testClientExpression")) {
            consumer.subscribe(Collections.singleton("test-expression-customer"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .payload("name", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createOutboundChannelModelBuilder()
                    .key("outboundCustomer")
                    .resourceName("outboundCustomer.channel")
                    .kafkaChannelAdapter("${application.test.kafka-topic}")
                    .recordKey("customer")
                    .eventProcessingPipeline()
                    .jsonSerializer()
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));

            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Collections.singleton(new TopicPartition("test-expression-customer", 0)));

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThat(record.key()).isEqualTo("customer");
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
    }

    @Test
    void kafkaOutboundChannelShouldResolveTopicFromExpressionUsingEnvironmentAsBean() {
        createTopic("demo-test-expression-customer-environment");

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testExpressionBean", "testClientExpressionBean")) {
            consumer.subscribe(Collections.singleton("demo-test-expression-customer-environment"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .payload("name", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createOutboundChannelModelBuilder()
                    .key("outboundCustomer")
                    .resourceName("outboundCustomer.channel")
                    .kafkaChannelAdapter("demo-#{environment.getProperty('application.test.kafka-topic')}-environment")
                    .recordKey("customer")
                    .eventProcessingPipeline()
                    .jsonSerializer()
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));

            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Collections.singleton(new TopicPartition("demo-test-expression-customer-environment", 0)));

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThat(record.key()).isEqualTo("customer");
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
    }

    @Test
    void kafkaOutboundChannelShouldResolveTopicFromExpressionUsingCustomBean() {
        createTopic("outbound-test-custom-bean-customer");

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testExpressionBean", "testClientExpressionBean")) {
            consumer.subscribe(Collections.singleton("outbound-test-custom-bean-customer"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .payload("name", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createOutboundChannelModelBuilder()
                    .key("outboundCustomer")
                    .resourceName("outboundCustomer.channel")
                    .kafkaChannelAdapter("outbound-#{customPropertiesBean.getProperty('test-custom-bean-customer')}")
                    .recordKey("customer")
                    .eventProcessingPipeline()
                    .jsonSerializer()
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("outboundCustomer");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));

            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Collections.singleton(new TopicPartition("outbound-test-custom-bean-customer", 0)));

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThat(record.key()).isEqualTo("customer");
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
    }

    protected void createTopic(String topicName) {

        CreateTopicsResult topicsResult = adminClient.createTopics(Collections.singleton(new NewTopic(topicName, 1, (short) 1)));
        try {
            topicsResult.all().get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for topic creation", e);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                // If the topic already exists, then we ignore it
                throw new AssertionError("Failed to create topics", e);
            }
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out waiting for create topics results", e);
        }
        topicsToDelete.add(topicName);
    }
}
