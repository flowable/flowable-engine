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
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
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
            CommandContextUtil.getTaskEntityManager(commandContext).insert(task, null, true);

            if (CommandContextUtil.getEventDispatcher().isEnabled()) {
                CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task));
            }

        } else {
            
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

            TaskInfo originalTaskEntity = CommandContextUtil.getTaskEntityManager(commandContext).findById(task.getId());
            
            if (originalTaskEntity == null && processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                originalTaskEntity = CommandContextUtil.getHistoricTaskInstanceEntityManager(commandContext).findById(task.getId());
            }
            
            String originalAssignee = originalTaskEntity.getAssignee();
            
            CommandContextUtil.getHistoryManager(commandContext).recordTaskInfoChange(task);
            
            if (!StringUtils.equals(originalAssignee, task.getAssignee())) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_ASSIGNMENT);
                
                if (CommandContextUtil.getEventDispatcher().isEnabled()) {
                    CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, task));
                }

            }
            
            CommandContextUtil.getTaskEntityManager(commandContext).update(task);
        }

        return null;
    }

}
