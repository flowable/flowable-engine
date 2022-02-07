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

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;

/**
 * @author Tijs Rademakers
 */
public class DeleteHistoricTaskInstanceCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected String taskId;

    public DeleteHistoricTaskInstanceCmd(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public Object execute(CommandContext commandContext) {

        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is null");
        }

        // Check if task is completed
        HistoricTaskInstanceEntity historicTaskInstance = CommandContextUtil.getHistoricTaskService().getHistoricTask(taskId);

        if (historicTaskInstance == null) {
            throw new FlowableObjectNotFoundException("No historic task instance found with id: " + taskId, HistoricTaskInstance.class);
        }
        if (historicTaskInstance.getEndTime() == null) {
            throw new FlowableException("task does not have an endTime, cannot delete historic task instance: " + taskId);
        }

        CommandContextUtil.getCmmnHistoryManager(commandContext).recordHistoricTaskDeleted(historicTaskInstance);
        
        return null;
    }

}
