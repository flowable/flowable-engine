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
package org.flowable.cmmn.api;

import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstance;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface DecisionTableVariableManager {

    void setVariablesOnPlanItemInstance(List<Map<String, Object>> executionResult, String decisionKey, PlanItemInstance planItemInstance, ObjectMapper objectMapper);
    void setDecisionServiceVariablesOnExecution(Map<String, List<Map<String, Object>>> executionResult, String decisionKey, PlanItemInstance planItemInstance, ObjectMapper objectMapper);

}
