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
package org.flowable.variable.service.impl.types;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.HasVariableServiceConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.json.VariableJsonMapper;
import org.flowable.common.engine.impl.variable.NoopVariableLengthVerifier;
import org.flowable.common.engine.impl.variable.VariableLengthVerifier;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class JsonType implements VariableType, MutableVariableType<Object, Object> {

    public static final String TYPE_NAME = "json";

    protected static final String LONG_JSON_TYPE_NAME = "longJson";

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonType.class);

    protected final int maxLength;
    protected final VariableLengthVerifier lengthVerifier;
    protected final boolean trackObjects;
    protected final String typeName;
    protected VariableJsonMapper jsonMapper;

    public JsonType(int maxLength, VariableJsonMapper jsonMapper, boolean trackObjects) {
        this(maxLength, NoopVariableLengthVerifier.INSTANCE, jsonMapper, trackObjects);
    }

    public JsonType(int maxLength, VariableLengthVerifier lengthVerifier, VariableJsonMapper jsonMapper, boolean trackObjects) {
        this(maxLength, lengthVerifier, jsonMapper, trackObjects, TYPE_NAME);
    }

    protected JsonType(int maxLength, VariableLengthVerifier lengthVerifier, VariableJsonMapper jsonMapper, boolean trackObjects, String typeName) {
        this.maxLength = maxLength;
        this.lengthVerifier = lengthVerifier;
        this.trackObjects = trackObjects;
        this.jsonMapper = jsonMapper;
        this.typeName = typeName;
    }

    // Needed for backwards compatibility of longJsonType
    public static JsonType longJsonType(int maxLength, VariableJsonMapper jsonMapper, boolean trackObjects) {
        return longJsonType(maxLength, NoopVariableLengthVerifier.INSTANCE, jsonMapper, trackObjects);
    }

    public static JsonType longJsonType(int maxLength, VariableLengthVerifier lengthVerifier, VariableJsonMapper jsonMapper, boolean trackObjects) {
        return new JsonType(maxLength, lengthVerifier, jsonMapper, trackObjects, LONG_JSON_TYPE_NAME);
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        if (valueFields.getCachedValue() != null) {
            return valueFields.getCachedValue();
        }

        Object jsonValue = null;
        String textValue = valueFields.getTextValue();
        if (textValue != null && textValue.length() > 0) {
            try {
                jsonValue = jsonMapper.readTree(textValue);
                valueFields.setCachedValue(jsonValue);
                traceValue(jsonValue, valueFields);
            } catch (Exception e) {
                LOGGER.error("Error reading json variable {}", valueFields.getName(), e);
            }
        } else {
            byte[] bytes = valueFields.getBytes();
            if (bytes != null && bytes.length > 0) {
                try {
                    jsonValue = jsonMapper.readTree(bytes);
                    valueFields.setCachedValue(jsonValue);
                    traceValue(jsonValue, valueFields);
                } catch (Exception e) {
                    LOGGER.error("Error reading json variable {}", valueFields.getName(), e);
                }
            }
        }
        return jsonValue;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (value == null) {
            valueFields.setTextValue(null);
            valueFields.setBytes(null);
            valueFields.setCachedValue(null);
        } else {

            String textValue = value.toString();
            int length = textValue.length();
            lengthVerifier.verifyLength(length, valueFields, this);
            if (length <= maxLength) {
                valueFields.setTextValue(textValue);
                valueFields.setBytes(null);
            } else {
                valueFields.setBytes(textValue.getBytes(StandardCharsets.UTF_8));
                valueFields.setTextValue(null);
            }
            valueFields.setCachedValue(value);
            traceValue(value, valueFields);
        }
    }

    @Override
    public boolean updateValueIfChanged(Object originalNode, Object originalCopyNode, VariableInstanceEntity variableInstanceEntity) {
        boolean valueChanged = false;
        if (!Objects.equals(originalNode, originalCopyNode)) {
            String textValue = originalNode.toString();
            if (textValue.length() <= maxLength) {
                variableInstanceEntity.setTextValue(textValue);
                if (variableInstanceEntity.getByteArrayRef() != null) {
                    variableInstanceEntity.getByteArrayRef().delete(getEngineType(variableInstanceEntity.getScopeType()));
                }
            } else {
                variableInstanceEntity.setTextValue(null);
                variableInstanceEntity.setBytes(textValue.getBytes(StandardCharsets.UTF_8));
            }
            valueChanged = true;
        }
        return valueChanged;
    }

    protected void traceValue(Object value, ValueFields valueFields) {
        if (trackObjects && valueFields instanceof VariableInstanceEntity) {
            CommandContext commandContext = Context.getCommandContext();
            if (commandContext != null) {
                VariableServiceConfiguration variableServiceConfiguration = getVariableServiceConfiguration(valueFields);
                if (variableServiceConfiguration != null) {
                    commandContext.addCloseListener(new TraceableVariablesCommandContextCloseListener(
                            new TraceableObject<>(this, value, jsonMapper.deepCopy(value), (VariableInstanceEntity) valueFields)
                    ));
                    
                }
            }
        }
    }
    
    protected VariableServiceConfiguration getVariableServiceConfiguration(ValueFields valueFields) {
        String engineType = getEngineType(valueFields.getScopeType());
        Map<String, AbstractEngineConfiguration> engineConfigurationMap = Context.getCommandContext().getEngineConfigurations();
        AbstractEngineConfiguration engineConfiguration = engineConfigurationMap.get(engineType);
        if (engineConfiguration == null) {
            for (AbstractEngineConfiguration possibleEngineConfiguration : engineConfigurationMap.values()) {
                if (possibleEngineConfiguration instanceof HasVariableServiceConfiguration) {
                    engineConfiguration = possibleEngineConfiguration;
                }
            }
        }
        
        if (engineConfiguration == null) {
            return null;
        }
        
        return (VariableServiceConfiguration) engineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG);
    } 
    
    protected String getEngineType(String scopeType) {
        if (StringUtils.isNotEmpty(scopeType)) {
            return scopeType;
        } else {
            return ScopeTypes.BPMN;
        }
    }

    @Override
    public boolean isAbleToStore(Object value) {
        if (value == null) {
            return true;
        }
        return jsonMapper.isJsonNode(value);
    }
}
