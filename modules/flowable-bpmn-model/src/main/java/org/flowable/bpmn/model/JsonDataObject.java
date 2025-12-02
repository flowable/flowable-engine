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
package org.flowable.bpmn.model;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Christophe DENEUX
 */
public class JsonDataObject extends ValuedDataObject {

    @Override
    public void setValue(Object value) {
    	if (value instanceof String && !StringUtils.isEmpty(((String) value).trim())) {
            try {
                this.value = JsonMapper.shared().readTree((String) value);
            } catch (JacksonException e) {
                throw new IllegalArgumentException("Invalid JSON expression to parse", e);
            }
        } else if (value instanceof JsonNode) {
    		this.value = value;
        } else {
            JsonMapper mapper = JsonMapper.builder()
                    // By default, Jackson serializes only public fields, we force to use all fields of the Java Bean
                    .changeDefaultVisibility(visibilityChecker -> visibilityChecker.withFieldVisibility(Visibility.ANY))
                    .build();

            this.value = mapper.convertValue(value, JsonNode.class);
    	}
    }

    @Override
    public JsonDataObject clone() {
        JsonDataObject clone = new JsonDataObject();
        clone.setValues(this);
        return clone;
    }
}
