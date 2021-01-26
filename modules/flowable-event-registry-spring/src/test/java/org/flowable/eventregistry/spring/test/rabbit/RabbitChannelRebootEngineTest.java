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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Filip Hrisafov
 */
@Tag("docker")
@Tag("rabbit")
public class RabbitChannelRebootEngineTest {

    @Test
    void springRabbitListenersShouldBeCorrectlyRegisteredWhenFlowableChannelsAreInTheDatabase() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(TestRabbitListener.class,
                EventRegistryRabbitConfiguration.class);

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

        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        rabbitTemplate.convertAndSend("test-customer", "{"
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

        TestRabbitListener firstTestRabbitListener = applicationContext.getBean(TestRabbitListener.class);

        rabbitTemplate.convertAndSend("testListener", "Test message");

        await("receive events @RabbitListener")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(firstTestRabbitListener.receivedMessages).containsExactlyInAnyOrder("Test message"));

        // Shut down the application
        applicationContext.close();

        // Create a new application context
        applicationContext = new AnnotationConfigApplicationContext(TestRabbitListener.class,
                EventRegistryRabbitConfiguration.class);

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

        rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        rabbitTemplate.convertAndSend("test-customer", "{"
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

        TestRabbitListener secondTestRabbitListener = applicationContext.getBean(TestRabbitListener.class);

        rabbitTemplate.convertAndSend("testListener", "Second test message");

        await("receive events @RabbitListener")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(secondTestRabbitListener.receivedMessages).containsExactlyInAnyOrder("Second test message"));

        List<EventDeployment> eventDeployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : eventDeployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Configuration
    static class TestRabbitListener implements DisposableBean {

        protected final List<String> receivedMessages = new ArrayList<>();
        protected final RabbitAdmin rabbitAdmin;

        TestRabbitListener(RabbitAdmin rabbitAdmin) {
            this.rabbitAdmin = rabbitAdmin;
        }

        @Override
        public void destroy() throws Exception {
            rabbitAdmin.deleteQueue("testListener");
            rabbitAdmin.deleteQueue("test-customer");
        }

        @Bean
        public Queue testListenerQueue() {
            return new Queue("testListener", false);
        }

        @Bean
        public Queue testCustomerQueue() {
            return new Queue("test-customer", false);
        }

        @RabbitListener(queues = "testListener")
        public void receiveMessage(Message message) {
            receivedMessages.add(new String(message.getBody(), StandardCharsets.UTF_8));
        }
    }
}
