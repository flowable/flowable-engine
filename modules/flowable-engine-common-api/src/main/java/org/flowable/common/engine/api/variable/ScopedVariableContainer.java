package org.flowable.common.engine.api.variable;

import java.util.Map;

/**
 * @author Ievgenii Bespal
 */
public interface ScopedVariableContainer {
    boolean isTransient();

    Object getVariable(String variableName, boolean isVariableLocal);

    Map<String, Object> getVariables(boolean isVariablesLocal);

    boolean hasVariable(String variableName, boolean isVariableLocal);

    boolean hasVariables(boolean isVariablesLocal);

    void setVariable(String variableName, Object variableValue, boolean isVariableLocal);

    void setVariables(Map<String, Object> variables, boolean isVariableLocal);
}
