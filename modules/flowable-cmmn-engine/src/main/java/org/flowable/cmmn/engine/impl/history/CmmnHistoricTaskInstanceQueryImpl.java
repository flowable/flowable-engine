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
package org.flowable.cmmn.engine.impl.history;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.engine.history.HistoricTaskInstanceQuery;
import org.flowable.cmmn.engine.impl.variable.VariableScopeType;
import org.flowable.engine.common.api.query.QueryProperty;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.task.service.history.HistoricTaskInstance;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;

/**
 * @author Joram Barrez
 */
public class CmmnHistoricTaskInstanceQueryImpl implements HistoricTaskInstanceQuery {
    
    protected HistoricTaskInstanceQueryImpl wrappedTaskQuery;
    
    public CmmnHistoricTaskInstanceQueryImpl(CommandExecutor commandExecutor) {
        this.wrappedTaskQuery = new HistoricTaskInstanceQueryImpl(commandExecutor);
    }

    @Override
    public HistoricTaskInstanceQuery taskId(String taskId) {
        wrappedTaskQuery.taskId(taskId);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskName(String name) {
        wrappedTaskQuery.taskName(name);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskNameIn(List<String> nameList) {
        wrappedTaskQuery.taskNameIn(nameList);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskNameInIgnoreCase(List<String> nameList) {
        wrappedTaskQuery.taskNameInIgnoreCase(nameList);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskNameLike(String nameLike) {
        wrappedTaskQuery.taskNameLike(nameLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        wrappedTaskQuery.taskNameLikeIgnoreCase(nameLikeIgnoreCase);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDescription(String description) {
        wrappedTaskQuery.taskDescription(description);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDescriptionLike(String descriptionLike) {
        wrappedTaskQuery.taskDescriptionLike(descriptionLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDescriptionLikeIgnoreCase(String descriptionLikeIgnoreCase) {
        wrappedTaskQuery.taskDescriptionLikeIgnoreCase(descriptionLikeIgnoreCase);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskPriority(Integer priority) {
        wrappedTaskQuery.taskPriority(priority);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskMinPriority(Integer minPriority) {
        wrappedTaskQuery.taskMinPriority(minPriority);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskMaxPriority(Integer maxPriority) {
        wrappedTaskQuery.taskMaxPriority(maxPriority);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssignee(String assignee) {
        wrappedTaskQuery.taskAssignee(assignee);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssigneeLike(String assigneeLike) {
        wrappedTaskQuery.taskAssigneeLike(assigneeLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssigneeLikeIgnoreCase(String assigneeLikeIgnoreCase) {
        wrappedTaskQuery.taskAssigneeLikeIgnoreCase(assigneeLikeIgnoreCase);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskAssigneeIds(List<String> assigneeListIds) {
        wrappedTaskQuery.taskAssigneeIds(assigneeListIds);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskOwner(String owner) {
        wrappedTaskQuery.taskOwner(owner);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskOwnerLike(String ownerLike) {
        wrappedTaskQuery.taskOwnerLike(ownerLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskOwnerLikeIgnoreCase(String ownerLikeIgnoreCase) {
        wrappedTaskQuery.taskOwnerLikeIgnoreCase(ownerLikeIgnoreCase);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCandidateUser(String candidateUser) {
        wrappedTaskQuery.taskCandidateUser(candidateUser);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskInvolvedUser(String involvedUser) {
        wrappedTaskQuery.taskInvolvedUser(involvedUser);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCandidateGroup(String candidateGroup) {
        wrappedTaskQuery.taskCandidateGroup(candidateGroup);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCandidateGroupIn(List<String> candidateGroups) {
        wrappedTaskQuery.taskCandidateGroupIn(candidateGroups);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskTenantId(String tenantId) {
        wrappedTaskQuery.taskTenantId(tenantId);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskTenantIdLike(String tenantIdLike) {
        wrappedTaskQuery.taskTenantIdLike(tenantIdLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskWithoutTenantId() {
        wrappedTaskQuery.taskWithoutTenantId();
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processInstanceId(String processInstanceId) {
        wrappedTaskQuery.processInstanceId(processInstanceId);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processInstanceIdIn(List<String> processInstanceIds) {
        wrappedTaskQuery.processInstanceIdIn(processInstanceIds);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processInstanceBusinessKey(String processInstanceBusinessKey) {
        wrappedTaskQuery.processInstanceBusinessKey(processInstanceBusinessKey);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processInstanceBusinessKeyLike(String processInstanceBusinessKeyLike) {
        wrappedTaskQuery.processInstanceBusinessKeyLike(processInstanceBusinessKeyLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processInstanceBusinessKeyLikeIgnoreCase(String processInstanceBusinessKeyLikeIgnoreCase) {
        wrappedTaskQuery.processInstanceBusinessKeyLikeIgnoreCase(processInstanceBusinessKeyLikeIgnoreCase);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery executionId(String executionId) {
        wrappedTaskQuery.executionId(executionId);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery scopeId(String scopeId) {
        wrappedTaskQuery.scopeId(scopeId);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery subScopeId(String subScopeId) {
        wrappedTaskQuery.subScopeId(subScopeId);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery scopeType(String scopeType) {
        wrappedTaskQuery.scopeType(scopeType);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery scopeDefinitionId(String scopeDefinitionId) {
        wrappedTaskQuery.scopeDefinitionId(scopeDefinitionId);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCreatedOn(Date createTime) {
        wrappedTaskQuery.taskCreatedOn(createTime);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCreatedBefore(Date before) {
        wrappedTaskQuery.taskCreatedBefore(before);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCreatedAfter(Date after) {
        wrappedTaskQuery.taskCreatedAfter(after);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskCategory(String category) {
        wrappedTaskQuery.taskCategory(category);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDefinitionKey(String key) {
        wrappedTaskQuery.taskDefinitionKey(key);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDefinitionKeyLike(String keyLike) {
        wrappedTaskQuery.taskDefinitionKeyLike(keyLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDueDate(Date dueDate) {
        wrappedTaskQuery.taskDueDate(dueDate);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDueBefore(Date dueDate) {
        wrappedTaskQuery.taskDueBefore(dueDate);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskDueAfter(Date dueDate) {
        wrappedTaskQuery.taskDueAfter(dueDate);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery withoutTaskDueDate() {
        wrappedTaskQuery.withoutTaskDueDate();
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionKey(String processDefinitionKey) {
        wrappedTaskQuery.processDefinitionKey(processDefinitionKey);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionKeyLike(String processDefinitionKeyLike) {
        wrappedTaskQuery.processDefinitionKeyLike(processDefinitionKeyLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionKeyLikeIgnoreCase(String processDefinitionKeyLikeIgnoreCase) {
        wrappedTaskQuery.processDefinitionKeyLikeIgnoreCase(processDefinitionKeyLikeIgnoreCase);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionKeyIn(List<String> processDefinitionKeys) {
        wrappedTaskQuery.processDefinitionKeyIn(processDefinitionKeys);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionId(String processDefinitionId) {
        wrappedTaskQuery.processDefinitionId(processDefinitionId);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionName(String processDefinitionName) {
        wrappedTaskQuery.processDefinitionName(processDefinitionName);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processDefinitionNameLike(String processDefinitionNameLike) {
        wrappedTaskQuery.processDefinitionNameLike(processDefinitionNameLike);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processCategoryIn(List<String> processCategoryInList) {
        wrappedTaskQuery.processCategoryIn(processCategoryInList);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processCategoryNotIn(List<String> processCategoryNotInList) {
        wrappedTaskQuery.processCategoryNotIn(processCategoryNotInList);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery deploymentId(String deploymentId) {
        wrappedTaskQuery.deploymentId(deploymentId);
        return this;
    }



    @Override
    public HistoricTaskInstanceQuery deploymentIdIn(List<String> deploymentIds) {
        wrappedTaskQuery.deploymentIdIn(deploymentIds);
        return this;
    }



    @Override
    public HistoricTaskInstanceQuery taskVariableValueEquals(String variableName, Object variableValue) {
        wrappedTaskQuery.taskVariableValueEquals(variableName, variableValue);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueEquals(Object variableValue) {
        wrappedTaskQuery.taskVariableValueEquals(variableValue);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueEqualsIgnoreCase(String name, String value) {
        wrappedTaskQuery.taskVariableValueEqualsIgnoreCase(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueNotEquals(String variableName, Object variableValue) {
        wrappedTaskQuery.taskVariableValueNotEquals(variableName, variableValue);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueNotEqualsIgnoreCase(String name, String value) {
        wrappedTaskQuery.taskVariableValueNotEqualsIgnoreCase(name, value);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueGreaterThan(String name, Object value) {
        wrappedTaskQuery.taskVariableValueGreaterThan(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueGreaterThanOrEqual(String name, Object value) {
        wrappedTaskQuery.taskVariableValueGreaterThanOrEqual(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueLessThan(String name, Object value) {
        wrappedTaskQuery.taskVariableValueLessThan(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueLessThanOrEqual(String name, Object value) {
        wrappedTaskQuery.taskVariableValueLessThanOrEqual(name, value);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueLike(String name, String value) {
        wrappedTaskQuery.taskVariableValueLike(name, value);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery taskVariableValueLikeIgnoreCase(String name, String value) {
        wrappedTaskQuery.taskVariableValueLikeIgnoreCase(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueEquals(String variableName, Object variableValue) {
        wrappedTaskQuery.processVariableValueEquals(variableName, variableValue);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueEquals(Object variableValue) {
        wrappedTaskQuery.processVariableValueEquals(variableValue);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueEqualsIgnoreCase(String name, String value) {
        wrappedTaskQuery.processVariableValueEqualsIgnoreCase(name, value);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueNotEquals(String variableName, Object variableValue) {
        wrappedTaskQuery.processVariableValueNotEquals(variableName, variableValue);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueNotEqualsIgnoreCase(String name, String value) {
        wrappedTaskQuery.processVariableValueNotEqualsIgnoreCase(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueGreaterThan(String name, Object value) {
        wrappedTaskQuery.processVariableValueGreaterThan(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueGreaterThanOrEqual(String name, Object value) {
        wrappedTaskQuery.processVariableValueGreaterThanOrEqual(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueLessThan(String name, Object value) {
        wrappedTaskQuery.processVariableValueLessThan(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueLessThanOrEqual(String name, Object value) {
        wrappedTaskQuery.processVariableValueGreaterThanOrEqual(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueLike(String name, String value) {
        wrappedTaskQuery.processVariableValueLike(name, value);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery processVariableValueLikeIgnoreCase(String name, String value) {
        wrappedTaskQuery.getProcessDefinitionKeyLikeIgnoreCase();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery includeTaskLocalVariables() {
        wrappedTaskQuery.includeTaskLocalVariables();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery includeProcessVariables() {
        wrappedTaskQuery.includeProcessVariables();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery limitTaskVariables(Integer taskVariablesLimit) {
        wrappedTaskQuery.limitTaskVariables(taskVariablesLimit);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery includeIdentityLinks() {
        wrappedTaskQuery.includeIdentityLinks();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery locale(String locale) {
        wrappedTaskQuery.locale(locale);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery withLocalizationFallback() {
        wrappedTaskQuery.withLocalizationFallback();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery or() {
        wrappedTaskQuery.or();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery endOr() {
        wrappedTaskQuery.endOr();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskId() {
        wrappedTaskQuery.orderByTaskId();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskName() {
        wrappedTaskQuery.orderByTaskName();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskDescription() {
        wrappedTaskQuery.orderByTaskDescription();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskPriority() {
        wrappedTaskQuery.orderByTaskPriority();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskAssignee() {
        wrappedTaskQuery.orderByTaskAssignee();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskCreateTime() {
        wrappedTaskQuery.orderByTaskCreateTime();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByProcessInstanceId() {
        wrappedTaskQuery.orderByProcessInstanceId();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByExecutionId() {
        wrappedTaskQuery.orderByExecutionId();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByProcessDefinitionId() {
        wrappedTaskQuery.orderByProcessDefinitionId();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskDueDate() {
        wrappedTaskQuery.orderByTaskDueDate();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskOwner() {
        wrappedTaskQuery.orderByTaskOwner();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTaskDefinitionKey() {
        wrappedTaskQuery.orderByTaskDefinitionKey();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByTenantId() {
        wrappedTaskQuery.orderByTenantId();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByDueDateNullsFirst() {
        wrappedTaskQuery.orderByDueDateNullsFirst();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderByDueDateNullsLast() {
        wrappedTaskQuery.orderByDueDateNullsLast();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery caseInstanceId(String caseInstanceId) {
        scopeId(caseInstanceId);
        scopeType(VariableScopeType.CMMN);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery caseDefinitionId(String caseDefinitionId) {
        scopeDefinitionId(caseDefinitionId);
        scopeType(VariableScopeType.CMMN);
        return this;
    }

    @Override
    public HistoricTaskInstanceQuery planItemInstanceId(String planItemInstanceId) {
        subScopeId(planItemInstanceId);
        scopeType(VariableScopeType.CMMN);
        return this;        
    }
    
    @Override
    public HistoricTaskInstanceQuery taskDeleteReason(String taskDeleteReason) {
        wrappedTaskQuery.taskDeleteReason(taskDeleteReason);
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery taskDeleteReasonLike(String taskDeleteReasonLike) {
        wrappedTaskQuery.taskDeleteReasonLike(taskDeleteReasonLike);
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery finished() {
        wrappedTaskQuery.finished();
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery unfinished() {
        wrappedTaskQuery.unfinished();
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery taskParentTaskId(String parentTaskId) {
        wrappedTaskQuery.taskParentTaskId(parentTaskId);
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery taskCompletedOn(Date endDate) {
        wrappedTaskQuery.taskCompletedOn(endDate);
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery taskCompletedBefore(Date endDate) {
        wrappedTaskQuery.taskCompletedBefore(endDate);
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery taskCompletedAfter(Date endDate) {
        wrappedTaskQuery.taskCompletedAfter(endDate);
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery orderByHistoricTaskInstanceDuration() {
        wrappedTaskQuery.orderByHistoricTaskInstanceDuration();
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery orderByHistoricTaskInstanceStartTime() {
        wrappedTaskQuery.orderByHistoricTaskInstanceStartTime();
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery orderByHistoricTaskInstanceEndTime() {
        wrappedTaskQuery.orderByHistoricTaskInstanceEndTime();
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery orderByDeleteReason() {
        wrappedTaskQuery.orderByDeleteReason();
        return this;  
    }

    @Override
    public HistoricTaskInstanceQuery asc() {
        wrappedTaskQuery.asc();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery desc() {
        wrappedTaskQuery.desc();
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderBy(QueryProperty property) {
        wrappedTaskQuery.orderBy(property);
        return this;        
    }

    @Override
    public HistoricTaskInstanceQuery orderBy(QueryProperty property, org.flowable.engine.common.api.query.Query.NullHandlingOnOrder nullHandlingOnOrder) {
        wrappedTaskQuery.orderBy(property, nullHandlingOnOrder);
        return this;        
    }

    @Override
    public long count() {
        return wrappedTaskQuery.count();
    }

    @Override
    public HistoricTaskInstance singleResult() {
        return wrappedTaskQuery.singleResult();
    }

    @Override
    public List<HistoricTaskInstance> list() {
        return wrappedTaskQuery.list();
    }

    @Override
    public List<HistoricTaskInstance> listPage(int firstResult, int maxResults) {
        return wrappedTaskQuery.listPage(firstResult, maxResults);
    }
    
}
