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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
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
import org.flowable.engine.impl.history.async.json.transformer.SubProcessInstanceHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskAssigneeChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskEndedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskPropertyChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.VariableCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.VariableUpdatedHistoryJsonTransformer;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.persistence.entity.VariableInstanceEntity;

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
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_KEY, processInstance.getProcessDefinitionKey());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_NAME, processInstance.getProcessDefinitionName());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_VERSION, processInstance.getProcessDefinitionVersion() != null ? Integer.toString(processInstance.getProcessDefinitionVersion()) : null);
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
            String activityId = getActivityIdForExecution(executionEntity);
            if (activityId != null && executionEntity.getCurrentFlowElement() != null && 
                            !getAsyncHistorySession().isActivityStartPresentWithoutEnd(activityId, executionEntity.getId())) {

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
        // TODO Auto-generated method stub

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
            putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
            putIfNotNull(data, HistoryJsonConstants.END_TIME, getClock().getCurrentTime());

            getAsyncHistorySession().addHistoricData(TaskEndedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskClaim(TaskEntity task) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, task.getId());
            putIfNotNull(data, HistoryJsonConstants.CLAIM_TIME, getClock().getCurrentTime());
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_CLAIM_TIME);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskAssigneeChange(TaskEntity taskEntity, String assignee) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, assignee);

            ExecutionEntity executionEntity = taskEntity.getExecution();
            if (executionEntity != null) {
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, executionEntity.getId());
                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, getActivityIdForExecution(executionEntity));
            }

            if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
                putIfNotNull(data, HistoryJsonConstants.ID, taskEntity.getId());
                putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_ASSIGNEE);
                putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, getClock().getCurrentTime());
            }

            getAsyncHistorySession().addHistoricData(TaskAssigneeChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskOwnerChange(String taskId, String owner) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.OWNER, owner);
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, getClock().getCurrentTime());
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_OWNER);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskNameChange(String taskId, String taskName) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.NAME, taskName);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_NAME);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskDescriptionChange(String taskId, String description) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.DESCRIPTION, description);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_DESCRIPTION);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskDueDateChange(String taskId, Date dueDate) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.DUE_DATE, dueDate);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_DUE_DATE);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskPriorityChange(String taskId, int priority) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.PRIORITY, priority);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_PRIORITY);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskCategoryChange(String taskId, String category) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.CATEGORY, category);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_CATEGORY);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskFormKeyChange(String taskId, String formKey) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.FORM_KEY, formKey);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_FORM_KEY);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskParentTaskIdChange(String taskId, String parentTaskId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.PARENT_TASK_ID, parentTaskId);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_PARENT_TASK_ID);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskDefinitionKeyChange(String taskId, String taskDefinitionKey) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.TASK_DEFINITION_KEY, taskDefinitionKey);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_TASK_DEFINITION_KEY);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
        }
    }

    @Override
    public void recordTaskProcessDefinitionChange(String taskId, String processDefinitionId) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<String, String>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskId);
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinitionId);
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, TaskPropertyChangedHistoryJsonTransformer.PROPERTY_PROCESS_DEFINITION_ID);
            getAsyncHistorySession().addHistoricData(TaskPropertyChangedHistoryJsonTransformer.TYPE, data);
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
            
            String activityId = getActivityIdForExecution(sourceActivityExecution);
            if (useActivityId && sourceActivityExecution != null && activityId != null) {
                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
                putIfNotNull(data, HistoryJsonConstants.SOURCE_EXECUTION_ID, sourceActivityExecution.getId());
   
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
        // TODO Auto-generated method stub

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

}
