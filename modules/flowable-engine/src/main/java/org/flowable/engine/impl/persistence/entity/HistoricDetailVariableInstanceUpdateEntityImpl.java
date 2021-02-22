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

package org.flowable.engine.impl.persistence.entity;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricDetailVariableInstanceUpdateEntityImpl extends HistoricDetailEntityImpl implements HistoricDetailVariableInstanceUpdateEntity {

    private static final long serialVersionUID = 1L;

    protected int revision;

    protected String name;
    protected VariableType variableType;

    protected Long longValue;
    protected Double doubleValue;
    protected String textValue;
    protected String textValue2;
    protected ByteArrayRef byteArrayRef;

    protected Object cachedValue;

    public HistoricDetailVariableInstanceUpdateEntityImpl() {
        this.detailType = "VariableUpdate";
    }

    @Override
    public Object getPersistentState() {
        // HistoricDetailVariableInstanceUpdateEntity is immutable, so always
        // the same object is returned
        return HistoricDetailVariableInstanceUpdateEntityImpl.class;
    }

    @Override
    public Object getValue() {
        if (!variableType.isCachable() || cachedValue == null) {
            cachedValue = variableType.getValue(this);
        }
        return cachedValue;
    }

    @Override
    public String getVariableTypeName() {
        return (variableType != null ? variableType.getTypeName() : null);
    }

    @Override
    public int getRevisionNext() {
        return revision + 1;
    }

    // byte array value /////////////////////////////////////////////////////////

    @Override
    public byte[] getBytes() {
        if (byteArrayRef != null) {
            return byteArrayRef.getBytes(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        }
        return null;
    }

    @Override
    public ByteArrayRef getByteArrayRef() {
        return byteArrayRef;
    }

    @Override
    public void setBytes(byte[] bytes) {
        String byteArrayName = "hist.detail.var-" + name;
        if (byteArrayRef == null) {
            byteArrayRef = new ByteArrayRef();
        }
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        byteArrayRef.setValue(byteArrayName, bytes, processEngineConfiguration.getEngineCfgKey());
    }

    // getters and setters ////////////////////////////////////////////////////////

    @Override
    public int getRevision() {
        return revision;
    }

    @Override
    public void setRevision(int revision) {
        this.revision = revision;
    }

    @Override
    public String getVariableName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public VariableType getVariableType() {
        return variableType;
    }

    @Override
    public void setVariableType(VariableType variableType) {
        this.variableType = variableType;
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

    // common methods ///////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HistoricDetailVariableInstanceUpdateEntity[");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
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

}
