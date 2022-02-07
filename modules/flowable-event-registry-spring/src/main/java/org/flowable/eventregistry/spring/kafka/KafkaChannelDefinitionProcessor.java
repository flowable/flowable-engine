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
package org.flowable.eventregistry.spring.kafka;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.KafkaInboundChannelModel;
import org.flowable.eventregistry.model.KafkaOutboundChannelModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.GenericMessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * @author Filip Hrisafov
 */
public class KafkaChannelDefinitionProcessor implements BeanFactoryAware, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, ChannelModelProcessor {

    public static final String CHANNEL_ID_PREFIX = "org.flowable.eventregistry.kafka.ChannelKafkaListenerEndpointContainer#";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected KafkaOperations<Object, Object> kafkaOperations;

    protected KafkaListenerEndpointRegistry endpointRegistry;

    protected String containerFactoryBeanName = KafkaListenerAnnotationBeanPostProcessor.DEFAULT_KAFKA_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

    protected KafkaListenerContainerFactory<?> containerFactory;

    protected BeanFactory beanFactory;
    protected ApplicationContext applicationContext;
    protected boolean contextRefreshed;

    protected BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    protected StringValueResolver embeddedValueResolver;
    protected BeanExpressionContext expressionContext;

    @Override
    public boolean canProcess(ChannelModel channelModel) {
        return channelModel instanceof KafkaInboundChannelModel || channelModel instanceof KafkaOutboundChannelModel;
    }

    @Override
    public void registerChannelModel(ChannelModel channelModel, String tenantId, EventRegistry eventRegistry, 
                    EventRepositoryService eventRepositoryService, boolean fallbackToDefaultTenant) {
        
        if (channelModel instanceof KafkaInboundChannelModel) {
            KafkaInboundChannelModel kafkaChannelModel = (KafkaInboundChannelModel) channelModel;
            logger.info("Starting to register inbound channel {} in tenant {}", channelModel.getKey(), tenantId);

            KafkaListenerEndpoint endpoint = createKafkaListenerEndpoint(kafkaChannelModel, tenantId, eventRegistry);
            registerEndpoint(endpoint, null);
            logger.info("Finished registering inbound channel {} in tenant {}", channelModel.getKey(), tenantId);
            
        } else if (channelModel instanceof KafkaOutboundChannelModel) {
            logger.info("Starting to register outbound channel {} in tenant {}", channelModel.getKey(), tenantId);
            processOutboundDefinition((KafkaOutboundChannelModel) channelModel);
            logger.info("Finished registering outbound channel {} in tenant {}", channelModel.getKey(), tenantId);
        }
    }

    protected KafkaListenerEndpoint createKafkaListenerEndpoint(KafkaInboundChannelModel channelModel, String tenantId, EventRegistry eventRegistry) {
        String endpointId = getEndpointId(channelModel, tenantId);

        SimpleKafkaListenerEndpoint<Object, Object> endpoint = new SimpleKafkaListenerEndpoint<>();

        endpoint.setId(endpointId);
        endpoint.setGroupId(getEndpointGroupId(channelModel, endpoint.getId()));
        endpoint.setTopics(resolveTopics(channelModel));
        endpoint.setTopicPattern(resolvePattern(channelModel));
        endpoint.setClientIdPrefix(resolveExpressionAsString(channelModel.getClientIdPrefix(), "clientIdPrefix"));

        endpoint.setConcurrency(resolveExpressionAsInteger(channelModel.getConcurrency(), "concurrency"));
        endpoint.setConsumerProperties(resolveProperties(channelModel.getCustomProperties()));

        endpoint.setMessageListener(createMessageListener(eventRegistry, channelModel));
        return endpoint;
    }

    protected void processOutboundDefinition(KafkaOutboundChannelModel channelModel) {
        String topic = channelModel.getTopic();
        if (channelModel.getOutboundEventChannelAdapter() == null && StringUtils.hasText(topic)) {
            String resolvedTopic = resolve(topic);
            channelModel.setOutboundEventChannelAdapter(new KafkaOperationsOutboundEventChannelAdapter(
                            kafkaOperations, resolvedTopic, channelModel.getRecordKey()));
        }
    }

    protected Integer resolveExpressionAsInteger(String value, String attribute) {
        Object resolved = resolveExpression(value);
        Integer result = null;
        if (resolved instanceof String) {
            result = Integer.parseInt((String) resolved);
        } else if (resolved instanceof Number) {
            result = ((Number) resolved).intValue();
        } else if (resolved != null) {
            throw new IllegalStateException(
                "The [" + attribute + "] must resolve to an Number or a String that can be parsed as an Integer. "
                    + "Resolved to [" + resolved.getClass() + "] for [" + value + "]");
        }
        return result;
    }

    protected String resolveExpressionAsString(String value, String attribute) {
        if (!StringUtils.hasLength(value)) {
            return null;
        }
        Object resolved = resolveExpression(value);
        if (resolved instanceof String) {
            return (String) resolved;
        } else {
            throw new IllegalStateException("The [" + attribute + "] must resolve to a String. "
                + "Resolved to [" + resolved.getClass() + "] for [" + value + "]");
        }
    }

    protected Collection<String> resolveTopics(KafkaInboundChannelModel channelDefinition) {
        Collection<String> topics = channelDefinition.getTopics();

        List<String> resultTopics = new ArrayList<>();

        for (String queue : topics) {
            resolveTopics(resolveExpression(queue), resultTopics, channelDefinition);
        }

        return resultTopics;
    }

    protected void resolveTopics(Object resolvedValue, List<String> result, KafkaInboundChannelModel channelDefinition) {
        if (resolvedValue instanceof String[]) {
            for (String object : (String[]) resolvedValue) {
                resolveTopics(object, result, channelDefinition);
            }

        } else if (resolvedValue instanceof String) {
            result.add((String) resolvedValue);
        } else if (resolvedValue instanceof Iterable) {
            for (Object object : (Iterable<?>) resolvedValue) {
                resolveTopics(object, result, channelDefinition);
            }
        } else {
            throw new IllegalArgumentException(
                "Channel definition " + channelDefinition + " cannot resolve " + resolvedValue + " as a String[] or a String");
        }
    }

    protected Pattern resolvePattern(KafkaInboundChannelModel channelModel) {
        Pattern pattern = null;
        String topicPattern = channelModel.getTopicPattern();
        if (StringUtils.hasText(topicPattern)) {
            Object resolved = resolveExpression(topicPattern);
            if (resolved instanceof String) {
                pattern = Pattern.compile((String) resolved);
            } else if (resolved instanceof Pattern) {
                pattern = (Pattern) resolved;
            } else if (resolved != null) {
                throw new IllegalStateException(
                    "topicPattern in channel model [ " + channelModel + " ] must resolve to a Pattern or String, not " + resolved.getClass());
            }
        }

        return pattern;
    }

    protected Object resolveExpression(String value) {
        String resolvedValue = resolve(value);

        return this.resolver.evaluate(resolvedValue, this.expressionContext);
    }

    @SuppressWarnings("unchecked")
    protected GenericMessageListener<ConsumerRecord<Object, Object>> createMessageListener(EventRegistry eventRegistry, InboundChannelModel inboundChannelModel) {
        @SuppressWarnings("rawtypes")
        GenericMessageListener kafkaChannelMessageListenerAdapter = new KafkaChannelMessageListenerAdapter(eventRegistry, inboundChannelModel);
        return kafkaChannelMessageListenerAdapter;
    }

    @Override
    public void unregisterChannelModel(ChannelModel channelModel, String tenantId, EventRepositoryService eventRepositoryService) {
        logger.info("Starting to unregister channel {} in tenant {}", channelModel.getKey(), tenantId);
        String endpointId = getEndpointId(channelModel, tenantId);
        // currently it is not possible to unregister a listener container
        // In order not to do a lot of the logic that Spring does we are manually accessing the containers to remove them
        // see https://github.com/spring-projects/spring-framework/issues/24228
        MessageListenerContainer listenerContainer = endpointRegistry.getListenerContainer(endpointId);
        if (listenerContainer != null) {
            logger.debug("Stopping message listener {} for channel {} in tenant {}", listenerContainer, channelModel.getKey(), tenantId);
            listenerContainer.stop();
        }

        if (listenerContainer instanceof DisposableBean) {
            try {
                logger.debug("Destroying message listener {} for channel {} in tenant {}", listenerContainer, channelModel.getKey(), tenantId);
                ((DisposableBean) listenerContainer).destroy();
            } catch (Exception e) {
                throw new RuntimeException("Failed to destroy listener container", e);
            }
        }

        Field listenerContainersField = ReflectionUtils.findField(endpointRegistry.getClass(), "listenerContainers");
        if (listenerContainersField != null) {
            listenerContainersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, MessageListenerContainer> listenerContainers = (Map<String, MessageListenerContainer>) ReflectionUtils
                .getField(listenerContainersField, endpointRegistry);
            if (listenerContainers != null) {
                listenerContainers.remove(endpointId);
            }
        } else {
            throw new IllegalStateException("Endpoint registry " + endpointRegistry + " does not have listenerContainers field");
        }

        logger.info("Finished unregistering channel {} in tenant {}", channelModel.getKey(), tenantId);
    }

    /**
     * Register a new {@link KafkaListenerEndpoint} alongside the
     * {@link KafkaListenerContainerFactory} to use to create the underlying container.
     * <p>The {@code factory} may be {@code null} if the default factory has to be
     * used for that endpoint.
     */
    protected void registerEndpoint(KafkaListenerEndpoint endpoint, KafkaListenerContainerFactory<?> factory) {
        Assert.notNull(endpoint, "Endpoint must not be null");
        Assert.hasText(endpoint.getId(), "Endpoint id must be set");

        Assert.state(this.endpointRegistry != null, "No KafkaListenerEndpointRegistry set");
        // We need to start the container immediately only if the endpoint registry is already running,
        // otherwise we should not start it and leave it to the registry to start all the containers when it starts.
        // We also need to start immediately if the application context has already been refreshed.
        // This also makes sure that we are not going to start our listener earlier than the KafkaListenerEndpointRegistry
        boolean startImmediately = contextRefreshed || endpointRegistry.isRunning();
        logger.info("Registering endpoint {}", endpoint);
        endpointRegistry.registerListenerContainer(endpoint, resolveContainerFactory(endpoint, factory), startImmediately);
        logger.info("Finished registering endpoint {}", endpoint);
    }

    protected KafkaListenerContainerFactory<?> resolveContainerFactory(KafkaListenerEndpoint endpoint, KafkaListenerContainerFactory<?> containerFactory) {
        if (containerFactory != null) {
            return containerFactory;
        } else if (this.containerFactory != null) {
            return this.containerFactory;
        } else if (containerFactoryBeanName != null) {
            Assert.state(beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
            // Consider changing this if live change of the factory is required...
            this.containerFactory = beanFactory.getBean(containerFactoryBeanName, KafkaListenerContainerFactory.class);
            return this.containerFactory;
        } else {
            throw new IllegalStateException("Could not resolve the " +
                KafkaListenerContainerFactory.class.getSimpleName() + " to use for [" +
                endpoint + "] no factory was given and no default is set.");
        }
    }

    protected String getEndpointId(ChannelModel channelModel, String tenantId) {
        String channelDefinitionKey = channelModel.getKey();
        if (!StringUtils.hasText(tenantId)) {
            return CHANNEL_ID_PREFIX + channelDefinitionKey;
        }
        return CHANNEL_ID_PREFIX + tenantId + "#" + channelDefinitionKey;
    }

    protected String getEndpointGroupId(KafkaInboundChannelModel channelDefinition, String id) {
        String groupId = resolveExpressionAsString(channelDefinition.getGroupId(), "groupId");
        if (groupId == null) {
            groupId = id;
        }

        return groupId;
    }

    protected String resolve(String value) {
        if (value == null) {
            return null;
        } else if (embeddedValueResolver != null) {
            return embeddedValueResolver.resolveStringValue(value);
        } else {
            return value;
        }
    }

    protected Properties resolveProperties(List<KafkaInboundChannelModel.CustomProperty> consumerProperties) {
        if (consumerProperties != null && !consumerProperties.isEmpty()) {
            Properties properties = new Properties();
            for (KafkaInboundChannelModel.CustomProperty consumerProperty : consumerProperties) {
                properties.put(consumerProperty.getName(), resolveExpressionAsString(consumerProperty.getValue(), consumerProperty.getName()));
            }

            return properties;
        }

        return null;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
            this.resolver = ((ConfigurableListableBeanFactory) beanFactory).getBeanExpressionResolver();
            this.expressionContext = new BeanExpressionContext((ConfigurableListableBeanFactory) beanFactory, null);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == this.applicationContext) {
            this.contextRefreshed = true;
        }
    }

    public KafkaOperations<Object, Object> getKafkaOperations() {
        return kafkaOperations;
    }

    public void setKafkaOperations(KafkaOperations<Object, Object> kafkaOperations) {
        this.kafkaOperations = kafkaOperations;
    }

    public KafkaListenerEndpointRegistry getEndpointRegistry() {
        return endpointRegistry;
    }

    public void setEndpointRegistry(KafkaListenerEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    public String getContainerFactoryBeanName() {
        return containerFactoryBeanName;
    }

    public void setContainerFactoryBeanName(String containerFactoryBeanName) {
        this.containerFactoryBeanName = containerFactoryBeanName;
    }

    public KafkaListenerContainerFactory<?> getContainerFactory() {
        return containerFactory;
    }

    public void setContainerFactory(KafkaListenerContainerFactory<?> containerFactory) {
        this.containerFactory = containerFactory;
    }

}
