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
package org.flowable.dmn.engine.impl;

import org.flowable.common.engine.api.engine.EngineLifecycleListener;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.DmnEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class DmnEngineImpl implements DmnEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnEngineImpl.class);

    protected String name;
    protected DmnManagementService dmnManagementService;
    protected DmnRepositoryService dmnRepositoryService;
    protected DmnDecisionService dmnDecisionService;
    protected DmnHistoryService dmnHistoryService;
    protected DmnEngineConfiguration dmnEngineConfiguration;

    public DmnEngineImpl(DmnEngineConfiguration dmnEngineConfiguration) {
        this.dmnEngineConfiguration = dmnEngineConfiguration;
        this.name = dmnEngineConfiguration.getEngineName();
        this.dmnManagementService = dmnEngineConfiguration.getDmnManagementService();
        this.dmnRepositoryService = dmnEngineConfiguration.getDmnRepositoryService();
        this.dmnDecisionService = dmnEngineConfiguration.getDmnDecisionService();
        this.dmnHistoryService = dmnEngineConfiguration.getDmnHistoryService();
        
        if (dmnEngineConfiguration.getSchemaManagementCmd() != null) {
            dmnEngineConfiguration.getCommandExecutor().execute(dmnEngineConfiguration.getSchemaCommandConfig(), dmnEngineConfiguration.getSchemaManagementCmd());
        }

        if (name == null) {
            LOGGER.info("default flowable DmnEngine created");
        } else {
            LOGGER.info("DmnEngine {} created", name);
        }

        DmnEngines.registerDmnEngine(this);

        if (dmnEngineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : dmnEngineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineBuilt(this);
            }
        }
    }

    @Override
    public void close() {
        DmnEngines.unregister(this);
        dmnEngineConfiguration.close();

        if (dmnEngineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : dmnEngineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineClosed(this);
            }
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DmnManagementService getDmnManagementService() {
        return dmnManagementService;
    }

    @Override
    public DmnRepositoryService getDmnRepositoryService() {
        return dmnRepositoryService;
    }

    @Override
    public DmnDecisionService getDmnDecisionService() {
        return dmnDecisionService;
    }
    
    @Override
    public DmnHistoryService getDmnHistoryService() {
        return dmnHistoryService;
    }

    @Override
    public DmnEngineConfiguration getDmnEngineConfiguration() {
        return dmnEngineConfiguration;
    }
}
