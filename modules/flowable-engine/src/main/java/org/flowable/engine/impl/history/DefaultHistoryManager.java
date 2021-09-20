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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
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
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntity;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
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

    public DefaultHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    // Process related history

    @Override
    public void recordProcessInstanceEnd(ExecutionEntity processInstance, String deleteReason, String activityId, Date endTime) {

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());

            if (historicProcessInstance != null) {
                historicProcessInstance.markEnded(deleteReason, endTime);
                historicProcessInstance.setEndActivityId(activityId);

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(
                            FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED, historicProcessInstance),
                            processEngineConfiguration.getEngineCfgKey());
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
                        FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED, historicProcessInstance),
                        processEngineConfiguration.getEngineCfgKey());
            }

        }
    }

    @Override
    public void recordProcessInstanceDeleted(String processInstanceId, String processDefinitionId, String processTenantId) {
        if (getHistoryManager().isHistoryEnabled(processDefinitionId)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);

            getHistoricDetailEntityManager().deleteHistoricDetailsByProcessInstanceId(processInstanceId);
            processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId);
            getHistoricActivityInstanceEntityManager().deleteHistoricActivityInstancesByProcessInstanceId(processInstanceId);
            TaskHelper.deleteHistoricTaskInstancesByProcessInstanceId(processInstanceId);
            processEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().deleteHistoricIdentityLinksByProcessInstanceId(processInstanceId);
            
            if (processEngineConfiguration.isEnableEntityLinks()) {
                processEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService().deleteHistoricEntityLinksByScopeIdAndScopeType(processInstanceId, ScopeTypes.BPMN);
            }
            
            getCommentEntityManager().deleteCommentsByProcessInstanceId(processInstanceId);

            if (historicProcessInstance != null) {
                getHistoricProcessInstanceEntityManager().delete(historicProcessInstance, false);
            }

            // Also delete any sub-processes that may be active (ACT-821)

            List<HistoricProcessInstance> selectList = getHistoricProcessInstanceEntityManager().findHistoricProcessInstancesBySuperProcessInstanceId(processInstanceId);
            for (HistoricProcessInstance child : selectList) {
                recordProcessInstanceDeleted(child.getId(), processDefinitionId, child.getTenantId());
            }
        }
    }
    
    @Override
    public void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId) {
        if (getHistoryManager().isHistoryEnabled(processDefinitionId)) {
            List<String> historicProcessInstanceIds = getHistoricProcessInstanceEntityManager().findHistoricProcessInstanceIdsByProcessDefinitionId(processDefinitionId);
            for (String historicProcessInstanceId : historicProcessInstanceIds) {
                // The tenantId is not important for the DefaultHistoryManager
                recordProcessInstanceDeleted(historicProcessInstanceId, processDefinitionId, null);
            }
        }
    }

    // Activity related history

    @Override
    public void recordActivityStart(ActivityInstance activityInstance) {
        if (activityInstance != null && isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, activityInstance.getProcessDefinitionId())) {
            if (activityInstance.getActivityId() != null) {
                // Historic activity instance could have been created (but only in cache, never persisted)
                // for example when submitting form properties
                HistoricActivityInstanceEntity historicActivityInstanceEntity = createNewHistoricActivityInstance(activityInstance);
                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_CREATED, historicActivityInstanceEntity),
                            processEngineConfiguration.getEngineCfgKey());
                }
            }
        }
    }

    @Override
    public void recordActivityEnd(ActivityInstance activityInstance) {
        if (activityInstance != null && isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, activityInstance.getProcessDefinitionId())) {
            HistoricActivityInstanceEntity historicActivityInstance = getHistoricActivityInstanceEntityManager().findById(activityInstance.getId());
            if (historicActivityInstance != null) {
                historicActivityInstance.setDeleteReason(activityInstance.getDeleteReason());
                historicActivityInstance.setEndTime(activityInstance.getEndTime());
                historicActivityInstance.setDurationInMillis(activityInstance.getDurationInMillis());

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_ENDED, historicActivityInstance),
                            processEngineConfiguration.getEngineCfgKey());
                }
            } else {
                LOGGER.debug("Historic activity instance was not found.");
            }
        }
    }

    @Override
    public void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason, Date endTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, executionEntity.getProcessDefinitionId())) {
            HistoricActivityInstanceEntity historicActivityInstance = findHistoricActivityInstance(executionEntity, true);
            if (historicActivityInstance != null) {
                historicActivityInstance.markEnded(deleteReason, endTime);

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_ENDED, historicActivityInstance),
                            processEngineConfiguration.getEngineCfgKey());
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
            HistoricTaskInstanceEntity historicTaskInstance = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().recordTaskCreated(task);
            historicTaskInstance.setLastUpdateTime(processEngineConfiguration.getClock().getCurrentTime());

            if (execution != null) {
                historicTaskInstance.setExecutionId(execution.getId());
            }
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
            HistoricTaskInstanceEntity historicTaskInstance = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().recordTaskEnd(task, deleteReason, endTime);
            if (historicTaskInstance != null) {
                historicTaskInstance.setLastUpdateTime(endTime);
            }
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity, String activityInstanceId, Date changeTime) {
        boolean assigneeChanged = false;
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
            HistoricTaskService historicTaskService = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
            HistoricTaskInstanceEntity originalHistoricTaskInstanceEntity = historicTaskService.getHistoricTask(taskEntity.getId());
            String originalAssignee = null;
            if (originalHistoricTaskInstanceEntity != null) {
                originalAssignee = originalHistoricTaskInstanceEntity.getAssignee();
            }

            HistoricTaskInstanceEntity historicTaskInstance = historicTaskService.recordTaskInfoChange(taskEntity, changeTime, processEngineConfiguration);
            if (historicTaskInstance != null) {
                if (!Objects.equals(originalAssignee, taskEntity.getAssignee())) {
                    assigneeChanged = true;
                }
            }
        }

        if (assigneeChanged && isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, taskEntity.getProcessDefinitionId())) {
            if (taskEntity.getExecutionId() != null) {
                HistoricActivityInstanceEntity historicActivityInstance;
                if (activityInstanceId != null) {
                    historicActivityInstance = getHistoricActivityInstanceEntityManager().findById(activityInstanceId);
                } else {
                    // backup for the case when runtime activityInstance was not created
                    ExecutionEntity executionEntity = getExecutionEntityManager().findById(taskEntity.getExecutionId());
                    historicActivityInstance = findHistoricActivityInstance(executionEntity, true);
                }
                if (historicActivityInstance != null) {
                    historicActivityInstance.setAssignee(taskEntity.getAssignee());
                }
            }
        }
    }

    // Variables related history

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable, Date createTime) {
        String processDefinitionId = null;
        if (isEnableProcessDefinitionHistoryLevel() && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = processEngineConfiguration.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().createAndInsert(variable, createTime);
        }
    }

    @Override
    public void recordHistoricDetailVariableCreate(VariableInstanceEntity variable, ExecutionEntity sourceActivityExecution, boolean useActivityId,
        String activityInstanceId, Date createTime) {
        String processDefinitionId = getProcessDefinitionId(variable, sourceActivityExecution);

        if (isHistoryLevelAtLeast(HistoryLevel.FULL, processDefinitionId)) {

            HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = getHistoricDetailEntityManager().copyAndInsertHistoricDetailVariableInstanceUpdateEntity(variable, createTime);

            if (StringUtils.isNotEmpty(activityInstanceId)) {
                historicVariableUpdate.setActivityInstanceId(activityInstanceId);
            } else {
                if (useActivityId && sourceActivityExecution != null) {
                    HistoricActivityInstanceEntity historicActivityInstance = findHistoricActivityInstance(sourceActivityExecution, false);
                    if (historicActivityInstance != null) {
                        historicVariableUpdate.setActivityInstanceId(historicActivityInstance.getId());
                    }
                }
            }
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variableInstanceEntity, Date updateTime) {
        String processDefinitionId = null;
        if (isEnableProcessDefinitionHistoryLevel() && variableInstanceEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = processEngineConfiguration.getExecutionEntityManager().findById(variableInstanceEntity.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().recordVariableUpdate(variableInstanceEntity, updateTime);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variableInstanceEntity) {
        String processDefinitionId = null;
        if (isEnableProcessDefinitionHistoryLevel() && variableInstanceEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = processEngineConfiguration.getExecutionEntityManager().findById(variableInstanceEntity.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }
        
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().recordVariableRemoved(variableInstanceEntity);
        }
    }

    @Override
    public void recordFormPropertiesSubmitted(ExecutionEntity processInstance, Map<String, String> properties, String taskId, Date createTime) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processInstance.getProcessDefinitionId())) {
            for (String propertyId : properties.keySet()) {
                String propertyValue = properties.get(propertyId);
                getHistoricDetailEntityManager().insertHistoricFormPropertyEntity(processInstance, propertyId, propertyValue, taskId, createTime);
            }
        }
    }

    // Identity link related history
    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        String processDefinitionId = getProcessDefinitionId(identityLink);

        // It makes no sense storing historic counterpart for an identity link that is related
        // to a process definition only as this is never kept in history
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId) && (identityLink.getProcessInstanceId() != null || identityLink.getTaskId() != null)) {
            HistoricIdentityLinkService historicIdentityLinkService = processEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService();
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
        String processDefinitionId = getProcessDefinitionId(identityLink);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            processEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().deleteHistoricIdentityLink(identityLink.getId());
        }
    }
    
    // Entity link related history
    @Override
    public void recordEntityLinkCreated(EntityLinkEntity entityLink) {
        String processDefinitionId = getProcessDefinitionId(entityLink);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            HistoricEntityLinkService historicEntityLinkService = processEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService();
            HistoricEntityLinkEntity historicEntityLinkEntity = (HistoricEntityLinkEntity) historicEntityLinkService.createHistoricEntityLink();
            historicEntityLinkEntity.setId(entityLink.getId());
            historicEntityLinkEntity.setLinkType(entityLink.getLinkType());
            historicEntityLinkEntity.setCreateTime(entityLink.getCreateTime());
            historicEntityLinkEntity.setScopeId(entityLink.getScopeId());
            historicEntityLinkEntity.setSubScopeId(entityLink.getSubScopeId());
            historicEntityLinkEntity.setScopeType(entityLink.getScopeType());
            historicEntityLinkEntity.setScopeDefinitionId(entityLink.getScopeDefinitionId());
            historicEntityLinkEntity.setParentElementId(entityLink.getParentElementId());
            historicEntityLinkEntity.setReferenceScopeId(entityLink.getReferenceScopeId());
            historicEntityLinkEntity.setReferenceScopeType(entityLink.getReferenceScopeType());
            historicEntityLinkEntity.setReferenceScopeDefinitionId(entityLink.getReferenceScopeDefinitionId());
            historicEntityLinkEntity.setRootScopeId(entityLink.getRootScopeId());
            historicEntityLinkEntity.setRootScopeType(entityLink.getRootScopeType());
            historicEntityLinkEntity.setHierarchyType(entityLink.getHierarchyType());
            historicEntityLinkService.insertHistoricEntityLink(historicEntityLinkEntity, false);
        }
    }
    
    @Override
    public void recordEntityLinkDeleted(EntityLinkEntity entityLink) {
        String processDefinitionId = getProcessDefinitionId(entityLink);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            processEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService().deleteHistoricEntityLink(entityLink.getId());
        }
    }

    @Override
    public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
        if (processInstance != null) {
            if (isHistoryEnabled(processInstance.getProcessDefinitionId())) {
                HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());
                if (historicProcessInstance != null) {
                    historicProcessInstance.setBusinessKey(processInstance.getProcessInstanceBusinessKey());
                    getHistoricProcessInstanceEntityManager().update(historicProcessInstance, false);
                }
            }
        }
    }
    
    @Override
    public void updateProcessBusinessStatusInHistory(ExecutionEntity processInstance) {
        if (processInstance != null) {
            if (isHistoryEnabled(processInstance.getProcessDefinitionId())) {
                HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());
                if (historicProcessInstance != null) {
                    historicProcessInstance.setBusinessStatus(processInstance.getProcessInstanceBusinessStatus());
                    getHistoricProcessInstanceEntityManager().update(historicProcessInstance, false);
                }
            }
        }
    }
    
    @Override
    public void updateProcessDefinitionIdInHistory(ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance) {
        if (isHistoryEnabled(processDefinitionEntity.getId())) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());
            historicProcessInstance.setProcessDefinitionId(processDefinitionEntity.getId());
            getHistoricProcessInstanceEntityManager().update(historicProcessInstance);
    
            HistoricTaskService historicTaskService = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
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

            // because of upgrade runtimeActivity instances can be only subset of historicActivity instances
            HistoricActivityInstanceQueryImpl historicActivityQuery = new HistoricActivityInstanceQueryImpl();
            historicActivityQuery.processInstanceId(processInstance.getId());
            List<HistoricActivityInstance> historicActivities = getHistoricActivityInstanceEntityManager().findHistoricActivityInstancesByQueryCriteria(historicActivityQuery);
            if (historicActivities != null) {
                for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                    HistoricActivityInstanceEntity activityEntity = (HistoricActivityInstanceEntity) historicActivityInstance;
                    activityEntity.setProcessDefinitionId(processDefinitionEntity.getId());
                    getHistoricActivityInstanceEntityManager().update(activityEntity);
                }
            }
        }
    }

    @Override
    public void updateHistoricActivityInstance(ActivityInstance activityInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, activityInstance.getProcessDefinitionId())) {
            if (activityInstance.getExecutionId() != null) {
                HistoricActivityInstanceEntity historicActivityInstance = getHistoricActivityInstanceEntityManager().findById(activityInstance.getId());
                if (historicActivityInstance != null) {
                    historicActivityInstance.setTaskId(activityInstance.getTaskId());
                    historicActivityInstance.setAssignee(activityInstance.getAssignee());
                    historicActivityInstance.setCalledProcessInstanceId(activityInstance.getCalledProcessInstanceId());
                }
            }
        }
    }
    
    @Override
    public void recordHistoricUserTaskLogEntry(HistoricTaskLogEntryBuilder taskLogEntryBuilder) {
        processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().createHistoricTaskLogEntry(taskLogEntryBuilder);
    }

    @Override
    public void deleteHistoryUserTaskLog(long logNumber) {
        processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntry(logNumber);
    }

    @Override
    public void createHistoricActivityInstance(ActivityInstance activityInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, activityInstance.getProcessDefinitionId())) {
            createNewHistoricActivityInstance(activityInstance);
        }
    }

    protected HistoricActivityInstanceEntity createNewHistoricActivityInstance(ActivityInstance activityInstance) {
        HistoricActivityInstanceEntity historicActivityInstanceEntity = getHistoricActivityInstanceEntityManager().create(activityInstance);

        getHistoricActivityInstanceEntityManager().insert(historicActivityInstanceEntity);
        return historicActivityInstanceEntity;
    }

}
