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
package org.flowable.eventregistry.impl.deployer;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder.InboundEventKeyJsonDetectorBuilder;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder.InboundEventKeyXmlDetectorBuilder;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder.InboundEventPayloadJsonExtractorBuilder;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder.InboundEventPayloadXmlExtractorBuilder;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder.InboundEventProcessingPipelineBuilder;
import org.flowable.eventregistry.api.model.InboundChannelModelBuilder.InboundEventTransformerBuilder;
import org.flowable.eventregistry.api.model.OutboundChannelModelBuilder;
import org.flowable.eventregistry.api.model.OutboundChannelModelBuilder.OutboundEventProcessingPipelineBuilder;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.deploy.ChannelDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.EventDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventregistry.json.converter.ChannelJsonConverter;
import org.flowable.eventregistry.json.converter.EventJsonConverter;
import org.flowable.eventregistry.model.ChannelEventKeyDetection;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;

/**
 * Updates caches and artifacts for a deployment and its event and channel definitions
 */
public class CachingAndArtifactsManager {

    protected EventJsonConverter eventJsonConverter = new EventJsonConverter();
    protected ChannelJsonConverter channelJsonConverter = new ChannelJsonConverter();

    /**
     * Ensures that the event and channel definitions are cached in the appropriate places, including the deployment's collection of deployed artifacts and the deployment manager's cache.
     */
    public void updateCachingAndArtifacts(ParsedDeployment parsedDeployment) {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = CommandContextUtil.getEventRegistryConfiguration();
        DeploymentCache<EventDefinitionCacheEntry> eventDefinitionCache = eventRegistryEngineConfiguration.getDeploymentManager().getEventDefinitionCache();
        EventDeploymentEntity deployment = parsedDeployment.getDeployment();

        for (EventDefinitionEntity eventDefinition : parsedDeployment.getAllEventDefinitions()) {
            EventModel eventModel = parsedDeployment.getEventModelForEventDefinition(eventDefinition);
            EventDefinitionCacheEntry cacheEntry = new EventDefinitionCacheEntry(eventDefinition, eventJsonConverter.convertToJson(eventModel));
            eventDefinitionCache.add(eventDefinition.getId(), cacheEntry);

            // Add to deployment for further usage
            deployment.addDeployedArtifact(eventDefinition);
        }
        
        DeploymentCache<ChannelDefinitionCacheEntry> channelDefinitionCache = eventRegistryEngineConfiguration.getDeploymentManager().getChannelDefinitionCache();

        for (ChannelDefinitionEntity channelDefinition : parsedDeployment.getAllChannelDefinitions()) {
            ChannelModel channelModel = parsedDeployment.getChannelModelForChannelDefinition(channelDefinition);
            ChannelDefinitionCacheEntry cacheEntry = new ChannelDefinitionCacheEntry(channelDefinition, channelJsonConverter.convertToJson(channelModel));
            channelDefinitionCache.add(channelDefinition.getId(), cacheEntry);
            
            registerChannelModel(channelModel, eventRegistryEngineConfiguration);

            // Add to deployment for further usage
            deployment.addDeployedArtifact(channelDefinition);
        }
    }
    
    protected void registerChannelModel(ChannelModel channelModel, EventRegistryEngineConfiguration eventRegistryEngineConfiguration) {
        if ("outbound".equalsIgnoreCase(channelModel.getChannelType())) {
            OutboundChannelModelBuilder outboundChannelModelBuilder = eventRegistryEngineConfiguration.getEventRegistry()
                            .newOutboundChannelModel()
                            .key(channelModel.getKey());
            
            if (StringUtils.isEmpty(channelModel.getDestination())) {
                throw new FlowableException("A destination is required");
            }
            
            OutboundEventProcessingPipelineBuilder outboundEventProcessingPipelineBuilder = null;
            if ("jms".equalsIgnoreCase(channelModel.getType())) {
                outboundEventProcessingPipelineBuilder = outboundChannelModelBuilder.jmsChannelAdapter(channelModel.getDestination())
                                .eventProcessingPipeline();
                
            } else if ("rabbit".equalsIgnoreCase(channelModel.getType())) {
                outboundEventProcessingPipelineBuilder = outboundChannelModelBuilder.rabbitChannelAdapter(channelModel.getDestination())
                                .eventProcessingPipeline();
                
            } else if ("kafka".equalsIgnoreCase(channelModel.getType())) {
                outboundEventProcessingPipelineBuilder = outboundChannelModelBuilder.kafkaChannelAdapter(channelModel.getDestination())
                                .eventProcessingPipeline();
            
            } else {
                throw new FlowableException("No supported channel model type was found " + channelModel.getType());
            }
            
            if ("json".equalsIgnoreCase(channelModel.getSerializerType())) {
                outboundEventProcessingPipelineBuilder.jsonSerializer();
            
            } else if ("xml".equalsIgnoreCase(channelModel.getSerializerType())) {
                outboundEventProcessingPipelineBuilder.xmlSerializer();
            
            } else {
                throw new FlowableException("The serializer type is not supported " + channelModel.getSerializerType());
            }
            
            outboundChannelModelBuilder.register();
            
        } else {
            InboundChannelModelBuilder inboundChannelModelBuilder = eventRegistryEngineConfiguration.getEventRegistry()
                    .newInboundChannelModel()
                    .key(channelModel.getKey());
            
            if (StringUtils.isEmpty(channelModel.getDestination())) {
                throw new FlowableException("A destination is required");
            }
            
            InboundEventProcessingPipelineBuilder inboundEventProcessingPipelineBuilder = null;
            if ("jms".equalsIgnoreCase(channelModel.getType())) {
                inboundEventProcessingPipelineBuilder = inboundChannelModelBuilder.jmsChannelAdapter(channelModel.getDestination())
                                .eventProcessingPipeline();
                
            } else if ("rabbit".equalsIgnoreCase(channelModel.getType())) {
                inboundEventProcessingPipelineBuilder = inboundChannelModelBuilder.rabbitChannelAdapter(channelModel.getDestination())
                                .eventProcessingPipeline();
                
            } else if ("kafka".equalsIgnoreCase(channelModel.getType())) {
                inboundEventProcessingPipelineBuilder = inboundChannelModelBuilder.kafkaChannelAdapter(channelModel.getDestination())
                                .eventProcessingPipeline();
            
            } else {
                throw new FlowableException("No supported channel model type was found " + channelModel.getType());
            }
            
            if (channelModel.getChannelEventKeyDetection() == null) {
                throw new FlowableException("A channel key detection value is required");
            }
            
            ChannelEventKeyDetection channelEventKeyDetection = channelModel.getChannelEventKeyDetection();
                
            InboundEventTransformerBuilder eventTransformerBuilder = null;
            if ("json".equalsIgnoreCase(channelModel.getDeserializerType())) {
                InboundEventKeyJsonDetectorBuilder jsonDetectorBuilder = inboundEventProcessingPipelineBuilder.jsonDeserializer();
                
                InboundEventPayloadJsonExtractorBuilder extractorBuilder = null;
                if (StringUtils.isNotEmpty(channelEventKeyDetection.getFixedValue())) {
                    extractorBuilder = jsonDetectorBuilder.fixedEventKey(channelEventKeyDetection.getFixedValue());
                
                } else if (StringUtils.isNotEmpty(channelEventKeyDetection.getJsonField())) {
                    extractorBuilder = jsonDetectorBuilder.detectEventKeyUsingJsonField(channelEventKeyDetection.getJsonField());
                
                } else if (StringUtils.isNotEmpty(channelEventKeyDetection.getJsonPathExpression())) {
                    extractorBuilder = jsonDetectorBuilder.detectEventKeyUsingJsonPathExpression(channelEventKeyDetection.getJsonPathExpression());
                
                } else {
                    throw new FlowableException("The channel key detection value was found");
                }
                
                eventTransformerBuilder = extractorBuilder.jsonFieldsMapDirectlyToPayload();
            
            } else if ("xml".equalsIgnoreCase(channelModel.getDeserializerType())) {
                InboundEventKeyXmlDetectorBuilder xmlDetectorBuilder = inboundEventProcessingPipelineBuilder.xmlDeserializer();
                
                InboundEventPayloadXmlExtractorBuilder extractorBuilder = null;
                if (StringUtils.isNotEmpty(channelEventKeyDetection.getFixedValue())) {
                    extractorBuilder = xmlDetectorBuilder.fixedEventKey(channelEventKeyDetection.getFixedValue());
                
                } else if (StringUtils.isNotEmpty(channelEventKeyDetection.getXmlXPathExpression())) {
                    extractorBuilder = xmlDetectorBuilder.detectEventKeyUsingXPathExpression(channelEventKeyDetection.getXmlXPathExpression());
                
                } else {
                    throw new FlowableException("The channel key detection value was found");
                }
                
                eventTransformerBuilder = extractorBuilder.xmlElementsMapDirectlyToPayload();
            
            } else {
                throw new FlowableException("The deserializer type is not supported " + channelModel.getDeserializerType());
            }
            
            eventTransformerBuilder.register();
        }
    }
}
