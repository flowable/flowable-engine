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

import java.util.Map;

public class ExecuteDecisionInfo {

    protected String decisionKey;
    protected String decisionDefinitionId;
    protected String deploymentId;
    protected String parentDeploymentId;
    protected String instanceId;
    protected String executionId;
    protected String activityId;
    protected String scopeType;
    protected Map<String, Object> variables;
    protected String tenantId;
    
    public String getDecisionKey() {
        return decisionKey;
    }
    public void setDecisionKey(String decisionKey) {
        this.decisionKey = decisionKey;
    }
    public String getDecisionDefinitionId() {
        return decisionDefinitionId;
    }
    public void setDecisionDefinitionId(String decisionDefinitionId) {
        this.decisionDefinitionId = decisionDefinitionId;
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
}
