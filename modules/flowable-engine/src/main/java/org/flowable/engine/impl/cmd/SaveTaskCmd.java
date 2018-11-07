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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.event.impl.FlowableTaskEventBuilder;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class SaveTaskCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected TaskEntity task;

    public SaveTaskCmd(Task task) {
        this.task = (TaskEntity) task;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (task == null) {
            throw new FlowableIllegalArgumentException("task is null");
        }

        if (task.getProcessDefinitionId() != null && Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.saveTask(task);
            return null;
        }
        
        TaskService taskService = CommandContextUtil.getTaskService(commandContext);

        if (task.getRevision() == 0) {
            TaskHelper.insertTask(task, null, true);

            FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher(commandContext);
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task));
            }
            
            handleSubTaskCount(taskService, null);

        } else {

            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

            TaskInfo originalTaskEntity = taskService.getTask(task.getId());
            
            if (originalTaskEntity == null && processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                originalTaskEntity = CommandContextUtil.getHistoricTaskService().getHistoricTask(task.getId());
            }

            CommandContextUtil.getHistoryManager(commandContext).recordTaskInfoChange(task);
            taskService.updateTask(task, true);

            // Special care needed to detect the assignee task has changed
            if (!StringUtils.equals(originalTaskEntity.getAssignee(), task.getAssignee())) {
                handleAssigneeChange(commandContext, processEngineConfiguration);
            }
            
            // Special care needed to detect the parent task has changed
            if (!StringUtils.equals(originalTaskEntity.getParentTaskId(), task.getParentTaskId())) {
                handleSubTaskCount(taskService, originalTaskEntity);
            }
            
        }

        return null;
    }

    protected void handleAssigneeChange(CommandContext commandContext,
            ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_ASSIGNMENT);

        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher(commandContext);
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, task));
        }
    }

    protected void handleSubTaskCount(TaskService taskService, TaskInfo originalTaskEntity) {
        if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(task)) {
            
            // Parent task is set, none was set before or it's a new subtask
            if (task.getParentTaskId() != null && (originalTaskEntity == null || originalTaskEntity.getParentTaskId() == null)) {
                TaskEntity parentTaskEntity = taskService.getTask(task.getParentTaskId());
                if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(parentTaskEntity)) {
                    CountingTaskEntity countingParentTaskEntity = (CountingTaskEntity) parentTaskEntity;
                    countingParentTaskEntity.setSubTaskCount(countingParentTaskEntity.getSubTaskCount() + 1);
                    parentTaskEntity.forceUpdate();
                }
                
            // Parent task removed and was set before   
            } else if (task.getParentTaskId() == null && originalTaskEntity != null && originalTaskEntity.getParentTaskId() != null) {
                TaskEntity parentTaskEntity = taskService.getTask(originalTaskEntity.getParentTaskId());
                if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(parentTaskEntity)) {
                    CountingTaskEntity countingParentTaskEntity = (CountingTaskEntity) parentTaskEntity;
                    countingParentTaskEntity.setSubTaskCount(countingParentTaskEntity.getSubTaskCount() - 1);
                    parentTaskEntity.forceUpdate();
                }
              
            // Parent task was changed    
            } else if (task.getParentTaskId() != null && originalTaskEntity.getParentTaskId() != null 
                    && !task.getParentTaskId().equals(originalTaskEntity.getParentTaskId())) {
                
                TaskEntity originalParentTaskEntity = taskService.getTask(originalTaskEntity.getParentTaskId());
                if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(originalParentTaskEntity)) {
                    CountingTaskEntity countingOriginalParentTaskEntity = (CountingTaskEntity) originalParentTaskEntity;
                    countingOriginalParentTaskEntity.setSubTaskCount(countingOriginalParentTaskEntity.getSubTaskCount() - 1);
                    originalParentTaskEntity.forceUpdate();
                }
                
                TaskEntity parentTaskEntity = taskService.getTask(task.getParentTaskId());
                if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(parentTaskEntity)) {
                    CountingTaskEntity countingParentTaskEntity = (CountingTaskEntity) parentTaskEntity;
                    countingParentTaskEntity.setSubTaskCount(countingParentTaskEntity.getSubTaskCount() + 1);
                    parentTaskEntity.forceUpdate();
                }
                
            }
            
        }
    }

}
