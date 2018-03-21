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
package org.flowable.dmn.engine.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.ExecuteDecisionBuilder;

/**
 * @author Tijs Rademakers
 */
public class ExecuteDecisionBuilderImpl implements ExecuteDecisionBuilder {

    protected DmnRuleServiceImpl ruleService;

    protected String decisionKey;
    protected String parentDeploymentId;
    protected String instanceId;
    protected String executionId;
    protected String activityId;
    protected String scopeType;
    protected String tenantId;
    protected Map<String, Object> variables;

    public ExecuteDecisionBuilderImpl(DmnRuleServiceImpl ruleService) {
        this.ruleService = ruleService;
    }

    @Override
    public ExecuteDecisionBuilder decisionKey(String decisionKey) {
        this.decisionKey = decisionKey;
        return this;
    }
    
    @Override
    public ExecuteDecisionBuilder parentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }

    @Override
    public ExecuteDecisionBuilder instanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }
    
    @Override
    public ExecuteDecisionBuilder executionId(String executionId) {
        this.executionId = executionId;
        return this;
    }
    
    @Override
    public ExecuteDecisionBuilder activityId(String activityId) {
        this.activityId = activityId;
        return this;
    }
    
    @Override
    public ExecuteDecisionBuilder scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public ExecuteDecisionBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ExecuteDecisionBuilder variables(Map<String, Object> variables) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        if (variables != null) {
            for (String variableName : variables.keySet()) {
                this.variables.put(variableName, variables.get(variableName));
            }
        }
        return this;
    }

    @Override
    public ExecuteDecisionBuilder variable(String variableName, Object value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(variableName, value);
        return this;
    }

    @Override
    public List<Map<String, Object>> execute() {
        return ruleService.executeDecision(this);
    }
    
    @Override
    public Map<String, Object> executeWithSingleResult() {
        return ruleService.executeDecisionWithSingleResult(this);
    }
    
    @Override
    public DecisionExecutionAuditContainer executeWithAuditTrail() {
        return ruleService.executeDecisionWithAuditTrail(this);
    }

    public String getDecisionKey() {
        return decisionKey;
    }

    public String getParentDeploymentId() {
        return parentDeploymentId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getActivityId() {
        return activityId;
    }
    
    public String getScopeType() {
        return scopeType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

   

}
