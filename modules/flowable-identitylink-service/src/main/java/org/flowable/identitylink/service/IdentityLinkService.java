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
package org.flowable.identitylink.service;

import java.util.Collection;
import java.util.List;

import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;

/**
 * Service which provides access to variables.
 * 
 * @author Tijs Rademakers
 */
public interface IdentityLinkService {
    
    IdentityLinkEntity getIdentityLink(String id);
    
    List<IdentityLinkEntity> findIdentityLinksByTaskId(String taskId);
    
    List<IdentityLinkEntity> findIdentityLinksByProcessInstanceId(String processInstanceId);
    
    List<IdentityLinkEntity> findIdentityLinksByProcessDefinitionId(String processDefinitionId);
    
    IdentityLinkEntity addCandidateUser(String taskId, String userId);
    
    List<IdentityLinkEntity> addCandidateUsers(String taskId, Collection<String> candidateUsers);
    
    IdentityLinkEntity addCandidateGroup(String taskId, String groupId);
    
    List<IdentityLinkEntity> addCandidateGroups(String taskId, Collection<String> candidateGroups);
    
    IdentityLinkEntity createProcessInstanceIdentityLink(String processInstanceId, String userId, String groupId, String type);
    
    IdentityLinkEntity createTaskIdentityLink(String taskId, String userId, String groupId, String type);
    
    IdentityLinkEntity createProcessDefinitionIdentityLink(String processDefinitionId, String userId, String groupId);
    
    IdentityLinkEntity createIdentityLink();
    
    void insertIdentityLink(IdentityLinkEntity identityLink);
    
    void deleteIdentityLink(IdentityLinkEntity identityLink);
    
    List<IdentityLinkEntity> deleteProcessInstanceIdentityLink(String processInstanceId, String userId, String groupId, String type);
    
    List<IdentityLinkEntity> deleteTaskIdentityLink(String taskId, List<IdentityLinkEntity> currentIdentityLinks, String userId, String groupId, String type);
    
    List<IdentityLinkEntity> deleteProcessDefinitionIdentityLink(String processDefinitionId, String userId, String groupId);
    
    List<IdentityLinkEntity> deleteIdentityLinksByTaskId(String taskId);
    
    void deleteIdentityLinksByProcessDefinitionId(String processDefinitionId);
    
}
