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
package org.flowable.variable.service.impl.persistence.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Tom Baeyens
 * @author Marcus Klimstra (CGI)
 * @author Joram Barrez
 */
public class VariableInstanceEntityImpl extends AbstractEntity implements VariableInstanceEntity, ValueFields, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected VariableType type;
    protected String typeName;

    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String taskId;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;

    protected Long longValue;
    protected Double doubleValue;
    protected String textValue;
    protected String textValue2;
    protected VariableByteArrayRef byteArrayRef;

    protected Object cachedValue;
    protected boolean forcedUpdate;
    protected boolean deleted;

    public VariableInstanceEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("name", name);
        if (type != null) {
            persistentState.put("typeName", type.getTypeName());
        }
        persistentState.put("executionId", executionId);
        persistentState.put("scopeId", scopeId);
        persistentState.put("subScopeId", subScopeId);
        persistentState.put("scopeType", scopeType);
        persistentState.put("longValue", longValue);
        persistentState.put("doubleValue", doubleValue);
        persistentState.put("textValue", textValue);
        persistentState.put("textValue2", textValue2);
        if (byteArrayRef != null && byteArrayRef.getId() != null) {
            persistentState.put("byteArrayValueId", byteArrayRef.getId());
        }
        if (forcedUpdate) {
            persistentState.put("forcedUpdate", Boolean.TRUE);
        }
        return persistentState;
    }

    @Override
    public void forceUpdate() {
        forcedUpdate = true;
    }
    
    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    
    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    // byte array value ///////////////////////////////////////////////////////////

    @Override
    public byte[] getBytes() {
        ensureByteArrayRefInitialized();
        return byteArrayRef.getBytes();
    }

    @Override
    public void setBytes(byte[] bytes) {
        ensureByteArrayRefInitialized();
        byteArrayRef.setValue("var-" + name, bytes);
    }

    @Override
    public VariableByteArrayRef getByteArrayRef() {
        return byteArrayRef;
    }

    protected void ensureByteArrayRefInitialized() {
        if (byteArrayRef == null) {
            byteArrayRef = new VariableByteArrayRef();
        }
    }

    // value //////////////////////////////////////////////////////////////////////

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

    // getters and setters ////////////////////////////////////////////////////////

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTypeName() {
        if (typeName != null) {
            return typeName;
        } else if (type != null) {
            return type.getTypeName();
        } else {
            return typeName;
        }
    }

    @Override
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public VariableType getType() {
        return type;
    }

    @Override
    public void setType(VariableType type) {
        this.type = type;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }
    
    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
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
    public String getScopeId() {
        return scopeId;
    }

    @Override
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }
    
    @Override
    public String getSubScopeId() {
        return subScopeId;
    }
    
    @Override
    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
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

    // misc methods ///////////////////////////////////////////////////////////////

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
        if (byteArrayRef != null && byteArrayRef.getId() != null) {
            sb.append(", byteArrayValueId=").append(byteArrayRef.getId());
        }
        sb.append("]");
        return sb.toString();
    }

}
