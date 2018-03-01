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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.type.VariableScopeType;

/**
 * @author Joram Barrez
 */
public class TaskHelper {
    
    public static void insertTask(TaskEntity taskEntity, boolean fireCreateEvent) {
        if (taskEntity.getOwner() != null) {
            addOwnerIdentityLink(taskEntity, taskEntity.getOwner());
        }
        if (taskEntity.getAssignee() != null) {
            addAssigneeIdentityLinks(taskEntity);
        }
        
        CommandContextUtil.getTaskService().insertTask(taskEntity, fireCreateEvent);
        CommandContextUtil.getCmmnHistoryManager().recordTaskCreated(taskEntity);
    }

    public static void deleteTask(TaskEntity task, String deleteReason, boolean cascade, boolean fireEvents) {
        if (!task.isDeleted()) {
            task.setDeleted(true);

            CommandContext commandContext = CommandContextUtil.getCommandContext();
            TaskService taskService = CommandContextUtil.getTaskService(commandContext);
            List<Task> subTasks = taskService.findTasksByParentTaskId(task.getId());
            for (Task subTask : subTasks) {
                deleteTask((TaskEntity) subTask, deleteReason, cascade, fireEvents);
            }

            CommandContextUtil.getIdentityLinkService(commandContext).deleteIdentityLinksByTaskId(task.getId());
            CommandContextUtil.getVariableService(commandContext).deleteVariableInstanceMap(task.getVariableInstanceEntities());
            
            if (cascade) {
                deleteHistoricTask(task.getId());
            } else {
                CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskEnd(task, deleteReason);
            }
            
            CommandContextUtil.getTaskService().deleteTask(task, fireEvents);
        }
    }
    
    public static void deleteTask(String taskId, String deleteReason, boolean cascade) {

        TaskEntity task = CommandContextUtil.getTaskService().getTask(taskId);

        if (task != null) {
            if (task.getScopeId() != null && VariableScopeType.CMMN.equals(task.getScopeType())) {
                throw new FlowableException("The task cannot be deleted because is part of a running case instance");
            }

            deleteTask(task, deleteReason, cascade, true);
            
        } else if (cascade) {
            deleteHistoricTask(taskId);
        }
    }
    
    public static void deleteTask(TaskEntity task, boolean fireEvents) {
        CommandContextUtil.getTaskService().deleteTask(task, fireEvents);
    }
    
    public static void deleteTasksByProcessInstanceId(String processInstanceId, String deleteReason, boolean cascade) {
        List<TaskEntity> tasks = CommandContextUtil.getTaskService().findTasksByProcessInstanceId(processInstanceId);
        
        for (TaskEntity task : tasks) {
            if (CommandContextUtil.getEventDispatcher().isEnabled() && !task.isCanceled()) {
                task.setCanceled(true);
            }

            deleteTask(task, deleteReason, cascade, true);
        }
    }
    
    public static void changeTaskAssignee(TaskEntity taskEntity, String assignee) {
        if ((taskEntity.getAssignee() != null && !taskEntity.getAssignee().equals(assignee))
                || (taskEntity.getAssignee() == null && assignee != null)) {
            
            CommandContextUtil.getTaskService().changeTaskAssignee(taskEntity, assignee);

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
    
    protected static void addAssigneeIdentityLinks(TaskEntity taskEntity) {
        if (taskEntity.getAssignee() != null && taskEntity.getScopeId() != null && VariableScopeType.CMMN.equals(taskEntity.getScopeType())) {
            CaseInstance caseInstance = CommandContextUtil.getCaseInstanceEntityManager().findById(taskEntity.getScopeId());
            IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstance, taskEntity.getAssignee(), null, IdentityLinkType.PARTICIPANT);
        }
    }
    
    protected static void addOwnerIdentityLink(TaskEntity taskEntity, String owner) {
        if (owner == null && taskEntity.getOwner() == null) {
            return;
        }

        if (owner != null && taskEntity.getScopeId() != null && VariableScopeType.CMMN.equals(taskEntity.getScopeType())) {
            CaseInstance caseInstance = CommandContextUtil.getCaseInstanceEntityManager().findById(taskEntity.getScopeId());
            IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstance, owner, null, IdentityLinkType.PARTICIPANT);
        }
    }
    
    public static void deleteHistoricTask(String taskId) {
        if (CommandContextUtil.getCmmnEngineConfiguration().getHistoryLevel() != HistoryLevel.NONE) {
            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
            HistoricTaskInstanceEntity historicTaskInstance = historicTaskService.getHistoricTask(taskId);
            if (historicTaskInstance != null) {
    
                List<HistoricTaskInstanceEntity> subTasks = historicTaskService.findHistoricTasksByParentTaskId(historicTaskInstance.getId());
                for (HistoricTaskInstance subTask : subTasks) {
                    deleteHistoricTask(subTask.getId());
                }
    
                CommandContextUtil.getHistoricVariableService().deleteHistoricVariableInstancesByTaskId(taskId);
                CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLinksByTaskId(taskId);
    
                historicTaskService.deleteHistoricTask(historicTaskInstance);
            }
        }
    }
    
}
