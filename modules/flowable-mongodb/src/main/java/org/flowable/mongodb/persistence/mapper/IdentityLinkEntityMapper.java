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
package org.flowable.mongodb.persistence.mapper;

import org.bson.Document;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.mongodb.persistence.EntityMapper;

/**
 * @author Joram Barrez
 */
public class IdentityLinkEntityMapper implements EntityMapper<IdentityLinkEntityImpl> {

    @Override
    public IdentityLinkEntityImpl fromDocument(Document document) {
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

    @Override
    public Document toDocument(IdentityLinkEntityImpl identityLinkEntity) {
        Document identityLinkDocument = new Document();
        identityLinkDocument.append("_id", identityLinkEntity.getId());
        identityLinkDocument.append("type", identityLinkEntity.getType());
        identityLinkDocument.append("userId", identityLinkEntity.getUserId());
        identityLinkDocument.append("groupId", identityLinkEntity.getGroupId());
        identityLinkDocument.append("taskId", identityLinkEntity.getTaskId());
        identityLinkDocument.append("processInstanceId", identityLinkEntity.getProcessInstanceId());
        identityLinkDocument.append("processDefinitionId", identityLinkEntity.getProcessDefinitionId());
        identityLinkDocument.append("scopeId", identityLinkEntity.getScopeId());
        identityLinkDocument.append("scopeType", identityLinkEntity.getScopeType());
        identityLinkDocument.append("scopeDefinitionId", identityLinkEntity.getScopeDefinitionId());
        return identityLinkDocument;
    }

}
