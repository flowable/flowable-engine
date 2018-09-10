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
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityImpl;
import org.flowable.mongodb.persistence.EntityToDocumentMapper;

/**
 * @author Tijs Rademakers
 */
public class HistoricIdentityLinkEntityMapper implements EntityToDocumentMapper<HistoricIdentityLinkEntityImpl> {

    @Override
    public HistoricIdentityLinkEntityImpl fromDocument(Document document) {
        HistoricIdentityLinkEntityImpl identityLinkEntity = new HistoricIdentityLinkEntityImpl();
        identityLinkEntity.setId(document.getString("_id"));
        identityLinkEntity.setType(document.getString("type"));
        identityLinkEntity.setUserId(document.getString("userId"));
        identityLinkEntity.setGroupId(document.getString("groupId"));
        identityLinkEntity.setCreateTime(document.getDate("createTime"));
        identityLinkEntity.setTaskId(document.getString("taskId"));
        identityLinkEntity.setProcessInstanceId(document.getString("processInstanceId"));
        identityLinkEntity.setScopeId(document.getString("scopeId"));
        identityLinkEntity.setScopeType(document.getString("scopeType"));
        identityLinkEntity.setScopeDefinitionId(document.getString("scopeDefinitionId"));
        return identityLinkEntity;
    }

    @Override
    public Document toDocument(HistoricIdentityLinkEntityImpl identityLinkEntity) {
        Document identityLinkDocument = new Document();
        identityLinkDocument.append("_id", identityLinkEntity.getId());
        identityLinkDocument.append("type", identityLinkEntity.getType());
        identityLinkDocument.append("userId", identityLinkEntity.getUserId());
        identityLinkDocument.append("groupId", identityLinkEntity.getGroupId());
        identityLinkDocument.append("createTime", identityLinkEntity.getCreateTime());
        identityLinkDocument.append("taskId", identityLinkEntity.getTaskId());
        identityLinkDocument.append("processInstanceId", identityLinkEntity.getProcessInstanceId());
        identityLinkDocument.append("scopeId", identityLinkEntity.getScopeId());
        identityLinkDocument.append("scopeType", identityLinkEntity.getScopeType());
        identityLinkDocument.append("scopeDefinitionId", identityLinkEntity.getScopeDefinitionId());
        return identityLinkDocument;
    }

}
