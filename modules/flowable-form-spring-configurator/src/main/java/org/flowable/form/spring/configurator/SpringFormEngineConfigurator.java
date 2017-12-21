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

import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.configurator.FormEngineConfigurator;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.spring.common.SpringEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringFormEngineConfigurator extends FormEngineConfigurator {

    protected SpringFormEngineConfiguration formEngineConfiguration;

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (formEngineConfiguration == null) {
            formEngineConfiguration = new SpringFormEngineConfiguration();
        }
        initialiseCommonProperties(engineConfiguration, formEngineConfiguration);
        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        formEngineConfiguration.setTransactionManager(springEngineConfiguration.getTransactionManager());

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

    @Override
    public SpringFormEngineConfiguration getFormEngineConfiguration() {
        return formEngineConfiguration;
    }

    public SpringFormEngineConfigurator setFormEngineConfiguration(SpringFormEngineConfiguration formEngineConfiguration) {
        this.formEngineConfiguration = formEngineConfiguration;
        return this;
    }

}
