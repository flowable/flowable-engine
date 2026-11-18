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
        valueFields.setDoubleValue(value != null ? toDouble((Float) value) : null);
    }

    /**
     * Converts the float to the double with its shortest decimal representation (e.g. 1.1f becomes 1.1 and not 1.100000023841858).
     * Otherwise, the value would be rounded by databases that do not store the double column as a binary floating point number
     * (e.g. Oracle uses NUMBER(*,10)), which makes the stored value different from the one used in queries.
     */
    protected double toDouble(Float value) {
        double doubleValue = Double.parseDouble(value.toString());
        // This should always be the case, but fall back to the exact conversion if the shortest representation is not the same float
        return (float) doubleValue == value ? doubleValue : value.doubleValue();
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return value instanceof Float;
    }
}
