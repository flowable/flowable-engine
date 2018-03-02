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
package org.flowable.spring.configurator;

import org.flowable.engine.common.impl.AbstractEngineConfiguration;
import org.flowable.engine.impl.cfg.IdmEngineConfigurator;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.common.SpringEngineConfiguration;

/**
 * @author Tijs Rademakers
 */
public class SpringIdmEngineConfigurator extends IdmEngineConfigurator {

    protected SpringIdmEngineConfiguration idmEngineConfiguration;

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (idmEngineConfiguration == null) {
            idmEngineConfiguration = new SpringIdmEngineConfiguration();
        }
        initialiseCommonProperties(engineConfiguration, idmEngineConfiguration);
        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        idmEngineConfiguration.setTransactionManager(springEngineConfiguration.getTransactionManager());

        idmEngineConfiguration.buildIdmEngine();
        
        initServiceConfigurations(engineConfiguration, idmEngineConfiguration);
    }

    @Override
    public IdmEngineConfiguration getIdmEngineConfiguration() {
        return idmEngineConfiguration;
    }

    public SpringIdmEngineConfigurator setIdmEngineConfiguration(SpringIdmEngineConfiguration idmEngineConfiguration) {
        this.idmEngineConfiguration = idmEngineConfiguration;
        return this;
    }

}
