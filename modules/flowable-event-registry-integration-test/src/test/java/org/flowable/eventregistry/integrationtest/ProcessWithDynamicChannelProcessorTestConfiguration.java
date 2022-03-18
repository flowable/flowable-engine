package org.flowable.eventregistry.integrationtest;

import java.time.Duration;
import java.util.Optional;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.flowable.eventregistry.spring.jms.JmsChannelModelProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Import(BpmnWithEventRegistryTestConfiguration.class)
@EnableJms
public class ProcessWithDynamicChannelProcessorTestConfiguration {

    /*private class OnInternalRequestProcessorCondition implements Condition {

        public OnInternalRequestProcessorCondition() {
        }

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return true;
        }
    }*/
    
    @ConditionalOnExpression("true")//@Conditional(OnInternalRequestProcessorCondition.class)
    @Bean
    public InternalFakeRequestProcessor internalService(JmsTemplate jmsTemplate) {
        return new InternalFakeRequestProcessor(jmsTemplate);
    }

    /**
     * to be used only for the JmsListeners of InternalFakeRequestProcessor, as well as the ones from the channel models
     * which are used to interact with the InternalFakeRequestProcessor
     */
    @ConditionalOnExpression("true")//@Conditional(OnInternalRequestProcessorCondition.class)
    @Bean
    public JmsListenerContainerFactory<?> testJmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        // configuration properties are Spring Boot defaults
        factory.setPubSubDomain(false);
        factory.setDestinationResolver(new DynamicDestinationResolver() {

            @Override
            public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain) throws JMSException {
                if (destinationName.equals("EXT_QUEUE_IN")) {
                    return resolveDestinationName(session, "another-test-queue", pubSubDomain);
                } else if (destinationName.equals("EXT_QUEUE_OUT")) {
                    return resolveDestinationName(session, "yet-another-test-queue", pubSubDomain);
                }
                return super.resolveDestinationName(session, destinationName, pubSubDomain);
            }
        });
        factory.setSessionTransacted(true);
        factory.setAutoStartup(true);

        return factory;
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

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
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
            JmsListenerEndpointRegistry endpointRegistry,
            JmsOperations jmsOperations,
            ObjectMapper objectMapper,
            @Qualifier("defaultJmsListenerContainerFactory") JmsListenerContainerFactory<?> defaultJmsListenerContainerFactory,
            @Qualifier("testJmsListenerContainerFactory") Optional<JmsListenerContainerFactory<?>> testJmsListenerContainerFactory) {

        DynamicJmsChannelModelProcessor jmsChannelDeployer = new DynamicJmsChannelModelProcessor(
                objectMapper);
        jmsChannelDeployer.setEndpointRegistry(endpointRegistry);
        jmsChannelDeployer.setJmsOperations(jmsOperations);
        jmsChannelDeployer.setContainerFactory(defaultJmsListenerContainerFactory);

        testJmsListenerContainerFactory.ifPresent(jmsChannelDeployer::setCustomContainerFactory);

        return jmsChannelDeployer;
    }

    /**
     * mocks an external service in order to run the application in environments where this service is not available
     */
    private static final class InternalFakeRequestProcessor {

        private final JmsTemplate jmsTemplate;

        public InternalFakeRequestProcessor(JmsTemplate jmsTemplate) {
            this.jmsTemplate = jmsTemplate;
        }

        @JmsListener(destination = "EXT_QUEUE_OUT", containerFactory = "testJmsListenerContainerFactory")
        public void replyToRequest(Object request) {
            ObjectNode requestNode = (ObjectNode) request;
            this.jmsTemplate.convertAndSend("EXT_QUEUE_IN", ((ObjectNode) new JsonMapper().createObjectNode().setAll(requestNode))
                    .put("value1", requestNode.get("value1").textValue() + requestNode.get("value2").textValue())
                    .put("value2", "your-fake-reply")
            );
        }
    }
    
    private static final class DynamicJmsChannelModelProcessor extends JmsChannelModelProcessor {

        private JmsListenerContainerFactory<?> customContainerFactory;

        public DynamicJmsChannelModelProcessor(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        public void setCustomContainerFactory(JmsListenerContainerFactory<?> customContainerFactory) {
            this.customContainerFactory = customContainerFactory;
        }

        @Override
        protected JmsListenerEndpoint processInboundDefinition(String tenantId, EventRegistry eventRegistry, JmsInboundChannelModel jmsChannelModel) {
            JmsListenerEndpoint endpoint = createJmsListenerEndpoint(jmsChannelModel, tenantId, eventRegistry);
            if(jmsChannelModel.getDestination().contains("EXT_QUEUE") && customContainerFactory != null) {
                registerEndpoint(endpoint, customContainerFactory);
            } else {
                registerEndpoint(endpoint, null);
            }
            return endpoint;
        }
    }
}
