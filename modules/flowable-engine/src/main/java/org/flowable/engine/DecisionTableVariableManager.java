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
package org.flowable.engine;

import java.util.List;
import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface DecisionTableVariableManager {

    @Deprecated
    void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper);

    @Deprecated
    void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper);

    default void setVariablesOnExecution(List<Map<String, Object>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper, boolean multipleResults) {
        this.setVariablesOnExecution(executionResult, decisionKey, execution, objectMapper);
    }

    default void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, DelegateExecution execution, ObjectMapper objectMapper, boolean multipleResults) {
        this.setDecisionServiceVariablesOnExecution(executionResult, decisionKey, execution, objectMapper);
    }
}
