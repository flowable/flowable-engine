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
import org.flowable.mongodb.persistence.EntityMapper;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;

/**
 * @author Joram Barrez
 */
public class VariableInstanceEntityMapper implements EntityMapper<VariableInstanceEntityImpl> {

    @Override
    public VariableInstanceEntityImpl fromDocument(Document document) {
        VariableInstanceEntityImpl variableEntity = new VariableInstanceEntityImpl();
        
        variableEntity.setId(document.getString("_id"));
        variableEntity.setName(document.getString("name"));
        variableEntity.setExecutionId(document.getString("executionId"));
        variableEntity.setProcessDefinitionId(document.getString("processDefinitionId"));
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
        variableEntity.setType(CommandContextUtil.getProcessEngineConfiguration().getVariableTypes()
                .getVariableType(document.getString("typeName")));
        variableEntity.setTypeName(document.getString("typeName"));
        
        return variableEntity;
    }

    @Override
    public Document toDocument(VariableInstanceEntityImpl variableEntity) {
        Document variableDocument = new Document();
        
        variableDocument.append("_id", variableEntity.getId());
        variableDocument.append("name", variableEntity.getName());
        variableDocument.append("executionId", variableEntity.getExecutionId());
        variableDocument.append("processDefinitionId", variableEntity.getProcessDefinitionId());
        variableDocument.append("processInstanceId", variableEntity.getProcessInstanceId());
        variableDocument.append("taskId", variableEntity.getTaskId());
        variableDocument.append("revision", variableEntity.getRevision());
        variableDocument.append("scopeId", variableEntity.getScopeId());
        variableDocument.append("subScopeId", variableEntity.getSubScopeId());
        variableDocument.append("scopeType", variableEntity.getScopeType());
        variableDocument.append("doubleValue", variableEntity.getDoubleValue());
        variableDocument.append("longValue", variableEntity.getLongValue());
        variableDocument.append("textValue", variableEntity.getTextValue());
        variableDocument.append("textValue2", variableEntity.getTextValue2());
        variableDocument.append("typeName", variableEntity.getTypeName());
        
        return variableDocument;
    }

}
