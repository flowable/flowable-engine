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
package org.flowable.eventregistry.converter;

import org.flowable.eventregistry.model.EventModel;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class EventJsonConverter {

    protected ObjectMapper objectMapper = new ObjectMapper();

    public EventModel convertToEventModel(String modelJson) {
        try {
            EventModel definition = objectMapper.readValue(modelJson, EventModel.class);

            return definition;
        } catch (Exception e) {
            throw new FlowableEventJsonException("Error reading event json", e);
        }
    }

    public String convertToJson(EventModel definition) {
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (Exception e) {
            throw new FlowableEventJsonException("Error writing event json", e);
        }
    }
}