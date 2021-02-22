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
 * @author Ievgenii Bespal
 */
public interface TaskCompletionBuilder {


    /**
     * Sets a variables before the task is completed.
     * The variables will be stored on the plan item instance.
     */
    TaskCompletionBuilder variables(Map<String, Object> variables);

    /**
     * Sets a local variables before the task is completed.
     * The variables will be stored locally on the plan item instance.
     */
    TaskCompletionBuilder variablesLocal(Map<String, Object> variablesLocal);

    /**
     * Sets a non-persisted variables before the task is completed.
     * The transient variables will not be persisted at the end of the database transaction.
     */
    TaskCompletionBuilder transientVariables(Map<String, Object> transientVariables);

    /**
     * Sets a non-persisted local variables before the task is completed.
     * The local transient variables will not be persisted at the end of the database transaction.
     */
    TaskCompletionBuilder transientVariablesLocal(Map<String, Object> transientVariablesLocal);

    /**
     * Sets a variable before the task is completed.
     * The variable will be stored on the process instance.
     */
    TaskCompletionBuilder variable(String variableName, Object variableValue);

    /**
     * Sets a local variable before the task is completed.
     * The local variable will be stored on the process instance.
     */
    TaskCompletionBuilder variableLocal(String variableName, Object variableValue);

    /**
     * Sets a non-persisted transient variable before the task is completed.
     * The transient variable will not be persisted at the end of the database transaction.
     */
    TaskCompletionBuilder transientVariable(String variableName, Object variableValue);

    /**
     * Sets a non-persisted transient local variable before the task is completed.
     * The transient local variable will not be persisted at the end of the database transaction.
     */
    TaskCompletionBuilder transientVariableLocal(String variableName, Object variableValue);

    /**
     * Sets a task id
     */
    TaskCompletionBuilder taskId(String id);

    /**
     * Sets a form definition id
     */
    TaskCompletionBuilder formDefinitionId(String formDefinitionId);

    /**
     * Sets an outcome
     */
    TaskCompletionBuilder outcome(String outcome);

    /**
     * Completes a task
     */
    void complete();
}
