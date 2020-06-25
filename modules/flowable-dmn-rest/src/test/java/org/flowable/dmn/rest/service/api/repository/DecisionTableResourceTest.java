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
package org.flowable.dmn.rest.service.api.repository;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Yvo Swillens
 */
public class DecisionTableResourceTest extends BaseSpringDmnRestTestCase {

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testGetDecisionTable() throws Exception {

        DmnDecision definition = dmnRepositoryService.createDecisionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION_TABLE, definition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals(definition.getId(), responseNode.get("id").textValue());
        assertEquals(definition.getKey(), responseNode.get("key").textValue());
        assertEquals(definition.getCategory(), responseNode.get("category").textValue());
        assertEquals(definition.getVersion(), responseNode.get("version").intValue());
        assertEquals(definition.getDescription(), responseNode.get("description").textValue());
        assertEquals(definition.getName(), responseNode.get("name").textValue());

        // Check URL's
        assertEquals(httpGet.getURI().toString(), responseNode.get("url").asText());
        assertEquals(definition.getDeploymentId(), responseNode.get("deploymentId").textValue());
    }

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testGetUnexistingDecisionTable() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION_TABLE, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

}
