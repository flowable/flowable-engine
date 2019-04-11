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
package org.flowable.cmmn.test.impl;

import java.io.IOException;
import java.io.InputStream;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.AbstractFlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the default flowable.cmmn.cfg.xml from the test classpath, but allows to change the settings before the engine is built.
 * This allows to run with the same settings as the regular test runs, but tweak the config slightly.
 * 
 * Note that a {@link CmmnEngine} is booted up and shut down for every test, 
 * so use with caution to avoid that total test times go up. 
 * 
 * @author Joram Barrez
 */
@RunWith(CmmnTestRunner.class)
public abstract class CustomCmmnConfigurationFlowableTestCase extends AbstractFlowableCmmnTestCase {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomCmmnConfigurationFlowableTestCase.class);
    
    public static CmmnEngineConfiguration originalCmmnEngineConfiguration;
    public static CmmnEngine cmmnEngine;

    @BeforeClass
    public static void copyOriginalConfig() {
        originalCmmnEngineConfiguration = CmmnTestRunner.getCmmnEngineConfiguration();
    }

    @AfterClass
    public static void resetConfig() {
        cmmnEngine.close();
        cmmnEngine = null;

        // Restore any previous engine and config
        CmmnTestRunner.setCmmnEngineConfiguration(originalCmmnEngineConfiguration);
    }

    @Before
    public void setupServices() {
        if (CustomCmmnConfigurationFlowableTestCase.cmmnEngine == null) {

            try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream(FlowableCmmnTestCase.FLOWABLE_CMMN_CFG_XML)) {
                if (inputStream != null) {
                    CmmnEngineConfiguration cmmnEngineConfiguration = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream);
                    cmmnEngineConfiguration.setCmmnEngineName(getEngineName());
                    configureConfiguration(cmmnEngineConfiguration);

                    CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngineConfiguration);
                    CustomCmmnConfigurationFlowableTestCase.cmmnEngine = cmmnEngineConfiguration.buildCmmnEngine();

                } else {
                    throw new RuntimeException("No " + FlowableCmmnTestCase.FLOWABLE_CMMN_CFG_XML + " file found on the classpath");
                }
            } catch (IOException e) {
                LOGGER.error("Could not create CMMN engine", e);
            }

        }

        CmmnTestRunner.getCmmnEngineConfiguration().getClock().reset();

        this.cmmnEngineConfiguration = CmmnTestRunner.getCmmnEngineConfiguration();
        this.cmmnRepositoryService = CmmnTestRunner.getCmmnEngineConfiguration().getCmmnRepositoryService();
        this.cmmnManagementService = CmmnTestRunner.getCmmnEngineConfiguration().getCmmnManagementService();
        this.cmmnRuntimeService = CmmnTestRunner.getCmmnEngineConfiguration().getCmmnRuntimeService();
        this.cmmnTaskService = CmmnTestRunner.getCmmnEngineConfiguration().getCmmnTaskService();
        this.cmmnHistoryService = CmmnTestRunner.getCmmnEngineConfiguration().getCmmnHistoryService();
    }

    protected abstract String getEngineName();
    
    protected abstract void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration);

}
