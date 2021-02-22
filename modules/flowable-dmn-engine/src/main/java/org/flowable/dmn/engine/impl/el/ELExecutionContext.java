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
package org.flowable.dmn.engine.impl.el;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.model.BuiltinAggregator;

/**
 * @author Yvo Swillens
 */
public class ELExecutionContext {

    protected Map<Integer, Map<String, Object>> ruleResults = new LinkedHashMap<>();
    protected Map<String, Object> stackVariables;
    protected DecisionExecutionAuditContainer auditContainer;
    protected Map<String, List<Object>> outputValues = new LinkedHashMap<>();
    protected BuiltinAggregator aggregator;
    protected String instanceId;
    protected String scopeType;
    protected String tenantId;
    protected boolean forceDMN11;

    public void checkExecutionContext(String variableId) {
        if (StringUtils.isEmpty(variableId)) {
            throw new IllegalArgumentException("Variable id cannot be empty");
        }
    }

    public void addRuleResult(int ruleNumber, String outputName, Object outputValue) {
        Map<String, Object> ruleResult;
        if (ruleResults.containsKey(ruleNumber)) {
            ruleResult = ruleResults.get(ruleNumber);
        } else {
            ruleResult = new HashMap<>();
            ruleResults.put(ruleNumber, ruleResult);
        }
        ruleResult.put(outputName, outputValue);
    }

    public void setStackVariables(Map<String, Object> variables) {
        this.stackVariables = variables;
    }

    public Map<String, Object> getStackVariables() {
        return stackVariables;
    }

    public Map<String, Object> getRuleResult(int ruleNumber) {
        return ruleResults.get(ruleNumber);
    }

    public Map<Integer, Map<String, Object>> getRuleResults() {
        return ruleResults;
    }

    public DecisionExecutionAuditContainer getAuditContainer() {
        return auditContainer;
    }

    public void setAuditContainer(DecisionExecutionAuditContainer auditContainer) {
        this.auditContainer = auditContainer;
    }

    public Map<String, List<Object>> getOutputValues() {
        return outputValues;
    }

    public void addOutputValues(String outputName, List<Object> outputValues) {
        this.outputValues.put(outputName, outputValues);
    }

    public BuiltinAggregator getAggregator() {
        return aggregator;
    }

    public void setAggregator(BuiltinAggregator aggregator) {
        this.aggregator = aggregator;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isForceDMN11() {
        return forceDMN11;
    }
    public void setForceDMN11(boolean forceDMN11) {
        this.forceDMN11 = forceDMN11;
    }
}
