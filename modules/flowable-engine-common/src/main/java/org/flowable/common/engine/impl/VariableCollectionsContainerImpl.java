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
package org.flowable.common.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.variable.VariableCollectionsContainer;

/**
 * @author Ievgenii Bespal
 */
public class VariableCollectionsContainerImpl implements VariableCollectionsContainer {
    protected Map<String, Object> variables;
    protected Map<String, Object> variablesLocal;
    protected Map<String, Object> transientVariables;
    protected Map<String, Object> transientLocalVariables;

    public VariableCollectionsContainerImpl() {
        this.variables = new HashMap<>();
        this.variablesLocal = new HashMap<>();
        this.transientVariables = new HashMap<>();
        this.transientLocalVariables = new HashMap<>();
    }

    @Override
    public Object getVariable(String variableName) {
        return  this.variables.get(variableName);
    }

    @Override
    public Object getVariableLocal(String localVariableName) {
        return  this.variablesLocal.get(localVariableName);
    }

    @Override
    public Object getTransientVariable(String transientVariableName) {
        return this.transientVariables.get(transientVariableName);
    }

    @Override
    public Object getTransientLocalVariable(String transientLocalVariableName) {
        return transientLocalVariables.get(transientLocalVariableName);
    }

    @Override
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    @Override
    public Map<String, Object> getVariablesLocal() {
        return this.variablesLocal;
    }

    @Override
    public Map<String, Object> getTransientVariables() {
        return this.transientVariables;
    }

    @Override
    public Map<String, Object> getTransientLocalVariables() {
        return this.transientLocalVariables;
    }

    @Override
    public Map<String, Object> getAllVariables() {
        Map<String, Object> variables = null;

        if (hasAnyVariables()) {
            variables = new HashMap<>();
            if (hasVariables()) {
                variables.putAll(getVariables());
            }
            if (hasLocalVariables()) {
                variables.putAll(getVariablesLocal());
            }
        }
        return variables;
    }

    @Override
    public Map<String, Object> getAllTransientVariables() {
        Map<String, Object> variables = null;

        if (hasAnyTransientVariables()) {
            variables = new HashMap<>();
            if (hasTransientVariables()) {
                variables.putAll(getTransientVariables());
            }
            if (hasTransientLocalVariables()) {
                variables.putAll(getTransientLocalVariables());
            }
        }
        return variables;
    }

    @Override
    public boolean hasVariables() {
        return !this.variables.isEmpty();
    }

    @Override
    public boolean hasLocalVariables() {
        return !this.variablesLocal.isEmpty();
    }

    @Override
    public boolean hasTransientVariables() {
        return !this.transientVariables.isEmpty();
    }

    @Override
    public boolean hasTransientLocalVariables() {
        return !this.transientLocalVariables.isEmpty();
    }

    @Override
    public boolean hasAnyVariables() {
        return hasVariables() || hasLocalVariables();
    }

    @Override
    public boolean hasAnyTransientVariables() {
        return hasTransientVariables() || hasTransientLocalVariables();
    }

    @Override
    public boolean hasVariable(String variableName) {
        return this.variables.containsValue(variableName);
    }

    @Override
    public boolean hasVariableLocal(String localVariableName) {
        return this.variablesLocal.containsValue(localVariableName);
    }

    @Override
    public boolean hasTransientVariable(String transientVariableName) {
        return this.transientVariables.containsValue(transientVariableName);
    }

    @Override
    public boolean hasTransientLocalVariable(String transientLocalVariableName) {
        return this.transientLocalVariables.containsValue(transientLocalVariableName);
    }

    @Override
    public void setVariable(String variableName, Object variableValue) {
        this.variables.put(variableName, variableValue);
    }

    @Override
    public void setVariableLocal(String variableName, Object variableValue) {
        this.variablesLocal.put(variableName, variableValue);
    }

    @Override
    public void setTransientVariable(String variableName, Object variableValue) {
        this.transientVariables.put(variableName, variableValue);
    }

    @Override
    public void setTransientVariableLocal(String variableName, Object variableValue) {
        this.transientLocalVariables.put(variableName, variableValue);
    }

    @Override
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    @Override
    public void setLocalVariables(Map<String, Object> localVariables) {
        this.variablesLocal = localVariables;
    }

    @Override
    public void setTransientVariables(Map<String, Object> transientVariables) {
        this.transientVariables = transientVariables;
    }

    @Override
    public void setTransientLocalVariables(Map<String, Object> transientLocalVariables) {
        this.transientLocalVariables = transientLocalVariables;
    }
}
