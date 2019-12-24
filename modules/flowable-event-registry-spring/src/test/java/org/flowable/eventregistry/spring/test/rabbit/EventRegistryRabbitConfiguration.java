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

import org.flowable.eventregistry.spring.rabbit.RabbitChannelDefinitionProcessor;
import org.flowable.eventregistry.spring.test.config.EventRegistryEngineTestConfiguration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@Import(EventRegistryEngineTestConfiguration.class)
@EnableRabbit
public class EventRegistryRabbitConfiguration {

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        return rabbitTemplate;
    }
    @Bean
    public CachingConnectionFactory rabbitConnectionFactory(RabbitMQContainer rabbitMQContainer) throws Exception {
        // configuration properties are Spring Boot defaults
        RabbitConnectionFactoryBean rabbitFactory = new RabbitConnectionFactoryBean();
        rabbitFactory.setPort(rabbitMQContainer.getAmqpPort());
        rabbitFactory.afterPropertiesSet();
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitFactory.getObject());
        return factory;
    }

    @Bean
    public RabbitChannelDefinitionProcessor rabbitChannelDefinitionProcessor(RabbitListenerEndpointRegistry endpointRegistry, RabbitOperations rabbitOperations) {
        RabbitChannelDefinitionProcessor rabbitChannelDefinitionProcessor = new RabbitChannelDefinitionProcessor();
        rabbitChannelDefinitionProcessor.setEndpointRegistry(endpointRegistry);
        rabbitChannelDefinitionProcessor.setRabbitOperations(rabbitOperations);
        return rabbitChannelDefinitionProcessor;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean(destroyMethod = "stop")
    public RabbitMQContainer rabbitMQContainer() {
        RabbitMQContainer container = new RabbitMQContainer();
        container.start();
        return container;
    }

}
