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
package org.flowable.dmn.rest.service.api.decision;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Yvo Swillens
 */
public class DmnRuleServiceResourceTest extends BaseSpringDmnRestTestCase {

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/multi-hit.dmn" })
    public void testExecuteWithDecision() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision1");

        requestNode.set("inputVariables", createDecisionTableMultiHitPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertThat(resultVariables).hasSize(3);
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/multi-hit.dmn" })
    public void testExecuteWithDecisionWithoutHistory() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision1");
        requestNode.put("disableHistory", true);

        requestNode.set("inputVariables", createDecisionTableMultiHitPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_CREATED);
        verifyNoHistory();
    }



    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/single-hit.dmn" })
    public void testExecuteWithDecisionSingleResult() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision");

        requestNode.set("inputVariables", createDecisionTableSingleHitPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertThat(resultVariables).hasSize(1);
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/single-hit.dmn" })
    public void testExecuteWithDecisionSingleResultWithoutHistory() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision");
        requestNode.put("disableHistory", true);

        requestNode.set("inputVariables", createDecisionTableSingleHitPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_CREATED);
        verifyNoHistory();

    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/multi-hit.dmn" })
    public void testExecuteWithDecisionSingleResultViolated() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision1");
        requestNode.put("disableHistory", true);

        requestNode.set("inputVariables", createDecisionTableMultiHitPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/decision-service-multiple-output-decisions.dmn" })
    public void testExecuteWithDecisionService() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "expandedDecisionService");

        requestNode.set("inputVariables", createDecisionServiceRequestPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertThatJson(resultVariables)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "    ["
                        + "        {"
                        + "            \"name\": \"output1\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"NOT EMPTY\""
                        + "        }"
                        + "    ],"
                        + "    ["
                        + "        {"
                        + "            \"name\": \"output1\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"NOT EMPTY 2\""
                        + "        }"
                        + "    ],"
                        + "    ["
                        + "        {"
                        + "            \"name\": \"output2\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"NOT EMPTY\""
                        + "        }"
                        + "    ]"
                        + "]");
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/decision-service-multiple-output-decisions.dmn" })
    public void testExecuteWithDecisionServiceWithoutHistory() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "expandedDecisionService");

        requestNode.set("inputVariables", createDecisionServiceRequestPayload());
        requestNode.put("disableHistory", true);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_CREATED);
        verifyNoHistory();

    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/evaluate-mortgage-request-service.dmn" })
    public void testExecuteWithDecisionServiceSingleResult() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "evaluateMortgageRequestService");

        requestNode.set("inputVariables", createMortgageRequestPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");
        assertThatJson(resultVariables)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "    ["
                        + "        {"
                        + "            \"name\": \"approval\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"APPROVED\""
                        + "        }"
                        + "    ]"
                        + "]");
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/evaluate-mortgage-request-service.dmn" })
    public void testExecuteWithDecisionServiceSingleResultWithoutHistory() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "evaluateMortgageRequestService");

        requestNode.set("inputVariables", createMortgageRequestPayload());
        requestNode.put("disableHistory", true);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_CREATED);
        verifyNoHistory();

    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/decision-service-multiple-output-decisions.dmn" })
    public void testExecuteWithDecisionServiceSingleResultViolated() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "expandedDecisionService");

        requestNode.set("inputVariables", createDecisionServiceRequestPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/multi-hit.dmn" })
    public void testExecuteDecision() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision1");

        requestNode.set("inputVariables", createDecisionTableSingleHitPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertThatJson(resultVariables)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "    ["
                        + "        {"
                        + "            \"name\": \"outputVariable1\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"result1\""
                        + "        }"
                        + "    ],"
                        + "    ["
                        + "        {"
                        + "            \"name\": \"outputVariable1\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"result3\""
                        + "        }"
                        + "    ],"
                        + "    ["
                        + "        {"
                        + "            \"name\": \"outputVariable1\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"result4\""
                        + "        }"
                        + "    ]"
                        + "]");
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/multi-hit.dmn" })
    public void testExecuteDecisionWithoutHistory() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision1");

        requestNode.set("inputVariables", createDecisionTableSingleHitPayload());
        requestNode.put("disableHistory", true);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_CREATED);
        verifyNoHistory();

    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/single-hit.dmn" })
    public void testExecuteDecisionSingleResult() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision");

        requestNode.set("inputVariables", createDecisionTableSingleHitPayload());

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertThatJson(resultVariables)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "        {"
                        + "            \"name\": \"outputVariable1\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"result2\""
                        + "        }"
                        + "]");
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/single-hit.dmn" })
    public void testExecuteDecisionSingleResultWithoutHistory() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision");

        requestNode.set("inputVariables", createDecisionTableSingleHitPayload());
        requestNode.put("disableHistory", true);

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_CREATED);
        verifyNoHistory();

    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/multi-hit.dmn" })
    public void testExecuteDecisionSingleResultViolated() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision1");

        requestNode.set("inputVariables", createDecisionTableSingleHitPayload());

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/decision-service-multiple-output-decisions.dmn" })
    public void testExecuteDecisionService() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "expandedDecisionService");

        requestNode.set("inputVariables", createDecisionServiceRequestPayload());

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION_SERVICE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertThatJson(resultVariables)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "    ["
                        + "        {"
                        + "            \"name\": \"output1\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"NOT EMPTY\""
                        + "        }"
                        + "    ],"
                        + "    ["
                        + "        {"
                        + "            \"name\": \"output1\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"NOT EMPTY 2\""
                        + "        }"
                        + "    ],"
                        + "    ["
                        + "        {"
                        + "            \"name\": \"output2\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"NOT EMPTY\""
                        + "        }"
                        + "    ]"
                        + "]");
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/decision-service-multiple-output-decisions.dmn" })
    public void testExecuteDecisionServiceWithoutHistory() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "expandedDecisionService");

        requestNode.set("inputVariables", createDecisionServiceRequestPayload());
        requestNode.put("disableHistory", true);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION_SERVICE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_CREATED);
        verifyNoHistory();

    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/evaluate-mortgage-request-service.dmn" })
    public void testExecuteDecisionServiceSingleResult() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "evaluateMortgageRequestService");

        requestNode.set("inputVariables", createMortgageRequestPayload());

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION_SERVICE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertThatJson(resultVariables)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "        {"
                        + "            \"name\": \"approval\","
                        + "            \"type\": \"string\","
                        + "            \"value\": \"APPROVED\""
                        + "        }"
                        + "]");
    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/evaluate-mortgage-request-service.dmn" })
    public void testExecuteDecisionServiceSingleResultWithoutHistory() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "evaluateMortgageRequestService");

        requestNode.set("inputVariables", createMortgageRequestPayload());
        requestNode.put("disableHistory", true);

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION_SERVICE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_CREATED);
        verifyNoHistory();

    }

    @Test
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/decision-service-multiple-output-decisions.dmn" })
    public void testExecuteDecisionServiceSingleResultViolated() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "expandedDecisionService");

        requestNode.set("inputVariables", createDecisionServiceRequestPayload());

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_DECISION_SERVICE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    protected ArrayNode createDecisionTableMultiHitPayload() {
        ArrayNode variablesNode = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableNode.put("name", "inputVariable1");
        variableNode.put("type", "integer");
        variableNode.put("value", 5);
        variablesNode.add(variableNode);

        return variablesNode;
    }

    protected ArrayNode createDecisionTableSingleHitPayload() {
        ArrayNode variablesNode = objectMapper.createArrayNode();
        ObjectNode variableNode1 = objectMapper.createObjectNode();
        variableNode1.put("name", "inputVariable1");
        variableNode1.put("type", "integer");
        variableNode1.put("value", 5);
        variablesNode.add(variableNode1);

        ObjectNode variableNode2 = objectMapper.createObjectNode();
        variableNode2.put("name", "inputVariable2");
        variableNode2.put("type", "string");
        variableNode2.put("value", "test2");
        variablesNode.add(variableNode2);

        return variablesNode;
    }

    protected ArrayNode createMortgageRequestPayload() {
        ArrayNode variablesNode = objectMapper.createArrayNode();

        ObjectNode variableNode1 = objectMapper.createObjectNode();
        variableNode1.put("name", "housePrice");
        variableNode1.put("type", "double");
        variableNode1.put("value", 300000D);
        variablesNode.add(variableNode1);

        ObjectNode variableNode2 = objectMapper.createObjectNode();
        variableNode2.put("name", "age");
        variableNode2.put("type", "double");
        variableNode2.put("value", 42D);
        variablesNode.add(variableNode2);

        ObjectNode variableNode3 = objectMapper.createObjectNode();
        variableNode3.put("name", "region");
        variableNode3.put("type", "string");
        variableNode3.put("value", "CITY_CENTRE");
        variablesNode.add(variableNode3);

        ObjectNode variableNode4 = objectMapper.createObjectNode();
        variableNode4.put("name", "doctorVisit");
        variableNode4.put("type", "boolean");
        variableNode4.put("value", false);
        variablesNode.add(variableNode4);

        ObjectNode variableNode5 = objectMapper.createObjectNode();
        variableNode5.put("name", "hospitalVisit");
        variableNode5.put("type", "boolean");
        variableNode5.put("value", false);
        variablesNode.add(variableNode5);

        return variablesNode;
    }

    protected ArrayNode createDecisionServiceRequestPayload() {
        ArrayNode variablesNode = objectMapper.createArrayNode();

        ObjectNode variableNode1 = objectMapper.createObjectNode();
        variableNode1.put("name", "input1");
        variableNode1.put("type", "string");
        variableNode1.put("value", "test1");
        variablesNode.add(variableNode1);

        ObjectNode variableNode2 = objectMapper.createObjectNode();
        variableNode2.put("name", "input2");
        variableNode2.put("type", "string");
        variableNode2.put("value", "test2");
        variablesNode.add(variableNode2);

        ObjectNode variableNode3 = objectMapper.createObjectNode();
        variableNode3.put("name", "input3");
        variableNode3.put("type", "string");
        variableNode3.put("value", "test3");
        variablesNode.add(variableNode3);

        ObjectNode variableNode4 = objectMapper.createObjectNode();
        variableNode4.put("name", "input4");
        variableNode4.put("type", "string");
        variableNode4.put("value", "test4");
        variablesNode.add(variableNode4);

        return variablesNode;
    }

    protected void verifyNoHistory() throws IOException {

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_HISTORIC_DECISION_EXECUTION_COLLECTION));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("data");

        assertThat(resultVariables).hasSize(0);
    }

}
