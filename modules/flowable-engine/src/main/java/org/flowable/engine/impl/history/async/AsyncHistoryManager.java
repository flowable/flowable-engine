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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstancePropertyChangedHistoryJsonTransformer;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.job.service.impl.history.async.AsyncHistorySession.AsyncHistorySessionData;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class AsyncHistoryManager extends AbstractAsyncHistoryManager {

    public AsyncHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    public AsyncHistorySession getAsyncHistorySession() {
        return getSession(AsyncHistorySession.class);
    }

    @Override
    public void recordProcessInstanceStart(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonProcessInstanceFields(processInstance, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_START, data, processInstance.getTenantId());
        }
    }

    @Override
    public void recordProcessInstanceEnd(ExecutionEntity processInstance, String deleteReason, String activityId, Date endTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonProcessInstanceFields(processInstance, data);
            
            putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
            putIfNotNull(data, HistoryJsonConstants.END_TIME, endTime);
            putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_END, data);
        }
    }

    @Override
    public void recordProcessInstanceNameChange(ExecutionEntity processInstanceExecution, String newName) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstanceExecution.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstanceExecution.getId());
            putIfNotNull(data, HistoryJsonConstants.NAME, newName);
            putIfNotNull(data, HistoryJsonConstants.REVISION, processInstanceExecution.getRevision());
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, ProcessInstancePropertyChangedHistoryJsonTransformer.PROPERTY_NAME);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_PROPERTY_CHANGED, data);
        }
    }

    @Override
    public void recordProcessInstanceDeleted(String processInstanceId, String processDefinitionId, String processTenantId) {
        if (isHistoryEnabled(processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstanceId);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_DELETED, data);
        }
    }
    
    @Override
    public void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId) {
        if (isHistoryEnabled(processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinitionId);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_DELETED_BY_PROCDEF_ID, data);
        }
    }

    @Override
    public void recordActivityStart(ActivityInstance activityInstance) {
        if (activityInstance != null && isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, activityInstance.getProcessDefinitionId())) {
            if (activityInstance.getActivityId() != null) {

                ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
                addCommonActivityInstanceFields(activityInstance, data);

                putIfNotNull(data, HistoryJsonConstants.START_TIME, activityInstance.getStartTime());

                getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_START, data, activityInstance.getTenantId());
            }
        }
    }

    @Override
    public void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason, Date endTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, executionEntity.getProcessDefinitionId())) {
            String activityId = getActivityIdForExecution(executionEntity);
            if (StringUtils.isNotEmpty(activityId)) {
                ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();

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
                putIfNotNull(data, HistoryJsonConstants.END_TIME, endTime);

                ObjectNode correspondingActivityStartData = getActivityStart(executionEntity.getId(), activityId, true);
                if (correspondingActivityStartData == null) {
                    getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_END, data);
                } else {
                    data.put(HistoryJsonConstants.START_TIME, getStringFromJson(correspondingActivityStartData, HistoryJsonConstants.START_TIME));
                    getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_FULL, data);
                }
            }
        }
    }

    @Override
    public void recordActivityEnd(ActivityInstance activityInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, activityInstance.getProcessDefinitionId())) {
            if (StringUtils.isNotEmpty(activityInstance.getActivityId())) {
                ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();

                addCommonActivityInstanceFields(activityInstance, data);

                putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, activityInstance.getDeleteReason());
                putIfNotNull(data, HistoryJsonConstants.END_TIME, activityInstance.getEndTime());
                putIfNotNull(data, HistoryJsonConstants.START_TIME, activityInstance.getStartTime());

                ObjectNode correspondingActivityStartData = getActivityStart(activityInstance.getId(), true);
                if (correspondingActivityStartData == null) {
                    getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_END, data);
                } else {
                    getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_FULL, data);
                }
            }
        }
    }

    @Override
    public void recordProcessDefinitionChange(String processInstanceId, String processDefinitionId) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
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
        } else {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonTaskFields(task, execution, data);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_CREATED, data, task.getTenantId());
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, ExecutionEntity execution, String deleteReason, Date endTime) {
        String processDefinitionId = null;
        if (execution != null) {
            processDefinitionId = execution.getProcessDefinitionId();
        } else if (task != null) {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonTaskFields(task, execution, data);
            
            putIfNotNull(data, HistoryJsonConstants.DELETE_REASON, deleteReason);
            putIfNotNull(data, HistoryJsonConstants.END_TIME, endTime);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_ENDED, data);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity, String runtimeActivityInstanceId, Date changeTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonTaskFields(taskEntity, null, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_PROPERTY_CHANGED, data);
        }
            
        Map<String, Object> originalPersistentState = (Map<String, Object>) taskEntity.getOriginalPersistentState();
        
        if ((originalPersistentState == null && taskEntity.getAssignee() != null) || 
                (originalPersistentState != null && !Objects.equals(originalPersistentState.get("assignee"), taskEntity.getAssignee()))) {
            
            handleTaskAssigneeChange(taskEntity, runtimeActivityInstanceId, changeTime);
        }
        
        if ((originalPersistentState == null && taskEntity.getOwner() != null) ||
                (originalPersistentState != null && !Objects.equals(originalPersistentState.get("owner"), taskEntity.getOwner()))) {
            
            handleTaskOwnerChange(taskEntity, runtimeActivityInstanceId, changeTime);
        }
    }
    
    protected void handleTaskAssigneeChange(TaskEntity taskEntity, String activityInstanceId, Date changeTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, taskEntity.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, taskEntity.getAssignee());

            if (taskEntity.getExecutionId() != null) {
                ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
                putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, executionEntity.getId());
                String activityId = getActivityIdForExecution(executionEntity);
                putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
                putIfNotNull(data, HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID, activityInstanceId);

                if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
                    ObjectNode activityStartData = getActivityStart(executionEntity.getId(), activityId, false);
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
                putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, changeTime);
                getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_ASSIGNEE_CHANGED, data);
            }
        }
    }
    
    protected void handleTaskOwnerChange(TaskEntity taskEntity, String activityInstanceId, Date changeTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.ID, taskEntity.getId());
            putIfNotNull(data, HistoryJsonConstants.OWNER, taskEntity.getOwner());
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, changeTime);
            putIfNotNull(data, HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID, activityInstanceId);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_TASK_OWNER_CHANGED, data);
        }
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable, Date createTime) {
        String processDefinitionId = null;
        if (isEnableProcessDefinitionHistoryLevel() && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonVariableFields(variable, data);
            
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, createTime);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_VARIABLE_CREATED, data);
        }
    }

    @Override
    public void recordHistoricDetailVariableCreate(VariableInstanceEntity variable, ExecutionEntity sourceActivityExecution, boolean useActivityId,
        String activityInstanceId, Date createTime) {

        String processDefinitionId = getProcessDefinitionId(variable, sourceActivityExecution);
        
        if (isHistoryLevelAtLeast(HistoryLevel.FULL, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonVariableFields(variable, data);
            
            if (sourceActivityExecution != null && sourceActivityExecution.isMultiInstanceRoot()) {
                putIfNotNull(data, HistoryJsonConstants.IS_MULTI_INSTANCE_ROOT_EXECUTION, true);
            }
           
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, createTime);

            putIfNotNull(data, HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID, activityInstanceId);
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
    public void recordVariableUpdate(VariableInstanceEntity variable, Date updateTime) {
        String processDefinitionId = null;
        if (isEnableProcessDefinitionHistoryLevel() && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonVariableFields(variable, data);
            
            putIfNotNull(data, HistoryJsonConstants.LAST_UPDATED_TIME, updateTime);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_VARIABLE_UPDATED, data);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        String processDefinitionId = null;
        if (isEnableProcessDefinitionHistoryLevel() && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.ID, variable.getId());
            putIfNotNull(data, HistoryJsonConstants.REVISION, variable.getRevision());
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_VARIABLE_REMOVED, data);
        }
    }

    @Override
    public void recordFormPropertiesSubmitted(ExecutionEntity execution, Map<String, String> properties, String taskId, Date createTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, execution.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addProcessDefinitionFields(data, execution.getProcessDefinitionId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
            putIfNotNull(data, HistoryJsonConstants.EXECUTION_ID, execution.getId());
            putIfNotNull(data, HistoryJsonConstants.TASK_ID, taskId);
            
            String activityId = getActivityIdForExecution(execution);
            putIfNotNull(data, HistoryJsonConstants.ACTIVITY_ID, activityId);
            
            putIfNotNull(data, HistoryJsonConstants.CREATE_TIME, createTime);
            
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
        String processDefinitionId = getProcessDefinitionId(identityLink);
        
        // It makes no sense storing historic counterpart for an identity-link that is related
        // to a process-definition only as this is never kept in history
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId) && (identityLink.getProcessInstanceId() != null || identityLink.getTaskId() != null)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonIdentityLinkFields(identityLink, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_IDENTITY_LINK_CREATED, data);
        }
    }
    
    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        String processDefinitionId = getProcessDefinitionId(identityLink);
        
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.ID, identityLink.getId());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_IDENTITY_LINK_DELETED, data);
        }
    }
    
    @Override
    public void recordEntityLinkCreated(EntityLinkEntity entityLink) {
        String processDefinitionId = getProcessDefinitionId(entityLink);
        
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonEntityLinkFields(entityLink, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ENTITY_LINK_CREATED, data);
        }
    }
    
    @Override
    public void recordEntityLinkDeleted(EntityLinkEntity entityLink) {
        String processDefinitionId = getProcessDefinitionId(entityLink);
        
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.ID, entityLink.getId());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ENTITY_LINK_DELETED, data);
        }
    }

    @Override
    public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getId());
            putIfNotNull(data, HistoryJsonConstants.BUSINESS_KEY, processInstance.getBusinessKey());
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, ProcessInstancePropertyChangedHistoryJsonTransformer.PROPERTY_BUSINESS_KEY);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_PROPERTY_CHANGED, data);
        }
    }
    
    @Override
    public void updateProcessBusinessStatusInHistory(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getId());
            putIfNotNull(data, HistoryJsonConstants.BUSINESS_STATUS, processInstance.getBusinessStatus());
            putIfNotNull(data, HistoryJsonConstants.PROPERTY, ProcessInstancePropertyChangedHistoryJsonTransformer.PROPERTY_BUSINESS_STATUS);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_PROCESS_INSTANCE_PROPERTY_CHANGED, data);
        }
    }
    
    @Override
    public void updateProcessDefinitionIdInHistory(ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance) {
        if (isHistoryEnabled(processDefinitionEntity.getId())) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.PROCESS_DEFINITION_ID, processDefinitionEntity.getId());
            putIfNotNull(data, HistoryJsonConstants.PROCESS_INSTANCE_ID, processInstance.getId());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_UPDATE_PROCESS_DEFINITION_CASCADE, data);
        }
    }

    @Override
    public void updateHistoricActivityInstance(ActivityInstance activityInstance) {
        // the update (in the new job) synchronizes changes with runtime activityInstance
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, activityInstance.getProcessDefinitionId())) {
            if (activityInstance.getExecutionId() != null) {
                ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
                putIfNotNull(data, HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID, activityInstance.getId());
                putIfNotNull(data, HistoryJsonConstants.TASK_ID, activityInstance.getTaskId());
                putIfNotNull(data, HistoryJsonConstants.ASSIGNEE, activityInstance.getAssignee());
                putIfNotNull(data, HistoryJsonConstants.CALLED_PROCESS_INSTANCE_ID, activityInstance.getCalledProcessInstanceId());
                getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_UPDATE_HISTORIC_ACTIVITY_INSTANCE, data);
            }
        }
    }

    @Override
    public void createHistoricActivityInstance(ActivityInstance activityInstance) {
        // create (in the new job) new historic activity instance from runtime activityInstance template
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, activityInstance.getProcessDefinitionId())) {
            if (activityInstance.getExecutionId() != null) {
                ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
                addCommonActivityInstanceFields(activityInstance, data);
                putIfNotNull(data, HistoryJsonConstants.START_TIME, activityInstance.getStartTime());
                putIfNotNull(data, HistoryJsonConstants.END_TIME, activityInstance.getEndTime());

                getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_ACTIVITY_FULL, data);
            }
        }
    }
    
    @Override
    public void recordHistoricUserTaskLogEntry(HistoricTaskLogEntryBuilder taskLogEntryBuilder) {
        if (processEngineConfiguration.isEnableHistoricTaskLogging()) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            addHistoricTaskLogEntryFields(taskLogEntryBuilder, data);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_HISTORIC_TASK_LOG_RECORD, data,
                taskLogEntryBuilder.getTenantId());
        }
    }

    @Override
    public void deleteHistoryUserTaskLog(long logNumber) {
        if (processEngineConfiguration.isEnableHistoricTaskLogging()) {
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, HistoryJsonConstants.LOG_ENTRY_LOGNUMBER, logNumber);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), HistoryJsonConstants.TYPE_HISTORIC_TASK_LOG_DELETE, data);
        }
    }

    /* Helper methods */

    protected ObjectNode getActivityStart(String executionId, String activityId, boolean removeFromAsyncHistorySession) {
        Map<JobServiceConfiguration, AsyncHistorySessionData> sessionData = getAsyncHistorySession().getSessionData();
        if (sessionData != null) {
            AsyncHistorySessionData asyncHistorySessionData = sessionData.get(getJobServiceConfiguration());
            if (asyncHistorySessionData != null) {
                Map<String, List<ObjectNode>> jobData = asyncHistorySessionData.getJobData();
                if (jobData != null && jobData.containsKey(HistoryJsonConstants.TYPE_ACTIVITY_START)) {
                    List<ObjectNode> activityStartDataList = jobData.get(HistoryJsonConstants.TYPE_ACTIVITY_START);
                    Iterator<ObjectNode> activityStartDataIterator = activityStartDataList.iterator();
                    while (activityStartDataIterator.hasNext()) {
                        ObjectNode activityStartData = activityStartDataIterator.next();
                        if (activityId.equals(getStringFromJson(activityStartData, HistoryJsonConstants.ACTIVITY_ID))
                                && executionId.equals(getStringFromJson(activityStartData, HistoryJsonConstants.EXECUTION_ID))) {
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

    protected ObjectNode getActivityStart(String runtimeActivityInstanceId, boolean removeFromAsyncHistorySession) {
        Map<JobServiceConfiguration, AsyncHistorySessionData> sessionData = getAsyncHistorySession().getSessionData();
        if (sessionData != null) {
            AsyncHistorySessionData asyncHistorySessionData = sessionData.get(getJobServiceConfiguration());
            if (asyncHistorySessionData != null) {
                Map<String, List<ObjectNode>> jobData = asyncHistorySessionData.getJobData();
                if (jobData != null && jobData.containsKey(HistoryJsonConstants.TYPE_ACTIVITY_START)) {
                    List<ObjectNode> activityStartDataList = jobData.get(HistoryJsonConstants.TYPE_ACTIVITY_START);
                    Iterator<ObjectNode> activityStartDataIterator = activityStartDataList.iterator();
                    while (activityStartDataIterator.hasNext()) {
                        ObjectNode activityStartData = activityStartDataIterator.next();
                        if (runtimeActivityInstanceId.equals(getStringFromJson(activityStartData,
                                HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID))) {
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
