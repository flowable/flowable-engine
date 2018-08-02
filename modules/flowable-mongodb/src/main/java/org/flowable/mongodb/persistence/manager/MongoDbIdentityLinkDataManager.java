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

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.data.IdentityLinkDataManager;

import com.mongodb.client.FindIterable;
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
        Document identityLinkDocument = new Document();
        
        identityLinkDocument.append("type", identityLinkEntity.getType());
        identityLinkDocument.append("userId", identityLinkEntity.getUserId());
        identityLinkDocument.append("groupId", identityLinkEntity.getGroupId());
        identityLinkDocument.append("taskId", identityLinkEntity.getTaskId());
        identityLinkDocument.append("processInstanceId", identityLinkEntity.getProcessInstanceId());
        identityLinkDocument.append("processDefinitionId", identityLinkEntity.getProcessDefinitionId());
        identityLinkDocument.append("scopeId", identityLinkEntity.getScopeId());
        identityLinkDocument.append("scopeType", identityLinkEntity.getScopeType());
        identityLinkDocument.append("scopeDefinitionId", identityLinkEntity.getScopeDefinitionId());
        
        getMongoDbSession().insertOne(identityLinkEntity, COLLECTION_IDENTITY_LINKS, identityLinkDocument);
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
        FindIterable<Document> identityLinkDocuments = getMongoDbSession().find(COLLECTION_IDENTITY_LINKS, Filters.eq("taskId"));
        List<IdentityLinkEntity> identityLinkEntities = new ArrayList<>();
        for (Document identityLinkDocument : identityLinkDocuments) {
            identityLinkEntities.add(transformToEntity(identityLinkDocument));
        }
        return identityLinkEntities;
    }

    @Override
    public List<IdentityLinkEntity> findIdentityLinksByProcessInstanceId(String processInstanceId) {
        throw new UnsupportedOperationException();
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
    
    public IdentityLinkEntityImpl transformToEntity(Document document) {
        IdentityLinkEntityImpl IdentityLinkEntity = new IdentityLinkEntityImpl();
        IdentityLinkEntity.setType(document.getString("type"));
        IdentityLinkEntity.setUserId(document.getString("userId"));
        IdentityLinkEntity.setGroupId(document.getString("groupId"));
        IdentityLinkEntity.setTaskId(document.getString("taskId"));
        IdentityLinkEntity.setProcessInstanceId(document.getString("processInstanceId"));
        IdentityLinkEntity.setProcessDefId(document.getString("processDefinitionId"));
        IdentityLinkEntity.setScopeId(document.getString("scopeId"));
        IdentityLinkEntity.setScopeType(document.getString("scopeType"));
        IdentityLinkEntity.setScopeDefinitionId(document.getString("scopeDefinitionId"));
        return IdentityLinkEntity;
    }

}
