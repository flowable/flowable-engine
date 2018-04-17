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
package org.flowable.engine.impl;

import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.cfg.TransactionContextFactory;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.SessionFactory;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class ProcessEngineImpl implements ProcessEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngineImpl.class);

    protected String name;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected HistoryService historicDataService;
    protected IdentityService identityService;
    protected TaskService taskService;
    protected FormService formService;
    protected ManagementService managementService;
    protected DynamicBpmnService dynamicBpmnService;
    protected AsyncExecutor asyncExecutor;
    protected AsyncExecutor asyncHistoryExecutor;
    protected CommandExecutor commandExecutor;
    protected Map<Class<?>, SessionFactory> sessionFactories;
    protected TransactionContextFactory transactionContextFactory;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public ProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
        this.name = processEngineConfiguration.getEngineName();
        this.repositoryService = processEngineConfiguration.getRepositoryService();
        this.runtimeService = processEngineConfiguration.getRuntimeService();
        this.historicDataService = processEngineConfiguration.getHistoryService();
        this.identityService = processEngineConfiguration.getIdentityService();
        this.taskService = processEngineConfiguration.getTaskService();
        this.formService = processEngineConfiguration.getFormService();
        this.managementService = processEngineConfiguration.getManagementService();
        this.dynamicBpmnService = processEngineConfiguration.getDynamicBpmnService();
        this.asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        this.asyncHistoryExecutor = processEngineConfiguration.getAsyncHistoryExecutor();
        this.commandExecutor = processEngineConfiguration.getCommandExecutor();
        this.sessionFactories = processEngineConfiguration.getSessionFactories();
        this.transactionContextFactory = processEngineConfiguration.getTransactionContextFactory();

        if (processEngineConfiguration.isUsingRelationalDatabase() && processEngineConfiguration.getDatabaseSchemaUpdate() != null) {
            commandExecutor.execute(processEngineConfiguration.getSchemaCommandConfig(), new SchemaOperationsProcessEngineBuild());
        }

        if (name == null) {
            LOGGER.info("default ProcessEngine created");
        } else {
            LOGGER.info("ProcessEngine {} created", name);
        }

        ProcessEngines.registerProcessEngine(this);

        if (processEngineConfiguration.getProcessEngineLifecycleListener() != null) {
            processEngineConfiguration.getProcessEngineLifecycleListener().onProcessEngineBuilt(this);
        }

        processEngineConfiguration.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createGlobalEvent(FlowableEngineEventType.ENGINE_CREATED));

        if (asyncExecutor != null && asyncExecutor.isAutoActivate()) {
            asyncExecutor.start();
        }
        if (asyncHistoryExecutor != null && asyncHistoryExecutor.isAutoActivate()) {
            asyncHistoryExecutor.start();
        }
    }

    @Override
    public void close() {
        ProcessEngines.unregister(this);
        if (asyncExecutor != null && asyncExecutor.isActive()) {
            asyncExecutor.shutdown();
        }
        if (asyncHistoryExecutor != null && asyncHistoryExecutor.isActive()) {
            asyncHistoryExecutor.shutdown();
        }

        Runnable closeRunnable = processEngineConfiguration.getProcessEngineCloseRunnable();
        if (closeRunnable != null) {
            closeRunnable.run();
        }

        if (processEngineConfiguration.getProcessEngineLifecycleListener() != null) {
            processEngineConfiguration.getProcessEngineLifecycleListener().onProcessEngineClosed(this);
        }

        processEngineConfiguration.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createGlobalEvent(FlowableEngineEventType.ENGINE_CLOSED));
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IdentityService getIdentityService() {
        return identityService;
    }

    @Override
    public ManagementService getManagementService() {
        return managementService;
    }

    @Override
    public TaskService getTaskService() {
        return taskService;
    }

    @Override
    public HistoryService getHistoryService() {
        return historicDataService;
    }

    @Override
    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    public FormService getFormService() {
        return formService;
    }

    @Override
    public DynamicBpmnService getDynamicBpmnService() {
        return dynamicBpmnService;
    }

    @Override
    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }
}
