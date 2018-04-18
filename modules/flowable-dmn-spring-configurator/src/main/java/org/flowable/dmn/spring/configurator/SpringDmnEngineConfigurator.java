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
package org.flowable.dmn.spring.configurator;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.spring.SpringEngineConfiguration;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.configurator.DmnEngineConfigurator;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.spring.SpringDmnExpressionManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringDmnEngineConfigurator extends DmnEngineConfigurator {

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (dmnEngineConfiguration == null) {
            dmnEngineConfiguration = new SpringDmnEngineConfiguration();
        } else if (!(dmnEngineConfiguration instanceof SpringDmnEngineConfiguration)) {
            throw new IllegalArgumentException("Expected dmnEngine configuration to be of type"
                + SpringDmnEngineConfiguration.class + " but was " + dmnEngineConfiguration.getClass());
        }
        initialiseCommonProperties(engineConfiguration, dmnEngineConfiguration);
        
        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        ((SpringDmnEngineConfiguration) dmnEngineConfiguration).setTransactionManager(springEngineConfiguration.getTransactionManager());
        dmnEngineConfiguration.setExpressionManager(new SpringDmnExpressionManager(
                        springEngineConfiguration.getApplicationContext(), springEngineConfiguration.getBeans()));

        initDmnEngine();
        
        initServiceConfigurations(engineConfiguration, dmnEngineConfiguration);
    }

    @Override
    protected synchronized DmnEngine initDmnEngine() {
        if (dmnEngineConfiguration == null) {
            throw new FlowableException("DmnEngineConfiguration is required");
        }

        return dmnEngineConfiguration.buildDmnEngine();
    }
}
