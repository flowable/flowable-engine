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
