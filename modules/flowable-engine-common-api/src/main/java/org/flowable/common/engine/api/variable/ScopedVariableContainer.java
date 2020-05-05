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

import java.util.Map;

/**
 * Class which provides methods to operate with variables.
 *
 * @author Ievgenii Bespal
 */
public interface ScopedVariableContainer {
    /**
     * Checks whether or not the defined variables is transient.
     *
     * @return whether or not the defined variables is transient.
     */
    boolean isTransient();

    /**
     * Checks whether or not there is a variable defined with the given name.
     *
     * @param variableName
     *          name of variable.
     * @param isVariableLocal
     *          define scope of variable.
     * @return the variable or null if the variable is undefined.
     */
    Object getVariable(String variableName, boolean isVariableLocal);

    /**
     * Get all variables.
     *
     * @param isVariablesLocal
     *          define scope of variables.
     * @return the variable instances or an empty map if no such variables are found.
     */
    Map<String, Object> getVariables(boolean isVariablesLocal);

    /**
     * Checks whether or not the task has a variable defined with the given name.
     *
     * @param variableName
     *          name of variable.
     * @param isVariableLocal
     *          define scope of variable.
     * @return whether or not exists the defined variable.
     */
    boolean hasVariable(String variableName, boolean isVariableLocal);

    /**
     * Checks whether or not the list is empty.
     *
     * @param isVariablesLocal
     *          define scope of variables.
     * @return whether or not the list is empty.
     */
    boolean hasVariables(boolean isVariablesLocal);

    /**
     * Set variable. If the variable is not already existing, it will be created.
     *
     * @param variableName
     *          name of variable.
     * @param variableValue
     *          value of variable.
     * @param isVariableLocal
     *          define scope of variable.
     */
    void setVariable(String variableName, Object variableValue, boolean isVariableLocal);

    /**
     * Set variables. If the variables is not already existing, it will be created.
     *
     * @param variables
     *          variables.
     * @param isVariableLocal
     *          define scope of variable.
     */
    void setVariables(Map<String, Object> variables, boolean isVariableLocal);
}
