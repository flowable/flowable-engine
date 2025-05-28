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
package org.flowable.task.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.Query;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;

/**
 * Interface containing shared methods between the {@link TaskQuery} and the {@link HistoricTaskInstanceQuery}.
 * 
 * @author Joram Barrez
 */
public interface TaskInfoQuery<T extends TaskInfoQuery<?, ?>, V extends TaskInfo> extends Query<T, V> {

    /**
     * Only select tasks with the given task id (in practice, there will be maximum one of this kind)
     */
    T taskId(String taskId);

    /**
     * Only select tasks with an id that is in the given list
     *
     * @throws FlowableIllegalArgumentException
     *             When passed id list is empty or <code>null</code> or contains <code>null String</code>.
     */
    T taskIds(Collection<String> taskIds);

    /** Only select tasks with the given name */
    T taskName(String name);

    /**
     * Only select tasks with a name that is in the given list
     * 
     * @throws FlowableIllegalArgumentException
     *             When passed name list is empty or <code>null</code> or contains <code>null String</code>.
     */
    T taskNameIn(Collection<String> nameList);

    /**
     * Only select tasks with a name that is in the given list
     * 
     * This method, unlike the {@link #taskNameIn(Collection)} method will not take in account the upper/lower case: both the input parameters as the column value are lowercased when the query is executed.
     * 
     * @throws FlowableIllegalArgumentException
     *             When passed name list is empty or <code>null</code> or contains <code>null String</code>.
     */
    T taskNameInIgnoreCase(Collection<String> nameList);

    /**
     * Only select tasks with a name matching the parameter. The syntax is that of SQL: for example usage: nameLike(%test%)
     */
    T taskNameLike(String nameLike);

    /**
     * Only select tasks with a name matching the parameter. The syntax is that of SQL: for example usage: nameLike(%test%)
     * 
     * This method, unlike the {@link #taskNameLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the query is
     * executed.
     */
    T taskNameLikeIgnoreCase(String nameLike);

    /** Only select tasks with the given description. */
    T taskDescription(String description);

    /**
     * Only select tasks with a description matching the parameter . The syntax is that of SQL: for example usage: descriptionLike(%test%)
     */
    T taskDescriptionLike(String descriptionLike);

    /**
     * Only select tasks with a description matching the parameter . The syntax is that of SQL: for example usage: descriptionLike(%test%)
     * 
     * This method, unlike the {@link #taskDescriptionLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the query is
     * executed.
     */
    T taskDescriptionLikeIgnoreCase(String descriptionLike);

    /** Only select tasks with the given priority. */
    T taskPriority(Integer priority);

    /** Only select tasks with the given priority or higher. */
    T taskMinPriority(Integer minPriority);

    /** Only select tasks with the given priority or lower. */
    T taskMaxPriority(Integer maxPriority);

    /** Only select tasks which are assigned to the given user. */
    T taskAssignee(String assignee);

    /**
     * Only select tasks which were last assigned to an assignee like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     */
    T taskAssigneeLike(String assigneeLike);

    /**
     * Only select tasks which were last assigned to an assignee like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     * 
     * This method, unlike the {@link #taskAssigneeLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the query is
     * executed.
     */
    T taskAssigneeLikeIgnoreCase(String assigneeLikeIgnoreCase);
    
    /** Only select tasks which don't have an assignee. */
    T taskUnassigned();
    
    /** Only select tasks which are assigned to any user */
    T taskAssigned();

    /**
     * Only select tasks with an assignee that is in the given list
     * 
     * @throws FlowableIllegalArgumentException
     *             When passed name list is empty or <code>null</code> or contains <code>null String</code>.
     */
    T taskAssigneeIds(Collection<String> assigneeListIds);

    /** Only select tasks for which the given user is the owner. */
    T taskOwner(String owner);

    /**
     * Only select tasks which were last assigned to an owner like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     */
    T taskOwnerLike(String ownerLike);

    /**
     * Only select tasks which were last assigned to an owner like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     * 
     * This method, unlike the {@link #taskOwnerLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the query is
     * executed.
     */
    T taskOwnerLikeIgnoreCase(String ownerLikeIgnoreCase);

    /** Only select tasks for which the given user is a candidate. */
    T taskCandidateUser(String candidateUser);

    /**
     * Only select tasks for which there exist an {@link IdentityLink} with the given user, including tasks which have been assigned to the given user (assignee) or owned by the given user (owner).
     */
    T taskInvolvedUser(String involvedUser);

    /**
     * Only select tasks for which there exist an {@link IdentityLink} with the given Groups.
     */
    T taskInvolvedGroups(Collection<String> involvedGroup);

    /**
     * Allows to select a task using {@link #taskCandidateGroup(String)} {@link #taskCandidateGroupIn(Collection)} or {@link #taskCandidateUser(String)} but ignore the assignee value instead of querying for an empty assignee.
     */
    T ignoreAssigneeValue();

    /** Only select tasks for which users in the given group are candidates. */
    T taskCandidateGroup(String candidateGroup);

    /**
     * Only select tasks for which the 'candidateGroup' is one of the given groups.
     * 
     * @throws FlowableIllegalArgumentException
     *             When query is executed and {@link #taskCandidateGroup(String)} or {@link #taskCandidateUser(String)} has been executed on the query instance. When passed group list is empty or
     *             <code>null</code>.
     */
    T taskCandidateGroupIn(Collection<String> candidateGroups);

    /**
     * Only select tasks that have the given tenant id.
     */
    T taskTenantId(String tenantId);

    /**
     * Only select tasks with a tenant id like the given one.
     */
    T taskTenantIdLike(String tenantIdLike);

    /**
     * Only select tasks that do not have a tenant id.
     */
    T taskWithoutTenantId();

    /**
     * Only select tasks for the given process instance id.
     */
    T processInstanceId(String processInstanceId);

    /**
     * Only select tasks for the given process ids.
     */
    T processInstanceIdIn(Collection<String> processInstanceIds);
    
    /**
     * Only select tasks without a process instance id.
     */
    T withoutProcessInstanceId();

    /**
     * Only select tasks for the given business key
     */
    T processInstanceBusinessKey(String processInstanceBusinessKey);

    /**
     * Only select tasks with a business key like the given value The syntax is that of SQL: for example usage: processInstanceBusinessKeyLike("%test%").
     */
    T processInstanceBusinessKeyLike(String processInstanceBusinessKeyLike);

    /**
     * Only select tasks with a business key like the given value The syntax is that of SQL: for example usage: processInstanceBusinessKeyLike("%test%").
     * 
     * This method, unlike the {@link #processInstanceBusinessKeyLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when
     * the query is executed.
     */
    T processInstanceBusinessKeyLikeIgnoreCase(String processInstanceBusinessKeyLikeIgnoreCase);

    /**
     * Only select tasks for the given execution.
     */
    T executionId(String executionId);
    
    /**
     * Only select tasks for the given case instance.
     */
    T caseInstanceId(String caseInstanceId);
    
    /**
     * Only select tasks for the given case definition.
     */
    T caseDefinitionId(String caseDefinitionId);

    /**
     * Only select tasks which are part of a case instance which has the given case definition key.
     */
    T caseDefinitionKey(String caseDefinitionKey);

    /**
     * Only select tasks which are part of a case instance which has a case definition key like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     */
    T caseDefinitionKeyLike(String caseDefinitionKeyLike);

    /**
     * Only select tasks which are part of a case instance which has a case definition key like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     *
     * This method, unlike the {@link #caseDefinitionKeyLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the
     * query is executed.
     */
    T caseDefinitionKeyLikeIgnoreCase(String caseDefinitionKeyLikeIgnoreCase);

    /** Only select tasks that have a case definition for which the key is present in the given list **/
    T caseDefinitionKeyIn(Collection<String> caseDefinitionKeys);

    /**
     * Only select tasks for the given plan item instance. 
     */
    T planItemInstanceId(String planItemInstanceId);
    
    /**
     * Only select tasks for the given scope identifier. 
     */
    T scopeId(String scopeId);

    /**
     * Only select tasks for the given scope identifiers.
     */
    T scopeIds(Set<String> scopeIds);
    
    /**
     * Only select tasks for the given sub scope identifier. 
     */
    T subScopeId(String subScopeId);
    
    /**
     * Only select tasks for the given scope type. 
     */
    T scopeType(String scopeType);
    
    /**
     * Only select tasks for the given scope definition identifier. 
     */
    T scopeDefinitionId(String scopeDefinitionId);

    /**
     * Only select tasks for the given stage, defined through its stage instance id.
     */
    T propagatedStageInstanceId(String propagatedStageInstanceId);
    
    /**
     * Select all tasks for the given process instance id and its children.
     */
    T processInstanceIdWithChildren(String processInstanceId);
    
    /**
     * Select all tasks for the given case instance id and its children.
     */
    T caseInstanceIdWithChildren(String caseInstanceId);

    /**
     * Only select tasks that are created on the given date.
     */
    T taskCreatedOn(Date createTime);

    /**
     * Only select tasks that are created before the given date.
     */
    T taskCreatedBefore(Date before);

    /**
     * Only select tasks that are created after the given date.
     */
    T taskCreatedAfter(Date after);
    
    /**
     * Only select tasks that are started in progress on the given date.
     */
    T taskInProgressStartTimeOn(Date claimedTime);

    /**
     * Only select tasks that are started in progress before the given date.
     */
    T taskInProgressStartTimeBefore(Date before);

    /**
     * Only select tasks that are started in progress after the given date.
     */
    T taskInProgressStartTimeAfter(Date after);
    
    /**
     * Select all tasks that have an in progress started user reference for the given value.
     */
    T taskInProgressStartedBy(String startedBy);
    
    /**
     * Only select tasks that are claimed on the given date.
     */
    T taskClaimedOn(Date claimedTime);

    /**
     * Only select tasks that are claimed before the given date.
     */
    T taskClaimedBefore(Date before);

    /**
     * Only select tasks that are claimed after the given date.
     */
    T taskClaimedAfter(Date after);
    
    /**
     * Select all tasks that have a claimed by user reference for the given value.
     */
    T taskClaimedBy(String claimedBy);
    
    /**
     * Only select tasks that are suspended on the given date.
     */
    T taskSuspendedOn(Date suspendedTime);

    /**
     * Only select tasks that are suspended before the given date.
     */
    T taskSuspendedBefore(Date before);

    /**
     * Only select tasks that are suspended after the given date.
     */
    T taskSuspendedAfter(Date after);
    
    /**
     * Select all tasks that have a suspended by user reference for the given value.
     */
    T taskSuspendedBy(String suspendedBy);

    /**
     * Only select tasks with the given category.
     */
    T taskCategory(String category);

    /**
     * Only select tasks belonging to one of the categories in the given list.
     *
     * @param taskCategoryInList
     * @throws FlowableIllegalArgumentException When passed category list is empty or <code>null</code> or contains <code>null</code> String.
     */
    T taskCategoryIn(Collection<String> taskCategoryInList);

    /**
     * Only select tasks with a defined category which do not belong to a category present in the given list.
     * <p>
     * NOTE: This method does <b>not</b> return tasks <b>without</b> category e.g. tasks having a <code>null</code> category.
     * To include <code>null</code> categories, use <code>query.or().taskCategoryNotIn(...).taskWithoutCategory().endOr()</code>
     * </p>
     *
     * @param taskCategoryNotInList
     * @throws FlowableIllegalArgumentException When passed category list is empty or <code>null</code> or contains <code>null String</code>.
     * @see #taskWithoutCategory
     */
    T taskCategoryNotIn(Collection<String> taskCategoryNotInList);

    /**
     * Selects tasks without category.
     * <p>
     * Can also be used in conjunction with other filter criteria to include tasks without category e.g. in <code>or</code> queries.
     * </p>
     * @see #taskCategoryNotIn(Collection)
     */
    T taskWithoutCategory();

    /**
     * Only select tasks with form key.
     */
    T taskWithFormKey();

    /**
     * Only select tasks with the given formKey.
     */
    T taskFormKey(String formKey);

    /**
     * Only select tasks with the given taskDefinitionKey. The task definition key is the id of the userTask: &lt;userTask id="xxx" .../&gt;
     **/
    T taskDefinitionKey(String key);

    /**
     * Only select tasks with a taskDefinitionKey that match the given parameter. The syntax is that of SQL: for example usage: taskDefinitionKeyLike("%test%"). The task definition key is the id of
     * the userTask: &lt;userTask id="xxx" .../&gt;
     **/
    T taskDefinitionKeyLike(String keyLike);

    /**
     * Only select tasks with the given taskDefinitionKeys. The task definition key is the id of the userTask: &lt;userTask id="xxx" .../&gt;
     **/
    T taskDefinitionKeys(Collection<String> keys);
    
    /**
     * Only select tasks with the given state.
     **/
    T taskState(String state);
    
    /**
     * Only select tasks with the given in progress start due date.
     */
    T taskInProgressStartDueDate(Date dueDate);

    /**
     * Only select tasks which have an in progress start due date before the given date.
     */
    T taskInProgressStartDueBefore(Date dueDate);

    /**
     * Only select tasks which have an in progress start due date after the given date.
     */
    T taskInProgressStartDueAfter(Date dueDate);

    /**
     * Only select tasks with no in progress start due date.
     */
    T withoutTaskInProgressStartDueDate();

    /**
     * Only select tasks with the given due date.
     */
    T taskDueDate(Date dueDate);

    /**
     * Only select tasks which have a due date before the given date.
     */
    T taskDueBefore(Date dueDate);

    /**
     * Only select tasks which have a due date after the given date.
     */
    T taskDueAfter(Date dueDate);

    /**
     * Only select tasks with no due date.
     */
    T withoutTaskDueDate();

    /**
     * Only select tasks which are part of a process instance which has the given process definition key.
     */
    T processDefinitionKey(String processDefinitionKey);

    /**
     * Only select tasks which are part of a process instance which has a process definition key like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     */
    T processDefinitionKeyLike(String processDefinitionKeyLike);

    /**
     * Only select tasks which are part of a process instance which has a process definition key like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     * 
     * This method, unlike the {@link #processDefinitionKeyLike(String)} method will not take in account the upper/lower case: both the input parameter as the column value are lowercased when the
     * query is executed.
     */
    T processDefinitionKeyLikeIgnoreCase(String processDefinitionKeyLikeIgnoreCase);

    /** Only select tasks that have a process definition for which the key is present in the given list **/
    T processDefinitionKeyIn(Collection<String> processDefinitionKeys);

    /**
     * Only select tasks which created from the given task definition referenced by id.
     */
    T taskDefinitionId(String taskDefinitionId);

    /**
     * Only select tasks which are part of a process instance which has the given process definition id.
     */
    T processDefinitionId(String processDefinitionId);

    /**
     * Only select tasks which are part of a process instance which has the given process definition name.
     */
    T processDefinitionName(String processDefinitionName);

    /**
     * Only select tasks which are part of a process instance which has a process definition name like the given value. The syntax that should be used is the same as in SQL, eg. %test%.
     */
    T processDefinitionNameLike(String processDefinitionNameLike);

    /**
     * Only select tasks which are part of a process instance whose definition belongs to the category which is present in the given list.
     * 
     * @throws FlowableIllegalArgumentException
     *             When passed category list is empty or <code>null</code> or contains <code>null String</code>.
     * @param processCategoryInList
     */
    T processCategoryIn(Collection<String> processCategoryInList);

    /**
     * Only select tasks which are part of a process instance whose definition does not belong to the category which is present in the given list.
     * 
     * @throws FlowableIllegalArgumentException
     *             When passed category list is empty or <code>null</code> or contains <code>null String</code>.
     * @param processCategoryNotInList
     */
    T processCategoryNotIn(Collection<String> processCategoryNotInList);

    /**
     * Only select tasks which are part of a process instance which has the given deployment id.
     */
    T deploymentId(String deploymentId);

    /**
     * Only select tasks which are part of a process instance which has the given deployment id.
     */
    T deploymentIdIn(Collection<String> deploymentIds);
    
    /**
     * Only select tasks which are related to a case instance for to the given deployment id.
     */
    T cmmnDeploymentId(String cmmnDeploymentId);
    
    /**
     * Only select tasks which are related to a case instances for the given deployment id.
     */
    T cmmnDeploymentIdIn(Collection<String> cmmnDeploymentIds);
    
    /**
     * Only select tasks which don't have a scope id set.
     */
    T withoutScopeId();

    /**
     * Only select tasks which have a local task variable with the given name set to the given value.
     */
    T taskVariableValueEquals(String variableName, Object variableValue);

    /**
     * Only select tasks which have at least one local task variable with the given value.
     */
    T taskVariableValueEquals(Object variableValue);

    /**
     * Only select tasks which have a local string variable with the given value, case insensitive.
     * <p>
     * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
     * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
     * </p>
     */
    T taskVariableValueEqualsIgnoreCase(String name, String value);

    /**
     * Only select tasks which have a local task variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not primitive
     * type wrappers) are not supported.
     */
    T taskVariableValueNotEquals(String variableName, Object variableValue);

    /**
     * Only select tasks which have a local string variable with is not the given value, case insensitive.
     * <p>
     * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
     * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
     * </p>
     */
    T taskVariableValueNotEqualsIgnoreCase(String name, String value);

    /**
     * Only select tasks which have a local variable value greater than the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers)
     * are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    T taskVariableValueGreaterThan(String name, Object value);

    /**
     * Only select tasks which have a local variable value greater than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive
     * type wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    T taskVariableValueGreaterThanOrEqual(String name, Object value);

    /**
     * Only select tasks which have a local variable value less than the passed value when the ended.Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
     * not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    T taskVariableValueLessThan(String name, Object value);

    /**
     * Only select tasks which have a local variable value less than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    T taskVariableValueLessThanOrEqual(String name, Object value);

    /**
     * Only select tasks which have a local variable value like the given value when they ended. This can be used on string variables only.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    T taskVariableValueLike(String name, String value);

    /**
     * Only select tasks which have a local variable value like the given value (case insensitive) when they ended. This can be used on string variables only.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    T taskVariableValueLikeIgnoreCase(String name, String value);
    
    /**
     * Only select tasks which have a local variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    T taskVariableExists(String name);
    
    /**
     * Only select tasks which does not have a local variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    T taskVariableNotExists(String name);

    /**
     * Only select tasks which are part of a process that has a variable with the given name set to the given value.
     */
    T processVariableValueEquals(String variableName, Object variableValue);

    /**
     * Only select tasks which are part of a process that has at least one variable with the given value.
     */
    T processVariableValueEquals(Object variableValue);

    /**
     * Only select tasks which are part of a process that has a local string variable which is not the given value, case insensitive.
     * <p>
     * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
     * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
     * </p>
     */
    T processVariableValueEqualsIgnoreCase(String name, String value);

    /**
     * Only select tasks which have a variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     */
    T processVariableValueNotEquals(String variableName, Object variableValue);

    /**
     * Only select tasks which are part of a process that has a string variable with the given value, case insensitive.
     * <p>
     * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
     * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
     * </p>
     */
    T processVariableValueNotEqualsIgnoreCase(String name, String value);

    /**
     * Only select tasks which have a global variable value greater than the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    T processVariableValueGreaterThan(String name, Object value);

    /**
     * Only select tasks which have a global variable value greater than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive
     * type wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    T processVariableValueGreaterThanOrEqual(String name, Object value);

    /**
     * Only select tasks which have a global variable value less than the passed value when the ended.Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
     * not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    T processVariableValueLessThan(String name, Object value);

    /**
     * Only select tasks which have a global variable value less than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    T processVariableValueLessThanOrEqual(String name, Object value);

    /**
     * Only select tasks which have a global variable value like the given value when they ended. This can be used on string variables only.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    T processVariableValueLike(String name, String value);

    /**
     * Only select tasks which have a global variable value like the given value (case insensitive) when they ended. This can be used on string variables only.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    T processVariableValueLikeIgnoreCase(String name, String value);
    
    /**
     * Only select tasks which have a global variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    T processVariableExists(String name);
    
    /**
     * Only select tasks which does not have a global variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    T processVariableNotExists(String name);

    /**
     * Only select tasks which are part of a case that has a variable with the given name set to the given value.
     */
    T caseVariableValueEquals(String variableName, Object variableValue);

    /**
     * Only select tasks which are part of a case that has at least one variable with the given value.
     */
    T caseVariableValueEquals(Object variableValue);

    /**
     * Only select tasks which are part of a case that has a local string variable which is not the given value, case insensitive.
     * <p>
     * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
     * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
     * </p>
     */
    T caseVariableValueEqualsIgnoreCase(String name, String value);

    /**
     * Only select tasks which have a variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     */
    T caseVariableValueNotEquals(String variableName, Object variableValue);

    /**
     * Only select tasks which are part of a case that has a string variable with the given value, case insensitive.
     * <p>
     * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
     * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
     * </p>
     */
    T caseVariableValueNotEqualsIgnoreCase(String name, String value);

    /**
     * Only select tasks which have a global variable value greater than the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     *
     * @param name cannot be null.
     * @param value cannot be null.
     */
    T caseVariableValueGreaterThan(String name, Object value);

    /**
     * Only select tasks which have a global variable value greater than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive
     * type wrappers) are not supported.
     *
     * @param name cannot be null.
     * @param value cannot be null.
     */
    T caseVariableValueGreaterThanOrEqual(String name, Object value);

    /**
     * Only select tasks which have a global variable value less than the passed value when the ended.Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
     * not supported.
     *
     * @param name cannot be null.
     * @param value cannot be null.
     */
    T caseVariableValueLessThan(String name, Object value);

    /**
     * Only select tasks which have a global variable value less than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     *
     * @param name cannot be null.
     * @param value cannot be null.
     */
    T caseVariableValueLessThanOrEqual(String name, Object value);

    /**
     * Only select tasks which have a global variable value like the given value when they ended. This can be used on string variables only.
     *
     * @param name cannot be null.
     * @param value cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    T caseVariableValueLike(String name, String value);

    /**
     * Only select tasks which have a global variable value like the given value (case insensitive) when they ended. This can be used on string variables only.
     *
     * @param name cannot be null.
     * @param value cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    T caseVariableValueLikeIgnoreCase(String name, String value);

    /**
     * Only select tasks which have a global variable with the given name.
     *
     * @param name cannot be null.
     */
    T caseVariableExists(String name);

    /**
     * Only select tasks which does not have a global variable with the given name.
     *
     * @param name cannot be null.
     */
    T caseVariableNotExists(String name);

    /**
     * Only selects tasks which with the given root scope id
     */
    T taskRootScopeId(String parentScopeId);

    /**
     * Only selects tasks which with the given parent scope id
     */
    T taskParentScopeId(String parentScopeId);

    /**
     * Include local task variables in the task query result
     */
    T includeTaskLocalVariables();

    /**
     * Include global process variables in the task query result
     */
    T includeProcessVariables();

    /**
     * Include global case variables in the task query result
     */
    T includeCaseVariables();

    /**
     * Include identity links in the task query result
     */
    T includeIdentityLinks();

    /**
     * Localize task name and description to specified locale.
     */
    T locale(String locale);

    /**
     * Instruct localization to fallback to more general locales including the default locale of the JVM if the specified locale is not found.
     */
    T withLocalizationFallback();

    /**
     * All query clauses called will be added to a single or-statement. This or-statement will be included with the other already existing clauses in the query, joined by an 'and'.
     * <p>
     * Calling endOr() will add all clauses to the regular query again. Calling or() after or() has been called or calling endOr() after endOr() has been called will result in an exception.
     * It is possible to call or() endOr() several times if each or() has a matching endOr(), e.g.:
     * </p>
     * {@code query.<ConditionA>}
     * {@code  .or().<conditionB>.<conditionC>.endOr()}
     * {@code  .<conditionD>.<conditionE>}
     * {@code  .or().<conditionF>.<conditionG>.endOr()}
     * <p>
     * will result in: conditionA &amp; (conditionB | conditionC) &amp; conditionD &amp; conditionE &amp; (conditionF | conditionG)
     */
    T or();

    T endOr();

    // ORDERING

    /**
     * Order by task id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskId();

    /**
     * Order by task name (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskName();

    /**
     * Order by description (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskDescription();

    /**
     * Order by priority (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskPriority();

    /**
     * Order by assignee (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskAssignee();

    /**
     * Order by the time on which the tasks were created (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskCreateTime();

    /**
     * Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByProcessInstanceId();

    /**
     * Order by execution id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByExecutionId();

    /**
     * Order by process definition id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByProcessDefinitionId();

    /**
     * Order by task due date (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskDueDate();

    /**
     * Order by task owner (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskOwner();

    /**
     * Order by task definition key (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTaskDefinitionKey();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTenantId();

    /**
     * Order by due date (needs to be followed by {@link #asc()} or {@link #desc()}). If any of the tasks have null for the due date, these will be first in the result.
     */
    T orderByDueDateNullsFirst();

    /**
     * Order by due date (needs to be followed by {@link #asc()} or {@link #desc()}). If any of the tasks have null for the due date, these will be last in the result.
     */
    T orderByDueDateNullsLast();

    /**
     * Order by category (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByCategory();

}
