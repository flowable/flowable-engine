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

package org.flowable.rest.service.api.history;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.flowable.common.rest.api.PaginateRequest;
import org.flowable.rest.service.api.engine.variable.QueryVariable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author Tijs Rademakers
 */
public class HistoricTaskInstanceQueryRequest extends PaginateRequest {

    protected String taskId;
    protected String processInstanceId;
    protected String processInstanceIdWithChildren;
    protected Boolean withoutProcessInstanceId;
    protected String processBusinessKey;
    protected String processBusinessKeyLike;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected String processDefinitionKeyLike;
    protected String processDefinitionName;
    protected String processDefinitionNameLike;
    protected String executionId;
    protected String taskName;
    protected String taskNameLike;
    protected String taskNameLikeIgnoreCase;
    protected String taskDescription;
    protected String taskDescriptionLike;
    protected String taskDefinitionKey;
    protected String taskDefinitionKeyLike;
    protected Collection<String> taskDefinitionKeys;
    protected String taskCategory;
    protected List<String> taskCategoryIn;
    protected List<String> taskCategoryNotIn;
    protected Boolean taskWithoutCategory;
    protected String taskDeleteReason;
    protected String taskDeleteReasonLike;
    protected String taskAssignee;
    protected String taskAssigneeLike;
    protected String taskOwner;
    protected String taskOwnerLike;
    protected String taskInvolvedUser;
    protected Integer taskPriority;
    protected Integer taskMinPriority;
    protected Integer taskMaxPriority;
    protected Boolean finished;
    protected Boolean processFinished;
    protected String parentTaskId;
    protected Date dueDate;
    protected Date dueDateAfter;
    protected Date dueDateBefore;
    protected Boolean withoutDueDate;
    protected Date taskCreatedOn;
    protected Date taskCreatedBefore;
    protected Date taskCreatedAfter;
    protected Date taskCompletedOn;
    protected Date taskCompletedBefore;
    protected Date taskCompletedAfter;
    protected Boolean includeTaskLocalVariables;
    protected Boolean includeProcessVariables;
    protected List<QueryVariable> taskVariables;
    protected List<QueryVariable> processVariables;
    protected String scopeDefinitionId;
    protected String scopeId;
    protected Boolean withoutScopeId;
    protected String scopeType;
    protected String propagatedStageInstanceId;
    protected String tenantId;
    protected String tenantIdLike;
    protected Boolean withoutTenantId;
    protected Boolean withoutDeleteReason;
    protected String taskCandidateGroup;
    protected boolean ignoreTaskAssignee;
    protected String rootScopeId;
    protected String parentScopeId;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public String getProcessBusinessKey() {
        return processBusinessKey;
    }

    public String getProcessBusinessKeyLike() {
        return processBusinessKeyLike;
    }

    public void setProcessBusinessKeyLike(String processBusinessKeyLike) {
        this.processBusinessKeyLike = processBusinessKeyLike;
    }

    public void setProcessBusinessKey(String processBusinessKey) {
        this.processBusinessKey = processBusinessKey;
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

    public String getProcessDefinitionKeyLike() {
        return processDefinitionKeyLike;
    }

    public void setProcessDefinitionKeyLike(String processDefinitionKeyLike) {
        this.processDefinitionKeyLike = processDefinitionKeyLike;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public String getProcessDefinitionNameLike() {
        return processDefinitionNameLike;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setProcessDefinitionNameLike(String processDefinitionNameLike) {
        this.processDefinitionNameLike = processDefinitionNameLike;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskNameLike() {
        return taskNameLike;
    }

    public void setTaskNameLike(String taskNameLike) {
        this.taskNameLike = taskNameLike;
    }

    public String getTaskNameLikeIgnoreCase() {
        return taskNameLikeIgnoreCase;
    }

    public void setTaskNameLikeIgnoreCase(String taskNameLikeIgnoreCase) {
        this.taskNameLikeIgnoreCase = taskNameLikeIgnoreCase;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getTaskDescriptionLike() {
        return taskDescriptionLike;
    }

    public void setTaskDescriptionLike(String taskDescriptionLike) {
        this.taskDescriptionLike = taskDescriptionLike;
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

    public String getTaskCategory() {
        return taskCategory;
    }

    public void setTaskCategory(String taskCategory) {
        this.taskCategory = taskCategory;
    }

    public List<String> getTaskCategoryIn() {
        return taskCategoryIn;
    }

    public void setTaskCategoryIn(List<String> taskCategoryIn) {
        this.taskCategoryIn = taskCategoryIn;
    }

    public List<String> getTaskCategoryNotIn() {
        return taskCategoryNotIn;
    }

    public void setTaskCategoryNotIn(List<String> taskCategoryNotIn) {
        this.taskCategoryNotIn = taskCategoryNotIn;
    }

    public Boolean getTaskWithoutCategory() {
        return taskWithoutCategory;
    }

    public void setTaskWithoutCategory(Boolean taskWithoutCategory) {
        this.taskWithoutCategory = taskWithoutCategory;
    }

    public String getTaskDeleteReason() {
        return taskDeleteReason;
    }

    public void setTaskDeleteReason(String taskDeleteReason) {
        this.taskDeleteReason = taskDeleteReason;
    }

    public String getTaskDeleteReasonLike() {
        return taskDeleteReasonLike;
    }

    public void setTaskDeleteReasonLike(String taskDeleteReasonLike) {
        this.taskDeleteReasonLike = taskDeleteReasonLike;
    }

    public String getTaskAssignee() {
        return taskAssignee;
    }

    public void setTaskAssignee(String taskAssignee) {
        this.taskAssignee = taskAssignee;
    }

    public String getTaskAssigneeLike() {
        return taskAssigneeLike;
    }

    public void setTaskAssigneeLike(String taskAssigneeLike) {
        this.taskAssigneeLike = taskAssigneeLike;
    }

    public String getTaskOwner() {
        return taskOwner;
    }

    public void setTaskOwner(String taskOwner) {
        this.taskOwner = taskOwner;
    }

    public String getTaskOwnerLike() {
        return taskOwnerLike;
    }

    public void setTaskOwnerLike(String taskOwnerLike) {
        this.taskOwnerLike = taskOwnerLike;
    }

    public String getTaskInvolvedUser() {
        return taskInvolvedUser;
    }

    public void setTaskInvolvedUser(String taskInvolvedUser) {
        this.taskInvolvedUser = taskInvolvedUser;
    }

    public Integer getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(Integer taskPriority) {
        this.taskPriority = taskPriority;
    }

    public Integer getTaskMaxPriority() {
        return taskMaxPriority;
    }

    public void setTaskMaxPriority(Integer taskMaxPriority) {
        this.taskMaxPriority = taskMaxPriority;
    }

    public Integer getTaskMinPriority() {
        return taskMinPriority;
    }

    public void setTaskMinPriority(Integer taskMinPriority) {
        this.taskMinPriority = taskMinPriority;
    }

    public Boolean getFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    public Boolean getProcessFinished() {
        return processFinished;
    }

    public void setProcessFinished(Boolean processFinished) {
        this.processFinished = processFinished;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getDueDateAfter() {
        return dueDateAfter;
    }

    public void setDueDateAfter(Date dueDateAfter) {
        this.dueDateAfter = dueDateAfter;
    }

    public Date getDueDateBefore() {
        return dueDateBefore;
    }

    public void setDueDateBefore(Date dueDateBefore) {
        this.dueDateBefore = dueDateBefore;
    }

    public Boolean getWithoutDueDate() {
        return withoutDueDate;
    }

    public void setWithoutDueDate(Boolean withoutDueDate) {
        this.withoutDueDate = withoutDueDate;
    }

    public Date getTaskCreatedOn() {
        return taskCreatedOn;
    }

    public void setTaskCreatedOn(Date taskCreatedOn) {
        this.taskCreatedOn = taskCreatedOn;
    }

    public void setTaskCreatedAfter(Date taskCreatedAfter) {
        this.taskCreatedAfter = taskCreatedAfter;
    }

    public Date getTaskCompletedAfter() {
        return taskCompletedAfter;
    }

    public void setTaskCompletedAfter(Date taskCompletedAfter) {
        this.taskCompletedAfter = taskCompletedAfter;
    }

    public Date getTaskCompletedBefore() {
        return taskCompletedBefore;
    }

    public void setTaskCompletedBefore(Date taskCompletedBefore) {
        this.taskCompletedBefore = taskCompletedBefore;
    }

    public Date getTaskCompletedOn() {
        return taskCompletedOn;
    }

    public void setTaskCompletedOn(Date taskCompletedOn) {
        this.taskCompletedOn = taskCompletedOn;
    }

    public Date getTaskCreatedAfter() {
        return taskCreatedAfter;
    }

    public void setTaskCreatedBefore(Date taskCreatedBefore) {
        this.taskCreatedBefore = taskCreatedBefore;
    }

    public Date getTaskCreatedBefore() {
        return taskCreatedBefore;
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
    public List<QueryVariable> getProcessVariables() {
        return processVariables;
    }

    public void setProcessVariables(List<QueryVariable> processVariables) {
        this.processVariables = processVariables;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public void setTenantIdLike(String tenantIdLike) {
        this.tenantIdLike = tenantIdLike;
    }

    public Boolean getWithoutTenantId() {
        return withoutTenantId;
    }

    public void setWithoutTenantId(Boolean withoutTenantId) {
        this.withoutTenantId = withoutTenantId;
    }

    public Boolean getWithoutDeleteReason() {
        return withoutDeleteReason;
    }

    public void setWithoutDeleteReason(Boolean withoutDeleteReason) {
        this.withoutDeleteReason = withoutDeleteReason;
    }

    public String getTaskCandidateGroup() {
        return taskCandidateGroup;
    }

    public void setTaskCandidateGroup(String taskCandidateGroup) {
        this.taskCandidateGroup = taskCandidateGroup;
    }

    public boolean isIgnoreTaskAssignee() {
        return ignoreTaskAssignee;
    }

    public void setIgnoreTaskAssignee(boolean ignoreTaskAssignee) {
        this.ignoreTaskAssignee = ignoreTaskAssignee;
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

}
