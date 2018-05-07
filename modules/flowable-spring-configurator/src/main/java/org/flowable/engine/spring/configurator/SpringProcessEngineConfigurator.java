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
package org.flowable.engine.spring.configurator;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.spring.SpringEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.configurator.ProcessEngineConfigurator;
import org.flowable.spring.SpringExpressionManager;
import org.flowable.spring.SpringProcessEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringProcessEngineConfigurator extends ProcessEngineConfigurator {

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (processEngineConfiguration == null) {
            processEngineConfiguration = new SpringProcessEngineConfiguration();
        }

        if (!(processEngineConfiguration instanceof SpringProcessEngineConfiguration)) {
            throw new FlowableException("SpringProcessEngineConfigurator accepts only SpringProcessEngineConfiguration. " + processEngineConfiguration.getClass().getName());
        }

        initialiseCommonProperties(engineConfiguration, processEngineConfiguration);

        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        SpringProcessEngineConfiguration springProcessEngineConfiguration = (SpringProcessEngineConfiguration) processEngineConfiguration;
        springProcessEngineConfiguration.setTransactionManager(springEngineConfiguration.getTransactionManager());
        springProcessEngineConfiguration.setExpressionManager(new SpringExpressionManager(
                        springEngineConfiguration.getApplicationContext(), springEngineConfiguration.getBeans()));

        initProcessEngine();

        initServiceConfigurations(engineConfiguration, processEngineConfiguration);
    }

    @Override
    protected synchronized ProcessEngine initProcessEngine() {
        if (processEngineConfiguration == null) {
            throw new FlowableException("ProcessEngineConfiguration is required");
        }

        return processEngineConfiguration.buildProcessEngine();
    }

    @Override
    public SpringProcessEngineConfiguration getProcessEngineConfiguration() {
        return (SpringProcessEngineConfiguration) processEngineConfiguration;
    }

    public SpringProcessEngineConfigurator setProcessEngineConfiguration(SpringProcessEngineConfiguration processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
        return this;
    }

}
