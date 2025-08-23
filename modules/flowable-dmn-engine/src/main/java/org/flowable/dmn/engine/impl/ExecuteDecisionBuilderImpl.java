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
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.flowable.dmn.api.ExecuteDecisionContext;

/**
 * @author Tijs Rademakers
 */
public class ExecuteDecisionBuilderImpl implements ExecuteDecisionBuilder {

    protected DmnDecisionService decisionService;

    protected String decisionKey;
    protected String parentDeploymentId;
    protected String instanceId;
    protected String executionId;
    protected String activityId;
    protected String scopeType;
    protected String tenantId;
    protected Map<String, Object> variables;
    protected boolean fallbackToDefaultTenant;
    protected boolean disableHistory;

    public ExecuteDecisionBuilderImpl(DmnDecisionService decisionService) {
        this.decisionService = decisionService;
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
    public ExecuteDecisionBuilder fallbackToDefaultTenant() {
        this.fallbackToDefaultTenant = true;
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
    public ExecuteDecisionBuilder disableHistory() {
        this.disableHistory = true;
        return this;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public List<Map<String, Object>> execute() {
        return decisionService.executeDecision(this);
    }

    @Override
    public List<Map<String, Object>> executeDecision() {
        return decisionService.executeDecision(this);
    }

    @Override
    public Map<String, List<Map<String, Object>>> executeDecisionService() {
        return decisionService.executeDecisionService(this);
    }

    @Override
    public Map<String, Object> executeWithSingleResult() {
        return decisionService.executeWithSingleResult(this);
    }

    @Override
    public Map<String, Object> executeDecisionWithSingleResult() {
        return decisionService.executeDecisionWithSingleResult(this);
    }

    @Override
    public Map<String, Object> executeDecisionServiceWithSingleResult() {
        return decisionService.executeDecisionServiceWithSingleResult(this);
    }


    @Override
    public DecisionExecutionAuditContainer executeWithAuditTrail() {
        return decisionService.executeWithAuditTrail(this);
    }

    @Override
    public DecisionExecutionAuditContainer executeDecisionWithAuditTrail() {
        return decisionService.executeDecisionWithAuditTrail(this);
    }

    @Override
    public DecisionServiceExecutionAuditContainer executeDecisionServiceWithAuditTrail() {
        return decisionService.executeDecisionServiceWithAuditTrail(this);
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

    public boolean isFallbackToDefaultTenant() {
        return this.fallbackToDefaultTenant;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public ExecuteDecisionContext buildExecuteDecisionContext() {
        ExecuteDecisionContext executeDecisionContext = new ExecuteDecisionContext();
        executeDecisionContext.setDecisionKey(decisionKey);
        executeDecisionContext.setParentDeploymentId(parentDeploymentId);
        executeDecisionContext.setInstanceId(instanceId);
        executeDecisionContext.setExecutionId(executionId);
        executeDecisionContext.setActivityId(activityId);
        executeDecisionContext.setScopeType(scopeType);
        executeDecisionContext.setVariables(variables);
        executeDecisionContext.setTenantId(tenantId);
        executeDecisionContext.setFallbackToDefaultTenant(fallbackToDefaultTenant);
        executeDecisionContext.setDisableHistory(disableHistory);

        return executeDecisionContext;
    }
}
