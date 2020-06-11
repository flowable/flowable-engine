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

    TaskCompletionBuilder setVariables(Map<String, Object> variables);

    TaskCompletionBuilder setVariablesLocal(Map<String, Object> variablesLocal);

    TaskCompletionBuilder setTransientVariables(Map<String, Object> transientVariables);

    TaskCompletionBuilder setTransientVariablesLocal(Map<String, Object> transientVariablesLocal);

    TaskCompletionBuilder setVariable(String variableName, Object variableValue);

    TaskCompletionBuilder setVariableLocal(String variableName, Object variableValue);

    TaskCompletionBuilder setTransientVariable(String variableName, Object variableValue);

    TaskCompletionBuilder setTransientVariableLocal(String variableName, Object variableValue);

    TaskCompletionBuilder setTaskId(String id);
    TaskCompletionBuilder setFormDefinitionId(String formDefinitionId);
    TaskCompletionBuilder setOutcome(String outcome);

    /**
     * Completes the task
     */
    void complete();
}
