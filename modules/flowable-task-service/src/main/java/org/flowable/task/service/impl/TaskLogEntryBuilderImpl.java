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
package org.flowable.task.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.service.impl.util.CommandContextUtil;

/**
 * @author martin.grofcik
 */
public class TaskLogEntryBuilderImpl extends BaseTaskLogEntryBuilderImpl implements Command<Void> {

    public TaskLogEntryBuilderImpl(CommandExecutor commandExecutor, TaskInfo task) {
        super(commandExecutor, task);
    }

    public TaskLogEntryBuilderImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public void add() {
        this.commandExecutor.execute(this);
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (StringUtils.isEmpty(getTaskId())) {
            throw new FlowableIllegalArgumentException("Empty taskId is not allowed for TaskLogEntry");
        }
        if (StringUtils.isEmpty(getUserId())) {
            userId(Authentication.getAuthenticatedUserId());
        }
        if (getTimeStamp() == null) {
            timeStamp(CommandContextUtil.getTaskServiceConfiguration().getClock().getCurrentTime());
        }

        CommandContextUtil.getTaskServiceConfiguration(commandContext).getTaskService().createTaskLogEntry(this);
        return null;
    }

}
