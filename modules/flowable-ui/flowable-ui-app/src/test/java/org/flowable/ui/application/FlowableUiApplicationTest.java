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
package org.flowable.ui.application;

import static org.assertj.core.api.Assertions.assertThat;

import javax.jms.ConnectionFactory;

import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest
@AutoConfigureMetrics
public class FlowableUiApplicationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    protected ApplicationContext applicationContext;

    @Test
    public void contextShouldLoad() {
        assertThat(environment.getPropertySources())
            .extracting(PropertySource::getName)
            .containsExactly(
                "configurationProperties",
                "Inlined Test Properties",
                "systemProperties",
                "systemEnvironment",
                "random",
                "Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'",
                "flowableDefaultConfig: [classpath:/flowable-default.properties]",
                "flowableTaskDefaultConfig: [classpath:/flowable-task-default.properties]",
                "flowable-liquibase-override",
                "Management Server"
            );

        assertThat(applicationContext.getBeanProvider(JmsTemplate.class).getIfAvailable())
            .as("JmsTemplate Bean")
            .isNull();

        assertThat(applicationContext.getBeanProvider(ConnectionFactory.class).getIfAvailable())
            .as("Jms ConnectionFactory Bean")
            .isNull();

        assertThat(applicationContext.getBeanProvider(org.springframework.amqp.rabbit.connection.ConnectionFactory.class).getIfAvailable())
            .as("Rabbit ConnectionFactory Bean")
            .isNull();

        assertThat(applicationContext.getBeanProvider(RabbitTemplate.class).getIfAvailable())
            .as("RabbitTemplate Bean")
            .isNull();

        assertThat(applicationContext.getBeanProvider(ConsumerFactory.class).getIfAvailable())
            .as("Kafka ConsumerFactory Bean")
            .isNull();

        assertThat(applicationContext.getBeanProvider(KafkaTemplate.class).getIfAvailable())
            .as("KafkaTemplate Bean")
            .isNull();

        assertThat(applicationContext.getBeansOfType(ChannelModelProcessor.class)).isEmpty();
    }
}
