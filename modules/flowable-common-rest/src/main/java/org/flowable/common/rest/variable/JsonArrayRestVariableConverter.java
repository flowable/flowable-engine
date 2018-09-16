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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;


public class JsonArrayRestVariableConverter implements RestVariableConverter {

    @Override
    public String getRestTypeName() {
        return "json";
    }

    @Override
    public Class<?> getVariableType() {
        return ArrayNode.class;
    }

    @Override
    public Object getVariableValue(EngineRestVariable result) {
        if (result.getValue() != null) {
            if (result.getValue() instanceof LinkedHashMap || result.getValue() instanceof ArrayList) {
                // When the variable is coming from the REST API it automatically gets converted to an ArrayList or
                // LinkedHashMap. In all other cases JSON is saved as ArrayNode or ObjectNode. For consistency the
                // variable is converted to such an object here.
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.valueToTree(result.getValue());
            } else {
                return result.getValue();
            }
        }
        return null;
    }

    @Override
    public void convertVariableValue(Object variableValue, EngineRestVariable result) {
        if (variableValue != null) {
            if (!(variableValue instanceof ArrayNode)) {
                throw new FlowableIllegalArgumentException("Converter can only convert com.fasterxml.jackson.databind.node.ArrayNode.");
            }
            result.setValue(variableValue);
        } else {
            result.setValue(null);
        }
    }

}
