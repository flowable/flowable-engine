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
package org.flowable.dmn.rest.service.api.history;

import java.util.Date;

import org.flowable.dmn.api.DmnHistoricDecisionExecution;

/**
 * @author Tijs Rademakers
 */
public class HistoricDecisionExecutionResponse {

    protected String id;
    protected String url;
    protected String decisionDefinitionId;
    protected String deploymentId;
    protected String activityId;
    protected String executionId;
    protected String instanceId;
    protected String scopeType;
    protected boolean failed;
    protected Date startTime;
    protected Date endTime;
    protected String tenantId;
    protected String decisionKey;
    protected String decisionName;
    protected String decisionVersion;

    public HistoricDecisionExecutionResponse(DmnHistoricDecisionExecution historicDecisionExecution) {
        setId(historicDecisionExecution.getId());
        setDecisionDefinitionId(historicDecisionExecution.getDecisionDefinitionId());
        setDeploymentId(historicDecisionExecution.getDeploymentId());
        setActivityId(historicDecisionExecution.getActivityId());
        setExecutionId(historicDecisionExecution.getExecutionId());
        setInstanceId(historicDecisionExecution.getInstanceId());
        setScopeType(historicDecisionExecution.getScopeType());
        setFailed(historicDecisionExecution.isFailed());
        setStartTime(historicDecisionExecution.getStartTime());
        setEndTime(historicDecisionExecution.getEndTime());
        setTenantId(historicDecisionExecution.getTenantId());
        setDecisionKey(historicDecisionExecution.getDecisionKey());
        setDecisionName(historicDecisionExecution.getDecisionName());
        setDecisionVersion(historicDecisionExecution.getDecisionVersion());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDecisionKey() {
        return decisionKey;
    }

    public void setDecisionKey(String decisionKey) {
        this.decisionKey = decisionKey;
    }

    public String getDecisionName() {
        return decisionName;
    }

    public void setDecisionName(String decisionName) {
        this.decisionName = decisionName;
    }

    public String getDecisionVersion() {
        return decisionVersion;
    }

    public void setDecisionVersion(String decisionVersion) {
        this.decisionVersion = decisionVersion;
    }
}
