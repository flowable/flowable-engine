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
import org.flowable.engine.impl.persistence.CountingTaskEntity;
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
        if (isTaskRelatedEntityCountEnabledGlobally()) {
            ((CountingTaskEntity) taskEntity).setCountEnabled(true);
        }
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
    public void insert(TaskEntity taskEntity, ExecutionEntity execution, boolean fireCreateEvent) {

        // Inherit tenant id (if applicable)
        if (execution != null && execution.getTenantId() != null) {
            taskEntity.setTenantId(execution.getTenantId());
        }

        if (execution != null) {
            execution.getTasks().add(taskEntity);
            taskEntity.setExecutionId(execution.getId());
            taskEntity.setProcessInstanceId(execution.getProcessInstanceId());
            taskEntity.setProcessDefinitionId(execution.getProcessDefinitionId());
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
    }

    @Override
    public void changeTaskAssignee(TaskEntity taskEntity, String assignee) {
        if ((taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee))
                || (taskEntity.getAssignee() == null && assignee != null)) {
            
            taskEntity.setAssignee(assignee);
            fireAssignmentEvents(taskEntity);

            if (taskEntity.getId() != null) {
                getHistoryManager().recordTaskInfoChange(taskEntity);
                addAssigneeIdentityLinks(taskEntity);
                update(taskEntity);
            }
        }
    }

    @Override
    public void changeTaskOwner(TaskEntity taskEntity, String owner) {
        if ((taskEntity.getOwner() != null && !taskEntity.getOwner().equals(owner))
                || (taskEntity.getOwner() == null && owner != null)) {
            
            taskEntity.setOwner(owner);

            if (taskEntity.getId() != null) {
                getHistoryManager().recordTaskInfoChange(taskEntity);
                addOwnerIdentityLink(taskEntity, taskEntity.getOwner());
                update(taskEntity);
            }
        }
    }

    protected void fireAssignmentEvents(TaskEntity taskEntity) {
        getProcessEngineConfiguration().getListenerNotificationHelper()
                .executeTaskListeners(taskEntity, TaskListener.EVENTNAME_ASSIGNMENT);

        if (getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity));
        }

    }

    protected void addAssigneeIdentityLinks(TaskEntity taskEntity) {
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
            if (getEventDispatcher().isEnabled() && !task.isCanceled()) {
                task.setCanceled(true);
                getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createActivityCancelledEvent(task.getExecution().getActivityId(), task.getName(),
                                task.getExecutionId(), task.getProcessInstanceId(),
                                task.getProcessDefinitionId(), "userTask", deleteReason));
            }

            deleteTask(task, deleteReason, cascade, true);
        }
    }

    @Override
    public void deleteTask(TaskEntity task, String deleteReason, boolean cascade, boolean fireEvents) {
        if (!task.isDeleted()) {
            if (fireEvents) {
                getProcessEngineConfiguration().getListenerNotificationHelper()
                        .executeTaskListeners(task, TaskListener.EVENTNAME_DELETE);
            }
            
            task.setDeleted(true);

            String taskId = task.getId();
            ExecutionEntity execution = null;
            if (task.getExecutionId() != null) {
                execution = task.getExecution();
            }

            List<Task> subTasks = findTasksByParentTaskId(taskId);
            for (Task subTask : subTasks) {
                deleteTask((TaskEntity) subTask, deleteReason, cascade, fireEvents);
            }

            boolean isTaskRelatedEntityCountEnabled = isTaskRelatedEntityCountEnabled(task);

            if (!isTaskRelatedEntityCountEnabled || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getIdentityLinkCount() > 0)) {
                getIdentityLinkEntityManager().deleteIdentityLinksByTaskId(taskId);
            }

            if (!isTaskRelatedEntityCountEnabled || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getVariableCount() > 0)) {
                getVariableInstanceEntityManager().deleteVariableInstanceByTask(task);
            }

            if (cascade) {
                getHistoricTaskInstanceEntityManager().delete(taskId);
            } else {
                getHistoryManager().recordTaskEnd(task, execution, deleteReason);
            }

            delete(task, false);
            
            if (getEventDispatcher().isEnabled() && fireEvents) {
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
    public List<Task> findTasksWithRelatedEntitiesByQueryCriteria(TaskQueryImpl taskQuery) {
        return taskDataManager.findTasksWithRelatedEntitiesByQueryCriteria(taskQuery);
    }

    @Override
    public long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery) {
        return taskDataManager.findTaskCountByQueryCriteria(taskQuery);
    }

    @Override
    public List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap) {
        return taskDataManager.findTasksByNativeQuery(parameterMap);
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
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                compatibilityHandler.deleteTask(taskId, deleteReason, cascade);
                return;
            }

            deleteTask(task, deleteReason, cascade, true);
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
