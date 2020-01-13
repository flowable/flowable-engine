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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.management.EventRegistryChangeDetector;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class DefaultEventRegistryChangeDetector implements EventRegistryChangeDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventRegistryChangeDetector.class);

    protected EventRegistryEngineConfiguration eventRegistryEngineConfiguration;
    protected long initialDelayInMs;
    protected long delayInMs;

    protected ScheduledExecutorService scheduledExecutorService;
    protected String threadName = "flowable-event-registry-change-detector-%d";
    protected Runnable changeDetectionRunnable;

    public DefaultEventRegistryChangeDetector(EventRegistryEngineConfiguration eventRegistryEngineConfiguration, long initialDelayInMs, long delayInMs) {
        this.eventRegistryEngineConfiguration = eventRegistryEngineConfiguration;
        this.initialDelayInMs = initialDelayInMs;
        this.delayInMs = delayInMs;
    }

    @Override
    public void initialize() {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern(threadName).build());
        this.changeDetectionRunnable = new EventRegistryChangeDetectionRunnable(this);
        this.scheduledExecutorService.scheduleAtFixedRate(this.changeDetectionRunnable, initialDelayInMs, delayInMs, TimeUnit.MILLISECONDS);
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

        EventRegistry eventRegistry = eventRegistryEngineConfiguration.getEventRegistry();
        Set<String> inboundChannelKeys = eventRegistry.getInboundChannelModels().keySet();
        Set<String> outboundChannelKeys = eventRegistry.getOutboundChannelModels().keySet();

        // Check for new deployments
        for (ChannelDefinition channelDefinition : channelDefinitions) {

            // The key is unique. When no instance is returned, the channel definition has not yet been deployed before (e.g. deployed on another node)
            if (!inboundChannelKeys.contains(channelDefinition.getKey()) && !outboundChannelKeys.contains(channelDefinition.getKey())) {
                eventRegistryEngineConfiguration.getEventRepositoryService().getChannelModelById(channelDefinition.getId());
                LOGGER.info("Deployed channel definition with key {}", channelDefinition.getKey());
            }

        }

        // Check for removed deployments
        Set<String> channelDefinitionKeys = channelDefinitions.stream().map(ChannelDefinition::getKey).collect(Collectors.toSet());
        for (String inboundChannelKey : inboundChannelKeys) {
            if (!channelDefinitionKeys.contains(inboundChannelKey)) {
                eventRegistry.removeChannelModel(inboundChannelKey);
            }
        }
        for (String outboundChannelKey: outboundChannelKeys) {
            if (!channelDefinitionKeys.contains(outboundChannelKey)) {
                eventRegistry.removeChannelModel(outboundChannelKey);
            }
        }
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }
    public String getThreadName() {
        return threadName;
    }
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    public Runnable getChangeDetectionRunnable() {
        return changeDetectionRunnable;
    }
    public void setChangeDetectionRunnable(Runnable changeDetectionRunnable) {
        this.changeDetectionRunnable = changeDetectionRunnable;
    }
}
