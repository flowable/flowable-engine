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

package org.flowable.cmmn.rest.service.api.history;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to the historic case instance query resource.
 *
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceQueryResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic case instance based on variables. POST cmmn-query/historic-case-instances
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryCaseInstancesWithVariables() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        identityService.setAuthenticatedUserId("kermit");
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .businessKey("myBusinessKey")
                .businessStatus("myBusinessStatus")
                .variables(caseVariables)
                .start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());

        caseVariables.put("oneVar", "test");

        identityService.setAuthenticatedUserId("fozzie");
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .businessKey("anotherBusinessKey")
                .businessStatus("anotherBusinessStatus")
                .variables(caseVariables)
                .start();
        
        identityService.setAuthenticatedUserId(null);

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceBusinessKey", "myBusinessKey");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());
        
        requestNode.put("caseInstanceBusinessKey", "none");
        assertResultsPresentInPostDataResponse(url, requestNode);
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceBusinessStatus", "myBusinessStatus");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());
        
        requestNode.put("caseInstanceBusinessStatus", "none");
        assertResultsPresentInPostDataResponse(url, requestNode);
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceState", "active");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance2.getId());
        
        requestNode.put("caseInstanceState", "completed");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("startedBy", "kermit");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());
        
        requestNode.put("startedBy", "fozzie");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("finishedBy", "kermit");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        Calendar todayCal = new GregorianCalendar();
        Calendar futureCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) + 2, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));
        Calendar historicCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) - 2, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_MONTH));
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("startedBefore", getISODateString(futureCal.getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());
        
        requestNode.put("startedBefore", getISODateString(historicCal.getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode);
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("startedAfter", getISODateString(historicCal.getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());
        
        requestNode.put("startedAfter", getISODateString(futureCal.getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode);
        
        // Case variables
        requestNode = objectMapper.createObjectNode();
        ArrayNode variableArray = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableArray.add(variableNode);
        requestNode.set("variables", variableArray);

        // String equals
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azeRTY");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        // String not equals ignore case (not supported)
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "HIJKLm");
        variableNode.put("operation", "notEqualsIgnoreCase");
        assertErrorResult(url, requestNode, HttpStatus.SC_BAD_REQUEST);

        // String equals without value
        variableNode.removeAll();
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        variableNode.removeAll();
        variableNode.put("name", "oneVar");
        variableNode.put("value", "test");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance2.getId());

        // String equals with non existing value
        variableNode.removeAll();
        variableNode.put("value", "Azerty2");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode);

        // String like ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azerty");
        variableNode.put("operation", "likeIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azerty2");
        variableNode.put("operation", "likeIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode);

        requestNode = objectMapper.createObjectNode();
        requestNode.put("finished", true);
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("finished", false);
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseDefinitionId", caseInstance.getCaseDefinitionId());
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseDefinitionKey", "oneHumanTaskCase");
        assertResultsPresentInPostDataResponse(url, requestNode, caseInstance.getId(), caseInstance2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseDefinitionKey", "oneHumanTaskCase");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url + "?sort=startTime");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    id: '" + caseInstance.getId() + "',"
                        + "    caseDefinitionName: 'One Human Task Case',"
                        + "    caseDefinitionDescription: 'A human task case'"
                        + "  },"
                        + "  {"
                        + "    id: '" + caseInstance2.getId() + "'"
                        + "  }"
                        + "]");
    }
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryCaseInstancesByActivePlanItemDefinitionId() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        // Test without any parameters
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("activePlanItemDefinitionId", "task1");

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryCaseInstancesByActivePlanItemDefinitionIds() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        // Test without any parameters
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode itemArrayNode = requestNode.putArray("activePlanItemDefinitionIds");
        itemArrayNode.add("task1");
        itemArrayNode.add("task3");

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryCaseInstancesByWithoutParentId() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        CaseInstance parentInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withoutBoth")
                .start();
        
        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withParentId").parentId(parentInstance.getId())
                .start();
        
        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withCallBackId").callbackId("testID")
                .start();
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("withoutCaseInstanceParentId", "true");
        
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { name: 'withoutBoth' },"
                        + "    { name: 'withCallBackId' }"
                        + "  ]"
                        + "}");
        
    }
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryCaseInstancesByWithoutCallbackId() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        CaseInstance parentInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withoutBoth")
                .start();
        
        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withParentId").parentId(parentInstance.getId())
                .start();
        
        runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("withCallBackId").callbackId("testID")
                .start();
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("withoutCaseInstanceCallbackId", "true");
        
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { name: 'withoutBoth' },"
                        + "    { name: 'withParentId' }"
                        + "  ]"
                        + "}");
        
    }
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn",
        "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryHistoricCaseInstancesByCaseDefinitionKeys() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode itemArrayNode = requestNode.putArray("caseDefinitionKeys");
        itemArrayNode.add("myCase");
        
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
                        + "  },"
                        + "  {"
                        + "    id: '" + caseInstance2.getId() + "'"
                        + "  }"
                        + "]");
        
        requestNode = objectMapper.createObjectNode();
        itemArrayNode = requestNode.putArray("caseDefinitionKeys");
        itemArrayNode.add("myCase");
        itemArrayNode.add("oneHumanTaskCase");
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
                        + "  },"
                        + "  {"
                        + "    id: '" + caseInstance2.getId() + "'"
                        + "  },"
                        + "  {"
                        + "    id: '" + caseInstance3.getId() + "'"
                        + "  }"
                        + "]");
        
        requestNode = objectMapper.createObjectNode();
        itemArrayNode = requestNode.putArray("caseDefinitionKeys");
        itemArrayNode.add("notExisting");
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        
        assertThatJson(dataNode)
            .isEqualTo("[]");
    }
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn",
        "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryHistoricCaseInstancesByExcludeCaseDefinitionKeys() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode itemArrayNode = requestNode.putArray("excludeCaseDefinitionKeys");
        itemArrayNode.add("myCase");
        
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        JsonNode dataNode = rootNode.get("data");
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("["
                        + "  {"
                        + "    id: '" + caseInstance3.getId() + "'"
                        + "  }"
                        + "]");
        
        requestNode = objectMapper.createObjectNode();
        itemArrayNode = requestNode.putArray("excludeCaseDefinitionKeys");
        itemArrayNode.add("myCase");
        itemArrayNode.add("oneHumanTaskCase");
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        
        assertThatJson(dataNode)
            .isEqualTo("[]");
        
        requestNode = objectMapper.createObjectNode();
        itemArrayNode = requestNode.putArray("excludeCaseDefinitionKeys");
        itemArrayNode.add("notExisting");
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        dataNode = rootNode.get("data");
        
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("["
                        + "  {"
                        + "    id: '" + caseInstance.getId() + "'"
                        + "  },"
                        + "  {"
                        + "    id: '" + caseInstance2.getId() + "'"
                        + "  },"
                        + "  {"
                        + "    id: '" + caseInstance3.getId() + "'"
                        + "  }"
                        + "]");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
    public void testQueryHistoricCaseInstancesByCaseInstanceIds() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        ObjectNode requestNode = objectMapper.createObjectNode();

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .inPath("data")
                .isEqualTo("""
                        [
                          { id: '%s' },
                          { id: '%s' },
                          { id: '%s' }
                        ]
                        """.formatted(caseInstance.getId(), caseInstance2.getId(), caseInstance3.getId()));

        requestNode = objectMapper.createObjectNode();
        ArrayNode itemArrayNode = requestNode.putArray("caseInstanceIds");
        itemArrayNode.add(caseInstance.getId());
        itemArrayNode.add(caseInstance3.getId());

        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .inPath("data")
                .isEqualTo("""
                        [
                          { id: '%s' },
                          { id: '%s' }
                        ]
                        """.formatted(caseInstance.getId(), caseInstance3.getId()));

        requestNode = objectMapper.createObjectNode();
        itemArrayNode = requestNode.putArray("caseInstanceIds");
        itemArrayNode.add("notExisting");

        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThatJson(rootNode)
                .inPath("data")
                .isEqualTo("[]");
    }

    @CmmnDeployment(resources = {"org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn"})
    public void testQueryHistoricCaseInstancesByCaseInstanceCallbackIds() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().callbackId("callBackId1").caseDefinitionKey("oneHumanTaskCase").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().callbackId("callBackId2").caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.createCaseInstanceBuilder().callbackId("callBackId3").caseDefinitionKey("oneHumanTaskCase").start();

        taskService.createTaskQuery().list().forEach(task -> {
            taskService.complete(task.getId());
        });

        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode itemArrayNode = requestNode.putArray("caseInstanceCallbackIds");
        itemArrayNode.add("callBackId1");
        itemArrayNode.add("callBackId2");
        itemArrayNode.add("someId");
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_QUERY);
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
                        + "  },"
                        + "  {"
                        + "    id: '" + caseInstance2.getId() + "'"
                        + "  }"
                        + "]");

    }
}
