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
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.task.IdentityLinkType;
import org.flowable.engine.task.Task;
import org.flowable.engine.task.TaskInfo;

/**
 * @author Joram Barrez
 */
public class SaveTaskCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected TaskEntity task;

    public SaveTaskCmd(Task task) {
        this.task = (TaskEntity) task;
    }

    public Void execute(CommandContext commandContext) {
        if (task == null) {
            throw new FlowableIllegalArgumentException("task is null");
        }

        if (task.getProcessDefinitionId() != null && Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.saveTask(task);
            return null;
        }

        if (task.getRevision() == 0) {
            commandContext.getTaskEntityManager().insert(task, null);

            if (commandContext.getEventDispatcher().isEnabled()) {
                commandContext.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task));
            }

        } else {

            TaskInfo originalTaskEntity = null;
            if (commandContext.getProcessEngineConfiguration().getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                originalTaskEntity = commandContext.getHistoricTaskInstanceEntityManager().findById(task.getId());
            }

            if (originalTaskEntity == null) {
                originalTaskEntity = commandContext.getTaskEntityManager().findById(task.getId());
            }

            String originalName = originalTaskEntity.getName();
            String originalAssignee = originalTaskEntity.getAssignee();
            String originalOwner = originalTaskEntity.getOwner();
            String originalDescription = originalTaskEntity.getDescription();
            Date originalDueDate = originalTaskEntity.getDueDate();
            int originalPriority = originalTaskEntity.getPriority();
            String originalCategory = originalTaskEntity.getCategory();
            String originalFormKey = originalTaskEntity.getFormKey();
            String originalParentTaskId = originalTaskEntity.getParentTaskId();
            String originalTaskDefinitionKey = originalTaskEntity.getTaskDefinitionKey();

            // Only update history if history is enabled
            if (commandContext.getProcessEngineConfiguration().getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {

                if (!StringUtils.equals(originalName, task.getName())) {
                    commandContext.getHistoryManager().recordTaskNameChange(task.getId(), task.getName());
                }
                if (!StringUtils.equals(originalDescription, task.getDescription())) {
                    commandContext.getHistoryManager().recordTaskDescriptionChange(task.getId(), task.getDescription());
                }
                if ((originalDueDate == null && task.getDueDate() != null)
                        || (originalDueDate != null && task.getDueDate() == null)
                        || (originalDueDate != null && !originalDueDate.equals(task.getDueDate()))) {
                    commandContext.getHistoryManager().recordTaskDueDateChange(task.getId(), task.getDueDate());
                }
                if (originalPriority != task.getPriority()) {
                    commandContext.getHistoryManager().recordTaskPriorityChange(task.getId(), task.getPriority());
                }
                if (!StringUtils.equals(originalCategory, task.getCategory())) {
                    commandContext.getHistoryManager().recordTaskCategoryChange(task.getId(), task.getCategory());
                }
                if (!StringUtils.equals(originalFormKey, task.getFormKey())) {
                    commandContext.getHistoryManager().recordTaskFormKeyChange(task.getId(), task.getFormKey());
                }
                if (!StringUtils.equals(originalParentTaskId, task.getParentTaskId())) {
                    commandContext.getHistoryManager().recordTaskParentTaskIdChange(task.getId(), task.getParentTaskId());
                }
                if (!StringUtils.equals(originalTaskDefinitionKey, task.getTaskDefinitionKey())) {
                    commandContext.getHistoryManager().recordTaskDefinitionKeyChange(task.getId(), task.getTaskDefinitionKey());
                }

            }

            if (!StringUtils.equals(originalOwner, task.getOwner())) {
                if (task.getProcessInstanceId() != null) {
                    commandContext.getIdentityLinkEntityManager().involveUser(task.getProcessInstance(), task.getOwner(), IdentityLinkType.PARTICIPANT);
                }
                commandContext.getHistoryManager().recordTaskOwnerChange(task.getId(), task.getOwner());
            }
            if (!StringUtils.equals(originalAssignee, task.getAssignee())) {
                if (task.getProcessInstanceId() != null) {
                    commandContext.getIdentityLinkEntityManager().involveUser(task.getProcessInstance(), task.getAssignee(), IdentityLinkType.PARTICIPANT);
                }
                commandContext.getHistoryManager().recordTaskAssigneeChange(task, task.getAssignee());

                commandContext.getProcessEngineConfiguration().getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_ASSIGNMENT);
                
                if (commandContext.getEventDispatcher().isEnabled()) {
                    commandContext.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, task));
                }

            }

            commandContext.getTaskEntityManager().update(task);
        }

        return null;
    }

}
