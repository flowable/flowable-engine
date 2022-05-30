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
package org.flowable.eventregistry.integrationtest;

import java.time.Duration;
import java.util.Optional;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.flowable.eventregistry.model.JmsOutboundChannelModel;
import org.flowable.eventregistry.spring.jms.JmsChannelModelProcessor;
import org.flowable.eventregistry.spring.jms.JmsOperationsOutboundEventChannelAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.backoff.ExponentialBackOff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Configuration
@Import(BpmnWithEventRegistryTestConfiguration.class)
@EnableJms
public class ProcessWithDynamicChannelProcessorTestConfiguration {

    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    @ConditionalOnExpression("true")//this would be for example based on the execution environment of the application or on some mocking property
    @Bean
    public InternalFakeRequestProcessor internalService(ObjectMapper objectMapper, @Qualifier("internalJmsTemplate") JmsTemplate internalJmsTemplate) {
        return new InternalFakeRequestProcessor(objectMapper, internalJmsTemplate);
    }

    /**
     * to be used only for the JmsListeners of InternalFakeRequestProcessor, as well as the ones from the channel models
     * which are used to interact with the InternalFakeRequestProcessor
     */
    @ConditionalOnExpression("true")//this would be for example based on the execution environment of the application or on some mocking property
    @Bean
    public JmsListenerContainerFactory<?> testJmsListenerContainerFactory(ConnectionFactory connectionFactory,
            @Qualifier("internalServiceDynamicDestinationResolver") DestinationResolver internalServiceDynamicDestinationResolver) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        // configuration properties are Spring Boot defaults
        factory.setPubSubDomain(false);
        factory.setDestinationResolver(internalServiceDynamicDestinationResolver);
        factory.setSessionTransacted(true);
        factory.setAutoStartup(true);

        factory.setBackOff(new ExponentialBackOff());

        return factory;
    }

    @ConditionalOnExpression("true")//this would be for example based on the execution environment of the application or on some mocking property
    @Bean
    public DynamicDestinationResolver internalServiceDynamicDestinationResolver() {
        return new DynamicDestinationResolver() {

            @Override
            public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain) throws JMSException {
                if (destinationName.equals("EXT_QUEUE_IN")) {
                    return resolveDestinationName(session, "another-test-queue", pubSubDomain);
                } else if (destinationName.equals("EXT_QUEUE_OUT")) {
                    return resolveDestinationName(session, "yet-another-test-queue", pubSubDomain);
                }
                return super.resolveDestinationName(session, destinationName, pubSubDomain);
            }
        };
    }

    /**
     * to be used only for all other JmsListeners
     */
    @Bean
    public JmsListenerContainerFactory<?> defaultJmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        // configuration properties are Spring Boot defaults
        factory.setPubSubDomain(false);
        factory.setSessionTransacted(true);
        factory.setAutoStartup(true);
        factory.setReceiveTimeout(Duration.ofSeconds(1).toMillis());

        return factory;
    }

    @ConditionalOnExpression("true")//this would be for example based on the execution environment of the application or on some mocking property
    @Bean
    public JmsTemplate internalJmsTemplate(ConnectionFactory connectionFactory,
            @Qualifier("internalServiceDynamicDestinationResolver") DestinationResolver internalServiceDynamicDestinationResolver) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDestinationResolver(internalServiceDynamicDestinationResolver);

        template.setPubSubDomain(false);

        return template;
    }

    @Bean
    public JmsTemplate defaultJmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);

        template.setPubSubDomain(false);

        return template;
    }

    @Bean
    public CachingConnectionFactory cachingJmsConnectionFactory() {
        // configuration properties are Spring Boot defaults
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        activeMQConnectionFactory.setCloseTimeout((int) Duration.ofSeconds(15).toMillis());
        activeMQConnectionFactory.setNonBlockingRedelivery(false);
        activeMQConnectionFactory.setSendTimeout(0); // wait forever

        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
        cachingConnectionFactory.setCacheConsumers(false);
        cachingConnectionFactory.setCacheProducers(true);
        cachingConnectionFactory.setSessionCacheSize(1);

        return cachingConnectionFactory;
    }

    @Bean
    public JmsChannelModelProcessor testJmsChannelDefinitionProcessor(
            ObjectMapper objectMapper,
            JmsListenerEndpointRegistry endpointRegistry,
            @Qualifier("defaultJmsTemplate") JmsOperations jmsOperations,
            @Qualifier("internalJmsTemplate") Optional<JmsOperations> internalJmsTemplate,
            @Qualifier("defaultJmsListenerContainerFactory") JmsListenerContainerFactory<?> defaultJmsListenerContainerFactory,
            @Qualifier("testJmsListenerContainerFactory") Optional<JmsListenerContainerFactory<?>> testJmsListenerContainerFactory) {

        DynamicJmsChannelModelProcessor jmsChannelDeployer = new DynamicJmsChannelModelProcessor(
                objectMapper);
        jmsChannelDeployer.setEndpointRegistry(endpointRegistry);
        jmsChannelDeployer.setJmsOperations(jmsOperations);
        jmsChannelDeployer.setContainerFactory(defaultJmsListenerContainerFactory);

        testJmsListenerContainerFactory.ifPresent(jmsChannelDeployer::setCustomContainerFactory);
        internalJmsTemplate.ifPresent(jmsChannelDeployer::setCustomJmsOperations);

        return jmsChannelDeployer;
    }

    /**
     * mocks an external service in order to run the application in environments where this service is not available
     */
    private static final class InternalFakeRequestProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(InternalFakeRequestProcessor.class);
        private final ObjectMapper objectMapper;
        private final JmsTemplate internalJmsTemplate;

        public InternalFakeRequestProcessor(ObjectMapper objectMapper, JmsTemplate jmsTemplate) {
            this.objectMapper = objectMapper;
            this.internalJmsTemplate = jmsTemplate;
        }

        @JmsListener(destination = "EXT_QUEUE_OUT", containerFactory = "testJmsListenerContainerFactory")
        public void replyToRequest(Message request) throws JMSException, JsonProcessingException {
            Object content = internalJmsTemplate.getMessageConverter().fromMessage(request);
            ObjectNode requestNode = (ObjectNode) objectMapper.readTree((String) content);
            LOGGER.info("Received request on EXT_QUEUE_OUT: {}", requestNode);

            this.internalJmsTemplate.convertAndSend("EXT_QUEUE_IN",
                    "{"
                            + "    \"payload1\": \"" + requestNode.get("payload1").asText() + requestNode.get("payload2").asText() + "\","
                            + "    \"payload2\": " + (1001 + 3 * requestNode.get("payload2").asInt()) + ""
                            + "}", messageProcessor -> {
                        messageProcessor.setStringProperty("headerProperty1", request.getStringProperty("headerProperty1"));
                        messageProcessor.setStringProperty("headerProperty2", request.getStringProperty("headerProperty2"));
                        return messageProcessor;
                    }
            );
            LOGGER.info("Replied on EXT_QUEUE_IN");
        }
    }

    private static final class DynamicJmsChannelModelProcessor extends JmsChannelModelProcessor {

        private JmsListenerContainerFactory<?> customContainerFactory;
        private JmsOperations customJmsOperations;

        public DynamicJmsChannelModelProcessor(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public void setCustomContainerFactory(JmsListenerContainerFactory<?> customContainerFactory) {
            this.customContainerFactory = customContainerFactory;
        }

        public void setCustomJmsOperations(JmsOperations customJmsOperations) {
            this.customJmsOperations = customJmsOperations;
        }

        @Override
        protected JmsListenerEndpoint processInboundDefinition(String tenantId, EventRegistry eventRegistry, JmsInboundChannelModel jmsChannelModel) {
            JmsListenerEndpoint endpoint = createJmsListenerEndpoint(jmsChannelModel, tenantId, eventRegistry);
            if (jmsChannelModel.getDestination().contains("EXT_QUEUE") && customContainerFactory != null) {
                registerEndpoint(endpoint, customContainerFactory);
            } else {
                registerEndpoint(endpoint, null);
            }
            return endpoint;
        }

        @Override
        protected OutboundEventChannelAdapter createOutboundEventChannelAdapter(JmsOutboundChannelModel channelModel) {
            String destination = resolve(channelModel.getDestination());
            if (channelModel.getDestination().contains("EXT_QUEUE") && customJmsOperations != null) {
                return new JmsOperationsOutboundEventChannelAdapter(customJmsOperations, destination);
            } else {
                return new JmsOperationsOutboundEventChannelAdapter(jmsOperations, destination);
            }
        }
    }
}
