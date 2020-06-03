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
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.deploy.ChannelDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.EventDefinitionCacheEntry;
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
    
    protected void registerChannelModel(ChannelModel channelModel, ChannelDefinition channelDefinition, EventRegistryEngineConfiguration eventRegistryEngineConfiguration) {
        String channelDefinitionKey = channelModel.getKey();
        if (StringUtils.isEmpty(channelDefinitionKey)) {
            throw new FlowableIllegalArgumentException("No key set for channel model");
        }

        if (channelModel instanceof InboundChannelModel) {

            InboundChannelModel inboundChannelModel = (InboundChannelModel) channelModel;

            if (inboundChannelModel.getInboundEventChannelAdapter() != null) {
                InboundEventChannelAdapter inboundEventChannelAdapter = (InboundEventChannelAdapter) inboundChannelModel.getInboundEventChannelAdapter();
                inboundEventChannelAdapter.setEventRegistry(eventRegistryEngineConfiguration.getEventRegistry());
                inboundEventChannelAdapter.setInboundChannelModel(inboundChannelModel);
            }

        } else if (!(channelModel instanceof OutboundChannelModel)) {
            throw new FlowableIllegalArgumentException("Unrecognized ChannelModel class : " + channelModel.getClass());
        }

        for (ChannelModelProcessor channelDefinitionProcessor : eventRegistryEngineConfiguration.getChannelModelProcessors()) {
            if (channelDefinitionProcessor.canProcess(channelModel)) {
                channelDefinitionProcessor.unregisterChannelModel(channelModel, channelDefinition.getTenantId(), eventRegistryEngineConfiguration.getEventRepositoryService());
                channelDefinitionProcessor.registerChannelModel(channelModel, channelDefinition.getTenantId(), eventRegistryEngineConfiguration.getEventRegistry(),
                                eventRegistryEngineConfiguration.getEventRepositoryService(), eventRegistryEngineConfiguration.isFallbackToDefaultTenant());
            }
        }
    }
}
