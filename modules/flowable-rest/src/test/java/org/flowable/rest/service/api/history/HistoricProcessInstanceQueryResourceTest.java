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

package org.flowable.rest.service.api.history;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to the historic process instance query resource.
 *
 * @author Tijs Rademakers
 */
public class HistoricProcessInstanceQueryResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic process instance based on variables. POST query/historic-process-instances
     */
    @Test
    @Deployment
    public void testQueryProcessInstancesWithVariables() throws Exception {
        HashMap<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringVar", "Azerty");
        processVariables.put("intVar", 67890);
        processVariables.put("booleanVar", false);

        Authentication.setAuthenticatedUserId("historyQueryAndSortUser");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_QUERY);

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
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azeRTY");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

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
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

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
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azerty2");
        variableNode.put("operation", "likeIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode);

        requestNode = objectMapper.createObjectNode();
        requestNode.put("finished", true);
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("finished", false);
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", processInstance.getProcessDefinitionId());
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionKey", "oneTaskProcess");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId(), processInstance2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("finishedBy", "historyQueryAndSortUser");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("state", ProcessInstanceState.COMPLETED);
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        requestNode.put("state", ProcessInstanceState.RUNNING);
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionKey", "oneTaskProcess");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url + "?sort=startTime");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "{"
                        + "    id: '" + processInstance.getId() + "',"
                        + "    processDefinitionName: 'The One Task Process',"
                        + "    processDefinitionDescription: 'One task process description',"
                        + "    startTime: '${json-unit.any-string}',"
                        + "    startUserId: '" + processInstance.getStartUserId() + "'"
                        + "},"
                        + "{"
                        + "    id: '" + processInstance2.getId() + "'"
                        + "}"
                        + "]");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testQueryProcessInstancesByActiveActivityId() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("activeActivityId", "processTask");
        
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "   id: '" + processInstance.getId() + "',"
                        + "   processDefinitionId: '" + processInstance.getProcessDefinitionId() + "'"
                        + "} ]"
                        + "}");
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("activeActivityId", "processTask2");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: []"
                        + "}");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        requestNode = objectMapper.createObjectNode();
        requestNode.put("activeActivityId", "processTask2");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
        .when(Option.IGNORING_EXTRA_FIELDS)
        .isEqualTo("{"
                + "data: [ {"
                + "   id: '" + processInstance.getId() + "',"
                + "   processDefinitionId: '" + processInstance.getProcessDefinitionId() + "'"
                + "} ]"
                + "}");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testQueryProcessInstancesByActiveActivityIds() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode activityIdArray = requestNode.putArray("activeActivityIds");
        activityIdArray.add("processTask");
        activityIdArray.add("processTask3");
        
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "   id: '" + processInstance.getId() + "',"
                        + "   processDefinitionId: '" + processInstance.getProcessDefinitionId() + "'"
                        + "} ]"
                        + "}");
        
        requestNode = objectMapper.createObjectNode();
        activityIdArray = requestNode.putArray("activeActivityIds");
        activityIdArray.add("processTask2");
        activityIdArray.add("processTask3");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: []"
                        + "}");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        requestNode = objectMapper.createObjectNode();
        activityIdArray = requestNode.putArray("activeActivityIds");
        activityIdArray.add("processTask2");
        activityIdArray.add("processTask3");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
        .when(Option.IGNORING_EXTRA_FIELDS)
        .isEqualTo("{"
                + "data: [ {"
                + "   id: '" + processInstance.getId() + "',"
                + "   processDefinitionId: '" + processInstance.getProcessDefinitionId() + "'"
                + "} ]"
                + "}");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testQueryProcessInstancesByProcessDefinitionKeys() throws Exception {
        ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode keyArray = requestNode.putArray("processDefinitionKeys");
        keyArray.add("oneTaskProcess");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "data: [ "
                        + " {"
                        + "   id: '" + instance1.getId() + "'"
                        + " }, "
                        + " {"
                        + "   id: '" + instance2.getId() + "'"
                        + " }"
                        + "]"
                        + "}");

        keyArray.removeAll();
        keyArray.add("undefined");

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ ]"
                        + "}");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testQueryProcessInstancesByExcludeProcessDefinitionKeys() throws Exception {
        ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode keyArray = requestNode.putArray("excludeProcessDefinitionKeys");
        keyArray.add("oneTaskProcess");
        
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ ]"
                        + "}");
        
        keyArray.removeAll();
        keyArray.add("undefined");

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "data: [ "
                        + " {"
                        + "   id: '" + instance1.getId() + "'"
                        + " }, "
                        + " {"
                        + "   id: '" + instance2.getId() + "'"
                        + " }"
                        + "]"
                        + "}");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryProcessInstancesByCallbackId() throws Exception {
        ProcessInstance instance1 = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").callbackId("callbackId1").start();
        ProcessInstance instance2 = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").callbackId("callbackId2").start();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));
        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.putArray("callbackIds").add("callbackId1").add("callbackId1");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "data: [ "
                        + " {"
                        + "   id: '" + instance1.getId() + "'"
                        + " }"
                        + "]"
                        + "}");
        requestNode.removeAll();
        requestNode.put("callbackId", "callbackId2");

        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "data: [ "
                        + " {"
                        + "   id: '" + instance2.getId() + "'"
                        + " }"
                        + "]"
                        + "}");

    }
}
