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
package org.flowable.variable.service.impl.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Joram Barrez
 */
public class VariableAggregationInfo {

    protected String instanceId; // process instance or case instance

    // Key for the following maps is the elementId
    protected Map<String, List<VariableAggregation>> variableAggregationsMap = new HashMap<>();

    /**
     * The scope id (e.g. execution id) where instance variables will be stored before aggregation is applied.
     */
    protected Map<String, String> beforeAggregationScopeIdMap = new HashMap<>();

    /**
     * The scope id (e.g. execution id) where aggregation results will be stored.
     */
    protected Map<String, String> aggregationScopeIdMap = new HashMap<>();

    public VariableAggregationInfo(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void addRuntimeInfo(String elementId, List<VariableAggregation> variableAggregations, String beforeAggregationScopeId, String aggregationScopeId) {
        variableAggregationsMap.put(elementId, variableAggregations);
        beforeAggregationScopeIdMap.put(elementId, beforeAggregationScopeId);
        aggregationScopeIdMap.put(elementId, aggregationScopeId);
    }

    /**
     * @return Returns any {@link VariableAggregation} matching the provided variable name.
     *         The key of the map is the elementId of the element in the model defining the variable aggregation.
     */
    public Map<String, List<VariableAggregation>> findMatchingVariableAggregations(String variableName) {

        Map<String, List<VariableAggregation>> result = null;

        for (String elementId : variableAggregationsMap.keySet()) {

            List<VariableAggregation> matchingVariableAggregations = variableAggregationsMap.get(elementId)
                .stream()
                .filter(variableAggregation -> Objects.equals(variableName, variableAggregation.getSource()))
                .collect(Collectors.toList());

            if (!matchingVariableAggregations.isEmpty()) {

                if (result == null) {
                    result = new HashMap<>();
                }
                if (!result.containsKey(elementId)) {
                    result.put(elementId, new ArrayList<>());
                }
                result.get(elementId).addAll(matchingVariableAggregations);

            }

        }

        return result;
    }

    public List<VariableAggregation> getVariableAggregationsForElementId(String elementId) {
        return variableAggregationsMap.get(elementId);
    }

    public String getBeforeAggregationScopeIdForElementId(String elementId) {
        return beforeAggregationScopeIdMap.get(elementId);
    }

    public String getAggregationScopeIdForElementId(String elementId) {
        return aggregationScopeIdMap.get(elementId);
    }

}
