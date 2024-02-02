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
import static org.assertj.core.api.Assertions.assertThat;

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
public class DecisionModelResourceTest extends BaseSpringDmnRestTestCase {

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testGetDecisionModel() throws Exception {

        DmnDecision decision = dmnRepositoryService.createDecisionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION_MODEL, decision.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode resultNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(resultNode).isNotNull();
        JsonNode firstDecision = resultNode.get("decisions").get(0);
        assertThat(firstDecision).isNotNull();

        JsonNode decisionTableNode = firstDecision.get("expression");
        assertThatJson(decisionTableNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   id: 'decisionTable'"
                        + " }");
    }

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/decision_service-2.dmn" })
    public void testGetDecisionServiceModel() throws Exception {
        DmnDecision decision = dmnRepositoryService.createDecisionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION_MODEL, decision.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode resultNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(resultNode).isNotNull();
        assertThat(resultNode.get("decisions").size()).isEqualTo(4);

        assertThat(resultNode.get("decisionServices").size()).isEqualTo(1);
        JsonNode firstDecisionService = resultNode.get("decisionServices").get(0);
        assertThat(firstDecisionService).isNotNull();

        assertThatJson(firstDecisionService)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   id: 'evaluateMortgageRequestService'"
                        + " }");
    }
}
