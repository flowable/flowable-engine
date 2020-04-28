package org.flowable.common.engine.api.variable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ievgenii Bespal
 */
public class ScopedVariableContainerImpl implements ScopedVariableContainer {
    private Map<String, Object> variables;
    private Map<String, Object> variablesLocal;
    private final boolean isTransient;

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
