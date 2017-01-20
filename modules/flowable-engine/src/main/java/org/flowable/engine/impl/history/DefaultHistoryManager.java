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
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class that centralises recording of all history-related operations that are originated from inside the engine.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class DefaultHistoryManager extends AbstractHistoryManager {

  private static Logger log = LoggerFactory.getLogger(DefaultHistoryManager.class.getName());

  public DefaultHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration, HistoryLevel historyLevel) {
    super(processEngineConfiguration, historyLevel);
  }

  // Process related history

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordProcessInstanceEnd(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void recordProcessInstanceEnd(String processInstanceId, String deleteReason, String activityId) {

    if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
      HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);

      if (historicProcessInstance != null) {
        historicProcessInstance.markEnded(deleteReason);
        historicProcessInstance.setEndActivityId(activityId);
        
        // Fire event
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
          eventDispatcher.dispatchEvent(
              FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED, historicProcessInstance));
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

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordProcessInstanceStart (org.flowable.engine.impl.persistence.entity.ExecutionEntity)
   */
  @Override
  public void recordProcessInstanceStart(ExecutionEntity processInstance, FlowElement startElement) {
    if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
      HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().create(processInstance); 
      historicProcessInstance.setStartActivityId(startElement.getId());

      // Insert historic process-instance
      getHistoricProcessInstanceEntityManager().insert(historicProcessInstance, false);
      
      // Fire event
      FlowableEventDispatcher eventDispatcher = getEventDispatcher();
      if (eventDispatcher != null && eventDispatcher.isEnabled()) {
        eventDispatcher.dispatchEvent(
            FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED, historicProcessInstance));
      }

    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordSubProcessInstanceStart (org.flowable.engine.impl.persistence.entity.ExecutionEntity,
   * org.flowable.engine.impl.persistence.entity.ExecutionEntity)
   */
  @Override
  public void recordSubProcessInstanceStart(ExecutionEntity parentExecution, ExecutionEntity subProcessInstance, FlowElement initialElement) {
    if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {

      HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().create(subProcessInstance); 

      // Fix for ACT-1728: startActivityId not initialized with subprocess instance
      if (historicProcessInstance.getStartActivityId() == null) {
        historicProcessInstance.setStartActivityId(initialElement.getId());
      }
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

  // Activity related history

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordActivityStart (org.flowable.engine.impl.persistence.entity.ExecutionEntity)
   */
  @Override
  public void recordActivityStart(ExecutionEntity executionEntity) {
    if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
      if (executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null) {
        
        HistoricActivityInstanceEntity historicActivityInstanceEntity = null;
        
        // Historic activity instance could have been created (but only in cache, never persisted)
        // for example when submitting form properties
        HistoricActivityInstanceEntity historicActivityInstanceEntityFromCache = 
            getHistoricActivityInstanceFromCache(executionEntity.getId(), executionEntity.getActivityId(), true);
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

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordActivityEnd (org.flowable.engine.impl.persistence.entity.ExecutionEntity)
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordProcessDefinitionChange(java.lang.String, java.lang.String)
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordTaskCreated (org.flowable.engine.impl.persistence.entity.TaskEntity,
   * org.flowable.engine.impl.persistence.entity.ExecutionEntity)
   */
  @Override
  public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().create(task, execution);
      
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

  /*
   * (non-Javadoc)
   *
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordTaskClaim (org.flowable.engine.impl.persistence.entity.TaskEntity)
   */

  @Override
  public void recordTaskClaim(TaskEntity task) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(task.getId());
      if (historicTaskInstance != null) {
        historicTaskInstance.setClaimTime(task.getClaimTime());
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordTaskEnd (java.lang.String, java.lang.String)
   */
  @Override
  public void recordTaskEnd(String taskId, String deleteReason) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.markEnded(deleteReason);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordTaskAssigneeChange(org.flowable.engine.impl.persistence.entity.TaskEntity, java.lang.String)
   */
  @Override
  public void recordTaskAssigneeChange(TaskEntity task, String assignee) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(task.getId());
      if (historicTaskInstance != null) {
        historicTaskInstance.setAssignee(assignee);
      }
    }
    
    if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
      ExecutionEntity executionEntity = task.getExecution();
      if (executionEntity != null) {
        HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(executionEntity, false, true);
        if (historicActivityInstance != null) {
          historicActivityInstance.setAssignee(assignee);
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordTaskOwnerChange(java.lang.String, java.lang.String)
   */
  @Override
  public void recordTaskOwnerChange(String taskId, String owner) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setOwner(owner);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordTaskNameChange (java.lang.String, java.lang.String)
   */
  @Override
  public void recordTaskNameChange(String taskId, String taskName) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setName(taskName);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordTaskDescriptionChange(java.lang.String, java.lang.String)
   */
  @Override
  public void recordTaskDescriptionChange(String taskId, String description) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setDescription(description);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordTaskDueDateChange(java.lang.String, java.util.Date)
   */
  @Override
  public void recordTaskDueDateChange(String taskId, Date dueDate) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setDueDate(dueDate);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordTaskPriorityChange(java.lang.String, int)
   */
  @Override
  public void recordTaskPriorityChange(String taskId, int priority) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setPriority(priority);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordTaskCategoryChange(java.lang.String, java.lang.String)
   */
  @Override
  public void recordTaskCategoryChange(String taskId, String category) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setCategory(category);
      }
    }
  }

  @Override
  public void recordTaskFormKeyChange(String taskId, String formKey) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setFormKey(formKey);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordTaskParentTaskIdChange(java.lang.String, java.lang.String)
   */
  @Override
  public void recordTaskParentTaskIdChange(String taskId, String parentTaskId) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setParentTaskId(parentTaskId);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordTaskDefinitionKeyChange (org.flowable.engine.impl.persistence.entity.TaskEntity, java.lang.String)
   */
  @Override
  public void recordTaskDefinitionKeyChange(String taskId, String taskDefinitionKey) {
    if (isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setTaskDefinitionKey(taskDefinitionKey);
      }
    }
  }
  
  /* (non-Javadoc)
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordTaskProcessDefinitionChange(java.lang.String, java.lang.String)
   */
  @Override
  public void recordTaskProcessDefinitionChange(String taskId, String processDefinitionId) {
    if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
      HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskInstanceEntityManager().findById(taskId);
      if (historicTaskInstance != null) {
        historicTaskInstance.setProcessDefinitionId(processDefinitionId);
      }
    }
  }

  // Variables related history

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordVariableCreate (org.flowable.engine.impl.persistence.entity.VariableInstanceEntity)
   */
  @Override
  public void recordVariableCreate(VariableInstanceEntity variable) {
    // Historic variables
    if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY)) {
     getHistoricVariableInstanceEntityManager().copyAndInsert(variable);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordHistoricDetailVariableCreate (org.flowable.engine.impl.persistence.entity.VariableInstanceEntity,
   * org.flowable.engine.impl.persistence.entity.ExecutionEntity, boolean)
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface#recordVariableUpdate (org.flowable.engine.impl.persistence.entity.VariableInstanceEntity)
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# reportFormPropertiesSubmitted (org.flowable.engine.impl.persistence.entity.ExecutionEntity, java.util.Map, java.lang.String)
   */
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
  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# recordIdentityLinkCreated (org.flowable.engine.impl.persistence.entity.IdentityLinkEntity)
   */
  @Override
  public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
    // It makes no sense storing historic counterpart for an identity-link
    // that is related
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

  /*
   * (non-Javadoc)
   * 
   * @see org.flowable.engine.impl.history.HistoryManagerInterface# updateProcessBusinessKeyInHistory (org.flowable.engine.impl.persistence.entity.ExecutionEntity)
   */
  @Override
  public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
    if (isHistoryEnabled()) {
      if (log.isDebugEnabled()) {
        log.debug("updateProcessBusinessKeyInHistory : {}", processInstance.getId());
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
