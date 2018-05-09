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

import org.flowable.common.rest.api.PaginateRequest;

import java.util.Date;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceQueryRequest extends PaginateRequest {

    private String planItemInstanceId;
    private String planItemInstanceName;
    private String planItemInstanceState;
    private String caseDefinitionId;
    private String caseInstanceId;
    private String stageInstanceId;
    private String elementId;
    private String planItemDefinitionId;
    private String planItemDefinitionType;
    private Date createdBefore;
    private Date createdAfter;
    private Date lastAvailableBefore;
    private Date lastAvailableAfter;
    private Date lastEnabledBefore;
    private Date lastEnabledAfter;
    private Date lastDisabledBefore;
    private Date lastDisabledAfter;
    private Date lastStartedBefore;
    private Date lastStartedAfter;
    private Date lastSuspendedBefore;
    private Date lastSuspendedAfter;
    private Date completedBefore;
    private Date completedAfter;
    private Date terminatedBefore;
    private Date terminatedAfter;
    private Date occurredBefore;
    private Date occurredAfter;
    private Date exitBefore;
    private Date exitAfter;
    private Date endedBefore;
    private Date endedAfter;
    private String startUserId;
    private String referenceId;
    private String referenceType;
    private String tenantId;
    private Boolean withoutTenantId;

    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }

    public void setPlanItemInstanceId(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    public String getPlanItemInstanceName() {
        return planItemInstanceName;
    }

    public void setPlanItemInstanceName(String planItemInstanceName) {
        this.planItemInstanceName = planItemInstanceName;
    }

    public String getPlanItemInstanceState() {
        return planItemInstanceState;
    }

    public void setPlanItemInstanceState(String state) {
        this.planItemInstanceState = state;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getStageInstanceId() {
        return stageInstanceId;
    }

    public void setStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
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

    public Date getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(Date createdBefore) {
        this.createdBefore = createdBefore;
    }

    public Date getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(Date createdAfter) {
        this.createdAfter = createdAfter;
    }

    public Date getLastAvailableBefore() {
        return lastAvailableBefore;
    }

    public void setLastAvailableBefore(Date lastAvailableBefore) {
        this.lastAvailableBefore = lastAvailableBefore;
    }

    public Date getLastAvailableAfter() {
        return lastAvailableAfter;
    }

    public void setLastAvailableAfter(Date lastAvailableAfter) {
        this.lastAvailableAfter = lastAvailableAfter;
    }

    public Date getLastEnabledBefore() {
        return lastEnabledBefore;
    }

    public void setLastEnabledBefore(Date lastEnabledBefore) {
        this.lastEnabledBefore = lastEnabledBefore;
    }

    public Date getLastEnabledAfter() {
        return lastEnabledAfter;
    }

    public void setLastEnabledAfter(Date lastEnabledAfter) {
        this.lastEnabledAfter = lastEnabledAfter;
    }

    public Date getLastDisabledBefore() {
        return lastDisabledBefore;
    }

    public void setLastDisabledBefore(Date lastDisabledBefore) {
        this.lastDisabledBefore = lastDisabledBefore;
    }

    public Date getLastDisabledAfter() {
        return lastDisabledAfter;
    }

    public void setLastDisabledAfter(Date lastDisabledAfter) {
        this.lastDisabledAfter = lastDisabledAfter;
    }

    public Date getLastStartedBefore() {
        return lastStartedBefore;
    }

    public void setLastStartedBefore(Date lastStartedBefore) {
        this.lastStartedBefore = lastStartedBefore;
    }

    public Date getLastStartedAfter() {
        return lastStartedAfter;
    }

    public void setLastStartedAfter(Date lastStartedAfter) {
        this.lastStartedAfter = lastStartedAfter;
    }

    public Date getLastSuspendedBefore() {
        return lastSuspendedBefore;
    }

    public void setLastSuspendedBefore(Date lastSuspendedBefore) {
        this.lastSuspendedBefore = lastSuspendedBefore;
    }

    public Date getLastSuspendedAfter() {
        return lastSuspendedAfter;
    }

    public void setLastSuspendedAfter(Date lastSuspendedAfter) {
        this.lastSuspendedAfter = lastSuspendedAfter;
    }

    public Date getCompletedBefore() {
        return completedBefore;
    }

    public void setCompletedBefore(Date completedBefore) {
        this.completedBefore = completedBefore;
    }

    public Date getCompletedAfter() {
        return completedAfter;
    }

    public void setCompletedAfter(Date completedAfter) {
        this.completedAfter = completedAfter;
    }

    public Date getTerminatedBefore() {
        return terminatedBefore;
    }

    public void setTerminatedBefore(Date terminatedBefore) {
        this.terminatedBefore = terminatedBefore;
    }

    public Date getTerminatedAfter() {
        return terminatedAfter;
    }

    public void setTerminatedAfter(Date terminatedAfter) {
        this.terminatedAfter = terminatedAfter;
    }

    public Date getOccurredBefore() {
        return occurredBefore;
    }

    public void setOccurredBefore(Date occurredBefore) {
        this.occurredBefore = occurredBefore;
    }

    public Date getOccurredAfter() {
        return occurredAfter;
    }

    public void setOccurredAfter(Date occurredAfter) {
        this.occurredAfter = occurredAfter;
    }

    public Date getExitBefore() {
        return exitBefore;
    }

    public void setExitBefore(Date exitBefore) {
        this.exitBefore = exitBefore;
    }

    public Date getExitAfter() {
        return exitAfter;
    }

    public void setExitAfter(Date exitAfter) {
        this.exitAfter = exitAfter;
    }

    public Date getEndedBefore() {
        return endedBefore;
    }

    public void setEndedBefore(Date endedBefore) {
        this.endedBefore = endedBefore;
    }

    public Date getEndedAfter() {
        return endedAfter;
    }

    public void setEndedAfter(Date endedAfter) {
        this.endedAfter = endedAfter;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Boolean getWithoutTenantId() {
        return withoutTenantId;
    }

    public void setWithoutTenantId(Boolean withoutTenantId) {
        this.withoutTenantId = withoutTenantId;
    }
}
