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
package org.flowable.identitylink.service.impl.persistence.entity.data;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;

/**
 * @author Joram Barrez
 */
public interface IdentityLinkDataManager extends DataManager<IdentityLinkEntity> {

    List<IdentityLinkEntity> findIdentityLinksByTaskId(String taskId);

    List<IdentityLinkEntity> findIdentityLinksByProcessInstanceId(String processInstanceId);

    List<IdentityLinkEntity> findIdentityLinksByProcessDefinitionId(String processDefinitionId);
    
    List<IdentityLinkEntity> findIdentityLinksByScopeIdAndType(String scopeId, String scopeType);
    
    List<IdentityLinkEntity> findIdentityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType);

    List<IdentityLinkEntity> findIdentityLinkByTaskUserGroupAndType(String taskId, String userId, String groupId, String type);

    List<IdentityLinkEntity> findIdentityLinkByProcessInstanceUserGroupAndType(String processInstanceId, String userId, String groupId, String type);

    List<IdentityLinkEntity> findIdentityLinkByProcessDefinitionUserAndGroup(String processDefinitionId, String userId, String groupId);
    
    List<IdentityLinkEntity> findIdentityLinkByScopeIdScopeTypeUserGroupAndType(String scopeId, String scopeType, String userId, String groupId, String type);

    List<IdentityLinkEntity> findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup(String scopeDefinitionId, String scopeType, String userId, String groupId);

    void deleteIdentityLinksByTaskId(String taskId);

    void deleteIdentityLinksByProcDef(String processDefId);
    
    void deleteIdentityLinksByProcessInstanceId(String processInstanceId);

    void deleteIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType);

}
