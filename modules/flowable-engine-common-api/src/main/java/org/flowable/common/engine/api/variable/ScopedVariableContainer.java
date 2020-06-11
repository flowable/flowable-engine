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
     * Checks whether or not there is a variable defined with the given name.
     *
     * @param variableName
     *          name of variable.
     * @return the variable or null if the variable is undefined.
     */
    Object getVariable(String variableName);

    /**
     * Checks whether or not there is a local variable defined with the given name.
     *
     * @param localVariableName
     *          name of local variable.
     * @return the variable or null if the variable is undefined.
     */
    Object getVariableLocal(String localVariableName);

    /**
     * Checks whether or not there is a transient variable defined with the given name.
     *
     * @param transientVariableName
     *          name of variable.
     * @return the variable or null if the transient variable is undefined.
     */
    Object getTransientVariable(String transientVariableName);

    /**
     * Checks whether or not there is a transient local variable defined with the given name.
     *
     * @param transientLocalVariableName
     *          name of local variable.
     * @return the variable or null if the transient variable is undefined.
     */
    Object getTransientLocalVariable(String transientLocalVariableName);

    /**
     * Get all variables.
     *
     * @return the variable instances or an empty map if no such variables are found.
     */
    Map<String, Object> getVariables();

    /**
     * Get all local variables.
     *
     * @return the local variable instances or an empty map if no such local variables are found.
     */
    Map<String, Object> getVariablesLocal();

    /**
     * Get all transient variables.
     *
     * @return the variable instances or an empty map if no such transient variables are found.
     */
    Map<String, Object> getTransientVariables();

    /**
     * Get all transient local variables.
     *
     * @return the local variable instances or an empty map if no such transient local variables are found.
     */
    Map<String, Object> getTransientLocalVariables();

    /**
     * Get all variables including local.
     *
     * @return the variable instances or an empty map if no such variables are found.
     */
    Map<String, Object> getAllVariables();

    /**
     * Get all transient variables including local.
     *
     * @return the variable instances or an empty map if no such transient variables are found.
     */
    Map<String, Object> getAllTransientVariables();

    /**
     * Checks whether or not the list is empty.
     *
     * @return whether or not the list is empty.
     */
    boolean hasVariables();

    /**
     * Checks whether or not the container has local variables.
     *
     * @return whether or not the container has local variables.
     */
    boolean hasLocalVariables();

    /**
     * Checks whether or not the container has transient variables.
     *
     * @return whether or not the container has transient variables.
     */
    boolean hasTransientVariables();

    /**
     * Checks whether or not the container has transient local variables.
     *
     * @return whether or not the container has transient local variables.
     */
    boolean hasTransientLocalVariables();

    /**
     * Checks whether or not the list is empty.
     *
     * @return whether or not the list is empty.
     */
    boolean hasAnyVariables();


    /**
     * Checks whether or not the list is empty.
     *
     * @return whether or not the list is empty.
     */
    boolean hasAnyTransientVariables();

    /**
     * Checks whether or not the task has a variable defined with the given name.
     *
     * @param variableName
     *          name of variable.
     * @return whether or not exists the defined variable.
     */
    boolean hasVariable(String variableName);

    /**
     * Checks whether or not the task has a local variable defined with the given name.
     *
     * @param localVariableName
     *          name of variable.
     *          define scope of variable.
     * @return whether or not exists the defined variable.
     */
    boolean hasVariableLocal(String localVariableName);

    /**
     * Checks whether or not the task has a transient variable defined with the given name.
     *
     * @param transientVariableName
     *          name of variable.
     * @return whether or not exists the defined transient variable.
     */
    boolean hasTransientVariable(String transientVariableName);

    /**
     * Checks whether or not the task has a transient local variable defined with the given name.
     *
     * @param transientLocalVariableName
     *          name of variable.
     *          define scope of variable.
     * @return whether or not exists the defined transient local variable.
     */
    boolean hasTransientLocalVariable(String transientLocalVariableName);


    /**
     * Set variable. If the variable is not already existing, it will be created.
     *
     * @param variableName
     *          name of variable.
     * @param variableValue
     *          value of variable.
     */
    void setVariable(String variableName, Object variableValue);

    /**
     * Set local variable. If the local variable is not already existing, it will be created.
     *
     * @param variableName
     *          name of variable.
     * @param variableValue
     *          value of variable.
     */
    void setVariableLocal(String variableName, Object variableValue);

    /**
     * Set transient variable. If the transient variable is not already existing, it will be created.
     *
     * @param variableName
     *          name of variable.
     * @param variableValue
     *          value of variable.
     */
    void setTransientVariable(String variableName, Object variableValue);

    /**
     * Set transient local variable. If the transient local variable is not already existing, it will be created.
     *
     * @param variableName
     *          name of variable.
     * @param variableValue
     *          value of variable.
     */
    void setTransientVariableLocal(String variableName, Object variableValue);

    /**
     * Set variables. If the variables is not already existing, it will be created.
     *
     * @param variables
     *          variables.
     */
    void setVariables(Map<String, Object> variables);

    /**
     * Set local variables. If the local variables is not already existing, it will be created.
     *
     * @param localVariables
     *          local variables.
     */
    void setLocalVariables(Map<String, Object> localVariables);

    /**
     * Set transient variables. If the transient variables is not already existing, it will be created.
     *
     * @param transientVariables
     *          transient variables.
     */
    void setTransientVariables(Map<String, Object> transientVariables);

    /**
     * Set transient local variables. If the transient local variables is not already existing, it will be created.
     *
     * @param transientLocalVariables
     *          transient local variables.
     */
    void setTransientLocalVariables(Map<String, Object> transientLocalVariables);
}
