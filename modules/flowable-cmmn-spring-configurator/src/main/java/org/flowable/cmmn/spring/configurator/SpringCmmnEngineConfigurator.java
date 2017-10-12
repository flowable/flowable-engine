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
package org.flowable.cmmn.spring.configurator;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.configurator.CmmnEngineConfigurator;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.cmmn.spring.SpringCmmnExpressionManager;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.spring.SpringProcessEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringCmmnEngineConfigurator extends CmmnEngineConfigurator {

    protected SpringCmmnEngineConfiguration cmmnEngineConfiguration;

    @Override
    public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (cmmnEngineConfiguration == null) {
            cmmnEngineConfiguration = new SpringCmmnEngineConfiguration();
        }
        initialiseCommonProperties(processEngineConfiguration, cmmnEngineConfiguration);
        
        SpringProcessEngineConfiguration springProcessEngineConfiguration = (SpringProcessEngineConfiguration) processEngineConfiguration;
        cmmnEngineConfiguration.setTransactionManager(springProcessEngineConfiguration.getTransactionManager());
        cmmnEngineConfiguration.setExpressionManager(new SpringCmmnExpressionManager(
                        springProcessEngineConfiguration.getApplicationContext(), springProcessEngineConfiguration.getBeans()));

        initCmmnEngine();
        
        initServiceConfigurations(processEngineConfiguration, cmmnEngineConfiguration);
    }

    @Override
    protected synchronized CmmnEngine initCmmnEngine() {
        if (cmmnEngineConfiguration == null) {
            throw new FlowableException("CmmnEngineConfiguration is required");
        }

        return cmmnEngineConfiguration.buildCmmnEngine();
    }

    @Override
    public SpringCmmnEngineConfiguration getCmmnEngineConfiguration() {
        return cmmnEngineConfiguration;
    }

    public SpringCmmnEngineConfigurator setCmmnEngineConfiguration(SpringCmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        return this;
    }

}
