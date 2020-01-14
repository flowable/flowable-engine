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

import java.util.Date;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventManagementService;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
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

        EventRegistry eventRegistry = getEventRegistry();
        EventRegistry otherEventRegistry = getOtherProcessEngineEventRegistry();
        assertThat(eventRegistry.getInboundChannelModels()).hasSize(0);
        assertThat(otherEventRegistry.getInboundChannelModels()).hasSize(0);

        // Set the time for both engines to the same start time
        Date startTtime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startTtime);
        otherProcessEngine.getProcessEngineConfiguration().getClock().setCurrentTime(startTtime);

        assertThat(eventRegistryEngine.getEventRepositoryService().createEventDefinitionQuery().count()).isEqualTo(0);

        // Deploy a channel definition on engine1
        EventDeployment engine1Deployment = eventRegistryEngine.getEventRepositoryService().createDeployment().addClasspathResource("org/flowable/engine/test/eventregistry/simpleChannel.channel").deploy();
        assertThat(eventRegistryEngine.getEventRepositoryService().createChannelDefinitionQuery().count()).isEqualTo(1);

        // Should be deployed on engine1, but not yet on engine2
        assertThat(eventRegistry.getInboundChannelModels()).hasSize(1);
        assertThat(otherEventRegistry.getInboundChannelModels()).hasSize(0);

        // Manually trigger the detect changes logic on engine2
        getOtherProcessEngineEventRegistryManagementService().executeEventRegistryChangeDetection();

        assertThat(eventRegistry.getInboundChannelModels()).hasSize(1);
        assertThat(otherEventRegistry.getInboundChannelModels()).hasSize(1);
        assertThat(eventRegistry.getInboundChannelModels().keySet().iterator().next()).isEqualTo(otherEventRegistry.getInboundChannelModels().keySet().iterator().next());

        // Deploying a channel definition on engine2, should have similar consequences on engine1
        EventDeployment engine2Deployment = getOtherProcessEngineEventRegistryRepositoryService()
            .createDeployment().addClasspathResource("org/flowable/engine/test/eventregistry/simpleChannel2.channel").deploy();
        assertThat(getOtherProcessEngineEventRegistryRepositoryService().createChannelDefinitionQuery().count()).isEqualTo(2);

        assertThat(eventRegistry.getInboundChannelModels()).hasSize(1);
        assertThat(otherEventRegistry.getInboundChannelModels()).hasSize(2);

        // Manually trigger the detect changes logic on engine1
        eventRegistryEngine.getEventManagementService().executeEventRegistryChangeDetection();

        assertThat(eventRegistry.getInboundChannelModels()).hasSize(2);
        assertThat(otherEventRegistry.getInboundChannelModels()).hasSize(2);
        assertThat(eventRegistry.getInboundChannelModels().keySet()).contains("myChannel", "myChannel2");
        assertThat(otherEventRegistry.getInboundChannelModels().keySet()).contains("myChannel", "myChannel2");

        // Removing a channel definition on engine1, should be gone on engine2
        eventRegistryEngine.getEventRepositoryService().deleteDeployment(engine1Deployment.getId());
        assertThat(eventRegistry.getInboundChannelModels()).hasSize(1); // removed on engine1
        assertThat(otherEventRegistry.getInboundChannelModels()).hasSize(2); // but not yet on engine2, timer job needs to pass first

        // Manually trigger the detect changes logic on engine2
        getOtherProcessEngineEventRegistryManagementService().executeEventRegistryChangeDetection();

        assertThat(eventRegistry.getInboundChannelModels()).hasSize(1);
        assertThat(otherEventRegistry.getInboundChannelModels()).hasSize(1);
    }

    private EventRegistry getEventRegistry() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRegistry();
    }

    private EventRepositoryService getOtherProcessEngineEventRegistryRepositoryService() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) otherProcessEngine.getProcessEngineConfiguration()
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRepositoryService();
    }

    private EventManagementService getOtherProcessEngineEventRegistryManagementService() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) otherProcessEngine.getProcessEngineConfiguration()
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventManagementService();
    }

    private EventRegistry getOtherProcessEngineEventRegistry() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) otherProcessEngine.getProcessEngineConfiguration()
            .getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRegistry();
    }

}
