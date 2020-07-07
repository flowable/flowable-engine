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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.flowable.eventregistry.json.converter.EventJsonConverter;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class EventJsonConverterTest {

    private static final String JSON_RESOURCE_1 = "org/flowable/eventregistry/converter/simpleEvent.json";
    private static final String JSON_RESOURCE_2 = "org/flowable/eventregistry/converter/simpleEventCorrelationPayload.json";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testConvertJsonToModel() {
        EventModel eventModel = readJson(JSON_RESOURCE_1);
        validateSimpleEventModel(eventModel);
    }

    @Test
    public void testModelToJson() {
        EventModel eventModel = readJson(JSON_RESOURCE_1);
        EventModel parsedEventModel = exportAndReadModel(eventModel);
        validateSimpleEventModel(parsedEventModel);
    }

    @Test
    public void testConvertCorrelationPayloadJsonToModel() {
        EventModel eventModel = readJson(JSON_RESOURCE_2);
        validateSimpleEventModel(eventModel);
    }

    @Test
    public void testCorrelationPayloadModelToJson() {
        EventModel eventModel = readJson(JSON_RESOURCE_2);
        EventModel parsedEventModel = exportAndReadModel(eventModel);
        validateSimpleEventModel(parsedEventModel);
    }

    protected void validateSimpleEventModel(EventModel eventModel) {
        assertThat(eventModel).isNotNull();
        assertThat(eventModel.getKey()).isEqualTo("myEvent");
        assertThat(eventModel.getName()).isEqualTo("My event");
        
        assertThat(eventModel.getCorrelationParameters())
                .extracting(EventPayload::getName, EventPayload::getType)
                .containsExactly(tuple("customerId", "string"));
        
        assertThat(eventModel.getPayload())
                .extracting(EventPayload::getName, EventPayload::getType)
                .containsExactly(
                        tuple("payload1", "string"),
                        tuple("payload2", "integer"),
                        tuple("customerId", "string")
                );
    }

    /* Helper methods */
    protected String readJsonToString(String resource) {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resource)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not read " + resource + " : " + e.getMessage());
            return null;
        }
    }

    protected EventModel readJson(String resource) {
        String modelJson = readJsonToString(resource);
        return new EventJsonConverter().convertToEventModel(modelJson);
    }

    protected EventModel exportAndReadModel(EventModel eventModel) {
        String modelJson = new EventJsonConverter().convertToJson(eventModel);
        return new EventJsonConverter().convertToEventModel(modelJson);
    }

}
