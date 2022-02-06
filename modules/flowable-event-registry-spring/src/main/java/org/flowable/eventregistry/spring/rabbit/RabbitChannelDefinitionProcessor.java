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
package org.flowable.eventregistry.spring.rabbit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.RabbitInboundChannelModel;
import org.flowable.eventregistry.model.RabbitOutboundChannelModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListenerAnnotationBeanPostProcessor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * @author Filip Hrisafov
 */
public class RabbitChannelDefinitionProcessor implements BeanFactoryAware, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, ChannelModelProcessor {

    public static final String CHANNEL_ID_PREFIX = "org.flowable.eventregistry.rabbit.ChannelRabbitListenerEndpointContainer#";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected RabbitListenerEndpointRegistry endpointRegistry;

    protected String containerFactoryBeanName = RabbitListenerAnnotationBeanPostProcessor.DEFAULT_RABBIT_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

    protected RabbitOperations rabbitOperations;

    protected RabbitListenerContainerFactory<?> containerFactory;

    protected BeanFactory beanFactory;
    protected ApplicationContext applicationContext;
    protected boolean contextRefreshed;

    protected BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    protected StringValueResolver embeddedValueResolver;
    protected BeanExpressionContext expressionContext;

    @Override
    public boolean canProcess(ChannelModel channelModel) {
        return channelModel instanceof RabbitInboundChannelModel || channelModel instanceof RabbitOutboundChannelModel;
    }

    @Override
    public void registerChannelModel(ChannelModel channelModel, String tenantId, EventRegistry eventRegistry, 
                    EventRepositoryService eventRepositoryService, boolean fallbackToDefaultTenant) {
        
        if (channelModel instanceof RabbitInboundChannelModel) {
            RabbitInboundChannelModel rabbitChannelDefinition = (RabbitInboundChannelModel) channelModel;
            logger.info("Starting to register inbound channel {} in tenant {}", channelModel.getKey(), tenantId);

            RabbitListenerEndpoint endpoint = createRabbitListenerEndpoint(rabbitChannelDefinition, tenantId, eventRegistry);
            registerEndpoint(endpoint, null);
            logger.info("Finished registering inbound channel {} in tenant {}", channelModel.getKey(), tenantId);
        } else if (channelModel instanceof RabbitOutboundChannelModel) {
            logger.info("Starting to register outbound channel {} in tenant {}", channelModel.getKey(), tenantId);
            processOutboundDefinition((RabbitOutboundChannelModel) channelModel);
            logger.info("Finished registering outbound channel {} in tenant {}", channelModel.getKey(), tenantId);
        }
    }

    protected RabbitListenerEndpoint createRabbitListenerEndpoint(RabbitInboundChannelModel channelModel, String tenantId, EventRegistry eventRegistry) {
        String endpointId = getEndpointId(channelModel, tenantId);

        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();

        endpoint.setId(endpointId);
        endpoint.setQueueNames(resolveQueues(channelModel));

        endpoint.setConcurrency(resolveExpressionAsStringOrInteger(channelModel.getConcurrency(), "concurrency"));
        endpoint.setBeanFactory(beanFactory);

        endpoint.setExclusive(channelModel.isExclusive());

        endpoint.setPriority(resolvePriority(channelModel));
        endpoint.setAdmin(resolveAdmin(channelModel));
        endpoint.setTaskExecutor(resolveExecutor(channelModel));
        endpoint.setAckMode(resolveAckMode(channelModel));

        endpoint.setMessageListener(createMessageListener(eventRegistry, channelModel));
        return endpoint;
    }

    protected void processOutboundDefinition(RabbitOutboundChannelModel channelDefinition) {
        String routingKey = channelDefinition.getRoutingKey();
        if (channelDefinition.getOutboundEventChannelAdapter() == null && StringUtils.hasText(routingKey)) {
            String resolvedRoutingKey = resolve(routingKey);
            String exchange = resolve(channelDefinition.getExchange());
            channelDefinition
                .setOutboundEventChannelAdapter(new RabbitOperationsOutboundEventChannelAdapter(rabbitOperations, exchange, resolvedRoutingKey));
        }
    }

    protected String resolveExpressionAsStringOrInteger(String value, String attribute) {
        if (!StringUtils.hasLength(value)) {
            return null;
        }
        Object resolved = resolveExpression(value);
        if (resolved instanceof String) {
            return (String) resolved;
        } else if (resolved instanceof Integer) {
            return resolved.toString();
        } else {
            throw new IllegalStateException("The [" + attribute + "] must resolve to a String. "
                + "Resolved to [" + resolved.getClass() + "] for [" + value + "]");
        }
    }

    protected String[] resolveQueues(RabbitInboundChannelModel channelDefinition) {
        Collection<String> queues = channelDefinition.getQueues();
        if (queues == null) {
            throw new IllegalArgumentException("Queues in " + channelDefinition + " must not be null");
        }

        List<String> resultQueues = new ArrayList<>();

        for (String queue : queues) {
            resolveQueues(resolveExpression(queue), resultQueues, channelDefinition);
        }

        return resultQueues.toArray(new String[0]);
    }

    protected void resolveQueues(Object resolvedValue, List<String> result, RabbitInboundChannelModel channelDefinition) {
        Object resolvedValueToUse = resolvedValue;
        if (resolvedValueToUse instanceof String[]) {
            resolvedValueToUse = Arrays.asList((String[]) resolvedValueToUse);
        }

        if (resolvedValueToUse instanceof String) {
            result.add((String) resolvedValueToUse);
        } else if (resolvedValueToUse instanceof Queue) {
            result.add(((Queue) resolvedValueToUse).getName());
        } else if (resolvedValueToUse instanceof Iterable) {
            for (Object object : (Iterable<?>) resolvedValueToUse) {
                resolveQueues(object, result, channelDefinition);
            }
        } else {
            throw new IllegalArgumentException(
                "Channel definition " + channelDefinition + " cannot resolve " + resolvedValue + " as a String[] or a String or a Queue");
        }
    }

    protected Integer resolvePriority(RabbitInboundChannelModel channelDefinition) {
        String priority = resolve(channelDefinition.getPriority());
        if (StringUtils.hasText(priority)) {
            try {
                return Integer.valueOf(priority);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid priority value for " +
                    channelDefinition + " (must be an integer)", ex);
            }
        } else {
            return null;
        }
    }

    protected RabbitAdmin resolveAdmin(RabbitInboundChannelModel channelDefinition) {
        String rabbitAdmin = resolve(channelDefinition.getAdmin());
        if (StringUtils.hasText(rabbitAdmin)) {
            Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve RabbitAdmin by bean name");
            try {
                return this.beanFactory.getBean(rabbitAdmin, RabbitAdmin.class);
            } catch (NoSuchBeanDefinitionException ex) {
                throw new IllegalArgumentException("Could not register rabbit listener endpoint on [" +
                    channelDefinition + "], no " + RabbitAdmin.class.getSimpleName() + " with id '" +
                    rabbitAdmin + "' was found in the application context", ex);
            }
        } else {
            return null;
        }
    }

    protected AcknowledgeMode resolveAckMode(RabbitInboundChannelModel channelDefinition) {
        String ackModeAttr = channelDefinition.getAckMode();
        if (StringUtils.hasText(ackModeAttr)) {
            Object ackMode = resolveExpression(ackModeAttr);
            if (ackMode instanceof String) {
                return AcknowledgeMode.valueOf((String) ackMode);
            } else if (ackMode instanceof AcknowledgeMode) {
                return (AcknowledgeMode) ackMode;
            } else {
                throw new IllegalArgumentException("ackMode in definition [ " + channelDefinition + " ] must resolve to a String or AcknowledgeMode");
            }
        } else {
            return null;
        }
    }

    protected TaskExecutor resolveExecutor(RabbitInboundChannelModel channelDefinition) {
        String executorBeanName = resolve(channelDefinition.getExecutor());
        if (StringUtils.hasText(executorBeanName)) {
            Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve TaskExecutor by bean name");
            try {
                return this.beanFactory.getBean(executorBeanName, TaskExecutor.class);
            } catch (NoSuchBeanDefinitionException ex) {
                throw new IllegalArgumentException("Could not register rabbit listener endpoint on [" +
                    channelDefinition + "], no " + TaskExecutor.class.getSimpleName() + " with id '" +
                    executorBeanName + "' was found in the application context", ex);
            }
        } else {
            return null;
        }
    }

    protected Object resolveExpression(String value) {
        String resolvedValue = resolve(value);

        return this.resolver.evaluate(resolvedValue, this.expressionContext);
    }

    protected MessageListener createMessageListener(EventRegistry eventRegistry, InboundChannelModel inboundChannelModel) {
        return new RabbitChannelMessageListenerAdapter(eventRegistry, inboundChannelModel);
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
     * Register a new {@link RabbitListenerEndpoint} alongside the
     * {@link RabbitListenerContainerFactory} to use to create the underlying container.
     * <p>The {@code factory} may be {@code null} if the default factory has to be
     * used for that endpoint.
     */
    protected void registerEndpoint(RabbitListenerEndpoint endpoint, RabbitListenerContainerFactory<?> factory) {
        Assert.notNull(endpoint, "Endpoint must not be null");
        Assert.hasText(endpoint.getId(), "Endpoint id must be set");

        Assert.state(this.endpointRegistry != null, "No RabbitListenerEndpointRegistry set");
        // We need to start the container immediately only if the endpoint registry is already running,
        // otherwise we should not start it and leave it to the registry to start all the containers when it starts.
        // If we do not do that then it is possible that @RabbitListener will not be started
        // We also need to start immediately if the application context has already been refreshed.
        // If we don't and the endpoint has no registered containers then our endpoint will never be started.
        // This also makes sure that we are not going to start our listener earlier than the RabbitListenerEndpointRegistry
        boolean startImmediately = contextRefreshed || endpointRegistry.isRunning();
        logger.info("Registering endpoint {} with start immediately {}", endpoint, startImmediately);
        endpointRegistry.registerListenerContainer(endpoint, resolveContainerFactory(endpoint, factory), startImmediately);
        logger.info("Finished registering endpoint {}", endpoint);
    }

    protected RabbitListenerContainerFactory<?> resolveContainerFactory(RabbitListenerEndpoint endpoint, RabbitListenerContainerFactory<?> containerFactory) {
        if (containerFactory != null) {
            return containerFactory;
        } else if (this.containerFactory != null) {
            return this.containerFactory;
        } else if (containerFactoryBeanName != null) {
            Assert.state(beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
            // Consider changing this if live change of the factory is required...
            this.containerFactory = beanFactory.getBean(containerFactoryBeanName, RabbitListenerContainerFactory.class);
            return this.containerFactory;
        } else {
            throw new IllegalStateException("Could not resolve the " +
                RabbitListenerContainerFactory.class.getSimpleName() + " to use for [" +
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

    protected String resolve(String value) {
        if (value == null) {
            return null;
        } else if (embeddedValueResolver != null) {
            return embeddedValueResolver.resolveStringValue(value);
        } else {
            return value;
        }
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

    public RabbitOperations getRabbitOperations() {
        return rabbitOperations;
    }

    public void setRabbitOperations(RabbitOperations rabbitOperations) {
        this.rabbitOperations = rabbitOperations;
    }

    public RabbitListenerEndpointRegistry getEndpointRegistry() {
        return endpointRegistry;
    }

    public void setEndpointRegistry(RabbitListenerEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    public String getContainerFactoryBeanName() {
        return containerFactoryBeanName;
    }

    public void setContainerFactoryBeanName(String containerFactoryBeanName) {
        this.containerFactoryBeanName = containerFactoryBeanName;
    }

    public RabbitListenerContainerFactory<?> getContainerFactory() {
        return containerFactory;
    }

    public void setContainerFactory(RabbitListenerContainerFactory<?> containerFactory) {
        this.containerFactory = containerFactory;
    }

}
