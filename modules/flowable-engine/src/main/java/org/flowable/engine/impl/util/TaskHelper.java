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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.identitylink.service.event.impl.FlowableIdentityLinkEventBuilder;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.BaseHistoricTaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.event.impl.FlowableVariableEventBuilder;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class TaskHelper {

    public static void completeTask(TaskEntity taskEntity, String userId, Map<String, Object> variables, Map<String, Object> localVariables,
            Map<String, Object> transientVariables, Map<String, Object> localTransientVariables, CommandContext commandContext) {

        if (taskEntity.getDelegationState() != null && taskEntity.getDelegationState() == DelegationState.PENDING) {
            throw new FlowableException("A delegated " + taskEntity + " cannot be completed, but should be resolved instead.");
        }

        if (localVariables != null && !localVariables.isEmpty()) {
            taskEntity.setVariablesLocal(localVariables);
        }

        if (variables != null && !variables.isEmpty()) {
            if (taskEntity.getExecutionId() != null) {
                ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
                if (execution != null) {
                    execution.setVariables(variables);
                }

            } else {
                taskEntity.setVariables(variables);

            }
        }

        if (localTransientVariables != null && !localTransientVariables.isEmpty()) {
            taskEntity.setTransientVariablesLocal(localTransientVariables);
        }

        if (transientVariables != null && !transientVariables.isEmpty()) {
            if (taskEntity.getExecutionId() != null) {
                ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
                if (execution != null) {
                    execution.setTransientVariables(transientVariables);
                }

            } else {
                taskEntity.setTransientVariables(transientVariables);

            }
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        boolean bpmnErrorPropagated = false;
        try {
            processEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(taskEntity, TaskListener.EVENTNAME_COMPLETE);
        } catch (BpmnError bpmnError) {
            if (taskEntity.getExecutionId() != null) {
                ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
                if (execution != null) {
                    ErrorPropagation.propagateError(bpmnError, execution);
                    bpmnErrorPropagated = true;
                }
            }
        }

        if (processEngineConfiguration.getIdentityLinkInterceptor() != null) {
            processEngineConfiguration.getIdentityLinkInterceptor().handleCompleteTask(taskEntity);
        }

        logUserTaskCompleted(taskEntity);

        if (taskEntity.getExecutionId() != null) {
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
            if (execution != null) {
                storeTaskCompleter(taskEntity, execution, processEngineConfiguration);
            }
        }

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            if (variables != null) {

                // The only way a task can be completed + event thrown is either with variables or with localvariables,
                // where a boolean flag is passed through the API.
                // This means that if the flag is set, local variables always have precedence in the event.

                boolean local = localVariables != null && !localVariables.isEmpty();
                Map<String, Object> eventVariables = null;
                if (local) {
                    eventVariables = localVariables;
                } else {
                    eventVariables = variables;
                }

                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityWithVariablesEvent(
                        FlowableEngineEventType.TASK_COMPLETED, taskEntity, eventVariables, local), processEngineConfiguration.getEngineCfgKey());

            } else {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_COMPLETED, taskEntity),
                        processEngineConfiguration.getEngineCfgKey());
            }
        }

        if (processEngineConfiguration.isLoggingSessionEnabled() && taskEntity.getExecutionId() != null) {
            String taskLabel = null;
            if (StringUtils.isNotEmpty(taskEntity.getName())) {
                taskLabel = taskEntity.getName();
            } else {
                taskLabel = taskEntity.getId();
            }

            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId());
            if (execution != null) {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_USER_TASK_COMPLETE,
                        "User task '" + taskLabel + "' completed", taskEntity, execution);
            }
        }

        completeTask(taskEntity, userId);

        // Continue process (if not a standalone task)
        if (taskEntity.getExecutionId() != null && !bpmnErrorPropagated) {
            ExecutionEntity executionEntity = processEngineConfiguration.getExecutionEntityManager().findById(taskEntity.getExecutionId());
            CommandContextUtil.getAgenda(commandContext).planTriggerExecutionOperation(executionEntity);
        }
    }

    protected static void logUserTaskCompleted(TaskEntity taskEntity) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        TaskServiceConfiguration taskServiceConfiguration = processEngineConfiguration.getTaskServiceConfiguration();
        if (taskServiceConfiguration.isEnableHistoricTaskLogging()) {
            BaseHistoricTaskLogEntryBuilderImpl taskLogEntryBuilder = new BaseHistoricTaskLogEntryBuilderImpl(taskEntity);
            ObjectNode data = taskServiceConfiguration.getObjectMapper().createObjectNode();
            taskLogEntryBuilder.timeStamp(taskServiceConfiguration.getClock().getCurrentTime());
            taskLogEntryBuilder.userId(Authentication.getAuthenticatedUserId());
            taskLogEntryBuilder.data(data.toString());
            taskLogEntryBuilder.type(HistoricTaskLogEntryType.USER_TASK_COMPLETED.name());
            taskServiceConfiguration.getInternalHistoryTaskManager().recordHistoryUserTaskLog(taskLogEntryBuilder);
        }
    }

    protected static void storeTaskCompleter(TaskEntity taskEntity, ExecutionEntity execution, ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (taskEntity.getProcessDefinitionId() != null) {
            FlowElement flowElement = execution.getCurrentFlowElement();
            if (flowElement instanceof UserTask userTask) {
                String taskCompleterVariableName = userTask.getTaskCompleterVariableName();
                if (StringUtils.isNotEmpty(taskCompleterVariableName)) {
                    ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
                    Expression expression = expressionManager.createExpression(userTask.getTaskCompleterVariableName());
                    String completerVariableName = (String) expression.getValue(execution);
                    String completer = Authentication.getAuthenticatedUserId();
                    if (StringUtils.isNotEmpty(completerVariableName)) {
                        execution.setVariable(completerVariableName, completer);
                    }
                }
            }
        }
    }

    public static void changeTaskAssignee(TaskEntity taskEntity, String assignee) {
        if ((taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee))
                || (taskEntity.getAssignee() == null && assignee != null)) {

            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            processEngineConfiguration.getTaskServiceConfiguration().getTaskService().changeTaskAssignee(taskEntity, assignee);
            fireAssignmentEvents(taskEntity);

            if (taskEntity.getId() != null) {
                addAssigneeIdentityLinks(taskEntity);
            }
        }
    }

    public static void changeTaskOwner(TaskEntity taskEntity, String owner) {
        if ((taskEntity.getOwner() != null && !taskEntity.getOwner().equals(owner))
                || (taskEntity.getOwner() == null && owner != null)) {

            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            processEngineConfiguration.getTaskServiceConfiguration().getTaskService().changeTaskOwner(taskEntity, owner);
            if (taskEntity.getId() != null) {
                addOwnerIdentityLink(taskEntity, taskEntity.getOwner());
            }
        }
    }

    public static void insertTask(TaskEntity taskEntity, ExecutionEntity execution, boolean fireCreateEvent, boolean addEntityLinks) {
        // Inherit tenant id (if applicable)
        if (execution != null && execution.getTenantId() != null) {
            taskEntity.setTenantId(execution.getTenantId());
        }

        if (execution != null) {
            taskEntity.setExecutionId(execution.getId());
            taskEntity.setProcessInstanceId(execution.getProcessInstanceId());
            taskEntity.setProcessDefinitionId(execution.getProcessDefinitionId());
        }

        insertTask(taskEntity, fireCreateEvent);

        if (execution != null) {

            if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(execution)) {
                CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) execution;
                countingExecutionEntity.setTaskCount(countingExecutionEntity.getTaskCount() + 1);
            }

            if (addEntityLinks) {
                EntityLinkUtil.createEntityLinks(execution.getProcessInstanceId(), execution.getId(),
                        taskEntity.getTaskDefinitionKey(), taskEntity.getId(), ScopeTypes.TASK);
            }

        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (fireCreateEvent && eventDispatcher != null && eventDispatcher.isEnabled()) {
            if (taskEntity.getAssignee() != null) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity),
                        processEngineConfiguration.getEngineCfgKey());
            }
        }

        processEngineConfiguration.getActivityInstanceEntityManager().recordTaskCreated(taskEntity, execution);
    }

    public static void insertTask(TaskEntity taskEntity, boolean fireCreateEvent) {
        if (taskEntity.getOwner() != null) {
            addOwnerIdentityLink(taskEntity, taskEntity.getOwner());
        }
        if (taskEntity.getAssignee() != null) {
            addAssigneeIdentityLinks(taskEntity);
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        processEngineConfiguration.getTaskServiceConfiguration().getTaskService().insertTask(taskEntity, fireCreateEvent);
    }

    public static void addAssigneeIdentityLinks(TaskEntity taskEntity) {
        if (taskEntity.getAssignee() == null) {
            return;
        }
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration.getIdentityLinkInterceptor() != null) {
            processEngineConfiguration.getIdentityLinkInterceptor().handleAddAssigneeIdentityLinkToTask(taskEntity, taskEntity.getAssignee());
        }
    }

    public static void addOwnerIdentityLink(TaskEntity taskEntity, String owner) {
        if (owner == null && taskEntity.getOwner() == null) {
            return;
        }
        
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration.getIdentityLinkInterceptor() != null) {
            processEngineConfiguration.getIdentityLinkInterceptor().handleAddOwnerIdentityLinkToTask(taskEntity, owner);
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
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        // Delete all entities related to the task entities
        for (TaskEntity taskEntity : taskEntities) {
            internalDeleteTask(taskEntity, null, deleteReason, false, false, true, true);
        }
        
        // Delete the task entities itself
        processEngineConfiguration.getTaskServiceConfiguration().getTaskService().deleteTasksByExecutionId(executionEntity.getId());
        
    }
    
    public static void completeTask(TaskEntity task, String userId) {
        internalDeleteTask(task, userId, null, false, true, true, true);
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
        internalDeleteTask(task, null, deleteReason, cascade, true, fireTaskListener, fireEvents);
    }
        
    protected static void internalDeleteTask(TaskEntity task, String userId, String deleteReason, 
            boolean cascade, boolean executeTaskDelete, boolean fireTaskListener, boolean fireEvents) {
        
        if (!task.isDeleted()) {
            
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher(commandContext);
            fireEvents = fireEvents && eventDispatcher != null && eventDispatcher.isEnabled();

            if (fireTaskListener) {
                try {
                    CommandContextUtil.getProcessEngineConfiguration(commandContext).getListenerNotificationHelper()
                            .executeTaskListeners(task, TaskListener.EVENTNAME_DELETE);
                } catch (BpmnError bpmnError) {
                    ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
                    if (execution != null) {
                        ErrorPropagation.propagateError(bpmnError, execution);
                    }
                }
            }

            task.setDeleted(true);

            handleRelatedEntities(task, deleteReason, cascade, fireTaskListener, fireEvents, eventDispatcher, commandContext);
            handleTaskHistory(task, userId, deleteReason, cascade, commandContext);

            if (executeTaskDelete) {
                executeTaskDelete(task, commandContext);
            }

            if (fireEvents) {
                fireTaskDeletedEvent(task, commandContext, eventDispatcher);
            }

        }
    }

    protected static void handleRelatedEntities(TaskEntity task, String deleteReason, boolean cascade,
            boolean fireTaskListener, boolean fireEvents, FlowableEventDispatcher eventDispatcher, CommandContext commandContext) {
        
        boolean isTaskRelatedEntityCountEnabled = CountingEntityUtil.isTaskRelatedEntityCountEnabled(task);
        
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        if (!isTaskRelatedEntityCountEnabled
                || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getSubTaskCount() > 0)) {
            TaskService taskService = processEngineConfiguration.getTaskServiceConfiguration().getTaskService();
            List<Task> subTasks = taskService.findTasksByParentTaskId(task.getId());
            for (Task subTask : subTasks) {
                internalDeleteTask((TaskEntity) subTask, null, deleteReason, cascade, true, fireTaskListener, fireEvents); // Sub tasks are always immediately deleted
            }
        }
        
        if (!isTaskRelatedEntityCountEnabled
                || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getIdentityLinkCount() > 0)) {
            
            boolean deleteIdentityLinks = true;
            if (fireEvents) {
                List<IdentityLinkEntity> identityLinks = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                        .findIdentityLinksByTaskId(task.getId());
                for (IdentityLinkEntity identityLinkEntity : identityLinks) {
                    eventDispatcher.dispatchEvent(FlowableIdentityLinkEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, identityLinkEntity),
                            processEngineConfiguration.getEngineCfgKey());
                }
                deleteIdentityLinks = !identityLinks.isEmpty();
            }
            
            if (deleteIdentityLinks) {
                processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService().deleteIdentityLinksByTaskId(task.getId());
            }
            
        }
        
        if (!isTaskRelatedEntityCountEnabled
                || (isTaskRelatedEntityCountEnabled && ((CountingTaskEntity) task).getVariableCount() > 0)) {
            
            Map<String, VariableInstanceEntity> taskVariables = task.getVariableInstanceEntities();
            List<ByteArrayRef> variableByteArrayRefs = new ArrayList<>();
            for (VariableInstanceEntity variableInstanceEntity : taskVariables.values()) {
                if (fireEvents) {
                    eventDispatcher.dispatchEvent(FlowableVariableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, variableInstanceEntity),
                            processEngineConfiguration.getEngineCfgKey());
                }
                if (variableInstanceEntity.getByteArrayRef() != null && variableInstanceEntity.getByteArrayRef().getId() != null) {
                    variableByteArrayRefs.add(variableInstanceEntity.getByteArrayRef());
                }
            }
            
            for (ByteArrayRef variableByteArrayRef : variableByteArrayRefs) {
                processEngineConfiguration.getByteArrayEntityManager().deleteByteArrayById(variableByteArrayRef.getId());
            }
            
            if (!taskVariables.isEmpty()) {
                processEngineConfiguration.getVariableServiceConfiguration().getVariableService().deleteVariablesByTaskId(task.getId());
            }
        }
    }

    protected static void handleTaskHistory(TaskEntity task, String userId, String deleteReason, boolean cascade, CommandContext commandContext) {
        if (cascade) {
            deleteHistoricTask(task.getId());
        } else {
            ExecutionEntity execution = null;
            if (task.getExecutionId() != null) {
                execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(task.getExecutionId());
            }
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityTaskEnd(task, execution,
                    userId, deleteReason, CommandContextUtil.getProcessEngineConfiguration(commandContext).getClock().getCurrentTime());
        }
    }

    protected static void executeTaskDelete(TaskEntity task, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        processEngineConfiguration.getTaskServiceConfiguration().getTaskService().deleteTask(task, false); // false: event will be sent out later
   
        if (task.getExecutionId() != null && CountingEntityUtil.isExecutionRelatedEntityCountEnabledGlobally()) {
            CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) CommandContextUtil
                    .getExecutionEntityManager(commandContext).findById(task.getExecutionId());
            if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(countingExecutionEntity)) {
                countingExecutionEntity.setTaskCount(countingExecutionEntity.getTaskCount() - 1);
            }
        }
    }

    protected static void fireTaskDeletedEvent(TaskEntity task, CommandContext commandContext, FlowableEventDispatcher eventDispatcher) {
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            CommandContextUtil.getEventDispatcher(commandContext).dispatchEvent(
                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, task),
                    processEngineConfiguration.getEngineCfgKey());
        }
    }

    public static void deleteTask(String taskId, String deleteReason, boolean cascade) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        TaskEntity task = processEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(taskId);

        if (task != null) {
            if (task.getExecutionId() != null) {
                throw new FlowableException("The " + task + " cannot be deleted because is part of a running process");
            } else if (task.getScopeId() != null && ScopeTypes.CMMN.equals(task.getScopeType())) {
                throw new FlowableException("The " + task + " cannot be deleted because is part of a running case");
            }

            if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
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
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        List<TaskEntity> tasks = processEngineConfiguration.getTaskServiceConfiguration().getTaskService().findTasksByProcessInstanceId(processInstanceId);

        for (TaskEntity task : tasks) {
            FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled() && !task.isCanceled()) {
                task.setCanceled(true);

                ExecutionEntity execution = processEngineConfiguration.getExecutionEntityManager().findById(task.getExecutionId());
                eventDispatcher
                        .dispatchEvent(org.flowable.engine.delegate.event.impl.FlowableEventBuilder
                                .createActivityCancelledEvent(execution.getActivityId(), task.getName(),
                                        task.getExecutionId(), task.getProcessInstanceId(),
                                        task.getProcessDefinitionId(), "userTask", deleteReason),
                                processEngineConfiguration.getEngineCfgKey());
            }

            deleteTask(task, deleteReason, cascade, true, true);
        }
    }

    public static void deleteHistoricTaskInstancesByProcessInstanceId(String processInstanceId) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration.getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            HistoricTaskService historicTaskService = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
            List<HistoricTaskInstanceEntity> taskInstances = historicTaskService.findHistoricTasksByProcessInstanceId(processInstanceId);
            for (HistoricTaskInstanceEntity historicTaskInstanceEntity : taskInstances) {
                deleteHistoricTask(historicTaskInstanceEntity.getId());
            }
        }
    }
    
    public static void bulkDeleteHistoricTaskInstancesForProcessInstanceIds(Collection<String> processInstanceIds) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration.getHistoryManager().isHistoryEnabled()) {
            List<String> taskIds = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskInstanceEntityManager()
                    .findHistoricTaskIdsForProcessInstanceIds(processInstanceIds);
            
            if (taskIds != null && !taskIds.isEmpty()) {
                bulkDeleteHistoricTaskInstances(taskIds, processEngineConfiguration);
            }
        }
    }

    public static void deleteHistoricTask(String taskId) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration.getHistoryManager().isHistoryEnabled()) {
            processEngineConfiguration.getCommentEntityManager().deleteCommentsByTaskId(taskId);
            processEngineConfiguration.getAttachmentEntityManager().deleteAttachmentsByTaskId(taskId);

            HistoricTaskService historicTaskService = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
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

                processEngineConfiguration.getHistoricDetailEntityManager().deleteHistoricDetailsByTaskId(taskId);
                if (processEngineConfiguration.getHistoryConfigurationSettings().isHistoryEnabledForVariables(historicTaskInstance)) {
                    processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().deleteHistoricVariableInstancesByTaskId(taskId);
                }
                processEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().deleteHistoricIdentityLinksByTaskId(taskId);

                historicTaskService.deleteHistoricTask(historicTaskInstance);
            }
        }
        deleteHistoricTaskEventLogEntries(taskId);
    }

    public static void deleteHistoricTaskEventLogEntries(String taskId) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        TaskServiceConfiguration taskServiceConfiguration = processEngineConfiguration.getTaskServiceConfiguration();
        if (taskServiceConfiguration.isEnableHistoricTaskLogging()) {
            taskServiceConfiguration.getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
        }
    }

    public static boolean isFormFieldValidationEnabled(VariableContainer variableContainer,
            ProcessEngineConfigurationImpl processEngineConfiguration, String formFieldValidationExpression) {

        if (StringUtils.isNotEmpty(formFieldValidationExpression)) {
            Boolean formFieldValidation = getBoolean(formFieldValidationExpression);
            if (formFieldValidation != null) {
                return formFieldValidation;
            }

            if (variableContainer != null) {
                ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
                Boolean formFieldValidationValue = getBoolean(
                    expressionManager.createExpression(formFieldValidationExpression).getValue(variableContainer)
                );
                if (formFieldValidationValue == null) {
                    throw new FlowableException("Unable to resolve formFieldValidationExpression to boolean value for " + variableContainer);
                }
                return formFieldValidationValue;
            }
            throw new FlowableException("Unable to resolve formFieldValidationExpression without variable container");
        }
        return true;
    }
    
    protected static void bulkDeleteHistoricTaskInstances(Collection<String> taskIds, ProcessEngineConfigurationImpl processEngineConfiguration) {
        HistoricTaskService historicTaskService = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
        List<String> subTaskIds = historicTaskService.findHistoricTaskIdsByParentTaskIds(taskIds);
        if (subTaskIds != null && !subTaskIds.isEmpty()) {
            bulkDeleteHistoricTaskInstances(subTaskIds, processEngineConfiguration);
        }
        
        processEngineConfiguration.getCommentEntityManager().bulkDeleteCommentsForTaskIds(taskIds);
        processEngineConfiguration.getAttachmentEntityManager().bulkDeleteAttachmentsByTaskId(taskIds);
        
        processEngineConfiguration.getHistoricDetailEntityManager().bulkDeleteHistoricDetailsByTaskIds(taskIds);
        processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().bulkDeleteHistoricVariableInstancesByTaskIds(taskIds);
        processEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().bulkDeleteHistoricIdentityLinksForTaskIds(taskIds);

        historicTaskService.bulkDeleteHistoricTaskInstances(taskIds);
        historicTaskService.bulkDeleteHistoricTaskLogEntriesForTaskIds(taskIds);
    }

    protected static Boolean getBoolean(Object booleanObject) {
        if (booleanObject instanceof Boolean) {
            return (Boolean) booleanObject;
        }
        if (booleanObject instanceof String) {
            if ("true".equalsIgnoreCase((String) booleanObject)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase((String) booleanObject)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    protected static void fireAssignmentEvents(TaskEntity taskEntity) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        try {
            processEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(taskEntity, TaskListener.EVENTNAME_ASSIGNMENT);
        } catch (BpmnError e) {
            ErrorPropagation.propagateError(e, CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getExecutionId()));
        }

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, taskEntity),
                    processEngineConfiguration.getEngineCfgKey());
        }
    }
}
