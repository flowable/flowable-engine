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

package org.flowable.cmmn.rest.service.api.runtime.planitem;

import java.util.Date;

import org.flowable.common.rest.util.DateToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class PlanItemInstanceResponse {

    protected String id;
    protected String url;
    protected String name;
    protected String caseInstanceId;
    protected String caseInstanceUrl;
    protected String caseDefinitionId;
    protected String caseDefinitionUrl;
    protected String derivedCaseDefinitionId;
    protected String derivedCaseDefinitionUrl;
    protected String stageInstanceId;
    protected String stageInstanceUrl;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected String state;
    protected boolean stage;
    protected String elementId;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date createTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date lastAvailableTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date lastEnabledTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date lastDisabledTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date lastStartedTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date lastSuspendedTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date completedTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date occurredTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date terminatedTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date exitTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date endedTime;
    protected String startUserId;
    protected String referenceId;
    protected String referenceType;
    protected boolean completable;
    protected String entryCriterionId;
    protected String exitCriterionId;
    protected String formKey;
    protected String extraValue;
    protected String tenantId;

    @ApiModelProperty(example = "5")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-runtime/plan-item-instances/5")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getCaseInstanceUrl() {
        return caseInstanceUrl;
    }

    public void setCaseInstanceUrl(String caseInstanceUrl) {
        this.caseInstanceUrl = caseInstanceUrl;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getCaseDefinitionUrl() {
        return caseDefinitionUrl;
    }

    public void setCaseDefinitionUrl(String caseDefinitionUrl) {
        this.caseDefinitionUrl = caseDefinitionUrl;
    }

    public String getDerivedCaseDefinitionId() {
        return derivedCaseDefinitionId;
    }

    public void setDerivedCaseDefinitionId(String derivedCaseDefinitionId) {
        this.derivedCaseDefinitionId = derivedCaseDefinitionId;
    }

    public String getDerivedCaseDefinitionUrl() {
        return derivedCaseDefinitionUrl;
    }

    public void setDerivedCaseDefinitionUrl(String derivedCaseDefinitionUrl) {
        this.derivedCaseDefinitionUrl = derivedCaseDefinitionUrl;
    }

    public String getStageInstanceId() {
        return stageInstanceId;
    }

    public void setStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
    }

    public String getStageInstanceUrl() {
        return stageInstanceUrl;
    }

    public void setStageInstanceUrl(String stageInstanceUrl) {
        this.stageInstanceUrl = stageInstanceUrl;
    }

    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }

    public void setPlanItemDefinitionId(String planItemDefinitionId) {
        this.planItemDefinitionId = planItemDefinitionId;
    }

    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }

    public void setPlanItemDefinitionType(String planItemDefinitionType) {
        this.planItemDefinitionType = planItemDefinitionType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isStage() {
        return stage;
    }

    public void setStage(boolean stage) {
        this.stage = stage;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastAvailableTime() {
        return lastAvailableTime;
    }

    public void setLastAvailableTime(Date lastAvailableTime) {
        this.lastAvailableTime = lastAvailableTime;
    }

    public Date getLastEnabledTime() {
        return lastEnabledTime;
    }

    public void setLastEnabledTime(Date lastEnabledTime) {
        this.lastEnabledTime = lastEnabledTime;
    }

    public Date getLastDisabledTime() {
        return lastDisabledTime;
    }

    public void setLastDisabledTime(Date lastDisabledTime) {
        this.lastDisabledTime = lastDisabledTime;
    }

    public Date getLastStartedTime() {
        return lastStartedTime;
    }

    public void setLastStartedTime(Date lastStartedTime) {
        this.lastStartedTime = lastStartedTime;
    }

    public Date getLastSuspendedTime() {
        return lastSuspendedTime;
    }

    public void setLastSuspendedTime(Date lastSuspendedTime) {
        this.lastSuspendedTime = lastSuspendedTime;
    }

    public Date getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Date completedTime) {
        this.completedTime = completedTime;
    }

    public Date getOccurredTime() {
        return occurredTime;
    }

    public void setOccurredTime(Date occurredTime) {
        this.occurredTime = occurredTime;
    }

    public Date getTerminatedTime() {
        return terminatedTime;
    }

    public void setTerminatedTime(Date terminatedTime) {
        this.terminatedTime = terminatedTime;
    }

    public Date getExitTime() {
        return exitTime;
    }

    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }

    public Date getEndedTime() {
        return endedTime;
    }

    public void setEndedTime(Date endedTime) {
        this.endedTime = endedTime;
    }

    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public boolean isCompletable() {
        return completable;
    }

    public void setCompletable(boolean completable) {
        this.completable = completable;
    }

    public String getEntryCriterionId() {
        return entryCriterionId;
    }

    public void setEntryCriterionId(String entryCriterionId) {
        this.entryCriterionId = entryCriterionId;
    }

    public String getExitCriterionId() {
        return exitCriterionId;
    }

    public void setExitCriterionId(String exitCriterionId) {
        this.exitCriterionId = exitCriterionId;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public String getExtraValue() {
        return extraValue;
    }

    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
