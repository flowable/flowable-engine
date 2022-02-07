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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.spring.test.TestEventConsumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author Filip Hrisafov
 */
@Tag("docker")
@Tag("kafka")
public class KafkaChannelRebootEngineTest {

    @Test
    void springKafkaListenersShouldBeCorrectlyRegisteredWhenFlowableChannelsAreInTheDatabase() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(TestKafkaListener.class,
                EventRegistryKafkaConfiguration.class);

        EventRepositoryService eventRepositoryService = applicationContext.getBean(EventRepositoryService.class);
        // At the beginning there should be no events or channels
        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).isEmpty();
        assertThat(eventRepositoryService.createEventDefinitionQuery().list()).isEmpty();

        EventRegistry eventRegistry = applicationContext.getBean(EventRegistry.class);
        TestEventConsumer firstEventConsumer = new TestEventConsumer();
        eventRegistry.registerEventRegistryEventConsumer(firstEventConsumer);

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

        eventRepositoryService.createEventModelBuilder()
                .resourceName("testEvent.event")
                .key("test")
                .correlationParameter("customer", EventPayloadTypes.STRING)
                .payload("name", EventPayloadTypes.STRING)
                .deploy();

        KafkaTemplate kafkaTemplate = applicationContext.getBean(KafkaTemplate.class);
        kafkaTemplate.send("test-customer", "{"
                + "    \"eventKey\": \"test\","
                + "    \"customer\": \"kermit\","
                + "    \"name\": \"Kermit the Frog\""
                + "}");

        await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(firstEventConsumer.getEvents())
                        .extracting(EventRegistryEvent::getType)
                        .containsExactlyInAnyOrder("test"));

        EventInstance eventInstance = (EventInstance) firstEventConsumer.getEvents().get(0).getEventObject();

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

        TestKafkaListener firstTestKafkaListener = applicationContext.getBean(TestKafkaListener.class);

        kafkaTemplate.send("testListener", "Test message");

        await("receive events @KafkaListener")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(firstTestKafkaListener.receivedMessages).containsExactlyInAnyOrder("Test message"));

        // Shut down the application
        applicationContext.close();

        // Create a new application context
        applicationContext = new AnnotationConfigApplicationContext(TestKafkaListener.class,
                EventRegistryKafkaConfiguration.class);

        eventRepositoryService = applicationContext.getBean(EventRepositoryService.class);

        // At the second start there should be the events and channels from the previous run
        assertThat(eventRepositoryService.createChannelDefinitionQuery().list())
                .extracting(ChannelDefinition::getKey)
                .containsExactlyInAnyOrder("testChannel");
        assertThat(eventRepositoryService.createEventDefinitionQuery().list())
                .extracting(EventDefinition::getKey)
                .containsExactlyInAnyOrder("test");

        eventRegistry = applicationContext.getBean(EventRegistry.class);
        TestEventConsumer secondEventConsumer = new TestEventConsumer();
        eventRegistry.registerEventRegistryEventConsumer(secondEventConsumer);

        kafkaTemplate = applicationContext.getBean(KafkaTemplate.class);
        kafkaTemplate.send("test-customer", "{"
                + "    \"eventKey\": \"test\","
                + "    \"customer\": \"fozzie\","
                + "    \"name\": \"Fozzie Bear\""
                + "}");

        await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(secondEventConsumer.getEvents())
                        .extracting(EventRegistryEvent::getType)
                        .containsExactlyInAnyOrder("test"));

        eventInstance = (EventInstance) secondEventConsumer.getEvents().get(0).getEventObject();

        assertThat(eventInstance).isNotNull();
        assertThat(eventInstance.getPayloadInstances())
                .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
                .containsExactlyInAnyOrder(
                        tuple("customer", "fozzie"),
                        tuple("name", "Fozzie Bear")
                );
        assertThat(eventInstance.getCorrelationParameterInstances())
                .extracting(EventPayloadInstance::getDefinitionName, EventPayloadInstance::getValue)
                .containsExactlyInAnyOrder(
                        tuple("customer", "fozzie")
                );

        TestKafkaListener secondTestKafkaListener = applicationContext.getBean(TestKafkaListener.class);

        kafkaTemplate.send("testListener", "Second test message");

        await("receive events @KafkaListener")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(secondTestKafkaListener.receivedMessages).containsExactlyInAnyOrder("Second test message"));

        List<EventDeployment> eventDeployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : eventDeployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Configuration
    static class TestKafkaListener implements DisposableBean {

        protected final List<String> receivedMessages = new ArrayList<>();
        protected final AdminClient adminClient;
        protected final ConsumerFactory<Object, Object> consumerFactory;

        TestKafkaListener(AdminClient adminClient, ConsumerFactory<Object, Object> consumerFactory) {
            this.adminClient = adminClient;
            this.consumerFactory = consumerFactory;
        }

        @Override
        public void destroy() throws Exception {
            Map<TopicPartition, RecordsToDelete> recordsToDelete = new HashMap<>();
            Map<String, TopicDescription> topicDescriptions = adminClient.describeTopics(Arrays.asList("testListener", "test-customer"))
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

        @Bean
        public KafkaAdmin.NewTopics newTopics() {
            return new KafkaAdmin.NewTopics(
                    new NewTopic("testListener", 1, (short) 1),
                    new NewTopic("test-customer", 1, (short) 1)

            );
        }

        @KafkaListener(topics = "testListener", groupId = "test")
        public void receiveMessage(@Payload String message) {
            receivedMessages.add(message);
        }
    }
}
