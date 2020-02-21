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

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * @author Christophe DENEUX
 */
public class JsonDataObject extends ValuedDataObject {

    @Override
    public void setValue(Object value) {
    	if (value instanceof String && !StringUtils.isEmpty(((String) value).trim())) {
            final ObjectMapper mapper = new ObjectMapper();
            try {
                this.value = mapper.readTree((String) value);
            } catch (final IOException e) {
                throw new IllegalArgumentException("Invalid JSON expression to parse", e);
            }
        } else if (value instanceof JsonNode) {
    		this.value = value;
        } else {
            final ObjectMapper mapper = new ObjectMapper();

            // By default, Jackson serializes only public fields, we force to use all fields of the Java Bean
            mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

            // By default, Jackson serializes java.util.Date as timestamp, we force ISO-8601
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));

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
