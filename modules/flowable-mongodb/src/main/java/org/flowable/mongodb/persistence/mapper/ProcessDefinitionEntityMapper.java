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
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.flowable.mongodb.persistence.EntityMapper;

/**
 * @author Joram Barrez
 */
public class ProcessDefinitionEntityMapper implements EntityMapper<ProcessDefinitionEntityImpl> {

    @Override
    public ProcessDefinitionEntityImpl fromDocument(Document document) {
        ProcessDefinitionEntityImpl processDefinitionEntity = new ProcessDefinitionEntityImpl();
        processDefinitionEntity.setId(document.getString("_id"));
        processDefinitionEntity.setName(document.getString("name"));
        processDefinitionEntity.setDescription(document.getString("description"));
        processDefinitionEntity.setKey(document.getString("key"));
        processDefinitionEntity.setVersion(document.getInteger("version", 1));
        processDefinitionEntity.setCategory(document.getString("category"));
        processDefinitionEntity.setDeploymentId(document.getString("deploymentId"));
        processDefinitionEntity.setResourceName(document.getString("resourceName"));
        processDefinitionEntity.setTenantId(document.getString("tenantId"));
        processDefinitionEntity.setHistoryLevel(document.getInteger("historyLevel"));
        processDefinitionEntity.setDiagramResourceName(document.getString("diagramResourceName"));
        processDefinitionEntity.setGraphicalNotationDefined(document.getBoolean("isGraphicalNotationDefined", false));
        processDefinitionEntity.setHasStartFormKey(document.getBoolean("hasStartFormKey"));
        processDefinitionEntity.setSuspensionState(document.getInteger("suspensionState"));
        processDefinitionEntity.setDerivedFrom(document.getString("derivedFrom"));
        processDefinitionEntity.setDerivedFromRoot(document.getString("derivedFromRoot"));
        processDefinitionEntity.setDerivedVersion(document.getInteger("derivedVersion"));
        return processDefinitionEntity;
    }

    @Override
    public Document toDocument(ProcessDefinitionEntityImpl processDefinitionEntity) {
        Document processDefinitionDocument = new Document();
        processDefinitionDocument.append("_id", processDefinitionEntity.getId());
        processDefinitionDocument.append("name", processDefinitionEntity.getName());
        processDefinitionDocument.append("description", processDefinitionEntity.getDescription());
        processDefinitionDocument.append("key", processDefinitionEntity.getKey());
        processDefinitionDocument.append("version", processDefinitionEntity.getVersion());
        processDefinitionDocument.append("category", processDefinitionEntity.getCategory());
        processDefinitionDocument.append("deploymentId", processDefinitionEntity.getDeploymentId());
        processDefinitionDocument.append("resourceName", processDefinitionEntity.getResourceName());
        processDefinitionDocument.append("tenantId", processDefinitionEntity.getTenantId());
        processDefinitionDocument.append("historyLevel", processDefinitionEntity.getHistoryLevel());
        processDefinitionDocument.append("diagramResourceName", processDefinitionEntity.getDiagramResourceName());
        processDefinitionDocument.append("isGraphicalNotationDefined", processDefinitionEntity.isGraphicalNotationDefined());
        processDefinitionDocument.append("hasStartFormKey", processDefinitionEntity.getHasStartFormKey());
        processDefinitionDocument.append("suspensionState", processDefinitionEntity.getSuspensionState());
        processDefinitionDocument.append("derivedFrom", processDefinitionEntity.getDerivedFrom());
        processDefinitionDocument.append("derivedFromRoot", processDefinitionEntity.getDerivedFromRoot());
        processDefinitionDocument.append("derivedVersion", processDefinitionEntity.getDerivedVersion());
        return processDefinitionDocument;
    }

}
