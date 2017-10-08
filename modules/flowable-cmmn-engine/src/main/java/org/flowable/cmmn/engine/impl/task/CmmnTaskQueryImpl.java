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
package org.flowable.cmmn.engine.impl.task;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.engine.impl.variable.VariableScopeType;
import org.flowable.cmmn.engine.task.TaskQuery;
import org.flowable.engine.common.api.query.QueryProperty;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.task.service.Task;
import org.flowable.task.service.impl.TaskQueryImpl;

/**
 * @author Joram Barrez
 */
public class CmmnTaskQueryImpl implements TaskQuery {
    
    protected TaskQueryImpl wrappedTaskQuery;
    
    public CmmnTaskQueryImpl(CommandExecutor commandExecutor) {
        this.wrappedTaskQuery = new TaskQueryImpl(commandExecutor);
    }

    @Override
    public TaskQuery taskId(String taskId) {
        wrappedTaskQuery.taskId(taskId);
        return this;
    }

    @Override
    public TaskQuery taskName(String name) {
        wrappedTaskQuery.taskName(name);
        return this;
    }

    @Override
    public TaskQuery taskNameIn(List<String> nameList) {
        wrappedTaskQuery.taskNameIn(nameList);
        return this;
    }

    @Override
    public TaskQuery taskNameInIgnoreCase(List<String> nameList) {
        wrappedTaskQuery.taskNameInIgnoreCase(nameList);
        return this;
    }

    @Override
    public TaskQuery taskNameLike(String nameLike) {
        wrappedTaskQuery.taskNameLike(nameLike);
        return this;
    }

    @Override
    public TaskQuery taskNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        wrappedTaskQuery.taskNameLikeIgnoreCase(nameLikeIgnoreCase);
        return this;
    }

    @Override
    public TaskQuery taskDescription(String description) {
        wrappedTaskQuery.taskDescription(description);
        return this;
    }

    @Override
    public TaskQuery taskDescriptionLike(String descriptionLike) {
        wrappedTaskQuery.taskDescriptionLike(descriptionLike);
        return this;
    }

    @Override
    public TaskQuery taskDescriptionLikeIgnoreCase(String descriptionLikeIgnoreCase) {
        wrappedTaskQuery.taskDescriptionLikeIgnoreCase(descriptionLikeIgnoreCase);
        return this;
    }

    @Override
    public TaskQuery taskPriority(Integer priority) {
        wrappedTaskQuery.taskPriority(priority);
        return this;
    }

    @Override
    public TaskQuery taskMinPriority(Integer minPriority) {
        wrappedTaskQuery.taskMinPriority(minPriority);
        return this;
    }

    @Override
    public TaskQuery taskMaxPriority(Integer maxPriority) {
        wrappedTaskQuery.taskMaxPriority(maxPriority);
        return this;
    }

    @Override
    public TaskQuery taskAssignee(String assignee) {
        wrappedTaskQuery.taskAssignee(assignee);
        return this;
    }

    @Override
    public TaskQuery taskAssigneeLike(String assigneeLike) {
        wrappedTaskQuery.taskAssigneeLike(assigneeLike);
        return this;
    }

    @Override
    public TaskQuery taskAssigneeLikeIgnoreCase(String assigneeLikeIgnoreCase) {
        wrappedTaskQuery.taskAssigneeLikeIgnoreCase(assigneeLikeIgnoreCase);
        return this;
    }

    @Override
    public TaskQuery taskAssigneeIds(List<String> assigneeListIds) {
        wrappedTaskQuery.taskAssigneeIds(assigneeListIds);
        return this;
    }

    @Override
    public TaskQuery taskOwner(String owner) {
        wrappedTaskQuery.taskOwner(owner);
        return this;
    }

    @Override
    public TaskQuery taskOwnerLike(String ownerLike) {
        wrappedTaskQuery.taskOwnerLike(ownerLike);
        return this;
    }

    @Override
    public TaskQuery taskOwnerLikeIgnoreCase(String ownerLikeIgnoreCase) {
        wrappedTaskQuery.taskOwnerLikeIgnoreCase(ownerLikeIgnoreCase);
        return this;
    }

    @Override
    public TaskQuery taskCandidateUser(String candidateUser) {
        wrappedTaskQuery.taskCandidateUser(candidateUser);
        return this;
    }

    @Override
    public TaskQuery taskInvolvedUser(String involvedUser) {
        wrappedTaskQuery.taskInvolvedUser(involvedUser);
        return this;
    }

    @Override
    public TaskQuery taskCandidateGroup(String candidateGroup) {
        wrappedTaskQuery.taskCandidateGroup(candidateGroup);
        return this;
    }

    @Override
    public TaskQuery taskCandidateGroupIn(List<String> candidateGroups) {
        wrappedTaskQuery.taskCandidateGroupIn(candidateGroups);
        return this;
    }

    @Override
    public TaskQuery taskTenantId(String tenantId) {
        wrappedTaskQuery.taskTenantId(tenantId);
        return this;
    }

    @Override
    public TaskQuery taskTenantIdLike(String tenantIdLike) {
        wrappedTaskQuery.taskTenantIdLike(tenantIdLike);
        return this;
    }

    @Override
    public TaskQuery taskWithoutTenantId() {
        wrappedTaskQuery.taskWithoutTenantId();
        return this;
    }

    @Override
    public TaskQuery processInstanceId(String processInstanceId) {
        wrappedTaskQuery.processInstanceId(processInstanceId);
        return this;
    }

    @Override
    public TaskQuery processInstanceIdIn(List<String> processInstanceIds) {
        wrappedTaskQuery.processInstanceIdIn(processInstanceIds);
        return this;
    }

    @Override
    public TaskQuery processInstanceBusinessKey(String processInstanceBusinessKey) {
        wrappedTaskQuery.processInstanceBusinessKey(processInstanceBusinessKey);
        return this;
    }

    @Override
    public TaskQuery processInstanceBusinessKeyLike(String processInstanceBusinessKeyLike) {
        wrappedTaskQuery.processInstanceBusinessKeyLike(processInstanceBusinessKeyLike);
        return this;
    }

    @Override
    public TaskQuery processInstanceBusinessKeyLikeIgnoreCase(String processInstanceBusinessKeyLikeIgnoreCase) {
        wrappedTaskQuery.processInstanceBusinessKeyLikeIgnoreCase(processInstanceBusinessKeyLikeIgnoreCase);
        return this;
    }

    @Override
    public TaskQuery executionId(String executionId) {
        wrappedTaskQuery.executionId(executionId);
        return this;
    }

    @Override
    public TaskQuery scopeId(String scopeId) {
        wrappedTaskQuery.scopeId(scopeId);
        return this;
    }

    @Override
    public TaskQuery subScopeId(String subScopeId) {
        wrappedTaskQuery.subScopeId(subScopeId);
        return this;
    }

    @Override
    public TaskQuery scopeType(String scopeType) {
        wrappedTaskQuery.scopeType(scopeType);
        return this;
    }

    @Override
    public TaskQuery scopeDefinitionId(String scopeDefinitionId) {
        wrappedTaskQuery.scopeDefinitionId(scopeDefinitionId);
        return this;
    }

    @Override
    public TaskQuery taskCreatedOn(Date createTime) {
        wrappedTaskQuery.taskCreatedOn(createTime);
        return this;
    }

    @Override
    public TaskQuery taskCreatedBefore(Date before) {
        wrappedTaskQuery.taskCreatedBefore(before);
        return this;
    }

    @Override
    public TaskQuery taskCreatedAfter(Date after) {
        wrappedTaskQuery.taskCreatedAfter(after);
        return this;
    }

    @Override
    public TaskQuery taskCategory(String category) {
        wrappedTaskQuery.taskCategory(category);
        return this;
    }

    @Override
    public TaskQuery taskDefinitionKey(String key) {
        wrappedTaskQuery.taskDefinitionKey(key);
        return this;
    }

    @Override
    public TaskQuery taskDefinitionKeyLike(String keyLike) {
        wrappedTaskQuery.taskDefinitionKeyLike(keyLike);
        return this;
    }

    @Override
    public TaskQuery taskDueDate(Date dueDate) {
        wrappedTaskQuery.taskDueDate(dueDate);
        return this;
    }

    @Override
    public TaskQuery taskDueBefore(Date dueDate) {
        wrappedTaskQuery.taskDueBefore(dueDate);
        return this;
    }

    @Override
    public TaskQuery taskDueAfter(Date dueDate) {
        wrappedTaskQuery.taskDueAfter(dueDate);
        return this;
    }

    @Override
    public TaskQuery withoutTaskDueDate() {
        wrappedTaskQuery.withoutTaskDueDate();
        return this;
    }

    @Override
    public TaskQuery processDefinitionKey(String processDefinitionKey) {
        wrappedTaskQuery.processDefinitionKey(processDefinitionKey);
        return this;
    }

    @Override
    public TaskQuery processDefinitionKeyLike(String processDefinitionKeyLike) {
        wrappedTaskQuery.processDefinitionKeyLike(processDefinitionKeyLike);
        return this;
    }

    @Override
    public TaskQuery processDefinitionKeyLikeIgnoreCase(String processDefinitionKeyLikeIgnoreCase) {
        wrappedTaskQuery.processDefinitionKeyLikeIgnoreCase(processDefinitionKeyLikeIgnoreCase);
        return this;
    }

    @Override
    public TaskQuery processDefinitionKeyIn(List<String> processDefinitionKeys) {
        wrappedTaskQuery.processDefinitionKeyIn(processDefinitionKeys);
        return this;
    }

    @Override
    public TaskQuery processDefinitionId(String processDefinitionId) {
        wrappedTaskQuery.processDefinitionId(processDefinitionId);
        return this;
    }

    @Override
    public TaskQuery processDefinitionName(String processDefinitionName) {
        wrappedTaskQuery.processDefinitionName(processDefinitionName);
        return this;
    }

    @Override
    public TaskQuery processDefinitionNameLike(String processDefinitionNameLike) {
        wrappedTaskQuery.processDefinitionNameLike(processDefinitionNameLike);
        return this;
    }

    @Override
    public TaskQuery processCategoryIn(List<String> processCategoryInList) {
        wrappedTaskQuery.processCategoryIn(processCategoryInList);
        return this;
    }

    @Override
    public TaskQuery processCategoryNotIn(List<String> processCategoryNotInList) {
        wrappedTaskQuery.processCategoryNotIn(processCategoryNotInList);
        return this;
    }

    @Override
    public TaskQuery deploymentId(String deploymentId) {
        wrappedTaskQuery.deploymentId(deploymentId);
        return this;
    }



    @Override
    public TaskQuery deploymentIdIn(List<String> deploymentIds) {
        wrappedTaskQuery.deploymentIdIn(deploymentIds);
        return this;
    }



    @Override
    public TaskQuery taskVariableValueEquals(String variableName, Object variableValue) {
        wrappedTaskQuery.taskVariableValueEquals(variableName, variableValue);
        return this;
    }

    @Override
    public TaskQuery taskVariableValueEquals(Object variableValue) {
        wrappedTaskQuery.taskVariableValueEquals(variableValue);
        return this;        
    }

    @Override
    public TaskQuery taskVariableValueEqualsIgnoreCase(String name, String value) {
        wrappedTaskQuery.taskVariableValueEqualsIgnoreCase(name, value);
        return this;        
    }

    @Override
    public TaskQuery taskVariableValueNotEquals(String variableName, Object variableValue) {
        wrappedTaskQuery.taskVariableValueNotEquals(variableName, variableValue);
        return this;        
    }

    @Override
    public TaskQuery taskVariableValueNotEqualsIgnoreCase(String name, String value) {
        wrappedTaskQuery.taskVariableValueNotEqualsIgnoreCase(name, value);
        return this;
    }

    @Override
    public TaskQuery taskVariableValueGreaterThan(String name, Object value) {
        wrappedTaskQuery.taskVariableValueGreaterThan(name, value);
        return this;        
    }

    @Override
    public TaskQuery taskVariableValueGreaterThanOrEqual(String name, Object value) {
        wrappedTaskQuery.taskVariableValueGreaterThanOrEqual(name, value);
        return this;        
    }

    @Override
    public TaskQuery taskVariableValueLessThan(String name, Object value) {
        wrappedTaskQuery.taskVariableValueLessThan(name, value);
        return this;        
    }

    @Override
    public TaskQuery taskVariableValueLessThanOrEqual(String name, Object value) {
        wrappedTaskQuery.taskVariableValueLessThanOrEqual(name, value);
        return this;
    }

    @Override
    public TaskQuery taskVariableValueLike(String name, String value) {
        wrappedTaskQuery.taskVariableValueLike(name, value);
        return this;
    }

    @Override
    public TaskQuery taskVariableValueLikeIgnoreCase(String name, String value) {
        wrappedTaskQuery.taskVariableValueLikeIgnoreCase(name, value);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueEquals(String variableName, Object variableValue) {
        wrappedTaskQuery.processVariableValueEquals(variableName, variableValue);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueEquals(Object variableValue) {
        wrappedTaskQuery.processVariableValueEquals(variableValue);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueEqualsIgnoreCase(String name, String value) {
        wrappedTaskQuery.processVariableValueEqualsIgnoreCase(name, value);
        return this;
    }

    @Override
    public TaskQuery processVariableValueNotEquals(String variableName, Object variableValue) {
        wrappedTaskQuery.processVariableValueNotEquals(variableName, variableValue);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueNotEqualsIgnoreCase(String name, String value) {
        wrappedTaskQuery.processVariableValueNotEqualsIgnoreCase(name, value);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueGreaterThan(String name, Object value) {
        wrappedTaskQuery.processVariableValueGreaterThan(name, value);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueGreaterThanOrEqual(String name, Object value) {
        wrappedTaskQuery.processVariableValueGreaterThanOrEqual(name, value);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueLessThan(String name, Object value) {
        wrappedTaskQuery.processVariableValueLessThan(name, value);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueLessThanOrEqual(String name, Object value) {
        wrappedTaskQuery.processVariableValueGreaterThanOrEqual(name, value);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueLike(String name, String value) {
        wrappedTaskQuery.processVariableValueLike(name, value);
        return this;        
    }

    @Override
    public TaskQuery processVariableValueLikeIgnoreCase(String name, String value) {
        wrappedTaskQuery.getProcessDefinitionKeyLikeIgnoreCase();
        return this;        
    }

    @Override
    public TaskQuery includeTaskLocalVariables() {
        wrappedTaskQuery.includeTaskLocalVariables();
        return this;        
    }

    @Override
    public TaskQuery includeProcessVariables() {
        wrappedTaskQuery.includeProcessVariables();
        return this;        
    }

    @Override
    public TaskQuery limitTaskVariables(Integer taskVariablesLimit) {
        wrappedTaskQuery.limitTaskVariables(taskVariablesLimit);
        return this;        
    }

    @Override
    public TaskQuery includeIdentityLinks() {
        wrappedTaskQuery.includeIdentityLinks();
        return this;        
    }

    @Override
    public TaskQuery locale(String locale) {
        wrappedTaskQuery.locale(locale);
        return this;        
    }

    @Override
    public TaskQuery withLocalizationFallback() {
        wrappedTaskQuery.withLocalizationFallback();
        return this;        
    }

    @Override
    public TaskQuery or() {
        wrappedTaskQuery.or();
        return this;        
    }

    @Override
    public TaskQuery endOr() {
        wrappedTaskQuery.endOr();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskId() {
        wrappedTaskQuery.orderByTaskId();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskName() {
        wrappedTaskQuery.orderByTaskName();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskDescription() {
        wrappedTaskQuery.orderByTaskDescription();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskPriority() {
        wrappedTaskQuery.orderByTaskPriority();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskAssignee() {
        wrappedTaskQuery.orderByTaskAssignee();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskCreateTime() {
        wrappedTaskQuery.orderByTaskCreateTime();
        return this;        
    }

    @Override
    public TaskQuery orderByProcessInstanceId() {
        wrappedTaskQuery.orderByProcessInstanceId();
        return this;        
    }

    @Override
    public TaskQuery orderByExecutionId() {
        wrappedTaskQuery.orderByExecutionId();
        return this;        
    }

    @Override
    public TaskQuery orderByProcessDefinitionId() {
        wrappedTaskQuery.orderByProcessDefinitionId();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskDueDate() {
        wrappedTaskQuery.orderByTaskDueDate();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskOwner() {
        wrappedTaskQuery.orderByTaskOwner();
        return this;        
    }

    @Override
    public TaskQuery orderByTaskDefinitionKey() {
        wrappedTaskQuery.orderByTaskDefinitionKey();
        return this;        
    }

    @Override
    public TaskQuery orderByTenantId() {
        wrappedTaskQuery.orderByTenantId();
        return this;        
    }

    @Override
    public TaskQuery orderByDueDateNullsFirst() {
        wrappedTaskQuery.orderByDueDateNullsFirst();
        return this;        
    }

    @Override
    public TaskQuery orderByDueDateNullsLast() {
        wrappedTaskQuery.orderByDueDateNullsLast();
        return this;        
    }

    @Override
    public TaskQuery caseInstanceId(String caseInstanceId) {
        scopeId(caseInstanceId);
        scopeType(VariableScopeType.CMMN);
        return this;        
    }

    @Override
    public TaskQuery caseDefinitionId(String caseDefinitionId) {
        scopeDefinitionId(caseDefinitionId);
        scopeType(VariableScopeType.CMMN);
        return this;
    }

    @Override
    public TaskQuery planItemInstanceId(String planItemInstanceId) {
        subScopeId(planItemInstanceId);
        scopeType(VariableScopeType.CMMN);
        return this;        
    }

    @Override
    public TaskQuery asc() {
        wrappedTaskQuery.asc();
        return this;        
    }

    @Override
    public TaskQuery desc() {
        wrappedTaskQuery.desc();
        return this;        
    }

    @Override
    public TaskQuery orderBy(QueryProperty property) {
        wrappedTaskQuery.orderBy(property);
        return this;        
    }

    @Override
    public TaskQuery orderBy(QueryProperty property, org.flowable.engine.common.api.query.Query.NullHandlingOnOrder nullHandlingOnOrder) {
        wrappedTaskQuery.orderBy(property, nullHandlingOnOrder);
        return this;        
    }

    @Override
    public long count() {
        return wrappedTaskQuery.count();
    }

    @Override
    public Task singleResult() {
        return wrappedTaskQuery.singleResult();
    }

    @Override
    public List<Task> list() {
        return wrappedTaskQuery.list();
    }

    @Override
    public List<Task> listPage(int firstResult, int maxResults) {
        return wrappedTaskQuery.listPage(firstResult, maxResults);
    }
    
}
