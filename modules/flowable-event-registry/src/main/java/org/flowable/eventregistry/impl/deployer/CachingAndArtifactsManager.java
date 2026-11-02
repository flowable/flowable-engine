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
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.api.InboundChannelModelCacheManager;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.deploy.ChannelDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.EventDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.EventDeploymentManager;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventregistry.json.converter.EventJsonConverter;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;

/**
 * Updates caches and artifacts for a deployment and its event and channel definitions
 */
public class CachingAndArtifactsManager {

    public void removeChannelDefinitionFromCache(String channelDefinitionId) {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = CommandContextUtil.getEventRegistryConfiguration();
        DeploymentCache<ChannelDefinitionCacheEntry> channelDefinitionCache = eventRegistryEngineConfiguration.getDeploymentManager().getChannelDefinitionCache();
        channelDefinitionCache.remove(channelDefinitionId);
    }

    /**
     * Ensures that the event and channel definitions are cached in the appropriate places, including the deployment's collection of deployed artifacts and the deployment manager's cache.
     */
    public void updateCachingAndArtifacts(ParsedDeployment parsedDeployment) {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = CommandContextUtil.getEventRegistryConfiguration();
        DeploymentCache<EventDefinitionCacheEntry> eventDefinitionCache = eventRegistryEngineConfiguration.getDeploymentManager().getEventDefinitionCache();
        EventDeploymentEntity deployment = parsedDeployment.getDeployment();

        EventJsonConverter eventJsonConverter = eventRegistryEngineConfiguration.getEventJsonConverter();
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
            ChannelDefinitionCacheEntry cacheEntry = new ChannelDefinitionCacheEntry(channelDefinition, channelModel);
            channelDefinitionCache.add(channelDefinition.getId(), cacheEntry);
            
            registerChannelModel(channelModel, channelDefinition, eventRegistryEngineConfiguration);

            // Add to deployment for further usage
            deployment.addDeployedArtifact(channelDefinition);
        }
    }
    
    public void registerChannelModel(ChannelModel channelModel, ChannelDefinition channelDefinition, EventRegistryEngineConfiguration eventRegistryEngineConfiguration) {
        String channelDefinitionKey = channelModel.getKey();
        if (StringUtils.isEmpty(channelDefinitionKey)) {
            throw new FlowableIllegalArgumentException("No key set for channel model");
        }

        if (channelModel instanceof InboundChannelModel inboundChannelModel) {

            if (inboundChannelModel.getInboundEventChannelAdapter() != null) {
                InboundEventChannelAdapter inboundEventChannelAdapter = (InboundEventChannelAdapter) inboundChannelModel.getInboundEventChannelAdapter();
                inboundEventChannelAdapter.setEventRegistry(eventRegistryEngineConfiguration.getEventRegistry());
                inboundEventChannelAdapter.setInboundChannelModel(inboundChannelModel);
            }

        } else if (!(channelModel instanceof OutboundChannelModel)) {
            throw new FlowableIllegalArgumentException("Unrecognized ChannelModel class : " + channelModel.getClass());
        }

        InboundChannelModelCacheManager.ChannelRegistration channelRegistration = null;
        if (channelModel instanceof InboundChannelModel) {
            channelRegistration = eventRegistryEngineConfiguration.getInboundChannelModelCacheManager()
                    .registerChannelModel((InboundChannelModel) channelModel, channelDefinition);
        }

        boolean channelRegistered = channelRegistration == null || channelRegistration.registered();
        for (ChannelModelProcessor channelDefinitionProcessor : eventRegistryEngineConfiguration.getChannelModelProcessors()) {
            boolean canProcessChannel;
            if (channelRegistered) {
                canProcessChannel = channelDefinitionProcessor.canProcess(channelModel);
            } else {
                canProcessChannel = channelDefinitionProcessor.canProcessIfChannelModelAlreadyRegistered(channelModel);
            }

            if (canProcessChannel) {
                channelDefinitionProcessor.unregisterChannelModel(channelModel, channelDefinition.getTenantId(),
                        eventRegistryEngineConfiguration.getEventRepositoryService());
                try {
                    channelDefinitionProcessor.registerChannelModel(channelModel, channelDefinition.getTenantId(),
                            eventRegistryEngineConfiguration.getEventRegistry(),
                            eventRegistryEngineConfiguration.getEventRepositoryService(),
                            eventRegistryEngineConfiguration.isFallbackToDefaultTenant());
                } catch (RuntimeException ex) {
                    // Registering a channel can lead to an exception
                    // In such a case we need to roll back the channel registration and re-register the previous channel
                    // The reason for that is that previous channel should keep listening i.e. JMS connection should exist
                    if (channelRegistration != null) {
                        // If there was a channel registration then we need to rollback it
                        channelRegistration.rollback();

                        InboundChannelModelCacheManager.RegisteredChannel previousChannel = channelRegistration.previousChannel();
                        if (previousChannel != null) {
                            // If there was a previous channel in the registration then we actually need to re-register that one
                            EventDeploymentManager deploymentManager = eventRegistryEngineConfiguration.getDeploymentManager();
                            ChannelDefinitionEntity previousChannelDefinition = deploymentManager
                                    .findDeployedChannelDefinitionById(previousChannel.getChannelDefinitionId());

                            ChannelDefinitionCacheEntry cacheEntry = deploymentManager.resolveChannelDefinition(previousChannelDefinition);
                            ChannelModel previousChannelModel = cacheEntry.getChannelModel();
                            if (previousChannelModel instanceof InboundChannelModel) {
                                channelDefinitionProcessor.registerChannelModel(previousChannelModel, previousChannelDefinition.getTenantId(),
                                        eventRegistryEngineConfiguration.getEventRegistry(), eventRegistryEngineConfiguration.getEventRepositoryService(),
                                        eventRegistryEngineConfiguration.isFallbackToDefaultTenant());
                            }

                        }
                    }
                    throw ex;
                }
            }
        }
    }
}
