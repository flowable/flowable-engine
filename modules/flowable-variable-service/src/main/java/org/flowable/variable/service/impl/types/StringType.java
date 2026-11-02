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
import org.flowable.variable.api.types.VariableType;

/**
 * @author Tom Baeyens
 */
public class StringType implements VariableType {

    public static final String TYPE_NAME = "string";
    private final int maxLength;
    private final VariableLengthVerifier lengthVerifier;

    public StringType(int maxLength) {
        this(maxLength, NoopVariableLengthVerifier.INSTANCE);
    }

    public StringType(int maxLength, VariableLengthVerifier lengthVerifier) {
        this.maxLength = maxLength;
        this.lengthVerifier = lengthVerifier;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        return valueFields.getTextValue();
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (value == null) {
            valueFields.setTextValue(null);
        } else {
            String textValue = (String) value;
            lengthVerifier.verifyLength(textValue.length(), valueFields, this);
            valueFields.setTextValue(textValue);
        }
    }

    @Override
    public boolean isAbleToStore(Object value) {
        if (value == null) {
            return true;
        }
        if (String.class.isAssignableFrom(value.getClass())) {
            String stringValue = (String) value;
            return stringValue.length() <= maxLength;
        }
        return false;
    }
}
