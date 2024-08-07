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

import org.flowable.cmmn.engine.configurator.CmmnEngineConfigurator;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.spring.SpringEngineConfiguration;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringCmmnEngineConfigurator extends CmmnEngineConfigurator {

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (cmmnEngineConfiguration == null) {
            cmmnEngineConfiguration = new SpringCmmnEngineConfiguration();
        }

        if (!(cmmnEngineConfiguration instanceof SpringCmmnEngineConfiguration)) {
            throw new FlowableException("SpringCmmnEngineConfigurator accepts only SpringCmmnEngineConfiguration. " + cmmnEngineConfiguration.getClass().getName());
        }

        initialiseCommonProperties(engineConfiguration, cmmnEngineConfiguration);

        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        
        SpringProcessEngineConfiguration springProcessEngineConfiguration = null;
        if (springEngineConfiguration instanceof SpringProcessEngineConfiguration) {
            springProcessEngineConfiguration = (SpringProcessEngineConfiguration) springEngineConfiguration;
        } else {
            AbstractEngineConfiguration processEngineConfiguration = engineConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
            if (processEngineConfiguration instanceof SpringProcessEngineConfiguration) {
                springProcessEngineConfiguration = (SpringProcessEngineConfiguration) processEngineConfiguration;
            }
        }
        
        if (springProcessEngineConfiguration != null) {
           copyProcessEngineProperties(springProcessEngineConfiguration);
        }

        ((SpringCmmnEngineConfiguration) cmmnEngineConfiguration).setTransactionManager(springEngineConfiguration.getTransactionManager());
        if (cmmnEngineConfiguration.getBeans() == null) {
            cmmnEngineConfiguration.setBeans(springEngineConfiguration.getBeans());
        }

        initEngine();

        if (springProcessEngineConfiguration != null) {
            cmmnEngineConfiguration.getJobServiceConfiguration().getInternalJobManager()
                    .registerScopedInternalJobManager(ScopeTypes.BPMN, springProcessEngineConfiguration.getJobServiceConfiguration().getInternalJobManager());

            springProcessEngineConfiguration.getJobServiceConfiguration().getInternalJobManager()
                    .registerScopedInternalJobManager(ScopeTypes.CMMN, cmmnEngineConfiguration.getJobServiceConfiguration().getInternalJobManager());
        }

        JobServiceConfiguration engineJobServiceConfiguration = getJobServiceConfiguration(engineConfiguration);
        if (engineJobServiceConfiguration != null) {
            engineJobServiceConfiguration.getInternalJobManager()
                    .registerScopedInternalJobManager(ScopeTypes.CMMN, cmmnEngineConfiguration.getJobServiceConfiguration().getInternalJobManager());
        }

        initServiceConfigurations(engineConfiguration, cmmnEngineConfiguration);
    }

    @Override
    public SpringCmmnEngineConfiguration getCmmnEngineConfiguration() {
        return (SpringCmmnEngineConfiguration) cmmnEngineConfiguration;
    }

    public SpringCmmnEngineConfigurator setCmmnEngineConfiguration(SpringCmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        return this;
    }

}
