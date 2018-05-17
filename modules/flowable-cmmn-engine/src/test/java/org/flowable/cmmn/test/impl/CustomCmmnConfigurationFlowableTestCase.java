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
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
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
public abstract class CustomCmmnConfigurationFlowableTestCase extends FlowableCmmnTestCase {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomCmmnConfigurationFlowableTestCase.class);
    
    protected CmmnEngine originalCmmnEngine;
    protected CmmnEngineConfiguration originalCmmnEngineConfiguration;
    
    @Override
    public void setupServices() {
        this.originalCmmnEngine = cmmnEngine;
        this.originalCmmnEngineConfiguration = CmmnTestRunner.getCmmnEngineConfiguration();
        
        try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream(FLOWABLE_CMMN_CFG_XML)) {
            if (inputStream != null) {
                CmmnEngineConfiguration cmmnEngineConfiguration = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream);
                cmmnEngineConfiguration.setCmmnEngineName(getEngineName());
                cmmnEngineConfiguration.setDatabaseSchemaUpdate(CmmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE); // override the default db setting of drop-create when running in QA 
                configureConfiguration(cmmnEngineConfiguration);
                
                cmmnEngine = cmmnEngineConfiguration.buildCmmnEngine();
                cmmnEngineConfiguration.getClock().reset();
                CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngineConfiguration);
                
                // Calling this will change the cmmnEngine and cmmnEngineConfiguration
                super.setupServices();
                
            } else {
               throw new RuntimeException("No " + FLOWABLE_CMMN_CFG_XML + " file found on the classpath");
            }
        } catch (IOException e) {
            LOGGER.error("Could not create CMMN engine", e);
        }
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        cmmnEngine.close();
        
        // Restore any previous engine and config
        cmmnEngine = originalCmmnEngine;
        CmmnTestRunner.setCmmnEngineConfiguration(originalCmmnEngineConfiguration);
    }
    
    protected abstract String getEngineName();
    
    protected abstract void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration);

}
