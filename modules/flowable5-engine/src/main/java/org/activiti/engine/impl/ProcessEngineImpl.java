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
package org.activiti.engine.impl;

import java.util.Map;

import org.activiti.engine.DynamicBpmnService;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.TransactionContextFactory;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Tijs Rademakers
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
    protected CommandExecutor commandExecutor;
    protected Map<Class<?>, SessionFactory> sessionFactories;
    protected ExpressionManager expressionManager;
    protected TransactionContextFactory transactionContextFactory;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public ProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
        this.name = processEngineConfiguration.getProcessEngineName();
        this.repositoryService = processEngineConfiguration.getRepositoryService();
        this.runtimeService = processEngineConfiguration.getRuntimeService();
        this.historicDataService = processEngineConfiguration.getHistoryService();
        this.identityService = processEngineConfiguration.getIdentityService();
        this.taskService = processEngineConfiguration.getTaskService();
        this.formService = processEngineConfiguration.getFormService();
        this.managementService = processEngineConfiguration.getManagementService();
        this.dynamicBpmnService = processEngineConfiguration.getDynamicBpmnService();
        this.commandExecutor = processEngineConfiguration.getCommandExecutor();
        this.sessionFactories = processEngineConfiguration.getSessionFactories();
        this.transactionContextFactory = processEngineConfiguration.getTransactionContextFactory();

        if (name == null) {
            LOGGER.info("default activiti ProcessEngine created");
        } else {
            LOGGER.info("ProcessEngine {} created", name);
        }

        ProcessEngines.registerProcessEngine(this);

        if (processEngineConfiguration.getProcessEngineLifecycleListener() != null) {
            processEngineConfiguration.getProcessEngineLifecycleListener().onProcessEngineBuilt(this);
        }

        processEngineConfiguration.getEventDispatcher().dispatchEvent(
                ActivitiEventBuilder.createGlobalEvent(FlowableEngineEventType.ENGINE_CREATED));
    }

    @Override
    public void close() {
        ProcessEngines.unregister(this);

        if (processEngineConfiguration.getProcessEngineLifecycleListener() != null) {
            processEngineConfiguration.getProcessEngineLifecycleListener().onProcessEngineClosed(this);
        }

        processEngineConfiguration.getEventDispatcher().dispatchEvent(
                ActivitiEventBuilder.createGlobalEvent(FlowableEngineEventType.ENGINE_CLOSED));
    }

    // getters and setters //////////////////////////////////////////////////////

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
