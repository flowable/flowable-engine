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
package org.flowable.variable.service.impl.persistence.entity;

/**
 * @author Joram Barrez
 */
public class VariableAggregationScopeInfo {

    /**
     * The id of a scope (e.g. execution) to which gathered variables are stored
     * before they are aggregated (typically at the end of a multi-instance).
     *
     * This is not necessarily the same as the scope onto which variables are grouped.
     * For example in BPMN, the gathering scope is typically the multi-instance root execution
     * (such that deletion of variables happens correctly), but the grouping id is one of it's child
     * executions (as those typically are one instance of a multi-instance construct).
     */
    protected String gatheredVariableScopeId;

    /**
     * The id with which variables will be correlated. See above.
     */
    protected String variableCorrelationScopeId;

    public VariableAggregationScopeInfo(String gatheredVariableScopeId, String variableCorrelationScopeId) {
        this.gatheredVariableScopeId = gatheredVariableScopeId;
        this.variableCorrelationScopeId = variableCorrelationScopeId;
    }

    public String getGatheredVariableScopeId() {
        return gatheredVariableScopeId;
    }
    public void setGatheredVariableScopeId(String gatheredVariableScopeId) {
        this.gatheredVariableScopeId = gatheredVariableScopeId;
    }
    public String getVariableCorrelationScopeId() {
        return variableCorrelationScopeId;
    }
    public void setVariableCorrelationScopeId(String variableCorrelationScopeId) {
        this.variableCorrelationScopeId = variableCorrelationScopeId;
    }
}
