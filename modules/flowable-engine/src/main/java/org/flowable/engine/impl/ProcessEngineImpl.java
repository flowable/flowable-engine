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

import org.flowable.content.api.ContentService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
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
import org.flowable.engine.common.impl.cfg.TransactionContextFactory;
import org.flowable.engine.common.impl.interceptor.SessionFactory;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.asyncexecutor.AsyncExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.TransactionListener;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.interceptor.CommandExecutor;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.idm.api.IdmIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class ProcessEngineImpl implements ProcessEngine {

    private static Logger log = LoggerFactory.getLogger(ProcessEngineImpl.class);

    protected String name;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected HistoryService historicDataService;
    protected IdentityService identityService;
    protected TaskService taskService;
    protected FormService formService;
    protected ManagementService managementService;
    protected DynamicBpmnService dynamicBpmnService;
    protected FormRepositoryService formEngineRepositoryService;
    protected org.flowable.form.api.FormService formEngineFormService;
    protected DmnRepositoryService dmnRepositoryService;
    protected DmnRuleService dmnRuleService;
    protected IdmIdentityService idmIdentityService;
    protected ContentService contentService;
    protected AsyncExecutor asyncExecutor;
    protected AsyncExecutor asyncHistoryExecutor;
    protected CommandExecutor commandExecutor;
    protected Map<Class<?>, SessionFactory> sessionFactories;
    protected TransactionContextFactory<TransactionListener, CommandContext> transactionContextFactory;
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
        this.commandExecutor = processEngineConfiguration.getCommandExecutor();
        this.sessionFactories = processEngineConfiguration.getSessionFactories();
        this.transactionContextFactory = processEngineConfiguration.getTransactionContextFactory();
        this.formEngineRepositoryService = processEngineConfiguration.getFormEngineRepositoryService();
        this.formEngineFormService = processEngineConfiguration.getFormEngineFormService();
        this.dmnRepositoryService = processEngineConfiguration.getDmnEngineRepositoryService();
        this.dmnRuleService = processEngineConfiguration.getDmnEngineRuleService();
        this.idmIdentityService = processEngineConfiguration.getIdmIdentityService();
        this.contentService = processEngineConfiguration.getContentService();

        if (processEngineConfiguration.isUsingRelationalDatabase() && processEngineConfiguration.getDatabaseSchemaUpdate() != null) {
            commandExecutor.execute(processEngineConfiguration.getSchemaCommandConfig(), new SchemaOperationsProcessEngineBuild());
        }

        if (name == null) {
            log.info("default ProcessEngine created");
        } else {
            log.info("ProcessEngine {} created", name);
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

    public String getName() {
        return name;
    }

    public IdentityService getIdentityService() {
        return identityService;
    }

    public ManagementService getManagementService() {
        return managementService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public HistoryService getHistoryService() {
        return historicDataService;
    }

    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public FormService getFormService() {
        return formService;
    }

    public DynamicBpmnService getDynamicBpmnService() {
        return dynamicBpmnService;
    }

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public FormRepositoryService getFormEngineRepositoryService() {
        return formEngineRepositoryService;
    }

    public org.flowable.form.api.FormService getFormEngineFormService() {
        return formEngineFormService;
    }

    public DmnRepositoryService getDmnRepositoryService() {
        return dmnRepositoryService;
    }

    public DmnRuleService getDmnRuleService() {
        return dmnRuleService;
    }

    public IdmIdentityService getIdmIdentityService() {
        return idmIdentityService;
    }

    public ContentService getContentService() {
        return contentService;
    }
}
