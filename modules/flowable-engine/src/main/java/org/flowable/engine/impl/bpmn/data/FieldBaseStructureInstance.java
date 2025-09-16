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
package org.flowable.engine.impl.bpmn.data;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * An instance of {@link FieldBaseStructureDefinition}
 * 
 * @author Esteban Robles Luna
 */
public class FieldBaseStructureInstance implements StructureInstance {

    protected FieldBaseStructureDefinition structureDefinition;

    protected Map<String, Object> fieldValues;

    public FieldBaseStructureInstance(FieldBaseStructureDefinition structureDefinition) {
        this.structureDefinition = structureDefinition;
        this.fieldValues = new HashMap<>();
    }

    public Object getFieldValue(String fieldName) {
        return this.fieldValues.get(fieldName);
    }

    public void setFieldValue(String fieldName, Object value) {
        this.fieldValues.put(fieldName, value);
    }

    public int getFieldSize() {
        return this.structureDefinition.getFieldSize();
    }

    public String getFieldNameAt(int index) {
        return this.structureDefinition.getFieldNameAt(index);
    }

    public Class<?> getFieldTypeAt(int index) {
        return this.structureDefinition.getFieldTypeAt(index);
    }

    @Override
    public Object[] toArray() {
        int fieldSize = this.getFieldSize();
        Object[] arguments = new Object[fieldSize];
        for (int i = 0; i < fieldSize; i++) {
            Object argument = this.getFieldValue(i);
            arguments[i] = argument;
        }
        return arguments;
    }

    private Object getFieldValue(int index) {
        final String fieldName = this.getFieldNameAt(index);
        final Object fieldValueObject = this.getFieldValue(fieldName);
        if (fieldValueObject instanceof ArrayNode fieldArrayNodeValueObject) {
            // The field is a Json Array

            final Class<?> fieldParameterType = this.structureDefinition.getFieldParameterTypeAt(index);
            final ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration().getObjectMapper();
            final Object value = objectMapper.convertValue(fieldValueObject,
                    Array.newInstance(fieldParameterType, fieldArrayNodeValueObject.size()).getClass());

            return Arrays.asList((Object[]) value);
        } else if (fieldValueObject instanceof ValueNode || fieldValueObject instanceof ObjectNode) {
            final ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration().getObjectMapper();
            return objectMapper.convertValue(fieldValueObject, this.getFieldTypeAt(index));
        } else {
            return fieldValueObject;
        }
    }

    @Override
    public void loadFrom(Object[] array) {
        int fieldSize = this.getFieldSize();
        for (int i = 0; i < fieldSize; i++) {
            String fieldName = this.getFieldNameAt(i);
            Object fieldValue = array[i];
            this.setFieldValue(fieldName, fieldValue);
        }
    }
}
