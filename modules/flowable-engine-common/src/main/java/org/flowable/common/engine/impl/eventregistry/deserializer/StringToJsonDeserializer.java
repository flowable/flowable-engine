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
package org.flowable.common.engine.impl.eventregistry.deserializer;

import java.io.IOException;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.eventregistry.EventProcessingContext;
import org.flowable.common.engine.api.eventregistry.InboundEventDeserializer;
import org.flowable.common.engine.impl.eventregistry.constant.EventProcessingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 */
public class StringToJsonDeserializer implements InboundEventDeserializer {

    private static Logger LOGGER = LoggerFactory.getLogger(StringToJsonDeserializer.class);

    public static String TYPE = "stringToJson";

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void deserialize(String rawEvent, EventProcessingContext eventProcessingContext) {
        try {
            JsonNode jsonNode = objectMapper.readTree(rawEvent);
            eventProcessingContext.addProcessingData(EventProcessingConstants.DESERIALIZED_JSON_NODE, jsonNode);
        } catch (IOException e) {
            throw new FlowableException("Could not deserialize event to json", e);
        }
    }
}
