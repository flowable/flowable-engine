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
package org.flowable.eventregistry.impl.keydetector;

import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class JsonPointerBasedInboundEventKeyDetector implements InboundEventKeyDetector<JsonNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPointerBasedInboundEventKeyDetector.class);

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected String jsonPointerValue;
    protected JsonPointer jsonPointerExpression;

    public JsonPointerBasedInboundEventKeyDetector(String jsonPointerExpression) {
        this.jsonPointerValue = jsonPointerExpression;
        this.jsonPointerExpression = JsonPointer.compile(jsonPointerExpression);
    }

    @Override
    public String detectEventDefinitionKey(JsonNode event) {
        JsonNode result = event.at(jsonPointerExpression);

        if (result == null || result.isMissingNode() || result.isNull()) {
            LOGGER.warn("JsonPointer expression {} did not detect event key", jsonPointerExpression);
            return null;
        }

        if (result.isTextual()) {
            return result.asText();
        }

        return null;
    }
    
    public String getJsonPointerValue() {
        return jsonPointerValue;
    }
}
