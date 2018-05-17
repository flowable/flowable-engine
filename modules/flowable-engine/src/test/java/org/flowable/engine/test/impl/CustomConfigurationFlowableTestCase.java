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
package org.flowable.engine.test.impl;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.AbstractFlowableTestCase;

/**
 * Reads the default flowable.cfg.xml from the test classpath, but allows to change the settings before the engine is built.
 * This allows to run with the same settings as the regular test runs, but tweak the config slightly.
 * 
 * Note that a {@link ProcessEngine} is booted up and shut down for every test, 
 * so use with caution to avoid that total test times go up. 
 * 
 * @author Joram Barrez
 */
public abstract class CustomConfigurationFlowableTestCase extends AbstractFlowableTestCase {
    
    protected String cfgResource = "flowable.cfg.xml";
    
    @Override
    protected void initializeProcessEngine() {
        ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.cfg.xml");
        processEngineConfiguration.setEngineName(getEngineName()); // to distinguish between different engines in different tests
        processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configureConfiguration((ProcessEngineConfigurationImpl) processEngineConfiguration);
        this.processEngine = processEngineConfiguration.buildProcessEngine();
    }
    
    @Override
    protected void closeDownProcessEngine() {
        this.processEngine.close();
    }
    
    protected abstract String getEngineName();
    
    protected abstract void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration);

    public String getCfgResource() {
        return cfgResource;
    }

    public void setCfgResource(String cfgResource) {
        this.cfgResource = cfgResource;
    }
    
}
