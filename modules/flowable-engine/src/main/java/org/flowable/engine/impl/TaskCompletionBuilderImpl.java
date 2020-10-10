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
package org.flowable.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cmd.CompleteTaskCmd;
import org.flowable.engine.impl.cmd.CompleteTaskWithFormCmd;
import org.flowable.task.api.TaskCompletionBuilder;


/**
 * @author Ievgenii Bespal
 */
public class TaskCompletionBuilderImpl implements TaskCompletionBuilder {

    protected CommandExecutor commandExecutor;
    protected String taskId;
    protected String formDefinitionId;
    protected String outcome;

    protected Map<String, Object> variables;
    protected Map<String, Object> variablesLocal;
    protected Map<String, Object> transientVariables;
    protected Map<String, Object> transientVariablesLocal;

    public TaskCompletionBuilderImpl(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public TaskCompletionBuilder variables(Map<String, Object> variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public TaskCompletionBuilder variablesLocal(Map<String, Object> variablesLocal) {
        this.variablesLocal = variablesLocal;
        return this;
    }

    @Override
    public TaskCompletionBuilder transientVariables(Map<String, Object> transientVariables) {
        this.transientVariables = transientVariables;
        return this;
    }

    @Override
    public TaskCompletionBuilder transientVariablesLocal(Map<String, Object> transientVariablesLocal) {
        this.transientVariablesLocal = transientVariablesLocal;
        return this;
    }

    @Override
    public TaskCompletionBuilder variable(String variableName, Object variableValue) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(variableName, variableValue);
        return this;
    }

    @Override
    public TaskCompletionBuilder variableLocal(String variableName, Object variableValue) {
        if (this.variablesLocal == null) {
            this.variablesLocal = new HashMap<>();
        }
        this.variablesLocal.put(variableName, variableValue);
        return this;
    }

    @Override
    public TaskCompletionBuilder transientVariable(String variableName, Object variableValue) {
        if (this.transientVariables == null) {
            this.transientVariables = new HashMap<>();
        }
        this.transientVariables.put(variableName, variableValue);
        return this;
    }

    @Override
    public TaskCompletionBuilder transientVariableLocal(String variableName, Object variableValue) {
        if (this.transientVariablesLocal == null) {
            this.transientVariablesLocal = new HashMap<>();
        }
        this.transientVariablesLocal.put(variableName, variableValue);
        return this;
    }

    @Override
    public TaskCompletionBuilder taskId(String id) {
        this.taskId = id;
        return this;
    }

    @Override
    public TaskCompletionBuilder formDefinitionId(String formDefinitionId) {
        this.formDefinitionId = formDefinitionId;
        return this;
    }

    @Override
    public TaskCompletionBuilder outcome(String outcome) {
        this.outcome = outcome;
        return this;
    }

    protected void completeTask() {
        this.commandExecutor.execute(new CompleteTaskCmd(this.taskId, variables, variablesLocal, transientVariables, transientVariablesLocal));
    }

    protected void completeTaskWithForm() {
        this.commandExecutor.execute(new CompleteTaskWithFormCmd(this.taskId, formDefinitionId, outcome,
            variables, variablesLocal, transientVariables, transientVariablesLocal));
    }

    @Override
    public void complete() {
        if (this.formDefinitionId != null) {
            completeTaskWithForm();
        } else {
            completeTask();
        }
    }
}
