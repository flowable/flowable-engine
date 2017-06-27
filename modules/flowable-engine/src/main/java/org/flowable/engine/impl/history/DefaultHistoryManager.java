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

package org.flowable.engine.impl.history;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.engine.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.engine.task.IdentityLinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class that centralises recording of all history-related operations that are originated from inside the engine.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class DefaultHistoryManager extends AbstractHistoryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHistoryManager.class.getName());

    public DefaultHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration, HistoryLevel historyLevel) {
        super(processEngineConfiguration, historyLevel);
    }

    // Process related history

    @Override
    public void recordProcessInstanceEnd(ExecutionEntity processInstance, String deleteReason, String activityId) {

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());

            if (historicProcessInstance != null) {
                historicProcessInstance.markEnded(deleteReason);
                historicProcessInstance.setEndActivityId(activityId);

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(
                            FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED, historicProcessInstance));
                }

            }
        }
    }

    @Override
    public void recordProcessInstanceNameChange(String processInstanceId, String newName) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);

            if (historicProcessInstance != null) {
                historicProcessInstance.setName(newName);
            }
        }
    }

    @Override
    public void recordProcessInstanceStart(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().create(processInstance);

            // Insert historic process-instance
            getHistoricProcessInstanceEntityManager().insert(historicProcessInstance, false);

            // Fire event
            FlowableEventDispatcher eventDispatcher = getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(
                        FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED, historicProcessInstance));
            }

        }
    }

    @Override
    public void recordSubProcessInstanceStart(ExecutionEntity parentExecution, ExecutionEntity subProcessInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {

            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().create(subProcessInstance);
            getHistoricProcessInstanceEntityManager().insert(historicProcessInstance, false);

            // Fire event
            FlowableEventDispatcher eventDispatcher = getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(
                                FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED, historicProcessInstance));
            }

            HistoricActivityInstanceEntity activityInstance = findActivityInstance(parentExecution, false, true);
            if (activityInstance != null) {
                activityInstance.setCalledProcessInstanceId(subProcessInstance.getProcessInstanceId());
            }

        }
    }
    
    @Override
    public void recordProcessInstanceDeleted(String processInstanceId) {
        if (getHistoryManager().isHistoryEnabled()) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);

            getHistoricDetailEntityManager().deleteHistoricDetailsByProcessInstanceId(processInstanceId);
            getHistoricVariableInstanceEntityManager().deleteHistoricVariableInstanceByProcessInstanceId(processInstanceId);
            getHistoricActivityInstanceEntityManager().deleteHistoricActivityInstancesByProcessInstanceId(processInstanceId);
            getHistoricTaskInstanceEntityManager().deleteHistoricTaskInstancesByProcessInstanceId(processInstanceId);
            getHistoricIdentityLinkEntityManager().deleteHistoricIdentityLinksByProcInstance(processInstanceId);
            getCommentEntityManager().deleteCommentsByProcessInstanceId(processInstanceId);

            if (historicProcessInstance != null) {
                getHistoricProcessInstanceEntityManager().delete(historicProcessInstance, false);
            }

            // Also delete any sub-processes that may be active (ACT-821)

            List<HistoricProcessInstance> selectList = getHistoricProcessInstanceEntityManager().findHistoricProcessInstancesBySuperProcessInstanceId(processInstanceId);
            for (HistoricProcessInstance child : selectList) {
                recordProcessInstanceDeleted(child.getId());
            }
        }
    }
    
    @Override
    public void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId) {
        if (getHistoryManager().isHistoryEnabled()) {
            List<String> historicProcessInstanceIds = getHistoricProcessInstanceEntityManager().findHistoricProcessInstanceIdsByProcessDefinitionId(processDefinitionId);
            for (String historicProcessInstanceId : historicProcessInstanceIds) {
                recordProcessInstanceDeleted(historicProcessInstanceId);
            }
        }
    }

    // Activity related history

    @Override
    public void recordActivityStart(ExecutionEntity executionEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            if (executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null) {

                HistoricActivityInstanceEntity historicActivityInstanceEntity = null;

                // Historic activity instance could have been created (but only in cache, never persisted)
                // for example when submitting form properties
                HistoricActivityInstanceEntity historicActivityInstanceEntityFromCache = getHistoricActivityInstanceFromCache(executionEntity.getId(), executionEntity.getActivityId(), true);
                if (historicActivityInstanceEntityFromCache != null) {
                    historicActivityInstanceEntity = historicActivityInstanceEntityFromCache;
                } else {
                    historicActivityInstanceEntity = createHistoricActivityInstanceEntity(executionEntity);
                }

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(
                                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_CREATED, historicActivityInstanceEntity));
                }

            }
        }
    }

    @Override
    public void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(executionEntity, false, true);
            if (historicActivityInstance != null) {
                historicActivityInstance.markEnded(deleteReason);

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(
                                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_ENDED, historicActivityInstance));
                }
            }
        }
    }

    @Override
    public void recordProcessDefinitionChange(String processInstanceId, String processDefinitionId) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);
            if (historicProcessInstance != null) {
                historicProcessInstance.setProcessDefinitionId(processDefinitionId);
            }
        }
    }

    // Task related history

    @Override
    public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().create(task, execution);
            historicTaskInstance.setLastUpdateTime(processEngineConfiguration.getClock().getCurrentTime());

            if (execution != null) {
                historicTaskInstance.setExecutionId(execution.getId());
            }

            getHistoricTaskInstanceEntityManager().insert(historicTaskInstance, false);
        }

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            if (execution != null) {
                HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(execution, false, true);
                if (historicActivityInstance != null) {
                    historicActivityInstance.setTaskId(task.getId());
                }
            }
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, ExecutionEntity execution, String deleteReason) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(task.getId());
            if (historicTaskInstance != null) {
                historicTaskInstance.markEnded(deleteReason);
                historicTaskInstance.setLastUpdateTime(processEngineConfiguration.getClock().getCurrentTime());
            }
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        boolean assigneeChanged = false;
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskEntity.getId());
            if (historicTaskInstance != null) {
                historicTaskInstance.setName(taskEntity.getName());
                historicTaskInstance.setDescription(taskEntity.getDescription());
                historicTaskInstance.setDueDate(taskEntity.getDueDate());
                historicTaskInstance.setPriority(taskEntity.getPriority());
                historicTaskInstance.setCategory(taskEntity.getCategory());
                historicTaskInstance.setFormKey(taskEntity.getFormKey());
                historicTaskInstance.setParentTaskId(taskEntity.getParentTaskId());
                historicTaskInstance.setTaskDefinitionKey(taskEntity.getTaskDefinitionKey());
                historicTaskInstance.setProcessDefinitionId(taskEntity.getProcessDefinitionId());
                historicTaskInstance.setClaimTime(taskEntity.getClaimTime());
                
                if (!Objects.equals(historicTaskInstance.getAssignee(), taskEntity.getAssignee())) {
                    historicTaskInstance.setAssignee(taskEntity.getAssignee());
                    assigneeChanged = true;
                    
                    createHistoricIdentityLink(historicTaskInstance.getId(), IdentityLinkType.ASSIGNEE, historicTaskInstance.getAssignee());
                }
                
                if (!Objects.equals(historicTaskInstance.getOwner(), taskEntity.getOwner())) {
                    historicTaskInstance.setOwner(taskEntity.getOwner());
                    
                    createHistoricIdentityLink(historicTaskInstance.getId(), IdentityLinkType.OWNER, historicTaskInstance.getOwner());
                }
                
                historicTaskInstance.setLastUpdateTime(processEngineConfiguration.getClock().getCurrentTime());
            }
        }
        
        if (assigneeChanged && isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            ExecutionEntity executionEntity = taskEntity.getExecution();
            if (executionEntity != null) {
                HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(executionEntity, false, true);
                if (historicActivityInstance != null) {
                    historicActivityInstance.setAssignee(taskEntity.getAssignee());
                }
            }
        }
    }
    
    protected void createHistoricIdentityLink(String taskId, String type, String userId) {
        HistoricIdentityLinkEntity historicIdentityLinkEntity = getHistoricIdentityLinkEntityManager().create();
        historicIdentityLinkEntity.setTaskId(taskId);
        historicIdentityLinkEntity.setType(type);
        historicIdentityLinkEntity.setUserId(userId);
        Date time = getClock().getCurrentTime();
        historicIdentityLinkEntity.setCreateTime(time);
        getHistoricIdentityLinkEntityManager().insert(historicIdentityLinkEntity, false);
    }

    // Variables related history

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        // Historic variables
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            getHistoricVariableInstanceEntityManager().copyAndInsert(variable);
        }
    }

    @Override
    public void recordHistoricDetailVariableCreate(VariableInstanceEntity variable, ExecutionEntity sourceActivityExecution, boolean useActivityId) {
        if (isHistoryLevelAtLeast(HistoryLevel.FULL)) {

            HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = getHistoricDetailEntityManager().copyAndInsertHistoricDetailVariableInstanceUpdateEntity(variable);

            if (useActivityId && sourceActivityExecution != null) {
                HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(sourceActivityExecution, false, false);
                if (historicActivityInstance != null) {
                    historicVariableUpdate.setActivityInstanceId(historicActivityInstance.getId());
                }
            }
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricVariableInstanceEntity historicProcessVariable = getEntityCache().findInCache(HistoricVariableInstanceEntity.class, variable.getId());
            if (historicProcessVariable == null) {
                historicProcessVariable = getHistoricVariableInstanceEntityManager().findHistoricVariableInstanceByVariableInstanceId(variable.getId());
            }

            if (historicProcessVariable != null) {
                getHistoricVariableInstanceEntityManager().copyVariableValue(historicProcessVariable, variable);
            } else {
                getHistoricVariableInstanceEntityManager().copyAndInsert(variable);
            }
        }
    }

    @Override
    public void recordFormPropertiesSubmitted(ExecutionEntity processInstance, Map<String, String> properties, String taskId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            for (String propertyId : properties.keySet()) {
                String propertyValue = properties.get(propertyId);
                getHistoricDetailEntityManager().insertHistoricFormPropertyEntity(processInstance, propertyId, propertyValue, taskId);
            }
        }
    }

    // Identity link related history
    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        // It makes no sense storing historic counterpart for an identity-link that is related
        // to a process-definition only as this is never kept in history
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT) && (identityLink.getProcessInstanceId() != null || identityLink.getTaskId() != null)) {
            HistoricIdentityLinkEntity historicIdentityLinkEntity = getHistoricIdentityLinkEntityManager().create();
            historicIdentityLinkEntity.setId(identityLink.getId());
            historicIdentityLinkEntity.setGroupId(identityLink.getGroupId());
            historicIdentityLinkEntity.setProcessInstanceId(identityLink.getProcessInstanceId());
            historicIdentityLinkEntity.setTaskId(identityLink.getTaskId());
            historicIdentityLinkEntity.setType(identityLink.getType());
            historicIdentityLinkEntity.setUserId(identityLink.getUserId());
            getHistoricIdentityLinkEntityManager().insert(historicIdentityLinkEntity, false);
        }
    }
    
    @Override
    public void recordIdentityLinkDeleted(String identityLinkId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            getHistoricIdentityLinkEntityManager().delete(identityLinkId);
        }
    }

    @Override
    public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
        if (isHistoryEnabled()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("updateProcessBusinessKeyInHistory : {}", processInstance.getId());
            }
            if (processInstance != null) {
                HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());
                if (historicProcessInstance != null) {
                    historicProcessInstance.setBusinessKey(processInstance.getProcessInstanceBusinessKey());
                    getHistoricProcessInstanceEntityManager().update(historicProcessInstance, false);
                }
            }
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricVariableInstanceEntity historicProcessVariable = getEntityCache()
                    .findInCache(HistoricVariableInstanceEntity.class, variable.getId());
            
            if (historicProcessVariable == null) {
                historicProcessVariable = getHistoricVariableInstanceEntityManager()
                        .findHistoricVariableInstanceByVariableInstanceId(variable.getId());
            }

            if (historicProcessVariable != null) {
                getHistoricVariableInstanceEntityManager().delete(historicProcessVariable);
            }
        }
    }

}
