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

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.api.model.OutboundChannelModelBuilder;
import org.flowable.eventregistry.impl.pipeline.DefaultOutboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.serialization.EventPayloadToJsonStringSerializer;
import org.flowable.eventregistry.impl.serialization.EventPayloadToXmlStringSerializer;
import org.flowable.eventregistry.model.JmsOutboundChannelModel;
import org.flowable.eventregistry.model.KafkaOutboundChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;
import org.flowable.eventregistry.model.RabbitOutboundChannelModel;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class OutboundChannelDefinitionBuilderImpl implements OutboundChannelModelBuilder {

    protected EventRegistry eventRegistry;

    protected OutboundChannelModel channelDefinition;
    protected String key;
    protected OutboundEventChannelAdapter outboundEventChannelAdapter;
    protected OutboundEventProcessingPipelineBuilder outboundEventProcessingPipelineBuilder;

    public OutboundChannelDefinitionBuilderImpl(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public OutboundChannelDefinitionBuilderImpl key(String key) {
        this.key = key;
        return this;
    }
    @Override
    public OutboundEventProcessingPipelineBuilder channelAdapter(OutboundEventChannelAdapter outboundEventChannelAdapter) {
        this.outboundEventChannelAdapter = outboundEventChannelAdapter;
        this.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRegistry, this);
        return this.outboundEventProcessingPipelineBuilder;
    }

    @Override
    public OutboundJmsChannelBuilder jmsChannelAdapter(String destination) {
        JmsOutboundChannelModel channelDefinition = new JmsOutboundChannelModel();
        channelDefinition.setDestination(destination);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundJmsChannelBuilderImpl(eventRegistry, this, channelDefinition);
    }

    @Override
    public OutboundRabbitChannelBuilder rabbitChannelAdapter(String routingKey) {
        RabbitOutboundChannelModel channelDefinition = new RabbitOutboundChannelModel();
        channelDefinition.setRoutingKey(routingKey);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundRabbitChannelBuilderImpl(eventRegistry, this, channelDefinition);
    }

    @Override
    public OutboundKafkaChannelBuilder kafkaChannelAdapter(String topic) {
        KafkaOutboundChannelModel channelDefinition = new KafkaOutboundChannelModel();
        channelDefinition.setTopic(topic);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundKafkaChannelBuilderImpl(eventRegistry, this, channelDefinition);
    }

    @Override
    public OutboundChannelModel register() {
        OutboundChannelModel outboundChannelDefinition;
        if (this.channelDefinition == null) {
            outboundChannelDefinition = new OutboundChannelModel();
        } else {
            outboundChannelDefinition = this.channelDefinition;
        }

        outboundChannelDefinition.setKey(key);
        outboundChannelDefinition.setOutboundEventChannelAdapter(outboundEventChannelAdapter);

        OutboundEventProcessingPipeline outboundEventProcessingPipeline = this.outboundEventProcessingPipelineBuilder.build();
        outboundChannelDefinition.setOutboundEventProcessingPipeline(outboundEventProcessingPipeline);

        eventRegistry.registerChannelModel(outboundChannelDefinition);

        return outboundChannelDefinition;
    }

    public static class OutboundJmsChannelBuilderImpl implements OutboundJmsChannelBuilder {

        protected final EventRegistry eventRegistry;
        protected final OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected JmsOutboundChannelModel jmsChannel;

        public OutboundJmsChannelBuilderImpl(EventRegistry eventRegistry, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
            JmsOutboundChannelModel jmsChannel) {
            this.eventRegistry = eventRegistry;
            this.outboundChannelDefinitionBuilder = outboundChannelDefinitionBuilder;
            this.jmsChannel = jmsChannel;
        }

        @Override
        public OutboundEventProcessingPipelineBuilder eventProcessingPipeline() {
            outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRegistry, outboundChannelDefinitionBuilder);
            return outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder;
        }
    }

    public static class OutboundRabbitChannelBuilderImpl implements OutboundRabbitChannelBuilder {

        protected final EventRegistry eventRegistry;
        protected final OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected RabbitOutboundChannelModel rabbitChannel;

        public OutboundRabbitChannelBuilderImpl(EventRegistry eventRegistry, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
            RabbitOutboundChannelModel rabbitChannel) {
            this.eventRegistry = eventRegistry;
            this.outboundChannelDefinitionBuilder = outboundChannelDefinitionBuilder;
            this.rabbitChannel = rabbitChannel;
        }

        @Override
        public OutboundRabbitChannelBuilder exchange(String exchange) {
            rabbitChannel.setExchange(exchange);
            return this;
        }

        @Override
        public OutboundEventProcessingPipelineBuilder eventProcessingPipeline() {
            outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRegistry,
                outboundChannelDefinitionBuilder);
            return outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder;
        }
    }

    public static class OutboundKafkaChannelBuilderImpl implements OutboundKafkaChannelBuilder {

        protected final EventRegistry eventRegistry;
        protected final OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected KafkaOutboundChannelModel kafkaChannel;

        public OutboundKafkaChannelBuilderImpl(EventRegistry eventRegistry, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
            KafkaOutboundChannelModel kafkaChannel) {
            this.eventRegistry = eventRegistry;
            this.outboundChannelDefinitionBuilder = outboundChannelDefinitionBuilder;
            this.kafkaChannel = kafkaChannel;
        }

        @Override
        public OutboundKafkaChannelBuilder recordKey(String key) {
            kafkaChannel.setRecordKey(key);
            return this;
        }

        @Override
        public OutboundEventProcessingPipelineBuilder eventProcessingPipeline() {
            outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRegistry,
                outboundChannelDefinitionBuilder);
            return outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder;
        }
    }

    public static class OutboundEventProcessingPipelineBuilderImpl implements OutboundEventProcessingPipelineBuilder {

        protected EventRegistry eventRegistry;
        protected OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected OutboundEventSerializer outboundEventSerializer;
        protected OutboundEventProcessingPipeline customOutboundEventProcessingPipeline;

        public OutboundEventProcessingPipelineBuilderImpl(EventRegistry eventRegistry,
            OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder) {
            this.eventRegistry = eventRegistry;
            this.outboundChannelDefinitionBuilder = outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelModelBuilder jsonSerializer() {
            this.outboundEventSerializer = new EventPayloadToJsonStringSerializer();
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelModelBuilder xmlSerializer() {
            this.outboundEventSerializer = new EventPayloadToXmlStringSerializer();
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelModelBuilder serializer(OutboundEventSerializer serializer) {
            this.outboundEventSerializer = serializer;
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelModelBuilder eventProcessingPipeline(OutboundEventProcessingPipeline outboundEventProcessingPipeline) {
            this.customOutboundEventProcessingPipeline = outboundEventProcessingPipeline;
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundEventProcessingPipeline build() {
            if (customOutboundEventProcessingPipeline != null) {
                return customOutboundEventProcessingPipeline;
            } else {
                return new DefaultOutboundEventProcessingPipeline(eventRegistry, outboundEventSerializer);
            }
        }

    }

}
