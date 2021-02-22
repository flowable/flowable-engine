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
package org.flowable.eventregistry.spring.configurator;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.spring.SpringEngineConfiguration;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.configurator.EventRegistryEngineConfigurator;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringEventRegistryConfigurator extends EventRegistryEngineConfigurator {

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (eventEngineConfiguration == null) {
            eventEngineConfiguration = new SpringEventRegistryEngineConfiguration();

        } else if (!(eventEngineConfiguration instanceof SpringEventRegistryEngineConfiguration)) {
            throw new IllegalArgumentException("Expected eventRegistryEngine configuration to be of type "
                + SpringEventRegistryEngineConfiguration.class + " but was " + eventEngineConfiguration.getClass());

        }

        initialiseCommonProperties(engineConfiguration, eventEngineConfiguration);
        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        ((SpringEventRegistryEngineConfiguration) eventEngineConfiguration).setTransactionManager(springEngineConfiguration.getTransactionManager());

        if (eventEngineConfiguration.getBeans() == null) {
            eventEngineConfiguration.setBeans(engineConfiguration.getBeans());
        }

        initEventRegistryEngine();
        initServiceConfigurations(engineConfiguration, eventEngineConfiguration);
    }

    @Override
    protected synchronized EventRegistryEngine initEventRegistryEngine() {
        if (eventEngineConfiguration == null) {
            throw new FlowableException("EventRegistryEngineConfiguration is required");
        }

        return eventEngineConfiguration.buildEventRegistryEngine();
    }
}
