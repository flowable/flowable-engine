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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.flowable.eventregistry.spring.kafka.KafkaMessageKeyProvider;
import org.flowable.eventregistry.spring.kafka.KafkaPartitionProvider;
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
        "application.test.kafka-topic=test-expression-customer",
        "application.test.kafka-partition1=0-2",
        "application.test.kafka-partition2=3-4",
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

    @Autowired
    protected TestKafkaMessageKeyProvider kafkaMessageKeyProvider;

    @Autowired
    protected TestKafkaPartitionProvider kafkaPartitionProvider;

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
        kafkaPartitionProvider.clear();
        kafkaMessageKeyProvider.clear();

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
    void eventWithConsumerRecordInformation() throws Exception {
        createTopic("test-customer-multi-partition", 2);
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .payload("name", EventPayloadTypes.STRING)
            .payload("receivedTopic", EventPayloadTypes.STRING)
            .payload("receivedOffset", EventPayloadTypes.LONG)
            .payload("receivedPartition", EventPayloadTypes.INTEGER)
            .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/consumerRecordInformationToEventKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>("test-customer-multi-partition", 0, null, "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Kermit the Frog 1\""
                + "}");
        kafkaTemplate.send(producerRecord).get(2, TimeUnit.SECONDS);

        producerRecord = new ProducerRecord<>("test-customer-multi-partition", 1, null, "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Kermit the Frog 2\""
                + "}");
        kafkaTemplate.send(producerRecord).get(2, TimeUnit.SECONDS);

        producerRecord = new ProducerRecord<>("test-customer-multi-partition", 0, null, "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Fozzie the Bear 1\""
                + "}");
        kafkaTemplate.send(producerRecord).get(2, TimeUnit.SECONDS);

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .hasSize(3));

        EventInstance event = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(event).isNotNull();
        assertThat(event.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("name", "Kermit the Frog 1"),
                tuple("receivedTopic", "test-customer-multi-partition"),
                tuple("receivedPartition", 0),
                tuple("receivedOffset", 0L)
            );

        event = (EventInstance) testEventConsumer.getEvents().get(1).getEventObject();

        assertThat(event).isNotNull();
        assertThat(event.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("name", "Kermit the Frog 2"),
                tuple("receivedTopic", "test-customer-multi-partition"),
                tuple("receivedPartition", 1),
                tuple("receivedOffset", 0L)
            );

        event = (EventInstance) testEventConsumer.getEvents().get(2).getEventObject();

        assertThat(event).isNotNull();
        assertThat(event.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("name", "Fozzie the Bear 1"),
                tuple("receivedTopic", "test-customer-multi-partition"),
                tuple("receivedPartition", 0),
                tuple("receivedOffset", 1L)
            );
    }

    @Test
    void eventWithConsumerRecordInformationAndHeader() throws Exception {
        createTopic("test-customer-header-and-consumer");
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .payload("name", EventPayloadTypes.STRING)
            .payload("topic", EventPayloadTypes.STRING)
            .payload("offset", EventPayloadTypes.LONG)
            .payload("partition", EventPayloadTypes.INTEGER)
            .header("testHeader", EventPayloadTypes.STRING)
            .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/consumerRecordAndHeaderInformationToEventKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>("test-customer-header-and-consumer", 0, (Object) null, "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Kermit the Frog\""
                + "}", Collections.singleton(new RecordHeader("testHeader", "Kermit header".getBytes(StandardCharsets.UTF_8))));
        kafkaTemplate.send(producerRecord).get(2, TimeUnit.SECONDS);

        producerRecord = new ProducerRecord<>("test-customer-header-and-consumer", 0, (Object) null, "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Fozzie the Bear\""
                + "}", Collections.singleton(new RecordHeader("testHeader", "Fozzie header".getBytes(StandardCharsets.UTF_8))));
        kafkaTemplate.send(producerRecord).get(2, TimeUnit.SECONDS);

        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .hasSize(2));

        EventInstance event = (EventInstance) testEventConsumer.getEvents().get(0).getEventObject();

        assertThat(event).isNotNull();
        assertThat(event.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("name", "Kermit the Frog"),
                tuple("testHeader", "Kermit header"),
                tuple("topic", "test-customer-header-and-consumer"),
                tuple("offset", 0L)
            );

        event = (EventInstance) testEventConsumer.getEvents().get(1).getEventObject();

        assertThat(event).isNotNull();
        assertThat(event.getPayloadInstances())
            .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
            .containsExactlyInAnyOrder(
                tuple("name", "Fozzie the Bear"),
                tuple("testHeader", "Fozzie header"),
                tuple("topic", "test-customer-header-and-consumer"),
                tuple("offset", 1L)
            );
    }

    @Test
    void differentConsumersListenerOnDifferentPartitions() throws Exception {
        createTopic("test-customer-split-partitions", 8);
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .payload("name", EventPayloadTypes.STRING)
            .payload("topic", EventPayloadTypes.STRING)
            .payload("partition", EventPayloadTypes.INTEGER)
            .deploy();

        EventDeployment deployment = eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/customTopicPartitonsKafkaPart1.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        String kafkaEvent = "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Kermit the Frog\""
                + "}";
        // Send to partitions 0-4, 6-7
        kafkaTemplate.send("test-customer-split-partitions", 0,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions", 1,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions", 2,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions", 3,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions", 4,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions", 6,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions", 7,  null, kafkaEvent).get(2, TimeUnit.SECONDS);

        // We should only receive the events that were send to partitions 0-3, 6-7
        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .hasSize(6));

        eventRepositoryService.deleteDeployment(deployment.getId());

        assertThat(testEventConsumer.getEventInstancePayloadValues("partition"))
                .containsExactlyInAnyOrder(0, 1, 2, 4, 6, 7);
        testEventConsumer.clear();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/customTopicPartitonsKafkaPart2.channel")
                .deploy();


        // Send to partition 5
        kafkaTemplate.send("test-customer-split-partitions", 5,  null, kafkaEvent).get(2, TimeUnit.SECONDS);

        // The rest of the events should be received 3 and 5 using the second channel
        await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                        .extracting(EventRegistryEvent::getType)
                        .hasSize(2));

        assertThat(testEventConsumer.getEventInstancePayloadValues("partition"))
                .containsExactlyInAnyOrder(3, 5);
    }

    @Test
    void differentConsumersListenerOnDifferentPartitionsUsingExpression() throws Exception {
        createTopic("test-customer-split-partitions-expression", 5);
        eventRepositoryService.createEventModelBuilder()
            .resourceName("testEvent.event")
            .key("test")
            .payload("name", EventPayloadTypes.STRING)
            .payload("topic", EventPayloadTypes.STRING)
            .payload("partition", EventPayloadTypes.INTEGER)
            .deploy();

        EventDeployment deployment = eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/customTopicPartitonsUsingExpression.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        String kafkaEvent = "{"
                + "    \"eventKey\": \"test\","
                + "    \"name\": \"Kermit the Frog\""
                + "}";
        // Send to partitions 0-4
        kafkaTemplate.send("test-customer-split-partitions-expression", 0,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions-expression", 1,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions-expression", 2,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions-expression", 3,  null, kafkaEvent).get(2, TimeUnit.SECONDS);
        kafkaTemplate.send("test-customer-split-partitions-expression", 4,  null, kafkaEvent).get(2, TimeUnit.SECONDS);

        // We should only receive the events that were send to partitions 3-4
        await("receive events")
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted(() -> assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .hasSize(2));

        eventRepositoryService.deleteDeployment(deployment.getId());

        assertThat(testEventConsumer.getEventInstancePayloadValues("partition"))
                .containsExactlyInAnyOrder(3, 4);
    }

    @Test
    void eventShouldBeReceivedMultipleTimesAfterAnExceptionIsThrown() throws Exception {
        createTopic("test-throwing-topic");
        AtomicInteger numberOfExceptionsToThrow = new AtomicInteger(5);
        testEventConsumer.setEventConsumer(event -> {
            if (numberOfExceptionsToThrow.decrementAndGet() >= 0) {
                throw new RuntimeException("Failed to receive event " + event.getType());
            }
        });

        eventRepositoryService.createInboundChannelModelBuilder()
                .key("newCustomerChannel")
                .resourceName("customer.channel")
                .kafkaChannelAdapter("test-throwing-topic")
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

        kafkaTemplate.send("test-throwing-topic", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(numberOfExceptionsToThrow).hasNegativeValue());

        // The test event consumer will receive 6 events, since it throws an exception
        assertThat(testEventConsumer.getEvents())
                .extracting(EventRegistryEvent::getType)
                .containsOnly("test")
                .hasSize(6);

    }

    @Test
    void channelShouldUseBlockingRetryConfiguration() throws Exception {
        createTopic("blocking-retry-test");

        AtomicInteger numberOfExceptionsToThrow = new AtomicInteger(3);
        testEventConsumer.setEventConsumer(event -> {
            if (numberOfExceptionsToThrow.decrementAndGet() >= 0) {
                throw new RuntimeException("Failed to receive event " + event.getType());
            }
        });

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/blockingRetryKafka.channel")
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

        kafkaTemplate.send("blocking-retry-test", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("blocking-retry-test", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEvents()).extracting(EventRegistryEvent::getType).hasSize(4));

        // The test event consumer will receive 4 events, since it throws an exception
        // The first 3 are kermit (since the consumer throws an exception), and the last one is fozzie

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactly(
                        "kermit", "kermit", "kermit",
                        "fozzie"
                );

        assertThat(numberOfExceptionsToThrow).hasValue(-1);
    }

    @Test
    void channelShouldUseBlockingRetryConfigurationWithDeadLetterSuffix() throws Exception {
        createTopic("blocking-retry-dlt-test");
        createTopic("blocking-retry-dlt-test-dlt");
        AtomicInteger numberOfExceptionsToThrow = new AtomicInteger(3);
        testEventConsumer.setEventConsumer(event -> {
            if (numberOfExceptionsToThrow.decrementAndGet() >= 0) {
                throw new RuntimeException("Failed to receive event " + event.getType());
            }
        });

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/blockingRetryDeadLetterSuffixKafka.channel")
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

        kafkaTemplate.send("blocking-retry-dlt-test", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("blocking-retry-dlt-test", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEvents()).extracting(EventRegistryEvent::getType).hasSize(4));

        // The test event consumer will receive 4 events, since it throws an exception
        // The first 3 are kermit (since the consumer throws an exception), and the last one is fozzie

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactly(
                        "kermit", "kermit", "kermit",
                        "fozzie"
                );

        assertThat(numberOfExceptionsToThrow).hasNegativeValue();

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            // The first event should land in the dead letter topic
            consumer.subscribe(Collections.singleton("blocking-retry-dlt-test-dlt"));
            consumer.poll(Duration.ofSeconds(1));
            consumer.seekToBeginning(consumer.assignment());

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }

    }

    @Test
    void channelShouldUseNonBlockingRetryConfigurationWithRetryTopicAndDeadLetterSuffix() throws Exception {
        createTopic("non-blocking-retry-test");
        createTopic("non-blocking-retry-test-retry");
        createTopic("non-blocking-retry-test-dlt");
        AtomicInteger numberOfTimesFozzieArrived = new AtomicInteger(0);
        testEventConsumer.setEventConsumer(event -> {
            EventInstance eventInstance = (EventInstance) event.getEventObject();
            Object customer = eventInstance.getCorrelationParameterInstances()
                    .stream()
                    .filter(pi -> "customer".equals(pi.getDefinitionName()))
                    .map(EventPayloadInstance::getValue)
                    .findAny()
                    .orElse(null);

            if ("kermit".equals(customer)) {
                throw new RuntimeException("Cannot receive " + customer);
            } else if ("fozzie".equals(customer)) {
                if (numberOfTimesFozzieArrived.incrementAndGet() < 2) {
                    throw new RuntimeException("Cannot receive " + customer);
                }
            }
        });

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/nonBlockingRetryAndDeadLetterSuffixKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        kafkaTemplate.send("non-blocking-retry-test", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("non-blocking-retry-test", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEventInstancePayloadValues("customer")).hasSize(5));

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactlyInAnyOrder(
                        "kermit", "kermit", "kermit",
                        "fozzie", "fozzie"
                );

        assertThat(numberOfTimesFozzieArrived).hasValue(2);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            // The first event should land in the dead letter topic
            consumer.subscribe(Collections.singleton("non-blocking-retry-test-dlt"));
            consumer.poll(Duration.ofSeconds(1));
            consumer.seekToBeginning(consumer.assignment());

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }

    }

    @Test
    void exponentialBackOffRetry() throws Exception {
        createTopic("exponential-backoff");
        createTopic("exponential-backoff-retry-topic-0");
        createTopic("exponential-backoff-retry-topic-1");
        createTopic("exponential-backoff-dlt-topic");
        AtomicInteger numberOfTimesFozzieArrived = new AtomicInteger(0);
        testEventConsumer.setEventConsumer(event -> {
            EventInstance eventInstance = (EventInstance) event.getEventObject();
            Object customer = eventInstance.getCorrelationParameterInstances()
                    .stream()
                    .filter(pi -> "customer".equals(pi.getDefinitionName()))
                    .map(EventPayloadInstance::getValue)
                    .findAny()
                    .orElse(null);

            if ("kermit".equals(customer)) {
                throw new RuntimeException("Cannot receive " + customer);
            } else if ("fozzie".equals(customer)) {
                if (numberOfTimesFozzieArrived.incrementAndGet() < 2) {
                    throw new RuntimeException("Cannot receive " + customer);
                }
            }
        });

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/nonBlockingExponentialBackOffRetryKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        kafkaTemplate.send("exponential-backoff", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("exponential-backoff", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEventInstancePayloadValues("customer")).hasSize(5));

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactlyInAnyOrder(
                        "kermit", "kermit", "kermit",
                        "fozzie", "fozzie"
                );

        assertThat(numberOfTimesFozzieArrived).hasValue(2);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            // The first event should land in the dead letter topic
            consumer.subscribe(Arrays.asList(
                    "exponential-backoff-retry-topic-0",
                    "exponential-backoff-retry-topic-1",
                    "exponential-backoff-dlt-topic"
            ));
            consumer.poll(Duration.ofSeconds(1));
            consumer.seekToBeginning(consumer.assignment());

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records.records("exponential-backoff-retry-topic-0"))
                    .satisfiesExactly(
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'kermit',"
                                                + "  name: 'Kermit the Frog'"
                                                + "}");
                            },
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'fozzie',"
                                                + "  name: 'Fozzie the Bear'"
                                                + "}");
                            }
                    );

            assertThat(records.records("exponential-backoff-retry-topic-1"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });

            assertThat(records.records("exponential-backoff-dlt-topic"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
    }

    @Test
    void exponentialBackOffRetryWithDelaySuffixing() throws Exception {
        createTopic("exponential-backoff-delay");
        createTopic("exponential-backoff-delay-retry-topic-100");
        createTopic("exponential-backoff-delay-dlt-topic");

        AtomicInteger numberOfTimesFozzieArrived = new AtomicInteger(0);
        testEventConsumer.setEventConsumer(event -> {
            EventInstance eventInstance = (EventInstance) event.getEventObject();
            Object customer = eventInstance.getCorrelationParameterInstances()
                    .stream()
                    .filter(pi -> "customer".equals(pi.getDefinitionName()))
                    .map(EventPayloadInstance::getValue)
                    .findAny()
                    .orElse(null);

            if ("kermit".equals(customer)) {
                throw new RuntimeException("Cannot receive " + customer);
            } else if ("fozzie".equals(customer)) {
                if (numberOfTimesFozzieArrived.incrementAndGet() < 2) {
                    throw new RuntimeException("Cannot receive " + customer);
                }
            }
        });

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/nonBlockingExponentialBackOffDelaySuffixingRetryKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        kafkaTemplate.send("exponential-backoff-delay", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("exponential-backoff-delay", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEventInstancePayloadValues("customer")).hasSize(5));

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactlyInAnyOrder(
                        "kermit", "kermit", "kermit",
                        "fozzie", "fozzie"
                );

        assertThat(numberOfTimesFozzieArrived).hasValue(2);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            // The first event should land in the dead letter topic
            consumer.subscribe(Arrays.asList(
                    "exponential-backoff-delay-retry-topic-100",
                    "exponential-backoff-delay-retry-topic-200",
                    "exponential-backoff-delay-dlt-topic"
            ));
            consumer.poll(Duration.ofSeconds(1));
            consumer.seekToBeginning(consumer.assignment());

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records.records("exponential-backoff-delay-retry-topic-100"))
                    .satisfiesExactly(
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'kermit',"
                                                + "  name: 'Kermit the Frog'"
                                                + "}");
                            },
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'fozzie',"
                                                + "  name: 'Fozzie the Bear'"
                                                + "}");
                            }
                    );

            assertThat(records.records("exponential-backoff-delay-retry-topic-200"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });

            assertThat(records.records("exponential-backoff-delay-dlt-topic"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
    }

    @Test
    void exponentialRandomBackOffRetry() throws Exception {
        createTopic("random-exponential-backoff");

        AtomicInteger numberOfTimesFozzieArrived = new AtomicInteger(0);
        testEventConsumer.setEventConsumer(event -> {
            EventInstance eventInstance = (EventInstance) event.getEventObject();
            Object customer = eventInstance.getCorrelationParameterInstances()
                    .stream()
                    .filter(pi -> "customer".equals(pi.getDefinitionName()))
                    .map(EventPayloadInstance::getValue)
                    .findAny()
                    .orElse(null);

            if ("kermit".equals(customer)) {
                throw new RuntimeException("Cannot receive " + customer);
            } else if ("fozzie".equals(customer)) {
                if (numberOfTimesFozzieArrived.incrementAndGet() < 2) {
                    throw new RuntimeException("Cannot receive " + customer);
                }
            }
        });

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/nonBlockingExponentialRandomBackOffRetryKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        kafkaTemplate.send("random-exponential-backoff", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("random-exponential-backoff", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEventInstancePayloadValues("customer")).hasSize(6));

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactlyInAnyOrder(
                        "kermit", "kermit", "kermit", "kermit",
                        "fozzie", "fozzie"
                );

        assertThat(numberOfTimesFozzieArrived).hasValue(2);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            // The first event should land in the dead letter topic
            consumer.subscribe(Arrays.asList(
                    "random-exponential-backoff-retry-0",
                    "random-exponential-backoff-retry-1",
                    "random-exponential-backoff-retry-2",
                    "random-exponential-backoff-dlt"
            ));
            consumer.poll(Duration.ofSeconds(1));
            consumer.seekToBeginning(consumer.assignment());

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records.records("random-exponential-backoff-retry-0"))
                    .satisfiesExactly(
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'kermit',"
                                                + "  name: 'Kermit the Frog'"
                                                + "}");
                            },
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'fozzie',"
                                                + "  name: 'Fozzie the Bear'"
                                                + "}");
                            }
                    );

            assertThat(records.records("random-exponential-backoff-retry-1"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });

            assertThat(records.records("random-exponential-backoff-retry-2"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });

            assertThat(records.records("random-exponential-backoff-dlt"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
    }

    @Test
    void uniformRandomBackOffRetry() throws Exception {
        createTopic("uniform-random-backoff");

        AtomicInteger numberOfTimesFozzieArrived = new AtomicInteger(0);
        testEventConsumer.setEventConsumer(event -> {
            EventInstance eventInstance = (EventInstance) event.getEventObject();
            Object customer = eventInstance.getCorrelationParameterInstances()
                    .stream()
                    .filter(pi -> "customer".equals(pi.getDefinitionName()))
                    .map(EventPayloadInstance::getValue)
                    .findAny()
                    .orElse(null);

            if ("kermit".equals(customer)) {
                throw new RuntimeException("Cannot receive " + customer);
            } else if ("fozzie".equals(customer)) {
                if (numberOfTimesFozzieArrived.incrementAndGet() < 2) {
                    throw new RuntimeException("Cannot receive " + customer);
                }
            }
        });

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/nonBlockingUniformRandomBackOffRetryKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        kafkaTemplate.send("uniform-random-backoff", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("uniform-random-backoff", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEventInstancePayloadValues("customer")).hasSize(6));

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactlyInAnyOrder(
                        "kermit", "kermit", "kermit", "kermit",
                        "fozzie", "fozzie"
                );

        assertThat(numberOfTimesFozzieArrived).hasValue(2);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            // The first event should land in the dead letter topic
            consumer.subscribe(Arrays.asList(
                    "uniform-random-backoff-retry-0",
                    "uniform-random-backoff-retry-1",
                    "uniform-random-backoff-retry-2",
                    "uniform-random-backoff-dlt"
            ));
            consumer.poll(Duration.ofSeconds(1));
            consumer.seekToBeginning(consumer.assignment());

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records.records("uniform-random-backoff-retry-0"))
                    .satisfiesExactly(
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'kermit',"
                                                + "  name: 'Kermit the Frog'"
                                                + "}");
                            },
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'fozzie',"
                                                + "  name: 'Fozzie the Bear'"
                                                + "}");
                            }
                    );

            assertThat(records.records("uniform-random-backoff-retry-1"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });

            assertThat(records.records("uniform-random-backoff-retry-2"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });

            assertThat(records.records("uniform-random-backoff-dlt"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
    }

    @Test
    void fixedBackOffMultiTopicRetry() throws Exception {
        createTopic("fixed-backoff-multi");
        createTopic("fixed-backoff-multi-dlt-topic");
        AtomicInteger numberOfTimesFozzieArrived = new AtomicInteger(0);
        testEventConsumer.setEventConsumer(event -> {
            EventInstance eventInstance = (EventInstance) event.getEventObject();
            Object customer = eventInstance.getCorrelationParameterInstances()
                    .stream()
                    .filter(pi -> "customer".equals(pi.getDefinitionName()))
                    .map(EventPayloadInstance::getValue)
                    .findAny()
                    .orElse(null);

            if ("kermit".equals(customer)) {
                throw new RuntimeException("Cannot receive " + customer);
            } else if ("fozzie".equals(customer)) {
                if (numberOfTimesFozzieArrived.incrementAndGet() < 2) {
                    throw new RuntimeException("Cannot receive " + customer);
                }
            }
        });

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/nonBlockingFixedBackOffMultiTopicRetryKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        kafkaTemplate.send("fixed-backoff-multi", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("fixed-backoff-multi", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEventInstancePayloadValues("customer")).hasSize(5));

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactlyInAnyOrder(
                        "kermit", "kermit", "kermit",
                        "fozzie", "fozzie"
                );

        assertThat(numberOfTimesFozzieArrived).hasValue(2);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            // The first event should land in the dead letter topic
            consumer.subscribe(Arrays.asList(
                    "fixed-backoff-multi-retry-topic-0",
                    "fixed-backoff-multi-retry-topic-1",
                    "fixed-backoff-multi-dlt-topic"
            ));
            consumer.poll(Duration.ofSeconds(1));
            consumer.seekToBeginning(consumer.assignment());

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records.records("fixed-backoff-multi-retry-topic-0"))
                    .satisfiesExactly(
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'kermit',"
                                                + "  name: 'Kermit the Frog'"
                                                + "}");
                            },
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'fozzie',"
                                                + "  name: 'Fozzie the Bear'"
                                                + "}");
                            }
                    );

            assertThat(records.records("fixed-backoff-multi-retry-topic-1"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });

            assertThat(records.records("fixed-backoff-multi-dlt-topic"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
    }

    @Test
    void fixedBackOffNoAutoCreateTopic() {
        createTopic("fixed-backoff-multi");

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        assertThatThrownBy(() -> eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/nonBlockingFixedBackOffRetryNoAutoCreateTopicKafka.channel")
                .deploy())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Topic(s) [fixed-backoff-no-auto-create] is/are not present and missingTopicsFatal is true");
    }

    @Test
    void fixedBackOffRetry() throws Exception {
        createTopic("fixed-backoff");
        createTopic("fixed-backoff-dlt-topic");
        AtomicInteger numberOfTimesFozzieArrived = new AtomicInteger(0);
        testEventConsumer.setEventConsumer(event -> {
            EventInstance eventInstance = (EventInstance) event.getEventObject();
            Object customer = eventInstance.getCorrelationParameterInstances()
                    .stream()
                    .filter(pi -> "customer".equals(pi.getDefinitionName()))
                    .map(EventPayloadInstance::getValue)
                    .findAny()
                    .orElse(null);

            if ("kermit".equals(customer)) {
                throw new RuntimeException("Cannot receive " + customer);
            } else if ("fozzie".equals(customer)) {
                if (numberOfTimesFozzieArrived.incrementAndGet() < 2) {
                    throw new RuntimeException("Cannot receive " + customer);
                }
            }
        });

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/nonBlockingFixedBackOffRetryKafka.channel")
                .deploy();

        // Give time for the consumers to register properly in the groups
        // This is linked to the session timeout property for the consumers
        Thread.sleep(600);

        kafkaTemplate.send("fixed-backoff", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"kermit\","
                        + "    \"name\": \"Kermit the Frog\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        kafkaTemplate.send("fixed-backoff", "{"
                        + "    \"eventKey\": \"test\","
                        + "    \"customer\": \"fozzie\","
                        + "    \"name\": \"Fozzie the Bear\""
                        + "}")
                .get(5, TimeUnit.SECONDS);

        await("receive events")
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(testEventConsumer.getEventInstancePayloadValues("customer")).hasSize(5));

        assertThat(testEventConsumer.getEventInstancePayloadValues("customer"))
                .containsExactlyInAnyOrder(
                        "kermit", "kermit", "kermit",
                        "fozzie", "fozzie"
                );

        assertThat(numberOfTimesFozzieArrived).hasValue(2);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("test", "testClient")) {
            // The first event should land in the dead letter topic
            consumer.subscribe(Arrays.asList(
                    "fixed-backoff-retry-topic",
                    "fixed-backoff-dlt-topic"
            ));
            consumer.poll(Duration.ofSeconds(1));
            consumer.seekToBeginning(consumer.assignment());

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records.records("fixed-backoff-retry-topic"))
                    .satisfiesExactly(
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'kermit',"
                                                + "  name: 'Kermit the Frog'"
                                                + "}");
                            },
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'fozzie',"
                                                + "  name: 'Fozzie the Bear'"
                                                + "}");
                            },
                            record -> {
                                assertThatJson(record.value())
                                        .isEqualTo("{"
                                                + "  eventKey: 'test',"
                                                + "  customer: 'kermit',"
                                                + "  name: 'Kermit the Frog'"
                                                + "}");
                            }
                    );

            assertThat(records.records("fixed-backoff-dlt-topic"))
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  eventKey: 'test',"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
        }
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
    void eventShouldBeSendWithoutRecordKeyWhenRecordKeyIsEmptyString() {
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
                    .recordKey("")
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
                        assertThat(record.key()).isNull();
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                    });
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

    @Test
    void kafkaOutboundChannelShouldUseEventPayloadForPartition() {
        createTopic("test-custom-partition", 2);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testCustomPartition", "testClientCustomPartition")) {
            consumer.subscribe(Collections.singleton("test-custom-partition"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .payload("name", EventPayloadTypes.STRING)
                    .metaParameter("partition", EventPayloadTypes.INTEGER)
                    .deploy();

            eventRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/outboundCustomPartitionFromEvent.channel")
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("customPartition");
            EventPayload customer = new EventPayload("customer", EventPayloadTypes.STRING);
            EventPayload name = new EventPayload("name", EventPayloadTypes.STRING);
            EventPayload partition = new EventPayload("partition", EventPayloadTypes.STRING);
            partition.setMetaParameter(true);

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(customer, "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(name, "Kermit the Frog"));
            payloadInstances.add(new EventPayloadInstanceImpl(partition, "0"));

            EventInstance event = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Arrays.asList(
                    new TopicPartition("test-custom-partition", 0),
                    new TopicPartition("test-custom-partition", 1)
            ));

            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  customer: 'kermit',"
                                        + "  name: 'Kermit the Frog'"
                                        + "}");
                        assertThat(record.partition()).isEqualTo(0);
                    });

            payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(customer, "fozzie"));
            payloadInstances.add(new EventPayloadInstanceImpl(name, "Fozzie the Bear"));
            payloadInstances.add(new EventPayloadInstanceImpl(partition, 1));
            event = new EventInstanceImpl("customer", payloadInstances);

            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{"
                                        + "  customer: 'fozzie',"
                                        + "  name: 'Fozzie the Bear'"
                                        + "}");
                        assertThat(record.partition()).isEqualTo(1);
                    });
        }
    }

    @Test
    void kafkaOutboundChannelShouldUseDelegateExpressionForPartition() {
        createTopic("test-custom-partition-delegate-expression", 5);

        Map<String, Integer> partitionMap = new HashMap<>();
        partitionMap.put("fozzie", 2);
        partitionMap.put("kermit", 3);
        partitionMap.put("piggy", 4);
        partitionMap.put("gonzo", 1);

        kafkaPartitionProvider.setPartitionProvider(eventPayload -> {
            String customer = (String) eventPayload.getEventInstance().getPayloadInstances().stream()
                    .filter(instance -> instance.getDefinitionName().equals("customer")).map(
                            EventPayloadInstance::getValue).findFirst().orElse(null);
            return partitionMap.get(customer);
        });

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testCustomPartitionDelegateExpression",
                "testClientCustomPartitionDelegateExpression")) {
            consumer.subscribe(Collections.singleton("test-custom-partition-delegate-expression"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/outboundCustomPartitionDelegateExpression.channel")
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("customPartitionDelegateExpression");
            EventPayloadInstanceImpl customer = new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(customer);

            EventInstance event = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(IntStream.range(0, 5)
                    .mapToObj(partition -> new TopicPartition("test-custom-partition-delegate-expression", partition))
                    .collect(Collectors.toList()));

            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'kermit' }");
                        assertThat(record.partition()).isEqualTo(3);
                    });

            customer.setValue("fozzie");

            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'fozzie' }");
                        assertThat(record.partition()).isEqualTo(2);
                    });

            customer.setValue("piggy");
            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'piggy' }");
                        assertThat(record.partition()).isEqualTo(4);
                    });

            customer.setValue("gonzo");
            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'gonzo' }");
                        assertThat(record.partition()).isEqualTo(1);
                    });
        }
    }

    @Test
    void kafkaOutboundChannelShouldUseRoundRobinForPartition() {
        createTopic("test-custom-partition-round-robin", 5);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testCustomPartitionRoundRobin", "testClientCustomPartitionRoundRobin")) {
            consumer.subscribe(Collections.singleton("test-custom-partition-round-robin"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/outboundCustomRoundPartition.channel")
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("customPartitionRoundRobin");
            EventPayloadInstanceImpl customer = new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(customer);

            EventInstance event = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(IntStream.range(0, 5)
                    .mapToObj(partition -> new TopicPartition("test-custom-partition-round-robin", partition))
                    .collect(Collectors.toList()));

            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'kermit' }");
                        assertThat(record.partition()).isEqualTo(1);
                    });

            customer.setValue("fozzie");

            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'fozzie' }");
                        assertThat(record.partition()).isEqualTo(2);
                    });

            customer.setValue("piggy");
            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'piggy' }");
                        assertThat(record.partition()).isEqualTo(3);
                    });

            customer.setValue("gonzo");
            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'gonzo' }");
                        assertThat(record.partition()).isEqualTo(1);
                    });
        }
    }

    @Test
    void kafkaOutboundChannelShouldUseRoundRobinForPartitionWithExpression() {
        createTopic("test-custom-partition-round-robin-expression", 5);

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testCustomPartitionRoundRobin", "testClientCustomPartitionRoundRobin")) {
            consumer.subscribe(Collections.singleton("test-custom-partition-round-robin-expression"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/outboundCustomRoundPartitionExpression.channel")
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("customPartitionRoundRobin");
            EventPayloadInstanceImpl customer = new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(customer);

            EventInstance event = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(IntStream.range(0, 5)
                    .mapToObj(partition -> new TopicPartition("test-custom-partition-round-robin-expression", partition))
                    .collect(Collectors.toList()));

            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'kermit' }");
                        assertThat(record.partition()).isEqualTo(0);
                    });

            customer.setValue("fozzie");

            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'fozzie' }");
                        assertThat(record.partition()).isEqualTo(1);
                    });

            customer.setValue("piggy");
            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'piggy' }");
                        assertThat(record.partition()).isEqualTo(2);
                    });

            customer.setValue("gonzo");
            eventRegistry.sendEventOutbound(event, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThatJson(record.value())
                                .isEqualTo("{ customer: 'gonzo' }");
                        assertThat(record.partition()).isEqualTo(0);
                    });
        }
    }

    @Test
    void kafkaOutboundChannelShouldUseFixedValueKeyForMessageKey() {
        createTopic("test-fixed-value-message-key");

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testFixedValueMessageKey", "testClientFixedValueMessageKey")) {
            consumer.subscribe(Collections.singleton("test-fixed-value-message-key"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .payload("name", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/outboundCustomMessageKeyFixedValue.channel")
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("messageKey");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));

            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Collections.singleton(new TopicPartition("test-fixed-value-message-key", 0)));

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThat(record.key()).isEqualTo("myKey");
                    });
        }
    }

    @Test
    void kafkaOutboundChannelShouldUseEventFieldForMessageKey() {
        createTopic("test-event-field-message-key");

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testEventFieldMessageKey", "testClientEventFieldMessageKey")) {
            consumer.subscribe(Collections.singleton("test-event-field-message-key"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .payload("name", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/outboundCustomMessageKeyFromEvent.channel")
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("messageKeyEventField");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));

            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Collections.singleton(new TopicPartition("test-event-field-message-key", 0)));

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThat(record.key()).isEqualTo("kermit");
                    });
        }
    }

    @Test
    void kafkaOutboundChannelShouldDelegateExpressionForMessageKey() {
        createTopic("test-delegate-expression-message-key");

        kafkaMessageKeyProvider.setMessageKeyProvider(ignore -> "testKey");

        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer("testDelegateExpressionMessageKey", "testClientDelegateExpressionMessageKey")) {
            consumer.subscribe(Collections.singleton("test-delegate-expression-message-key"));

            eventRepositoryService.createEventModelBuilder()
                    .resourceName("testEvent.event")
                    .key("customer")
                    .correlationParameter("customer", EventPayloadTypes.STRING)
                    .payload("name", EventPayloadTypes.STRING)
                    .deploy();

            eventRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/eventregistry/spring/test/kafka/outboundCustomMessageKeyFromDelegateExpression.channel")
                    .deploy();

            ChannelModel channelModel = eventRepositoryService.getChannelModelByKey("messageKeyDelegateExpression");

            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("customer", EventPayloadTypes.STRING), "kermit"));
            payloadInstances.add(new EventPayloadInstanceImpl(new EventPayload("name", EventPayloadTypes.STRING), "Kermit the Frog"));

            EventInstance kermitEvent = new EventInstanceImpl("customer", payloadInstances);

            ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(2));
            assertThat(records).isEmpty();
            consumer.commitSync();
            consumer.seekToBeginning(Collections.singleton(new TopicPartition("test-delegate-expression-message-key", 0)));

            eventRegistry.sendEventOutbound(kermitEvent, Collections.singleton(channelModel));

            records = consumer.poll(Duration.ofSeconds(2));

            assertThat(records)
                    .hasSize(1)
                    .first()
                    .isNotNull()
                    .satisfies(record -> {
                        assertThat(record.key()).isEqualTo("testKey");
                    });
        }
    }

    protected void createTopic(String topicName) {
        createTopic(topicName, 1);
    }

    protected void createTopic(String topicName, int numPartitions) {

        CreateTopicsResult topicsResult = adminClient.createTopics(Collections.singleton(new NewTopic(topicName, numPartitions, (short) 1)));
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
