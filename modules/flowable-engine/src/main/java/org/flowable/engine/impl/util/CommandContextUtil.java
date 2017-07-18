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

import org.flowable.content.api.ContentEngineConfigurationApi;
import org.flowable.content.api.ContentService;
import org.flowable.dmn.api.DmnEngineConfigurationApi;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnRuleService;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.db.DbSqlSession;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.engine.impl.asyncexecutor.JobManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManager;
import org.flowable.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.flowable.engine.impl.persistence.entity.CommentEntityManager;
import org.flowable.engine.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManager;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntityManager;
import org.flowable.engine.impl.persistence.entity.IdentityLinkEntityManager;
import org.flowable.engine.impl.persistence.entity.JobEntityManager;
import org.flowable.engine.impl.persistence.entity.ModelEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.flowable.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.engine.impl.persistence.entity.SuspendedJobEntityManager;
import org.flowable.engine.impl.persistence.entity.TableDataManager;
import org.flowable.engine.impl.persistence.entity.TaskEntityManager;
import org.flowable.engine.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.engine.impl.persistence.entity.VariableInstanceEntityManager;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntityManager;

public class CommandContextUtil {
    
    public static final String ATTRIBUTE_INVOLVED_EXECUTIONS = "ctx.attribue.involvedExecutions";
    
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
    
    public static IdmEngineConfiguration getIdmEngineConfiguration() {
        return getIdmEngineConfiguration(getCommandContext());
    }
    
    public static IdmEngineConfiguration getIdmEngineConfiguration(CommandContext commandContext) {
        return (IdmEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
    }
    
    public static IdmIdentityService getIdmIdentityService() {
        IdmIdentityService idmIdentityService = null;
        IdmEngineConfiguration idmEngineConfiguration = getIdmEngineConfiguration();
        if (idmEngineConfiguration != null) {
            idmIdentityService = idmEngineConfiguration.getIdmIdentityService();
        }
        
        return idmIdentityService;
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
    
    public static DmnRuleService getDmnRuleService() {
        DmnRuleService dmnRuleService = null;
        DmnEngineConfigurationApi dmnEngineConfiguration = getDmnEngineConfiguration();
        if (dmnEngineConfiguration != null) {
            dmnRuleService = dmnEngineConfiguration.getDmnRuleService();
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
        FormRepositoryService formRepositoryService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration();
        if (formEngineConfiguration != null) {
            formRepositoryService = formEngineConfiguration.getFormRepositoryService();
        }
        
        return formRepositoryService;
    }
    
    public static FormService getFormService() {
        FormService formService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration();
        if (formEngineConfiguration != null) {
            formService = formEngineConfiguration.getFormService();
        }
        
        return formService;
    }
    
    public static FormManagementService getFormManagementService() {
        FormManagementService formManagementService = null;
        FormEngineConfigurationApi formEngineConfiguration = getFormEngineConfiguration();
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
        ContentService contentService = null;
        ContentEngineConfigurationApi contentEngineConfiguration = getContentEngineConfiguration();
        if (contentEngineConfiguration != null) {
            contentService = contentEngineConfiguration.getContentService();
        }
        
        return contentService;
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
    
    public static boolean hasInvolvedExecutions(CommandContext commandContext) {
        return getInvolvedExecutions(commandContext) != null;
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
    
    public static TaskEntityManager getTaskEntityManager() {
        return getTaskEntityManager(getCommandContext());
    }
    
    public static TaskEntityManager getTaskEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getTaskEntityManager();
    }
    
    public static VariableInstanceEntityManager getVariableInstanceEntityManager() {
        return getVariableInstanceEntityManager(getCommandContext());
    }
    
    public static VariableInstanceEntityManager getVariableInstanceEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getVariableInstanceEntityManager();
    }
    
    public static JobEntityManager getJobEntityManager() {
        return getJobEntityManager(getCommandContext());
    }
    
    public static JobEntityManager getJobEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getJobEntityManager();
    }
    
    public static TimerJobEntityManager getTimerJobEntityManager() {
        return getTimerJobEntityManager(getCommandContext());
    }
    
    public static TimerJobEntityManager getTimerJobEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getTimerJobEntityManager();
    }
    
    public static SuspendedJobEntityManager getSuspendedJobEntityManager() {
        return getSuspendedJobEntityManager(getCommandContext());
    }
    
    public static SuspendedJobEntityManager getSuspendedJobEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getSuspendedJobEntityManager();
    }
    
    public static DeadLetterJobEntityManager getDeadLetterJobEntityManager() {
        return getDeadLetterJobEntityManager(getCommandContext());
    }
    
    public static DeadLetterJobEntityManager getDeadLetterJobEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getDeadLetterJobEntityManager();
    }
    
    public static JobManager getJobManager() {
        return getJobManager(getCommandContext());
    }
    
    public static JobManager getJobManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getJobManager();
    }
    
    public static EventSubscriptionEntityManager getEventSubscriptionEntityManager() {
        return getEventSubscriptionEntityManager(getCommandContext());
    }
    
    public static EventSubscriptionEntityManager getEventSubscriptionEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getEventSubscriptionEntityManager();
    }
    
    public static IdentityLinkEntityManager getIdentityLinkEntityManager() {
        return getIdentityLinkEntityManager(getCommandContext());
    }
    
    public static IdentityLinkEntityManager getIdentityLinkEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getIdentityLinkEntityManager();
    }
    
    public static PrivilegeEntityManager getPrivilegeEntityManager() {
        return getPrivilegeEntityManager(getCommandContext());
    }
    
    public static PrivilegeEntityManager getPrivilegeEntityManager(CommandContext commandContext) {
        return getIdmEngineConfiguration(commandContext).getPrivilegeEntityManager();
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
    
    public static HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
        return getHistoricTaskInstanceEntityManager(getCommandContext());
    }
    
    public static HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoricTaskInstanceEntityManager();
    }
    
    public static HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager() {
        return getHistoricActivityInstanceEntityManager(getCommandContext());
    }
    
    public static HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoricActivityInstanceEntityManager();
    }
    
    public static HistoricVariableInstanceEntityManager getHistoricVariableInstanceEntityManager() {
        return getHistoricVariableInstanceEntityManager(getCommandContext());
    }
    
    public static HistoricVariableInstanceEntityManager getHistoricVariableInstanceEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoricVariableInstanceEntityManager();
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
    
    public static HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
        return getHistoricIdentityLinkEntityManager(getCommandContext());
    }
    
    public static HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoricIdentityLinkEntityManager();
    }
    
    public static HistoryJobEntityManager getHistoryJobEntityManager() {
        return getHistoryJobEntityManager(getCommandContext());
    }
    
    public static HistoryJobEntityManager getHistoryJobEntityManager(CommandContext commandContext) {
        return getProcessEngineConfiguration(commandContext).getHistoryJobEntityManager();
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
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
