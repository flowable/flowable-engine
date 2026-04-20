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

import org.flowable.common.engine.impl.variable.NoopVariableLengthVerifier;
import org.flowable.common.engine.impl.variable.VariableLengthVerifier;
import org.flowable.variable.api.types.ValueFields;

/**
 * @author Martin Grofcik
 */
public class LongStringType extends SerializableType {

    public static final String TYPE_NAME = "longString";
    private final int minLength;

    public LongStringType(int minLength) {
        this(minLength, NoopVariableLengthVerifier.INSTANCE);
    }

    public LongStringType(int minLength, VariableLengthVerifier lengthVerifier) {
        super(false, lengthVerifier);
        this.minLength = minLength;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (value == null) {
            valueFields.setCachedValue(null);
            valueFields.setBytes(null);
            return;
        }
        String textValue = (String) value;
        lengthVerifier.verifyLength(textValue.length(), valueFields, this);
        byte[] serializedValue = serialize(textValue, valueFields);
        valueFields.setBytes(serializedValue);
        valueFields.setCachedValue(textValue);
    }

    @Override
    public boolean isAbleToStore(Object value) {
        if (value == null) {
            return false;
        }
        if (String.class.isAssignableFrom(value.getClass())) {
            String stringValue = (String) value;
            return stringValue.length() >= minLength;
        }
        return false;
    }
}
