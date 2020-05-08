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
package org.flowable.cmmn.engine.test;

import java.io.IOException;
import java.io.InputStream;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public abstract class FlowableCmmnTestCase extends AbstractFlowableCmmnTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableCmmnTestCase.class);

    public static final String FLOWABLE_CMMN_CFG_XML = "flowable.cmmn.cfg.xml";

    @BeforeClass
    public static void setupEngine() {
        if (CmmnTestRunner.getCmmnEngineConfiguration() == null) {
            initCmmnEngine();
        }
    }

    protected static void initCmmnEngine() {
        try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream(FLOWABLE_CMMN_CFG_XML)) {
            if (inputStream != null) {
                cmmnEngine = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream).buildCmmnEngine();
            } else {
               throw new RuntimeException("No " + FLOWABLE_CMMN_CFG_XML + " file found on the classpath");
            }
            CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngine.getCmmnEngineConfiguration());
        } catch (IOException e) {
            LOGGER.error("Could not create CMMN engine", e);
        }
    }

    @Before
    public void setupServices() {
        CmmnEngineConfiguration cmmnEngineConfiguration = CmmnTestRunner.getCmmnEngineConfiguration();
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        this.cmmnManagementService = cmmnEngineConfiguration.getCmmnManagementService();
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.dynamicCmmnService = cmmnEngineConfiguration.getDynamicCmmnService();
        this.cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        this.cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();
        this.cmmnMigrationService = cmmnEngineConfiguration.getCmmnMigrationService();
    }

    protected EventRepositoryService getEventRepositoryService() {
        return getEventRegistryEngineConfiguration().getEventRepositoryService();
    }
    
    protected EventRegistry getEventRegistry() {
        return getEventRegistryEngineConfiguration().getEventRegistry();
    }
    
    protected EventRegistryEngineConfiguration getEventRegistryEngineConfiguration() {
        return (EventRegistryEngineConfiguration) cmmnEngineConfiguration.getEngineConfigurations()
                        .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
    }
}
