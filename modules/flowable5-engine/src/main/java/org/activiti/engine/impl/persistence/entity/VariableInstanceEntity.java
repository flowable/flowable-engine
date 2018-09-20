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
package org.activiti.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.BulkDeleteable;
import org.activiti.engine.impl.db.HasRevision;
import org.activiti.engine.impl.db.PersistentObject;
import org.apache.commons.lang3.StringUtils;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.impl.persistence.entity.AbstractVariableServiceEntity;

/**
 * @author Tom Baeyens
 * @author Marcus Klimstra (CGI)
 */
public class VariableInstanceEntity extends AbstractVariableServiceEntity implements VariableInstance, ValueFields, PersistentObject, HasRevision, BulkDeleteable, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String localizedName;
    protected String localizedDescription;
    protected VariableType type;
    protected String typeName;

    protected String processInstanceId;
    protected String executionId;
    protected String taskId;

    protected Long longValue;
    protected Double doubleValue;
    protected String textValue;
    protected String textValue2;
    protected final ByteArrayRef byteArrayRef = new ByteArrayRef();

    protected Object cachedValue;
    protected boolean forcedUpdate;
    protected boolean deleted;

    // Default constructor for SQL mapping
    protected VariableInstanceEntity() {
    }

    public static VariableInstanceEntity createAndInsert(String name, VariableType type, Object value) {
        VariableInstanceEntity variableInstance = create(name, type, value);
        variableInstance.setRevision(0);

        Context.getCommandContext()
                .getDbSqlSession()
                .insert(variableInstance);

        return variableInstance;
    }

    public static VariableInstanceEntity create(String name, VariableType type, Object value) {
        VariableInstanceEntity variableInstance = new VariableInstanceEntity();
        variableInstance.name = name;
        variableInstance.type = type;
        variableInstance.typeName = type.getTypeName();
        variableInstance.setValue(value);
        return variableInstance;
    }

    public void setExecution(ExecutionEntity execution) {
        this.executionId = execution.getId();
        this.processInstanceId = execution.getProcessInstanceId();
        forceUpdate();
    }

    public void forceUpdate() {
        forcedUpdate = true;
    }

    public void delete() {
        Context
                .getCommandContext()
                .getDbSqlSession()
                .delete(this);

        byteArrayRef.delete();
        deleted = true;
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        if (longValue != null) {
            persistentState.put("longValue", longValue);
        }
        if (doubleValue != null) {
            persistentState.put("doubleValue", doubleValue);
        }
        if (textValue != null) {
            persistentState.put("textValue", textValue);
        }
        if (textValue2 != null) {
            persistentState.put("textValue2", textValue2);
        }
        if (byteArrayRef.getId() != null) {
            persistentState.put("byteArrayValueId", byteArrayRef.getId());
        }
        if (forcedUpdate) {
            persistentState.put("forcedUpdate", Boolean.TRUE);
        }
        return persistentState;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    // lazy initialized relations ///////////////////////////////////////////////

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    // byte array value /////////////////////////////////////////////////////////

    @Override
    public byte[] getBytes() {
        return byteArrayRef.getBytes();
    }

    @Override
    public void setBytes(byte[] bytes) {
        byteArrayRef.setValue("var-" + name, bytes);
    }

    // value ////////////////////////////////////////////////////////////////////

    @Override
    public Object getValue() {
        if (!type.isCachable() || cachedValue == null) {
            cachedValue = type.getValue(this);
        }
        return cachedValue;
    }

    @Override
    public void setValue(Object value) {
        type.setValue(value, this);
        typeName = type.getTypeName();
        cachedValue = value;
    }

    // getters and setters //////////////////////////////////////////////////////

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    public String getLocalizedDescription() {
        return localizedDescription;
    }

    public void setLocalizedDescription(String localizedDescription) {
        this.localizedDescription = localizedDescription;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
        this.type = type;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public String getProcessDefinitionId() {
        return null;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public Long getLongValue() {
        return longValue;
    }

    @Override
    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    @Override
    public Double getDoubleValue() {
        return doubleValue;
    }

    @Override
    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    @Override
    public String getTextValue() {
        return textValue;
    }

    @Override
    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    @Override
    public String getTextValue2() {
        return textValue2;
    }

    @Override
    public void setTextValue2(String textValue2) {
        this.textValue2 = textValue2;
    }

    @Override
    public Object getCachedValue() {
        return cachedValue;
    }

    @Override
    public void setCachedValue(Object cachedValue) {
        this.cachedValue = cachedValue;
    }

    // misc methods /////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VariableInstanceEntity[");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", type=").append(type != null ? type.getTypeName() : "null");
        if (longValue != null) {
            sb.append(", longValue=").append(longValue);
        }
        if (doubleValue != null) {
            sb.append(", doubleValue=").append(doubleValue);
        }
        if (textValue != null) {
            sb.append(", textValue=").append(StringUtils.abbreviate(textValue, 40));
        }
        if (textValue2 != null) {
            sb.append(", textValue2=").append(StringUtils.abbreviate(textValue2, 40));
        }
        if (byteArrayRef.getId() != null) {
            sb.append(", byteArrayValueId=").append(byteArrayRef.getId());
        }
        sb.append("]");
        return sb.toString();
    }
    
    // non-supported (v6)

    @Override
    public String getScopeId() {
        return null;
    }
    
    @Override
    public String getSubScopeId() {
        return null;
    }

    @Override
    public String getScopeType() {
        return null;
    }

    @Override
    public void setScopeId(String scopeId) {
        
    }
    
    @Override
    public void setSubScopeId(String subScopeId) {
        
    }

    @Override
    public void setScopeType(String scopeType) {
        
    }

}
