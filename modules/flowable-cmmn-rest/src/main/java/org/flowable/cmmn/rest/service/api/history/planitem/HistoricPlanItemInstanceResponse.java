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

package org.flowable.cmmn.rest.service.api.history.planitem;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import org.flowable.common.rest.util.DateToStringSerializer;

import java.util.Date;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceResponse {

    protected String id;
    protected String name;
    protected String state;
    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected boolean isStage;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date createdTime;
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
    protected String tenantId;
    protected String url;
    protected String historicCaseInstanceUrl;
    protected String caseDefinitionUrl;

    @ApiModelProperty(example = "5")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "myPlanItemName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "completed")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @ApiModelProperty(example = "myCaseId%3A1%3A4")
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @ApiModelProperty(example = "12345")
    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @ApiModelProperty(example = "stageId")
    public String getStageInstanceId() {
        return stageInstanceId;
    }

    public void setStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
    }

    @ApiModelProperty(example = "true")
    public boolean isStage() {
        return isStage;
    }

    public void setStage(boolean stage) {
        isStage = stage;
    }

    @ApiModelProperty(example = "someElementId")
    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    @ApiModelProperty(example = "someId")
    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }

    public void setPlanItemDefinitionId(String planItemDefinitionId) {
        this.planItemDefinitionId = planItemDefinitionId;
    }

    @ApiModelProperty(example = "timerEventListener")
    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }

    public void setPlanItemDefinitionType(String planItemDefinitionType) {
        this.planItemDefinitionType = planItemDefinitionType;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getLastAvailableTime() {
        return lastAvailableTime;
    }

    public void setLastAvailableTime(Date lastAvailableTime) {
        this.lastAvailableTime = lastAvailableTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getLastEnabledTime() {
        return lastEnabledTime;
    }

    public void setLastEnabledTime(Date lastEnabledTime) {
        this.lastEnabledTime = lastEnabledTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getLastDisabledTime() {
        return lastDisabledTime;
    }

    public void setLastDisabledTime(Date lastDisabledTime) {
        this.lastDisabledTime = lastDisabledTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getLastStartedTime() {
        return lastStartedTime;
    }

    public void setLastStartedTime(Date lastStartedTime) {
        this.lastStartedTime = lastStartedTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getLastSuspendedTime() {
        return lastSuspendedTime;
    }

    public void setLastSuspendedTime(Date lastSuspendedTime) {
        this.lastSuspendedTime = lastSuspendedTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Date completedTime) {
        this.completedTime = completedTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getOccurredTime() {
        return occurredTime;
    }

    public void setOccurredTime(Date occurredTime) {
        this.occurredTime = occurredTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getTerminatedTime() {
        return terminatedTime;
    }

    public void setTerminatedTime(Date terminatedTime) {
        this.terminatedTime = terminatedTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getExitTime() {
        return exitTime;
    }

    public void setExitTime(Date exitTime) {
        this.exitTime = exitTime;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getEndedTime() {
        return endedTime;
    }

    public void setEndedTime(Date endedTime) {
        this.endedTime = endedTime;
    }

    @ApiModelProperty(example = "kermit")
    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    @ApiModelProperty(example = "referenceId")
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @ApiModelProperty(example = "referenceType")
    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-history/historic-planitem-instances/5")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-history/historic-case-instances/12345")
    public String getHistoricCaseInstanceUrl() {
        return historicCaseInstanceUrl;
    }

    public void setHistoricCaseInstanceUrl(String historicCaseInstanceUrl) {
        this.historicCaseInstanceUrl = historicCaseInstanceUrl;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/case-definitions/myCaseId%3A1%3A4")
    public String getCaseDefinitionUrl() {
        return caseDefinitionUrl;
    }

    public void setCaseDefinitionUrl(String caseDefinitionUrl) {
        this.caseDefinitionUrl = caseDefinitionUrl;
    }


}
