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

import com.fasterxml.jackson.databind.JsonNode;

import tools.jackson.databind.ObjectMapper;

/**
 * Support for converting Jackson 2 JSON variables over REST.
 * This is only for transforming Jackson 2 JSON variables from a variable to REST.
 * It does not support receiving a JSON variable and transforming it as a Jackson 3 variable.
 */
@Deprecated
public class Jackson2JsonObjectRestVariableConverter implements RestVariableConverter {

    protected ObjectMapper objectMapper;

    public Jackson2JsonObjectRestVariableConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getRestTypeName() {
        return "json";
    }

    @Override
    public Class<?> getVariableType() {
        return JsonNode.class;
    }

    @Override
    public Object getVariableValue(EngineRestVariable result) {
        throw new UnsupportedOperationException("Cannot get variable value for jackson 2");
    }

    @Override
    public void convertVariableValue(Object variableValue, EngineRestVariable result) {
        if (variableValue != null) {
            if (!(variableValue instanceof JsonNode)) {
                throw new FlowableIllegalArgumentException("Converter can only convert com.fasterxml.jackson.databind.JsonNode.");
            }
            tools.jackson.databind.JsonNode valueNode = objectMapper.readTree(variableValue.toString());
            result.setValue(valueNode);
        } else {
            result.setValue(null);
        }
    }

}
