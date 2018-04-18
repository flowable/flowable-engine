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
import org.flowable.cmmn.api.runtime.MilestoneInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.UserEventListenerInstanceQuery;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.identitylink.api.IdentityLink;

/**
 * @author Joram Barrez
 */
public interface CmmnRuntimeService {

    CaseInstanceBuilder createCaseInstanceBuilder();
    
    void triggerPlanItemInstance(String planItemInstanceId);
    
    void enablePlanItemInstance(String planItemInstanceId);
    
    void startPlanItemInstance(String planItemInstanceId);
    
    void disablePlanItemInstance(String planItemInstanceId);

    void completeStagePlanItemInstance(String planItemInstanceId);
    
    void completeCaseInstance(String caseInstanceId);
    
    void terminateCaseInstance(String caseInstanceId);
    
    void evaluateCriteria(String caseInstanceId);

    void completeUserEventListenerInstance(String userEventListenerInstanceId);
    
    Map<String, Object> getVariables(String caseInstanceId);
    
    Map<String, Object> getLocalVariables(String planItemInstanceId);
    
    Object getVariable(String caseInstanceId, String variableName);
    
    Object getLocalVariable(String planItemInstanceId, String variableName);
    
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
    
    void removeLocalVariables(String caseInstanceId, Collection<String> variableNames);

    CaseInstanceQuery createCaseInstanceQuery();
    
    PlanItemInstanceQuery createPlanItemInstanceQuery();
    
    MilestoneInstanceQuery createMilestoneInstanceQuery();

    UserEventListenerInstanceQuery createUserEventListenerInstanceQuery();
    
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
     *             when the process instance doesn't exist.
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
    
}
