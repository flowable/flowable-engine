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

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.flowable.eventregistry.spring.jms.JmsChannelModelProcessor;
import org.flowable.eventregistry.spring.test.config.EventRegistryEngineTestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author Filip Hrisafov
 */
@Configuration
@Import(EventRegistryEngineTestConfiguration.class)
@EnableJms
public class EventRegistryJmsConfiguration {

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
    public JmsChannelModelProcessor jmsChannelDefinitionProcessor(JmsListenerEndpointRegistry endpointRegistry, JmsOperations jmsOperations) {
        JmsChannelModelProcessor jmsChannelDeployer = new JmsChannelModelProcessor();
        jmsChannelDeployer.setEndpointRegistry(endpointRegistry);
        jmsChannelDeployer.setJmsOperations(jmsOperations);

        return jmsChannelDeployer;
    }
}
