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
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.api.runtime.ChangePlanItemStateBuilder;
import org.flowable.cmmn.api.runtime.GenericEventListenerInstanceQuery;
import org.flowable.cmmn.api.runtime.MilestoneInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceTransitionBuilder;
import org.flowable.cmmn.api.runtime.SignalEventListenerInstanceQuery;
import org.flowable.cmmn.api.runtime.UserEventListenerInstanceQuery;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.form.api.FormInfo;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * @author Joram Barrez
 */
public interface CmmnRuntimeService {

    CaseInstanceBuilder createCaseInstanceBuilder();

    PlanItemInstanceTransitionBuilder createPlanItemInstanceTransitionBuilder(String planItemInstanceId);
    
    void triggerPlanItemInstance(String planItemInstanceId);
    
    void enablePlanItemInstance(String planItemInstanceId);
    
    void startPlanItemInstance(String planItemInstanceId);
    
    void disablePlanItemInstance(String planItemInstanceId);

    void completeStagePlanItemInstance(String planItemInstanceId);

    void completeStagePlanItemInstance(String planItemInstanceId, boolean force);
    
    void completeCaseInstance(String caseInstanceId);
    
    void terminateCaseInstance(String caseInstanceId);

    void terminatePlanItemInstance(String planItemInstanceId);
    
    void deleteCaseInstance(String caseInstanceId);

    void evaluateCriteria(String caseInstanceId);
    
    void completeGenericEventListenerInstance(String genericEventListenerInstanceId);

    void completeUserEventListenerInstance(String userEventListenerInstanceId);
    
    /**
     * All variables visible from the given case instance scope.
     *
     * @param caseInstanceId
     *     id of case instance, cannot be null.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException
     *     when no case instance is found for the given caseInstanceId.
     */
    Map<String, Object> getVariables(String caseInstanceId);
    
    /**
     * All variables visible from the given case instance scope.
     *
     * @param caseInstanceId
     *     id of case instance, cannot be null.
     * @return the variable instances or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException
     *     when no case instance is found for the given caseInstanceId.
     */
    Map<String, VariableInstance> getVariableInstances(String caseInstanceId);
    
    /**
     * All variable values that are defined in the plan item instance scope, without taking outer scopes into account.
     *
     * @param planItemInstanceId
     *     id of plan item instance, cannot be null.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException
     *     when no plan item instance is found for the given planItemInstanceId.
     */
    Map<String, Object> getLocalVariables(String planItemInstanceId);
    
    /**
     * All variable values that are defined in the plan item instance scope, without taking outer scopes into account.
     *
     * @param planItemInstanceId
     *     id of plan item instance, cannot be null.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException
     *     when no plan item instance is found for the given planItemInstanceId.
     */
    Map<String, VariableInstance> getLocalVariableInstances(String planItemInstanceId);
    
    /**
     * The variable value. Returns null when no variable value is found with the given name or when the value is set to null.
     *
     * @param caseInstanceId
     *     id of case instance, cannot be null.
     * @param variableName
     *     name of variable, cannot be null.
     * @return the variable value or null if the variable is undefined or the value of the variable is null.
     * @throws FlowableObjectNotFoundException
     *     when no case instance is found for the given caseInstanceId.
     */
    Object getVariable(String caseInstanceId, String variableName);
    
    /**
     * The variable. Returns null when no variable value is found with the given name or when the value is set to null.
     *
     * @param caseInstanceId
     *     id of case instance, cannot be null.
     * @param variableName
     *     name of variable, cannot be null.
     * @return the variable or null if the variable is undefined.
     * @throws FlowableObjectNotFoundException
     *     when no case instance is found for the given caseInstanceId.
     */
    VariableInstance getVariableInstance(String caseInstanceId, String variableName);
    
    /**
     * The local variable value. Returns null when no variable value is found with the given name or when the value is set to null.
     *
     * @param planItemInstanceId
     *     id of plan item instance, cannot be null.
     * @param variableName
     *     name of variable, cannot be null.
     * @return the variable value or null if the variable is undefined or the value of the variable is null.
     * @throws FlowableObjectNotFoundException
     *     when no plan item instance is found for the given planItemInstanceId.
     */
    Object getLocalVariable(String planItemInstanceId, String variableName);
    
    /**
     * The local variable. Returns null when no variable value is found with the given name or when the value is set to null.
     *
     * @param planItemInstanceId
     *     id of plan item instance, cannot be null.
     * @param variableName
     *     name of variable, cannot be null.
     * @return the variable or null if the variable is undefined.
     * @throws FlowableObjectNotFoundException
     *     when no plan item instance is found for the given planItemInstanceId.
     */
    VariableInstance getLocalVariableInstance(String planItemInstanceId, String variableName);
    
    /**
     * Check whether or not this case instance has variable set with the given name, Searching for the variable is done in all scopes that are visible to the given case instance.
     */
    boolean hasVariable(String caseInstanceId, String variableName);
    
    void setVariables(String caseInstanceId, Map<String, Object> variables);
    
    void setVariable(String caseInstanceId, String variableName, Object variableValue);
    
    void setLocalVariables(String planItemInstanceId, Map<String, Object> variables);
    
    void setLocalVariable(String planItemInstanceId, String variableName, Object variableValue);
    
    void removeVariable(String caseInstanceId, String variableName);
    
    void removeVariables(String caseInstanceId, Collection<String> variableNames);
    
    void removeLocalVariable(String planItemInstanceId, String variableName);
    
    void removeLocalVariables(String planItemInstanceId, Collection<String> variableNames);

    /**
     * Set or change the name of the case instance.
     *
     * @param caseInstanceId the id of the case to set the name
     * @param caseName the name to be set on the case
     */
    void setCaseInstanceName(String caseInstanceId, String caseName);

    CaseInstanceQuery createCaseInstanceQuery();
    
    PlanItemInstanceQuery createPlanItemInstanceQuery();
    
    MilestoneInstanceQuery createMilestoneInstanceQuery();
    
    GenericEventListenerInstanceQuery createGenericEventListenerInstanceQuery();
    
    SignalEventListenerInstanceQuery createSignalEventListenerInstanceQuery();

    UserEventListenerInstanceQuery createUserEventListenerInstanceQuery();
    
    /**
     * Creates a new {@link EventSubscriptionQuery} instance, that can be used to query the event subscriptions.
     */
    EventSubscriptionQuery createEventSubscriptionQuery();
    
    /**
     * Gives back a stage overview of the case instance which includes the stage information of the case model.
     * 
     * @param caseInstanceId
     *            id of the case instance, cannot be null.
     * @return list of stage info objects 
     * @throws FlowableObjectNotFoundException
     *             when the case instance doesn't exist.
     */
    List<StageResponse> getStageOverview(String caseInstanceId);
    
    /**
     * Involves a user with a case instance. The type of identity link is defined by the given identityLinkType.
     * 
     * @param caseInstanceId
     *            id of the case instance, cannot be null.
     * @param userId
     *            id of the user involve, cannot be null.
     * @param identityLinkType
     *            type of identityLink, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the case instance doesn't exist.
     */
    void addUserIdentityLink(String caseInstanceId, String userId, String identityLinkType);

    /**
     * Involves a group with a case instance. The type of identityLink is defined by the given identityLink.
     * 
     * @param caseInstanceId
     *            id of the case instance, cannot be null.
     * @param groupId
     *            id of the group to involve, cannot be null.
     * @param identityLinkType
     *            type of identity, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the process instance or group doesn't exist.
     */
    void addGroupIdentityLink(String caseInstanceId, String groupId, String identityLinkType);

    /**
     * Removes the association between a user and a process instance for the given identityLinkType.
     * 
     * @param caseInstanceId
     *            id of the case instance, cannot be null.
     * @param userId
     *            id of the user involve, cannot be null.
     * @param identityLinkType
     *            type of identityLink, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the task or user doesn't exist.
     */
    void deleteUserIdentityLink(String caseInstanceId, String userId, String identityLinkType);

    /**
     * Removes the association between a group and a process instance for the given identityLinkType.
     * 
     * @param caseInstanceId
     *            id of the case instance, cannot be null.
     * @param groupId
     *            id of the group to involve, cannot be null.
     * @param identityLinkType
     *            type of identity, cannot be null.
     * @throws FlowableObjectNotFoundException
     *             when the task or group doesn't exist.
     */
    void deleteGroupIdentityLink(String caseInstanceId, String groupId, String identityLinkType);

    /**
     * Retrieves the {@link IdentityLink}s associated with the given case instance. Such an identity link informs how a certain user is involved with a case instance.
     */
    List<IdentityLink> getIdentityLinksForCaseInstance(String instanceId);
    
    /**
     * Retrieves the {@link IdentityLink}s associated with the given plan item instance. Such an identity link informs how a certain user is involved with a plan item instance.
     */
    List<IdentityLink> getIdentityLinksForPlanItemInstance(String instanceId);
    
    /**
     * Retrieves the {@link EntityLink}s associated with the given case instance.
     */
    List<EntityLink> getEntityLinkChildrenForCaseInstance(String instanceId);

    /**
     * Retrieves all the {@link EntityLink}s associated with the same root as the given case instance.
     */
    List<EntityLink> getEntityLinkChildrenWithSameRootAsCaseInstance(String instanceId);

    /**
     * Retrieves the {@link EntityLink}s where the given case instance is referenced.
     */
    List<EntityLink> getEntityLinkParentsForCaseInstance(String instanceId);

    /**
     * Gets a Form model instance of the start form of a specific case definition or case instance
     *
     * @param caseDefinitionId
     *            id of case definition for which the start form should be retrieved.
     * @param caseInstanceId
     *            id of case instance for which the start form should be retrieved.
     */
    FormInfo getStartFormModel(String caseDefinitionId, String caseInstanceId);
    
    /**
     * Create a {@link ChangePlanItemStateBuilder}, that allows to set various options for changing the state of a process instance.
     */
    ChangePlanItemStateBuilder createChangePlanItemStateBuilder();

    /**
     * Updates the business key for the provided case instance
     *
     * @param caseInstanceId
     *     id of the case instance to set the business key, cannot be null
     * @param businessKey
     *     new businessKey value
     */
    void updateBusinessKey(String caseInstanceId, String businessKey);
    
    /**
     * Updates the business status for the provided case instance
     *
     * @param caseInstanceId
     *     id of the case instance to set the business status, cannot be null
     * @param businessStatus
     *     new business status value
     */
    void updateBusinessStatus(String caseInstanceId, String businessStatus);

}
