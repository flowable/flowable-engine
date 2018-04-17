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
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Christian Lipphardt (camunda)
 * @author Joram Barrez
 */
public class HistoricVariableInstanceEntityImpl extends AbstractEntity implements HistoricVariableInstanceEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected VariableType variableType;

    protected String processInstanceId;
    protected String executionId;
    protected String taskId;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;

    protected Date createTime;
    protected Date lastUpdatedTime;

    protected Long longValue;
    protected Double doubleValue;
    protected String textValue;
    protected String textValue2;
    protected VariableByteArrayRef byteArrayRef;

    protected Object cachedValue;

    public HistoricVariableInstanceEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        HashMap<String, Object> persistentState = new HashMap<>();

        persistentState.put("name", name);
        persistentState.put("scopeId", scopeId);
        persistentState.put("subScopeId", subScopeId);
        persistentState.put("scopeType", scopeType);
        persistentState.put("textValue", textValue);
        persistentState.put("textValue2", textValue2);
        persistentState.put("doubleValue", doubleValue);
        persistentState.put("longValue", longValue);
        
        if (variableType != null) {
            persistentState.put("typeName", variableType.getTypeName());
        }

        if (byteArrayRef != null) {
            persistentState.put("byteArrayRef", byteArrayRef.getId());
        }

        persistentState.put("createTime", createTime);
        persistentState.put("lastUpdatedTime", lastUpdatedTime);

        return persistentState;
    }

    @Override
    public Object getValue() {
        if (!variableType.isCachable() || cachedValue == null) {
            cachedValue = variableType.getValue(this);
        }
        return cachedValue;
    }

    // byte array value /////////////////////////////////////////////////////////

    @Override
    public byte[] getBytes() {
        if (byteArrayRef != null) {
            return byteArrayRef.getBytes();
        }
        return null;
    }

    @Override
    public void setBytes(byte[] bytes) {
        if (byteArrayRef == null) {
            byteArrayRef = new VariableByteArrayRef();
        }
        byteArrayRef.setValue("hist.var-" + name, bytes);
    }

    // getters and setters //////////////////////////////////////////////////////

    @Override
    public String getVariableTypeName() {
        return (variableType != null ? variableType.getTypeName() : null);
    }

    @Override
    public String getVariableName() {
        return name;
    }

    @Override
    public VariableType getVariableType() {
        return variableType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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

    @Override
    public void setVariableType(VariableType variableType) {
        this.variableType = variableType;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
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
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    @Override
    public void setLastUpdatedTime(Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }
    
    @Override
    public Date getTime() {
        return createTime;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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
    public VariableByteArrayRef getByteArrayRef() {
        return byteArrayRef;
    }

    // common methods //////////////////////////////////////////////////////////

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HistoricVariableInstanceEntity[");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", revision=").append(revision);
        sb.append(", type=").append(variableType != null ? variableType.getTypeName() : "null");
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
