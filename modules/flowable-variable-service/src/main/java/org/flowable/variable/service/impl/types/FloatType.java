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

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * Stores {@link Float} values in the double column.
 *
 * @author Filip Hrisafov
 */
public class FloatType implements VariableType {

    public static final String TYPE_NAME = "float";

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
        Double doubleValue = valueFields.getDoubleValue();
        return doubleValue != null ? doubleValue.floatValue() : null;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        valueFields.setDoubleValue(value != null ? ((Float) value).doubleValue() : null);
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return value instanceof Float;
    }
}
