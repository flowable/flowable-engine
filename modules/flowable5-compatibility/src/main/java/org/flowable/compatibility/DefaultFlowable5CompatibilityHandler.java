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

package org.flowable.compatibility;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.JobProcessorContextImpl;
import org.activiti.engine.impl.asyncexecutor.AsyncJobUtil;
import org.activiti.engine.impl.bpmn.behavior.BpmnActivityBehavior;
import org.activiti.engine.impl.bpmn.helper.ErrorPropagation;
import org.activiti.engine.impl.bpmn.helper.ErrorThrowingEventListener;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.engine.impl.persistence.entity.AbstractJobEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.runtime.JobProcessor;
import org.activiti.engine.runtime.JobProcessorContext;
import org.activiti.engine.runtime.ProcessInstanceBuilder;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.common.engine.api.FlowableClassLoadingException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.compatibility.wrapper.Flowable5AttachmentWrapper;
import org.flowable.compatibility.wrapper.Flowable5CommentWrapper;
import org.flowable.compatibility.wrapper.Flowable5DeploymentWrapper;
import org.flowable.compatibility.wrapper.Flowable5ProcessInstanceWrapper;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.impl.cmd.AddIdentityLinkCmd;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.repository.DeploymentBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.variable.api.persistence.entity.VariableInstance;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class DefaultFlowable5CompatibilityHandler implements Flowable5CompatibilityHandler {

    protected DefaultProcessEngineFactory processEngineFactory;
    protected volatile ProcessEngine processEngine;
    protected volatile org.flowable.engine.ProcessEngineConfiguration flowable6ProcessEngineConfiguration;

    @Override
    public ProcessDefinition getProcessDefinition(final String processDefinitionId) {
        final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        ProcessDefinition processDefinitionEntity = processEngineConfig.getCommandExecutor().execute(new Command<ProcessDefinition>() {

            @Override
            public ProcessDefinition execute(CommandContext commandContext) {
                return processEngineConfig.getDeploymentManager().findDeployedProcessDefinitionById(processDefinitionId);
            }
        });

        return processDefinitionEntity;
    }

    @Override
    public ProcessDefinition getProcessDefinitionByKey(final String processDefinitionKey) {
        final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        ProcessDefinition processDefinition = processEngineConfig.getCommandExecutor().execute(new Command<ProcessDefinition>() {

            @Override
            public ProcessDefinition execute(CommandContext commandContext) {
                return processEngineConfig.getDeploymentManager().findDeployedLatestProcessDefinitionByKey(processDefinitionKey);
            }
        });

        return processDefinition;
    }

    @Override
    public org.flowable.bpmn.model.Process getProcessDefinitionProcessObject(final String processDefinitionId) {
        final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        org.flowable.bpmn.model.Process process = processEngineConfig.getCommandExecutor().execute(new Command<org.flowable.bpmn.model.Process>() {

            @Override
            public org.flowable.bpmn.model.Process execute(CommandContext commandContext) {
                org.flowable.bpmn.model.Process process = null;
                DeploymentManager deploymentManager = processEngineConfig.getDeploymentManager();
                ProcessDefinition processDefinition = deploymentManager.findDeployedProcessDefinitionById(processDefinitionId);
                if (processDefinition != null) {
                    BpmnModel bpmnModel = deploymentManager.getBpmnModelById(processDefinitionId);
                    if (bpmnModel != null) {
                        process = bpmnModel.getProcessById(processDefinition.getKey());
                    }
                }
                return process;
            }
        });

        return process;
    }

    @Override
    public BpmnModel getProcessDefinitionBpmnModel(final String processDefinitionId) {
        final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        return processEngineConfig.getDeploymentManager().getBpmnModelById(processDefinitionId);
    }

    @Override
    public void addCandidateStarter(String processDefinitionId, String userId, String groupId) {
        try {
            if (userId != null) {
                getProcessEngine().getRepositoryService().addCandidateStarterUser(processDefinitionId, userId);
            } else {
                getProcessEngine().getRepositoryService().addCandidateStarterGroup(processDefinitionId, groupId);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public ObjectNode getProcessDefinitionInfo(String processDefinitionId) {
        try {
            return getProcessEngine().getDynamicBpmnService().getProcessDefinitionInfo(processDefinitionId);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public ProcessDefinitionCacheEntry resolveProcessDefinition(final ProcessDefinition processDefinition) {
        try {
            final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
            ProcessDefinitionCacheEntry cacheEntry = processEngineConfig.getCommandExecutor().execute(new Command<ProcessDefinitionCacheEntry>() {

                @Override
                public ProcessDefinitionCacheEntry execute(CommandContext commandContext) {
                    return commandContext.getProcessEngineConfiguration().getDeploymentManager().resolveProcessDefinition(processDefinition);
                }
            });

            return cacheEntry;

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public boolean isProcessDefinitionSuspended(String processDefinitionId) {
        try {
            return getProcessEngine().getRepositoryService().isProcessDefinitionSuspended(processDefinitionId);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return false;
        }
    }

    @Override
    public void deleteCandidateStarter(String processDefinitionId, String userId, String groupId) {
        try {
            if (userId != null) {
                getProcessEngine().getRepositoryService().deleteCandidateStarterUser(processDefinitionId, userId);
            } else {
                getProcessEngine().getRepositoryService().deleteCandidateStarterGroup(processDefinitionId, groupId);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void suspendProcessDefinition(String processDefinitionId, String processDefinitionKey, boolean suspendProcessInstances, Date suspensionDate, String tenantId) {
        try {
            if (processDefinitionId != null) {
                getProcessEngine().getRepositoryService().suspendProcessDefinitionById(processDefinitionId, suspendProcessInstances, suspensionDate);
            } else {
                getProcessEngine().getRepositoryService().suspendProcessDefinitionByKey(processDefinitionKey, suspendProcessInstances, suspensionDate, tenantId);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void activateProcessDefinition(String processDefinitionId, String processDefinitionKey, boolean activateProcessInstances, Date activationDate, String tenantId) {
        try {
            if (processDefinitionId != null) {
                getProcessEngine().getRepositoryService().activateProcessDefinitionById(processDefinitionId, activateProcessInstances, activationDate);
            } else {
                getProcessEngine().getRepositoryService().activateProcessDefinitionByKey(processDefinitionKey, activateProcessInstances, activationDate, tenantId);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void setProcessDefinitionCategory(String processDefinitionId, String category) {
        try {
            getProcessEngine().getRepositoryService().setProcessDefinitionCategory(processDefinitionId, category);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public Deployment deploy(DeploymentBuilderImpl activiti6DeploymentBuilder) {
        try {
            DeploymentBuilder deploymentBuilder = getProcessEngine().getRepositoryService().createDeployment();

            // Copy settings

            deploymentBuilder.name(activiti6DeploymentBuilder.getDeployment().getName());
            deploymentBuilder.category(activiti6DeploymentBuilder.getDeployment().getCategory());
            deploymentBuilder.tenantId(activiti6DeploymentBuilder.getDeployment().getTenantId());

            // Copy flags

            if (!activiti6DeploymentBuilder.isBpmn20XsdValidationEnabled()) {
                deploymentBuilder.disableSchemaValidation();
            }

            if (!activiti6DeploymentBuilder.isProcessValidationEnabled()) {
                deploymentBuilder.disableBpmnValidation();
            }

            if (activiti6DeploymentBuilder.isDuplicateFilterEnabled()) {
                deploymentBuilder.enableDuplicateFiltering();
            }

            if (activiti6DeploymentBuilder.getProcessDefinitionsActivationDate() != null) {
                deploymentBuilder.activateProcessDefinitionsOn(activiti6DeploymentBuilder.getProcessDefinitionsActivationDate());
            }

            // Copy resources
            DeploymentEntity activiti6DeploymentEntity = activiti6DeploymentBuilder.getDeployment();
            Map<String, org.activiti.engine.impl.persistence.entity.ResourceEntity> activiti5Resources = new HashMap<>();
            for (String resourceKey : activiti6DeploymentEntity.getResources().keySet()) {
                EngineResource activiti6ResourceEntity = activiti6DeploymentEntity.getResources().get(resourceKey);

                org.activiti.engine.impl.persistence.entity.ResourceEntity activiti5ResourceEntity = new org.activiti.engine.impl.persistence.entity.ResourceEntity();
                activiti5ResourceEntity.setName(activiti6ResourceEntity.getName());
                activiti5ResourceEntity.setBytes(activiti6ResourceEntity.getBytes());
                activiti5Resources.put(resourceKey, activiti5ResourceEntity);
            }

            org.activiti.engine.impl.persistence.entity.DeploymentEntity activiti5DeploymentEntity = ((org.activiti.engine.impl.repository.DeploymentBuilderImpl) deploymentBuilder).getDeployment();
            activiti5DeploymentEntity.setResources(activiti5Resources);

            return new Flowable5DeploymentWrapper(deploymentBuilder.deploy());

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public void setDeploymentCategory(String deploymentId, String category) {
        try {
            getProcessEngine().getRepositoryService().setDeploymentCategory(deploymentId, category);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void changeDeploymentTenantId(String deploymentId, String newTenantId) {
        try {
            getProcessEngine().getRepositoryService().changeDeploymentTenantId(deploymentId, newTenantId);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        try {
            final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
            processEngineConfig.getRepositoryService().deleteDeployment(deploymentId, cascade);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public ProcessInstance startProcessInstance(String processDefinitionKey, String processDefinitionId,
                                                Map<String, Object> variables, Map<String, Object> transientVariables, String businessKey, String tenantId, String processInstanceName) {

        org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());

        try {

            ProcessInstanceBuilder processInstanceBuilder = getProcessEngine().getRuntimeService().createProcessInstanceBuilder();
            if (processDefinitionKey != null) {
                processInstanceBuilder.processDefinitionKey(processDefinitionKey);
            }
            if (processDefinitionId != null) {
                processInstanceBuilder.processDefinitionId(processDefinitionId);
            }
            if (variables != null) {
                processInstanceBuilder.variables(variables);
            }
            if (transientVariables != null) {
                processInstanceBuilder.transientVariables(transientVariables);
            }
            if (businessKey != null) {
                processInstanceBuilder.businessKey(businessKey);
            }
            if (tenantId != null) {
                processInstanceBuilder.tenantId(tenantId);
            }
            if (processInstanceName != null) {
                processInstanceBuilder.name(processInstanceName);
            }

            org.activiti.engine.runtime.ProcessInstance activiti5ProcessInstance = processInstanceBuilder.start();
            return new Flowable5ProcessInstanceWrapper(activiti5ProcessInstance);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public ProcessInstance startProcessInstanceByMessage(String messageName, Map<String, Object> variables,
                                                         Map<String, Object> transientVariables, String businessKey, String tenantId) {

        try {

            ProcessInstanceBuilder processInstanceBuilder = getProcessEngine().getRuntimeService().createProcessInstanceBuilder();
            if (messageName != null) {
                processInstanceBuilder.messageName(messageName);
            }
            if (variables != null) {
                processInstanceBuilder.variables(variables);
            }
            if (transientVariables != null) {
                processInstanceBuilder.transientVariables(transientVariables);
            }
            if (businessKey != null) {
                processInstanceBuilder.businessKey(businessKey);
            }
            if (tenantId != null) {
                processInstanceBuilder.tenantId(tenantId);
            }

            org.activiti.engine.runtime.ProcessInstance activiti5ProcessInstance = processInstanceBuilder.start();
            return new Flowable5ProcessInstanceWrapper(activiti5ProcessInstance);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }
    
    @Override
    public ProcessInstance getProcessInstance(String processInstanceId) {
        org.activiti.engine.runtime.ProcessInstance processInstance = getProcessEngine().getRuntimeService()
                .createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (processInstance != null) {
            return new Flowable5ProcessInstanceWrapper(processInstance);
        } else {
            CommandContext commandContext = Context.getCommandContext();
            if (commandContext != null) {
                ExecutionEntity execution = commandContext
                        .getExecutionEntityManager()
                        .findExecutionById(processInstanceId);
                if (execution != null) {
                    return new Flowable5ProcessInstanceWrapper(execution);
                }
            }
        }
        return null;
    }
    
    @Override
    public void setProcessInstanceName(String processInstanceId, String processInstanceName) {
        getProcessEngine().getRuntimeService().setProcessInstanceName(processInstanceId, processInstanceName);
    }

    @Override
    public Object getExecutionVariable(String executionId, String variableName, boolean isLocal) {
        try {
            if (isLocal) {
                return getProcessEngine().getRuntimeService().getVariableLocal(executionId, variableName);
            } else {
                return getProcessEngine().getRuntimeService().getVariable(executionId, variableName);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public VariableInstance getExecutionVariableInstance(String executionId, String variableName, boolean isLocal) {
        try {
            if (isLocal) {
                return getProcessEngine().getRuntimeService().getVariableInstanceLocal(executionId, variableName);
            } else {
                return getProcessEngine().getRuntimeService().getVariableInstance(executionId, variableName);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getExecutionVariables(String executionId, Collection<String> variableNames, boolean isLocal) {
        try {
            if (isLocal) {
                return getProcessEngine().getRuntimeService().getVariablesLocal(executionId, variableNames);
            } else {
                return getProcessEngine().getRuntimeService().getVariables(executionId, variableNames);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public Map<String, VariableInstance> getExecutionVariableInstances(String executionId, Collection<String> variableNames, boolean isLocal) {
        try {
            if (isLocal) {
                return getProcessEngine().getRuntimeService().getVariableInstancesLocal(executionId, variableNames);
            } else {
                return getProcessEngine().getRuntimeService().getVariableInstances(executionId, variableNames);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public void setExecutionVariables(String executionId, Map<String, ? extends Object> variables, boolean isLocal) {
        try {
            if (isLocal) {
                getProcessEngine().getRuntimeService().setVariablesLocal(executionId, variables);
            } else {
                getProcessEngine().getRuntimeService().setVariables(executionId, variables);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void removeExecutionVariables(String executionId, Collection<String> variableNames, boolean isLocal) {
        try {
            if (isLocal) {
                getProcessEngine().getRuntimeService().removeVariablesLocal(executionId, variableNames);
            } else {
                getProcessEngine().getRuntimeService().removeVariables(executionId, variableNames);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void updateBusinessKey(String processInstanceId, String businessKey) {
        try {
            getProcessEngine().getRuntimeService().updateBusinessKey(processInstanceId, businessKey);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void suspendProcessInstance(String processInstanceId) {
        try {
            getProcessEngine().getRuntimeService().suspendProcessInstanceById(processInstanceId);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void activateProcessInstance(String processInstanceId) {
        try {
            getProcessEngine().getRuntimeService().activateProcessInstanceById(processInstanceId);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        try {
            getProcessEngine().getRuntimeService().deleteProcessInstance(processInstanceId, deleteReason);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void deleteHistoricProcessInstance(String processInstanceId) {
        try {
            getProcessEngine().getHistoryService().deleteHistoricProcessInstance(processInstanceId);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void addIdentityLinkForProcessInstance(String processInstanceId, String userId, String groupId, String identityLinkType) {
        try {
            if (userId != null) {
                getProcessEngine().getRuntimeService().addUserIdentityLink(processInstanceId, userId, identityLinkType);
            } else {
                getProcessEngine().getRuntimeService().addGroupIdentityLink(processInstanceId, groupId, identityLinkType);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void deleteIdentityLinkForProcessInstance(String processInstanceId, String userId, String groupId, String identityLinkType) {
        try {
            if (userId != null) {
                getProcessEngine().getRuntimeService().deleteUserIdentityLink(processInstanceId, userId, identityLinkType);
            } else {
                getProcessEngine().getRuntimeService().deleteGroupIdentityLink(processInstanceId, groupId, identityLinkType);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void completeTask(TaskEntity taskEntity, Map<String, Object> variables, boolean localScope) {
        org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());
        try {
            getProcessEngine().getTaskService().complete(taskEntity.getId(), variables, localScope);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void completeTask(TaskEntity taskEntity, Map<String, Object> variables, Map<String, Object> transientVariables) {
        org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());
        try {
            getProcessEngine().getTaskService().complete(taskEntity.getId(), variables, transientVariables);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void claimTask(String taskId, String userId) {
        org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());
        try {
            getProcessEngine().getTaskService().claim(taskId, userId);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void setTaskVariables(String taskId, Map<String, ? extends Object> variables, boolean isLocal) {
        try {
            if (isLocal) {
                getProcessEngine().getTaskService().setVariablesLocal(taskId, variables);
            } else {
                getProcessEngine().getTaskService().setVariables(taskId, variables);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void removeTaskVariables(String taskId, Collection<String> variableNames, boolean isLocal) {
        try {
            if (isLocal) {
                getProcessEngine().getTaskService().removeVariablesLocal(taskId, variableNames);
            } else {
                getProcessEngine().getTaskService().removeVariables(taskId, variableNames);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void setTaskDueDate(String taskId, Date dueDate) {
        try {
            getProcessEngine().getTaskService().setDueDate(taskId, dueDate);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void setTaskPriority(String taskId, int priority) {
        try {
            getProcessEngine().getTaskService().setPriority(taskId, priority);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void deleteTask(String taskId, String deleteReason, boolean cascade) {
        try {
            if (deleteReason != null) {
                getProcessEngine().getTaskService().deleteTask(taskId, deleteReason);
            } else {
                getProcessEngine().getTaskService().deleteTask(taskId, cascade);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void deleteHistoricTask(String taskId) {
        try {
            getProcessEngine().getHistoryService().deleteHistoricTaskInstance(taskId);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public StartFormData getStartFormData(String processDefinitionId) {
        try {
            return getProcessEngine().getFormService().getStartFormData(processDefinitionId);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public String getFormKey(String processDefinitionId, String taskDefinitionKey) {
        try {
            if (taskDefinitionKey != null) {
                return getProcessEngine().getFormService().getTaskFormKey(processDefinitionId, taskDefinitionKey);
            } else {
                return getProcessEngine().getFormService().getStartFormKey(processDefinitionId);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public Object getRenderedStartForm(String processDefinitionId, String formEngineName) {
        try {
            return getProcessEngine().getFormService().getRenderedStartForm(processDefinitionId, formEngineName);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public ProcessInstance submitStartFormData(String processDefinitionId, String businessKey, Map<String, String> properties) {
        org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());
        try {
            return new Flowable5ProcessInstanceWrapper(getProcessEngine().getFormService().submitStartFormData(processDefinitionId, businessKey, properties));
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public void submitTaskFormData(String taskId, Map<String, String> properties, boolean completeTask) {
        org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());
        try {
            if (completeTask) {
                getProcessEngine().getFormService().submitTaskFormData(taskId, properties);
            } else {
                getProcessEngine().getFormService().saveFormData(taskId, properties);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void saveTask(TaskEntity task) {
        try {
            org.activiti.engine.impl.persistence.entity.TaskEntity activiti5Task = convertToActiviti5TaskEntity(task);
            getProcessEngine().getTaskService().saveTask(activiti5Task);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void addIdentityLink(String taskId, String identityId, int identityIdType, String identityType) {
        if (identityIdType == AddIdentityLinkCmd.IDENTITY_USER) {
            getProcessEngine().getTaskService().addUserIdentityLink(taskId, identityId, identityType);
        } else if (identityIdType == AddIdentityLinkCmd.IDENTITY_GROUP) {
            getProcessEngine().getTaskService().addGroupIdentityLink(taskId, identityId, identityType);
        }
    }

    @Override
    public void deleteIdentityLink(String taskId, String userId, String groupId, String identityLinkType) {
        if (userId != null) {
            getProcessEngine().getTaskService().deleteUserIdentityLink(taskId, userId, identityLinkType);
        } else {
            getProcessEngine().getTaskService().deleteGroupIdentityLink(taskId, groupId, identityLinkType);
        }
    }

    @Override
    public Comment addComment(String taskId, String processInstanceId, String type, String message) {
        try {
            return new Flowable5CommentWrapper(getProcessEngine().getTaskService().addComment(taskId, processInstanceId, type, message));

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public void deleteComment(String commentId, String taskId, String processInstanceId) {
        try {
            if (commentId != null) {
                getProcessEngine().getTaskService().deleteComment(commentId);
            } else {
                getProcessEngine().getTaskService().deleteComments(taskId, processInstanceId);
            }

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public Attachment createAttachment(String attachmentType, String taskId, String processInstanceId, String attachmentName, String attachmentDescription, InputStream content, String url) {
        try {
            org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());
            if (content != null) {
                return new Flowable5AttachmentWrapper(getProcessEngine().getTaskService().createAttachment(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, content));
            } else {
                return new Flowable5AttachmentWrapper(getProcessEngine().getTaskService().createAttachment(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, url));
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public void saveAttachment(Attachment attachment) {
        try {
            org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());
            org.activiti.engine.task.Attachment activiti5Attachment = getProcessEngine().getTaskService().getAttachment(attachment.getId());
            activiti5Attachment.setName(attachment.getName());
            activiti5Attachment.setDescription(attachment.getDescription());
            activiti5Attachment.setTime(attachment.getTime());
            getProcessEngine().getTaskService().saveAttachment(activiti5Attachment);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void deleteAttachment(String attachmentId) {
        try {
            org.activiti.engine.impl.identity.Authentication.setAuthenticatedUserId(Authentication.getAuthenticatedUserId());
            getProcessEngine().getTaskService().deleteAttachment(attachmentId);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void trigger(String executionId, Map<String, Object> processVariables, Map<String, Object> transientVariables) {
        try {
            if (transientVariables == null) {
                getProcessEngine().getRuntimeService().signal(executionId, processVariables);
            } else {
                getProcessEngine().getRuntimeService().signal(executionId, processVariables, transientVariables);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void messageEventReceived(String messageName, String executionId, Map<String, Object> processVariables, boolean async) {
        try {
            if (!async) {
                getProcessEngine().getRuntimeService().messageEventReceived(messageName, executionId, processVariables);
            } else {
                getProcessEngine().getRuntimeService().messageEventReceivedAsync(messageName, executionId);
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void signalEventReceived(String signalName, String executionId, Map<String, Object> processVariables, boolean async, String tenantId) {
        try {
            if (tenantId != null) {
                if (async) {
                    getProcessEngine().getRuntimeService().signalEventReceivedAsyncWithTenantId(signalName, tenantId);
                } else {
                    getProcessEngine().getRuntimeService().signalEventReceivedWithTenantId(signalName, processVariables, tenantId);
                }
            } else {
                if (async) {
                    getProcessEngine().getRuntimeService().signalEventReceivedAsync(signalName, executionId);
                } else {
                    getProcessEngine().getRuntimeService().signalEventReceived(signalName, executionId, processVariables);
                }
            }
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void signalEventReceived(final SignalEventSubscriptionEntity signalEventSubscriptionEntity, final Object payload, final boolean async) {
        final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        processEngineConfig.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                org.activiti.engine.impl.persistence.entity.SignalEventSubscriptionEntity activiti5SignalEvent = new org.activiti.engine.impl.persistence.entity.SignalEventSubscriptionEntity();
                activiti5SignalEvent.setId(signalEventSubscriptionEntity.getId());
                activiti5SignalEvent.setExecutionId(signalEventSubscriptionEntity.getExecutionId());
                activiti5SignalEvent.setActivityId(signalEventSubscriptionEntity.getActivityId());
                activiti5SignalEvent.setEventName(signalEventSubscriptionEntity.getEventName());
                activiti5SignalEvent.setEventType(signalEventSubscriptionEntity.getEventType());
                activiti5SignalEvent.setConfiguration(signalEventSubscriptionEntity.getConfiguration());
                activiti5SignalEvent.setProcessDefinitionId(signalEventSubscriptionEntity.getProcessDefinitionId());
                activiti5SignalEvent.setProcessInstanceId(signalEventSubscriptionEntity.getProcessInstanceId());
                activiti5SignalEvent.setTenantId(signalEventSubscriptionEntity.getTenantId());
                activiti5SignalEvent.setRevision(signalEventSubscriptionEntity.getRevision());
                activiti5SignalEvent.eventReceived(payload, async);
                return null;
            }
        });

    }

    @Override
    public void executeJob(Job job) {
        if (job == null)
            return;
        final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        final org.activiti.engine.impl.persistence.entity.JobEntity activiti5Job = convertToActiviti5JobEntity((JobEntity) job);

        callJobProcessors(JobProcessorContext.Phase.BEFORE_EXECUTE, activiti5Job, processEngineConfig);
        processEngineConfig.getCommandExecutor().execute(new ExecuteJobsCmd(activiti5Job));
    }

    @Override
    public void executeJobWithLockAndRetry(Job job) {
        if (job == null)
            return;
        final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        org.activiti.engine.impl.persistence.entity.JobEntity activiti5Job;
        if (job instanceof org.activiti.engine.impl.persistence.entity.JobEntity) {
            activiti5Job = (org.activiti.engine.impl.persistence.entity.JobEntity) job;
        } else {
            activiti5Job = convertToActiviti5JobEntity((JobEntity) job);
        }

        callJobProcessors(JobProcessorContext.Phase.BEFORE_EXECUTE, activiti5Job, processEngineConfig);
        AsyncJobUtil.executeJob(activiti5Job, processEngineConfig.getCommandExecutor());
    }

    @Override
    public void handleFailedJob(Job job, Throwable exception) {
        if (job == null)
            return;
        final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        final org.activiti.engine.impl.persistence.entity.JobEntity activity5Job = convertToActiviti5JobEntity((JobEntity) job);
        AsyncJobUtil.handleFailedJob(activity5Job, exception, processEngineConfig.getCommandExecutor());
    }

    @Override
    public void deleteJob(String jobId) {
        try {
            getProcessEngine().getManagementService().deleteJob(jobId);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void leaveExecution(DelegateExecution execution) {
        try {
            BpmnActivityBehavior bpmnActivityBehavior = new BpmnActivityBehavior();
            bpmnActivityBehavior.performDefaultOutgoingBehavior((ActivityExecution) execution);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public void propagateError(BpmnError bpmnError, DelegateExecution execution) {
        try {
            org.activiti.engine.delegate.BpmnError activiti5BpmnError = new org.activiti.engine.delegate.BpmnError(bpmnError.getErrorCode(), bpmnError.getMessage());
            ErrorPropagation.propagateError(activiti5BpmnError, (ActivityExecution) execution);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
        }
    }

    @Override
    public boolean mapException(Exception camelException, DelegateExecution execution, List<MapExceptionEntry> mapExceptions) {
        try {
            return ErrorPropagation.mapException(camelException, (ExecutionEntity) execution, mapExceptions);
        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getVariables(ProcessInstance processInstance) {
        org.activiti.engine.runtime.ProcessInstance activiti5ProcessInstance = ((Flowable5ProcessInstanceWrapper) processInstance).getRawObject();
        return ((ExecutionEntity) activiti5ProcessInstance).getVariables();
    }

    @Override
    public Object getScriptingEngineValue(String payloadExpressionValue, String languageValue, DelegateExecution execution) {
        try {
            final ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
            ScriptingEngines scriptingEngines = processEngineConfig.getScriptingEngines();
            return scriptingEngines.evaluate(payloadExpressionValue, languageValue, execution);

        } catch (org.activiti.engine.ActivitiException e) {
            handleActivitiException(e);
            return null;
        }
    }

    @Override
    public void throwErrorEvent(FlowableEvent event) {
        ErrorThrowingEventListener eventListener = new ErrorThrowingEventListener();
        eventListener.onEvent(event);
    }

    @Override
    public void setClock(Clock clock) {
        ProcessEngineConfiguration processEngineConfig = getProcessEngine().getProcessEngineConfiguration();
        if (processEngineConfig.getClock() == null) {
            getProcessEngine().getProcessEngineConfiguration().setClock(clock);
        } else {
            Clock activiti5Clock = processEngineConfig.getClock();
            activiti5Clock.setCurrentCalendar(clock.getCurrentCalendar());
        }
    }

    @Override
    public void resetClock() {
        ProcessEngineConfiguration processEngineConfig = getProcessEngine().getProcessEngineConfiguration();
        if (processEngineConfig.getClock() != null) {
            processEngineConfig.getClock().reset();
        }
    }

    @Override
    public Object getRawProcessEngine() {
        return getProcessEngine();
    }

    @Override
    public Object getRawProcessConfiguration() {
        return getProcessEngine().getProcessEngineConfiguration();
    }

    @Override
    public Object getRawCommandExecutor() {
        ProcessEngineConfigurationImpl processEngineConfig = (ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration();
        return processEngineConfig.getCommandExecutor();
    }

    @Override
    public Object getCamelContextObject(String camelContextValue) {
        throw new FlowableException("Getting the Camel context is not support in this engine configuration");
    }

    @Override
    public void setJobProcessor(List<Object> flowable5JobProcessors) {
        getProcessEngine().getProcessEngineConfiguration().setJobProcessors(convertToFlowable5JobProcessors(flowable5JobProcessors));
    }

    private List<JobProcessor> convertToFlowable5JobProcessors(List<Object> jobProcessors) {
        ArrayList<JobProcessor> flowable5JobProcessors = new ArrayList<>();
        for (Object jobProcessor : jobProcessors) {
            flowable5JobProcessors.add((org.activiti.engine.runtime.JobProcessor) jobProcessor);
        }
        return flowable5JobProcessors;
    }

    protected ProcessEngine getProcessEngine() {
        if (processEngine == null) {
            synchronized (this) {
                if (processEngine == null) {
                    processEngine = getProcessEngineFactory().buildProcessEngine(CommandContextUtil.getProcessEngineConfiguration());
                }
            }
        }
        return processEngine;
    }

    public DefaultProcessEngineFactory getProcessEngineFactory() {
        if (processEngineFactory == null) {
            processEngineFactory = new DefaultProcessEngineFactory();
        }
        return processEngineFactory;
    }

    public void setProcessEngineFactory(DefaultProcessEngineFactory processEngineFactory) {
        this.processEngineFactory = processEngineFactory;
    }

    @Override
    public org.flowable.engine.ProcessEngineConfiguration getFlowable6ProcessEngineConfiguration() {
        return flowable6ProcessEngineConfiguration;
    }

    @Override
    public void setFlowable6ProcessEngineConfiguration(org.flowable.engine.ProcessEngineConfiguration flowable6ProcessEngineConfiguration) {
        this.flowable6ProcessEngineConfiguration = flowable6ProcessEngineConfiguration;
    }

    protected org.activiti.engine.impl.persistence.entity.TaskEntity convertToActiviti5TaskEntity(TaskEntity task) {
        org.activiti.engine.impl.persistence.entity.TaskEntity activiti5Task = new org.activiti.engine.impl.persistence.entity.TaskEntity();
        activiti5Task.setAssigneeWithoutCascade(task.getAssignee());
        activiti5Task.setInitialAssignee(((TaskEntityImpl) task).getOriginalAssignee());
        activiti5Task.setCategoryWithoutCascade(task.getCategory());
        activiti5Task.setCreateTime(task.getCreateTime());
        activiti5Task.setDelegationStateString(((TaskEntityImpl) task).getDelegationStateString());
        activiti5Task.setDescriptionWithoutCascade(task.getDescription());
        activiti5Task.setDueDateWithoutCascade(task.getDueDate());
        activiti5Task.setExecutionId(task.getExecutionId());
        activiti5Task.setFormKeyWithoutCascade(task.getFormKey());
        activiti5Task.setId(task.getId());
        activiti5Task.setNameWithoutCascade(task.getName());
        activiti5Task.setOwnerWithoutCascade(task.getOwner());
        activiti5Task.setParentTaskIdWithoutCascade(task.getParentTaskId());
        activiti5Task.setPriorityWithoutCascade(task.getPriority());
        activiti5Task.setProcessDefinitionId(task.getProcessDefinitionId());
        activiti5Task.setProcessInstanceId(task.getProcessInstanceId());
        activiti5Task.setRevision(task.getRevision());
        activiti5Task.setTaskDefinitionKeyWithoutCascade(task.getTaskDefinitionKey());
        activiti5Task.setTenantId(task.getTenantId());
        return activiti5Task;
    }

    protected org.activiti.engine.impl.persistence.entity.JobEntity convertToActiviti5JobEntity(final JobEntity job) {
        org.activiti.engine.impl.persistence.entity.JobEntity activity5Job = new org.activiti.engine.impl.persistence.entity.JobEntity();
        activity5Job.setJobType(job.getJobType());
        activity5Job.setDuedate(job.getDuedate());
        activity5Job.setExclusive(job.isExclusive());
        activity5Job.setExecutionId(job.getExecutionId());
        activity5Job.setId(job.getId());
        activity5Job.setJobHandlerConfiguration(job.getJobHandlerConfiguration());
        activity5Job.setJobHandlerType(job.getJobHandlerType());
        activity5Job.setEndDate(job.getEndDate());
        activity5Job.setRepeat(job.getRepeat());
        activity5Job.setProcessDefinitionId(job.getProcessDefinitionId());
        activity5Job.setProcessInstanceId(job.getProcessInstanceId());
        activity5Job.setRetries(job.getRetries());
        activity5Job.setRevision(job.getRevision());
        activity5Job.setTenantId(job.getTenantId());
        activity5Job.setExceptionMessage(job.getExceptionMessage());
        return activity5Job;
    }

    protected void handleActivitiException(org.activiti.engine.ActivitiException e) {
        if (e instanceof org.activiti.engine.delegate.BpmnError) {
            org.activiti.engine.delegate.BpmnError activiti5BpmnError = (org.activiti.engine.delegate.BpmnError) e;
            throw new BpmnError(activiti5BpmnError.getErrorCode(), activiti5BpmnError.getMessage());

        } else if (e instanceof org.activiti.engine.ActivitiClassLoadingException) {
            throw new FlowableClassLoadingException(e.getMessage(), e.getCause());

        } else if (e instanceof org.activiti.engine.ActivitiObjectNotFoundException) {
            org.activiti.engine.ActivitiObjectNotFoundException activiti5ObjectNotFoundException = (org.activiti.engine.ActivitiObjectNotFoundException) e;
            throw new FlowableObjectNotFoundException(activiti5ObjectNotFoundException.getMessage(),
                    activiti5ObjectNotFoundException.getObjectClass(), activiti5ObjectNotFoundException.getCause());

        } else if (e instanceof org.activiti.engine.ActivitiOptimisticLockingException) {
            throw new FlowableOptimisticLockingException(e.getMessage());

        } else if (e instanceof org.activiti.engine.ActivitiIllegalArgumentException) {
            throw new FlowableIllegalArgumentException(e.getMessage(), e.getCause());

        } else {
            if (e.getCause() instanceof org.activiti.engine.ActivitiClassLoadingException) {
                throw new FlowableException(e.getMessage(), new FlowableClassLoadingException(e.getCause().getMessage(), e.getCause().getCause()));
            } else if (e.getCause() instanceof org.activiti.engine.impl.javax.el.PropertyNotFoundException) {
                throw new FlowableException(e.getMessage(), new PropertyNotFoundException(e.getCause().getMessage(), e.getCause().getCause()));
            } else if (e.getCause() instanceof org.activiti.engine.ActivitiException) {
                throw new FlowableException(e.getMessage(), new FlowableException(e.getCause().getMessage(), e.getCause().getCause()));
            } else {
                throw new FlowableException(e.getMessage(), e.getCause());
            }
        }
    }

    protected void callJobProcessors(JobProcessorContext.Phase processorType, AbstractJobEntity abstractJobEntity, ProcessEngineConfigurationImpl processEngineConfiguration) {
        JobProcessorContextImpl jobProcessorContext = new JobProcessorContextImpl(processorType, abstractJobEntity);
        for (JobProcessor jobProcessor : processEngineConfiguration.getJobProcessors()) {
            jobProcessor.process(jobProcessorContext);
        }
    }

}
