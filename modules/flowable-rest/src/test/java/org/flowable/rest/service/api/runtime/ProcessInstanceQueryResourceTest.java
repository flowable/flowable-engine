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

package org.flowable.rest.service.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.common.engine.impl.identity.Authentication;
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
 * Test for all REST-operations related to the process instance query resource.
 *
 * @author Frederik Heremans
 */
public class ProcessInstanceQueryResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying process instance based on variables. POST query/process-instances
     */
    @Test
    @Deployment
    public void testQueryProcessInstancesWithVariables() throws Exception {
        HashMap<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringVar", "Azerty");
        processVariables.put("intVar", 67890);
        processVariables.put("booleanVar", false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);

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
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azeRTY");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String not equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "HIJKLm");
        variableNode.put("operation", "notEqualsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String not like
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azer%");
        variableNode.put("operation", "like");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String not like Ignore Case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "AzEr%");
        variableNode.put("operation", "likeIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String equals without value
        variableNode.removeAll();
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processInstance.getId());

        // String equals with non existing value
        variableNode.removeAll();
        variableNode.put("value", "Azerty2");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode);
    }

    /**
     * Test querying process instance based on variables. POST query/process-instances
     */
    @Test
    @Deployment
    public void testQueryProcessInstancesPagingAndSorting() throws Exception {
        Authentication.setAuthenticatedUserId("queryAndSortingTestUser");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("aOneTaskProcess");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("bOneTaskProcess");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("cOneTaskProcess");

        // Create request node
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("order", "desc");
        requestNode.put("sort", "processDefinitionKey");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check order
        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "         id: '" + processInstance3.getId() + "'"
                        + "      }, {"
                        + "         id: '" + processInstance2.getId() + "'"
                        + "      }, {"
                        + "         id: '" + processInstance1.getId() + "'"
                        + "      } ]"
                        + "}");

        // Check paging size
        requestNode = objectMapper.createObjectNode();
        requestNode.put("start", 0);
        requestNode.put("size", 1);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "         id: '" + processInstance1.getId() + "'"
                        + "      } ]"
                        + "}");

        // Check paging start and size
        requestNode = objectMapper.createObjectNode();
        requestNode.put("start", 1);
        requestNode.put("size", 1);
        requestNode.put("order", "desc");
        requestNode.put("sort", "processDefinitionKey");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "        id: '" + processInstance2.getId() + "',"
                        + "        processDefinitionName: 'The One Task Process',"
                        + "        processDefinitionDescription: 'One task process description',"
                        + "        startUserId: '" + processInstance2.getStartUserId() + "',"
                        + "        startTime: '${json-unit.any-string}'"
                        + "      } ]"
                        + "}");

    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testQueryProcessInstancesByActiveActivityId() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("activeActivityId", "processTask");
        
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
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
        
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
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
    public void testQueryProcessInstancesByBusinessKey() throws Exception {
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey3");
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey1");
        runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey2");

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("order", "asc");
        requestNode.put("sort", "businessKey");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ "
                        + " {"
                        + "   businessKey: 'businessKey1'"
                        + " }, "
                        + " {"
                        + "   businessKey: 'businessKey2'"
                        + " }, "
                        + " {"
                        + "   businessKey: 'businessKey3'"
                        + " } "
                        + "]"
                        + "}");

        requestNode.put("order", "desc");

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
        httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);

        rootNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ "
                        + " {"
                        + "   businessKey: 'businessKey3'"
                        + " }, "
                        + " {"
                        + "   businessKey: 'businessKey2'"
                        + " }, "
                        + " {"
                        + "   businessKey: 'businessKey1'"
                        + " } "
                        + "]"
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

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
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

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
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
    public void testQueryProcessInstancesByCallbackId() throws Exception {
        ProcessInstance instance1 = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").callbackId("callbackId1").start();
        ProcessInstance instance2 = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").callbackId("callbackId2").start();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.putArray("callbackIds").add("callbackId1").add("callbackId1");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
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

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testQueryProcessInstancesByExcludeProcessDefinitionKeys() throws Exception {
        ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode keyArray = requestNode.putArray("excludeProcessDefinitionKeys");
        keyArray.add("oneTaskProcess");
        
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
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

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_QUERY);
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
}
