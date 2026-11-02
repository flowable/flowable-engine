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
package org.flowable.task.api;

import java.util.Map;

/**
 * This builder is an alternative to using any of the complete methods on the TaskService.
 *
 * @author Ievgenii Bespal
 * @author Joram Barrez
 */
public interface TaskCompletionBuilder {


    /**
     * Sets variables that are added on the instance level.
     */
    TaskCompletionBuilder variables(Map<String, Object> variables);

    /**
     * Sets task-local variables instead of instance-level variables.
     */
    TaskCompletionBuilder variablesLocal(Map<String, Object> variablesLocal);

    /**
     * Sets non-persisted instance variables.
     */
    TaskCompletionBuilder transientVariables(Map<String, Object> transientVariables);

    /**
     * Sets non-persisted task-local variables.
     */
    TaskCompletionBuilder transientVariablesLocal(Map<String, Object> transientVariablesLocal);

    /**
     * Sets one instance-level variable.
     */
    TaskCompletionBuilder variable(String variableName, Object variableValue);

    /**
     * Sets one task-local variables instead of instance-level variables.
     */
    TaskCompletionBuilder variableLocal(String variableName, Object variableValue);

    /**
     * Sets one non-persisted instance variables.
     */
    TaskCompletionBuilder transientVariable(String variableName, Object variableValue);

    /**
     * Sets one non-persisted instance variables.
     */
    TaskCompletionBuilder transientVariableLocal(String variableName, Object variableValue);

    /**
     * Sets the id of the task which is completed.
     */
    TaskCompletionBuilder taskId(String id);

    /**
     * Sets a form definition id. Only needed when there's a form associated with the task.
     */
    TaskCompletionBuilder formDefinitionId(String formDefinitionId);

    /**
     * Sets an outcome for the form.
     */
    TaskCompletionBuilder outcome(String outcome);

    /**
     * Completes the task.
     */
    void complete();

}
