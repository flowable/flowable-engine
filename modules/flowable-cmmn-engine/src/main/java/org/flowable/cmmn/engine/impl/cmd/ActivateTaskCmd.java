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

import java.util.Date;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.task.api.Task;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class ActivateTaskCmd implements Command<Void> {

    protected String taskId;
    protected String userId;

    public ActivateTaskCmd(String taskId, String userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is null");
        }

        TaskEntity task = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(taskId);

        if (task == null) {
            throw new FlowableObjectNotFoundException("Cannot find task with id " + taskId, Task.class);
        }

        if (task.isDeleted()) {
            throw new FlowableException("Task " + taskId + " is already deleted");
        }

        if (!task.isSuspended()) {
            throw new FlowableException("Task " + taskId + " is not suspended, so can't be activated");
        }
        
        Clock clock = cmmnEngineConfiguration.getClock();
        Date updateTime = clock.getCurrentTime();
        task.setSuspendedTime(null);
        task.setSuspendedBy(null);
        if (task.getInProgressStartTime() != null) {
            task.setState(Task.IN_PROGRESS);
        } else if (task.getClaimTime() != null) {
            task.setState(Task.CLAIMED);
        } else {
            task.setState(Task.CREATED);
        }
        task.setSuspensionState(SuspensionState.ACTIVE.getStateCode());
        
        HistoricTaskService historicTaskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
        historicTaskService.recordTaskInfoChange(task, updateTime, cmmnEngineConfiguration);

        return null;
    }

}
