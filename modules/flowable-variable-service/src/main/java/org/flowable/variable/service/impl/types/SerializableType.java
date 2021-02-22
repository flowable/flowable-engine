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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.HasVariableServiceConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Marcus Klimstra (CGI)
 */
public class SerializableType extends ByteArrayType implements MutableVariableType<Object, byte[]> {

    public static final String TYPE_NAME = "serializable";

    protected boolean trackDeserializedObjects;

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    public SerializableType() {
    }

    public SerializableType(boolean trackDeserializedObjects) {
        this.trackDeserializedObjects = trackDeserializedObjects;
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        Object cachedObject = valueFields.getCachedValue();
        if (cachedObject != null) {
            return cachedObject;
        }

        byte[] bytes = (byte[]) super.getValue(valueFields);
        if (bytes != null) {

            Object deserializedObject = deserialize(bytes, valueFields);
            valueFields.setCachedValue(deserializedObject);

            traceValue(valueFields.getCachedValue(), bytes, valueFields);

            return deserializedObject;
        }
        return null; // byte array is null
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        byte[] bytes = serialize(value, valueFields);
        valueFields.setCachedValue(value);

        super.setValue(bytes, valueFields);

        traceValue(valueFields.getCachedValue(), bytes, valueFields);
    }

    protected void traceValue(Object value, byte[] valueBytes, ValueFields valueFields) {
        if (trackDeserializedObjects && valueFields instanceof VariableInstanceEntity) {
            CommandContext commandContext = Context.getCommandContext();
            if (commandContext != null) {
                VariableServiceConfiguration variableServiceConfiguration = getVariableServiceConfiguration(valueFields);
                if (variableServiceConfiguration != null) {
                    commandContext.addCloseListener(new TraceableVariablesCommandContextCloseListener(
                        new TraceableObject<>(this, value, valueBytes, (VariableInstanceEntity) valueFields)
                    ));
                    variableServiceConfiguration.getInternalHistoryVariableManager().initAsyncHistoryCommandContextCloseListener();
                }
            }
        }
    }


    @Override
    public boolean updateValueIfChanged(Object tracedObject, byte[] originalBytes,
        VariableInstanceEntity variableInstanceEntity) {
        byte[] bytes = serialize(tracedObject, variableInstanceEntity);
        boolean valueChanged = false;
        // this first check verifies if the variable value was not overwritten with another object
        if (!Arrays.equals(originalBytes, bytes)) {

            // Add an additional check to prevent byte differences due to JDK changes etc
            Object originalObject = deserialize(originalBytes, variableInstanceEntity);
            byte[] refreshedOriginalBytes = serialize(originalObject, variableInstanceEntity);

            if (!Arrays.equals(refreshedOriginalBytes, bytes)) {
                variableInstanceEntity.setBytes(bytes);
                valueChanged = true;
            }
        }
        return valueChanged;
    }

    public byte[] serialize(Object value, ValueFields valueFields) {
        if (value == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = createObjectOutputStream(baos);
            oos.writeObject(value);
        } catch (Exception e) {
            throw new FlowableException("Couldn't serialize value '" + value + "' in variable '" + valueFields.getName() + "'", e);
        } finally {
            IoUtil.closeSilently(oos);
        }
        return baos.toByteArray();
    }

    public Object deserialize(byte[] bytes, ValueFields valueFields) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = createObjectInputStream(bais);
            Object deserializedObject = ois.readObject();

            return deserializedObject;
        } catch (Exception e) {
            throw new FlowableException("Couldn't deserialize object in variable '" + valueFields.getName() + "'", e);
        } finally {
            IoUtil.closeSilently(bais);
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
        // TODO don't we need null support here?
        return value instanceof Serializable;
    }

    protected ObjectInputStream createObjectInputStream(InputStream is) throws IOException {
        return new ObjectInputStream(is) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return ReflectUtil.loadClass(desc.getName());
            }
        };
    }

    protected ObjectOutputStream createObjectOutputStream(OutputStream os) throws IOException {
        return new ObjectOutputStream(os);
    }
}
