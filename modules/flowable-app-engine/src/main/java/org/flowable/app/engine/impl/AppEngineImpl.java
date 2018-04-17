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
package org.flowable.app.engine.impl;

import org.flowable.app.api.AppManagementService;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.AppEngines;
import org.flowable.app.engine.impl.cmd.SchemaOperationsAppEngineBuild;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class AppEngineImpl implements AppEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AppEngineImpl.class);

    protected String name;
    protected AppEngineConfiguration appEngineConfiguration;
    protected AppManagementService appManagementService;
    protected AppRepositoryService appRepositoryService;
    
    public AppEngineImpl(AppEngineConfiguration appEngineConfiguration) {
        this.appEngineConfiguration = appEngineConfiguration;
        this.name = appEngineConfiguration.getEngineName();
        this.appManagementService = appEngineConfiguration.getAppManagementService();
        this.appRepositoryService = appEngineConfiguration.getAppRepositoryService();
        
        if (appEngineConfiguration.isUsingRelationalDatabase() && appEngineConfiguration.getDatabaseSchemaUpdate() != null) {
            CommandExecutor commandExecutor = appEngineConfiguration.getCommandExecutor();
            commandExecutor.execute(appEngineConfiguration.getSchemaCommandConfig(), new SchemaOperationsAppEngineBuild());
        }

        LOGGER.info("AppEngine {} created", name);
        
        AppEngines.registerAppEngine(this);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public void close() {
        AppEngines.unregister(this);
    }
    
    public AppEngineConfiguration getAppEngineConfiguration() {
        return appEngineConfiguration;
    }

    public void setAppEngineConfiguration(AppEngineConfiguration appEngineConfiguration) {
        this.appEngineConfiguration = appEngineConfiguration;
    }
    
    @Override
    public AppManagementService getAppManagementService() {
        return appManagementService;
    }

    public void setAppManagementService(AppManagementService appManagementService) {
        this.appManagementService = appManagementService;
    }

    @Override
    public AppRepositoryService getAppRepositoryService() {
        return appRepositoryService;
    }
    
    public void setAppRepositoryService(AppRepositoryService appRepositoryService) {
        this.appRepositoryService = appRepositoryService;
    }
    
}
