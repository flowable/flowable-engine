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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Yvo Swillens
 */
public class DecisionResourceTest extends BaseSpringDmnRestTestCase {

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testGetDecision() throws Exception {
        DmnDecision decision = dmnRepositoryService.createDecisionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION, decision.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                    + "  id: '" + decision.getId() + "',"
                    + "  url: '" + httpGet.getURI().toString() + "',"
                    + "  category: " + decision.getCategory() + ","
                    + "  name: '" + decision.getName() + "',"
                    + "  key: '" + decision.getKey() + "',"
                    + "  description: " + decision.getDescription() + ","
                    + "  version: " + decision.getVersion() + ","
                    + "  decisionType: '" + decision.getDecisionType() + "',"
                    + "  deploymentId: '" + decision.getDeploymentId() + "',"
                    + "  decisionType: '" + decision.getDecisionType() + "'"
                    + "  }"
                );
    }

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testGetUnexistingDecision() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/decision_service-1.dmn" })
    public void testGetDecisionService() throws Exception {
        DmnDecision decision = dmnRepositoryService.createDecisionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION, decision.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + decision.getId() + "',"
                        + "  url: '" + httpGet.getURI().toString() + "',"
                        + "  category: " + decision.getCategory() + ","
                        + "  name: '" + decision.getName() + "',"
                        + "  key: '" + decision.getKey() + "',"
                        + "  description: " + decision.getDescription() + ","
                        + "  version: " + decision.getVersion() + ","
                        + "  decisionType: '" + decision.getDecisionType() + "',"
                        + "  deploymentId: '" + decision.getDeploymentId() + "',"
                        + "  decisionType: '" + decision.getDecisionType() + "'"
                        + "  }"
                );
    }
}
