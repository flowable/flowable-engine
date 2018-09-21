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
import org.flowable.mongodb.persistence.EntityToDocumentMapper;

/**
 * @author Joram Barrez
 */
public class IdentityLinkEntityMapper extends AbstractEntityToDocumentMapper<IdentityLinkEntityImpl> {

    @Override
    public IdentityLinkEntityImpl fromDocument(Document document) {
        IdentityLinkEntityImpl identityLinkEntity = new IdentityLinkEntityImpl();
        identityLinkEntity.setId(document.getString("_id"));
        identityLinkEntity.setType(document.getString("type"));
        identityLinkEntity.setUserId(document.getString("userId"));
        identityLinkEntity.setGroupId(document.getString("groupId"));
        identityLinkEntity.setTaskId(document.getString("taskId"));
        identityLinkEntity.setProcessInstanceId(document.getString("processInstanceId"));
        identityLinkEntity.setProcessDefId(document.getString("processDefinitionId"));
        identityLinkEntity.setScopeId(document.getString("scopeId"));
        identityLinkEntity.setScopeType(document.getString("scopeType"));
        identityLinkEntity.setScopeDefinitionId(document.getString("scopeDefinitionId"));
        return identityLinkEntity;
    }

    @Override
    public Document toDocument(IdentityLinkEntityImpl identityLinkEntity) {
        Document identityLinkDocument = new Document();
        appendIfNotNull(identityLinkDocument, "_id", identityLinkEntity.getId());
        appendIfNotNull(identityLinkDocument, "type", identityLinkEntity.getType());
        appendIfNotNull(identityLinkDocument, "userId", identityLinkEntity.getUserId());
        appendIfNotNull(identityLinkDocument, "groupId", identityLinkEntity.getGroupId());
        appendIfNotNull(identityLinkDocument, "taskId", identityLinkEntity.getTaskId());
        appendIfNotNull(identityLinkDocument, "processInstanceId", identityLinkEntity.getProcessInstanceId());
        appendIfNotNull(identityLinkDocument, "processDefinitionId", identityLinkEntity.getProcessDefinitionId());
        appendIfNotNull(identityLinkDocument, "scopeId", identityLinkEntity.getScopeId());
        appendIfNotNull(identityLinkDocument, "scopeType", identityLinkEntity.getScopeType());
        appendIfNotNull(identityLinkDocument, "scopeDefinitionId", identityLinkEntity.getScopeDefinitionId());
        return identityLinkDocument;
    }

}
