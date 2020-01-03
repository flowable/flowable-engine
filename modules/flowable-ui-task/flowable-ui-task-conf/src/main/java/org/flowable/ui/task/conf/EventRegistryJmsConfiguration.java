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
package org.flowable.ui.task.conf;

import java.time.Duration;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FlowableEventRegistryProperties.class)
@ConditionalOnProperty(prefix = "flowable.eventregistry", name = "jms-enabled", havingValue = "true")
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
    public CachingConnectionFactory cachingJmsConnectionFactory(FlowableEventRegistryProperties eventRegistryProperties) {
        // configuration properties are Spring Boot defaults
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(eventRegistryProperties.getJmsConnectionUrl());
        activeMQConnectionFactory.setCloseTimeout((int) Duration.ofSeconds(15).toMillis());
        activeMQConnectionFactory.setNonBlockingRedelivery(false);
        activeMQConnectionFactory.setSendTimeout(0); // wait forever

        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
        cachingConnectionFactory.setCacheConsumers(false);
        cachingConnectionFactory.setCacheProducers(true);
        cachingConnectionFactory.setSessionCacheSize(1);

        return cachingConnectionFactory;
    }
}
