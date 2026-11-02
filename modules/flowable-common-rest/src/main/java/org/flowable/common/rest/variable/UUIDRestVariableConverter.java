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

import java.util.UUID;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;

public class UUIDRestVariableConverter implements RestVariableConverter {

    @Override
    public String getRestTypeName() {
        return "uuid";
    }

    @Override
    public Class<?> getVariableType() {
        return UUID.class;
    }

    @Override
    public Object getVariableValue(EngineRestVariable result) {
        if (result.getValue() != null) {
            if (!(result.getValue() instanceof String)) {
                throw new FlowableIllegalArgumentException("Converter can only convert Strings");
            }
            return UUID.fromString((String) result.getValue());
        }
        return null;
    }

    @Override
    public void convertVariableValue(Object variableValue, EngineRestVariable result) {
        if (variableValue != null) {
            if (!(variableValue instanceof UUID)) {
                throw new FlowableIllegalArgumentException("Converter can only convert UUIDs");
            }
            result.setValue(((UUID)variableValue).toString());
        } else {
            result.setValue(null);
        }
    }

}
