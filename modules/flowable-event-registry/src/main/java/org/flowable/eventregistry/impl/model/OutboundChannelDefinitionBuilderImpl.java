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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventProcessingPipeline;
import org.flowable.eventregistry.api.OutboundEventSerializer;
import org.flowable.eventregistry.api.model.OutboundChannelModelBuilder;
import org.flowable.eventregistry.impl.pipeline.DefaultOutboundEventProcessingPipeline;
import org.flowable.eventregistry.impl.serialization.EventPayloadToJsonStringSerializer;
import org.flowable.eventregistry.impl.serialization.EventPayloadToXmlStringSerializer;
import org.flowable.eventregistry.json.converter.ChannelJsonConverter;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.JmsOutboundChannelModel;
import org.flowable.eventregistry.model.KafkaOutboundChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;
import org.flowable.eventregistry.model.RabbitOutboundChannelModel;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class OutboundChannelDefinitionBuilderImpl implements OutboundChannelModelBuilder {

    protected EventRepositoryService eventRepository;

    protected OutboundChannelModel channelDefinition;
    protected String deploymentName;
    protected String resourceName;
    protected String category;
    protected String parentDeploymentId;
    protected String deploymentTenantId;
    protected String key;
    protected OutboundEventChannelAdapter outboundEventChannelAdapter;
    protected OutboundEventProcessingPipelineBuilder outboundEventProcessingPipelineBuilder;

    public OutboundChannelDefinitionBuilderImpl(EventRepositoryService eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public OutboundChannelModelBuilder key(String key) {
        this.key = key;
        return this;
    }
    
    @Override
    public OutboundChannelModelBuilder deploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
        return this;
    }
    
    @Override
    public OutboundChannelModelBuilder resourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }
    
    @Override
    public OutboundChannelModelBuilder category(String category) {
        this.category = category;
        return this;
    }
    
    @Override
    public OutboundChannelModelBuilder parentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }
    
    @Override
    public OutboundChannelModelBuilder deploymentTenantId(String deploymentTenantId) {
        this.deploymentTenantId = deploymentTenantId;
        return this;
    }
    
    @Override
    public OutboundEventProcessingPipelineBuilder channelAdapter(OutboundEventChannelAdapter outboundEventChannelAdapter) {
        this.outboundEventChannelAdapter = outboundEventChannelAdapter;
        this.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRepository, this);
        return this.outboundEventProcessingPipelineBuilder;
    }

    @Override
    public OutboundJmsChannelBuilder jmsChannelAdapter(String destination) {
        JmsOutboundChannelModel channelDefinition = new JmsOutboundChannelModel();
        channelDefinition.setDestination(destination);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundJmsChannelBuilderImpl(eventRepository, this, channelDefinition);
    }

    @Override
    public OutboundRabbitChannelBuilder rabbitChannelAdapter(String routingKey) {
        RabbitOutboundChannelModel channelDefinition = new RabbitOutboundChannelModel();
        channelDefinition.setRoutingKey(routingKey);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundRabbitChannelBuilderImpl(eventRepository, this, channelDefinition);
    }

    @Override
    public OutboundKafkaChannelBuilder kafkaChannelAdapter(String topic) {
        KafkaOutboundChannelModel channelDefinition = new KafkaOutboundChannelModel();
        channelDefinition.setTopic(topic);
        this.channelDefinition = channelDefinition;
        this.channelDefinition.setKey(key);
        return new OutboundKafkaChannelBuilderImpl(eventRepository, this, channelDefinition);
    }
    
    @Override
    public EventDeployment deploy() {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("A resource name is mandatory");
        }
        
        ChannelModel channelModel = buildChannelModel();

        EventDeployment eventDeployment = eventRepository.createDeployment()
            .name(deploymentName)
            .addChannelDefinition(resourceName, new ChannelJsonConverter().convertToJson(channelModel))
            .category(category)
            .parentDeploymentId(parentDeploymentId)
            .tenantId(deploymentTenantId)
            .deploy();

        return eventDeployment;
    }

    public OutboundChannelModel buildChannelModel() {
        OutboundChannelModel outboundChannelModel;
        if (this.channelDefinition == null) {
            outboundChannelModel = new OutboundChannelModel();
        } else {
            outboundChannelModel = this.channelDefinition;
        }

        outboundChannelModel.setKey(key);
        outboundChannelModel.setOutboundEventChannelAdapter(outboundEventChannelAdapter);

        OutboundEventProcessingPipeline outboundEventProcessingPipeline = this.outboundEventProcessingPipelineBuilder.build();
        outboundChannelModel.setOutboundEventProcessingPipeline(outboundEventProcessingPipeline);
        
        OutboundEventSerializer eventSerializer = outboundEventProcessingPipeline.getOutboundEventSerializer();
        if (eventSerializer != null) {
            outboundChannelModel.setSerializerType(eventSerializer.getType());
        }

        return outboundChannelModel;
    }

    public static class OutboundJmsChannelBuilderImpl implements OutboundJmsChannelBuilder {

        protected final EventRepositoryService eventRepositoryService;
        protected final OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected JmsOutboundChannelModel jmsChannel;

        public OutboundJmsChannelBuilderImpl(EventRepositoryService eventRepositoryService, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
                        JmsOutboundChannelModel jmsChannel) {
            
            this.eventRepositoryService = eventRepositoryService;
            this.outboundChannelDefinitionBuilder = outboundChannelDefinitionBuilder;
            this.jmsChannel = jmsChannel;
        }

        @Override
        public OutboundEventProcessingPipelineBuilder eventProcessingPipeline() {
            outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRepositoryService, outboundChannelDefinitionBuilder);
            return outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder;
        }
    }

    public static class OutboundRabbitChannelBuilderImpl implements OutboundRabbitChannelBuilder {

        protected final EventRepositoryService eventRepositoryService;
        protected final OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected RabbitOutboundChannelModel rabbitChannel;

        public OutboundRabbitChannelBuilderImpl(EventRepositoryService eventRepositoryService, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
                        RabbitOutboundChannelModel rabbitChannel) {
            
            this.eventRepositoryService = eventRepositoryService;
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
            outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRepositoryService,
                outboundChannelDefinitionBuilder);
            return outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder;
        }
    }

    public static class OutboundKafkaChannelBuilderImpl implements OutboundKafkaChannelBuilder {

        protected final EventRepositoryService eventRepositoryService;
        protected final OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected KafkaOutboundChannelModel kafkaChannel;

        public OutboundKafkaChannelBuilderImpl(EventRepositoryService eventRepositoryService, OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
                        KafkaOutboundChannelModel kafkaChannel) {
            
            this.eventRepositoryService = eventRepositoryService;
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
            outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder = new OutboundEventProcessingPipelineBuilderImpl(eventRepositoryService,
                outboundChannelDefinitionBuilder);
            return outboundChannelDefinitionBuilder.outboundEventProcessingPipelineBuilder;
        }
    }

    public static class OutboundEventProcessingPipelineBuilderImpl implements OutboundEventProcessingPipelineBuilder {

        protected EventRepositoryService eventRepositoryService;
        protected OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected OutboundEventSerializer outboundEventSerializer;
        protected OutboundEventProcessingPipeline customOutboundEventProcessingPipeline;

        public OutboundEventProcessingPipelineBuilderImpl(EventRepositoryService eventRepositoryService,
                        OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder) {
            
            this.eventRepositoryService = eventRepositoryService;
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
                return new DefaultOutboundEventProcessingPipeline(eventRepositoryService, outboundEventSerializer);
            }
        }

    }

}
