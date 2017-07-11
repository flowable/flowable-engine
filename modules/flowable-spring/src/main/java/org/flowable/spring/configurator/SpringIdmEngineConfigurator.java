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

import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.cfg.IdmEngineConfigurator;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;

/**
 * @author Tijs Rademakers
 */
public class SpringIdmEngineConfigurator extends IdmEngineConfigurator {

    protected SpringIdmEngineConfiguration idmEngineConfiguration;

    @Override
    public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (idmEngineConfiguration == null) {
            idmEngineConfiguration = new SpringIdmEngineConfiguration();
        }
        initialiseCommonProperties(processEngineConfiguration, idmEngineConfiguration, EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
        idmEngineConfiguration.setTransactionManager(((SpringProcessEngineConfiguration) processEngineConfiguration).getTransactionManager());


        idmEngineConfiguration.buildIdmEngine();
    }

    public IdmEngineConfiguration getIdmEngineConfiguration() {
        return idmEngineConfiguration;
    }

    public SpringIdmEngineConfigurator setIdmEngineConfiguration(SpringIdmEngineConfiguration idmEngineConfiguration) {
        this.idmEngineConfiguration = idmEngineConfiguration;
        return this;
    }

}
