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

import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.form.FormHandlerHelper;
import org.flowable.engine.impl.form.TaskFormHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class SubmitTaskFormCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;

    protected String taskId;
    protected Map<String, String> properties;
    protected boolean completeTask;

    public SubmitTaskFormCmd(String taskId, Map<String, String> properties, boolean completeTask) {
        super(taskId);
        this.taskId = taskId;
        this.properties = properties;
        this.completeTask = completeTask;
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {

        // Backwards compatibility
        if (task.getProcessDefinitionId() != null) {
            if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                compatibilityHandler.submitTaskFormData(taskId, properties, completeTask);
                return null;
            }
        }

        ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
        CommandContextUtil.getHistoryManager(commandContext).recordFormPropertiesSubmitted(executionEntity, properties, taskId);

        FormHandlerHelper formHandlerHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getFormHandlerHelper();
        TaskFormHandler taskFormHandler = formHandlerHelper.getTaskFormHandlder(task);

        if (taskFormHandler != null) {
            taskFormHandler.submitFormProperties(properties, executionEntity);

            if (completeTask) {
                TaskHelper.completeTask(task, null, null, false, commandContext);
            }
        }

        return null;
    }

    @Override
    protected String getSuspendedTaskException() {
        return "Cannot submit a form to a suspended task";
    }

}
