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

import java.util.Date;

import org.bson.Document;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.persistence.entity.ModelEntityImpl;
import org.flowable.mongodb.persistence.EntityToDocumentMapper;
import org.flowable.mongodb.persistence.entity.MongoDbModelEntityImpl;

/**
 * @author Joram Barrez
 */
public class ModelEntityMapper extends AbstractEntityToDocumentMapper<MongoDbModelEntityImpl> {

    @Override
    public MongoDbModelEntityImpl fromDocument(Document document) {
        MongoDbModelEntityImpl modelEntity = new MongoDbModelEntityImpl();
        modelEntity.setId(document.getString("_id"));
        modelEntity.setName(document.getString("name"));
        modelEntity.setKey(document.getString("key"));
        modelEntity.setCategory(document.getString("category"));
        modelEntity.setCreateTime(document.getDate("createTime"));
        modelEntity.setLastUpdateTime(document.getDate("lastUpdateTime"));
        modelEntity.setVersion(document.getInteger("version"));
        modelEntity.setMetaInfo(document.getString("metaInfo"));
        modelEntity.setDeploymentId(document.getString("deploymentId"));
        modelEntity.setEditorSourceValueId(document.getString("editorSourceValueId"));
        modelEntity.setEditorSourceExtraValueId(document.getString("editorSourceExtraValueId"));
        modelEntity.setTenantId(document.getString("tenantId"));

        // Mongo impl specific
        modelEntity.setLatest(document.getBoolean("latest"));

        return modelEntity;
    }

    @Override
    public Document toDocument(MongoDbModelEntityImpl modelEntity) {
        Document modelDocument = new Document();
        appendIfNotNull(modelDocument, "name", modelEntity.getName());
        appendIfNotNull(modelDocument, "key", modelEntity.getKey());
        appendIfNotNull(modelDocument, "category", modelEntity.getCategory());
        appendIfNotNull(modelDocument, "createTime", modelEntity.getCreateTime());
        appendIfNotNull(modelDocument, "lastUpdateTime", modelEntity.getLastUpdateTime());
        appendIfNotNull(modelDocument, "version", modelEntity.getVersion());
        appendIfNotNull(modelDocument, "metaInfo", modelEntity.getMetaInfo());
        appendIfNotNull(modelDocument, "deploymentId", modelEntity.getDeploymentId());
        appendIfNotNull(modelDocument, "editorSourceValueId", modelEntity.getEditorSourceValueId());
        appendIfNotNull(modelDocument, "editorSourceExtraValueId", modelEntity.getEditorSourceExtraValueId());
        appendIfNotNull(modelDocument, "tenantId", modelEntity.getTenantId());

        // Mongo impl specific
        appendIfNotNull(modelDocument, "latest", modelEntity.isLatest());

        return modelDocument;
    }

}
