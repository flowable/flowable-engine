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
import org.flowable.eventregistry.api.model.OutboundChannelDefinitionBuilder;
import org.flowable.eventregistry.impl.pipeline.DefaultOutboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.serialization.EventPayloadToJsonStringSerializer;
import org.flowable.eventregistry.impl.serialization.EventPayloadToXmlStringSerializer;
import org.flowable.eventregistry.model.JmsOutboundChannelDefinition;
import org.flowable.eventregistry.model.KafkaOutboundChannelDefinition;
import org.flowable.eventregistry.model.OutboundChannelDefinition;
import org.flowable.eventregistry.model.RabbitOutboundChannelDefinition;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class OutboundChannelDefinitionBuilderImpl implements OutboundChannelDefinitionBuilder {

    protected EventRegistry eventRegistry;

    protected OutboundChannelDefinition channelDefinition;
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
        JmsOutboundChannelDefinition channelDefinition = new JmsOutboundChannelDefinition();
        channelDefinition.setDestination(destination);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundJmsChannelBuilderImpl(eventRegistry, this, channelDefinition);
    }

    @Override
    public OutboundRabbitChannelBuilder rabbitChannelAdapter(String routingKey) {
        RabbitOutboundChannelDefinition channelDefinition = new RabbitOutboundChannelDefinition();
        channelDefinition.setRoutingKey(routingKey);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundRabbitChannelBuilderImpl(eventRegistry, this, channelDefinition);
    }

    @Override
    public OutboundKafkaChannelBuilder kafkaChannelAdapter(String topic) {
        KafkaOutboundChannelDefinition channelDefinition = new KafkaOutboundChannelDefinition();
        channelDefinition.setTopic(topic);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundKafkaChannelBuilderImpl(eventRegistry, this, channelDefinition);
    }

    @Override
    public OutboundChannelDefinition register() {
        OutboundChannelDefinition outboundChannelDefinition;
        if (this.channelDefinition == null) {
            outboundChannelDefinition = new OutboundChannelDefinition();
        } else {
            outboundChannelDefinition = this.channelDefinition;
        }

        outboundChannelDefinition.setKey(key);
        outboundChannelDefinition.setOutboundEventChannelAdapter(outboundEventChannelAdapter);

        OutboundEventProcessingPipeline outboundEventProcessingPipeline = this.outboundEventProcessingPipelineBuilder.build();
        outboundChannelDefinition.setOutboundEventProcessingPipeline(outboundEventProcessingPipeline);

        eventRegistry.registerChannelDefinition(outboundChannelDefinition);

        return outboundChannelDefinition;
    }

    public static class OutboundJmsChannelBuilderImpl implements OutboundJmsChannelBuilder {

        protected final EventRegistry eventRegistry;
        protected final OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected JmsOutboundChannelDefinition jmsChannel;

        public OutboundJmsChannelBuilderImpl(EventRegistry eventRegistry, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
            JmsOutboundChannelDefinition jmsChannel) {
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

        protected RabbitOutboundChannelDefinition rabbitChannel;

        public OutboundRabbitChannelBuilderImpl(EventRegistry eventRegistry, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
            RabbitOutboundChannelDefinition rabbitChannel) {
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

        protected KafkaOutboundChannelDefinition kafkaChannel;

        public OutboundKafkaChannelBuilderImpl(EventRegistry eventRegistry, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
            KafkaOutboundChannelDefinition kafkaChannel) {
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
        public OutboundChannelDefinitionBuilder jsonSerializer() {
            this.outboundEventSerializer = new EventPayloadToJsonStringSerializer();
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelDefinitionBuilder xmlSerializer() {
            this.outboundEventSerializer = new EventPayloadToXmlStringSerializer();
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelDefinitionBuilder serializer(OutboundEventSerializer serializer) {
            this.outboundEventSerializer = serializer;
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelDefinitionBuilder eventProcessingPipeline(OutboundEventProcessingPipeline outboundEventProcessingPipeline) {
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
