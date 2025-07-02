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
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.common.rest.api.PaginateRequest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author Tijs Rademakers
 */
public class PlanItemInstanceQueryRequest extends PaginateRequest {
    
    private String id;
    private String elementId;
    private String name;
    private String caseInstanceId;
    private Set<String> caseInstanceIds;
    private String caseDefinitionId;
    private String stageInstanceId;
    private String planItemDefinitionId;
    private String planItemDefinitionType;
    private List<String> planItemDefinitionTypes;
    private String state;
    private Date createdBefore;
    private Date createdAfter;
    private String startUserId;
    private String referenceId;
    private String referenceType;
    private Boolean includeEnded;
    private Boolean includeLocalVariables;
    private List<QueryVariable> variables;
    private List<QueryVariable> caseInstanceVariables;
    private String tenantId;
    private Boolean withoutTenantId;

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = QueryVariable.class)
    public List<QueryVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<QueryVariable> variables) {
        this.variables = variables;
    }

    public List<QueryVariable> getCaseInstanceVariables() {
        return caseInstanceVariables;
    }

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = QueryVariable.class)
    public void setCaseInstanceVariables(List<QueryVariable> caseInstanceVariables) {
        this.caseInstanceVariables = caseInstanceVariables;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
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

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getStageInstanceId() {
        return stageInstanceId;
    }

    public void setStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
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

    public List<String> getPlanItemDefinitionTypes() {
        return planItemDefinitionTypes;
    }

    public void setPlanItemDefinitionTypes(List<String> planItemDefinitionTypes) {
        this.planItemDefinitionTypes = planItemDefinitionTypes;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public Boolean getIncludeEnded() {
        return includeEnded;
    }

    public void setIncludeEnded(Boolean includeEnded) {
        this.includeEnded = includeEnded;
    }

    public Boolean getIncludeLocalVariables() {
        return includeLocalVariables;
    }

    public void setIncludeLocalVariables(boolean includeLocalVariables) {
        this.includeLocalVariables = includeLocalVariables;
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

    public Set<String> getCaseInstanceIds() {
        return caseInstanceIds;
    }

    public void setCaseInstanceIds(Set<String> caseInstanceIds) {
        this.caseInstanceIds = caseInstanceIds;
    }
}
