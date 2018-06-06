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
package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.service.event.impl.FlowableTaskEventBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
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

        if (task.getRevision() == 0) {
            TaskHelper.insertTask(task, true);

            if (CommandContextUtil.getEventDispatcher() != null && CommandContextUtil.getEventDispatcher().isEnabled()) {
                CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task));
            }

        } else {
            
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);

            TaskInfo originalTaskEntity = CommandContextUtil.getTaskService().getTask(task.getId());
            
            if (originalTaskEntity == null && cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                originalTaskEntity = CommandContextUtil.getHistoricTaskService().getHistoricTask(task.getId());
            }
            
            String originalAssignee = originalTaskEntity.getAssignee();
            
            CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskInfoChange(task);
            CommandContextUtil.getTaskService().updateTask(task, true);
            
            if (!StringUtils.equals(originalAssignee, task.getAssignee())) {
                if (CommandContextUtil.getEventDispatcher() != null && CommandContextUtil.getEventDispatcher().isEnabled()) {
                    CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_ASSIGNED, task));
                }

            }
        }

        return null;
    }

}
