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

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.flowable.eventregistry.spring.kafka.KafkaChannelDefinitionProcessor;
import org.flowable.eventregistry.spring.test.config.EventRegistryEngineTestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.LoggingProducerListener;
import org.testcontainers.containers.KafkaContainer;

/**
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@Import(EventRegistryEngineTestConfiguration.class)
@EnableKafka
public class EventRegistryKafkaConfiguration {

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);

        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setMissingTopicsFatal(true);
        return factory;
    }

    @Bean
    public ConsumerFactory<?, ?> kafkaConsumerFactory(KafkaContainer kafkaContainer) {
        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 500);
        consumerProperties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 400);

        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    public ProducerFactory<?, ?> kafkaProducerFactory(KafkaContainer kafkaContainer) {
        Map<String, Object> producerProperties = new HashMap<>();
        producerProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    public KafkaTemplate<?, ?> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
        KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setProducerListener(new LoggingProducerListener<>());

        return kafkaTemplate;
    }

    @Bean
    public KafkaChannelDefinitionProcessor kafkaChannelDefinitionProcessor(KafkaListenerEndpointRegistry endpointRegistry, KafkaOperations<Object, Object> kafkaOperations) {
        KafkaChannelDefinitionProcessor kafkaChannelDefinitionProcessor = new KafkaChannelDefinitionProcessor();
        kafkaChannelDefinitionProcessor.setEndpointRegistry(endpointRegistry);
        kafkaChannelDefinitionProcessor.setKafkaOperations(kafkaOperations);
        return kafkaChannelDefinitionProcessor;
    }

    @Bean
    public AdminClient kafkaAdminClient(KafkaContainer kafkaContainer) {
        Map<String, Object> adminProperties = new HashMap<>();
        adminProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());

        return AdminClient.create(adminProperties);
    }

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaContainer kafkaContainer) {
        Map<String, Object> adminProperties = new HashMap<>();
        adminProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        return new KafkaAdmin(adminProperties);
    }

    @Bean(destroyMethod = "stop")
    public KafkaContainer kafkaContainer() {
        KafkaContainer container = new KafkaContainer();
        container.withEnv("KAFKA_DELETE_TOPIC_ENABLE", "true");
        container.withEnv("KAFKA_GROUP_MIN_SESSION_TIMEOUT_MS ", "500");
        container.start();
        return container;
    }

}
