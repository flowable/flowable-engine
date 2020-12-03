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

package org.flowable.cmmn.rest.service.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to the case instance query resource.
 *
 * @author Yvo Swillens
 */
public class CaseInstanceQueryResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying case instance based on variables. POST query/case-instances
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryCaseInstancesWithVariables() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_QUERY);

        // Process variables
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode variableArray = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableArray.add(variableNode);
        requestNode.set("variables", variableArray);

        // String equals
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azeRTY");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // String not equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "HIJKLm");
        variableNode.put("operation", "notEqualsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // String like
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azer%");
        variableNode.put("operation", "like");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // String like ignore case
        variableNode.put("name", "stringVar");
        variableNode.put("value", "AzEr%");
        variableNode.put("operation", "likeIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // String equals without value
        variableNode.removeAll();
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        // String equals with non existing value
        variableNode.removeAll();
        variableNode.put("value", "Azerty2");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode);
    }

    /**
     * Test querying case instance based on variables. POST query/case-instances
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn",
            "org/flowable/cmmn/rest/service/api/repository/repeatingStage.cmmn" })
    public void testQueryCaseInstancesPagingAndSorting() throws Exception {
        Authentication.setAuthenticatedUserId("queryCaseUser");
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingStage").start();

        // Create request node
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("order", "desc");
        requestNode.put("sort", "caseDefinitionKey");

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check order
        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        JsonNode dataNode = rootNode.get("data");
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    id: '" + caseInstance2.getId() + "'"
                        + "  },"
                        + "  {"
                        + "    id: '" + caseInstance1.getId() + "'"
                        + "  }"
                        + "]");

        // Check paging size
        requestNode = objectMapper.createObjectNode();
        requestNode.put("start", 0);
        requestNode.put("size", 1);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        assertThat(dataNode).hasSize(1);

        // Check paging start and size
        requestNode = objectMapper.createObjectNode();
        requestNode.put("start", 1);
        requestNode.put("size", 1);
        requestNode.put("order", "desc");
        requestNode.put("sort", "caseDefinitionKey");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    id: '" + caseInstance1.getId() + "',"
                        + "    caseDefinitionName: 'One Human Task Case',"
                        + "    caseDefinitionDescription: 'A human task case',"
                        + "    startTime: " + new TextNode(getISODateStringWithTZ(caseInstance1.getStartTime())) + ","
                        + "    startUserId: 'queryCaseUser'"
                        + "  }"
                        + "]");
    }
    
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryCaseInstancesByActivePlanItemDefinitionId() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        // Test without any parameters
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("activePlanItemDefinitionId", "task1");

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        JsonNode dataNode = rootNode.get("data");
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    id: '" + caseInstance.getId() + "'"
                        + "  }"
                        + "]");
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("activePlanItemDefinitionId", "task2");
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("[]");
        
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("activePlanItemDefinitionId", "task2");
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");

        assertThatJson(dataNode)
            .when(Option.IGNORING_EXTRA_FIELDS)
            .isEqualTo("["
                    + "  {"
                    + "    id: '" + caseInstance.getId() + "'"
                    + "  }"
                    + "]");
    }
    
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryCaseInstancesByActivePlanItemDefinitionIds() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        // Test without any parameters
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode itemArrayNode = requestNode.putArray("activePlanItemDefinitionIds");
        itemArrayNode.add("task1");
        itemArrayNode.add("task3");

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        JsonNode dataNode = rootNode.get("data");
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    id: '" + caseInstance.getId() + "'"
                        + "  }"
                        + "]");
        
        requestNode = objectMapper.createObjectNode();
        itemArrayNode = requestNode.putArray("activePlanItemDefinitionIds");
        itemArrayNode.add("task2");
        itemArrayNode.add("task3");
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("[]");
        
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        requestNode = objectMapper.createObjectNode();
        itemArrayNode = requestNode.putArray("activePlanItemDefinitionIds");
        itemArrayNode.add("task2");
        itemArrayNode.add("task3");
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");

        assertThatJson(dataNode)
            .when(Option.IGNORING_EXTRA_FIELDS)
            .isEqualTo("["
                    + "  {"
                    + "    id: '" + caseInstance.getId() + "'"
                    + "  }"
                    + "]");
    }
}
