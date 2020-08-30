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

import java.util.List;

/**
 * @author Joram Barrez
 */
public class VariableAggregationInfo {

    protected String instanceId; // process instance or case instance
    protected List<VariableAggregation> variableAggregations;

    /**
     * The scope id (e.g. execution id) where instance variables will be stored before aggregation is applied.
     */
    protected String beforeAggregationScopeId;

    /**
     * The scope id (e.g. execution id) where aggregation results will be stored.
     */
    protected String aggregationScopeId;

    public VariableAggregationInfo(String instanceId, List<VariableAggregation> variableAggregations, String beforeAggregationScopeId,
        String aggregationScopeId) {
        this.instanceId = instanceId;
        this.variableAggregations = variableAggregations;
        this.beforeAggregationScopeId = beforeAggregationScopeId;
        this.aggregationScopeId = aggregationScopeId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public List<VariableAggregation> getVariableAggregations() {
        return variableAggregations;
    }
    public void setVariableAggregations(List<VariableAggregation> variableAggregations) {
        this.variableAggregations = variableAggregations;
    }
    public String getBeforeAggregationScopeId() {
        return beforeAggregationScopeId;
    }
    public void setBeforeAggregationScopeId(String beforeAggregationScopeId) {
        this.beforeAggregationScopeId = beforeAggregationScopeId;
    }
    public String getAggregationScopeId() {
        return aggregationScopeId;
    }
    public void setAggregationScopeId(String aggregationScopeId) {
        this.aggregationScopeId = aggregationScopeId;
    }
}
