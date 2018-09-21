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
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.mongodb.persistence.EntityToDocumentMapper;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityImpl;

/**
 * @author Joram Barrez
 */
public class HistoricVariableInstanceEntityMapper extends AbstractEntityToDocumentMapper<HistoricVariableInstanceEntityImpl> {

    @Override
    public HistoricVariableInstanceEntityImpl fromDocument(Document document) {
        HistoricVariableInstanceEntityImpl variableEntity = new HistoricVariableInstanceEntityImpl();
        
        variableEntity.setId(document.getString("_id"));
        variableEntity.setName(document.getString("name"));
        variableEntity.setExecutionId(document.getString("executionId"));
        variableEntity.setProcessInstanceId(document.getString("processInstanceId"));
        variableEntity.setTaskId(document.getString("taskId"));
        variableEntity.setRevision(document.getInteger("revision"));
        variableEntity.setScopeId(document.getString("scopeId"));
        variableEntity.setScopeType(document.getString("scopeType"));
        variableEntity.setSubScopeId(document.getString("subScopeId"));
        variableEntity.setDoubleValue(document.getDouble("doubleValue"));
        variableEntity.setLongValue(document.getLong("longValue"));
        variableEntity.setTextValue(document.getString("textValue"));
        variableEntity.setTextValue2(document.getString("textValue2"));
        variableEntity.setVariableType(CommandContextUtil.getProcessEngineConfiguration().getVariableTypes()
                .getVariableType(document.getString("typeName")));
        
        return variableEntity;
    }

    @Override
    public Document toDocument(HistoricVariableInstanceEntityImpl variableEntity) {
        Document variableDocument = new Document();
        
        appendIfNotNull(variableDocument, "_id", variableEntity.getId());
        appendIfNotNull(variableDocument, "name", variableEntity.getName());
        appendIfNotNull(variableDocument, "executionId", variableEntity.getExecutionId());
        appendIfNotNull(variableDocument, "processInstanceId", variableEntity.getProcessInstanceId());
        appendIfNotNull(variableDocument, "taskId", variableEntity.getTaskId());
        appendIfNotNull(variableDocument, "revision", variableEntity.getRevision());
        appendIfNotNull(variableDocument, "scopeId", variableEntity.getScopeId());
        appendIfNotNull(variableDocument, "subScopeId", variableEntity.getSubScopeId());
        appendIfNotNull(variableDocument, "scopeType", variableEntity.getScopeType());
        appendIfNotNull(variableDocument, "doubleValue", variableEntity.getDoubleValue());
        appendIfNotNull(variableDocument, "longValue", variableEntity.getLongValue());
        appendIfNotNull(variableDocument, "textValue", variableEntity.getTextValue());
        appendIfNotNull(variableDocument, "textValue2", variableEntity.getTextValue2());
        appendIfNotNull(variableDocument, "typeName", variableEntity.getVariableTypeName());
        
        return variableDocument;
    }

}
