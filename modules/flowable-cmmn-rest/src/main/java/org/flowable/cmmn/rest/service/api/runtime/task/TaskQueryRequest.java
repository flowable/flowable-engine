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

package org.flowable.cmmn.rest.service.api.runtime.task;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.rest.service.api.engine.variable.QueryVariable;
import org.flowable.common.rest.api.PaginateRequest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author Frederik Heremans
 */
public class TaskQueryRequest extends PaginateRequest {

    protected String taskId;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String description;
    protected String descriptionLike;
    protected Integer priority;
    protected Integer minimumPriority;
    protected Integer maximumPriority;
    protected String assignee;
    protected String assigneeLike;
    protected String owner;
    protected String ownerLike;
    protected Boolean unassigned;
    protected String delegationState;
    protected String candidateUser;
    protected String candidateGroup;
    protected Collection<String> candidateGroupIn;
    protected boolean ignoreAssignee;
    protected String involvedUser;
    protected String caseInstanceId;
    protected String caseInstanceIdWithChildren;
    protected String planItemInstanceId;
    protected String propagatedStageInstanceId;
    protected String scopeId;
    protected Set<String> scopeIds;
    protected Boolean withoutScopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String caseDefinitionId;
    protected String caseDefinitionKey;
    protected String caseDefinitionKeyLike;
    protected String caseDefinitionKeyLikeIgnoreCase;
    protected Collection<String> caseDefinitionKeys;
    protected Date createdOn;
    protected Date createdBefore;
    protected Date createdAfter;
    protected Boolean excludeSubTasks;
    protected String taskDefinitionKey;
    protected String taskDefinitionKeyLike;
    protected Date dueDate;
    protected Date dueBefore;
    protected Date dueAfter;
    protected Boolean withoutDueDate;
    protected Boolean active;
    protected Boolean includeTaskLocalVariables;
    protected Boolean includeProcessVariables;
    protected String tenantId;
    protected String tenantIdLike;
    protected Boolean withoutTenantId;
    protected Boolean withoutProcessInstanceId;
    protected String candidateOrAssigned;
    protected String category;
    protected String rootScopeId;
    protected String parentScopeId;
    private List<QueryVariable> taskVariables;
    protected List<String> categoryIn;
    protected List<String> categoryNotIn;
    protected Boolean withoutCategory;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public void setNameLike(String nameLike) {
        this.nameLike = nameLike;
    }

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }

    public void setNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        this.nameLikeIgnoreCase = nameLikeIgnoreCase;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionLike() {
        return descriptionLike;
    }

    public void setDescriptionLike(String descriptionLike) {
        this.descriptionLike = descriptionLike;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getMinimumPriority() {
        return minimumPriority;
    }

    public void setMinimumPriority(Integer minimumPriority) {
        this.minimumPriority = minimumPriority;
    }

    public Integer getMaximumPriority() {
        return maximumPriority;
    }

    public void setMaximumPriority(Integer maximumPriority) {
        this.maximumPriority = maximumPriority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getAssigneeLike() {
        return assigneeLike;
    }

    public void setAssigneeLike(String assigneeLike) {
        this.assigneeLike = assigneeLike;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwnerLike() {
        return ownerLike;
    }

    public void setOwnerLike(String ownerLike) {
        this.ownerLike = ownerLike;
    }

    public Boolean getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(Boolean unassigned) {
        this.unassigned = unassigned;
    }

    public String getDelegationState() {
        return delegationState;
    }

    public void setDelegationState(String delegationState) {
        this.delegationState = delegationState;
    }

    public String getCandidateUser() {
        return candidateUser;
    }

    public void setCandidateUser(String candidateUser) {
        this.candidateUser = candidateUser;
    }

    public String getCandidateGroup() {
        return candidateGroup;
    }

    public void setCandidateGroup(String candidateGroup) {
        this.candidateGroup = candidateGroup;
    }

    public Collection<String> getCandidateGroupIn() {
        return candidateGroupIn;
    }

    public void setCandidateGroupIn(Collection<String> candidateGroupIn) {
        this.candidateGroupIn = candidateGroupIn;
    }

    public boolean isIgnoreAssignee() {
        return ignoreAssignee;
    }

    public void setIgnoreAssignee(boolean ignoreAssignee) {
        this.ignoreAssignee = ignoreAssignee;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public void setInvolvedUser(String involvedUser) {
        this.involvedUser = involvedUser;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
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

    public Boolean getExcludeSubTasks() {
        return excludeSubTasks;
    }

    public void setExcludeSubTasks(Boolean excludeSubTasks) {
        this.excludeSubTasks = excludeSubTasks;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getTaskDefinitionKeyLike() {
        return taskDefinitionKeyLike;
    }

    public void setTaskDefinitionKeyLike(String taskDefinitionKeyLike) {
        this.taskDefinitionKeyLike = taskDefinitionKeyLike;
    }
    
    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }

    public void setPlanItemInstanceId(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    public String getPropagatedStageInstanceId() {
        return propagatedStageInstanceId;
    }

    public void setPropagatedStageInstanceId(String propagatedStageInstanceId) {
        this.propagatedStageInstanceId = propagatedStageInstanceId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public Boolean getWithoutScopeId() {
        return withoutScopeId;
    }

    public void setWithoutScopeId(Boolean withoutScopeId) {
        this.withoutScopeId = withoutScopeId;
    }

    public String getSubScopeId() {
        return subScopeId;
    }

    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getCaseInstanceIdWithChildren() {
        return caseInstanceIdWithChildren;
    }

    public void setCaseInstanceIdWithChildren(String caseInstanceIdWithChildren) {
        this.caseInstanceIdWithChildren = caseInstanceIdWithChildren;
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

    public String getCaseDefinitionKeyLike() {
        return caseDefinitionKeyLike;
    }

    public void setCaseDefinitionKeyLike(String caseDefinitionKeyLike) {
        this.caseDefinitionKeyLike = caseDefinitionKeyLike;
    }

    public String getCaseDefinitionKeyLikeIgnoreCase() {
        return caseDefinitionKeyLikeIgnoreCase;
    }

    public void setCaseDefinitionKeyLikeIgnoreCase(String caseDefinitionKeyLikeIgnoreCase) {
        this.caseDefinitionKeyLikeIgnoreCase = caseDefinitionKeyLikeIgnoreCase;
    }
    public Collection<String> getCaseDefinitionKeys() {
        return caseDefinitionKeys;
    }
    public void setCaseDefinitionKeys(Collection<String> caseDefinitionKeys) {
        this.caseDefinitionKeys = caseDefinitionKeys;
    }
    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getDueBefore() {
        return dueBefore;
    }

    public void setDueBefore(Date dueBefore) {
        this.dueBefore = dueBefore;
    }

    public Date getDueAfter() {
        return dueAfter;
    }

    public void setDueAfter(Date dueAfter) {
        this.dueAfter = dueAfter;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getIncludeTaskLocalVariables() {
        return includeTaskLocalVariables;
    }

    public void setIncludeTaskLocalVariables(Boolean includeTaskLocalVariables) {
        this.includeTaskLocalVariables = includeTaskLocalVariables;
    }

    public Boolean getIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public void setIncludeProcessVariables(Boolean includeProcessVariables) {
        this.includeProcessVariables = includeProcessVariables;
    }

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = QueryVariable.class)
    public List<QueryVariable> getTaskVariables() {
        return taskVariables;
    }

    public void setTaskVariables(List<QueryVariable> taskVariables) {
        this.taskVariables = taskVariables;
    }

    public void setWithoutDueDate(Boolean withoutDueDate) {
        this.withoutDueDate = withoutDueDate;
    }

    public Boolean getWithoutDueDate() {
        return withoutDueDate;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantIdLike(String tenantIdLike) {
        this.tenantIdLike = tenantIdLike;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public void setWithoutTenantId(Boolean withoutTenantId) {
        this.withoutTenantId = withoutTenantId;
    }

    public Boolean getWithoutTenantId() {
        return withoutTenantId;
    }

    public Boolean getWithoutProcessInstanceId() {
        return withoutProcessInstanceId;
    }

    public void setWithoutProcessInstanceId(Boolean withoutProcessInstanceId) {
        this.withoutProcessInstanceId = withoutProcessInstanceId;
    }

    public String getCandidateOrAssigned() {
        return candidateOrAssigned;
    }

    public void setCandidateOrAssigned(String candidateOrAssigned) {
        this.candidateOrAssigned = candidateOrAssigned;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getCategoryIn() {
        return categoryIn;
    }

    public void setCategoryIn(List<String> categoryIn) {
        this.categoryIn = categoryIn;
    }

    public List<String> getCategoryNotIn() {
        return categoryNotIn;
    }

    public void setCategoryNotIn(List<String> categoryNotIn) {
        this.categoryNotIn = categoryNotIn;
    }

    public Boolean getWithoutCategory() {
        return withoutCategory;
    }

    public void setWithoutCategory(Boolean withoutCategory) {
        this.withoutCategory = withoutCategory;
    }

    public String getRootScopeId() {
        return rootScopeId;
    }

    public void setRootScopeId(String rootScopeId) {
        this.rootScopeId = rootScopeId;
    }

    public String getParentScopeId() {
        return parentScopeId;
    }

    public void setParentScopeId(String parentScopeId) {
        this.parentScopeId = parentScopeId;
    }

    public Set<String> getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(Set<String> scopeIds) {
        this.scopeIds = scopeIds;
    }
}
