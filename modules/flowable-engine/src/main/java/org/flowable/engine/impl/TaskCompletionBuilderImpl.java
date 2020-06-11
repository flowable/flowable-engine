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

import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cmd.CompleteTaskCmd;
import org.flowable.engine.impl.cmd.CompleteTaskWithFormCmd;
import org.flowable.task.api.TaskCompletionBuilder;
import org.flowable.common.engine.api.variable.ScopedVariableContainer;
import org.flowable.common.engine.api.variable.ScopedVariableContainerImpl;


/**
 * @author Ievgenii Bespal
 */
public class TaskCompletionBuilderImpl implements TaskCompletionBuilder {
    protected final ScopedVariableContainer scopedVariableContainer;
    protected CommandExecutor commandExecutor;
    protected String taskId;
    protected String formDefinitionId;
    protected String outcome;

    protected TaskCompletionBuilderImpl() {
        this.scopedVariableContainer = new ScopedVariableContainerImpl();
    }

    public TaskCompletionBuilderImpl(CommandExecutor commandExecutor) {
        this();
        this.commandExecutor = commandExecutor;
    }

    @Override
    public TaskCompletionBuilder variables(Map<String, Object> variables) {
        this.scopedVariableContainer.setVariables(variables);
        return this;
    }

    @Override
    public TaskCompletionBuilder variablesLocal(Map<String, Object> variablesLocal) {
        this.scopedVariableContainer.setLocalVariables(variablesLocal);
        return this;
    }

    @Override
    public TaskCompletionBuilder transientVariables(Map<String, Object> transientVariables) {
        this.scopedVariableContainer.setTransientVariables(transientVariables);
        return this;
    }

    @Override
    public TaskCompletionBuilder transientVariablesLocal(Map<String, Object> transientVariablesLocal) {
        this.scopedVariableContainer.setTransientLocalVariables(transientVariablesLocal);
        return this;
    }

    @Override
    public TaskCompletionBuilder variable(String variableName, Object variableValue) {
        this.scopedVariableContainer.setVariable(variableName, variableValue);
        return this;
    }

    @Override
    public TaskCompletionBuilder variableLocal(String variableName, Object variableValue) {
        this.scopedVariableContainer.setVariableLocal(variableName, variableValue);
        return this;
    }

    @Override
    public TaskCompletionBuilder transientVariable(String variableName, Object variableValue) {
        this.scopedVariableContainer.setTransientVariable(variableName, variableValue);
        return this;
    }

    @Override
    public TaskCompletionBuilder transientVariableLocal(String variableName, Object variableValue) {
        this.scopedVariableContainer.setTransientVariableLocal(variableName, variableValue);
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
        this.commandExecutor.execute(new CompleteTaskCmd(this.taskId, this.scopedVariableContainer));
    }

    protected void completeTaskWithForm() {
        this.commandExecutor.execute(new CompleteTaskWithFormCmd(this.taskId, formDefinitionId, outcome, this.scopedVariableContainer));
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
