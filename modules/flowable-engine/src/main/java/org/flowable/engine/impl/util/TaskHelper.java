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
package org.flowable.engine.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.event.impl.FlowableIdentityLinkEventBuilder;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.event.impl.FlowableVariableEventBuilder;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayRef;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class TaskHelper {

    public static void changeTaskAssignee(TaskEntity taskEntity, String assignee, String parentIdentityLink) {
        if ((taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee))
                || (taskEntity.getAssignee() == null && assignee != null)) {

            CommandContextUtil.getTaskService().changeTaskAssignee(taskEntity, assignee);
            fireAssignmentEvents(taskEntity);

            if (taskEntity.getId() != null) {
                addAssigneeIdentityLinks(taskEntity, parentIdentityLink);
            }
        }
    }

    public static void changeTaskOwner(TaskEntity taskEntity, String owner, String parentIdentityLink) {
        if ((taskEntity.getOwner() != null && !taskEntity.getOwner().equals(owner))
                || (taskEntity.getOwner() == null && owner != null)) {

            CommandContextUtil.getTaskService().changeTaskOwner(taskEntity, owner);
            if (taskEntity.getId() != null) {
                addOwnerIdentityLink(taskEntity, taskEntity.getOwner(), parentIdentityLink);
            }
        }
    }

    public static void insertTask(TaskEntity taskEntity, ExecutionEntity execution, boolean fireCreateEvent) {
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

        insertTask(taskEntity, fireCreateEvent, IdentityLinkType.PARTICIPANT);

        if (execution != null && CountingEntityUtil.isExecutionRelatedEntityCountEnabled(execution)) {
            CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) execution;
            countingExecutionEntity.setTaskCount(countingExecutionEntity.getTaskCount() + 1);
        }

        if (fireCreateEvent && CommandContextUtil.getEventDispatcher().isEnabled()) {
            if (taskEntity.getAssignee() != null) {
                CommandContextUtil.getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity));
            }
        }

        CommandContextUtil.getHistoryManager().recordTaskCreated(taskEntity, execution);
    }

    public static void insertTask(TaskEntity taskEntity, boolean fireCreateEvent, String parentIdentityLink) {
        if (taskEntity.getOwner() != null) {
            addOwnerIdentityLink(taskEntity, taskEntity.getOwner(), parentIdentityLink);
        }
        if (taskEntity.getAssignee() != null) {
            addAssigneeIdentityLinks(taskEntity, parentIdentityLink);
        }

        CommandContextUtil.getTaskService().insertTask(taskEntity, fireCreateEvent);
    }

    protected static void addAssigneeIdentityLinks(TaskEntity taskEntity, String parentIdentityLink) {
        if (taskEntity.getAssignee() != null && taskEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstance = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getProcessInstanceId());
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstance, taskEntity.getAssignee(), null, parentIdentityLink);
        }
    }

    protected static void addOwnerIdentityLink(TaskEntity taskEntity, String owner, String parentIdentityLink) {
        if (owner == null && taskEntity.getOwner() == null) {
            return;
        }

        if (owner != null && taskEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstance = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getProcessInstanceId());
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstance, owner, null, parentIdentityLink);
        }
    }
    
    /**
     * Deletes all tasks that relate to the same execution.
     *  
     * @param executionEntity The {@link ExecutionEntity} to which the {@link TaskEntity} relate to.
     * @param taskEntities Tasks to be deleted. It is assumed that all {@link TaskEntity} instances need to be related to the same execution.
     */
    public static void deleteTasksForExecution(ExecutionEntity executionEntity, Collection<TaskEntity> taskEntities, String deleteReason) {
        
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        
        // Delete all entities related to the task entities
        for (TaskEntity taskEntity : taskEntities) {
            internalDeleteTask(taskEntity, deleteReason, false, false, true, true);
        }
        
        // Delete the task entities itself
        CommandContextUtil.getTaskService(commandContext).deleteTasksByExecutionId(executionEntity.getId());
        
    }

    /**
     * @param task
     *            The task to be deleted.
     * @param deleteReason
     *            A delete reason that will be stored in the history tables.
     * @param cascade
     *            If true, the historical counterpart will be deleted, otherwise
     *            it will be updated with an end time.
     * @param fireTaskListener
     *            If true, the delete event of the task listener will be called.
     * @param fireEvents
     *            If true, the event dispatcher will be used to fire an event
     *            for the deletion.
     */
    public static void deleteTask(TaskEntity task, String deleteReason, boolean cascade, boolean fireTaskListener, boolean fireEvents) {
        internalDeleteTask(task, deleteReason, cascade, true, fireTaskListener, fireEvents);
    }
        
    protected static void internalDeleteTask(TaskEntity task, String deleteReason, 
            boolean cascade, boolean executeTaskDelete, boolean fireTaskListener, boolean fireEvents) {
        
        if (!task.isDeleted()) {
            
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher();
            fireEvents = fireEvents && eventDispatcher != null && eventDispatcher.isEnabled();

            if (fireTaskListener) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getListenerNotificationHelper()
                        .executeTaskListeners(task, TaskListener.EVENTNAME_DELETE);
            }

            task.setDeleted(true);

            handleRelatedEntities(commandContext, task, deleteReason, cascade, fireTaskListener, fireEvents, eventDispatcher);
            handleTaskHistory(commandContext, task, deleteReason, cascade);

            if (executeTaskDelete) {
                executeTaskDelete(task, commandContext);
            }

            if (fireEvents) {
                fireTaskDeletedEvent(task, commandContext, eventDispatcher);
            }

        }
    }

    protected static void handleRelatedEntities(CommandContext commandContext, TaskEntity task, String deleteReason, boolean cascade,
            boolean fireTaskListener, boolean fireEvents, FlowableEventDispatcher eventDispatcher) {
        
        boolean isTaskRelatedEntityCountEnabled = CountingEntityUtil.isTaskRelatedEntityCountEnabled(task);
        
        if (!isTaskRelatedEntityCountEnabled
                || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getSubTaskCount() > 0)) {
            TaskService taskService = CommandContextUtil.getTaskService(commandContext);
            List<Task> subTasks = taskService.findTasksByParentTaskId(task.getId());
            for (Task subTask : subTasks) {
                internalDeleteTask((TaskEntity) subTask, deleteReason, cascade, true, fireTaskListener, fireEvents); // Sub tasks are always immediately deleted
            }
        }
        
        if (!isTaskRelatedEntityCountEnabled
                || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getIdentityLinkCount() > 0)) {
            
            boolean deleteIdentityLinks = true;
            if (fireEvents) {
                List<IdentityLinkEntity> identityLinks = CommandContextUtil.getIdentityLinkService(commandContext).findIdentityLinksByTaskId(task.getId());
                for (IdentityLinkEntity identityLinkEntity : identityLinks) {
                    eventDispatcher.dispatchEvent(FlowableIdentityLinkEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, identityLinkEntity));
                }
                deleteIdentityLinks = !identityLinks.isEmpty();
            }
            
            if (deleteIdentityLinks) {
                CommandContextUtil.getIdentityLinkService(commandContext).deleteIdentityLinksByTaskId(task.getId());
            }
            
        }
        
        if (!isTaskRelatedEntityCountEnabled
                || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getVariableCount() > 0)) {
            
            Map<String, VariableInstanceEntity> taskVariables = task.getVariableInstanceEntities();
            ArrayList<VariableByteArrayRef> variableByteArrayRefs = new ArrayList<>();
            for (VariableInstanceEntity variableInstanceEntity : taskVariables.values()) {
                if (fireEvents) {
                    eventDispatcher.dispatchEvent(FlowableVariableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, variableInstanceEntity));
                }
                if (variableInstanceEntity.getByteArrayRef() != null && variableInstanceEntity.getByteArrayRef().getId() != null) {
                    variableByteArrayRefs.add(variableInstanceEntity.getByteArrayRef());
                }
            }
            
            for (VariableByteArrayRef variableByteArrayRef : variableByteArrayRefs) {
                CommandContextUtil.getByteArrayEntityManager(commandContext).deleteByteArrayById(variableByteArrayRef.getId());
            }
            
            if (!taskVariables.isEmpty()) {
                CommandContextUtil.getVariableService(commandContext).deleteVariablesByTaskId(task.getId());
            }
        }
    }

    protected static void handleTaskHistory(CommandContext commandContext, TaskEntity task, String deleteReason, boolean cascade) {
        if (cascade) {
            deleteHistoricTask(task.getId());
        } else {
            ExecutionEntity execution = null;
            if (task.getExecutionId() != null) {
                execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(task.getExecutionId());
            }
            CommandContextUtil.getHistoryManager(commandContext).recordTaskEnd(task, execution, deleteReason);
        }
    }

    protected static void executeTaskDelete(TaskEntity task, CommandContext commandContext) {
        CommandContextUtil.getTaskService(commandContext).deleteTask(task, false); // false: event will be sent out later
   
        if (task.getExecutionId() != null && CountingEntityUtil.isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) CommandContextUtil
                    .getExecutionEntityManager(commandContext).findById(task.getExecutionId());
            if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(countingExecutionEntity)) {
                countingExecutionEntity.setTaskCount(countingExecutionEntity.getTaskCount() - 1);
            }
        }
    }

    protected static void fireTaskDeletedEvent(TaskEntity task, CommandContext commandContext,
            FlowableEventDispatcher eventDispatcher) {
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            CommandContextUtil.getEventDispatcher(commandContext).dispatchEvent(
                FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, task));
        }
    }

    public static void deleteTask(String taskId, String deleteReason, boolean cascade) {

        CommandContext commandContext = CommandContextUtil.getCommandContext();
        TaskEntity task = CommandContextUtil.getTaskService(commandContext).getTask(taskId);

        if (task != null) {
            if (task.getExecutionId() != null) {
                throw new FlowableException("The task cannot be deleted because is part of a running process");
            }

            if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext,task.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                compatibilityHandler.deleteTask(taskId, deleteReason, cascade);
                return;
            }

            deleteTask(task, deleteReason, cascade, true, true);
        } else if (cascade) {
            deleteHistoricTask(taskId);
        }
    }

    public static void deleteTasksByProcessInstanceId(String processInstanceId, String deleteReason, boolean cascade) {
        List<TaskEntity> tasks = CommandContextUtil.getTaskService().findTasksByProcessInstanceId(processInstanceId);

        for (TaskEntity task : tasks) {
            if (CommandContextUtil.getEventDispatcher().isEnabled() && !task.isCanceled()) {
                task.setCanceled(true);

                ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
                CommandContextUtil.getEventDispatcher()
                        .dispatchEvent(org.flowable.engine.delegate.event.impl.FlowableEventBuilder
                                .createActivityCancelledEvent(execution.getActivityId(), task.getName(),
                                        task.getExecutionId(), task.getProcessInstanceId(),
                                        task.getProcessDefinitionId(), "userTask", deleteReason));
            }

            deleteTask(task, deleteReason, cascade, true, true);
        }
    }

    public static void deleteHistoricTaskInstancesByProcessInstanceId(String processInstanceId) {
        if (CommandContextUtil.getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
            List<HistoricTaskInstanceEntity> taskInstances = historicTaskService.findHistoricTasksByProcessInstanceId(processInstanceId);
            for (HistoricTaskInstanceEntity historicTaskInstanceEntity : taskInstances) {
                deleteHistoricTask(historicTaskInstanceEntity.getId());
            }
        }
    }

    public static void deleteHistoricTask(String taskId) {
        if (CommandContextUtil.getHistoryManager().isHistoryEnabled()) {
            CommandContextUtil.getCommentEntityManager().deleteCommentsByTaskId(taskId);
            CommandContextUtil.getAttachmentEntityManager().deleteAttachmentsByTaskId(taskId);

            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
            HistoricTaskInstanceEntity historicTaskInstance = historicTaskService.getHistoricTask(taskId);
            if (historicTaskInstance != null) {

                if (historicTaskInstance.getProcessDefinitionId() != null
                        && Flowable5Util.isFlowable5ProcessDefinitionId(CommandContextUtil.getCommandContext(), historicTaskInstance.getProcessDefinitionId())) {
                    Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                    compatibilityHandler.deleteHistoricTask(taskId);
                    return;
                }

                List<HistoricTaskInstanceEntity> subTasks = historicTaskService.findHistoricTasksByParentTaskId(historicTaskInstance.getId());
                for (HistoricTaskInstance subTask : subTasks) {
                    deleteHistoricTask(subTask.getId());
                }

                CommandContextUtil.getHistoricDetailEntityManager().deleteHistoricDetailsByTaskId(taskId);
                CommandContextUtil.getHistoricVariableService().deleteHistoricVariableInstancesByTaskId(taskId);
                CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLinksByTaskId(taskId);

                historicTaskService.deleteHistoricTask(historicTaskInstance);
            }
        }
    }

    protected static void fireAssignmentEvents(TaskEntity taskEntity) {
        CommandContextUtil.getProcessEngineConfiguration().getListenerNotificationHelper().executeTaskListeners(taskEntity, TaskListener.EVENTNAME_ASSIGNMENT);

        if (CommandContextUtil.getEventDispatcher().isEnabled()) {
            CommandContextUtil.getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity));
        }
    }
}
