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
package org.flowable.dmn.rest.service.api.history;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Tijs Rademakers
 */
public class HistoricDecisionExecutionAuditDataResourceTest extends BaseSpringDmnRestTestCase {

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testGetHistoricDecisionExecutionAuditData() throws Exception {

        Map<String, Object> variables = new HashMap<>();
        variables.put("inputVariable1", 1);
        variables.put("inputVariable2", "test");
        dmnRuleService.createExecuteDecisionBuilder().decisionKey("decision").variables(variables).activityId("test1").instanceId("instance1")
                .executeWithSingleResult();

        String executionId1 = dmnHistoryService.createHistoricDecisionExecutionQuery().activityId("test1").singleResult().getId();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_HISTORIC_DECISION_EXECUTION_AUDITDATA, executionId1));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).isNotNull();

        JsonNode auditNode = new ObjectMapper().readTree(content);
        assertThatJson(auditNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   decisionKey: 'decision',"
                        + "   decisionName: 'Full Decision'"
                        + " }");
    }

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/decision_service-2.dmn" })
    public void testGetHistoricDecisionServiceExecutionAuditData() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("housePrice", 300000D);
        variables.put("age", 42D);
        variables.put("region", "CITY_CENTRE");
        variables.put("doctorVisit", false);
        variables.put("hospitalVisit", false);
        dmnRuleService.createExecuteDecisionBuilder().decisionKey("evaluateMortgageRequestService").variables(variables).activityId("test1")
                .instanceId("instance1").executeWithSingleResult();

        String executionId1 = dmnHistoryService.createHistoricDecisionExecutionQuery().activityId("test1").singleResult().getId();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_HISTORIC_DECISION_EXECUTION_AUDITDATA, executionId1));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).isNotNull();

        JsonNode auditNode = new ObjectMapper().readTree(content);
        assertThatJson(auditNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   decisionKey: 'evaluateMortgageRequestService',"
                        + "   decisionName: 'Evaluate Mortgage Request Service'"
                        + " }");
    }

    public void testGetDecisionTableResourceForUnexistingDecisionTable() throws Exception {
        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_HISTORIC_DECISION_EXECUTION_AUDITDATA, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        try (InputStream contentStream = response.getEntity().getContent()) {
            JsonNode errorNode = objectMapper.readTree(contentStream);
            assertThatJson(errorNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "   exception: 'Could not find a decision execution with id \\'unexisting\\''"
                            + " }");
        }
        closeResponse(response);
    }
}
