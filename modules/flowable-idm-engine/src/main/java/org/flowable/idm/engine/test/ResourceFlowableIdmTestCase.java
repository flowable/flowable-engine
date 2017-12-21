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

package org.flowable.idm.engine.test;

import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.IdmEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class ResourceFlowableIdmTestCase extends AbstractFlowableIdmTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceFlowableIdmTestCase.class);

    protected String idmConfigurationResource;
    protected String idmEngineName;

    public ResourceFlowableIdmTestCase(String idmConfigurationResource) {
        this(idmConfigurationResource, null);
    }

    public ResourceFlowableIdmTestCase(String idmConfigurationResource, String idmEngineName) {
        this.idmConfigurationResource = idmConfigurationResource;
        this.idmEngineName = idmEngineName;
    }

    @Override
    protected void closeDownIdmEngine() {
        super.closeDownIdmEngine();
        IdmEngines.unregister(idmEngine);
        idmEngine = null;
    }

    @Override
    protected void initializeIdmEngine() {
        IdmEngineConfiguration config = IdmEngineConfiguration.createIdmEngineConfigurationFromResource(idmConfigurationResource);
        if (idmEngineName != null) {
            LOGGER.info("Initializing idm engine with name '{}'", idmEngineName);
            config.setEngineName(idmEngineName);
        }
        additionalConfiguration(config);
        idmEngine = config.buildIdmEngine();
    }

    protected void additionalConfiguration(IdmEngineConfiguration idmEngineConfiguration) {

    }

}
