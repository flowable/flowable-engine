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

package org.flowable.engine.impl.test;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class ResourceFlowableTestCase extends AbstractFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceFlowableTestCase.class);

    protected String activitiConfigurationResource;
    protected String processEngineName;

    public ResourceFlowableTestCase(String activitiConfigurationResource) {
        this(activitiConfigurationResource, null);
    }

    public ResourceFlowableTestCase(String activitiConfigurationResource, String processEngineName) {
        this.activitiConfigurationResource = activitiConfigurationResource;
        this.processEngineName = processEngineName;
    }

    @Override
    protected void closeDownProcessEngine() {
        super.closeDownProcessEngine();
        ProcessEngines.unregister(processEngine);
        processEngine = null;
        nullifyServices();
    }

    @Override
    protected void initializeProcessEngine() {
        ProcessEngineConfiguration config = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource(activitiConfigurationResource);
        if (processEngineName != null) {
            LOGGER.info("Initializing process engine with name '{}'", processEngineName);
            config.setEngineName(processEngineName);
        }
        additionalConfiguration(config);
        processEngine = config.buildProcessEngine();
    }

    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {

    }

}
