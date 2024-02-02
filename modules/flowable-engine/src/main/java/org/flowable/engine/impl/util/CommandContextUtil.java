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
package org.flowable.engine.impl.util;

import java.util.HashMap;
import java.util.Map;

import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.common.engine.impl.persistence.entity.TableDataManager;
import org.flowable.content.api.ContentEngineConfigurationApi;
import org.flowable.content.api.ContentService;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnEngineConfigurationApi;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManager;
import org.flowable.engine.impl.persistence.entity.CommentEntityManager;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManager;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.ModelEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.IdentityLinkService;
import org.flowable.idm.api.IdmEngineConfigurationApi;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.asyncexecutor.FailedJobCommandFactory;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskService;
import org.flowable.variable.service.HistoricVariableService;
import org.flowable.variable.service.VariableService;

public class CommandContextUtil {

    public static final String ATTRIBUTE_INVOLVED_EXECUTIONS = "ctx.attribute.involvedExecutions";

    public static ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return getProcessEngineConfiguration(getCommandContext());
    }

    public static ProcessEngineConfigurationImpl getProcessEngineConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (ProcessEngineConfigurationImpl) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        }
        return null;
    }

    // IDM ENGINE

    public static IdmEngineConfigurationApi getIdmEngineConfiguration() {
        return getIdmEngineConfiguration(getCommandContext());
    }

    public static IdmEngineConfigurationApi getIdmEngineConfiguration(CommandContext commandContext) {
        return (IdmEngineConfigurationApi) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
    }

    public static IdmIdentityService getIdmIdentityService() {
        IdmIdentityService idmIdentityService = null;
        IdmEngineConfigurationApi idmEngineConfiguration = getIdmEngineConfiguration();
        if (idmEngineConfiguration != null) {
            idmIdentityService = idmEngineConfiguration.getIdmIdentityService();
        }

        return idmIdentityService;
    }
    
    // EVENT REGISTRY
    
    public static EventRegistryEngineConfiguration getEventRegistryEngineConfiguration() {
        return getEventRegistryEngineConfiguration(getCommandContext());
    }
    
    public static EventRegistryEngineConfiguration getEventRegistryEngineConfiguration(CommandContext commandContext) {
        return (EventRegistryEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
    }
    
    public static EventRegistry getEventRegistry() {
        return getEventRegistry(getCommandContext());
    }
    
    public static EventRegistry getEventRegistry(CommandContext commandContext) {
        EventRegistry eventRegistry = null;
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = getEventRegistryEngineConfiguration(commandContext);
        if (eventRegistryEngineConfiguration != null) {
            eventRegistry = eventRegistryEngineConfiguration.getEventRegistry();
        }

        return eventRegistry;
    }
    
    public static EventRepositoryService getEventRepositoryService() {
        return getEventRepositoryService(getCommandContext());
    }
    
    public static EventRepositoryService getEventRepositoryService(CommandContext commandContext) {
        EventRepositoryService eventRepositoryService = null;
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = getEventRegistryEngineConfiguration(commandContext);
        if (eventRegistryEngineConfiguration != null) {
            eventRepositoryService = eventRegistryEngineConfiguration.getEventRepositoryService();
        }

        return eventRepositoryService;
    }

    // DMN ENGINE

    public static DmnEngineConfigurationApi getDmnEngineConfiguration() {
        return getDmnEngineConfiguration(getCommandContext());
    }

    public static DmnEngineConfigurationApi getDmnEngineConfiguration(CommandContext commandContext) {
        return (DmnEngineConfigurationApi) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
    }

    public static DmnRepositoryService getDmnRepositoryService() {
        DmnRepositoryService dmnRepositoryService = null;
        DmnEngineConfigurationApi dmnEngineConfiguration = getDmnEngineConfiguration();
        if (dmnEngineConfiguration != null) {
            dmnRepositoryService = dmnEngineConfiguration.getDmnRepositoryService();
        }

        return dmnRepositoryService;
    }

    public static DmnDecisionService getDmnRuleService() {
        DmnDecisionService dmnRuleService = null;
        DmnEngineConfigurationApi dmnEngineConfiguration = getDmnEngineConfiguration();
        if (dmnEngineConfiguration != null) {
            dmnRuleService = dmnEngineConfiguration.getDmnDecisionService();
        }

        return dmnRuleService;
    }

    public static DmnManagementService getDmnManagementService() {
        DmnManagementService dmnManagementService = null;
        DmnEngineConfigurationApi dmnEngineConfiguration = getDmnEngineConfiguration();
        if (dmnEngineConfiguration != null) {
            dmnManagementService = dmnEngineConfiguration.getDmnManagementService();
        }

        return dmnManagementService;
    }

    // FORM ENGINE

    public static FormEngineConfigurationApi getFormEngineConfiguration() {
        return getFormEngineConfiguration(getCommandContext());
    }

    public static FormEngineConfigurationApi getFormEngineConfiguration(CommandContext commandContext) {
        return (FormEngineConfigurationApi) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
    }

    public static FormRepositoryService getFormRepositoryService() {
        return getFormRepositoryService(getCommandContext());
    }
    
    public static FormRepositoryService getFormRepositoryService(CommandContext commandContext) {
        FormRepositoryService formRepositoryService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration(commandContext);
        if (formEngineConfiguration != null) {
            formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        }

        return formRepositoryService;
    }

    public static FormService getFormService() {
        return getFormService(getCommandContext());
    }
    
    public static FormService getFormService(CommandContext commandContext) {
        FormService formService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration(commandContext);
        if (formEngineConfiguration != null) {
            formService = formEngineConfiguration.getFormService();
        }

        return formService;
    }

    public static FormManagementService getFormManagementService() {
        return getFormManagementService(getCommandContext());
    }
    
    public static FormManagementService getFormManagementService(CommandContext commandContext) {
        FormManagementService formManagementService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration(commandContext);
        if (formEngineConfiguration != null) {
            formManagementService = formEngineConfiguration.getFormManagementService();
        }

        return formManagementService;
    }

    // CONTENT ENGINE

    public static ContentEngineConfigurationApi getContentEngineConfiguration() {
        return getContentEngineConfiguration(getCommandContext());
    }

    public static ContentEngineConfigurationApi getContentEngineConfiguration(CommandContext commandContext) {
        return (ContentEngineConfigurationApi) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_CONTENT_ENGINE_CONFIG);
    }

    public static ContentService getContentService() {
        return getContentService(getCommandContext());
    }
    
    public static ContentService getContentService(CommandContext commandContext) {
        ContentService contentService = null;
        ContentEngineConfigurationApi contentEngineConfiguration = getContentEngineConfiguration(commandContext);
        if (contentEngineConfiguration != null) {
            contentService = contentEngineConfiguration.getContentService();
        }

        return contentService;
    }
    
    public static VariableService getVariableService() {
        return getVariableService(getCommandContext());
    }
    
    public static VariableService getVariableService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getVariableServiceConfiguration().getVariableService();
    }
    
    public static HistoricVariableService getHistoricVariableService() {
        return getHistoricVariableService(getCommandContext());
    }
    
    public static HistoricVariableService getHistoricVariableService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getVariableServiceConfiguration().getHistoricVariableService();
    }
    
    public static IdentityLinkService getIdentityLinkService() {
        return getIdentityLinkService(getCommandContext());
    }
    
    public static IdentityLinkService getIdentityLinkService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getIdentityLinkServiceConfiguration().getIdentityLinkService();
    }
    
    public static HistoricIdentityLinkService getHistoricIdentityLinkService() {
        return getHistoricIdentityLinkService(getCommandContext());
    }
    
    public static HistoricIdentityLinkService getHistoricIdentityLinkService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService();
    }
    
    public static EntityLinkService getEntityLinkService() {
        return getEntityLinkService(getCommandContext());
    }
    
    public static EntityLinkService getEntityLinkService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getEntityLinkServiceConfiguration().getEntityLinkService();
    }
    
    public static HistoricEntityLinkService getHistoricEntityLinkService() {
        return getHistoricEntityLinkService(getCommandContext());
    }
    
    public static HistoricEntityLinkService getHistoricEntityLinkService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getEntityLinkServiceConfiguration().getHistoricEntityLinkService();
    }
    
    public static JobService getJobService() {
        return getJobService(getCommandContext());
    }
    
    public static JobService getJobService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService();
    }
    
    public static TimerJobService getTimerJobService() {
        return getTimerJobService(getCommandContext());
    }
    
    public static TimerJobService getTimerJobService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService();
    }
    
    public static TaskService getTaskService() {
        return getTaskService(getCommandContext());
    }
    
    public static TaskService getTaskService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getTaskServiceConfiguration().getTaskService();
    }
    
    public static HistoricTaskService getHistoricTaskService() {
        return getHistoricTaskService(getCommandContext());
    }
    
    public static HistoricTaskService getHistoricTaskService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getTaskServiceConfiguration().getHistoricTaskService();
    }
    
    public static EventSubscriptionService getEventSubscriptionService() {
        return getEventSubscriptionService(getCommandContext());
    }
    
    public static EventSubscriptionService getEventSubscriptionService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
    }
    
    public static BatchService getBatchService() {
        return getBatchService(getCommandContext());
    }
    
    public static BatchService getBatchService(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getBatchServiceConfiguration().getBatchService();
    }

    public static FlowableEngineAgenda getAgenda() {
        return getAgenda(getCommandContext());
    }

    public static FlowableEngineAgenda getAgenda(CommandContext commandContext) {
        return commandContext.getSession(FlowableEngineAgenda.class);
    }

    public static DbSqlSession getDbSqlSession() {
        return getDbSqlSession(getCommandContext());
    }

    public static DbSqlSession getDbSqlSession(CommandContext commandContext) {
        return commandContext.getSession(DbSqlSession.class);
    }

    public static EntityCache getEntityCache() {
        return getEntityCache(getCommandContext());
    }

    public static EntityCache getEntityCache(CommandContext commandContext) {
        return commandContext.getSession(EntityCache.class);
    }

    @SuppressWarnings("unchecked")
    public static void addInvolvedExecution(CommandContext commandContext, ExecutionEntity executionEntity) {
        if (executionEntity.getId() != null) {
            Map<String, ExecutionEntity> involvedExecutions = null;
            Object obj = commandContext.getAttribute(ATTRIBUTE_INVOLVED_EXECUTIONS);
            if (obj != null) {
                involvedExecutions = (Map<String, ExecutionEntity>) obj;
            } else {
                involvedExecutions = new HashMap<>();
                commandContext.addAttribute(ATTRIBUTE_INVOLVED_EXECUTIONS, involvedExecutions);
            }
            involvedExecutions.put(executionEntity.getId(), executionEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ExecutionEntity> getInvolvedExecutions(CommandContext commandContext) {
        Object obj = commandContext.getAttribute(ATTRIBUTE_INVOLVED_EXECUTIONS);
        if (obj != null) {
            return (Map<String, ExecutionEntity>) obj;
        }
        return null;
    }

    public static void clearInvolvedExecutions(CommandContext commandContext) {
        commandContext.removeAttribute(ATTRIBUTE_INVOLVED_EXECUTIONS);
    }

    public static TableDataManager getTableDataManager() {
        return getTableDataManager(getCommandContext());
    }

    public static TableDataManager getTableDataManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getTableDataManager();
    }

    public static ByteArrayEntityManager getByteArrayEntityManager() {
        return getByteArrayEntityManager(getCommandContext());
    }

    public static ByteArrayEntityManager getByteArrayEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getByteArrayEntityManager();
    }

    public static ResourceEntityManager getResourceEntityManager() {
        return getResourceEntityManager(getCommandContext());
    }

    public static ResourceEntityManager getResourceEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getResourceEntityManager();
    }

    public static DeploymentEntityManager getDeploymentEntityManager() {
        return getDeploymentEntityManager(getCommandContext());
    }

    public static DeploymentEntityManager getDeploymentEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getDeploymentEntityManager();
    }

    public static PropertyEntityManager getPropertyEntityManager() {
        return getPropertyEntityManager(getCommandContext());
    }

    public static PropertyEntityManager getPropertyEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getPropertyEntityManager();
    }

    public static ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
        return getProcessDefinitionEntityManager(getCommandContext());
    }

    public static ProcessDefinitionEntityManager getProcessDefinitionEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getProcessDefinitionEntityManager();
    }

    public static ProcessDefinitionInfoEntityManager getProcessDefinitionInfoEntityManager() {
        return getProcessDefinitionInfoEntityManager(getCommandContext());
    }

    public static ProcessDefinitionInfoEntityManager getProcessDefinitionInfoEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getProcessDefinitionInfoEntityManager();
    }

    public static ExecutionEntityManager getExecutionEntityManager() {
        return getExecutionEntityManager(getCommandContext());
    }

    public static ExecutionEntityManager getExecutionEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getExecutionEntityManager();
    }

    public static CommentEntityManager getCommentEntityManager() {
        return getCommentEntityManager(getCommandContext());
    }

    public static CommentEntityManager getCommentEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getCommentEntityManager();
    }

    public static ModelEntityManager getModelEntityManager() {
        return getModelEntityManager(getCommandContext());
    }

    public static ModelEntityManager getModelEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getModelEntityManager();
    }

    public static HistoryManager getHistoryManager() {
        return getHistoryManager(getCommandContext());
    }

    public static HistoricProcessInstanceEntityManager getHistoricProcessInstanceEntityManager() {
        return getHistoricProcessInstanceEntityManager(getCommandContext());
    }

    public static HistoricProcessInstanceEntityManager getHistoricProcessInstanceEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoricProcessInstanceEntityManager();
    }
    
    public static ActivityInstanceEntityManager getActivityInstanceEntityManager() {
        return getActivityInstanceEntityManager(getCommandContext());
    }
    
    public static ActivityInstanceEntityManager getActivityInstanceEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getActivityInstanceEntityManager();
    }

    public static HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager() {
        return getHistoricActivityInstanceEntityManager(getCommandContext());
    }

    public static HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoricActivityInstanceEntityManager();
    }

    public static HistoryManager getHistoryManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoryManager();
    }

    public static HistoricDetailEntityManager getHistoricDetailEntityManager() {
        return getHistoricDetailEntityManager(getCommandContext());
    }

    public static HistoricDetailEntityManager getHistoricDetailEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoricDetailEntityManager();
    }

    public static AttachmentEntityManager getAttachmentEntityManager() {
        return getAttachmentEntityManager(getCommandContext());
    }

    public static AttachmentEntityManager getAttachmentEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getAttachmentEntityManager();
    }

    public static EventLogEntryEntityManager getEventLogEntryEntityManager() {
        return getEventLogEntryEntityManager(getCommandContext());
    }

    public static EventLogEntryEntityManager getEventLogEntryEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getEventLogEntryEntityManager();
    }

    public static FlowableEventDispatcher getEventDispatcher() {
        return getEventDispatcher(getCommandContext());
    }

    public static FlowableEventDispatcher getEventDispatcher(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getEventDispatcher();
    }

    public static FailedJobCommandFactory getFailedJobCommandFactory() {
        return getFailedJobCommandFactory(getCommandContext());
    }

    public static FailedJobCommandFactory getFailedJobCommandFactory(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getFailedJobCommandFactory();
    }

    public static ProcessInstanceHelper getProcessInstanceHelper() {
        return getProcessInstanceHelper(getCommandContext());
    }

    public static ProcessInstanceHelper getProcessInstanceHelper(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
    }

    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }
}
