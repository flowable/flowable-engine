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
package org.flowable.cmmn.engine.impl.persistence.entity;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceEntityImpl extends AbstractEntity implements HistoricPlanItemInstanceEntity {

    protected String name;
    protected String state;
    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected boolean isStage;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected Date createdTime;
    protected Date lastAvailableTime;
    protected Date lastEnabledTime;
    protected Date lastDisabledTime;
    protected Date lastStartedTime;
    protected Date lastSuspendedTime;
    protected Date completedTime;
    protected Date occurredTime;
    protected Date terminatedTime;
    protected Date exitTime;
    protected Date endedTime;
    protected Date lastUpdatedTime;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("stageInstanceId", stageInstanceId);
        persistentState.put("isStage", isStage);
        persistentState.put("elementId", elementId);
        persistentState.put("name", name);
        persistentState.put("state", state);
        persistentState.put("createdTime", createdTime);
        persistentState.put("lastAvailableTime", lastAvailableTime);
        persistentState.put("lastEnabledTime", lastEnabledTime);
        persistentState.put("lastDisabledTime", lastDisabledTime);
        persistentState.put("lastStartedTime", lastStartedTime);
        persistentState.put("lastSuspendedTime", lastSuspendedTime);
        persistentState.put("completedTime", completedTime);
        persistentState.put("occurredTime", occurredTime);
        persistentState.put("terminatedTime", terminatedTime);
        persistentState.put("exitTime", exitTime);
        persistentState.put("endedTime", endedTime);
        persistentState.put("lastUpdatedTime", lastUpdatedTime);
        persistentState.put("startUserId", startUserId);
        persistentState.put("referenceId", referenceId);
        persistentState.put("referenceType", referenceType);
        persistentState.put("tenantId", tenantId);
        persistentState.put("planItemDefinitionId", planItemDefinitionId);
        persistentState.put("planItemDefinitionType", planItemDefinitionType);
        return persistentState;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    @Override
    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @Override
    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    @Override
    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @Override
    public String getStageInstanceId() {
        return stageInstanceId;
    }

    @Override
    public void setStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
    }

    @Override
    public boolean isStage() {
        return isStage;
    }

    @Override
    public void setStage(boolean isStage) {
        this.isStage = isStage;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    @Override
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    @Override
    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }

    @Override
    public void setPlanItemDefinitionId(String planItemDefinitionId) {
        this.planItemDefinitionId = planItemDefinitionId;
    }

    @Override
    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }

    @Override
    public void setPlanItemDefinitionType(String planItemDefinitionType) {
        this.planItemDefinitionType = planItemDefinitionType;
    }

    @Override
    public Date getCreatedTime() {
        return createdTime;
    }

    @Override
    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public Date getLastAvailableTime() {
        return lastAvailableTime;
    }

    @Override
    public void setLastAvailableTime(Date lastAvailableTime) {
        this.lastAvailableTime = lastAvailableTime;
    }

    @Override
    public Date getLastEnabledTime() {
        return lastEnabledTime;
    }

    @Override
    public void setLastEnabledTime(Date lastEnabledTime) {
        this.lastEnabledTime = lastEnabledTime;
    }

    @Override
    public Date getLastDisabledTime() {
        return lastDisabledTime;
    }

    @Override
    public void setLastDisabledTime(Date lastDisabledTime) {
        this.lastDisabledTime = lastDisabledTime;
    }

    @Override
    public Date getLastStartedTime() {
        return lastStartedTime;
    }

    @Override
    public void setLastStartedTime(Date lastStartedTime) {
        this.lastStartedTime = lastStartedTime;
    }

    @Override
    public Date getLastSuspendedTime() {
        return lastSuspendedTime;
    }

    @Override
    public void setLastSuspendedTime(Date lastSuspendedTime) {
        this.lastSuspendedTime = lastSuspendedTime;
    }

    @Override
    public Date getCompletedTime() {
        return completedTime;
    }

    @Override
    public void setCompletedTime(Date completedTime) {
        this.completedTime = completedTime;
    }

    @Override
    public Date getOccurredTime() {
        return occurredTime;
    }

    @Override
    public void setOccurredTime(Date occurredTime) {
        this.occurredTime = occurredTime;
    }

    @Override
    public Date getTerminatedTime() {
        return terminatedTime;
    }

    @Override
    public void setTerminatedTime(Date terminatedTime) {
        this.terminatedTime = terminatedTime;
    }

    @Override
    public Date getExitTime() {
        return exitTime;
    }

    @Override
    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }

    @Override
    public Date getEndedTime() {
        return endedTime;
    }

    @Override
    public void setEndedTime(Date endedTime) {
        this.endedTime = endedTime;
    }
    
    @Override
    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    @Override
    public void setLastUpdatedTime(Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    @Override
    public String getStartUserId() {
        return startUserId;
    }

    @Override
    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    @Override
    public String getReferenceId() {
        return referenceId;
    }

    @Override
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public String getReferenceType() {
        return referenceType;
    }

    @Override
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}
