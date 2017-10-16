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
package org.flowable.content.spring.configurator;

import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.configurator.ContentEngineConfigurator;
import org.flowable.content.spring.SpringContentEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.spring.SpringProcessEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringContentEngineConfigurator extends ContentEngineConfigurator {

    protected SpringContentEngineConfiguration contentEngineConfiguration;

    @Override
    public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (contentEngineConfiguration == null) {
            contentEngineConfiguration = new SpringContentEngineConfiguration();
        }
        initialiseCommonProperties(processEngineConfiguration, contentEngineConfiguration);
        contentEngineConfiguration.setTransactionManager(((SpringProcessEngineConfiguration) processEngineConfiguration).getTransactionManager());
        
        initContentEngine();
        
        initServiceConfigurations(processEngineConfiguration, contentEngineConfiguration);
    }

    @Override
    protected synchronized ContentEngine initContentEngine() {
        if (contentEngineConfiguration == null) {
            throw new FlowableException("ContentEngineConfiguration is required");
        }

        return contentEngineConfiguration.buildContentEngine();
    }

    @Override
    public SpringContentEngineConfiguration getContentEngineConfiguration() {
        return contentEngineConfiguration;
    }

    public SpringContentEngineConfigurator setContentEngineConfiguration(SpringContentEngineConfiguration contentEngineConfiguration) {
        this.contentEngineConfiguration = contentEngineConfiguration;
        return this;
    }

}
