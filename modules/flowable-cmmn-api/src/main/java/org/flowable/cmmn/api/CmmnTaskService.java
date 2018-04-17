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
package org.flowable.cmmn.api;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.FlowableTaskAlreadyClaimedException;
import org.flowable.form.api.FormInfo;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.api.TaskQuery;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * @author Joram Barrez
 */
public interface CmmnTaskService {
    
    Task newTask();
    
    Task newTask(String taskId);

    /**
     * Create a builder for the task
     *
     * @return task builder
     */
    TaskBuilder createTaskBuilder();
    
    void saveTask(Task task);

    void complete(String taskId);
    
    void complete(String taskId, Map<String, Object> variables);
    
    void complete(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables);
    
    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables);

    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome,
            Map<String, Object> variables, Map<String, Object> transientVariables);

    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome,
            Map<String, Object> variables, boolean localScope);
    
    /**
     * Claim responsibility for a task: the given user is made assignee for the task. The difference with {@link #setAssignee(String, String)} is that here a check is done if the task already has a
     * user assigned to it. No check is done whether the user is known by the identity component.
     * 
     * @param taskId
     *            task to claim, cannot be null.
     * @param userId
     *            user that claims the task. When userId is null the task is unclaimed, assigned to no one.
     * @throws FlowableObjectNotFoundException
     *             when the task doesn't exist.
     * @throws FlowableTaskAlreadyClaimedException
     *             when the task is already claimed by another user.
     */
    void claim(String taskId, String userId);

    /**
     * A shortcut to {@link #claim} with null user in order to unclaim the task
     * 
     * @param taskId
     *            task to unclaim, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the task doesn't exist.
     */
    void unclaim(String taskId);

    /**
     * Delegates the task to another user. This means that the assignee is set and the delegation state is set to {@link DelegationState#PENDING}. If no owner is set on the task, the owner is set to
     * the current assignee of the task.
     * 
     * @param taskId
     *            The id of the task that will be delegated.
     * @param userId
     *            The id of the user that will be set as assignee.
     * @throws FlowableObjectNotFoundException
     *             when no task exists with the given id.
     */
    void delegateTask(String taskId, String userId);

    /**
     * Marks that the assignee is done with this task and that it can be send back to the owner. Can only be called when this task is {@link DelegationState#PENDING} delegation. After this method
     * returns, the {@link Task#getDelegationState() delegationState} is set to {@link DelegationState#RESOLVED}.
     * 
     * @param taskId
     *            the id of the task to resolve, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when no task exists with the given id.
     */
    void resolveTask(String taskId);

    /**
     * Marks that the assignee is done with this task providing the required variables and that it can be sent back to the owner. Can only be called when this task is {@link DelegationState#PENDING}
     * delegation. After this method returns, the {@link Task#getDelegationState() delegationState} is set to {@link DelegationState#RESOLVED}.
     * 
     * @param taskId
     * @param variables
     *             When no task exists with the given id.
     */
    void resolveTask(String taskId, Map<String, Object> variables);

    /**
     * Similar to {@link #resolveTask(String, Map)}, but allows to set transient variables too.
     */
    void resolveTask(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables);
    
    /**
     * Deletes the given task, not deleting historic information that is related to this task.
     * 
     * @param taskId
     *            The id of the task that will be deleted, cannot be null. If no task exists with the given taskId, the operation is ignored.
     * @throws FlowableObjectNotFoundException
     *             when the task with given id does not exist.
     * @throws FlowableException
     *             when an error occurs while deleting the task or in case the task is part of a running process.
     */
    void deleteTask(String taskId);
    
    /**
     * Deletes all tasks of the given collection, not deleting historic information that is related to these tasks.
     * 
     * @param taskIds
     *            The id's of the tasks that will be deleted, cannot be null. All id's in the list that don't have an existing task will be ignored.
     * @throws FlowableObjectNotFoundException
     *             when one of the task does not exist.
     * @throws FlowableException
     *             when an error occurs while deleting the tasks or in case one of the tasks is part of a running process.
     */
    void deleteTasks(Collection<String> taskIds);

    /**
     * Deletes the given task.
     * 
     * @param taskId
     *            The id of the task that will be deleted, cannot be null. If no task exists with the given taskId, the operation is ignored.
     * @param cascade
     *            If cascade is true, also the historic information related to this task is deleted.
     * @throws FlowableObjectNotFoundException
     *             when the task with given id does not exist.
     * @throws FlowableException
     *             when an error occurs while deleting the task or in case the task is part of a running process.
     */
    void deleteTask(String taskId, boolean cascade);

    /**
     * Deletes all tasks of the given collection.
     * 
     * @param taskIds
     *            The id's of the tasks that will be deleted, cannot be null. All id's in the list that don't have an existing task will be ignored.
     * @param cascade
     *            If cascade is true, also the historic information related to this task is deleted.
     * @throws FlowableObjectNotFoundException
     *             when one of the tasks does not exist.
     * @throws FlowableException
     *             when an error occurs while deleting the tasks or in case one of the tasks is part of a running process.
     */
    void deleteTasks(Collection<String> taskIds, boolean cascade);

    /**
     * Deletes the given task, not deleting historic information that is related to this task..
     * 
     * @param taskId
     *            The id of the task that will be deleted, cannot be null. If no task exists with the given taskId, the operation is ignored.
     * @param deleteReason
     *            reason the task is deleted. Is recorded in history, if enabled.
     * @throws FlowableObjectNotFoundException
     *             when the task with given id does not exist.
     * @throws FlowableException
     *             when an error occurs while deleting the task or in case the task is part of a running process
     */
    void deleteTask(String taskId, String deleteReason);

    /**
     * Deletes all tasks of the given collection, not deleting historic information that is related to these tasks.
     * 
     * @param taskIds
     *            The id's of the tasks that will be deleted, cannot be null. All id's in the list that don't have an existing task will be ignored.
     * @param deleteReason
     *            reason the task is deleted. Is recorded in history, if enabled.
     * @throws FlowableObjectNotFoundException
     *             when one of the tasks does not exist.
     * @throws FlowableException
     *             when an error occurs while deleting the tasks or in case one of the tasks is part of a running process.
     */
    void deleteTasks(Collection<String> taskIds, String deleteReason);
    
    /** The list of subtasks for this parent task */
    List<Task> getSubTasks(String parentTaskId);
    
    /**
     * set variable on a task. If the variable is not already existing, it will be created in the most outer scope. This means the process instance in case this task is related to an execution.
     */
    void setVariable(String taskId, String variableName, Object value);

    /**
     * set variables on a task. If the variable is not already existing, it will be created in the most outer scope. This means the process instance in case this task is related to an execution.
     */
    void setVariables(String taskId, Map<String, ? extends Object> variables);

    /**
     * set variable on a task. If the variable is not already existing, it will be created in the task.
     */
    void setVariableLocal(String taskId, String variableName, Object value);

    /**
     * set variables on a task. If the variable is not already existing, it will be created in the task.
     */
    void setVariablesLocal(String taskId, Map<String, ? extends Object> variables);

    /**
     * get a variables and search in the task scope and if available also the execution scopes.
     */
    Object getVariable(String taskId, String variableName);

    /**
     * get a variables and search in the task scope and if available also the execution scopes.
     */
    <T> T getVariable(String taskId, String variableName, Class<T> variableClass);

    /**
     * The variable. Searching for the variable is done in all scopes that are visible to the given task (including parent scopes). Returns null when no variable value is found with the given name.
     *
     * @param taskId
     *            id of task, cannot be null.
     * @param variableName
     *            name of variable, cannot be null.
     * @return the variable or null if the variable is undefined.
     * @throws FlowableObjectNotFoundException
     *             when no execution is found for the given taskId.
     */
    VariableInstance getVariableInstance(String taskId, String variableName);

    /**
     * checks whether or not the task has a variable defined with the given name, in the task scope and if available also the execution scopes.
     */
    boolean hasVariable(String taskId, String variableName);

    /**
     * checks whether or not the task has a variable defined with the given name.
     */
    Object getVariableLocal(String taskId, String variableName);

    /**
     * checks whether or not the task has a variable defined with the given name.
     */
    <T> T getVariableLocal(String taskId, String variableName, Class<T> variableClass);

    /**
     * The variable for a task. Returns the variable when it is set for the task (and not searching parent scopes). Returns null when no variable is found with the given name.
     *
     * @param taskId
     *            id of task, cannot be null.
     * @param variableName
     *            name of variable, cannot be null.
     * @return the variable or null if the variable is undefined.
     * @throws FlowableObjectNotFoundException
     *             when no task is found for the given taskId.
     */
    VariableInstance getVariableInstanceLocal(String taskId, String variableName);

    /**
     * checks whether or not the task has a variable defined with the given name, local task scope only.
     */
    boolean hasVariableLocal(String taskId, String variableName);

    /**
     * get all variables and search in the task scope and if available also the execution scopes. If you have many variables and you only need a few, consider using
     * {@link #getVariables(String, Collection)} for better performance.
     */
    Map<String, Object> getVariables(String taskId);

    /**
     * All variables visible from the given task scope (including parent scopes).
     *
     * @param taskId
     *            id of task, cannot be null.
     * @return the variable instances or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException
     *             when no task is found for the given taskId.
     */
    Map<String, VariableInstance> getVariableInstances(String taskId);

    /**
     * The variable values for all given variableNames, takes all variables into account which are visible from the given task scope (including parent scopes).
     *
     * @param taskId
     *            id of taskId, cannot be null.
     * @param variableNames
     *            the collection of variable names that should be retrieved.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException
     *             when no taskId is found for the given taskId.
     */
    Map<String, VariableInstance> getVariableInstances(String taskId, Collection<String> variableNames);

    /**
     * get all variables and search only in the task scope. If you have many task local variables and you only need a few, consider using {@link #getVariablesLocal(String, Collection)} for better
     * performance.
     */
    Map<String, Object> getVariablesLocal(String taskId);

    /**
     * get values for all given variableNames and search only in the task scope.
     */
    Map<String, Object> getVariables(String taskId, Collection<String> variableNames);

    /** get a variable on a task */
    Map<String, Object> getVariablesLocal(String taskId, Collection<String> variableNames);

    /** get all variables and search only in the task scope. */
    List<VariableInstance> getVariableInstancesLocalByTaskIds(Set<String> taskIds);

    /**
     * All variable values that are defined in the task scope, without taking outer scopes into account. If you have many task local variables and you only need a few, consider using
     * {@link #getVariableInstancesLocal(String, Collection)} for better performance.
     *
     * @param taskId
     *            id of task, cannot be null.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException
     *             when no task is found for the given taskId.
     */
    Map<String, VariableInstance> getVariableInstancesLocal(String taskId);

    /**
     * The variable values for all given variableNames that are defined in the given task's scope. (Does not searching parent scopes).
     *
     * @param taskId
     *            id of taskId, cannot be null.
     * @param variableNames
     *            the collection of variable names that should be retrieved.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException
     *             when no taskId is found for the given taskId.
     */
    Map<String, VariableInstance> getVariableInstancesLocal(String taskId, Collection<String> variableNames);

    /**
     * Removes the variable from the task. When the variable does not exist, nothing happens.
     */
    void removeVariable(String taskId, String variableName);

    /**
     * Removes the variable from the task (not considering parent scopes). When the variable does not exist, nothing happens.
     */
    void removeVariableLocal(String taskId, String variableName);

    /**
     * Removes all variables in the given collection from the task. Non existing variable names are simply ignored.
     */
    void removeVariables(String taskId, Collection<String> variableNames);

    /**
     * Removes all variables in the given collection from the task (not considering parent scopes). Non existing variable names are simply ignored.
     */
    void removeVariablesLocal(String taskId, Collection<String> variableNames);

    FormInfo getTaskFormModel(String taskId);
    
    /**
     * Changes the priority of the task.
     * 
     * Authorization: actual owner / business admin
     * 
     * @param taskId
     *            id of the task, cannot be null.
     * @param priority
     *            the new priority for the task.
     * @throws FlowableObjectNotFoundException
     *             when the task doesn't exist.
     */
    void setPriority(String taskId, int priority);

    /**
     * Changes the due date of the task
     * 
     * @param taskId
     *            id of the task, cannot be null.
     * @param dueDate
     *            the new due date for the task
     * @throws FlowableException
     *             when the task doesn't exist.
     */
    void setDueDate(String taskId, Date dueDate);
    
    TaskQuery createTaskQuery();
    
    /**
     * Changes the assignee of the given task to the given userId. No check is done whether the user is known by the identity component.
     * 
     * @param taskId
     *            id of the task, cannot be null.
     * @param userId
     *            id of the user to use as assignee.
     * @throws FlowableObjectNotFoundException
     *             when the task or user doesn't exist.
     */
    void setAssignee(String taskId, String userId);

    /**
     * Transfers ownership of this task to another user. No check is done whether the user is known by the identity component.
     * 
     * @param taskId
     *            id of the task, cannot be null.
     * @param userId
     *            of the person that is receiving ownership.
     * @throws FlowableObjectNotFoundException
     *             when the task or user doesn't exist.
     */
    void setOwner(String taskId, String userId);
    
    /**
     * Retrieves the {@link IdentityLink}s associated with the given task. Such an {@link IdentityLink} informs how a certain identity (eg. group or user) is associated with a certain task (eg. as
     * candidate, assignee, etc.)
     */
    List<IdentityLink> getIdentityLinksForTask(String taskId);

    /**
     * Involves a user with a task. The type of identity link is defined by the given identityLinkType.
     * 
     * @param taskId
     *            id of the task, cannot be null.
     * @param userId
     *            id of the user involve, cannot be null.
     * @param identityLinkType
     *            type of identityLink, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the task or user doesn't exist.
     */
    void addUserIdentityLink(String taskId, String userId, String identityLinkType);

    /**
     * Involves a group with a task. The type of identityLink is defined by the given identityLink.
     * 
     * @param taskId
     *            id of the task, cannot be null.
     * @param groupId
     *            id of the group to involve, cannot be null.
     * @param identityLinkType
     *            type of identity, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the task or group doesn't exist.
     */
    void addGroupIdentityLink(String taskId, String groupId, String identityLinkType);
    
    /**
     * Removes the association between a user and a task for the given identityLinkType.
     * 
     * @param taskId
     *            id of the task, cannot be null.
     * @param userId
     *            id of the user involve, cannot be null.
     * @param identityLinkType
     *            type of identityLink, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the task or user doesn't exist.
     */
    void deleteUserIdentityLink(String taskId, String userId, String identityLinkType);

    /**
     * Removes the association between a group and a task for the given identityLinkType.
     * 
     * @param taskId
     *            id of the task, cannot be null.
     * @param groupId
     *            id of the group to involve, cannot be null.
     * @param identityLinkType
     *            type of identity, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the task or group doesn't exist.
     */
    void deleteGroupIdentityLink(String taskId, String groupId, String identityLinkType);
}
