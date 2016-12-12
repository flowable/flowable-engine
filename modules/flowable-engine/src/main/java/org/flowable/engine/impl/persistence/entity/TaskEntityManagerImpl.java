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

package org.flowable.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.TaskQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.data.TaskDataManager;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.task.IdentityLinkType;
import org.flowable.engine.task.Task;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TaskEntityManagerImpl extends AbstractEntityManager<TaskEntity> implements TaskEntityManager {
  
  protected TaskDataManager taskDataManager;
  
  public TaskEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, TaskDataManager taskDataManager) {
    super(processEngineConfiguration);
    this.taskDataManager = taskDataManager;
  }
  
  @Override
  protected DataManager<TaskEntity> getDataManager() {
    return taskDataManager;
  }
  
  @Override
  public TaskEntity create() {
    TaskEntity taskEntity = super.create();
    taskEntity.setCreateTime(getClock().getCurrentTime());
    return taskEntity;
  }
  
  @Override
  public void insert(TaskEntity taskEntity, boolean fireCreateEvent) {

    if (taskEntity.getOwner() != null) {
      addOwnerIdentityLink(taskEntity, taskEntity.getOwner());
    }
    if (taskEntity.getAssignee() != null) {
      addAssigneeIdentityLinks(taskEntity);
    }
    
    super.insert(taskEntity, fireCreateEvent);
    
  }
  
  @Override
  public void insert(TaskEntity taskEntity, ExecutionEntity execution) {

    // Inherit tenant id (if applicable)
    if (execution != null && execution.getTenantId() != null) {
      taskEntity.setTenantId(execution.getTenantId());
    }

    if (execution != null) {
      execution.getTasks().add(taskEntity);
      taskEntity.setExecutionId(execution.getId());
      taskEntity.setProcessInstanceId(execution.getProcessInstanceId());
      taskEntity.setProcessDefinitionId(execution.getProcessDefinitionId());
      
      getHistoryManager().recordTaskExecutionIdChange(taskEntity.getId(), taskEntity.getExecutionId());
    }
    
    insert(taskEntity, true);
    
    if (execution != null && isExecutionRelatedEntityCountEnabled(execution)) {
      CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) execution;
      countingExecutionEntity.setTaskCount(countingExecutionEntity.getTaskCount() + 1);
    }
    
    if (getEventDispatcher().isEnabled()) {
      if (taskEntity.getAssignee() != null) {
        getEventDispatcher().dispatchEvent(
            FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity));
      }
    }
    
    getHistoryManager().recordTaskCreated(taskEntity, execution);
    getHistoryManager().recordTaskId(taskEntity);
    if (taskEntity.getFormKey() != null) {
      getHistoryManager().recordTaskFormKeyChange(taskEntity.getId(), taskEntity.getFormKey());
    }
  }
  
  @Override
  public void changeTaskAssignee(TaskEntity taskEntity, String assignee) {
    if ( (taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee)) 
        || (taskEntity.getAssignee() == null && assignee != null)) {
      taskEntity.setAssignee(assignee);
      fireAssignmentEvents(taskEntity);
      
      if (taskEntity.getId() != null) {
        getHistoryManager().recordTaskAssigneeChange(taskEntity.getId(), taskEntity.getAssignee());
        addAssigneeIdentityLinks(taskEntity);
        update(taskEntity);
      }
    }
  }
  
  @Override
  public void changeTaskOwner(TaskEntity taskEntity, String owner) {
    if ( (taskEntity.getOwner() != null && !taskEntity.getOwner().equals(owner)) 
        || (taskEntity.getOwner() == null && owner != null)) {
      taskEntity.setOwner(owner);
      
      if (taskEntity.getId() != null) {
        getHistoryManager().recordTaskOwnerChange(taskEntity.getId(), taskEntity.getOwner());
        addOwnerIdentityLink(taskEntity, taskEntity.getOwner());
        update(taskEntity);
      }
    }
  }
  
  protected void fireAssignmentEvents(TaskEntity taskEntity) {
    getProcessEngineConfiguration().getListenerNotificationHelper()
      .executeTaskListeners(taskEntity, TaskListener.EVENTNAME_ASSIGNMENT);
    getHistoryManager().recordTaskAssignment(taskEntity);

    if (getEventDispatcher().isEnabled()) {
      getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity));
    }

  }

  private void addAssigneeIdentityLinks(TaskEntity taskEntity) {
    if (taskEntity.getAssignee() != null && taskEntity.getProcessInstance() != null) {
      getIdentityLinkEntityManager().involveUser(taskEntity.getProcessInstance(), taskEntity.getAssignee(), IdentityLinkType.PARTICIPANT);
    }
  }
  
  protected void addOwnerIdentityLink(TaskEntity taskEntity, String owner) {
    if (owner == null && taskEntity.getOwner() == null) {
      return;
    }
    
    if (owner != null && taskEntity.getProcessInstanceId() != null) {
      getIdentityLinkEntityManager().involveUser(taskEntity.getProcessInstance(), owner, IdentityLinkType.PARTICIPANT);
    }
  }
  
  @Override
  public void deleteTasksByProcessInstanceId(String processInstanceId, String deleteReason, boolean cascade) {
    List<TaskEntity> tasks = findTasksByProcessInstanceId(processInstanceId);

    for (TaskEntity task : tasks) {
      if (getEventDispatcher().isEnabled() && task.isCanceled() == false) {
        task.setCanceled(true);
        getEventDispatcher().dispatchEvent(
              FlowableEventBuilder.createActivityCancelledEvent(task.getExecution().getActivityId(), task.getName(), 
                  task.getExecutionId(), task.getProcessInstanceId(),
                  task.getProcessDefinitionId(), "userTask", deleteReason));
      }

      deleteTask(task, deleteReason, cascade, false);
    }
  }
  
  @Override
  public void deleteTask(TaskEntity task, String deleteReason, boolean cascade, boolean cancel) {
    if (!task.isDeleted()) {
      getProcessEngineConfiguration().getListenerNotificationHelper()
          .executeTaskListeners(task, TaskListener.EVENTNAME_DELETE);
      task.setDeleted(true);

      String taskId = task.getId();

      List<Task> subTasks = findTasksByParentTaskId(taskId);
      for (Task subTask : subTasks) {
        deleteTask((TaskEntity) subTask, deleteReason, cascade, cancel);
      }

      getIdentityLinkEntityManager().deleteIdentityLinksByTaskId(taskId);
      getVariableInstanceEntityManager().deleteVariableInstanceByTask(task);

      if (cascade) {
        getHistoricTaskInstanceEntityManager().delete(taskId);
      } else {
        getHistoryManager().recordTaskEnd(taskId, deleteReason);
      }

      delete(task, false);

      if (getEventDispatcher().isEnabled()) {
        if (cancel && task.isCanceled() == false) {
          task.setCanceled(true);
          getEventDispatcher().dispatchEvent(
                  FlowableEventBuilder.createActivityCancelledEvent(task.getExecution() != null ? task.getExecution().getActivityId() : null, 
                      task.getName(), task.getExecutionId(), 
                      task.getProcessInstanceId(),
                      task.getProcessDefinitionId(), 
                      "userTask", 
                      deleteReason));
        }
        
        getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, task));
      }
    }
  }
  
  @Override
  public void delete(TaskEntity entity, boolean fireDeleteEvent) {
    super.delete(entity, fireDeleteEvent);
    
    if (entity.getExecutionId() != null && isExecutionRelatedEntityCountEnabledGlobally()) {
      CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) entity.getExecution();
      if (isExecutionRelatedEntityCountEnabled(countingExecutionEntity)) {
        countingExecutionEntity.setTaskCount(countingExecutionEntity.getTaskCount() - 1);
      }
    }
  }

  @Override
  public List<TaskEntity> findTasksByExecutionId(String executionId) {
    return taskDataManager.findTasksByExecutionId(executionId);
  }

  @Override
  public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
    return taskDataManager.findTasksByProcessInstanceId(processInstanceId);
  }

  @Override
  public List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery) {
    return taskDataManager.findTasksByQueryCriteria(taskQuery);
  }

  @Override
  public List<Task> findTasksAndVariablesByQueryCriteria(TaskQueryImpl taskQuery) {
    return taskDataManager.findTasksAndVariablesByQueryCriteria(taskQuery);
  }

  @Override
  public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
    return taskDataManager.findTaskCountByQueryCriteria(taskQuery);
  }

  @Override
  public List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return taskDataManager.findTasksByNativeQuery(parameterMap, firstResult, maxResults);
  }

  @Override
  public long findTaskCountByNativeQuery(Map<String, Object> parameterMap) {
    return taskDataManager.findTaskCountByNativeQuery(parameterMap);
  }

  @Override
  public List<Task> findTasksByParentTaskId(String parentTaskId) {
    return taskDataManager.findTasksByParentTaskId(parentTaskId);
  }

  @Override
  public void deleteTask(String taskId, String deleteReason, boolean cascade) {
    
    TaskEntity task = findById(taskId);

    if (task != null) {
      if (task.getExecutionId() != null) {
        throw new FlowableException("The task cannot be deleted because is part of a running process");
      }
      
      if (Flowable5Util.isFlowable5ProcessDefinitionId(getCommandContext(), task.getProcessDefinitionId())) {
        Flowable5CompatibilityHandler activiti5CompatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler(); 
        activiti5CompatibilityHandler.deleteTask(taskId, deleteReason, cascade);
        return;
      }

      deleteTask(task, deleteReason, cascade, false);
    } else if (cascade) {
      getHistoricTaskInstanceEntityManager().delete(taskId);
    }
  }

  @Override
  public void updateTaskTenantIdForDeployment(String deploymentId, String newTenantId) {
    taskDataManager.updateTaskTenantIdForDeployment(deploymentId, newTenantId);
  }

  public TaskDataManager getTaskDataManager() {
    return taskDataManager;
  }

  public void setTaskDataManager(TaskDataManager taskDataManager) {
    this.taskDataManager = taskDataManager;
  }
  
}
