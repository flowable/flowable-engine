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
package org.flowable.rest.service.api.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to Properties
 *
 * @author Frederik Heremans
 */
public class PropertiesCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting the engine properties.
     */
    @Test
    public void testGetProperties() throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROPERTIES_COLLECTION)),
                HttpStatus.SC_OK);

        Map<String, String> properties = managementService.getProperties();

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).hasSize(properties.size());

        Iterator<Map.Entry<String, JsonNode>> nodes = responseNode.fields();
        Map.Entry<String, JsonNode> node = null;
        while (nodes.hasNext()) {
            node = nodes.next();
            String propValue = properties.get(node.getKey());
            assertThat(propValue).isNotNull();
            assertThat(node.getValue().textValue()).isEqualTo(propValue);
        }
    }
}
