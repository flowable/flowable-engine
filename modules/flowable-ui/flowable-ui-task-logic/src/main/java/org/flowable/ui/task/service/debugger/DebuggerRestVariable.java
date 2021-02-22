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
package org.flowable.ui.task.service.debugger;

import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance; 

/**
 * @author martin.grofcik
 */
public class DebuggerRestVariable {
    protected String name;
    protected String type;
    protected Object value;
    protected String executionId;
    protected String taskId;
    protected String processId;

    public DebuggerRestVariable(HistoricVariableInstance historicVariableInstance) {
        type = historicVariableInstance.getVariableTypeName();
        name = historicVariableInstance.getVariableName();
        value = historicVariableInstance.getValue();
        executionId = historicVariableInstance.getProcessInstanceId();
        processId = historicVariableInstance.getProcessInstanceId();
        taskId = historicVariableInstance.getTaskId();
    }

    public DebuggerRestVariable(VariableInstance variableInstance) {
        type = variableInstance.getTypeName();
        name = variableInstance.getName();
        value = variableInstance.getValue();
        executionId = variableInstance.getExecutionId();
        processId = variableInstance.getProcessInstanceId();
        taskId = variableInstance.getTaskId();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getProcessId() {
        return processId;
    }
}
