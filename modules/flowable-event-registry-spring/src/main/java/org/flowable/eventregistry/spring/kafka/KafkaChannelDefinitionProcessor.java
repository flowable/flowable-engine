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
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.KafkaInboundChannelModel;
import org.flowable.eventregistry.model.KafkaOutboundChannelModel;
import org.flowable.eventregistry.spring.kafka.payload.EventPayloadKafkaMessageKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdminOperations;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ExceptionClassifier;
import org.springframework.kafka.listener.FailedRecordProcessor;
import org.springframework.kafka.listener.GenericMessageListener;
import org.springframework.kafka.listener.KafkaConsumerBackoffManager;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.retrytopic.DeadLetterPublishingRecovererFactory;
import org.springframework.kafka.retrytopic.DefaultDestinationTopicProcessor;
import org.springframework.kafka.retrytopic.DefaultDestinationTopicResolver;
import org.springframework.kafka.retrytopic.DestinationTopic;
import org.springframework.kafka.retrytopic.DestinationTopicProcessor;
import org.springframework.kafka.retrytopic.FixedDelayStrategy;
import org.springframework.kafka.retrytopic.ListenerContainerFactoryConfigurer;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.retrytopic.RetryTopicSchedulerWrapper;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Suffixer;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.SleepingBackOffPolicy;
import org.springframework.retry.backoff.UniformRandomBackOffPolicy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link ChannelModelProcessor} which is responsible for configuring Kafka Event registry integration.
 * This class is not meant to be extended.
 *
 * @author Filip Hrisafov
 */
public class KafkaChannelDefinitionProcessor implements BeanFactoryAware, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, ChannelModelProcessor {

    public static final String CHANNEL_ID_PREFIX = "org.flowable.eventregistry.kafka.ChannelKafkaListenerEndpointContainer#";

    protected static final int DEFAULT_PARTITION_FOR_MANUAL_ASSIGNMENT = 0;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected KafkaOperations<Object, Object> kafkaOperations;
    protected KafkaAdminOperations kafkaAdminOperations;

    protected KafkaListenerEndpointRegistry endpointRegistry;

    protected String containerFactoryBeanName = KafkaListenerAnnotationBeanPostProcessor.DEFAULT_KAFKA_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

    protected KafkaListenerContainerFactory<?> containerFactory;
    protected KafkaConsumerBackoffManager kafkaConsumerBackoffManager;

    protected BeanFactory beanFactory;
    protected ApplicationContext applicationContext;
    protected boolean contextRefreshed;
    protected ObjectMapper objectMapper;

    protected BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    protected StringValueResolver embeddedValueResolver;
    protected BeanExpressionContext expressionContext;

    protected Map<String, Collection<String>> retryEndpointsByMainEndpointId = new HashMap<>();

    public KafkaChannelDefinitionProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canProcess(ChannelModel channelModel) {
        return channelModel instanceof KafkaInboundChannelModel || channelModel instanceof KafkaOutboundChannelModel;
    }
    
    @Override
    public boolean canProcessIfChannelModelAlreadyRegistered(ChannelModel channelModel) {
        return channelModel instanceof KafkaOutboundChannelModel;
    }

    @Override
    public void registerChannelModel(ChannelModel channelModel, String tenantId, EventRegistry eventRegistry,
            EventRepositoryService eventRepositoryService,
            boolean fallbackToDefaultTenant) {

        if (channelModel instanceof KafkaInboundChannelModel) {
            KafkaInboundChannelModel kafkaChannelModel = (KafkaInboundChannelModel) channelModel;
            logger.info("Starting to register inbound channel {} in tenant {}", channelModel.getKey(), tenantId);

            processAndRegisterEndpoints(kafkaChannelModel, tenantId, eventRegistry);
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
        endpoint.setTopicPartitions(resolveTopicPartitions(channelModel));
        endpoint.setClientIdPrefix(resolveExpressionAsString(channelModel.getClientIdPrefix(), "clientIdPrefix"));

        endpoint.setConcurrency(resolveExpressionAsInteger(channelModel.getConcurrency(), "concurrency"));
        endpoint.setConsumerProperties(resolveProperties(channelModel.getCustomProperties()));

        endpoint.setMessageListener(createMessageListener(eventRegistry, channelModel));

        return endpoint;
    }

    protected void processAndRegisterEndpoints(KafkaInboundChannelModel channelModel, String tenantId, EventRegistry eventRegistry) {

        KafkaListenerEndpoint mainEndpoint = createKafkaListenerEndpoint(channelModel, tenantId, eventRegistry);
        KafkaListenerContainerFactory<?> containerFactory = resolveContainerFactory(mainEndpoint, null);
        Collection<KafkaChannelDefinitionProcessor.Configuration> configurations = createEndpointConfigurations(channelModel, tenantId, eventRegistry,
                mainEndpoint, containerFactory);

        // Register all the configurations that belong to the main endpoint in order to be able to unregister them later
        retryEndpointsByMainEndpointId.put(mainEndpoint.getId(),
                configurations.stream().map(Configuration::getEndpoint).map(KafkaListenerEndpoint::getId).collect(Collectors.toList()));
        for (Configuration configuration : configurations) {
            registerEndpoint(configuration.getEndpoint(), configuration.getFactory());
        }

    }

    protected Collection<KafkaChannelDefinitionProcessor.Configuration> createEndpointConfigurations(KafkaInboundChannelModel channelModel, String tenantId,
            EventRegistry eventRegistry,
            KafkaListenerEndpoint mainEndpoint, KafkaListenerContainerFactory<?> containerFactory
    ) {
        ResolvedRetryConfiguration retryConfiguration = resolveRetryConfiguration(channelModel);

        BackOff backOff;
        if (retryConfiguration != null && retryConfiguration.attempts != null) {
            backOff = new FixedBackOff(0, retryConfiguration.attempts - 1);
        } else {
            backOff = null;
        }

        RetryTopicConfiguration retryTopicConfiguration = createRetryTopicConfiguration(retryConfiguration);

        if (retryTopicConfiguration != null) {

            Collection<String> topics;
            if (mainEndpoint.getTopics().isEmpty()) {
                TopicPartitionOffset[] topicPartitionsToAssign = mainEndpoint.getTopicPartitionsToAssign();
                if (topicPartitionsToAssign != null && topicPartitionsToAssign.length > 0) {
                    topics = Arrays.stream(topicPartitionsToAssign)
                            .map(TopicPartitionOffset::getTopic)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                } else {
                    topics = Collections.emptyList();
                }
            } else {
                topics = mainEndpoint.getTopics();
            }

            if (topics.isEmpty()) {
                throw new FlowableException("Channel model " + channelModel.getKey() + " in tenant " + tenantId
                        + " has retry configuration but no topics have been provided for it");
            }

            Collection<KafkaChannelDefinitionProcessor.Configuration> configurations = new ArrayList<>(
                    retryTopicConfiguration.getDestinationTopicProperties().size());

            DefaultDestinationTopicResolver topicResolver = new DefaultDestinationTopicResolver(Clock.systemUTC(), applicationContext);
            DefaultDestinationTopicProcessor processor = new DefaultDestinationTopicProcessor(topicResolver);
            ListenerContainerFactoryConfigurer factoryConfigurer = createListenerContainerFactoryConfigurer(retryConfiguration, backOff, topicResolver);

            DestinationTopicProcessor.Context context = new DestinationTopicProcessor.Context(retryTopicConfiguration.getDestinationTopicProperties());
            processor.processDestinationTopicProperties(destinationTopicProperties -> {
                Suffixer suffixer = new Suffixer(destinationTopicProperties.suffix());
                if (destinationTopicProperties.isMainEndpoint() || destinationTopicProperties.isDltTopic() || retryConfiguration.hasRetryTopic()) {
                    // We only need to register the retry topic if the retry topic suffix is configured
                    // Otherwise we are going to send to the retry topic instead of only sending to the dead letter
                    for (String topic : topics) {
                        String destinationTopic = suffixer.maybeAddTo(topic);
                        processor.registerDestinationTopic(topic, destinationTopic, destinationTopicProperties, context);
                    }
                }

                KafkaListenerEndpoint endpoint;
                if (destinationTopicProperties.isMainEndpoint()) {
                    // We are always going to register the main endpoint
                    endpoint = mainEndpoint;
                } else if (!destinationTopicProperties.isDltTopic() && retryConfiguration.hasRetryTopic()) {
                    // If the endpoint is not the DLT topic, and we have a retry topic suffix we need to configure the retry endpoint
                    endpoint = createKafkaListenerEndpoint(channelModel, tenantId, eventRegistry);
                } else {
                    endpoint = null;
                }

                if (endpoint instanceof SimpleKafkaListenerEndpoint) {
                    SimpleKafkaListenerEndpoint<?, ?> simpleEndpoint = (SimpleKafkaListenerEndpoint<?, ?>) endpoint;
                    simpleEndpoint.setId(suffixer.maybeAddTo(simpleEndpoint.getId()));
                    simpleEndpoint.setGroupId(suffixer.maybeAddTo(simpleEndpoint.getGroupId()));
                    TopicPartitionOffset[] topicPartitionsToAssign = endpoint.getTopicPartitionsToAssign();
                    if (endpoint.getTopics().isEmpty() && topicPartitionsToAssign != null) {
                        simpleEndpoint.setTopicPartitions(getTopicPartitions(destinationTopicProperties, suffixer,
                                endpoint.getTopicPartitionsToAssign()));
                    } else {
                        simpleEndpoint.setTopics(suffixer.maybeAddTo(simpleEndpoint.getTopics()));
                    }
                    simpleEndpoint.setClientIdPrefix(suffixer.maybeAddTo(simpleEndpoint.getClientIdPrefix()));

                    configurations.add(
                            new Configuration(
                                    simpleEndpoint,
                                    decorateFactory(destinationTopicProperties, factoryConfigurer, retryTopicConfiguration)
                            )
                    );
                }
            }, context);

            processor.processRegisteredDestinations(getTopicCreationFunction(retryConfiguration), context);

            // We need to do this, because the topic resolver is only scoped to this registration,
            // and we need to make sure that destination topics are resolved in a non-synchronized blocks
            topicResolver.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
            return configurations;

        } else if (backOff != null) {
            return Collections.singleton(new Configuration(mainEndpoint,
                    new RetryTopicContainerFactoryDecorator(containerFactory, () -> new DefaultErrorHandler(backOff))));
        } else {
            return Collections.singleton(new Configuration(mainEndpoint, containerFactory));
        }
    }

    protected static Collection<TopicPartitionOffset> getTopicPartitions(DestinationTopic.Properties properties,
            Suffixer suffixer,
            TopicPartitionOffset[] topicPartitionOffsets) {
        return Stream.of(topicPartitionOffsets)
                .map(tpo -> properties.isMainEndpoint()
                        ? getTPOForMainTopic(suffixer, tpo)
                        : getTPOForRetryTopics(properties, suffixer, tpo))
                .collect(Collectors.toList());
    }

    protected static TopicPartitionOffset getTPOForRetryTopics(DestinationTopic.Properties properties, Suffixer suffixer, TopicPartitionOffset tpo) {
        return new TopicPartitionOffset(suffixer.maybeAddTo(tpo.getTopic()),
                tpo.getPartition() <= properties.numPartitions() ? tpo.getPartition() : DEFAULT_PARTITION_FOR_MANUAL_ASSIGNMENT);
    }

    protected static TopicPartitionOffset getTPOForMainTopic(Suffixer suffixer, TopicPartitionOffset tpo) {
        TopicPartitionOffset newTpo = new TopicPartitionOffset(suffixer.maybeAddTo(tpo.getTopic()),
                tpo.getPartition(), tpo.getOffset(), tpo.getPosition());
        newTpo.setRelativeToCurrent(tpo.isRelativeToCurrent());
        return newTpo;
    }

    protected Consumer<Collection<String>> getTopicCreationFunction(ResolvedRetryConfiguration retryConfiguration) {
        if (retryConfiguration.autoCreateTopics) {
            if (kafkaAdminOperations == null) {
                throw new FlowableException("It is not possible to auto create new topics when no kafka admin operations have been configured");
            }
            return topics -> createNewTopics(topics, retryConfiguration.numPartitions, retryConfiguration.replicationFactor);
        }
        return topics -> {};
    }

    protected void createNewTopics(Collection<String> topics, int numPartitions, short replicationFactor) {
        kafkaAdminOperations.createOrModifyTopics(topics.stream().map(topic -> new NewTopic(topic, numPartitions, replicationFactor)).toArray(NewTopic[]::new));
    }

    protected ListenerContainerFactoryConfigurer createListenerContainerFactoryConfigurer(ResolvedRetryConfiguration retryConfiguration, BackOff backOff,
            DefaultDestinationTopicResolver topicResolver) {
        DeadLetterPublishingRecovererFactory recovererFactory = new DeadLetterPublishingRecovererFactory(topicResolver);

        KafkaConsumerBackoffManager manager = getOrCreateKafkaConsumerBackoffManager();
        ListenerContainerFactoryConfigurer factoryConfigurer = new ListenerContainerFactoryConfigurer(manager, recovererFactory, Clock.systemUTC());
        if (retryConfiguration.hasNoRetryTopic()) {
            // If we do not have a retry topic, then the retries have to be blocking
            factoryConfigurer.setErrorHandlerCustomizer(errorHandler -> {
                if (errorHandler instanceof ExceptionClassifier) {
                    // This is the default done in Spring Kafka
                    Map<Class<? extends Throwable>, Boolean> classified = new HashMap<>();
                    classified.put(DeserializationException.class, false);
                    classified.put(MessageConversionException.class, false);
                    classified.put(ConversionException.class, false);
                    classified.put(MethodArgumentResolutionException.class, false);
                    classified.put(NoSuchMethodException.class, false);
                    classified.put(ClassCastException.class, false);
                    ((ExceptionClassifier) errorHandler).setClassifications(classified, true);
                }

                if (errorHandler instanceof FailedRecordProcessor) {
                    ((FailedRecordProcessor) errorHandler).setCommitRecovered(false);
                }
            });

            if (backOff != null) {
                factoryConfigurer.setBlockingRetriesBackOff(backOff);
            }
        }
        return factoryConfigurer;
    }

    protected KafkaConsumerBackoffManager getOrCreateKafkaConsumerBackoffManager() {
        if (this.kafkaConsumerBackoffManager != null) {
            return this.kafkaConsumerBackoffManager;
        }

        this.kafkaConsumerBackoffManager = KafkaBackOffManagerUtils.createKafkaBackoffManagerFactory(endpointRegistry, applicationContext,
                this::getOrCreateRetryTopicTaskScheduler).create();
        return this.kafkaConsumerBackoffManager;

    }

    protected TaskScheduler getOrCreateRetryTopicTaskScheduler() {
        ObjectProvider<RetryTopicSchedulerWrapper> retryTopicSchedulerWrapperProvider = applicationContext.getBeanProvider(RetryTopicSchedulerWrapper.class);
        RetryTopicSchedulerWrapper schedulerWrapper = retryTopicSchedulerWrapperProvider.getIfAvailable();
        TaskScheduler retryTopicTaskScheduler;
        if (schedulerWrapper != null) {
            retryTopicTaskScheduler = schedulerWrapper.getScheduler();
        } else {
            retryTopicTaskScheduler = applicationContext.getBeanProvider(TaskScheduler.class).getIfAvailable();
        }

        if (retryTopicTaskScheduler == null) {
            ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
            threadPoolTaskScheduler.setThreadNamePrefix("flowable-kafka-retry-scheduling-");
            threadPoolTaskScheduler.afterPropertiesSet();
            ((ConfigurableApplicationContext) applicationContext).addApplicationListener(
                    (ApplicationListener<ContextClosedEvent>) event -> threadPoolTaskScheduler.destroy());
            retryTopicTaskScheduler = threadPoolTaskScheduler;
        }

        return retryTopicTaskScheduler;
    }

    protected KafkaListenerContainerFactory<?> decorateFactory(DestinationTopic.Properties destinationTopicProperties,
            ListenerContainerFactoryConfigurer factoryConfigurer, RetryTopicConfiguration retryTopicConfiguration) {
        // This is the same as it is done in the Spring Kata RetryTopicConfigurer
        return destinationTopicProperties.isMainEndpoint() ?
                factoryConfigurer.decorateFactoryWithoutSettingContainerProperties((ConcurrentKafkaListenerContainerFactory<?, ?>) containerFactory,
                        retryTopicConfiguration.forContainerFactoryConfigurer()) :
                factoryConfigurer.decorateFactory((ConcurrentKafkaListenerContainerFactory<?, ?>) containerFactory,
                        retryTopicConfiguration.forContainerFactoryConfigurer());
    }

    protected void processOutboundDefinition(KafkaOutboundChannelModel channelModel) {
        String topic = channelModel.getTopic();
        if (channelModel.getOutboundEventChannelAdapter() == null && StringUtils.hasText(topic)) {
            String resolvedTopic = resolve(topic);

            KafkaPartitionProvider partitionProvider = resolveKafkaPartitionProvider(channelModel);
            KafkaMessageKeyProvider<?> messageKeyProvider = resolveKafkaMessageKeyProvider(channelModel);

            channelModel.setOutboundEventChannelAdapter(new KafkaOperationsOutboundEventChannelAdapter(
                            kafkaOperations, partitionProvider, resolvedTopic, messageKeyProvider));
        }
    }

    protected Integer resolveExpressionAsInteger(String value, String attribute) {
        return resolveExpressionAsInteger(value, attribute, null);
    }

    protected Integer resolveExpressionAsInteger(String value, String attribute, Integer defaultValue) {
        Object resolved = resolveExpression(value);
        Integer result = defaultValue;
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

    protected Long resolveExpressionAsLong(String value, String attribute) {
        Object resolved = resolveExpression(value);
        Long result = null;
        if (resolved instanceof String) {
            result = Long.parseLong((String) resolved);
        } else if (resolved instanceof Number) {
            result = ((Number) resolved).longValue();
        } else if (resolved != null) {
            throw new IllegalStateException(
                    "The [" + attribute + "] must resolve to an Number or a String that can be parsed as a Long. "
                            + "Resolved to [" + resolved.getClass() + "] for [" + value + "]");
        }
        return result;
    }

    protected Double resolveExpressionAsDouble(String value, String attribute) {
        Object resolved = resolveExpression(value);
        Double result = null;
        if (resolved instanceof String) {
            result = Double.parseDouble((String) resolved);
        } else if (resolved instanceof Number) {
            result = ((Number) resolved).doubleValue();
        } else if (resolved != null) {
            throw new IllegalStateException(
                    "The [" + attribute + "] must resolve to an Number or a String that can be parsed as a Double. "
                            + "Resolved to [" + resolved.getClass() + "] for [" + value + "]");
        }
        return result;
    }

    protected Boolean resolveExpressionAsBoolean(String value, String attribute) {
        return resolveExpressionAsBoolean(value, attribute, null);
    }

    protected Boolean resolveExpressionAsBoolean(String value, String attribute, Boolean defaultValue) {
        Object resolved = resolveExpression(value);
        Boolean result = defaultValue;
        if (resolved instanceof String) {
            result = Boolean.parseBoolean((String) resolved);
        } else if (resolved instanceof Boolean) {
            result = (Boolean) resolved;
        } else if (resolved != null) {
            throw new IllegalStateException(
                    "The [" + attribute + "] must resolve to a Boolean or a String that can be parsed as a Boolean. "
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

        if (topics == null || topics.isEmpty()) {
            return Collections.emptyList();
        }

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

    protected Collection<TopicPartitionOffset> resolveTopicPartitions(KafkaInboundChannelModel channelModel) {
        Collection<KafkaInboundChannelModel.TopicPartition> topicPartitions = channelModel.getTopicPartitions();
        if (topicPartitions == null || topicPartitions.isEmpty()) {
            return Collections.emptyList();
        }

        List<TopicPartitionOffset> tps = new ArrayList<>();
        for (KafkaInboundChannelModel.TopicPartition topicPartition : topicPartitions) {
            String topic = resolveExpressionAsString(topicPartition.getTopic(), "topicPartitions[].topic");
            if (!StringUtils.hasText(topic)) {
                throw new FlowableIllegalArgumentException(
                        "topic in topic partition in channel model [ " + channelModel.getKey() + " ] must resolve to a non empty string");
            }

            Collection<String> partitions = topicPartition.getPartitions();
            if (partitions == null || partitions.isEmpty()) {
                throw new FlowableIllegalArgumentException(
                        "partitions in topic partition in channel model [ " + channelModel.getKey() + " ] must not be empty");
            }

            for (String partition : partitions) {
                resolvePartitionAsInteger(topic, resolveExpression(partition), tps);
            }

        }

        return tps;
    }

    protected void resolvePartitionAsInteger(String topic, Object resolvedValue, List<TopicPartitionOffset> result) {
        // This is the same as it is done in the Spring KafkaListenerAnnotationBeanPostProcessor#resolvePartitionAsInteger

        if (resolvedValue instanceof String[]) {
            for (Object object : (String[]) resolvedValue) {
                resolvePartitionAsInteger(topic, object, result);
            }
        }
        else if (resolvedValue instanceof String) {
            Assert.state(StringUtils.hasText((String) resolvedValue),
                    () -> "partition in TopicPartition for topic '" + topic + "' cannot be empty");
            List<TopicPartitionOffset> collected = parsePartitions((String) resolvedValue)
                    .map(part -> new TopicPartitionOffset(topic, part))
                    .collect(Collectors.toList());
            result.addAll(collected);
        }
        else if (resolvedValue instanceof Integer[]) {
            for (Integer partition : (Integer[]) resolvedValue) {
                result.add(new TopicPartitionOffset(topic, partition));
            }
        }
        else if (resolvedValue instanceof Integer) {
            result.add(new TopicPartitionOffset(topic, (Integer) resolvedValue));
        }
        else if (resolvedValue instanceof Iterable) {
            //noinspection unchecked
            for (Object object : (Iterable<Object>) resolvedValue) {
                resolvePartitionAsInteger(topic, object, result);
            }
        }
        else {
            throw new IllegalArgumentException(
                    "partition in TopicPartition for topic '" + topic + "' can't resolve '" + resolvedValue + "' as an Integer or String");
        }
    }

    /**
     * Parse a list of partitions into a {@link List}. Example: "0-5,10-15".
     * This parsing is the same as it is done in the Spring {@code KafkaListenerAnnotationBeanPostProcessor}.
     *
     * @param partsString the comma-delimited list of partitions/ranges.
     * @return the stream of partition numbers, sorted and de-duplicated.
     */
    protected Stream<Integer> parsePartitions(String partsString) {
        // This is the same as it is done in the Spring KafkaListenerAnnotationBeanPostProcessor#parsePartitions
        String[] partsStrings = partsString.split(",");
        if (partsStrings.length == 1 && !partsStrings[0].contains("-")) {
            return Stream.of(Integer.parseInt(partsStrings[0].trim()));
        }
        List<Integer> parts = new ArrayList<>();
        for (String part : partsStrings) {
            if (part.contains("-")) {
                String[] startEnd = part.split("-");
                Assert.state(startEnd.length == 2, "Only one hyphen allowed for a range of partitions: " + part);
                int start = Integer.parseInt(startEnd[0].trim());
                int end = Integer.parseInt(startEnd[1].trim());
                Assert.state(end >= start, "Invalid range: " + part);
                for (int i = start; i <= end; i++) {
                    parts.add(i);
                }
            }
            else {
                parsePartitions(part).forEach(p -> parts.add(p));
            }
        }
        return parts.stream()
                .sorted()
                .distinct();
    }

    protected KafkaPartitionProvider resolveKafkaPartitionProvider(KafkaOutboundChannelModel channelModel) {
        KafkaOutboundChannelModel.KafkaPartition partition = channelModel.getPartition();
        if (partition == null) {
            return null;
        }

        if (StringUtils.hasText(partition.getEventField())) {
            return new EventPayloadKafkaPartitionProvider(partition.getEventField());
        } else if (StringUtils.hasText(partition.getDelegateExpression())) {
            return resolveExpression(partition.getDelegateExpression(), KafkaPartitionProvider.class);
        } else if (StringUtils.hasText(partition.getRoundRobin())) {
            List<TopicPartitionOffset> tpo = new ArrayList<>();
            resolvePartitionAsInteger(channelModel.getTopic(), resolveExpression(partition.getRoundRobin()), tpo);
            List<Integer> partitions = new ArrayList<>(tpo.size());
            for (TopicPartitionOffset offset : tpo) {
                partitions.add(offset.getPartition());
            }
            return new RoundRobinKafkaPartitionProvider(partitions);
        } else {
            throw new FlowableException(
                    "The kafka partition value was not found for the channel model with key " + channelModel.getKey()
                            + ". One of eventField, delegateExpression should be set.");
        }
    }

    protected KafkaMessageKeyProvider<?> resolveKafkaMessageKeyProvider(KafkaOutboundChannelModel channelModel) {
        KafkaOutboundChannelModel.RecordKey recordKey = channelModel.getRecordKey();
        if (recordKey == null) {
            return null;
        }
        if (StringUtils.hasText(recordKey.getEventField())) {
            return new EventPayloadKafkaMessageKeyProvider(recordKey.getEventField());
        } else if (StringUtils.hasText(recordKey.getDelegateExpression())) {
            return resolveExpression(recordKey.getDelegateExpression(), KafkaMessageKeyProvider.class);
        } else if (recordKey.getFixedValue() != null) {
            String fixedValue = org.apache.commons.lang3.StringUtils.defaultIfBlank(recordKey.getFixedValue(), null);
            return ignore -> fixedValue;
        } else {
            throw new FlowableException(
                    "The kafka recordKey value was not found for the channel model with key " + channelModel.getKey()
                            + ". One of fixedValue, delegateExpression or eventField should be set.");
        }
    }

    protected <T> T resolveExpression(String expression, Class<T> type) {
        Object value = this.resolver.evaluate(expression, this.expressionContext);
        if (type.isInstance(value)) {
            return type.cast(value);
        }

        throw new FlowableException("expected expression " + expression + " to resolve to " + type + " but it did not. Resolved value is " + value);

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
        String mainEndpointId = getEndpointId(channelModel, tenantId);
        Collection<String> endpointsToUnregister = retryEndpointsByMainEndpointId.getOrDefault(mainEndpointId, Collections.singleton(mainEndpointId));
        for (String endpointId : endpointsToUnregister) {
            unregisterEndpoint(endpointId, channelModel, tenantId);
        }
        logger.info("Finished unregistering channel {} in tenant {}", channelModel.getKey(), tenantId);
    }

    protected void unregisterEndpoint(String endpointId, ChannelModel channelModel, String tenantId) {
        // currently it is not possible to unregister a listener container
        // In order not to do a lot of the logic that Spring does we are manually accessing the containers to remove them
        // see https://github.com/spring-projects/spring-framework/issues/24228
        logger.info("Unregistering endpoint {}", endpointId);
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
        logger.info("Finished unregistering endpoint {}", endpointId);
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

    public KafkaAdminOperations getKafkaAdminOperations() {
        return kafkaAdminOperations;
    }

    public void setKafkaAdminOperations(KafkaAdminOperations kafkaAdminOperations) {
        this.kafkaAdminOperations = kafkaAdminOperations;
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

    public KafkaConsumerBackoffManager getKafkaConsumerBackoffManager() {
        return kafkaConsumerBackoffManager;
    }

    public void setKafkaConsumerBackoffManager(KafkaConsumerBackoffManager kafkaConsumerBackoffManager) {
        this.kafkaConsumerBackoffManager = kafkaConsumerBackoffManager;
    }

    protected static class RetryTopicContainerFactoryDecorator implements KafkaListenerContainerFactory<MessageListenerContainer> {
        // We need this class in order to provide only blocking retries with dead letter topic

        private final KafkaListenerContainerFactory<?> delegate;
        private final Supplier<CommonErrorHandler> errorHandlerProvider;

        private RetryTopicContainerFactoryDecorator(KafkaListenerContainerFactory<?> delegate, Supplier<CommonErrorHandler> errorHandlerProvider) {
            this.delegate = delegate;
            this.errorHandlerProvider = errorHandlerProvider;
        }

        @Override
        public MessageListenerContainer createListenerContainer(KafkaListenerEndpoint endpoint) {
            return decorate(this.delegate.createListenerContainer(endpoint));
        }

        @Override
        public MessageListenerContainer createContainer(TopicPartitionOffset... topicPartitions) {
            return decorate(this.delegate.createContainer(topicPartitions));
        }

        @Override
        public MessageListenerContainer createContainer(String... topics) {
            return decorate(this.delegate.createContainer(topics));
        }

        @Override
        public MessageListenerContainer createContainer(Pattern topicPattern) {
            return decorate(this.delegate.createContainer(topicPattern));
        }

        protected MessageListenerContainer decorate(MessageListenerContainer listenerContainer) {
            if (listenerContainer instanceof AbstractMessageListenerContainer) {
                AbstractMessageListenerContainer<?, ?> container = (ConcurrentMessageListenerContainer<?, ?>) listenerContainer;
                container.setCommonErrorHandler(errorHandlerProvider.get());
            }
            return listenerContainer;
        }

    }

    protected static class Configuration {

        protected final KafkaListenerEndpoint endpoint;
        protected final KafkaListenerContainerFactory<?> factory;

        protected Configuration(KafkaListenerEndpoint endpoint, KafkaListenerContainerFactory<?> factory) {
            this.endpoint = endpoint;
            this.factory = factory;
        }

        public KafkaListenerEndpoint getEndpoint() {
            return endpoint;
        }

        public KafkaListenerContainerFactory<?> getFactory() {
            return factory;
        }
    }

    protected RetryTopicConfiguration createRetryTopicConfiguration(ResolvedRetryConfiguration retryConfiguration) {
        if (retryConfiguration == null) {
            return null;
        }
        String dltTopicSuffix = retryConfiguration.dltTopicSuffix;
        String retryTopicSuffix = retryConfiguration.retryTopicSuffix;

        if (dltTopicSuffix == null && retryTopicSuffix == null) {
            return null;
        }

        Integer attempts = retryConfiguration.attempts;

        RetryTopicConfigurationBuilder retryTopicConfigurationBuilder = RetryTopicConfigurationBuilder.newInstance()
                .autoStartDltHandler(false)
                .autoCreateTopics(retryConfiguration.autoCreateTopics, retryConfiguration.numPartitions, retryConfiguration.replicationFactor)
                .dltSuffix(dltTopicSuffix)
                .retryTopicSuffix(retryTopicSuffix)
                .useSingleTopicForFixedDelays(retryConfiguration.fixedDelayTopicStrategy)
                .setTopicSuffixingStrategy(retryConfiguration.topicSuffixingStrategy);

        if (dltTopicSuffix == null) {
            retryTopicConfigurationBuilder.doNotConfigureDlt();
        }

        if (retryConfiguration.hasRetryTopic()) {
            retryTopicConfigurationBuilder.customBackoff(retryConfiguration.nonBlockingBackOff);
        } else {
            retryTopicConfigurationBuilder.noBackoff();
        }

        if (attempts != null) {
            retryTopicConfigurationBuilder.maxAttempts(attempts);
        }

        RetryTopicConfiguration retryTopicConfiguration = retryTopicConfigurationBuilder
                .create(kafkaOperations);

        return retryTopicConfiguration;
    }

    protected ResolvedRetryConfiguration resolveRetryConfiguration(KafkaInboundChannelModel channelModel) {
        KafkaInboundChannelModel.RetryConfiguration retry = channelModel.getRetry();
        if (retry == null) {
            return null;
        }

        ResolvedRetryConfiguration resolvedRetryConfiguration = new ResolvedRetryConfiguration();

        resolvedRetryConfiguration.attempts = resolveExpressionAsInteger(retry.getAttempts(), "retry.attempts");
        resolvedRetryConfiguration.dltTopicSuffix = resolveExpressionAsString(retry.getDltTopicSuffix(), "retry.dltTopicSuffix");
        resolvedRetryConfiguration.retryTopicSuffix = resolveExpressionAsString(retry.getRetryTopicSuffix(), "retry.retryTopicSuffix");

        String fixedDelayTopicStrategy = resolveExpressionAsString(retry.getFixedDelayTopicStrategy(), "retry.fixedDelayTopicStrategy");
        // Different default from Spring Kafka
        resolvedRetryConfiguration.fixedDelayTopicStrategy =
                fixedDelayTopicStrategy == null ? FixedDelayStrategy.SINGLE_TOPIC : FixedDelayStrategy.valueOf(fixedDelayTopicStrategy);

        String topicSuffixingStrategy = resolveExpressionAsString(retry.getTopicSuffixingStrategy(), "retry.topicSuffixingStrategy");
        // Different default from Spring Kafka
        resolvedRetryConfiguration.topicSuffixingStrategy =
                topicSuffixingStrategy == null ? TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE : TopicSuffixingStrategy.valueOf(topicSuffixingStrategy);

        resolvedRetryConfiguration.nonBlockingBackOff = createNonBlockingBackOffPolicy(retry.getNonBlockingBackOff());

        // Same defaults as in Spring Kafka
        resolvedRetryConfiguration.autoCreateTopics = resolveExpressionAsBoolean(retry.getAutoCreateTopics(), "retry.autoCreateTopics", true);
        resolvedRetryConfiguration.numPartitions = resolveExpressionAsInteger(retry.getNumPartitions(), "retry.numPartitions", 1);
        resolvedRetryConfiguration.replicationFactor = resolveExpressionAsInteger(retry.getReplicationFactor(), "retry.replicationFactor", 1).shortValue();

        return resolvedRetryConfiguration;
    }

    protected SleepingBackOffPolicy<?> createNonBlockingBackOffPolicy(KafkaInboundChannelModel.NonBlockingRetryBackOff backOff) {
        if (backOff == null) {
            return new FixedBackOffPolicy();
        }
        // This code is the same as the one from Spring Kafka
        Long delay = resolveExpressionAsLong(backOff.getDelay(), "retry.nonBlockingBackOff.delay");
        // Same default as Spring Retry
        Long min = delay;
        Long max = resolveExpressionAsLong(backOff.getMaxDelay(), "retry.nonBlockingBackOff.maxDelay");
        Double multiplier = resolveExpressionAsDouble(backOff.getMultiplier(), "retry.nonBlockingBackOff.multiplier");
        if (multiplier != null && multiplier > 0) {
            ExponentialBackOffPolicy policy;
            Boolean random = resolveExpressionAsBoolean(backOff.getRandom(), "retry.nonBlockingBackOff.random");
            if (Boolean.TRUE.equals(random)) {
                policy = new ExponentialRandomBackOffPolicy();
            } else {
                policy = new ExponentialBackOffPolicy();
            }

            if (min != null) {
                policy.setInitialInterval(min);
            }

            policy.setMultiplier(multiplier);

            if (max != null && max > policy.getInitialInterval()) {
                policy.setMaxInterval(max);
            }

            return policy;
        }

        if (max != null && min != null && max > min) {
            UniformRandomBackOffPolicy policy = new UniformRandomBackOffPolicy();
            policy.setMinBackOffPeriod(min);
            policy.setMaxBackOffPeriod(max);
            return policy;
        }

        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        if (min != null) {
            policy.setBackOffPeriod(min);
        }

        return policy;
    }

    protected static class ResolvedRetryConfiguration {

        protected Integer attempts;
        protected String dltTopicSuffix;
        protected String retryTopicSuffix;
        protected FixedDelayStrategy fixedDelayTopicStrategy;
        protected TopicSuffixingStrategy topicSuffixingStrategy;
        protected SleepingBackOffPolicy<?> nonBlockingBackOff;
        protected boolean autoCreateTopics;
        protected int numPartitions;
        protected short replicationFactor;

        protected boolean hasRetryTopic() {
            return retryTopicSuffix != null;
        }

        protected boolean hasNoRetryTopic() {
            return !hasRetryTopic();
        }

    }

}
