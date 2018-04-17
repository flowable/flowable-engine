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
package org.flowable.identitylink.service.impl.persistence.entity;

import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.EntityManager;

/**
 * @author Joram Barrez
 */
public interface IdentityLinkEntityManager extends EntityManager<IdentityLinkEntity> {

    List<IdentityLinkEntity> findIdentityLinksByTaskId(String taskId);

    List<IdentityLinkEntity> findIdentityLinksByProcessInstanceId(String processInstanceId);
    
    List<IdentityLinkEntity> findIdentityLinksByScopeIdAndType(String scopeId, String scopeType);

    List<IdentityLinkEntity> findIdentityLinksByProcessDefinitionId(String processDefinitionId);
    
    List<IdentityLinkEntity> findIdentityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType);

    List<IdentityLinkEntity> findIdentityLinkByTaskUserGroupAndType(String taskId, String userId, String groupId, String type);

    List<IdentityLinkEntity> findIdentityLinkByProcessInstanceUserGroupAndType(String processInstanceId, String userId, String groupId, String type);

    List<IdentityLinkEntity> findIdentityLinkByProcessDefinitionUserAndGroup(String processDefinitionId, String userId, String groupId);
    
    List<IdentityLinkEntity> findIdentityLinkByScopeIdScopeTypeUserGroupAndType(String scopeId, String scopeType, String userId, String groupId, String type);

    List<IdentityLinkEntity> findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup(String scopeDefinitionId, String scopeType, String userId, String groupId);

    IdentityLinkEntity addProcessInstanceIdentityLink(String processInstanceId, String userId, String groupId, String type);
    
    IdentityLinkEntity addScopeIdentityLink(String scopeDefinitionId, String scopeId, String scopeType, String userId, String groupId, String type);

    IdentityLinkEntity addTaskIdentityLink(String taskId, String userId, String groupId, String type);

    IdentityLinkEntity addProcessDefinitionIdentityLink(String processDefinitionId, String userId, String groupId);
    
    IdentityLinkEntity addScopeDefinitionIdentityLink(String scopeDefinitionId, String scopeType, String userId, String groupId);

    IdentityLinkEntity addCandidateUser(String taskId, String userId);

    List<IdentityLinkEntity> addCandidateUsers(String taskId, Collection<String> candidateUsers);

    IdentityLinkEntity addCandidateGroup(String taskId, String groupId);

    List<IdentityLinkEntity> addCandidateGroups(String taskId, Collection<String> candidateGroups);

    List<IdentityLinkEntity> deleteProcessInstanceIdentityLink(String processInstanceId, String userId, String groupId, String type);
    
    List<IdentityLinkEntity> deleteScopeIdentityLink(String scopeId, String scopeType, String userId, String groupId, String type);

    List<IdentityLinkEntity> deleteTaskIdentityLink(String taskId, List<IdentityLinkEntity> currentIdentityLinks, String userId, String groupId, String type);

    List<IdentityLinkEntity> deleteProcessDefinitionIdentityLink(String processDefinitionId, String userId, String groupId);
    
    List<IdentityLinkEntity> deleteScopeDefinitionIdentityLink(String scopeDefinitionId, String scopeType, String userId, String groupId);

    void deleteIdentityLinksByTaskId(String taskId);

    void deleteIdentityLinksByProcDef(String processDefId);
    
    void deleteIdentityLinksByProcessInstanceId(String processInstanceId);

    void deleteIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType);

}