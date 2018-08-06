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
package org.flowable.mongodb.persistence.manager;

import java.util.List;

import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.data.IdentityLinkDataManager;

import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbIdentityLinkDataManager extends AbstractMongoDbDataManager implements IdentityLinkDataManager {
    
    public static String COLLECTION_IDENTITY_LINKS = "identityLinks";

    @Override
    public IdentityLinkEntity create() {
        return new IdentityLinkEntityImpl();
    }

    @Override
    public IdentityLinkEntity findById(String entityId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(IdentityLinkEntity identityLinkEntity) {
        getMongoDbSession().insertOne(identityLinkEntity);
    }

    @Override
    public IdentityLinkEntity update(IdentityLinkEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void delete(IdentityLinkEntity entity) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByTaskId(String taskId) {
        return getMongoDbSession().find(COLLECTION_IDENTITY_LINKS, Filters.eq("taskId"));
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByProcessInstanceId(String processInstanceId) {
        return getMongoDbSession().find(COLLECTION_IDENTITY_LINKS, Filters.eq("processInstanceId", processInstanceId));
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByProcessDefinitionId(String processDefinitionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByScopeIdAndType(String scopeId, String scopeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByTaskUserGroupAndType(String taskId, String userId, String groupId, String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByProcessInstanceUserGroupAndType(String processInstanceId,
            String userId, String groupId, String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByProcessDefinitionUserAndGroup(String processDefinitionId,
            String userId, String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByScopeIdScopeTypeUserGroupAndType(String scopeId, String scopeType,
            String userId, String groupId, String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinkByScopeDefinitionScopeTypeUserAndGroup(String scopeDefinitionId,
            String scopeType, String userId, String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteIdentityLinksByTaskId(String taskId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteIdentityLinksByProcDef(String processDefId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteIdentityLinksByProcessInstanceId(String processInstanceId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteIdentityLinksByScopeIdAndScopeType(String scopeId, String scopeType) {
        throw new UnsupportedOperationException();        
    }
    
}
