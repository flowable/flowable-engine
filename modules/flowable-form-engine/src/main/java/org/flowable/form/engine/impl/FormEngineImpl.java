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
package org.flowable.form.engine.impl;

import org.flowable.common.engine.api.engine.EngineLifecycleListener;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.FormEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class FormEngineImpl implements FormEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormEngineImpl.class);

    protected String name;
    protected FormManagementService managementService;
    protected FormRepositoryService repositoryService;
    protected FormService formService;
    protected FormEngineConfiguration engineConfiguration;

    public FormEngineImpl(FormEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
        this.name = engineConfiguration.getEngineName();
        this.managementService = engineConfiguration.getFormManagementService();
        this.repositoryService = engineConfiguration.getFormRepositoryService();
        this.formService = engineConfiguration.getFormService();
        
        if (engineConfiguration.getSchemaManagementCmd() != null) {
            engineConfiguration.getCommandExecutor().execute(engineConfiguration.getSchemaCommandConfig(), engineConfiguration.getSchemaManagementCmd());
        }

        if (name == null) {
            LOGGER.info("default flowable FormEngine created");
        } else {
            LOGGER.info("FormEngine {} created", name);
        }

        FormEngines.registerFormEngine(this);

        if (engineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : engineConfiguration.getEngineLifecycleListeners()) {
                engineLifecycleListener.onEngineBuilt(this);
            }
        }
    }

    @Override
    public void close() {
        FormEngines.unregister(this);
        engineConfiguration.close();

        if (engineConfiguration.getEngineLifecycleListeners() != null) {
            for (EngineLifecycleListener engineLifecycleListener : engineConfiguration.getEngineLifecycleListeners()) {
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
    public FormManagementService getFormManagementService() {
        return managementService;
    }

    @Override
    public FormRepositoryService getFormRepositoryService() {
        return repositoryService;
    }

    @Override
    public FormService getFormService() {
        return formService;
    }

    @Override
    public FormEngineConfiguration getFormEngineConfiguration() {
        return engineConfiguration;
    }
}
