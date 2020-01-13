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

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.InboundEventDeserializer;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.InboundEventProcessingPipeline;
import org.flowable.eventregistry.api.InboundEventTenantDetector;
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder;
import org.flowable.eventregistry.impl.keydetector.JsonFieldBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.keydetector.JsonPathBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.keydetector.InboundEventStaticKeyDetector;
import org.flowable.eventregistry.impl.keydetector.XpathBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.payload.JsonFieldToMapPayloadExtractor;
import org.flowable.eventregistry.impl.payload.XmlElementsToMapPayloadExtractor;
import org.flowable.eventregistry.impl.pipeline.DefaultInboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.serialization.StringToJsonDeserializer;
import org.flowable.eventregistry.impl.serialization.StringToXmlDocumentDeserializer;
import org.flowable.eventregistry.impl.tenantdetector.JsonPathBasedInboundEventTenantDetector;
import org.flowable.eventregistry.impl.tenantdetector.InboundEventStaticTenantDetector;
import org.flowable.eventregistry.impl.tenantdetector.XpathBasedInboundEventTenantDetector;
import org.flowable.eventregistry.impl.transformer.DefaultInboundEventTransformer;
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

    protected EventRegistry eventRegistry;

    protected InboundChannelModel channelModel;
    protected String key;
    protected InboundEventChannelAdapter inboundEventChannelAdapter;
    protected InboundEventProcessingPipelineBuilder inboundEventProcessingPipelineBuilder;

    public InboundChannelDefinitionBuilderImpl(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public InboundChannelModelBuilder key(String key) {
        this.key = key;
        return this;
    }

    @Override
    public InboundEventProcessingPipelineBuilder channelAdapter(InboundEventChannelAdapter inboundEventChannelAdapter) {
        this.inboundEventChannelAdapter = inboundEventChannelAdapter;
        this.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(eventRegistry, this);
        return this.inboundEventProcessingPipelineBuilder;
    }

    @Override
    public InboundJmsChannelBuilder jmsChannelAdapter(String destinationName) {
        JmsInboundChannelModel channelModel = new JmsInboundChannelModel();
        channelModel.setDestination(destinationName);
        this.channelModel = channelModel;
        this.channelModel.setKey(key);
        return new InboundJmsChannelBuilderImpl(channelModel, eventRegistry, this);
    }

    @Override
    public InboundRabbitChannelBuilder rabbitChannelAdapter(String queueName) {
        RabbitInboundChannelModel channelModel = new RabbitInboundChannelModel();
        Set<String> queues = new LinkedHashSet<>();
        queues.add(queueName);
        channelModel.setQueues(queues);
        this.channelModel = channelModel;
        this.channelModel.setKey(key);
        return new InboundRabbitChannelBuilderImpl(channelModel, eventRegistry, this);
    }

    @Override
    public InboundKafkaChannelBuilder kafkaChannelAdapter(String topic) {
        KafkaInboundChannelModel channelModel = new KafkaInboundChannelModel();
        Set<String> topics = new LinkedHashSet<>();
        topics.add(topic);
        channelModel.setTopics(topics);
        this.channelModel = channelModel;
        this.channelModel.setKey(key);
        return new InboundKafkaChannelBuilderImpl(channelModel, eventRegistry, this);
    }

    @Override
    public InboundChannelModel register() {
        if (this.channelModel == null) {
            channelModel = new InboundChannelModel();
        }

        channelModel.setKey(key);
        channelModel.setInboundEventChannelAdapter(inboundEventChannelAdapter);

        InboundEventProcessingPipeline inboundEventProcessingPipeline = inboundEventProcessingPipelineBuilder.build();
        channelModel.setInboundEventProcessingPipeline(inboundEventProcessingPipeline);

        eventRegistry.registerChannelModel(channelModel, null);

        return channelModel;
    }

    public static class InboundJmsChannelBuilderImpl implements InboundJmsChannelBuilder {

        protected final EventRegistry eventRegistry;
        protected final InboundChannelDefinitionBuilderImpl channelDefinitionBuilder;

        protected JmsInboundChannelModel jmsChannel;

        public InboundJmsChannelBuilderImpl(JmsInboundChannelModel jmsChannel, EventRegistry eventRegistry, InboundChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            this.jmsChannel = jmsChannel;
            this.eventRegistry = eventRegistry;
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
            channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(eventRegistry, channelDefinitionBuilder);
            return channelDefinitionBuilder.inboundEventProcessingPipelineBuilder;
        }
    }

    public static class InboundRabbitChannelBuilderImpl implements InboundRabbitChannelBuilder {

        protected final EventRegistry eventRegistry;
        protected final InboundChannelDefinitionBuilderImpl channelDefinitionBuilder;

        protected RabbitInboundChannelModel rabbitChannel;

        public InboundRabbitChannelBuilderImpl(RabbitInboundChannelModel rabbitChannel, EventRegistry eventRegistry, InboundChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            this.rabbitChannel = rabbitChannel;
            this.eventRegistry = eventRegistry;
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
            channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(eventRegistry, channelDefinitionBuilder);
            return channelDefinitionBuilder.inboundEventProcessingPipelineBuilder;
        }
    }

    public static class InboundKafkaChannelBuilderImpl implements InboundKafkaChannelBuilder {

        protected final EventRegistry eventRegistry;
        protected final InboundChannelDefinitionBuilderImpl channelDefinitionBuilder;

        protected KafkaInboundChannelModel kafkaChannel;

        public InboundKafkaChannelBuilderImpl(KafkaInboundChannelModel kafkaChannel, EventRegistry eventRegistry, InboundChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            this.kafkaChannel = kafkaChannel;
            this.eventRegistry = eventRegistry;
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
            kafkaChannel.addProperty(name, value);
            return this;
        }

        @Override
        public InboundEventProcessingPipelineBuilder eventProcessingPipeline() {
            channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(eventRegistry, channelDefinitionBuilder);
            return channelDefinitionBuilder.inboundEventProcessingPipelineBuilder;
        }
    }

    public static class InboundEventProcessingPipelineBuilderImpl<T> implements InboundEventProcessingPipelineBuilder {

        protected EventRegistry eventRegistry;
        protected InboundChannelDefinitionBuilderImpl channelDefinitionBuilder;

        protected InboundEventProcessingPipeline customInboundEventProcessingPipeline;
        protected InboundEventDeserializer<T> inboundEventDeserializer;
        protected InboundEventKeyDetector<T> inboundEventKeyDetector;
        protected InboundEventTenantDetector<T> inboundEventTenantDetector;
        protected InboundEventPayloadExtractor<T> inboundEventPayloadExtractor;
        protected InboundEventTransformer inboundEventTransformer;

        public InboundEventProcessingPipelineBuilderImpl(EventRegistry eventRegistry, InboundChannelDefinitionBuilderImpl channelDefinitionBuilder) {
            this.eventRegistry = eventRegistry;
            this.channelDefinitionBuilder = channelDefinitionBuilder;
        }

        @Override
        public InboundEventKeyJsonDetectorBuilder jsonDeserializer() {
            InboundEventProcessingPipelineBuilderImpl<JsonNode> jsonPipelineBuilder
                = new InboundEventProcessingPipelineBuilderImpl<>(eventRegistry, channelDefinitionBuilder);
            this.channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = jsonPipelineBuilder;

            jsonPipelineBuilder.inboundEventDeserializer = new StringToJsonDeserializer();
            return new InboundEventKeyJsonDetectorBuilderImpl(jsonPipelineBuilder);
        }

        @Override
        public InboundEventKeyXmlDetectorBuilder xmlDeserializer() {
            InboundEventProcessingPipelineBuilderImpl<Document> xmlPipelineBuilder
                = new InboundEventProcessingPipelineBuilderImpl<>(eventRegistry, channelDefinitionBuilder);
            this.channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = xmlPipelineBuilder;

            xmlPipelineBuilder.inboundEventDeserializer = new StringToXmlDocumentDeserializer();
            return new InboundEventKeyXmlDetectorBuilderImpl(xmlPipelineBuilder);
        }

        @Override
        public <D> InboundEventKeyDetectorBuilder<D> deserializer(InboundEventDeserializer<D> deserializer) {
            InboundEventProcessingPipelineBuilderImpl<D> customPipelineBuilder = new InboundEventProcessingPipelineBuilderImpl<>(eventRegistry,
                channelDefinitionBuilder);
            this.channelDefinitionBuilder.inboundEventProcessingPipelineBuilder = customPipelineBuilder;
            customPipelineBuilder.inboundEventDeserializer = deserializer;
            return new InboundEventDefinitionKeyDetectorBuilderImpl<>(customPipelineBuilder);
        }

        @Override
        public InboundChannelModelBuilder eventProcessingPipeline(InboundEventProcessingPipeline inboundEventProcessingPipeline) {
            this.customInboundEventProcessingPipeline = inboundEventProcessingPipeline;
            return channelDefinitionBuilder;
        }

        public InboundEventProcessingPipeline build() {
            if (customInboundEventProcessingPipeline != null) {
                return customInboundEventProcessingPipeline;
            } else {
                return new DefaultInboundEventProcessingPipeline<>(eventRegistry,
                    inboundEventDeserializer,
                    inboundEventKeyDetector,
                    inboundEventTenantDetector,
                    inboundEventPayloadExtractor,
                    inboundEventTransformer);
            }
        }

    }

    public static class InboundEventKeyJsonDetectorBuilderImpl implements InboundEventKeyJsonDetectorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder;

        public InboundEventKeyJsonDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTenantJsonDetectorBuilder fixedEventKey(String key) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new InboundEventStaticKeyDetector(key);
            return new InboundEventTenantJsonDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventTenantJsonDetectorBuilder detectEventKeyUsingJsonField(String field) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new JsonFieldBasedInboundEventKeyDetector(field);
            return new InboundEventTenantJsonDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventTenantJsonDetectorBuilder detectEventKeyUsingJsonPathExpression(String jsonPathExpression) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new JsonPathBasedInboundEventKeyDetector(jsonPathExpression);
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
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new InboundEventStaticKeyDetector(key);
            return new InboundEventTenantXmlDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventTenantXmlDetectorBuilder detectEventKeyUsingXPathExpression(String xPathExpression) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new XpathBasedInboundEventKeyDetector(xPathExpression);
            return new InboundEventTenantXmlDetectorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventDefinitionKeyDetectorBuilderImpl<T> implements InboundEventKeyDetectorBuilder<T> {

        protected InboundEventProcessingPipelineBuilderImpl<T> inboundEventProcessingPipelineBuilder;

        public InboundEventDefinitionKeyDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<T> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTenantDetectorBuilder<T> detectEventKeyUsingKeyDetector(InboundEventKeyDetector<T> inboundEventKeyDetector) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = inboundEventKeyDetector;
            return new InboundEventTenantDetectorBuilderImpl<>(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventTenantJsonDetectorBuilderImpl
            extends InboundEventPayloadJsonExtractorBuilderImpl implements InboundEventTenantJsonDetectorBuilder {

        public InboundEventTenantJsonDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder) {
            super(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadJsonExtractorBuilder fixedTenantId(String tenantId) {
            inboundEventProcessingPipelineBuilder.inboundEventTenantDetector = new InboundEventStaticTenantDetector(tenantId);
            return new InboundEventPayloadJsonExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadJsonExtractorBuilder detectEventTenantUsingJsonPathExpression(String jsonPathExpression) {
            inboundEventProcessingPipelineBuilder.inboundEventTenantDetector = new JsonPathBasedInboundEventTenantDetector(jsonPathExpression);
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
            inboundEventProcessingPipelineBuilder.inboundEventTenantDetector = new InboundEventStaticTenantDetector(tenantId);
            return new InboundEventPayloadXmlExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadXmlExtractorBuilder detectEventTenantUsingXPathExpression(String xPathExpression) {
            inboundEventProcessingPipelineBuilder.inboundEventTenantDetector = new XpathBasedInboundEventTenantDetector(xPathExpression);
            return new InboundEventPayloadXmlExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventTenantDetectorBuilderImpl<T>
            extends InboundEventPayloadExtractorBuilderImpl<T> implements InboundEventTenantDetectorBuilder<T> {

        public InboundEventTenantDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<T> inboundEventProcessingPipelineBuilder) {
            super(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadExtractorBuilder<T> fixedTenantId(String tenantId) {
            inboundEventProcessingPipelineBuilder.inboundEventTenantDetector = new InboundEventStaticTenantDetector(tenantId);
            return new InboundEventPayloadExtractorBuilderImpl<>(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadExtractorBuilder<T> detectTenantUsingTenantDetector(InboundEventTenantDetector<T> inboundEventTenantDetector) {
            inboundEventProcessingPipelineBuilder.inboundEventTenantDetector = inboundEventTenantDetector;
            return new InboundEventPayloadExtractorBuilderImpl<>(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventPayloadJsonExtractorBuilderImpl implements InboundEventPayloadJsonExtractorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder;

        public InboundEventPayloadJsonExtractorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTransformerBuilder jsonFieldsMapDirectlyToPayload() {
            this.inboundEventProcessingPipelineBuilder.inboundEventPayloadExtractor = new JsonFieldToMapPayloadExtractor();
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
            this.inboundEventProcessingPipelineBuilder.inboundEventPayloadExtractor = new XmlElementsToMapPayloadExtractor();
            return new InboundEventTransformerBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventPayloadExtractorBuilderImpl<T> implements InboundEventPayloadExtractorBuilder<T> {

        protected InboundEventProcessingPipelineBuilderImpl<T> inboundEventProcessingPipelineBuilder;

        public InboundEventPayloadExtractorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<T> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventTransformerBuilder payloadExtractor(InboundEventPayloadExtractor<T> inboundEventPayloadExtractor) {
            this.inboundEventProcessingPipelineBuilder.inboundEventPayloadExtractor = inboundEventPayloadExtractor;
            return new InboundEventTransformerBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventTransformerBuilderImpl implements InboundEventTransformerBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<?> inboundEventProcessingPipelineBuilder;

        public InboundEventTransformerBuilderImpl(InboundEventProcessingPipelineBuilderImpl<?> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundChannelModelBuilder transformer(InboundEventTransformer inboundEventTransformer) {
            this.inboundEventProcessingPipelineBuilder.inboundEventTransformer = inboundEventTransformer;
            return this.inboundEventProcessingPipelineBuilder.channelDefinitionBuilder;
        }

        @Override
        public InboundChannelModel register() {
            this.inboundEventProcessingPipelineBuilder.inboundEventTransformer = new DefaultInboundEventTransformer();
            return this.inboundEventProcessingPipelineBuilder.channelDefinitionBuilder.register();
        }

    }

}
