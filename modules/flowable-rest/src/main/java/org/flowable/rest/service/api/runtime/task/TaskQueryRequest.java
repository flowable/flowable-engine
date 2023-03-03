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

package org.flowable.rest.service.api.runtime.task;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.flowable.common.rest.api.PaginateRequest;
import org.flowable.rest.service.api.engine.variable.QueryVariable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author Frederik Heremans
 */
public class TaskQueryRequest extends PaginateRequest {

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
    protected List<String> candidateGroupIn;
    protected boolean ignoreAssignee;
    protected String involvedUser;
    protected String processInstanceId;
    protected String processInstanceIdWithChildren;
    protected Boolean withoutProcessInstanceId;
    protected String processInstanceBusinessKey;
    protected String processInstanceBusinessKeyLike;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected String processDefinitionName;
    protected String processDefinitionKeyLike;
    protected String processDefinitionNameLike;
    protected String executionId;
    protected Date createdOn;
    protected Date createdBefore;
    protected Date createdAfter;
    protected Boolean excludeSubTasks;
    protected String taskDefinitionKey;
    protected String taskDefinitionKeyLike;
    protected Collection<String> taskDefinitionKeys;
    protected Date dueDate;
    protected Date dueBefore;
    protected Date dueAfter;
    protected Boolean withoutDueDate;
    protected Boolean active;
    protected Boolean includeTaskLocalVariables;
    protected Boolean includeProcessVariables;
    protected String scopeDefinitionId;
    protected String scopeId;
    protected Boolean withoutScopeId;
    protected String scopeType;
    protected String propagatedStageInstanceId;
    protected String tenantId;
    protected String tenantIdLike;
    protected Boolean withoutTenantId;
    protected String candidateOrAssigned;
    protected String category;
    protected List<String> categoryIn;
    protected List<String> categoryNotIn;
    protected Boolean withoutCategory;

    private List<QueryVariable> taskVariables;
    private List<QueryVariable> processInstanceVariables;

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

    public List<String> getCandidateGroupIn() {
        return candidateGroupIn;
    }

    public void setCandidateGroupIn(List<String> candidateGroupIn) {
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

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceIdWithChildren() {
        return processInstanceIdWithChildren;
    }

    public void setProcessInstanceIdWithChildren(String processInstanceIdWithChildren) {
        this.processInstanceIdWithChildren = processInstanceIdWithChildren;
    }

    public Boolean getWithoutProcessInstanceId() {
        return withoutProcessInstanceId;
    }

    public void setWithoutProcessInstanceId(Boolean withoutProcessInstanceId) {
        this.withoutProcessInstanceId = withoutProcessInstanceId;
    }

    public String getProcessInstanceBusinessKey() {
        return processInstanceBusinessKey;
    }

    public void setProcessInstanceBusinessKey(String processInstanceBusinessKey) {
        this.processInstanceBusinessKey = processInstanceBusinessKey;
    }

    public String getProcessInstanceBusinessKeyLike() {
        return processInstanceBusinessKeyLike;
    }

    public void setProcessInstanceBusinessKeyLike(String processInstanceBusinessKeyLike) {
        this.processInstanceBusinessKeyLike = processInstanceBusinessKeyLike;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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

    public Collection<String> getTaskDefinitionKeys() {
        return taskDefinitionKeys;
    }

    public void setTaskDefinitionKeys(Collection<String> taskDefinitionKeys) {
        this.taskDefinitionKeys = taskDefinitionKeys;
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

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = QueryVariable.class)
    public List<QueryVariable> getProcessInstanceVariables() {
        return processInstanceVariables;
    }

    public void setProcessInstanceVariables(List<QueryVariable> processInstanceVariables) {
        this.processInstanceVariables = processInstanceVariables;
    }

    public void setProcessDefinitionNameLike(String processDefinitionNameLike) {
        this.processDefinitionNameLike = processDefinitionNameLike;
    }

    public String getProcessDefinitionNameLike() {
        return processDefinitionNameLike;
    }

    public String getProcessDefinitionKeyLike() {
        return processDefinitionKeyLike;
    }

    public void setProcessDefinitionKeyLike(String processDefinitionKeyLike) {
        this.processDefinitionKeyLike = processDefinitionKeyLike;
    }

    public void setWithoutDueDate(Boolean withoutDueDate) {
        this.withoutDueDate = withoutDueDate;
    }

    public Boolean getWithoutDueDate() {
        return withoutDueDate;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }
    
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
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

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getPropagatedStageInstanceId() {
        return propagatedStageInstanceId;
    }

    public void setPropagatedStageInstanceId(String propagatedStageInstanceId) {
        this.propagatedStageInstanceId = propagatedStageInstanceId;
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
}
