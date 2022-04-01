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

package org.flowable.cmmn.rest.service.api.history.caze;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.common.rest.api.PaginateRequest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceQueryRequest extends PaginateRequest {

    private String caseInstanceId;
    private List<String> caseInstanceIds;
    private String caseDefinitionId;
    private String caseDefinitionKey;
    private String caseDefinitionCategory;
    private String caseDefinitionName;
    private String caseBusinessKey;
    private String caseInstanceBusinessKey;
    private String caseInstanceParentId;
    private String caseInstanceState;
    private String caseInstanceCallbackId;
    private String caseInstanceCallbackType;
    private String caseInstanceReferenceId;
    private String caseInstanceReferenceType;
    private Boolean finished;
    private String involvedUser;
    private Date finishedAfter;
    private Date finishedBefore;
    private Date startedAfter;
    private Date startedBefore;
    private String startedBy;
    private String lastReactivatedBy;
    private Date lastReactivatedBefore;
    private Date lastReactivatedAfter;
    private String activePlanItemDefinitionId;
    private Set<String> activePlanItemDefinitionIds;
    private Boolean includeCaseVariables;
    private List<QueryVariable> variables;
    private String tenantId;
    private Boolean withoutTenantId;

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public List<String> getCaseInstanceIds() {
        return caseInstanceIds;
    }

    public void setCaseInstanceIds(List<String> caseInstanceIds) {
        this.caseInstanceIds = caseInstanceIds;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public void setCaseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
    }
    
    public String getCaseDefinitionCategory() {
        return caseDefinitionCategory;
    }

    public void setCaseDefinitionCategory(String caseDefinitionCategory) {
        this.caseDefinitionCategory = caseDefinitionCategory;
    }

    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }

    public void setCaseDefinitionName(String caseDefinitionName) {
        this.caseDefinitionName = caseDefinitionName;
    }

    public String getCaseBusinessKey() {
        return caseBusinessKey;
    }

    public void setCaseBusinessKey(String caseBusinessKey) {
        this.caseBusinessKey = caseBusinessKey;
    }
    
    public String getCaseInstanceBusinessKey() {
        return caseInstanceBusinessKey;
    }

    public void setCaseInstanceBusinessKey(String caseInstanceBusinessKey) {
        this.caseInstanceBusinessKey = caseInstanceBusinessKey;
    }

    public String getCaseInstanceParentId() {
        return caseInstanceParentId;
    }

    public void setCaseInstanceParentId(String caseInstanceParentId) {
        this.caseInstanceParentId = caseInstanceParentId;
    }

    public String getCaseInstanceState() {
        return caseInstanceState;
    }

    public void setCaseInstanceState(String caseInstanceState) {
        this.caseInstanceState = caseInstanceState;
    }

    public String getCaseInstanceCallbackId() {
        return caseInstanceCallbackId;
    }

    public void setCaseInstanceCallbackId(String caseInstanceCallbackId) {
        this.caseInstanceCallbackId = caseInstanceCallbackId;
    }

    public String getCaseInstanceCallbackType() {
        return caseInstanceCallbackType;
    }

    public void setCaseInstanceCallbackType(String caseInstanceCallbackType) {
        this.caseInstanceCallbackType = caseInstanceCallbackType;
    }

    public String getCaseInstanceReferenceId() {
        return caseInstanceReferenceId;
    }

    public void setCaseInstanceReferenceId(String caseInstanceReferenceId) {
        this.caseInstanceReferenceId = caseInstanceReferenceId;
    }

    public String getCaseInstanceReferenceType() {
        return caseInstanceReferenceType;
    }

    public void setCaseInstanceReferenceType(String caseInstanceReferenceType) {
        this.caseInstanceReferenceType = caseInstanceReferenceType;
    }

    public Boolean getFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public void setInvolvedUser(String involvedUser) {
        this.involvedUser = involvedUser;
    }

    public Date getFinishedAfter() {
        return finishedAfter;
    }

    public void setFinishedAfter(Date finishedAfter) {
        this.finishedAfter = finishedAfter;
    }

    public Date getFinishedBefore() {
        return finishedBefore;
    }

    public void setFinishedBefore(Date finishedBefore) {
        this.finishedBefore = finishedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public void setStartedAfter(Date startedAfter) {
        this.startedAfter = startedAfter;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public void setStartedBefore(Date startedBefore) {
        this.startedBefore = startedBefore;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }
    
    public String getLastReactivatedBy() {
        return lastReactivatedBy;
    }

    public void setLastReactivatedBy(String lastReactivatedBy) {
        this.lastReactivatedBy = lastReactivatedBy;
    }

    public Date getLastReactivatedBefore() {
        return lastReactivatedBefore;
    }

    public void setLastReactivatedBefore(Date lastReactivatedBefore) {
        this.lastReactivatedBefore = lastReactivatedBefore;
    }

    public Date getLastReactivatedAfter() {
        return lastReactivatedAfter;
    }

    public void setLastReactivatedAfter(Date lastReactivatedAfter) {
        this.lastReactivatedAfter = lastReactivatedAfter;
    }

    public String getActivePlanItemDefinitionId() {
        return activePlanItemDefinitionId;
    }

    public void setActivePlanItemDefinitionId(String activePlanItemDefinitionId) {
        this.activePlanItemDefinitionId = activePlanItemDefinitionId;
    }

    public Set<String> getActivePlanItemDefinitionIds() {
        return activePlanItemDefinitionIds;
    }

    public void setActivePlanItemDefinitionIds(Set<String> activePlanItemDefinitionIds) {
        this.activePlanItemDefinitionIds = activePlanItemDefinitionIds;
    }

    public Boolean getIncludeCaseVariables() {
        return includeCaseVariables;
    }

    public void setIncludeCaseVariables(Boolean includeCaseVariables) {
        this.includeCaseVariables = includeCaseVariables;
    }

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = QueryVariable.class)
    public List<QueryVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<QueryVariable> variables) {
        this.variables = variables;
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
