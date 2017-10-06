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

package org.flowable.dmn.engine.impl.test;

import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.DmnEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public abstract class ResourceFlowableDmnTestCase extends AbstractFlowableDmnTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceFlowableDmnTestCase.class);

    protected String flowableConfigurationResource;
    protected String dmnEngineName;

    public ResourceFlowableDmnTestCase(String flowableConfigurationResource) {
        this(flowableConfigurationResource, null);
    }

    public ResourceFlowableDmnTestCase(String flowableConfigurationResource, String dmnEngineName) {
        this.flowableConfigurationResource = flowableConfigurationResource;
        this.dmnEngineName = dmnEngineName;
    }

    @Override
    protected void closeDownDmnEngine() {
        super.closeDownDmnEngine();
        DmnEngines.unregister(dmnEngine);
        dmnEngine = null;
        nullifyServices();
    }

    @Override
    protected void initializeDmnEngine() {
        DmnEngineConfiguration config = DmnEngineConfiguration.createDmnEngineConfigurationFromResource(flowableConfigurationResource);
        if (dmnEngineName != null) {
            LOGGER.info("Initializing DMN engine with name '{}'", dmnEngineName);
            config.setEngineName(dmnEngineName);
        }
        additionalConfiguration(config);
        dmnEngine = config.buildDmnEngine();
    }

    protected void additionalConfiguration(DmnEngineConfiguration dmnEngineConfiguration) {

    }

}
