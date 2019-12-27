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
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.api.model.InboundChannelDefinitionBuilder;
import org.flowable.eventregistry.impl.keydetector.JsonFieldBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.keydetector.JsonPathBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.keydetector.StaticKeyDetector;
import org.flowable.eventregistry.impl.keydetector.XpathBasedInboundEventKeyDetector;
import org.flowable.eventregistry.impl.payload.JsonFieldToMapPayloadExtractor;
import org.flowable.eventregistry.impl.payload.XmlElementsToMapPayloadExtractor;
import org.flowable.eventregistry.impl.pipeline.DefaultInboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.serialization.StringToJsonDeserializer;
import org.flowable.eventregistry.impl.serialization.StringToXmlDocumentDeserializer;
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
public class InboundChannelDefinitionBuilderImpl implements InboundChannelDefinitionBuilder {

    protected EventRegistry eventRegistry;

    protected InboundChannelModel channelDefinition;
    protected String key;
    protected InboundEventChannelAdapter inboundEventChannelAdapter;
    protected InboundEventProcessingPipelineBuilder inboundEventProcessingPipelineBuilder;

    public InboundChannelDefinitionBuilderImpl(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public InboundChannelDefinitionBuilder key(String key) {
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
        JmsInboundChannelModel channelDefinition = new JmsInboundChannelModel();
        channelDefinition.setDestination(destinationName);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new InboundJmsChannelBuilderImpl(channelDefinition, eventRegistry, this);
    }

    @Override
    public InboundRabbitChannelBuilder rabbitChannelAdapter(String queueName) {
        RabbitInboundChannelModel channelDefinition = new RabbitInboundChannelModel();
        Set<String> queues = new LinkedHashSet<>();
        queues.add(queueName);
        channelDefinition.setQueues(queues);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new InboundRabbitChannelBuilderImpl(channelDefinition, eventRegistry, this);
    }

    @Override
    public InboundKafkaChannelBuilder kafkaChannelAdapter(String topic) {
        KafkaInboundChannelModel channelDefinition = new KafkaInboundChannelModel();
        Set<String> topics = new LinkedHashSet<>();
        topics.add(topic);
        channelDefinition.setTopics(topics);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new InboundKafkaChannelBuilderImpl(channelDefinition, eventRegistry, this);
    }

    @Override
    public InboundChannelModel register() {
        if (this.channelDefinition == null) {
            channelDefinition = new InboundChannelModel();
        }

        channelDefinition.setKey(key);
        channelDefinition.setInboundEventChannelAdapter(inboundEventChannelAdapter);

        InboundEventProcessingPipeline inboundEventProcessingPipeline = inboundEventProcessingPipelineBuilder.build();
        channelDefinition.setInboundEventProcessingPipeline(inboundEventProcessingPipeline);

        eventRegistry.registerChannelDefinition(channelDefinition);

        return channelDefinition;
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
        public InboundChannelDefinitionBuilder eventProcessingPipeline(InboundEventProcessingPipeline inboundEventProcessingPipeline) {
            this.customInboundEventProcessingPipeline = inboundEventProcessingPipeline;
            return channelDefinitionBuilder;
        }

        public InboundEventProcessingPipeline build() {
            if (customInboundEventProcessingPipeline != null) {
                return customInboundEventProcessingPipeline;
            } else {
                return new DefaultInboundEventProcessingPipeline<>(eventRegistry,
                    inboundEventDeserializer, inboundEventKeyDetector, inboundEventPayloadExtractor, inboundEventTransformer);
            }
        }

    }

    public static class InboundEventKeyJsonDetectorBuilderImpl implements InboundEventKeyJsonDetectorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder;

        public InboundEventKeyJsonDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<JsonNode> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventPayloadJsonExtractorBuilder fixedEventKey(String key) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new StaticKeyDetector(key);
            return new InboundEventPayloadJsonExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadJsonExtractorBuilder detectEventKeyUsingJsonField(String field) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new JsonFieldBasedInboundEventKeyDetector(field);
            return new InboundEventPayloadJsonExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadJsonExtractorBuilder detectEventKeyUsingJsonPathExpression(String jsonPathExpression) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new JsonPathBasedInboundEventKeyDetector(jsonPathExpression);
            return new InboundEventPayloadJsonExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }
    }

    public static class InboundEventKeyXmlDetectorBuilderImpl implements InboundEventKeyXmlDetectorBuilder {

        protected InboundEventProcessingPipelineBuilderImpl<Document> inboundEventProcessingPipelineBuilder;

        public InboundEventKeyXmlDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<Document> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventPayloadXmlExtractorBuilder fixedEventKey(String key) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new StaticKeyDetector(key);
            return new InboundEventPayloadXmlExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

        @Override
        public InboundEventPayloadXmlExtractorBuilder detectEventKeyUsingXPathExpression(String xpathExpression) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = new XpathBasedInboundEventKeyDetector(xpathExpression);
            return new InboundEventPayloadXmlExtractorBuilderImpl(inboundEventProcessingPipelineBuilder);
        }

    }

    public static class InboundEventDefinitionKeyDetectorBuilderImpl<T> implements InboundEventKeyDetectorBuilder<T> {

        protected InboundEventProcessingPipelineBuilderImpl<T> inboundEventProcessingPipelineBuilder;

        public InboundEventDefinitionKeyDetectorBuilderImpl(InboundEventProcessingPipelineBuilderImpl<T> inboundEventProcessingPipelineBuilder) {
            this.inboundEventProcessingPipelineBuilder = inboundEventProcessingPipelineBuilder;
        }

        @Override
        public InboundEventPayloadExtractorBuilder<T> detectEventKeyUsingKeyDetector(InboundEventKeyDetector<T> inboundEventKeyDetector) {
            this.inboundEventProcessingPipelineBuilder.inboundEventKeyDetector = inboundEventKeyDetector;
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
        public InboundChannelDefinitionBuilder transformer(InboundEventTransformer inboundEventTransformer) {
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
