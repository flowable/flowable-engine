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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.convertToBase64;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.AbstractHistoryManager;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstancePropertyChangedHistoryJsonTransformer;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.job.service.impl.history.async.AsyncHistorySession.AsyncHistorySessionData;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class AsyncHistoryManager extends AbstractHistoryManager {

    public AsyncHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration, HistoryLevel historyLevel, boolean usePrefixId) {
        super(processEngineConfiguration, historyLevel, usePrefixId);
    }

    public AsyncHistorySession getAsyncHistorySession() {
        return getSession(AsyncHistorySession.class);
    }

    @Override
    public void recordProcessInstanceStart(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            addCommonProcessInstanceFields(processInstance, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_START, data, processInstance.getTenantId());
        }
    }

    @Override
    public void recordProcessInstanceEnd(ExecutionEntity processInstance, String deleteReason, String activityId) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            addCommonProcessInstanceFields(processInstance, data);
            
            putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
            putIfNotNull(data, HistoryJsonConstants.END_TIME, getClock().getCurrentTime());
            putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_END, data);
        }
    }
    
    protected void addCommonProcessInstanceFields(ExecutionEntity processInstance, Map<String, String> data) {
        putIfNotNull(data, HistoryJsonConstants.ID, processInstance.getId());
        putIfNotNull(data, HistoryJsonConstants.REVISION, processInstance.getRevision());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getProcessInstanceId());
        putIfNotNull(data, HistoryJsonConstants.NAME, processInstance.getName());
        putIfNotNull(data, HistoryJsonConstants.BUSINESS_KEY, processInstance.getBusinessKey());
        putIfNotNull(data, HistoryJsonConstants.DEPLOYMENT_ID, processInstance.getDeploymentId());
        putIfNotNull(data, HistoryJsonConstants.START_TIME, processInstance.getStartTime());
        putIfNotNull(data, HistoryJsonConstants.START_USER_ID, processInstance.getStartUserId());
        putIfNotNull(data, HistoryJsonConstants.START_ACTIVITY_ID, processInstance.getStartActivityId());
        putIfNotNull(data, HistoryJsonConstants.SUPER_PROCESS_INSTANCE_ID, processInstance.getSuperExecution() != null ? processInstance.getSuperExecution().getProcessInstanceId() : null);
        putIfNotNull(data, HistoryJsonConstants.CALLBACK_ID, processInstance.getCallbackId());
        putIfNotNull(data, HistoryJsonConstants.CALLBACK_TYPE, processInstance.getCallbackType());
        putIfNotNull(data, HistoryJsonConstants.TENANT_ID, processInstance.getTenantId());
        
        addProcessDefinitionFields(data, processInstance.getProcessDefinitionId());
    }

    protected void addProcessDefinitionFields(Map<String, String> data, String processDefinitionId) {
        if (processDefinitionId != null) {
            ProcessDefinition processDefinition = processEngineConfiguration.getDeploymentManager().findDeployedProcessDefinitionById(processDefinitionId);
            if (processDefinition != null) {
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinition.getId());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_KEY, processDefinition.getKey());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_NAME, processDefinition.getName());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_VERSION, processDefinition.getVersion());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_CATEGORY, processDefinition.getCategory());
                putIfNotNull(data, HistoryJsonConstants.DEPLOYMENT_ID, processDefinition.getDeploymentId());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITIN_DERIVED_FROM, processDefinition.getDerivedFrom());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITIN_DERIVED_FROM_ROOT, processDefinition.getDerivedFromRoot());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITIN_DERIVED_VERSION, processDefinition.getDerivedVersion());
            }
        }
    }

    @Override
    public void recordProcessInstanceNameChange(ExecutionEntity processInstanceExecution, String newName) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstanceExecution.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstanceExecution.getId());
            putIfNotNull(data, HistoryJsonConstants.NAME, newName);
            putIfNotNull(data, HistoryJsonConstants.REVISION, processInstanceExecution.getRevision());
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, ProcessInstancePropertyChangedHistoryJsonTransformer.PROPERTY_NAME);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_PROPERTY_CHANGED, data);
        }
    }

    @Override
    public void recordSubProcessInstanceStart(ExecutionEntity parentExecution, ExecutionEntity subProcessInstance) {
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, subProcessInstance.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            addCommonProcessInstanceFields(subProcessInstance, data);
            
            String activityId = getActivityIdForExecution(parentExecution);
            putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
            putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, parentExecution.getId());

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_SUBPROCESS_INSTANCE_START, data, subProcessInstance.getTenantId());
        }
    }
    
    @Override
    public void recordProcessInstanceDeleted(String processInstanceId, String processDefinitionId) {
        if (isHistoryEnabled(processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstanceId);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_DELETED, data);
        }
    }
    
    @Override
    public void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId) {
        if (isHistoryEnabled(processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinitionId);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_DELETED_BY_PROCDEF_ID, data);
        }
    }

    @Override
    public void recordActivityStart(ExecutionEntity executionEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, executionEntity.getProcessDefinitionId())) {
            if (executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null) {

                Map<String, String> data = new HashMap<>();
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

                getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_START, data, executionEntity.getTenantId());

            }
        }
    }

    @Override
    public void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, executionEntity.getProcessDefinitionId())) {
            String activityId = getActivityIdForExecution(executionEntity);
            if (StringUtils.isNotEmpty(activityId)) {
                Map<String, String> data = new HashMap<>();
                
                putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, executionEntity.getProcessDefinitionId());
                putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, executionEntity.getProcessInstanceId());
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, executionEntity.getId());
                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);

                if (executionEntity.getCurrentFlowElement() != null) {
                    putIfNotNull(data, HistoryJsonConstants.ACTIVITY_NAME, executionEntity.getCurrentFlowElement().getName());
                    putIfNotNull(data, HistoryJsonConstants.ACTIVITY_TYPE, parseActivityType(executionEntity.getCurrentFlowElement()));
                }

                if (executionEntity.getTenantId() != null) {
                    putIfNotNull(data, HistoryJsonConstants.TENANT_ID, executionEntity.getTenantId());
                }
                
                putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
                putIfNotNull(data, HistoryJsonConstants.END_TIME, getClock().getCurrentTime());

                Map<String, String> correspondingActivityStartData = getActivityStart(executionEntity.getId(), activityId, true);
                if (correspondingActivityStartData == null) {
                    getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_END, data);
                } else {
                    data.put(HistoryJsonConstants.START_TIME, correspondingActivityStartData.get(HistoryJsonConstants.START_TIME));
                    getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_FULL, data);
                }
            }
        }
    }

    @Override
    public void recordProcessDefinitionChange(String processInstanceId, String processDefinitionId) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstanceId);
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinitionId);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_SET_PROCESS_DEFINITION, data);
        }
    }

    @Override
    public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
        String processDefinitionId = null;
        if (execution != null) {
            processDefinitionId = execution.getProcessDefinitionId();
        } else if (task != null) {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            addCommonTaskFields(task, execution, data);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_CREATED, data, task.getTenantId());
        }
    }
    
    protected void addCommonTaskFields(TaskEntity task, ExecutionEntity execution, Map<String, String> data) {
        putIfNotNull(data, HistoryJsonConstants.ID, task.getId());
        putIfNotNull(data, HistoryJsonConstants.REVISION, task.getRevision());
        putIfNotNull(data, HistoryJsonConstants.NAME, task.getName());
        putIfNotNull(data, HistoryJsonConstants.PARENT_TASK_ID, task.getParentTaskId());
        putIfNotNull(data, HistoryJsonConstants.DESCRIPTION, task.getDescription());
        putIfNotNull(data, HistoryJsonConstants.OWNER, task.getOwner());
        putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, task.getAssignee());
        putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, task.getCreateTime());
        putIfNotNull(data, HistoryJsonConstants.TASK_DEFINITION_KEY, task.getTaskDefinitionKey());
        putIfNotNull(data, HistoryJsonConstants.TASK_DEFINITION_ID, task.getTaskDefinitionId());
        putIfNotNull(data, HistoryJsonConstants.FORM_KEY, task.getFormKey());
        putIfNotNull(data, HistoryJsonConstants.PRIORITY, task.getPriority());
        putIfNotNull(data, HistoryJsonConstants.DUE_DATE, task.getDueDate());
        putIfNotNull(data, HistoryJsonConstants.CATEGORY, task.getCategory());
        putIfNotNull(data, HistoryJsonConstants.CLAIM_TIME, task.getClaimTime());
        putIfNotNull(data, HistoryJsonConstants.TENANT_ID, task.getTenantId());

        if (execution != null) {
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, execution.getId());
            putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, getActivityIdForExecution(execution));
            
            addProcessDefinitionFields(data, execution.getProcessDefinitionId());
            
        } else if (task.getProcessDefinitionId() != null) {
            addProcessDefinitionFields(data, task.getProcessDefinitionId());
            
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, ExecutionEntity execution, String deleteReason) {
        String processDefinitionId = null;
        if (execution != null) {
            processDefinitionId = execution.getProcessDefinitionId();
        } else if (task != null) {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            addCommonTaskFields(task, execution, data);
            
            putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
            putIfNotNull(data, HistoryJsonConstants.END_TIME, getClock().getCurrentTime());

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_ENDED, data);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            addCommonTaskFields(taskEntity, null, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_PROPERTY_CHANGED, data);
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
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, taskEntity.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, taskEntity.getAssignee());

            if (taskEntity.getExecutionId() != null) {
                ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, executionEntity.getId());
                String activityId = getActivityIdForExecution(executionEntity);
                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
                
                
                if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
                    Map<String, String> activityStartData = getActivityStart(executionEntity.getId(), activityId, false); 
                    if (activityStartData != null) {
                        putIfNotNull(activityStartData, HistoryJsonConstants.ASSIGNEE, taskEntity.getAssignee());
                        data.put(HistoryJsonConstants.ACTIVITY_ASSIGNEE_HANDLED, String.valueOf(true));
                    }
                    
                } else {
                    data.put(HistoryJsonConstants.ACTIVITY_ASSIGNEE_HANDLED, String.valueOf(true));
                }
            }

            if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
                putIfNotNull(data, HistoryJsonConstants.ID, taskEntity.getId());
                putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, getClock().getCurrentTime());
                getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_ASSIGNEE_CHANGED, data);
            }
        }
    }
    
    protected void handleTaskOwnerChange(TaskEntity taskEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.ID, taskEntity.getId());
            putIfNotNull(data, HistoryJsonConstants.OWNER, taskEntity.getOwner());
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, getClock().getCurrentTime());

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_OWNER_CHANGED, data);
        }
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            addCommonVariableFields(variable, data);
            
            Date time = getClock().getCurrentTime();
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, time);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_VARIABLE_CREATED, data);
        }
    }

    protected void addCommonVariableFields(VariableInstanceEntity variable, Map<String, String> data) {
        putIfNotNull(data, HistoryJsonConstants.ID, variable.getId());
        putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, variable.getProcessInstanceId());
        putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, variable.getExecutionId());
        putIfNotNull(data, HistoryJsonConstants.TASK_ID, variable.getTaskId());
        putIfNotNull(data, HistoryJsonConstants.REVISION, variable.getRevision());
        putIfNotNull(data, HistoryJsonConstants.NAME, variable.getName());
        
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_TYPE, variable.getType().getTypeName());
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE, variable.getTextValue());
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_TEXT_VALUE2, variable.getTextValue2());
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE, variable.getDoubleValue());
        putIfNotNull(data, HistoryJsonConstants.VARIABLE_LONG_VALUE, variable.getLongValue());
        if (variable.getByteArrayRef() != null) {
            putIfNotNull(data, HistoryJsonConstants.VARIABLE_BYTES_VALUE, convertToBase64(variable));
        }
        
        if (variable.getExecutionId() != null) {
            addProcessDefinitionFields(data, variable.getProcessDefinitionId());
        }
    }

    @Override
    public void recordHistoricDetailVariableCreate(VariableInstanceEntity variable,
                    ExecutionEntity sourceActivityExecution, boolean useActivityId) {
        
        String processDefinitionId = null;
        if (sourceActivityExecution != null) {
            processDefinitionId = sourceActivityExecution.getProcessDefinitionId();
        } else if (variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            if (processInstanceExecution != null) {
                processDefinitionId = processInstanceExecution.getProcessDefinitionId();
            }
        } else if (variable.getTaskId() != null) {
            TaskEntity taskEntity = CommandContextUtil.getTaskService().getTask(variable.getTaskId());
            if (taskEntity != null) {
                processDefinitionId = taskEntity.getProcessDefinitionId();
            }
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.FULL, processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            addCommonVariableFields(variable, data);
            
            if (sourceActivityExecution != null && sourceActivityExecution.isMultiInstanceRoot()) {
                putIfNotNull(data, HistoryJsonConstants.IS_MULTI_INSTANCE_ROOT_EXECUTION, true);
            }
           
            Date time = getClock().getCurrentTime();
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, time);
            
            if (useActivityId && sourceActivityExecution != null) {
                String activityId = getActivityIdForExecution(sourceActivityExecution);
                if (activityId != null) {
                    putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
                    putIfNotNull(data, HistoryJsonConstants.SOURCE_EXECUTION_ID, sourceActivityExecution.getId());
                }
            }
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_HISTORIC_DETAIL_VARIABLE_UPDATE, data);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            addCommonVariableFields(variable, data);
            
            Date time = getClock().getCurrentTime();
            putIfNotNull(data, HistoryJsonConstants.LAST_UPDATED_TIME, time);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_VARIABLE_UPDATED, data);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.ID, variable.getId());
            putIfNotNull(data, HistoryJsonConstants.REVISION, variable.getRevision());
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_VARIABLE_REMOVED, data);
        }
    }

    @Override
    public void recordFormPropertiesSubmitted(ExecutionEntity execution, Map<String, String> properties, String taskId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, execution.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            if (execution != null) {
                addProcessDefinitionFields(data, execution.getProcessDefinitionId());
            }
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
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_FORM_PROPERTIES_SUBMITTED, data);
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        String processDefinitionId = null;
        if (identityLink.getProcessInstanceId() != null) {
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(identityLink.getProcessInstanceId());
            if (execution != null) {
                processDefinitionId = execution.getProcessDefinitionId();
            }
        } else if (identityLink.getTaskId() != null) {
            TaskEntity task = CommandContextUtil.getTaskService().getTask(identityLink.getTaskId());
            if (task != null) {
                processDefinitionId = task.getProcessDefinitionId();
            }
        }
        
        // It makes no sense storing historic counterpart for an identity-link that is related
        // to a process-definition only as this is never kept in history
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId) && (identityLink.getProcessInstanceId() != null || identityLink.getTaskId() != null)) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.ID, identityLink.getId());
            putIfNotNull(data, HistoryJsonConstants.GROUP_ID, identityLink.getGroupId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, identityLink.getProcessDefinitionId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, identityLink.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.TASK_ID, identityLink.getTaskId());
            putIfNotNull(data, HistoryJsonConstants.IDENTITY_LINK_TYPE, identityLink.getType());
            putIfNotNull(data, HistoryJsonConstants.USER_ID, identityLink.getUserId());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_IDENTITY_LINK_CREATED, data);
        }
    }
    
    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        String processDefinitionId = null;
        if (identityLink.getProcessInstanceId() != null) {
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(identityLink.getProcessInstanceId());
            if (execution != null) {
                processDefinitionId = execution.getProcessDefinitionId();
            }
        } else if (identityLink.getTaskId() != null) {
            TaskEntity task = CommandContextUtil.getTaskService().getTask(identityLink.getTaskId());
            if (task != null) {
                processDefinitionId = task.getProcessDefinitionId();
            }
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.ID, identityLink.getId());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_IDENTITY_LINK_DELETED, data);
        }
    }

    @Override
    public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getId());
            putIfNotNull(data, HistoryJsonConstants.BUSINESS_KEY, processInstance.getBusinessKey());
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, ProcessInstancePropertyChangedHistoryJsonTransformer.PROPERTY_BUSINESS_KEY);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_PROPERTY_CHANGED, data);
        }
    }
    
    @Override
    public void updateProcessDefinitionIdInHistory(ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance) {
        if (isHistoryEnabled(processDefinitionEntity.getId())) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinitionEntity.getId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getId());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_UPDATE_PROCESS_DEFINITION_CASCADE, data);
        }
    }
    

    /* Helper methods */

    protected Map<String, String> getActivityStart(String executionId, String activityId, boolean removeFromAsyncHistorySession) {
        Map<JobServiceConfiguration, AsyncHistorySessionData> sessionData = getAsyncHistorySession().getSessionData();
        if (sessionData != null) {
            AsyncHistorySessionData asyncHistorySessionData = sessionData.get(getJobServiceConfiguration());
            if (asyncHistorySessionData != null) {
                Map<String, List<Map<String, String>>> jobData = asyncHistorySessionData.getJobData();
                if (jobData != null && jobData.containsKey(HistoryJsonConstants.TYPE_ACTIVITY_START)) {
                    List<Map<String, String>> activityStartDataList = jobData.get(HistoryJsonConstants.TYPE_ACTIVITY_START);
                    Iterator<Map<String, String>> activityStartDataIterator = activityStartDataList.iterator();
                    while (activityStartDataIterator.hasNext()) {
                        Map<String, String> activityStartData = activityStartDataIterator.next();
                        if (activityId.equals(activityStartData.get(HistoryJsonConstants.ACTIVITY_ID))
                                && executionId.equals(activityStartData.get(HistoryJsonConstants.EXECUTION_ID))) {
                            if (removeFromAsyncHistorySession) {
                                activityStartDataIterator.remove();
                            }
                            return activityStartData;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    protected JobServiceConfiguration getJobServiceConfiguration() {
        return getProcessEngineConfiguration().getJobServiceConfiguration();
    }
    
}
