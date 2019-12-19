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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.flowable.eventregistry.json.converter.EventJsonConverter;
import org.flowable.eventregistry.model.EventCorrelationParameterDefinition;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayloadDefinition;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class EventJsonConverterTest {

    private static final String JSON_RESOURCE_1 = "org/flowable/eventregistry/converter/simpleEvent.json";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSimpleJsonEvent() throws Exception {

        String testJsonResource = readJsonToString(JSON_RESOURCE_1);
        EventModel eventModel = new EventJsonConverter().convertToEventModel(testJsonResource);

        assertNotNull(eventModel);
        assertEquals("myEvent", eventModel.getKey());
        assertEquals("My event", eventModel.getName());
        
        assertEquals(1, eventModel.getInboundChannelKeys().size());
        assertEquals("test-channel", eventModel.getInboundChannelKeys().iterator().next());
        
        assertEquals(1, eventModel.getCorrelationParameters().size());
        EventCorrelationParameterDefinition correlationParameter = eventModel.getCorrelationParameters().iterator().next();
        assertEquals("customerId", correlationParameter.getName());
        assertEquals("string", correlationParameter.getType());
        
        assertEquals(2, eventModel.getPayload().size());
        Iterator<EventPayloadDefinition> itPayload = eventModel.getPayload().iterator();
        EventPayloadDefinition payloadDefinition = itPayload.next();
        assertEquals("payload1", payloadDefinition.getName());
        assertEquals("string", payloadDefinition.getType());
        
        payloadDefinition = itPayload.next();
        assertEquals("payload2", payloadDefinition.getName());
        assertEquals("integer", payloadDefinition.getType());
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

    protected JsonNode parseJson(String resource) {
        String jsonString = readJsonToString(resource);
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            fail("Could not parse " + resource + " : " + e.getMessage());
        }
        return null;
    }
}
