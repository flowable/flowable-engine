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
package org.flowable.eventregistry.impl.configurator;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.AbstractEngineConfigurator;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.cfg.StandaloneEventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.configurator.deployer.EventDeployer;
import org.flowable.eventregistry.impl.db.EntityDependencyOrder;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class EventRegistryEngineConfigurator extends AbstractEngineConfigurator {

    protected EventRegistryEngine eventRegistryEngine;
    protected EventRegistryEngineConfiguration eventEngineConfiguration;

    @Override
    public int getPriority() {
        return EngineConfigurationConstants.PRIORITY_ENGINE_EVENT_REGISTRY;
    }

    @Override
    protected List<EngineDeployer> getCustomDeployers() {
        List<EngineDeployer> deployers = new ArrayList<>();
        deployers.add(new EventDeployer());
        return deployers;
    }

    @Override
    protected String getMybatisCfgPath() {
        return EventRegistryEngineConfiguration.DEFAULT_MYBATIS_MAPPING_FILE;
    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (eventEngineConfiguration == null) {
            eventEngineConfiguration = new StandaloneEventRegistryEngineConfiguration();
        }

        initialiseEventRegistryEngineConfiguration(eventEngineConfiguration);
        initialiseCommonProperties(engineConfiguration, eventEngineConfiguration);
        this.eventRegistryEngine = initEventRegistryEngine();
        initServiceConfigurations(engineConfiguration, eventEngineConfiguration);
    }

    protected void initialiseEventRegistryEngineConfiguration(EventRegistryEngineConfiguration eventRegistryEngineConfiguration) {
        // meant to be overridden
    }

    @Override
    protected List<Class<? extends Entity>> getEntityInsertionOrder() {
        return EntityDependencyOrder.INSERT_ORDER;
    }

    @Override
    protected List<Class<? extends Entity>> getEntityDeletionOrder() {
        return EntityDependencyOrder.DELETE_ORDER;
    }

    protected synchronized EventRegistryEngine initEventRegistryEngine() {
        if (eventEngineConfiguration == null) {
            throw new FlowableException("EventRegistryEngineConfiguration is required");
        }

        return eventEngineConfiguration.buildEventRegistryEngine();
    }

    public EventRegistryEngineConfiguration getEventEngineConfiguration() {
        return eventEngineConfiguration;
    }

    public EventRegistryEngineConfigurator setEventEngineConfiguration(EventRegistryEngineConfiguration eventEngineConfiguration) {
        this.eventEngineConfiguration = eventEngineConfiguration;
        return this;
    }

    public EventRegistryEngine getEventRegistryEngine() {
        return eventRegistryEngine;
    }

    public void setEventRegistryEngine(EventRegistryEngine eventRegistryEngine) {
        this.eventRegistryEngine = eventRegistryEngine;
    }
}