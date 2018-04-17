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
package org.flowable.dmn.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;
import org.flowable.dmn.engine.DmnEngineConfiguration;

/**
 * @author Tijs Rademakers
 */
public class HistoricDecisionExecutionEntityImpl extends AbstractEntityNoRevision implements HistoricDecisionExecutionEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String decisionDefinitionId;
    protected String deploymentId;
    protected Date startTime;
    protected Date endTime;
    protected String instanceId;
    protected String executionId;
    protected String activityId;
    protected String scopeType;
    protected boolean failed;
    protected String tenantId = DmnEngineConfiguration.NO_TENANT_ID;
    protected String executionJson;
    protected String decisionKey;
    protected String decisionName;
    protected String decisionVersion;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("decisionDefinitionId", this.decisionDefinitionId);
        persistentState.put("deploymentId", this.deploymentId);
        persistentState.put("instanceId", this.instanceId);
        persistentState.put("executionId", this.executionId);
        persistentState.put("activityId", this.activityId);
        persistentState.put("scopeType", this.scopeType);
        persistentState.put("tenantId", this.tenantId);
        return persistentState;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getDecisionDefinitionId() {
        return decisionDefinitionId;
    }

    @Override
    public void setDecisionDefinitionId(String decisionDefinitionId) {
        this.decisionDefinitionId = decisionDefinitionId;
    }

    @Override
    public String getDeploymentId() {
        return deploymentId;
    }

    @Override
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public String getActivityId() {
        return activityId;
    }

    @Override
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public boolean isFailed() {
        return failed;
    }

    @Override
    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getExecutionJson() {
        return executionJson;
    }

    @Override
    public void setExecutionJson(String executionJson) {
        this.executionJson = executionJson;
    }

    @Override
    public String getDecisionKey() {
        return decisionKey;
    }

    public void setDecisionKey(String decisionKey) {
        this.decisionKey = decisionKey;
    }

    @Override
    public String getDecisionName() {
        return decisionName;
    }

    public void setDecisionName(String decisionName) {
        this.decisionName = decisionName;
    }

    @Override
    public String getDecisionVersion() {
        return decisionVersion;
    }

    public void setDecisionVersion(String decisionVersion) {
        this.decisionVersion = decisionVersion;
    }

    @Override
    public String toString() {
        return "HistoricDecisionExecutionEntity[" + id + "]";
    }

}
