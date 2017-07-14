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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.impl.form.TaskFormHandler;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.FormHandlerUtil;
import org.flowable.engine.task.Task;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class GetTaskFormCmd implements Command<TaskFormData>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String taskId;

    public GetTaskFormCmd(String taskId) {
        this.taskId = taskId;
    }

    public TaskFormData execute(CommandContext commandContext) {
        TaskEntity task = CommandContextUtil.getTaskEntityManager(commandContext).findById(taskId);
        if (task == null) {
            throw new FlowableObjectNotFoundException("No task found for taskId '" + taskId + "'", Task.class);
        }

        TaskFormHandler taskFormHandler = FormHandlerUtil.getTaskFormHandlder(task);
        if (taskFormHandler == null) {
            throw new FlowableException("No taskFormHandler specified for task '" + taskId + "'");
        }

        return taskFormHandler.createTaskForm(task);
    }

}
