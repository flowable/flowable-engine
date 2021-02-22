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

import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.model.OutboundChannelModelBuilder;
import org.flowable.eventregistry.json.converter.ChannelJsonConverter;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.DelegateExpressionOutboundChannelModel;
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
    protected ChannelJsonConverter channelJsonConverter;

    protected OutboundChannelModel channelDefinition;
    protected String deploymentName;
    protected String resourceName;
    protected String category;
    protected String parentDeploymentId;
    protected String deploymentTenantId;
    protected String key;

    public OutboundChannelDefinitionBuilderImpl(EventRepositoryService eventRepository, ChannelJsonConverter channelJsonConverter) {
        this.eventRepository = eventRepository;
        this.channelJsonConverter = channelJsonConverter;
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
    public OutboundEventProcessingPipelineBuilder channelAdapter(String delegateExpression) {
        DelegateExpressionOutboundChannelModel channelDefinition = new DelegateExpressionOutboundChannelModel();
        channelDefinition.setAdapterDelegateExpression(delegateExpression);
        this.channelDefinition = channelDefinition;
        return new OutboundEventProcessingPipelineBuilderImpl(this, channelDefinition);
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
            resourceName = "outbound-" + key + ".channel";
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

    public OutboundChannelModel buildChannelModel() {
        OutboundChannelModel outboundChannelModel;
        if (this.channelDefinition == null) {
            outboundChannelModel = new OutboundChannelModel();
        } else {
            outboundChannelModel = this.channelDefinition;
        }

        outboundChannelModel.setKey(key);

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
            return new OutboundEventProcessingPipelineBuilderImpl(outboundChannelDefinitionBuilder, jmsChannel);
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
            return new OutboundEventProcessingPipelineBuilderImpl(outboundChannelDefinitionBuilder, rabbitChannel);
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
            return new OutboundEventProcessingPipelineBuilderImpl(outboundChannelDefinitionBuilder, kafkaChannel);
        }
    }

    public static class OutboundEventProcessingPipelineBuilderImpl implements OutboundEventProcessingPipelineBuilder {

        protected OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder;

        protected OutboundChannelModel outboundChannel;

        public OutboundEventProcessingPipelineBuilderImpl(OutboundChannelDefinitionBuilderImpl outboundChannelDefinitionBuilder,
            OutboundChannelModel outboundChannel) {
            this.outboundChannelDefinitionBuilder = outboundChannelDefinitionBuilder;
            this.outboundChannel = outboundChannel;
        }

        @Override
        public OutboundChannelModelBuilder jsonSerializer() {
            this.outboundChannel.setSerializerType("json");
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelModelBuilder xmlSerializer() {
            this.outboundChannel.setSerializerType("xml");
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelModelBuilder delegateExpressionSerializer(String delegateExpression) {
            this.outboundChannel.setSerializerType("expression");
            this.outboundChannel.setSerializerDelegateExpression(delegateExpression);
            return outboundChannelDefinitionBuilder;
        }

        @Override
        public OutboundChannelModelBuilder eventProcessingPipeline(String delegateExpression) {
            this.outboundChannel.setPipelineDelegateExpression(delegateExpression);
            return outboundChannelDefinitionBuilder;
        }

    }

}
