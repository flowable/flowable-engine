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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yvo Swillens
 */
public class DmnRuleServiceResourceTest extends BaseSpringDmnRestTestCase {

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/multi-hit.dmn" })
    public void testExecutionDecision() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision1");

        // add input variable
        ArrayNode variablesNode = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableNode.put("name", "inputVariable1");
        variableNode.put("type", "integer");
        variableNode.put("value", 5);
        variablesNode.add(variableNode);

        requestNode.set("inputVariables", variablesNode);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertEquals(3, resultVariables.size());
    }


    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/single-hit.dmn" })
    public void testExecutionDecisionSingleResult() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision");

        // add input variable
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

        requestNode.set("inputVariables", variablesNode);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        // Check response
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        ArrayNode resultVariables = (ArrayNode) responseNode.get("resultVariables");

        assertEquals(1, resultVariables.size());
    }


    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/decision/multi-hit.dmn" })
    public void testExecutionDecisionSingleResultViolated() throws Exception {
        // Add decision key
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("decisionKey", "decision1");

        // add input variable
        ArrayNode variablesNode = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableNode.put("name", "inputVariable1");
        variableNode.put("type", "integer");
        variableNode.put("value", 5);
        variablesNode.add(variableNode);

        requestNode.set("inputVariables", variablesNode);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_RULE_SERVICE_EXECUTE_SINGLE_RESULT));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPost, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

}
