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
package org.flowable.form.spring.configurator;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.spring.SpringEngineConfiguration;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.configurator.FormEngineConfigurator;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.form.spring.SpringFormExpressionManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringFormEngineConfigurator extends FormEngineConfigurator {

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (formEngineConfiguration == null) {
            formEngineConfiguration = new SpringFormEngineConfiguration();
        } else if (!(formEngineConfiguration instanceof SpringFormEngineConfiguration)) {
            throw new IllegalArgumentException("Expected formEngine configuration to be of type "
                + SpringFormEngineConfiguration.class + " but was " + formEngineConfiguration.getClass());
        }
        initialiseCommonProperties(engineConfiguration, formEngineConfiguration);
        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        ((SpringFormEngineConfiguration) formEngineConfiguration).setTransactionManager(springEngineConfiguration.getTransactionManager());
        if (formEngineConfiguration.getExpressionManager() == null) {
            formEngineConfiguration.setExpressionManager(new SpringFormExpressionManager(
                springEngineConfiguration.getApplicationContext(), springEngineConfiguration.getBeans()));
        }

        initFormEngine();
        
        initServiceConfigurations(engineConfiguration, formEngineConfiguration);
    }

    @Override
    protected synchronized FormEngine initFormEngine() {
        if (formEngineConfiguration == null) {
            throw new FlowableException("FormEngineConfiguration is required");
        }

        return formEngineConfiguration.buildFormEngine();
    }
}
