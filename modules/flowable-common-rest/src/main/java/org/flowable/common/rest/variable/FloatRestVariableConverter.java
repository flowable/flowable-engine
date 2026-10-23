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
package org.flowable.common.rest.variable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;

/**
 * @author Filip Hrisafov
 */
public class FloatRestVariableConverter implements RestVariableConverter {

    @Override
    public String getRestTypeName() {
        return "float";
    }

    @Override
    public Class<?> getVariableType() {
        return Float.class;
    }

    @Override
    public Object getVariableValue(EngineRestVariable result) {
        if (result.getValue() != null) {
            if (!(result.getValue() instanceof Number)) {
                throw new FlowableIllegalArgumentException("Converter can only convert floats");
            }
            return ((Number) result.getValue()).floatValue();
        }
        return null;
    }

    @Override
    public void convertVariableValue(Object variableValue, EngineRestVariable result) {
        if (variableValue != null) {
            if (!(variableValue instanceof Float)) {
                throw new FlowableIllegalArgumentException("Converter can only convert floats");
            }
            result.setValue(variableValue);
        } else {
            result.setValue(null);
        }
    }

}
