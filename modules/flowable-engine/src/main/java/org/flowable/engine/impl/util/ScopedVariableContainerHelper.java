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
package org.flowable.engine.impl.util;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.variable.ScopedVariableContainer;
import org.flowable.common.engine.api.variable.ScopedVariableContainerImpl;

/**
 * @author Ievgenii Bespal
 */
public class ScopedVariableContainerHelper {
    protected final ScopedVariableContainer variables;
    protected final ScopedVariableContainer transientVariables;

    public ScopedVariableContainerHelper() {
        this.variables = new ScopedVariableContainerImpl(false);
        this.transientVariables = new ScopedVariableContainerImpl(true);
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables.setVariables(variables, false);
    }

    public void setVariablesLocal(Map<String, Object> variablesLocal) {
        this.variables.setVariables(variablesLocal, true);
    }

    public void setTransientVariables(Map<String, Object> transientVariables) {
        this.transientVariables.setVariables(transientVariables, false);
    }

    public void setTransientVariablesLocal(Map<String, Object> transientVariablesLocal) {
        this.transientVariables.setVariables(transientVariablesLocal, true);
    }

    public void setVariable(String variableName, Object variableValue) {
        this.variables.setVariable(variableName, variableValue, false);
    }

    public void setVariableLocal(String variableName, Object variableValue) {
        this.variables.setVariable(variableName, variableValue, true);
    }

    public void setTransientVariable(String variableName, Object variableValue) {
        this.transientVariables.setVariable(variableName, variableValue, false);
    }

    public void setTransientVariableLocal(String variableName, Object variableValue) {
        this.transientVariables.setVariable(variableName, variableValue, true);
    }

    public Map<String, Object> getVariables() {
        return this.variables.getVariables(false);
    }

    public Map<String, Object> getVariablesLocal() {
        return this.variables.getVariables(true);
    }

    public Map<String, Object> getTransientVariables() {
        return this.transientVariables.getVariables(false);
    }

    public Map<String, Object> getTransientVariablesLocal() {
        return this.transientVariables.getVariables(true);
    }

    public Map<String, Object> getAllVariables() {
        Map<String, Object> variables = null;

        if (hasAnyVariables()) {
            variables = new HashMap<>();
            if (hasVariables()) {
                variables.putAll(getVariables());
            }
            if (hasVariablesLocal()) {
                variables.putAll(getVariablesLocal());
            }
        }
        return variables;
    }

    public Map<String, Object> getAllTransientVariables() {
        Map<String, Object> transientVariables = null;

        if (hasAnyTransientVariables()) {
            transientVariables = new HashMap<>();
            if (hasTransientVariables()) {
                transientVariables.putAll(getTransientVariables());
            }
            if (hasTransientVariablesLocal()) {
                transientVariables.putAll(getTransientVariablesLocal());
            }
        }
        return transientVariables;
    }


    public Object getVariable(String variableName) {
        return this.variables.getVariable(variableName, false);
    }

    public Object getVariableLocal(String variableName) {
        return this.variables.getVariable(variableName, true);

    }

    public Object getTransientVariable(String variableName) {
        return this.transientVariables.getVariable(variableName, false);

    }

    public Object getTransientVariableLocal(String variableName) {
        return this.transientVariables.getVariable(variableName, true);
    }

    public boolean hasVariable(String variableName) {
        return this.variables.hasVariable(variableName, false);
    }

    public boolean hasVariableLocal(String variableName) {
        return this.variables.hasVariable(variableName, true);

    }

    public boolean hasTransientVariable(String variableName) {
        return this.transientVariables.hasVariable(variableName, false);

    }

    public boolean hasTransientVariableLocal(String variableName) {
        return this.transientVariables.hasVariable(variableName, true);
    }

    public boolean hasVariables() {
        return this.variables.hasVariables(false);
    }

    public boolean hasVariablesLocal() {
        return this.variables.hasVariables(true);
    }

    public boolean hasTransientVariables() {
        return this.transientVariables.hasVariables(false);
    }

    public boolean hasTransientVariablesLocal() {
        return this.transientVariables.hasVariables(true);
    }

    public boolean hasAnyTransientVariables() {
        return hasTransientVariables() || hasTransientVariablesLocal();
    }

    public boolean hasAnyVariables() {
        return hasVariables() || hasVariablesLocal();
    }
}
