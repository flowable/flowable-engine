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
package org.flowable.engine.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventManagementService;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundChannelModelCacheManager;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.configurator.EventRegistryEngineConfigurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * @author Joram Barrez
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventRegistryDataChangeDetectorTest extends PluggableFlowableTestCase {

    private EventRegistryEngine eventRegistryEngine;

    private ProcessEngine otherProcessEngine;

    @BeforeEach
    public void getEventRegistryEngine() {
        EventRegistryEngineConfigurator eventRegistryEngineConfigurator = (EventRegistryEngineConfigurator) processEngineConfiguration.getAllConfigurators()
            .stream().filter(c -> c instanceof EventRegistryEngineConfigurator).findFirst().get();
        this.eventRegistryEngine = eventRegistryEngineConfigurator.getEventRegistryEngine();

        if (otherProcessEngine == null) {
            otherProcessEngine = initOtherProcessEngine();
        }
    }

    protected ProcessEngine initOtherProcessEngine() {
        return ProcessEngineConfiguration
            .createProcessEngineConfigurationFromResource("flowable.cfg.xml") // same datasource
            .setEngineName("otherEngine")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
            .buildProcessEngine();
    }

    @AfterAll
    public void closeOtherEngine() {
        if (otherProcessEngine != null) {
            otherProcessEngine.close();
        }
    }

    @Test
    public void testOtherEnginePicksUpChannelDeploymentsAutomatically() {
        initOtherProcessEngine();

        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        InboundChannelModelCacheManager eventInboundChannelModelCacheManager = getInboundChannelModelCacheManager();
        EventRepositoryService otherEventRepositoryService = getOtherProcessEngineEventRegistryRepositoryService();
        InboundChannelModelCacheManager otherEventInboundChannelModelCacheManager = getOtherProcessEngineEventRegistryInboundChannelModelCacheManager();
        
        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).isEmpty();
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels()).isEmpty();
        
        assertThat(otherEventRepositoryService.createChannelDefinitionQuery().list()).isEmpty();
        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels()).isEmpty();

        // Set the time for both engines to the same start time
        Date startTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startTime);
        otherProcessEngine.getProcessEngineConfiguration().getClock().setCurrentTime(startTime);

        assertThat(eventRegistryEngine.getEventRepositoryService().createEventDefinitionQuery().count()).isZero();

        // Deploy a channel definition on engine1
        EventDeployment engine1Deployment = eventRegistryEngine.getEventRepositoryService().createDeployment().addClasspathResource("org/flowable/engine/test/eventregistry/simpleChannel.channel").deploy();
        assertThat(eventRegistryEngine.getEventRepositoryService().createChannelDefinitionQuery().count()).isEqualTo(1);

        // Should be deployed on engine1, but not yet on engine2
        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).hasSize(1);
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(1);
        
        assertThat(otherEventRepositoryService.createChannelDefinitionQuery().list()).hasSize(1);
        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels()).isEmpty();

        // Manually trigger the detect changes logic on engine2
        getOtherProcessEngineEventRegistryManagementService().executeEventRegistryChangeDetection();

        assertThat(otherEventRepositoryService.createChannelDefinitionQuery().list()).hasSize(1);
        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(1);
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels().iterator().next().getChannelDefinitionId()).isEqualTo(
                        otherEventInboundChannelModelCacheManager.getRegisteredChannels().iterator().next().getChannelDefinitionId());

        // Deploying a channel definition on engine2, should have similar consequences on engine1
        EventDeployment engine2Deployment = getOtherProcessEngineEventRegistryRepositoryService()
            .createDeployment().addClasspathResource("org/flowable/engine/test/eventregistry/simpleChannel2.channel").deploy();
        assertThat(getOtherProcessEngineEventRegistryRepositoryService().createChannelDefinitionQuery().count()).isEqualTo(2);

        List<ChannelDefinition> latestChannelDefinitions = eventRepositoryService.createChannelDefinitionQuery().latestVersion().list();
        assertThat(latestChannelDefinitions)
                .extracting(ChannelDefinition::getKey, ChannelDefinition::getVersion)
                .containsExactlyInAnyOrder(
                        tuple("myChannel", 1),
                        tuple("myChannel2", 1)
                );
        List<String> latestChannelDefinitionIds = latestChannelDefinitions
                .stream()
                .map(ChannelDefinition::getId)
                .collect(Collectors.toList());

        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).hasSize(2);
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(1);
        
        assertThat(otherEventRepositoryService.createChannelDefinitionQuery().list()).hasSize(2);
        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(2);

        // Manually trigger the detect changes logic on engine1
        eventRegistryEngine.getEventManagementService().executeEventRegistryChangeDetection();

        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).hasSize(2);
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(2);
        
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels())
                .extracting(InboundChannelModelCacheManager.RegisteredChannel::getChannelDefinitionId)
                .containsExactlyInAnyOrderElementsOf(latestChannelDefinitionIds);

        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels())
                .extracting(InboundChannelModelCacheManager.RegisteredChannel::getChannelDefinitionId)
                .containsExactlyInAnyOrderElementsOf(latestChannelDefinitionIds);

        // Removing a channel definition on engine1, should be gone on engine2
        eventRegistryEngine.getEventRepositoryService().deleteDeployment(engine1Deployment.getId());
        
        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).hasSize(1); // removed on engine1
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(1);
        
        assertThat(otherEventRepositoryService.createChannelDefinitionQuery().list()).hasSize(1); // but not yet on engine2, timer job needs to pass first
        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(2);

        // Manually trigger the detect changes logic on engine2
        getOtherProcessEngineEventRegistryManagementService().executeEventRegistryChangeDetection();

        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).hasSize(1);
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(1);
        
        assertThat(otherEventRepositoryService.createChannelDefinitionQuery().list()).hasSize(1);
        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(1);

        eventRegistryEngine.getEventRepositoryService().deleteDeployment(engine2Deployment.getId());

        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).isEmpty(); // removed on engine1
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels()).isEmpty();

        assertThat(otherEventRepositoryService.createChannelDefinitionQuery().list()).isEmpty(); // but not yet on engine2, timer job needs to pass first
        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels()).hasSize(1);

        // Manually trigger the detect changes logic on engine2
        getOtherProcessEngineEventRegistryManagementService().executeEventRegistryChangeDetection();

        assertThat(eventRepositoryService.createChannelDefinitionQuery().list()).isEmpty();
        assertThat(eventInboundChannelModelCacheManager.getRegisteredChannels()).isEmpty();

        assertThat(otherEventRepositoryService.createChannelDefinitionQuery().list()).isEmpty();
        assertThat(otherEventInboundChannelModelCacheManager.getRegisteredChannels()).isEmpty();
    }

    protected EventRegistry getEventRegistry() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRegistry();
    }
    
    protected EventRepositoryService getEventRepositoryService() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRepositoryService();
    }
    
    protected InboundChannelModelCacheManager getInboundChannelModelCacheManager() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getInboundChannelModelCacheManager();
    }

    protected EventRepositoryService getOtherProcessEngineEventRegistryRepositoryService() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) otherProcessEngine.getProcessEngineConfiguration()
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRepositoryService();
    }

    protected EventManagementService getOtherProcessEngineEventRegistryManagementService() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) otherProcessEngine.getProcessEngineConfiguration()
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventManagementService();
    }
    
    protected InboundChannelModelCacheManager getOtherProcessEngineEventRegistryInboundChannelModelCacheManager() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) otherProcessEngine.getProcessEngineConfiguration()
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getInboundChannelModelCacheManager();
    }

    protected EventRegistry getOtherProcessEngineEventRegistry() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) otherProcessEngine.getProcessEngineConfiguration()
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRegistry();
    }

}
