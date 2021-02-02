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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceEntityImpl extends AbstractCmmnEngineEntity implements HistoricPlanItemInstanceEntity {

    protected String name;
    protected String state;
    protected String caseDefinitionId;
    protected String derivedCaseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected boolean isStage;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected Date createTime;
    protected Date lastAvailableTime;
    protected Date lastUnavailableTime;
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
    protected String entryCriterionId;
    protected String exitCriterionId;
    protected String extraValue;
    protected boolean showInOverview;
    protected String tenantId = CmmnEngineConfiguration.NO_TENANT_ID;
    protected String localizedName;

    public HistoricPlanItemInstanceEntityImpl() {
    }

    public HistoricPlanItemInstanceEntityImpl(PlanItemInstance planItemInstance) {
        this.id = planItemInstance.getId();
        this.name = planItemInstance.getName();
        this.state = planItemInstance.getState();
        this.caseDefinitionId = planItemInstance.getCaseDefinitionId();
        this.derivedCaseDefinitionId = planItemInstance.getDerivedCaseDefinitionId();
        this.caseInstanceId = planItemInstance.getCaseInstanceId();
        this.stageInstanceId = planItemInstance.getStageInstanceId();
        this.isStage = planItemInstance.isStage();
        this.elementId = planItemInstance.getElementId();
        this.planItemDefinitionId = planItemInstance.getPlanItemDefinitionId();
        this.planItemDefinitionType = planItemInstance.getPlanItemDefinitionType();
        this.startUserId = planItemInstance.getStartUserId();
        this.referenceId = planItemInstance.getReferenceId();
        this.referenceType = planItemInstance.getReferenceType();
        this.createTime = planItemInstance.getCreateTime();
        this.entryCriterionId = planItemInstance.getEntryCriterionId();
        this.exitCriterionId = planItemInstance.getExitCriterionId();
        this.extraValue = planItemInstance.getExtraValue();

        this.lastAvailableTime = planItemInstance.getLastAvailableTime();
        this.lastUnavailableTime = planItemInstance.getLastUnavailableTime();
        this.lastEnabledTime = planItemInstance.getLastEnabledTime();
        this.lastDisabledTime = planItemInstance.getLastDisabledTime();
        this.lastStartedTime = planItemInstance.getLastStartedTime();
        this.lastSuspendedTime = planItemInstance.getLastSuspendedTime();
        this.completedTime = planItemInstance.getCompletedTime();
        this.occurredTime = planItemInstance.getOccurredTime();
        this.terminatedTime = planItemInstance.getTerminatedTime();
        this.endedTime = planItemInstance.getEndedTime();

        if (planItemInstance.getTenantId() != null) {
            this.tenantId = planItemInstance.getTenantId();
        }
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("derivedCaseDefinitionId", derivedCaseDefinitionId);
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("stageInstanceId", stageInstanceId);
        persistentState.put("isStage", isStage);
        persistentState.put("elementId", elementId);
        persistentState.put("name", name);
        persistentState.put("state", state);
        persistentState.put("createTime", createTime);
        persistentState.put("lastAvailableTime", lastAvailableTime);
        persistentState.put("lastUnavailableTime", lastUnavailableTime);
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
        persistentState.put("planItemDefinitionId", planItemDefinitionId);
        persistentState.put("planItemDefinitionType", planItemDefinitionType);
        persistentState.put("entryCriterionId", entryCriterionId);
        persistentState.put("exitCriterionId", exitCriterionId);
        persistentState.put("extraValue", extraValue);
        persistentState.put("showInOverview", showInOverview);
        persistentState.put("tenantId", tenantId);
        return persistentState;
    }

    @Override
    public String getName() {
        if (StringUtils.isNotBlank(localizedName)) {
            return localizedName;
        }
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
    public void setDerivedCaseDefinitionId(String derivedCaseDefinitionId) {
        this.derivedCaseDefinitionId = derivedCaseDefinitionId;
    }

    @Override
    public String getDerivedCaseDefinitionId() {
        return derivedCaseDefinitionId;
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
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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
    public Date getLastUnavailableTime() {
        return lastUnavailableTime;
    }

    @Override
    public void setLastUnavailableTime(Date lastUnavailableTime) {
        this.lastUnavailableTime = lastUnavailableTime;
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
    public String getEntryCriterionId() {
        return entryCriterionId;
    }

    @Override
    public void setEntryCriterionId(String entryCriterionId) {
        this.entryCriterionId = entryCriterionId;
    }

    @Override
    public String getExitCriterionId() {
        return exitCriterionId;
    }

    @Override
    public void setExitCriterionId(String exitCriterionId) {
        this.exitCriterionId = exitCriterionId;
    }
    
    @Override
    public String getFormKey() {
        return extraValue;
    }

    @Override
    public void setFormKey(String formKey) {
        this.extraValue = formKey;
    }

    @Override
    public String getExtraValue() {
        return extraValue;
    }

    @Override
    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
    }

    @Override
    public boolean isShowInOverview() {
        return showInOverview;
    }

    @Override
    public void setShowInOverview(boolean showInOverview) {
        this.showInOverview = showInOverview;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }
}
