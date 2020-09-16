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
package org.flowable.eventregistry.impl;

import java.util.List;

import org.flowable.common.engine.api.engine.EngineLifecycleListener;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventManagementService;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.model.ChannelModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class EventRegistryEngineImpl implements EventRegistryEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryEngineImpl.class);

    protected String name;
    protected EventRepositoryService repositoryService;
    protected EventManagementService managementService;
    protected EventRegistry eventRegistry;
    protected EventRegistryEngineConfiguration engineConfiguration;

    public EventRegistryEngineImpl(EventRegistryEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
        this.name = engineConfiguration.getEngineName();
        this.repositoryService = engineConfiguration.getEventRepositoryService();
        this.managementService = engineConfiguration.getEventManagementService();
        this.eventRegistry = engineConfiguration.getEventRegistry();
        
        if (engineConfiguration.getSchemaManagementCmd() != null) {
            engineConfiguration.getCommandExecutor().execute(engineConfiguration.getSchemaCommandConfig(), engineConfiguration.getSchemaManagementCmd());
        }

        if (name == null) {
            LOGGER.info("default flowable EventRegistryEngine created");
        } else {
            LOGGER.info("EventRegistryEngine {} created", name);
        }

        EventRegistryEngines.registerEventRegistryEngine(this);

        if (engineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : engineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineBuilt(this);
            }
        }
    }
    
    @Override
    public void handleDeployedChannelDefinitions() {
        // Fetching and deploying all existing channel definitions at bootup
        List<ChannelDefinition> channelDefinitions = repositoryService.createChannelDefinitionQuery().latestVersion().list();
        for (ChannelDefinition channelDefinition : channelDefinitions) {
            // Getting the channel model will trigger a deployment and set up the channel and associated adapters
            ChannelModel channelModel = repositoryService.getChannelModelById(channelDefinition.getId());
            LOGGER.info("Booted up channel {} ", channelModel.getKey());
        }
    }

    @Override
    public void close() {
        EventRegistryEngines.unregister(this);

        if (engineConfiguration.getEventRegistryChangeDetectionExecutor() != null) {
            engineConfiguration.getEventRegistryChangeDetectionExecutor().shutdown();
        }

        engineConfiguration.close();

        if (engineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : engineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineClosed(this);
            }
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getName() {
        return name;
    }

    @Override
    public EventRepositoryService getEventRepositoryService() {
        return repositoryService;
    }
    
    @Override
    public EventManagementService getEventManagementService() {
        return managementService;
    }

    @Override
    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    @Override
    public EventRegistryEngineConfiguration getEventRegistryEngineConfiguration() {
        return engineConfiguration;
    }
}
