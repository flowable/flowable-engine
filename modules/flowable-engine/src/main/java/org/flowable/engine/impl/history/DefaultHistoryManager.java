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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.HistoricActivityInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
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

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
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
    public void recordProcessInstanceNameChange(ExecutionEntity processInstanceExecution, String newName) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstanceExecution.getProcessDefinitionId())) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceExecution.getId());

            if (historicProcessInstance != null) {
                historicProcessInstance.setName(newName);
            }
        }
    }

    @Override
    public void recordProcessInstanceStart(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
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
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, subProcessInstance.getProcessDefinitionId())) {

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
    public void recordProcessInstanceDeleted(String processInstanceId, String processDefinitionId) {
        if (getHistoryManager().isHistoryEnabled(processDefinitionId)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);

            getHistoricDetailEntityManager().deleteHistoricDetailsByProcessInstanceId(processInstanceId);
            CommandContextUtil.getHistoricVariableService().deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId);
            getHistoricActivityInstanceEntityManager().deleteHistoricActivityInstancesByProcessInstanceId(processInstanceId);
            TaskHelper.deleteHistoricTaskInstancesByProcessInstanceId(processInstanceId);
            CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLinksByProcessInstanceId(processInstanceId);
            getCommentEntityManager().deleteCommentsByProcessInstanceId(processInstanceId);

            if (historicProcessInstance != null) {
                getHistoricProcessInstanceEntityManager().delete(historicProcessInstance, false);
            }

            // Also delete any sub-processes that may be active (ACT-821)

            List<HistoricProcessInstance> selectList = getHistoricProcessInstanceEntityManager().findHistoricProcessInstancesBySuperProcessInstanceId(processInstanceId);
            for (HistoricProcessInstance child : selectList) {
                recordProcessInstanceDeleted(child.getId(), processDefinitionId);
            }
        }
    }
    
    @Override
    public void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId) {
        if (getHistoryManager().isHistoryEnabled(processDefinitionId)) {
            List<String> historicProcessInstanceIds = getHistoricProcessInstanceEntityManager().findHistoricProcessInstanceIdsByProcessDefinitionId(processDefinitionId);
            for (String historicProcessInstanceId : historicProcessInstanceIds) {
                recordProcessInstanceDeleted(historicProcessInstanceId, processDefinitionId);
            }
        }
    }

    // Activity related history

    @Override
    public void recordActivityStart(ExecutionEntity executionEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, executionEntity.getProcessDefinitionId())) {
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
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, executionEntity.getProcessDefinitionId())) {
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
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);
            if (historicProcessInstance != null) {
                historicProcessInstance.setProcessDefinitionId(processDefinitionId);
            }
        }
    }

    // Task related history

    @Override
    public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
        String processDefinitionId = null;
        if (execution != null) {
            processDefinitionId = execution.getProcessDefinitionId();
        } else if (task != null) {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            if (execution != null) {
                task.setExecutionId(execution.getId());
                task.setProcessInstanceId(execution.getProcessInstanceId());
                task.setProcessDefinitionId(execution.getProcessDefinitionId());
                
                if (execution.getTenantId() != null) {
                    task.setTenantId(execution.getTenantId());
                }
            }
            HistoricTaskInstanceEntity historicTaskInstance = CommandContextUtil.getHistoricTaskService().recordTaskCreated(task);
            historicTaskInstance.setLastUpdateTime(processEngineConfiguration.getClock().getCurrentTime());

            if (execution != null) {
                historicTaskInstance.setExecutionId(execution.getId());
            }
        }

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
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
        String processDefinitionId = null;
        if (execution != null) {
            processDefinitionId = execution.getProcessDefinitionId();
        } else if (task != null) {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            HistoricTaskInstanceEntity historicTaskInstance = CommandContextUtil.getHistoricTaskService().recordTaskEnd(task, deleteReason);
            if (historicTaskInstance != null) {
                historicTaskInstance.setLastUpdateTime(processEngineConfiguration.getClock().getCurrentTime());
            }
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        
        boolean assigneeChanged = false;
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
            HistoricTaskInstanceEntity originalHistoricTaskInstanceEntity = historicTaskService.getHistoricTask(taskEntity.getId());
            String originalAssignee = null;
            if (originalHistoricTaskInstanceEntity != null) {
                originalAssignee = originalHistoricTaskInstanceEntity.getAssignee();
            }

            HistoricTaskInstanceEntity historicTaskInstance = historicTaskService.recordTaskInfoChange(taskEntity);
            if (historicTaskInstance != null) {
                if (!Objects.equals(originalAssignee, taskEntity.getAssignee())) {
                    assigneeChanged = true;
                }
            }
        }
        
        if (assigneeChanged && isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, taskEntity.getProcessDefinitionId())) {
            if (taskEntity.getExecutionId() != null) {
                ExecutionEntity executionEntity = getExecutionEntityManager().findById(taskEntity.getExecutionId());
                HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(executionEntity, false, true);
                if (historicActivityInstance != null) {
                    historicActivityInstance.setAssignee(taskEntity.getAssignee());
                }
            }
        }
    }
    
    // Variables related history

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            CommandContextUtil.getHistoricVariableService().createAndInsert(variable);
        }
    }

    @Override
    public void recordHistoricDetailVariableCreate(VariableInstanceEntity variable, ExecutionEntity sourceActivityExecution, boolean useActivityId) {
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
    public void recordVariableUpdate(VariableInstanceEntity variableInstanceEntity) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variableInstanceEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variableInstanceEntity.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            CommandContextUtil.getHistoricVariableService().recordVariableUpdate(variableInstanceEntity);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variableInstanceEntity) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variableInstanceEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variableInstanceEntity.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            CommandContextUtil.getHistoricVariableService().recordVariableRemoved(variableInstanceEntity);
        }
    }

    @Override
    public void recordFormPropertiesSubmitted(ExecutionEntity processInstance, Map<String, String> properties, String taskId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processInstance.getProcessDefinitionId())) {
            for (String propertyId : properties.keySet()) {
                String propertyValue = properties.get(propertyId);
                getHistoricDetailEntityManager().insertHistoricFormPropertyEntity(processInstance, propertyId, propertyValue, taskId);
            }
        }
    }

    // Identity link related history
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
        
        // It makes no sense storing historic counterpart for an identity link that is related
        // to a process definition only as this is never kept in history
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId) && (identityLink.getProcessInstanceId() != null || identityLink.getTaskId() != null)) {
            HistoricIdentityLinkService historicIdentityLinkService = CommandContextUtil.getHistoricIdentityLinkService();
            HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkService.createHistoricIdentityLink();
            historicIdentityLinkEntity.setId(identityLink.getId());
            historicIdentityLinkEntity.setGroupId(identityLink.getGroupId());
            historicIdentityLinkEntity.setProcessInstanceId(identityLink.getProcessInstanceId());
            historicIdentityLinkEntity.setTaskId(identityLink.getTaskId());
            historicIdentityLinkEntity.setType(identityLink.getType());
            historicIdentityLinkEntity.setUserId(identityLink.getUserId());
            historicIdentityLinkService.insertHistoricIdentityLink(historicIdentityLinkEntity, false);
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
            CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLink(identityLink.getId());
        }
    }

    @Override
    public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
        if (processInstance != null) {
            if (isHistoryEnabled(processInstance.getProcessDefinitionId())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("updateProcessBusinessKeyInHistory : {}", processInstance.getId());
                }
                
                HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());
                if (historicProcessInstance != null) {
                    historicProcessInstance.setBusinessKey(processInstance.getProcessInstanceBusinessKey());
                    getHistoricProcessInstanceEntityManager().update(historicProcessInstance, false);
                }
            }
        }
    }
    
    @Override
    public void updateProcessDefinitionIdInHistory(ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance) {
        if (isHistoryEnabled(processDefinitionEntity.getId())) {
            HistoricProcessInstanceEntity historicProcessInstance = (HistoricProcessInstanceEntity) getHistoricProcessInstanceEntityManager().findById(processInstance.getId());
            historicProcessInstance.setProcessDefinitionId(processDefinitionEntity.getId());
            getHistoricProcessInstanceEntityManager().update(historicProcessInstance);
    
            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
            HistoricTaskInstanceQueryImpl taskQuery = new HistoricTaskInstanceQueryImpl();
            taskQuery.processInstanceId(processInstance.getId());
            List<HistoricTaskInstance> historicTasks = historicTaskService.findHistoricTaskInstancesByQueryCriteria(taskQuery);
            if (historicTasks != null) {
                for (HistoricTaskInstance historicTaskInstance : historicTasks) {
                    HistoricTaskInstanceEntity taskEntity = (HistoricTaskInstanceEntity) historicTaskInstance;
                    taskEntity.setProcessDefinitionId(processDefinitionEntity.getId());
                    historicTaskService.updateHistoricTask(taskEntity, true);
                }
            }
            
            HistoricActivityInstanceQueryImpl activityQuery = new HistoricActivityInstanceQueryImpl();
            activityQuery.processInstanceId(processInstance.getId());
            List<HistoricActivityInstance> historicActivities = getHistoricActivityInstanceEntityManager().findHistoricActivityInstancesByQueryCriteria(activityQuery);
            if (historicActivities != null) {
                for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                    HistoricActivityInstanceEntity activityEntity = (HistoricActivityInstanceEntity) historicActivityInstance;
                    activityEntity.setProcessDefinitionId(processDefinitionEntity.getId());
                    getHistoricActivityInstanceEntityManager().update(activityEntity);
                }
            }
        }
    }

}
