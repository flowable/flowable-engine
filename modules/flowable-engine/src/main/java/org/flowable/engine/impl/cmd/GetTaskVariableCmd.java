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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class GetTaskVariableCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String taskId;
    protected String variableName;
    protected boolean isLocal;

    public GetTaskVariableCmd(String taskId, String variableName, boolean isLocal) {
        this.taskId = taskId;
        this.variableName = variableName;
        this.isLocal = isLocal;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is null");
        }
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        TaskEntity task = processEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(taskId);

        if (task == null) {
            throw new FlowableObjectNotFoundException("task " + taskId + " doesn't exist", Task.class);
        }

        Object value;

        if (isLocal) {
            value = task.getVariableLocal(variableName, false);
        } else {
            value = task.getVariable(variableName, false);
        }

        return value;
    }
}
