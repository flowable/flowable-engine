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

import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.model.DmnElement;

public class ExecuteDecisionContext {

    protected String decisionKey;
    protected String decisionId;
    protected int decisionVersion;
    protected String deploymentId;
    protected String parentDeploymentId;
    protected String instanceId;
    protected String executionId;
    protected String activityId;
    protected String scopeType;
    protected Map<String, Object> variables;
    protected String tenantId;
    protected boolean fallbackToDefaultTenant;
    protected boolean forceDMN11;
    protected DmnElement dmnElement;
    protected DecisionExecutionAuditContainer decisionExecutionAuditContainer;
    protected Map<String, DecisionExecutionAuditContainer> decisionResults = new LinkedHashMap<>();
    protected ExecuteDecisionContext parentExecuteDecisionContext;

    public ExecuteDecisionContext() {}

    public ExecuteDecisionContext(DmnElement dmnElement, ExecuteDecisionContext parentExecuteDecisionContext) {
        decisionKey = parentExecuteDecisionContext.getDecisionKey();
        decisionId = parentExecuteDecisionContext.getTenantId();
        decisionVersion = parentExecuteDecisionContext.getDecisionVersion();
        deploymentId = parentExecuteDecisionContext.getDeploymentId();
        parentDeploymentId = parentExecuteDecisionContext.getParentDeploymentId();
        instanceId = parentExecuteDecisionContext.getInstanceId();
        executionId = parentExecuteDecisionContext.getExecutionId();
        activityId = parentExecuteDecisionContext.getActivityId();
        scopeType = parentExecuteDecisionContext.getScopeType();
        variables = parentExecuteDecisionContext.getVariables();
        tenantId = parentExecuteDecisionContext.getTenantId();
        fallbackToDefaultTenant = parentExecuteDecisionContext.isFallbackToDefaultTenant();
        this.dmnElement = dmnElement;
        this.parentExecuteDecisionContext = parentExecuteDecisionContext;
    }

    public String getDecisionKey() {
        return decisionKey;
    }
    public void setDecisionKey(String decisionKey) {
        this.decisionKey = decisionKey;
    }
    public String getDecisionId() {
        return decisionId;
    }
    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }
    public int getDecisionVersion() {
        return decisionVersion;
    }
    public void setDecisionVersion(int decisionVersion) {
        this.decisionVersion = decisionVersion;
    }
    public String getDeploymentId() {
        return deploymentId;
    }
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }
    public String getParentDeploymentId() {
        return parentDeploymentId;
    }
    public void setParentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
    }
    public String getInstanceId() {
        return instanceId;
    }
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    public String getExecutionId() {
        return executionId;
    }
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    public String getActivityId() {
        return activityId;
    }
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
    public String getScopeType() {
        return scopeType;
    }
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
    public Map<String, Object> getVariables() {
        return variables;
    }
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
    public String getTenantId() {
        return tenantId;
    }
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    public boolean isFallbackToDefaultTenant() {
        return fallbackToDefaultTenant;
    }
    public void setFallbackToDefaultTenant(boolean fallbackToDefaultTenant) {
        this.fallbackToDefaultTenant = fallbackToDefaultTenant;
    }
    public boolean isForceDMN11() {
        return forceDMN11;
    }
    public void setForceDMN11(boolean forceDMN11) {
        this.forceDMN11 = forceDMN11;
    }
    public DmnElement getDmnElement() {
        return dmnElement;
    }
    public void setDmnElement(DmnElement dmnElement) {
        this.dmnElement = dmnElement;
    }
    public DecisionExecutionAuditContainer getDecisionExecutionAuditContainer() {
        return decisionExecutionAuditContainer;
    }
    public void setDecisionExecutionAuditContainer(DecisionExecutionAuditContainer decisionExecutionAuditContainer) {
        this.decisionExecutionAuditContainer = decisionExecutionAuditContainer;
    }
    public DecisionExecutionAuditContainer getDecisionResult(String decisionKey) {
        return decisionResults.get(decisionKey);
    }
    public void addDecisionResult(String decisionKey, DecisionExecutionAuditContainer decisionResult) {
        decisionResults.put(decisionKey, decisionResult);
    }
    public Map<String, DecisionExecutionAuditContainer> getDecisionResults() {
        return decisionResults;
    }
    public void setDecisionResults(Map<String, DecisionExecutionAuditContainer> decisionResults) {
        this.decisionResults = decisionResults;
    }
    public ExecuteDecisionContext getParentExecuteDecisionContext() {
        return parentExecuteDecisionContext;
    }
    public void setParentExecuteDecisionContext(ExecuteDecisionContext parentExecuteDecisionContext) {
        this.parentExecuteDecisionContext = parentExecuteDecisionContext;
    }
}
