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

import java.util.List;
import java.util.Map;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.api.delegate.event.FlowableEventDispatcher;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.common.impl.identity.Authentication;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public class TaskHelper {

    public static void completeTask(TaskEntity taskEntity, Map<String, Object> variables,
            Map<String, Object> transientVariables, boolean localScope, CommandContext commandContext) {
        
        // Task complete logic

        if (taskEntity.getDelegationState() != null && taskEntity.getDelegationState() == DelegationState.PENDING) {
            throw new FlowableException("A delegated task cannot be completed, but should be resolved instead.");
        }

        if (variables != null) {
            if (localScope) {
                taskEntity.setVariablesLocal(variables);
                
            } else if (taskEntity.getExecutionId() != null) {
                ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
                if (execution != null) {
                    execution.setVariables(variables);
                }
                
            } else {
                taskEntity.setVariables(variables);
            }
        }

        if (transientVariables != null) {
            if (localScope) {
                taskEntity.setTransientVariablesLocal(transientVariables);
            } else {
                taskEntity.setTransientVariables(transientVariables);
            }
        }

        CommandContextUtil.getProcessEngineConfiguration(commandContext).getListenerNotificationHelper().executeTaskListeners(taskEntity, TaskListener.EVENTNAME_COMPLETE);
        if (Authentication.getAuthenticatedUserId() != null && taskEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(taskEntity.getProcessInstanceId());
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstanceEntity, Authentication.getAuthenticatedUserId(), null, IdentityLinkType.PARTICIPANT);
        }

        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher();
        if (eventDispatcher.isEnabled()) {
            if (variables != null) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityWithVariablesEvent(FlowableEngineEventType.TASK_COMPLETED, taskEntity, variables, localScope));
            } else {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_COMPLETED, taskEntity));
            }
        }

        deleteTask(taskEntity, null, false, true);

        // Continue process (if not a standalone task)
        if (taskEntity.getExecutionId() != null) {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(taskEntity.getExecutionId());
            CommandContextUtil.getAgenda(commandContext).planTriggerExecutionOperation(executionEntity);
        }
    }
    
    public static void changeTaskAssignee(TaskEntity taskEntity, String assignee) {
        if ((taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee))
                || (taskEntity.getAssignee() == null && assignee != null)) {
            
            CommandContextUtil.getTaskService().changeTaskAssignee(taskEntity, assignee);
            fireAssignmentEvents(taskEntity);

            if (taskEntity.getId() != null) {
                addAssigneeIdentityLinks(taskEntity);
            }
        }
    }
    
    public static void changeTaskOwner(TaskEntity taskEntity, String owner) {
        if ((taskEntity.getOwner() != null && !taskEntity.getOwner().equals(owner))
                || (taskEntity.getOwner() == null && owner != null)) {
            
            CommandContextUtil.getTaskService().changeTaskOwner(taskEntity, owner);
            
            if (taskEntity.getId() != null) {
                addOwnerIdentityLink(taskEntity, taskEntity.getOwner());
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
        
        insertTask(taskEntity, fireCreateEvent);
        
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
    
    public static void insertTask(TaskEntity taskEntity, boolean fireCreateEvent) {
        if (taskEntity.getOwner() != null) {
            addOwnerIdentityLink(taskEntity, taskEntity.getOwner());
        }
        if (taskEntity.getAssignee() != null) {
            addAssigneeIdentityLinks(taskEntity);
        }
        
        CommandContextUtil.getTaskService().insertTask(taskEntity, fireCreateEvent);
    }
    
    public static void addAssigneeIdentityLinks(TaskEntity taskEntity) {
        if (taskEntity.getAssignee() != null && taskEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstance = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getProcessInstanceId());
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstance, taskEntity.getAssignee(), null, IdentityLinkType.PARTICIPANT);
        }
    }
    
    public static void addOwnerIdentityLink(TaskEntity taskEntity, String owner) {
        if (owner == null && taskEntity.getOwner() == null) {
            return;
        }

        if (owner != null && taskEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstance = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getProcessInstanceId());
            IdentityLinkUtil.createProcessInstanceIdentityLink(processInstance, owner, null, IdentityLinkType.PARTICIPANT);
        }
    }
    
    public static void deleteTask(TaskEntity task, String deleteReason, boolean cascade, boolean fireEvents) {
        if (!task.isDeleted()) {
            if (fireEvents) {
                CommandContextUtil.getProcessEngineConfiguration().getListenerNotificationHelper()
                        .executeTaskListeners(task, TaskListener.EVENTNAME_DELETE);
            }
            
            task.setDeleted(true);

            String taskId = task.getId();
            ExecutionEntity execution = null;
            if (task.getExecutionId() != null) {
                execution = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
            }

            TaskService taskService = CommandContextUtil.getTaskService();
            List<Task> subTasks = taskService.findTasksByParentTaskId(taskId);
            for (Task subTask : subTasks) {
                deleteTask((TaskEntity) subTask, deleteReason, cascade, fireEvents);
            }

            boolean isTaskRelatedEntityCountEnabled = CountingEntityUtil.isTaskRelatedEntityCountEnabled(task);

            if (!isTaskRelatedEntityCountEnabled || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getIdentityLinkCount() > 0)) {
                List<IdentityLinkEntity> identityLinks = CommandContextUtil.getIdentityLinkService().deleteIdentityLinksByTaskId(taskId);
                IdentityLinkUtil.handleTaskIdentityLinkDeletions(task, identityLinks, false);
            }

            if (!isTaskRelatedEntityCountEnabled || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getVariableCount() > 0)) {
                CommandContextUtil.getVariableService().deleteVariableInstanceMap(task.getVariableInstanceEntities());
            }

            if (cascade) {
                deleteHistoricTask(taskId);
            } else {
                CommandContextUtil.getHistoryManager().recordTaskEnd(task, execution, deleteReason);
            }

            deleteTask(task, false);
            
            if (CommandContextUtil.getEventDispatcher().isEnabled() && fireEvents) {
                CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, task));
            }

        }
    }
    
    public static void deleteTask(String taskId, String deleteReason, boolean cascade) {

        TaskEntity task = CommandContextUtil.getTaskService().getTask(taskId);

        if (task != null) {
            if (task.getExecutionId() != null) {
                throw new FlowableException("The task cannot be deleted because is part of a running process");
            }

            if (Flowable5Util.isFlowable5ProcessDefinitionId(CommandContextUtil.getCommandContext(), task.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                compatibilityHandler.deleteTask(taskId, deleteReason, cascade);
                return;
            }

            deleteTask(task, deleteReason, cascade, true);
        } else if (cascade) {
            deleteHistoricTask(taskId);
        }
    }
    
    public static void deleteTask(TaskEntity task, boolean fireEvents) {
        CommandContextUtil.getTaskService().deleteTask(task, fireEvents);
        
        if (task.getExecutionId() != null && CountingEntityUtil.isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
            if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(countingExecutionEntity)) {
                countingExecutionEntity.setTaskCount(countingExecutionEntity.getTaskCount() - 1);
            }
        }
    }
    
    public static void deleteTasksByProcessInstanceId(String processInstanceId, String deleteReason, boolean cascade) {
        List<TaskEntity> tasks = CommandContextUtil.getTaskService().findTasksByProcessInstanceId(processInstanceId);
        
        for (TaskEntity task : tasks) {
            if (CommandContextUtil.getEventDispatcher().isEnabled() && !task.isCanceled()) {
                task.setCanceled(true);
                
                ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
                
                CommandContextUtil.getEventDispatcher().dispatchEvent(
                        org.flowable.engine.delegate.event.impl.FlowableEventBuilder.createActivityCancelledEvent(execution.getActivityId(), task.getName(),
                                task.getExecutionId(), task.getProcessInstanceId(),
                                task.getProcessDefinitionId(), "userTask", deleteReason));
            }

            deleteTask(task, deleteReason, cascade, true);
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
            CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity));
        }
    }
}
