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
package org.flowable.eventregistry.impl.management;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.InboundChannelModelCacheManager;
import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionManager;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class DefaultEventRegistryChangeDetectionManager implements EventRegistryChangeDetectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventRegistryChangeDetectionManager.class);

    protected EventRegistryEngineConfiguration eventRegistryEngineConfiguration;

    public DefaultEventRegistryChangeDetectionManager(EventRegistryEngineConfiguration eventRegistryEngineConfiguration) {
        this.eventRegistryEngineConfiguration = eventRegistryEngineConfiguration;
    }

    @Override
    public void detectChanges() {
        // This query could be optimized in the future by keeping a timestamp of the last query
        // and querying by createtime (but detecting deletes would need dedicated logic!).
        // The amount of channel definitions however, should typically not be large.
        List<ChannelDefinition> channelDefinitions = eventRegistryEngineConfiguration.getEventRepositoryService()
            .createChannelDefinitionQuery()
            .latestVersion()
            .list();

        InboundChannelModelCacheManager inboundChannelModelCacheManager = eventRegistryEngineConfiguration.getInboundChannelModelCacheManager();
        Collection<String> latestChannelDefinitionIds = new HashSet<>();

        // Check for new deployments
        for (ChannelDefinition channelDefinition : channelDefinitions) {
            InboundChannelModelCacheManager.RegisteredChannel registeredChannel = inboundChannelModelCacheManager.findRegisteredChannel(channelDefinition);
            if (registeredChannel != null && registeredChannel.getChannelDefinitionVersion() > channelDefinition.getVersion()) {
                // If the registered channel has a version higher than the latest one then we need to remove it
                // This can happen when a deployment was reverted (i.e. a newer deployment was deleted)
                ChannelDefinition unregisteredChannel = eventRegistryEngineConfiguration.getDeploymentManager().removeChannelDefinitionFromCache(registeredChannel.getChannelDefinitionId());
                if (unregisteredChannel != null) {
                    LOGGER.info("Unregistered channel definition with key {} and tenant {} from cache", unregisteredChannel.getKey(), unregisteredChannel.getTenantId());
                }
            }
            latestChannelDefinitionIds.add(channelDefinition.getId());
            // If the registered channel IDs does not contain the latest version we need to deploy it
            // fetching the channel model by ID will trigger its deployment. If it does not we will manually do it
            ChannelModel channelModel = eventRegistryEngineConfiguration.getEventRepositoryService().getChannelModelById(channelDefinition.getId());
            if (channelModel instanceof InboundChannelModel && inboundChannelModelCacheManager.findRegisteredChannel(channelDefinition) == null) {
                // The model has not been registered in the inbound cache manager
                // This means that most likely a newer deployment was deleted
                // We need to manually register it.
                eventRegistryEngineConfiguration.getCommandExecutor()
                        .execute(commandContext -> {
                            EventRegistryEngineConfiguration eventRegistryConfiguration = CommandContextUtil.getEventRegistryConfiguration(commandContext);
                            eventRegistryConfiguration
                                    .getEventDeployer()
                                    .getCachingAndArtifcatsManager()
                                    .registerChannelModel(channelModel, channelDefinition, eventRegistryEngineConfiguration);
                            return null;
                        });
            }
            LOGGER.info("Deployed channel definition with key {} and tenant {}", channelDefinition.getKey(), channelDefinition.getTenantId());
        }

        // Once the latest definitions are deployed we need to see if there are any lingering older channels that should be unregistered
        Collection<InboundChannelModelCacheManager.RegisteredChannel> registeredChannels = inboundChannelModelCacheManager
                .getRegisteredChannels();

        for (InboundChannelModelCacheManager.RegisteredChannel registeredChannel : registeredChannels) {
            if (!latestChannelDefinitionIds.contains(registeredChannel.getChannelDefinitionId())) {
                // If a registered channel is not within the latest channel definitions then we need to unregister it
                // This can happen when all deployments for a particular channel have been removed
                // The cache is a synchronized map (default impl), so no need to synchronize, both adds (during deployment) and remove (here) are synchronized
                ChannelDefinition unregisteredChannel = eventRegistryEngineConfiguration.getDeploymentManager().removeChannelDefinitionFromCache(registeredChannel.getChannelDefinitionId());
                if (unregisteredChannel != null) {
                    LOGGER.info("Unregistered channel definition with key {} and tenant {} from cache", unregisteredChannel.getKey(), unregisteredChannel.getTenantId());
                }
            }
        }
    }

}
