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
package org.flowable.eventregistry.impl.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder;
import org.flowable.eventregistry.json.converter.ChannelJsonConverter;
import org.flowable.eventregistry.model.ChannelEventKeyDetection;
import org.flowable.eventregistry.model.ChannelEventTenantIdDetection;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.DelegateExpressionInboundChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.flowable.eventregistry.model.KafkaInboundChannelModel;
import org.flowable.eventregistry.model.RabbitInboundChannelModel;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class InboundChannelDefinitionBuilderImpl implements InboundChannelModelBuilder {

    protected EventRepositoryService eventRepository;
    protected ChannelJsonConverter channelJsonConverter;

    protected InboundChannelModel channelModel;
    protected String deploymentName;
    protected String resourceName;
    protected String category;
    protected String parentDeploymentId;
    protected String deploymentTenantId;
    protected String key;
    protected InboundEventProcessingPipelineBuilder inboundEventProcessingPipelineBuilder;

    public InboundChannelDefinitionBuilderImpl(EventRepositoryService eventRepository, ChannelJsonConverter channelJsonConverter) {
        this.eventRepository = eventRepository;
        this.channelJsonConverter = channelJsonConverter;
    }

    @Override
    public InboundChannelModelBuilder key(String key) {
        this.key = key;
        return this;
    }
    
    @Override
    public InboundChannelModelBuilder deploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
        return this;
    }
    
    @Override
    public InboundChannelModelBuilder resourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }
    
    @Override
    public InboundChannelModelBuilder category(String category) {
        this.category = category;
        return this;
    }
    
    @Override
    public InboundChannelModelBuilder parentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }
    
    @Override
    public InboundChannelModelBuilder deploymentTenantId(String deploymentTenantId) {
        this.deploymentTenantId = deploymentTenantId;
        return this;
    }

    @Override
    public InboundEventProcessingPipelineBuilder channelAdapter(String delegateExpression) {
        DelegateExpressionInboundChannelModel channelModel = new DelegateExpressionInboundChannelModel();
        channelModel.setAdapterDelegateExpression(delegateExpression);
        this.channelModel = channelModel;
        this.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(channelModel, eventRepository, this);
        return this.inboundEventProcessingPipelineBuilder;
    }

    @Override
    public InboundJmsChannelBuilder jmsChannelAdapter(String destinationName) {
        JmsInboundChannelModel channelModel = new JmsInboundChannelModel();
        channelModel.setDestination(destinationName);
        this.channelModel = channelModel;
        this.channelModel.setKey(key);
        return new InboundJmsChannelBuilderImpl(channelModel, eventRepository, this);
    }

    @Override
    public InboundRabbitChannelBuilder rabbitChannelAdapter(String queueName) {
        RabbitInboundChannelModel channelModel = new RabbitInboundChannelModel();
        Set<String> queues = new LinkedHashSet<>();
        queues.add(queueName);
        channelModel.setQueues(queues);
        this.channelModel = channelModel;
        this.channelModel.setKey(key);
        return new InboundRabbitChannelBuilderImpl(channelModel, eventRepository, this);
    }

    @Override
    public InboundKafkaChannelBuilder kafkaChannelAdapter(String topic) {
        KafkaInboundChannelModel channelModel = new KafkaInboundChannelModel();
        Set<String> topics = new LinkedHashSet<>();
        topics.add(topic);
        channelModel.setTopics(topics);
        this.channelModel = channelModel;
        this.channelModel.setKey(key);
        return new InboundKafkaChannelBuilderImpl(channelModel, eventRepository, this);
    }
    
    @Override
    public EventDeployment deploy() {
        if (resourceName == null) {
            resourceName = "inbound-" + key + ".channel";
        }
        
        ChannelModel channelModel = buildChannelModel();

        EventDeployment eventDeployment = eventRepository.createDeployment()
            .name(deploymentName)
            .addChannelDefinition(resourceName, channelJsonConverter.convertToJson(channelModel))
            .category(category)
            .parentDeploymentId(parentDeploymentId)
            .tenantId(deploymentTenantId)
            .deploy();

        return eventDeployment;
    }
    
    protected ChannelModel buildChannelModel() {
        if (this.channelModel == null) {
            channelModel = new InboundChannelModel();
        }

        channelModel.setKey(key);

        return channelModel;
    }

    public static class InboundJmsChannelBuilderImpl implements InboundJmsChannelBuilder {

        protected final EventRepositoryService eventRepositoryService;
        protected final InboundChannelDefinitionBuilderImpl channelDefinitionBuilder;

        protected JmsInboundChannelModel jmsChannel;

        public InboundJmsChannelBuilderImpl(JmsInboundChannelModel jmsChannel, EventRepositoryService eventRepositoryService, 
                        InboundChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            
            this.jmsChannel = jmsChannel;
            this.eventRepositoryService = eventRepositoryService;
            this.channelDefinitionBuilder = channelDefinitionBuilder;
        }

        @Override
        public InboundJmsChannelBuilder selector(String selector) {
            jmsChannel.setSelector(selector);
            return this;
        }

        @Override
        public InboundJmsChannelBuilder subscription(String subscription) {
            jmsChannel.setSubscription(subscription);
            return this;
        }

        @Override
        public InboundJmsChannelBuilder concurrency(String concurrency) {
            jmsChannel.setConcurrency(concurrency);
            return this;
        }

        @Override
        public InboundEventProcessingPipelineBuilder eventProcessingPipeline() {
            channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(jmsChannel,
                            eventRepositoryService, channelDefinitionBuilder);
            return channelDefinitionBuilder.inboundEventProcessingPipelineBuilder;
        }
    }

    public static class InboundRabbitChannelBuilderImpl implements InboundRabbitChannelBuilder {

        protected final EventRepositoryService eventRepositoryService;
        protected final InboundChannelDefinitionBuilderImpl channelDefinitionBuilder;

        protected RabbitInboundChannelModel rabbitChannel;

        public InboundRabbitChannelBuilderImpl(RabbitInboundChannelModel rabbitChannel, EventRepositoryService eventRepositoryService, 
                        InboundChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            
            this.rabbitChannel = rabbitChannel;
            this.eventRepositoryService = eventRepositoryService;
            this.channelDefinitionBuilder = channelDefinitionBuilder;
        }

        @Override
        public InboundRabbitChannelBuilder exclusive(boolean exclusive) {
            this.rabbitChannel.setExclusive(exclusive);
            return this;
        }

        @Override
        public InboundRabbitChannelBuilder priority(String priority) {
            this.rabbitChannel.setPriority(priority);
            return this;
        }

        @Override
        public InboundRabbitChannelBuilder admin(String admin) {
            this.rabbitChannel.setAdmin(admin);
            return this;
        }

        @Override
        public InboundRabbitChannelBuilder concurrency(String concurrency) {
            rabbitChannel.setConcurrency(concurrency);
            return this;
        }

        @Override
        public InboundRabbitChannelBuilder executor(String executor) {
            this.rabbitChannel.setExecutor(executor);
            return this;
        }

        @Override
        public InboundRabbitChannelBuilder ackMode(String ackMode) {
            this.rabbitChannel.setAckMode(ackMode);
            return this;
        }

        @Override
        public InboundEventProcessingPipelineBuilder eventProcessingPipeline() {
            channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(rabbitChannel, eventRepositoryService,
                            channelDefinitionBuilder);
            return channelDefinitionBuilder.inboundEventProcessingPipelineBuilder;
        }
    }

    public static class InboundKafkaChannelBuilderImpl implements InboundKafkaChannelBuilder {

        protected final EventRepositoryService eventRepositoryService;
        protected final InboundChannelDefinitionBuilderImpl channelDefinitionBuilder;

        protected KafkaInboundChannelModel kafkaChannel;

        public InboundKafkaChannelBuilderImpl(KafkaInboundChannelModel kafkaChannel, EventRepositoryService eventRepositoryService, 
                        InboundChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            
            this.kafkaChannel = kafkaChannel;
            this.eventRepositoryService = eventRepositoryService;
            this.channelDefinitionBuilder = channelDefinitionBuilder;
        }

        @Override
        public InboundKafkaChannelBuilder groupId(String groupId) {
            kafkaChannel.setGroupId(groupId);
            return this;
        }

        @Override
        public InboundKafkaChannelBuilder clientIdPrefix(String clientIdPrefix) {
            kafkaChannel.setClientIdPrefix(clientIdPrefix);
            return this;
        }

        @Override
        public InboundKafkaChannelBuilder concurrency(String concurrency) {
            kafkaChannel.setConcurrency(concurrency);
            return this;
        }

        @Override
        public InboundKafkaChannelBuilder property(String name, String value) {
            kafkaChannel.addCustomProperty(name, value);
            return this;
        }

        @Override
        public InboundEventProcessingPipelineBuilder eventProcessingPipeline() {
            channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(kafkaChannel,
                            eventRepositoryService, channelDefinitionBuilder);
            return channelDefinitionBuilder.inboundEventProcessingPipelineBuilder;
        }
    }

    public static class InboundEventProcessingPipelineBuilderImpl<T> implements InboundEventProcessingPipelineBuilder {

        protected EventRepositoryService eventRepository;
        protected InboundChannelDefinitionBuilderImpl channelDefinitionBuilder;
        protected InboundChannelModel channelModel;

        public InboundEventProcessingPipelineBuilderImpl(InboundChannelModel channelModel, EventRepositoryService eventRepository,
                        InboundChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            this.channelModel = channelModel;
            this.eventRepository = eventRepository;
            this.channelDefinitionBuilder = channelDefinitionBuilder;
        }

        @Override
        public InboundEventKeyJsonDetectorBuilder jsonDeserializer() {
            channelModel.setDeserializerType("json");

            InboundEventProcessingPipelineBuilderImpl<JsonNode> jsonPipelineBuilder
                = new InboundEventProcessingPipelineBuilderImpl<>(channelModel, eventRepository, channelDefinitionBuilder);
            this.channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = jsonPipelineBuilder;

            return new InboundEventKeyJsonDetectorBuilderImpl(jsonPipelineBuilder);
        }

        @Override
        public InboundEventKeyXmlDetectorBuilder xmlDeserializer() {
            channelModel.setDeserializerType("xml");
            InboundEventProcessingPipelineBuilderImpl<Document> xmlPipelineBuilder
                = new InboundEventProcessingPipelineBuilderImpl<>(channelModel, eventRepository, channelDefinitionBuilder);
            this.channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = xmlPipelineBuilder;

            return new InboundEventKeyXmlDetectorBuilderImpl(xmlPipelineBuilder);
        }

        @Override
        public InboundEventKeyDetectorBuilder delegateExpressionDeserializer(String delegateExpression) {
            channelModel.setDeserializerType("expression");
            channelModel.setDeserializerDelegateExpression(delegateExpression);
            InboundEventProcessingPipelineBuilderImpl customPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(channelModel,
                eventRepository, channelDefinitionBuilder);
            this.channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = customPipelineBuilder;
            return new InboundEventDefinitionKeyDetectorBuilderImpl(customPipelineBuilder);
        }

        @Override
        public InboundChannelModelBuilder eventProcessingPipeline(String delegateExpression) {
            this.channelModel.setPipelineDelegateExpression(delegateExpression);
            return channelDefinitionBuilder;
        }

    }

    public static class InboundEventKeyJsonDetectorBuilderImpl implements InboundEventKeyJsonDetectorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder;

        public InboundEventKeyJsonDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTenantJsonDetectorBuilder fixedEventKey(String key) {
            ChannelEventKeyDetection keyDetection = new ChannelEventKeyDetection();
            keyDetection.setFixedValue(key);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventKeyDetection(keyDetection);
            return new InboundEventTenantJsonDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventTenantJsonDetectorBuilder detectEventKeyUsingJsonField(String field) {
            ChannelEventKeyDetection keyDetection = new ChannelEventKeyDetection();
            keyDetection.setJsonField(field);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventKeyDetection(keyDetection);
            return new InboundEventTenantJsonDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventTenantJsonDetectorBuilder detectEventKeyUsingJsonPointerExpression(String jsonPointerExpression) {
            ChannelEventKeyDetection keyDetection = new ChannelEventKeyDetection();
            keyDetection.setJsonPointerExpression(jsonPointerExpression);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventKeyDetection(keyDetection);
            return new InboundEventTenantJsonDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }
    }

    public static class InboundEventKeyXmlDetectorBuilderImpl implements InboundEventKeyXmlDetectorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<Document> inboundEventProcessingPipelineBuilder;

        public InboundEventKeyXmlDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<Document> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTenantXmlDetectorBuilder fixedEventKey(String key) {
            ChannelEventKeyDetection keyDetection = new ChannelEventKeyDetection();
            keyDetection.setFixedValue(key);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventKeyDetection(keyDetection);
            return new InboundEventTenantXmlDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventTenantXmlDetectorBuilder detectEventKeyUsingXPathExpression(String xPathExpression) {
            ChannelEventKeyDetection keyDetection = new ChannelEventKeyDetection();
            keyDetection.setXmlXPathExpression(xPathExpression);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventKeyDetection(keyDetection);
            return new InboundEventTenantXmlDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventDefinitionKeyDetectorBuilderImpl implements InboundEventKeyDetectorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder;

        public InboundEventDefinitionKeyDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTenantDetectorBuilder delegateExpressionKeyDetector(String delegateExpression) {
            ChannelEventKeyDetection keyDetection = new ChannelEventKeyDetection();
            keyDetection.setDelegateExpression(delegateExpression);
            inboundEventProcessingPipelineBuilder.channelModel.setChannelEventKeyDetection(keyDetection);
            return new InboundEventTenantDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventTenantJsonDetectorBuilderImpl
            extends InboundEventPayloadJsonExtractorBuilderImpl implements InboundEventTenantJsonDetectorBuilder {

        public InboundEventTenantJsonDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder) {
            super(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadJsonExtractorBuilder fixedTenantId(String tenantId) {
            ChannelEventTenantIdDetection tenantIdDetection = new ChannelEventTenantIdDetection();
            tenantIdDetection.setFixedValue(tenantId);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventTenantIdDetection(tenantIdDetection);
            return new InboundEventPayloadJsonExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadJsonExtractorBuilder detectEventTenantUsingJsonPointerExpression(String jsonPointerExpression) {
            ChannelEventTenantIdDetection tenantIdDetection = new ChannelEventTenantIdDetection();
            tenantIdDetection.setJsonPointerExpression(jsonPointerExpression);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventTenantIdDetection(tenantIdDetection);
            return new InboundEventPayloadJsonExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventTenantXmlDetectorBuilderImpl
            extends InboundEventPayloadXmlExtractorBuilderImpl implements InboundEventTenantXmlDetectorBuilder {

        public InboundEventTenantXmlDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<Document> inboundEventProcessingPipelineBuilder) {
            super(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadXmlExtractorBuilder fixedTenantId(String tenantId) {
            ChannelEventTenantIdDetection tenantIdDetection = new ChannelEventTenantIdDetection();
            tenantIdDetection.setFixedValue(tenantId);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventTenantIdDetection(tenantIdDetection);
            return new InboundEventPayloadXmlExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadXmlExtractorBuilder detectEventTenantUsingXPathExpression(String xPathExpression) {
            ChannelEventTenantIdDetection tenantIdDetection = new ChannelEventTenantIdDetection();
            tenantIdDetection.setxPathExpression(xPathExpression);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventTenantIdDetection(tenantIdDetection);
            return new InboundEventPayloadXmlExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventTenantDetectorBuilderImpl
            extends InboundEventPayloadExtractorBuilderImpl implements InboundEventTenantDetectorBuilder {

        public InboundEventTenantDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder) {
            super(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadExtractorBuilder fixedTenantId(String tenantId) {
            ChannelEventTenantIdDetection tenantIdDetection = new ChannelEventTenantIdDetection();
            tenantIdDetection.setFixedValue(tenantId);
            this.inboundEventProcessingPipelineBuilder.channelModel.setChannelEventTenantIdDetection(tenantIdDetection);
            return new InboundEventPayloadExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadExtractorBuilder delegateExpressionTenantDetector(String delegateExpression) {
            ChannelEventTenantIdDetection tenantIdDetection = new ChannelEventTenantIdDetection();
            tenantIdDetection.setDelegateExpression(delegateExpression);
            inboundEventProcessingPipelineBuilder.channelModel.setChannelEventTenantIdDetection(tenantIdDetection);
            return new InboundEventPayloadExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventPayloadJsonExtractorBuilderImpl implements InboundEventPayloadJsonExtractorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder;

        public InboundEventPayloadJsonExtractorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTransformerBuilder jsonFieldsMapDirectlyToPayload() {
            return new InboundEventTransformerBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventPayloadXmlExtractorBuilderImpl implements InboundEventPayloadXmlExtractorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<Document> inboundEventProcessingPipelineBuilder;

        public InboundEventPayloadXmlExtractorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<Document> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTransformerBuilder xmlElementsMapDirectlyToPayload() {
            return new InboundEventTransformerBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventPayloadExtractorBuilderImpl implements InboundEventPayloadExtractorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder;

        public InboundEventPayloadExtractorBuilderImpl(InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTransformerBuilder payloadExtractor(String delegateExpression) {
            inboundEventProcessingPipelineBuilder.channelModel.setPayloadExtractorDelegateExpression(delegateExpression);
            return new InboundEventTransformerBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventTransformerBuilderImpl implements InboundEventTransformerBuilder {

        protected InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder;

        public InboundEventTransformerBuilderImpl(InboundEventProcessingPipelineBuilderImpl inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundChannelModelBuilder transformer(String delegateExpression) {
            this.inboundEventProcessingPipelineBuilder.channelModel.setEventTransformerDelegateExpression(delegateExpression);
            return this.inboundEventProcessingPipelineBuilder.channelDefinitionBuilder;
        }

        @Override
        public EventDeployment deploy() {
            return this.inboundEventProcessingPipelineBuilder.channelDefinitionBuilder.deploy();
        }

    }

}
