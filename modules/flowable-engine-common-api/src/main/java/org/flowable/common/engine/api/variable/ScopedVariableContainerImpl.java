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
package org.flowable.common.engine.api.variable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ievgenii Bespal
 */
public class ScopedVariableContainerImpl implements ScopedVariableContainer {
    protected Map<String, Object> variables;
    protected Map<String, Object> variablesLocal;
    protected final boolean isTransient;

    public ScopedVariableContainerImpl(boolean isTransient) {
        this.isTransient = isTransient;
        this.variables = new HashMap<>();
        this.variablesLocal = new HashMap<>();
    }

    @Override
    public boolean isTransient() {
        return this.isTransient;
    }

    @Override
    public Object getVariable(String variableName, boolean isVariableLocal) {
        return isVariableLocal ? this.variablesLocal.get(variableName) : this.variables.get(variableName);
    }

    @Override
    public Map<String, Object> getVariables(boolean isVariablesLocal) {
        return isVariablesLocal ? this.variablesLocal : this.variables;
    }

    @Override
    public boolean hasVariable(String variableName, boolean isVariableLocal) {
        return isVariableLocal ? this.variablesLocal.containsValue(variableName) : this.variables.containsValue(variableName);
    }

    @Override
    public boolean hasVariables(boolean isVariablesLocal) {
        return isVariablesLocal ? !this.variablesLocal.isEmpty() : !this.variables.isEmpty();
    }

    @Override
    public void setVariable(String variableName, Object variableValue, boolean isVariableLocal) {
        if (isVariableLocal) {
            this.variablesLocal.put(variableName, variableValue);
        } else {
            this.variables.put(variableName, variableValue);
        }
    }

    @Override
    public void setVariables(Map<String, Object> variables, boolean isVariablesLocal) {
        if (isVariablesLocal) {
            this.variablesLocal = variables;
        } else {
            this.variables = variables;
        }
    }
}
