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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.TaskLogEntryBuilder;

/**
 * @author martin.grofcik
 */
public class AddTaskLogEntryCmd implements Command<Void> {

    protected TaskLogEntryBuilder taskLogEntryBuilder;

    public AddTaskLogEntryCmd(TaskLogEntryBuilder taskLogEntryBuilder) {
        this.taskLogEntryBuilder = taskLogEntryBuilder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (StringUtils.isEmpty(taskLogEntryBuilder.getTaskId())) {
            throw new FlowableIllegalArgumentException("Empty taskId is not allowed for TaskLogEntry");
        }
        if (StringUtils.isEmpty(taskLogEntryBuilder.getUserId())) {
            taskLogEntryBuilder.userId(Authentication.getAuthenticatedUserId());
        }
        if (taskLogEntryBuilder.getTimeStamp() == null) {
            taskLogEntryBuilder.timeStamp(CommandContextUtil.getTaskServiceConfiguration().getClock().getCurrentTime());
        }

        CommandContextUtil.getTaskService(commandContext).createTaskLogEntry(this.taskLogEntryBuilder);
        return null;
    }
}
