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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.jms.ConnectionFactory;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.flowable.eventregistry.spring.jms.JmsChannelModelProcessor;
import org.flowable.eventregistry.spring.test.config.EventRegistryEngineTestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
@Configuration
@Import(EventRegistryEngineTestConfiguration.class)
@EnableJms
public class EventRegistryJmsConfiguration {

    private static final AtomicInteger serverIdCounter = new AtomicInteger();

    protected final int serverId = serverIdCounter.getAndIncrement();

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
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
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);

        template.setPubSubDomain(false);

        return template;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public EmbeddedActiveMQ embeddedActiveMQ() {
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setSecurityEnabled(false);
        configuration.setPersistenceEnabled(false);
        TransportConfiguration transportConfiguration = new TransportConfiguration(InVMAcceptorFactory.class.getName(), generateTransportParameter());
        configuration.getAcceptorConfigurations().add(transportConfiguration);
        configuration.setClusterPassword("flowable");
        AddressSettings deadLetterSetting = new AddressSettings();
        deadLetterSetting.setDeadLetterAddress(SimpleString.of("ActiveMQ.DLQ"));
        deadLetterSetting.setAutoCreateDeadLetterResources(true);
        deadLetterSetting.setMaxDeliveryAttempts(3);
        configuration.addAddressSetting("#", deadLetterSetting);
        EmbeddedActiveMQ embeddedActiveMQ = new EmbeddedActiveMQ();
        embeddedActiveMQ.setConfiguration(configuration);

        return embeddedActiveMQ;
    }

    @Bean
    @DependsOn("embeddedActiveMQ")
    public CachingConnectionFactory cachingJmsConnectionFactory() {
        // configuration properties are Spring Boot defaults
        TransportConfiguration transportConfiguration = new TransportConfiguration(InVMConnectorFactory.class.getName(), generateTransportParameter());
        ServerLocator serverLocator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(serverLocator);

        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
        cachingConnectionFactory.setCacheConsumers(false);
        cachingConnectionFactory.setCacheProducers(true);
        cachingConnectionFactory.setSessionCacheSize(1);

        return cachingConnectionFactory;
    }

    protected Map<String, Object> generateTransportParameter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(TransportConstants.SERVER_ID_PROP_NAME, serverId);
        return parameters;
    }

    @Bean
    public JmsChannelModelProcessor jmsChannelDefinitionProcessor(JmsListenerEndpointRegistry endpointRegistry, 
            JmsOperations jmsOperations, ObjectMapper objectMapper) {
        
        JmsChannelModelProcessor jmsChannelDeployer = new JmsChannelModelProcessor(objectMapper);
        jmsChannelDeployer.setEndpointRegistry(endpointRegistry);
        jmsChannelDeployer.setJmsOperations(jmsOperations);

        return jmsChannelDeployer;
    }
}
