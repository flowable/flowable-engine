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

package org.flowable.rest.service.api.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for all REST-operations related to a Form data resource.
 * 
 * @author Tijs Rademakers
 */
public class FormDataResourceTest extends BaseSpringRestTestCase {

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Deployment
    public void testGetFormData() throws Exception {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("SpeakerName", "John Doe");
        Address address = new Address();
        variableMap.put("address", address);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variableMap);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA) + "?taskId=" + task.getId()), HttpStatus.SC_OK);

        // Check resulting task
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals(7, responseNode.get("formProperties").size());
        Map<String, JsonNode> mappedProperties = new HashMap<>();
        for (JsonNode propNode : responseNode.get("formProperties")) {
            mappedProperties.put(propNode.get("id").asText(), propNode);
        }
        JsonNode propNode = mappedProperties.get("room");
        assertNotNull(propNode);
        assertEquals("room", propNode.get("id").asText());
        assertTrue(propNode.get("name").isNull());
        assertTrue(propNode.get("type").isNull());
        assertTrue(propNode.get("value").isNull());
        assertTrue(propNode.get("readable").asBoolean());
        assertTrue(propNode.get("writable").asBoolean());
        assertFalse(propNode.get("required").asBoolean());

        propNode = mappedProperties.get("duration");
        assertNotNull(propNode);
        assertEquals("duration", propNode.get("id").asText());
        assertTrue(propNode.get("name").isNull());
        assertEquals("long", propNode.get("type").asText());
        assertTrue(propNode.get("value").isNull());
        assertTrue(propNode.get("readable").asBoolean());
        assertTrue(propNode.get("writable").asBoolean());
        assertFalse(propNode.get("required").asBoolean());

        propNode = mappedProperties.get("speaker");
        assertNotNull(propNode);
        assertEquals("speaker", propNode.get("id").asText());
        assertTrue(propNode.get("name").isNull());
        assertTrue(propNode.get("type").isNull());
        assertEquals("John Doe", propNode.get("value").asText());
        assertTrue(propNode.get("readable").asBoolean());
        assertFalse(propNode.get("writable").asBoolean());
        assertFalse(propNode.get("required").asBoolean());

        propNode = mappedProperties.get("street");
        assertNotNull(propNode);
        assertEquals("street", propNode.get("id").asText());
        assertTrue(propNode.get("name").isNull());
        assertTrue(propNode.get("type").isNull());
        assertTrue(propNode.get("value").isNull());
        assertTrue(propNode.get("readable").asBoolean());
        assertTrue(propNode.get("writable").asBoolean());
        assertTrue(propNode.get("required").asBoolean());

        propNode = mappedProperties.get("start");
        assertNotNull(propNode);
        assertEquals("start", propNode.get("id").asText());
        assertTrue(propNode.get("name").isNull());
        assertEquals("date", propNode.get("type").asText());
        assertTrue(propNode.get("value").isNull());
        assertEquals("dd-MMM-yyyy", propNode.get("datePattern").asText());
        assertTrue(propNode.get("readable").asBoolean());
        assertTrue(propNode.get("writable").asBoolean());
        assertFalse(propNode.get("required").asBoolean());

        propNode = mappedProperties.get("end");
        assertNotNull(propNode);
        assertEquals("end", propNode.get("id").asText());
        assertEquals("End", propNode.get("name").asText());
        assertEquals("date", propNode.get("type").asText());
        assertTrue(propNode.get("value").isNull());
        assertEquals("dd/MM/yyyy", propNode.get("datePattern").asText());
        assertTrue(propNode.get("readable").asBoolean());
        assertTrue(propNode.get("writable").asBoolean());
        assertFalse(propNode.get("required").asBoolean());

        propNode = mappedProperties.get("direction");
        assertNotNull(propNode);
        assertEquals("direction", propNode.get("id").asText());
        assertTrue(propNode.get("name").isNull());
        assertEquals("enum", propNode.get("type").asText());
        assertTrue(propNode.get("value").isNull());
        assertTrue(propNode.get("datePattern").isNull());
        assertTrue(propNode.get("readable").asBoolean());
        assertTrue(propNode.get("writable").asBoolean());
        assertFalse(propNode.get("required").asBoolean());
        JsonNode enumValues = propNode.get("enumValues");
        assertEquals(4, enumValues.size());
        Map<String, String> mappedEnums = new HashMap<>();
        for (JsonNode enumNode : enumValues) {
            mappedEnums.put(enumNode.get("id").asText(), enumNode.get("name").asText());
        }
        assertEquals("Go Left", mappedEnums.get("left"));
        assertEquals("Go Right", mappedEnums.get("right"));
        assertEquals("Go Up", mappedEnums.get("up"));
        assertEquals("Go Down", mappedEnums.get("down"));

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA) + "?processDefinitionId=" + processInstance.getProcessDefinitionId()),
                HttpStatus.SC_OK);

        // Check resulting task
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals(2, responseNode.get("formProperties").size());
        mappedProperties.clear();
        for (JsonNode propertyNode : responseNode.get("formProperties")) {
            mappedProperties.put(propertyNode.get("id").asText(), propertyNode);
        }

        propNode = mappedProperties.get("number");
        assertNotNull(propNode);
        assertEquals("number", propNode.get("id").asText());
        assertEquals("Number", propNode.get("name").asText());
        assertEquals("long", propNode.get("type").asText());
        assertTrue(propNode.get("value").isNull());
        assertTrue(propNode.get("readable").asBoolean());
        assertTrue(propNode.get("writable").asBoolean());
        assertFalse(propNode.get("required").asBoolean());

        propNode = mappedProperties.get("description");
        assertNotNull(propNode);
        assertEquals("description", propNode.get("id").asText());
        assertEquals("Description", propNode.get("name").asText());
        assertTrue(propNode.get("type").isNull());
        assertTrue(propNode.get("value").isNull());
        assertTrue(propNode.get("readable").asBoolean());
        assertTrue(propNode.get("writable").asBoolean());
        assertFalse(propNode.get("required").asBoolean());

        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA) + "?processDefinitionId=123"), HttpStatus.SC_NOT_FOUND));

        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA) + "?processDefinitionId2=123"), HttpStatus.SC_BAD_REQUEST));
    }

    @Test
    @Deployment
    public void testSubmitFormData() throws Exception {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("SpeakerName", "John Doe");
        Address address = new Address();
        variableMap.put("address", address);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variableMap);
        String processInstanceId = processInstance.getId();
        String processDefinitionId = processInstance.getProcessDefinitionId();
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("taskId", task.getId());
        ArrayNode propertyArray = objectMapper.createArrayNode();
        requestNode.set("properties", propertyArray);
        ObjectNode propNode = objectMapper.createObjectNode();
        propNode.put("id", "room");
        propNode.put("value", 123L);
        propertyArray.add(propNode);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_INTERNAL_SERVER_ERROR));

        propNode = objectMapper.createObjectNode();
        propNode.put("id", "street");
        propNode.put("value", "test");
        propertyArray.add(propNode);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

        task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        assertNull(task);
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        assertNull(processInstance);
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list();
        Map<String, HistoricVariableInstance> historyMap = new HashMap<>();
        for (HistoricVariableInstance historicVariableInstance : variables) {
            historyMap.put(historicVariableInstance.getVariableName(), historicVariableInstance);
        }

        assertEquals("123", historyMap.get("room").getValue());
        assertEquals(processInstanceId, historyMap.get("room").getProcessInstanceId());

        processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variableMap);
        processInstanceId = processInstance.getId();
        task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();

        requestNode.put("taskId", task.getId());
        propNode = objectMapper.createObjectNode();
        propNode.put("id", "direction");
        propNode.put("value", "nowhere");
        propertyArray.add(propNode);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        propNode.put("value", "up");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

        task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        assertNull(task);
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        assertNull(processInstance);
        variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list();
        historyMap.clear();
        for (HistoricVariableInstance historicVariableInstance : variables) {
            historyMap.put(historicVariableInstance.getVariableName(), historicVariableInstance);
        }

        assertEquals("123", historyMap.get("room").getValue());
        assertEquals(processInstanceId, historyMap.get("room").getProcessInstanceId());
        assertEquals("up", historyMap.get("direction").getValue());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("processDefinitionId", processDefinitionId);
        propertyArray = objectMapper.createArrayNode();
        requestNode.set("properties", propertyArray);
        propNode = objectMapper.createObjectNode();
        propNode.put("id", "number");
        propNode.put("value", 123);
        propertyArray.add(propNode);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode.get("id").asText());
        assertEquals(processDefinitionId, responseNode.get("processDefinitionId").asText());
        task = taskService.createTaskQuery().processInstanceId(responseNode.get("id").asText()).singleResult();
        assertNotNull(task);
    }
}
