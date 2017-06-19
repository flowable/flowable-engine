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
package org.flowable.engine.impl.history.async;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.AbstractHistoryManager;
import org.flowable.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.history.async.json.transformer.ActivityEndHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ActivityStartHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.FormPropertiesSubmittedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.HistoricDetailVariableUpdateHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.IdentityLinkCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.IdentityLinkDeletedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceDeleteHistoryByProcessDefinitionIdJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceDeleteHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceEndHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstancePropertyChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceStartHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.SetProcessDefinitionHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.SubProcessInstanceHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskAssigneeChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskEndedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskOwnerChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskPropertyChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.VariableCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.VariableRemovedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.VariableUpdatedHistoryJsonTransformer;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.engine.repository.ProcessDefinition;

public class AsyncHistoryManager extends AbstractHistoryManager {

    public AsyncHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration, HistoryLevel historyLevel) {
        super(processEngineConfiguration, historyLevel);
    }

    public AsyncHistorySession getAsyncHistorySession() {
        return getSession(AsyncHistorySession.class);
    }

    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        return processEngineConfiguration.getHistoryLevel().isAtLeast(level);
    }

    @Override
    public boolean isHistoryEnabled() {
        return processEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE;
    }

    @Override
    public void recordProcessInstanceStart(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, processInstance.getId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.NAME, processInstance.getName());
            putIfNotNull(data, HistoryJsonConstants.BUSINESS_KEY, processInstance.getBusinessKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processInstance.getProcessDefinitionId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_KEY, processInstance.getProcessDefinitionKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_NAME, processInstance.getProcessDefinitionName());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_VERSION, processInstance.getProcessDefinitionVersion() != null ? Integer.toString(processInstance.getProcessDefinitionVersion()) : null);
            putIfNotNull(data, HistoryJsonConstants.DEPLOYMENT_ID, processInstance.getDeploymentId());
            putIfNotNull(data, HistoryJsonConstants.START_TIME, processInstance.getStartTime());
            putIfNotNull(data, HistoryJsonConstants.START_USER_ID, processInstance.getStartUserId());
            putIfNotNull(data, HistoryJsonConstants.START_ACTIVITY_ID, processInstance.getStartActivityId());
            putIfNotNull(data, HistoryJsonConstants.SUPER_PROCESS_INSTANCE_ID, processInstance.getSuperExecution() != null ? processInstance.getSuperExecution().getProcessInstanceId() : null);
            putIfNotNull(data, HistoryJsonConstants.TENANT_ID, processInstance.getTenantId());

            getAsyncHistorySession().addHistoricData(ProcessInstanceStartHistoryJsonTransformer.TYPE, data, processInstance.getTenantId());
        }
    }

    @Override
    public void recordProcessInstanceEnd(ExecutionEntity processInstance, String deleteReason, String activityId) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, processInstance.getId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.NAME, processInstance.getName());
            putIfNotNull(data, HistoryJsonConstants.BUSINESS_KEY, processInstance.getBusinessKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processInstance.getProcessDefinitionId());
            
            ProcessDefinition processDefinition = processEngineConfiguration.getDeploymentManager().findDeployedProcessDefinitionById(processInstance.getProcessDefinitionId());
            
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_KEY, processDefinition.getKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_NAME, processDefinition.getName());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_VERSION, processDefinition.getVersion());
            putIfNotNull(data, HistoryJsonConstants.DEPLOYMENT_ID, processInstance.getDeploymentId());
            putIfNotNull(data, HistoryJsonConstants.START_TIME, processInstance.getStartTime());
            putIfNotNull(data, HistoryJsonConstants.START_USER_ID, processInstance.getStartUserId());
            putIfNotNull(data, HistoryJsonConstants.START_ACTIVITY_ID, processInstance.getStartActivityId());
            putIfNotNull(data, HistoryJsonConstants.SUPER_PROCESS_INSTANCE_ID, processInstance.getSuperExecution() != null ? processInstance.getSuperExecution().getProcessInstanceId() : null);
            putIfNotNull(data, HistoryJsonConstants.TENANT_ID, processInstance.getTenantId());
            putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
            putIfNotNull(data, HistoryJsonConstants.END_TIME, getClock().getCurrentTime());
            putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);

            getAsyncHistorySession().addHistoricData(ProcessInstanceEndHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordProcessInstanceNameChange(String processInstanceId, String newName) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstanceId);
            putIfNotNull(data, HistoryJsonConstants.NAME, newName);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, ProcessInstancePropertyChangedHistoryJsonTransformer.PROPERTY_NAME);
            getAsyncHistorySession().addHistoricData(ProcessInstancePropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordSubProcessInstanceStart(ExecutionEntity parentExecution, ExecutionEntity subProcessInstance) {
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, subProcessInstance.getId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, subProcessInstance.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.BUSINESS_KEY, subProcessInstance.getBusinessKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, subProcessInstance.getProcessDefinitionId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_KEY, subProcessInstance.getProcessDefinitionKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_NAME, subProcessInstance.getProcessDefinitionName());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_VERSION, subProcessInstance.getProcessDefinitionVersion() != null ? 
                    Integer.toString(subProcessInstance.getProcessDefinitionVersion()) : null);
            putIfNotNull(data, HistoryJsonConstants.DEPLOYMENT_ID, subProcessInstance.getDeploymentId());
            putIfNotNull(data, HistoryJsonConstants.START_TIME, subProcessInstance.getStartTime());
            putIfNotNull(data, HistoryJsonConstants.START_USER_ID, subProcessInstance.getStartUserId());
            putIfNotNull(data, HistoryJsonConstants.START_ACTIVITY_ID, subProcessInstance.getStartActivityId());
            putIfNotNull(data, HistoryJsonConstants.SUPER_PROCESS_INSTANCE_ID, subProcessInstance.getSuperExecution() != null ? subProcessInstance.getSuperExecution().getProcessInstanceId() : null);
            putIfNotNull(data, HistoryJsonConstants.TENANT_ID, subProcessInstance.getTenantId());
            putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, parentExecution.getId());
            
            String activityId = getActivityIdForExecution(parentExecution);
            putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);

            getAsyncHistorySession().addHistoricData(SubProcessInstanceHistoryJsonTransformer.TYPE, data, subProcessInstance.getTenantId());
        }
    }
    
    @Override
    public void recordProcessInstanceDeleted(String processInstanceId) {
        if (isHistoryEnabled()) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstanceId);

            getAsyncHistorySession().addHistoricData(ProcessInstanceDeleteHistoryJsonTransformer.TYPE, data);
        }
    }
    
    @Override
    public void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId) {
        if (isHistoryEnabled()) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinitionId);

            getAsyncHistorySession().addHistoricData(ProcessInstanceDeleteHistoryByProcessDefinitionIdJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordActivityStart(ExecutionEntity executionEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            if (executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null && 
                    getActivityStartWithoutEnd(executionEntity.getActivityId(), executionEntity.getId()) == null) {

                Map<String, String> data = new HashMap<String, String>();
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, executionEntity.getProcessDefinitionId());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, executionEntity.getProcessInstanceId());
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, executionEntity.getId());
                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, executionEntity.getActivityId());
                putIfNotNull(data, HistoryJsonConstants.START_TIME, getClock().getCurrentTime());
                
                if (executionEntity.getCurrentFlowElement() != null) {
                    putIfNotNull(data, HistoryJsonConstants.ACTIVITY_NAME, executionEntity.getCurrentFlowElement().getName());
                    putIfNotNull(data, HistoryJsonConstants.ACTIVITY_TYPE, parseActivityType(executionEntity.getCurrentFlowElement()));
                }
                
                if (executionEntity.getTenantId() != null) {
                    putIfNotNull(data, HistoryJsonConstants.TENANT_ID, executionEntity.getTenantId());
                }

                getAsyncHistorySession().addHistoricData(ActivityStartHistoryJsonTransformer.TYPE, data, executionEntity.getTenantId());

            }
        }
    }

    @Override
    public void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            String activityId = getActivityIdForExecution(executionEntity);
            if (StringUtils.isNotEmpty(activityId)) {
                Map<String, String> data = new HashMap<String, String>();
                
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, executionEntity.getProcessDefinitionId());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, executionEntity.getProcessInstanceId());
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, executionEntity.getId());
                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
                putIfNotNull(data, HistoryJsonConstants.START_TIME, getClock().getCurrentTime());

                if (executionEntity.getCurrentFlowElement() != null) {
                    putIfNotNull(data, HistoryJsonConstants.ACTIVITY_NAME, executionEntity.getCurrentFlowElement().getName());
                    putIfNotNull(data, HistoryJsonConstants.ACTIVITY_TYPE, parseActivityType(executionEntity.getCurrentFlowElement()));
                }

                if (executionEntity.getTenantId() != null) {
                    putIfNotNull(data, HistoryJsonConstants.TENANT_ID, executionEntity.getTenantId());
                }
                
                putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
                putIfNotNull(data, HistoryJsonConstants.END_TIME, getClock().getCurrentTime());

                getAsyncHistorySession().addHistoricData(ActivityEndHistoryJsonTransformer.TYPE, data);
            }
        }
    }

    @Override
    public void recordProcessDefinitionChange(String processInstanceId, String processDefinitionId) {
        Map<String, String> data = new HashMap<String, String>();
        putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstanceId);
        putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinitionId);
        
        getAsyncHistorySession().addHistoricData(SetProcessDefinitionHistoryJsonTransformer.TYPE, data);
    }

    @Override
    public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, task.getId());
            putIfNotNull(data, HistoryJsonConstants.NAME, task.getName());
            putIfNotNull(data, HistoryJsonConstants.PARENT_TASK_ID, task.getParentTaskId());
            putIfNotNull(data, HistoryJsonConstants.DESCRIPTION, task.getDescription());
            putIfNotNull(data, HistoryJsonConstants.OWNER, task.getOwner());
            putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, task.getAssignee());
            putIfNotNull(data, HistoryJsonConstants.START_TIME, getClock().getCurrentTime());
            putIfNotNull(data, HistoryJsonConstants.TASK_DEFINITION_KEY, task.getTaskDefinitionKey());
            putIfNotNull(data, HistoryJsonConstants.PRIORITY, task.getPriority());
            if (task.getDueDate() != null) {
                putIfNotNull(data, HistoryJsonConstants.DUE_DATE, task.getDueDate());
            }
            putIfNotNull(data, HistoryJsonConstants.CATEGORY, task.getCategory());
            putIfNotNull(data, HistoryJsonConstants.TENANT_ID, task.getTenantId());

            if (execution != null) {
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, execution.getProcessDefinitionId());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, execution.getId());

                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, getActivityIdForExecution(execution));
            }

            getAsyncHistorySession().addHistoricData(TaskCreatedHistoryJsonTransformer.TYPE, data, task.getTenantId());
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, ExecutionEntity execution, String deleteReason) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, task.getId());
            putIfNotNull(data, HistoryJsonConstants.NAME, task.getName());
            putIfNotNull(data, HistoryJsonConstants.PARENT_TASK_ID, task.getParentTaskId());
            putIfNotNull(data, HistoryJsonConstants.DESCRIPTION, task.getDescription());
            putIfNotNull(data, HistoryJsonConstants.OWNER, task.getOwner());
            putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, task.getAssignee());
            putIfNotNull(data, HistoryJsonConstants.START_TIME, getClock().getCurrentTime());
            putIfNotNull(data, HistoryJsonConstants.TASK_DEFINITION_KEY, task.getTaskDefinitionKey());
            putIfNotNull(data, HistoryJsonConstants.PRIORITY, task.getPriority());
            putIfNotNull(data, HistoryJsonConstants.DUE_DATE, task.getDueDate());
            putIfNotNull(data, HistoryJsonConstants.FORM_KEY, task.getFormKey());
            putIfNotNull(data, HistoryJsonConstants.CATEGORY, task.getCategory());
            putIfNotNull(data, HistoryJsonConstants.CLAIM_TIME, task.getClaimTime());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, task.getProcessDefinitionId());
            putIfNotNull(data, HistoryJsonConstants.TENANT_ID, task.getTenantId());

            if (execution != null) {
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, execution.getProcessDefinitionId());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, execution.getId());

                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, getActivityIdForExecution(execution));
            }
            putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
            putIfNotNull(data, HistoryJsonConstants.END_TIME, getClock().getCurrentTime());

            getAsyncHistorySession().addHistoricData(TaskEndedHistoryJsonTransformer.TYPE, data);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskEntity.getId());
            putIfNotNull(data, HistoryJsonConstants.NAME, taskEntity.getName());
            putIfNotNull(data, HistoryJsonConstants.DESCRIPTION, taskEntity.getDescription());
            putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, taskEntity.getAssignee());
            putIfNotNull(data, HistoryJsonConstants.OWNER, taskEntity.getOwner());
            putIfNotNull(data, HistoryJsonConstants.DUE_DATE, taskEntity.getDueDate());
            putIfNotNull(data, HistoryJsonConstants.PRIORITY, taskEntity.getPriority());
            putIfNotNull(data, HistoryJsonConstants.CATEGORY, taskEntity.getCategory());
            putIfNotNull(data, HistoryJsonConstants.FORM_KEY, taskEntity.getFormKey());
            putIfNotNull(data, HistoryJsonConstants.PARENT_TASK_ID, taskEntity.getParentTaskId());
            putIfNotNull(data, HistoryJsonConstants.TASK_DEFINITION_KEY, taskEntity.getTaskDefinitionKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, taskEntity.getProcessDefinitionId());
            putIfNotNull(data, HistoryJsonConstants.CLAIM_TIME, taskEntity.getClaimTime());
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
            
        Map<String, Object> originalPersistentState = (Map<String, Object>) taskEntity.getOriginalPersistentState();
        
        if ((originalPersistentState == null && taskEntity.getAssignee() != null) || 
                (originalPersistentState != null && !Objects.equals(originalPersistentState.get("assignee"), taskEntity.getAssignee()))) {
            
            handleTaskAssigneeChange(taskEntity);
        }
        
        if ((originalPersistentState == null && taskEntity.getOwner() != null) ||
                (originalPersistentState != null && !Objects.equals(originalPersistentState.get("owner"), taskEntity.getOwner()))) {
            
            handleTaskOwnerChange(taskEntity);
        }
    }
    
    protected void handleTaskAssigneeChange(TaskEntity taskEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, taskEntity.getAssignee());

            ExecutionEntity executionEntity = taskEntity.getExecution();
            if (executionEntity != null) {
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, executionEntity.getId());
                String activityId = getActivityIdForExecution(executionEntity);
                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
                Map<String, String> activityStartData = getActivityStartWithoutEnd(activityId, executionEntity.getId());
                if (activityStartData != null) {
                    putIfNotNull(activityStartData, HistoryJsonConstants.ASSIGNEE, taskEntity.getAssignee());
                    data.put(HistoryJsonConstants.ACTIVITY_ASSIGNEE_HANDLED, String.valueOf(true));
                }
            }

            if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
                putIfNotNull(data, HistoryJsonConstants.ID, taskEntity.getId());
                putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, getClock().getCurrentTime());
                getAsyncHistorySession().addHistoricData(TaskAssigneeChangedHistoryJsonTransformer.TYPE, data);
            }
        }
    }
    
    protected void handleTaskOwnerChange(TaskEntity taskEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskEntity.getId());
            putIfNotNull(data, HistoryJsonConstants.OWNER, taskEntity.getOwner());
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, getClock().getCurrentTime());

            getAsyncHistorySession().addHistoricData(TaskOwnerChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, variable.getId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, variable.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, variable.getExecutionId());
            putIfNotNull(data, HistoryJsonConstants.TASK_ID, variable.getTaskId());
            putIfNotNull(data, HistoryJsonConstants.REVISION, variable.getRevision());
            putIfNotNull(data, HistoryJsonConstants.NAME, variable.getName());
            
            Date time = getClock().getCurrentTime();
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, time);
            
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TYPE, variable.getType().getTypeName());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE, variable.getTextValue());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE2, variable.getTextValue2());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE, variable.getDoubleValue());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_LONG_VALUE, variable.getLongValue());
            if (variable.getByteArrayRef() != null) {
                putIfNotNull(data, HistoryJsonConstants.VARIABLE_BYTES_VALUE, Base64.encodeBase64String(variable.getBytes()));
            }
            
            getAsyncHistorySession().addHistoricData(VariableCreatedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordHistoricDetailVariableCreate(VariableInstanceEntity variable,
                    ExecutionEntity sourceActivityExecution, boolean useActivityId) {
        
        if (isHistoryLevelAtLeast(HistoryLevel.FULL)) {

            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, variable.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, variable.getExecutionId());
            putIfNotNull(data, HistoryJsonConstants.TASK_ID, variable.getTaskId());
            putIfNotNull(data, HistoryJsonConstants.REVISION, variable.getRevision());
            putIfNotNull(data, HistoryJsonConstants.NAME, variable.getName());
            
            if (sourceActivityExecution != null && sourceActivityExecution.isMultiInstanceRoot()) {
                putIfNotNull(data, HistoryJsonConstants.IS_MULTI_INSTANCE_ROOT_EXECUTION, true);
            }
           
            Date time = getClock().getCurrentTime();
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, time);
            
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TYPE, variable.getType().getTypeName());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE, variable.getTextValue());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE2, variable.getTextValue2());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE, variable.getDoubleValue());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_LONG_VALUE, variable.getLongValue());
            if (variable.getBytes() != null) {
                putIfNotNull(data, HistoryJsonConstants.VARIABLE_BYTES_VALUE, Base64.encodeBase64String(variable.getBytes()));
            }
            
            if (useActivityId && sourceActivityExecution != null) {
                String activityId = getActivityIdForExecution(sourceActivityExecution);
                if (activityId != null) {
                    putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
                    putIfNotNull(data, HistoryJsonConstants.SOURCE_EXECUTION_ID, sourceActivityExecution.getId());
                }
            }
            
            getAsyncHistorySession().addHistoricData(HistoricDetailVariableUpdateHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, variable.getId());
            
            Date time = getClock().getCurrentTime();
            putIfNotNull(data, HistoryJsonConstants.LAST_UPDATED_TIME, time);
            
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TYPE, variable.getType().getTypeName());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE, variable.getTextValue());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE2, variable.getTextValue2());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE, variable.getDoubleValue());
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_LONG_VALUE, variable.getLongValue());
            if (variable.getByteArrayRef() != null) {
                putIfNotNull(data, HistoryJsonConstants.VARIABLE_BYTES_VALUE, Base64.encodeBase64String(variable.getBytes()));
            }
            
            getAsyncHistorySession().addHistoricData(VariableUpdatedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, variable.getId());
            
            getAsyncHistorySession().addHistoricData(VariableRemovedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordFormPropertiesSubmitted(ExecutionEntity execution, Map<String, String> properties, String taskId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, execution.getId());
            putIfNotNull(data, HistoryJsonConstants.TASK_ID, taskId);
            
            String activityId = getActivityIdForExecution(execution);
            putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
            
            Date currentTime = getClock().getCurrentTime();
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, currentTime);
            
            int counter = 1;
            for (String propertyId : properties.keySet()) {
                String propertyValue = properties.get(propertyId);
                data.put(HistoryJsonConstants.FORM_PROPERTY_ID + counter, propertyId);
                data.put(HistoryJsonConstants.FORM_PROPERTY_VALUE + counter, propertyValue);
                counter++;
            }
            
            getAsyncHistorySession().addHistoricData(FormPropertiesSubmittedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        // It makes no sense storing historic counterpart for an identity-link that is related
        // to a process-definition only as this is never kept in history
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT) && (identityLink.getProcessInstanceId() != null || identityLink.getTaskId() != null)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, identityLink.getId());
            putIfNotNull(data, HistoryJsonConstants.GROUP_ID, identityLink.getGroupId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, identityLink.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.TASK_ID, identityLink.getTaskId());
            putIfNotNull(data, HistoryJsonConstants.IDENTITY_LINK_TYPE, identityLink.getType());
            putIfNotNull(data, HistoryJsonConstants.USER_ID, identityLink.getUserId());
            getAsyncHistorySession().addHistoricData(IdentityLinkCreatedHistoryJsonTransformer.TYPE, data);
        }
    }
    
    @Override
    public void recordIdentityLinkDeleted(String identityLinkId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, identityLinkId);
            getAsyncHistorySession().addHistoricData(IdentityLinkDeletedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getId());
            putIfNotNull(data, HistoryJsonConstants.BUSINESS_KEY, processInstance.getBusinessKey());
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, ProcessInstancePropertyChangedHistoryJsonTransformer.PROPERTY_BUSINESS_KEY);
            getAsyncHistorySession().addHistoricData(ProcessInstancePropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    /* Helper methods */
    
    protected Map<String, String> getActivityStartWithoutEnd(String activityId, String executionId) {
        List<Pair<String, Map<String, String>>> jobData = getAsyncHistorySession().getJobData();
        Map<String, String> activityStartData = null;
        if (jobData != null) {
            List<Integer> matchedActvityEndIndexes = new ArrayList<>();
            for (int i = 0; i < jobData.size(); i++) {
                Pair<String, Map<String, String>> historicData = jobData.get(i);
                if (ActivityStartHistoryJsonTransformer.TYPE.equals(historicData.getKey()) && 
                                activityId.equals(historicData.getValue().get(HistoryJsonConstants.ACTIVITY_ID)) && 
                                executionId.equals(historicData.getValue().get(HistoryJsonConstants.EXECUTION_ID))) {
                    
                    activityStartData = historicData.getValue();
                    
                    String activityKey = historicData.getValue().get(HistoryJsonConstants.EXECUTION_ID) + "_" + 
                                    historicData.getValue().get(HistoryJsonConstants.ACTIVITY_ID);
                    
                    for (int j = i; j < jobData.size(); j++) {
                        Pair<String, Map<String, String>> historicEndData = jobData.get(j);
                        if (ActivityEndHistoryJsonTransformer.TYPE.equals(historicEndData.getKey()) && !matchedActvityEndIndexes.contains(j)) {
                            
                            String activityEndKey = historicEndData.getValue().get(HistoryJsonConstants.EXECUTION_ID) + "_" + 
                                            historicEndData.getValue().get(HistoryJsonConstants.ACTIVITY_ID);
                            
                            if (activityEndKey.equals(activityKey)) {
                                matchedActvityEndIndexes.add(j);
                                activityStartData = null;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return activityStartData;
    }
    
    protected void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    protected void putIfNotNull(Map<String, String> map, String key, int value) {
        map.put(key, Integer.toString(value));
    }
    
    protected void putIfNotNull(Map<String, String> map, String key, Double value) {
        if (value != null) {
            map.put(key, Double.toString(value));
        }
    }
    
    protected void putIfNotNull(Map<String, String> map, String key, Long value) {
        if (value != null) {
            map.put(key, Long.toString(value));
        }
    }

    protected void putIfNotNull(Map<String, String> map, String key, Date value) {
        if (value != null) {
            map.put(key, AsyncHistoryDateUtil.formatDate(value));
        }
    }
    
    protected void putIfNotNull(Map<String, String> map, String key, Boolean value) {
        if (value != null) {
            map.put(key, Boolean.toString(value));
        }
    }

}
