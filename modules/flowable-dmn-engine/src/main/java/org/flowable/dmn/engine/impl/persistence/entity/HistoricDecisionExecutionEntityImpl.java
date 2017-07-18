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

import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.engine.common.impl.persistence.entity.AbstractEntityNoRevision;

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
    protected boolean failed;
    protected String tenantId = DmnEngineConfiguration.NO_TENANT_ID;
    protected String executionJson;

    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("decisionDefinitionId", this.decisionDefinitionId);
        persistentState.put("deploymentId", this.deploymentId);
        persistentState.put("instanceId", this.instanceId);
        persistentState.put("executionId", this.executionId);
        persistentState.put("activityId", this.activityId);
        persistentState.put("tenantId", this.tenantId);
        return persistentState;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

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

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
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

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getExecutionJson() {
        return executionJson;
    }

    public void setExecutionJson(String executionJson) {
        this.executionJson = executionJson;
    }
    
    public String toString() {
        return "HistoricDecisionExecutionEntity[" + id + "]";
    }

}
