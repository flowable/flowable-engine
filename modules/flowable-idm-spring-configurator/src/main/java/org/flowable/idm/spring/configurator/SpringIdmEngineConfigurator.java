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
package org.flowable.idm.spring.configurator;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.spring.SpringEngineConfiguration;
import org.flowable.idm.engine.configurator.IdmEngineConfigurator;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;

/**
 * @author Tijs Rademakers
 */
public class SpringIdmEngineConfigurator extends IdmEngineConfigurator {

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (idmEngineConfiguration == null) {
            idmEngineConfiguration = new SpringIdmEngineConfiguration();
        } else if (!(idmEngineConfiguration instanceof SpringIdmEngineConfiguration)) {
            throw new IllegalArgumentException("Expected idmEngine configuration to be of type"
                + SpringIdmEngineConfiguration.class + " but was " + idmEngineConfiguration.getClass());
        }
        initialiseCommonProperties(engineConfiguration, idmEngineConfiguration);
        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        ((SpringIdmEngineConfiguration) idmEngineConfiguration).setTransactionManager(springEngineConfiguration.getTransactionManager());

        idmEngineConfiguration.buildIdmEngine();

        initServiceConfigurations(engineConfiguration, idmEngineConfiguration);
    }

    public SpringIdmEngineConfigurator setIdmEngineConfiguration(SpringIdmEngineConfiguration idmEngineConfiguration) {
        this.idmEngineConfiguration = idmEngineConfiguration;
        return this;
    }

}
