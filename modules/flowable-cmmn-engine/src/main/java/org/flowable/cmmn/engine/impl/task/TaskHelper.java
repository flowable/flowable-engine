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
package org.flowable.cmmn.engine.impl.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.event.FlowableCmmnEventBuilder;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.task.service.impl.BaseHistoricTaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class TaskHelper {

    public static void insertTask(TaskEntity taskEntity, boolean fireCreateEvent, CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().insertTask(taskEntity, fireCreateEvent);

        if (taskEntity.getOwner() != null) {
            addOwnerIdentityLink(taskEntity, cmmnEngineConfiguration);
        }

        if (taskEntity.getAssignee() != null) {
            addAssigneeIdentityLinks(taskEntity, cmmnEngineConfiguration);
            fireAssignmentEvents(taskEntity, cmmnEngineConfiguration);
        }

    }
    
    public static void completeTask(TaskEntity task, String userId, CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.getPlanItemInstanceEntityManager().updateHumanTaskPlanItemInstanceCompletedBy(task, userId);
        internalDeleteTask(task, userId, null, false, true, cmmnEngineConfiguration);
    }

    public static void deleteTask(String taskId, String deleteReason, boolean cascade, CmmnEngineConfiguration cmmnEngineConfiguration) {
        TaskEntity task = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(taskId);
        if (task != null) {
            if (task.getScopeId() != null && ScopeTypes.CMMN.equals(task.getScopeType())) {
                throw new FlowableException("The " + task + " cannot be deleted because is part of a running case instance");
            } else if (task.getExecutionId() != null) {
                throw new FlowableException("The " + task + " cannot be deleted because is part of a running process instance");
            }
            deleteTask(task, deleteReason, cascade, true, cmmnEngineConfiguration);
            
        } else if (cascade) {
            deleteHistoricTask(taskId, cmmnEngineConfiguration);
        }
    }
    
    public static void deleteTask(TaskEntity task, String deleteReason, boolean cascade, 
            boolean fireEvents, CmmnEngineConfiguration cmmnEngineConfiguration) {
        
        internalDeleteTask(task, null, deleteReason, cascade, fireEvents, cmmnEngineConfiguration);
    }

    protected static void internalDeleteTask(TaskEntity task, String userId, String deleteReason, boolean cascade, 
            boolean fireEvents, CmmnEngineConfiguration cmmnEngineConfiguration) {
        
        if (!task.isDeleted()) {
            task.setDeleted(true);

            TaskService taskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService();
            List<Task> subTasks = taskService.findTasksByParentTaskId(task.getId());
            for (Task subTask : subTasks) {
                internalDeleteTask((TaskEntity) subTask, userId, deleteReason, cascade, fireEvents, cmmnEngineConfiguration);
            }

            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) task;
            
            if (countingTaskEntity.isCountEnabled() && countingTaskEntity.getIdentityLinkCount() > 0) {    
                cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService().deleteIdentityLinksByTaskId(task.getId());
            }
            
            if (countingTaskEntity.isCountEnabled() && countingTaskEntity.getVariableCount() > 0) {
                
                Map<String, VariableInstanceEntity> taskVariables = task.getVariableInstanceEntities();
                List<ByteArrayRef> variableByteArrayRefs = new ArrayList<>();
                for (VariableInstanceEntity variableInstanceEntity : taskVariables.values()) {
                    if (variableInstanceEntity.getByteArrayRef() != null && variableInstanceEntity.getByteArrayRef().getId() != null) {
                        variableByteArrayRefs.add(variableInstanceEntity.getByteArrayRef());
                    }
                }
                
                for (ByteArrayRef variableByteArrayRef : variableByteArrayRefs) {
                    cmmnEngineConfiguration.getByteArrayEntityManager().deleteByteArrayById(variableByteArrayRef.getId());
                }
                
                if (!taskVariables.isEmpty()) {
                    cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService().deleteVariablesByTaskId(task.getId());
                }
                
                cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService().deleteVariablesByTaskId(task.getId());
            }
            
            if (cascade) {
                deleteHistoricTask(task.getId(), cmmnEngineConfiguration);
            } else {
                cmmnEngineConfiguration.getCmmnHistoryManager().recordTaskEnd(task, userId, deleteReason,
                        cmmnEngineConfiguration.getClock().getCurrentTime());
            }

            cmmnEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_DELETE);
            cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().deleteTask(task, fireEvents);
        }
    }

    public static void changeTaskAssignee(TaskEntity taskEntity, String assignee, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if ((taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee))
                || (taskEntity.getAssignee() == null && assignee != null)) {
            
            cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().changeTaskAssignee(taskEntity, assignee);
            cmmnEngineConfiguration.getPlanItemInstanceEntityManager().updateHumanTaskPlanItemInstanceAssignee(taskEntity, assignee);
            fireAssignmentEvents(taskEntity, cmmnEngineConfiguration);

            if (taskEntity.getId() != null) {
                addAssigneeIdentityLinks(taskEntity, cmmnEngineConfiguration);
            }
        }
    }
    
    public static void changeTaskOwner(TaskEntity taskEntity, String owner, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if ((taskEntity.getOwner() != null && !taskEntity.getOwner().equals(owner))
                || (taskEntity.getOwner() == null && owner != null)) {

            cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().changeTaskOwner(taskEntity, owner);

            if (taskEntity.getId() != null) {
                addOwnerIdentityLink(taskEntity, cmmnEngineConfiguration);
            }
        }
    }
    
    protected static void addAssigneeIdentityLinks(TaskEntity taskEntity, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if (taskEntity.getAssignee() == null) {
            return;
        }
        if (cmmnEngineConfiguration.getIdentityLinkInterceptor() != null) {
            cmmnEngineConfiguration.getIdentityLinkInterceptor().handleAddAssigneeIdentityLinkToTask(taskEntity, taskEntity.getAssignee());
        }
    }

    protected static void addOwnerIdentityLink(TaskEntity taskEntity, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if (taskEntity.getOwner() == null) {
            return;
        }
        if (cmmnEngineConfiguration.getIdentityLinkInterceptor() != null) {
            cmmnEngineConfiguration.getIdentityLinkInterceptor().handleAddOwnerIdentityLinkToTask(taskEntity, taskEntity.getOwner());
        }
    }

    public static void deleteHistoricTaskInstancesByCaseInstanceId(String caseInstanceId, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            List<HistoricTaskInstance> taskInstances = cmmnEngineConfiguration.getCmmnHistoryService()
                    .createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstanceId)
                    .list();

            for (HistoricTaskInstance taskInstance : taskInstances) {
                deleteHistoricTask(taskInstance.getId(), cmmnEngineConfiguration);
            }
        }
    }
    
    public static void bulkDeleteHistoricTaskInstancesByCaseInstanceIds(Collection<String> caseInstanceIds, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            List<String> taskIds = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskInstanceEntityManager()
                    .findHistoricTaskIdsForScopeIdsAndScopeType(caseInstanceIds, ScopeTypes.CMMN);
            
            if (taskIds != null && !taskIds.isEmpty()) {
                bulkDeleteHistoricTaskInstances(taskIds, cmmnEngineConfiguration);
            }
        }
    }

    public static void deleteHistoricTask(String taskId, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            HistoricTaskService historicTaskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
            HistoricTaskInstanceEntity historicTaskInstance = historicTaskService.getHistoricTask(taskId);
            if (historicTaskInstance != null) {
    
                List<HistoricTaskInstanceEntity> subTasks = historicTaskService.findHistoricTasksByParentTaskId(historicTaskInstance.getId());
                for (HistoricTaskInstance subTask : subTasks) {
                    deleteHistoricTask(subTask.getId(), cmmnEngineConfiguration);
                }

                if (cmmnEngineConfiguration.getCmmnHistoryConfigurationSettings().isHistoryEnabledForVariables(historicTaskInstance)) {
                    cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().deleteHistoricVariableInstancesByTaskId(taskId);
                }

                cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().deleteHistoricIdentityLinksByTaskId(taskId);
    
                historicTaskService.deleteHistoricTask(historicTaskInstance);
            }
        }
        deleteHistoricTaskLogEntries(taskId, cmmnEngineConfiguration);
    }

    public static void deleteHistoricTaskLogEntries(String taskId, CmmnEngineConfiguration cmmnEngineConfiguration) {
        if (cmmnEngineConfiguration.getTaskServiceConfiguration().isEnableHistoricTaskLogging()) {
            cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
        }
    }

    public static void logUserTaskCompleted(TaskEntity taskEntity, CmmnEngineConfiguration cmmnEngineConfiguration) {
        TaskServiceConfiguration taskServiceConfiguration = cmmnEngineConfiguration.getTaskServiceConfiguration();
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

    public static boolean isFormFieldValidationEnabled(VariableContainer variableContainer,
        CmmnEngineConfiguration cmmnEngineConfiguration, String formFieldValidationExpression) {
        if (StringUtils.isNotEmpty(formFieldValidationExpression)) {
            Boolean formFieldValidation = getBoolean(formFieldValidationExpression);
            if (formFieldValidation != null) {
                return formFieldValidation;
            }

            if (variableContainer != null) {
                ExpressionManager expressionManager = cmmnEngineConfiguration.getExpressionManager();
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
    
    protected static void bulkDeleteHistoricTaskInstances(Collection<String> taskIds, CmmnEngineConfiguration cmmnEngineConfiguration) {
        HistoricTaskService historicTaskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
        
        List<String> subTaskIds = historicTaskService.findHistoricTaskIdsByParentTaskIds(taskIds);
        if (subTaskIds != null && !subTaskIds.isEmpty()) {
            bulkDeleteHistoricTaskInstances(subTaskIds, cmmnEngineConfiguration);
        }
        
        cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().bulkDeleteHistoricVariableInstancesByTaskIds(taskIds);
        cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().bulkDeleteHistoricIdentityLinksForTaskIds(taskIds);
        
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

    protected static void fireAssignmentEvents(TaskEntity taskEntity, CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(taskEntity, TaskListener.EVENTNAME_ASSIGNMENT);

        FlowableEventDispatcher eventDispatcher = cmmnEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableCmmnEventBuilder.createTaskAssignedEvent(taskEntity), cmmnEngineConfiguration.getEngineCfgKey());
        }
    }

}
