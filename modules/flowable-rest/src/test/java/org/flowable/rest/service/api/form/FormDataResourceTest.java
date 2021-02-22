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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

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

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA) + "?taskId=" + task.getId()), HttpStatus.SC_OK);

        // Check resulting task
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "formProperties: [ {"
                        + "  id: 'room',"
                        + "  name: null,"
                        + "  type: null,"
                        + "  value: null,"
                        + "  readable: true,"
                        + "  writable: true,"
                        + "  required: false"
                        + "},"
                        + "{"
                        + "  id: 'duration',"
                        + "  name: null,"
                        + "  type: 'long',"
                        + "  value: null,"
                        + "  readable: true,"
                        + "  writable: true,"
                        + "  required: false"
                        + "},"
                        + "{"
                        + "  id: 'speaker',"
                        + "  name: null,"
                        + "  type: null,"
                        + "  value: 'John Doe',"
                        + "  readable: true,"
                        + "  writable: false,"
                        + "  required: false"
                        + "},"
                        + "{"
                        + "  id: 'street',"
                        + "  name: null,"
                        + "  type: null,"
                        + "  value: null,"
                        + "  readable: true,"
                        + "  writable: true,"
                        + "  required: true"
                        + "},"
                        + "{"
                        + "  id: 'start',"
                        + "  name: null,"
                        + "  type: 'date',"
                        + "  value: null,"
                        + "  readable: true,"
                        + "  writable: true,"
                        + "  required: false,"
                        + "  datePattern: 'dd-MMM-yyyy'"
                        + "},"
                        + "{"
                        + "  id: 'end',"
                        + "  name: 'End',"
                        + "  type: 'date',"
                        + "  value: null,"
                        + "  readable: true,"
                        + "  writable: true,"
                        + "  required: false,"
                        + "  datePattern: 'dd/MM/yyyy'"
                        + "},"
                        + "{"
                        + "id: 'direction',"
                        + "  name: null,"
                        + "  type: 'enum',"
                        + "  value: null,"
                        + "  readable: true,"
                        + "  writable: true,"
                        + "  required: false,"
                        + "  datePattern: null,"
                        + "  enumValues: [ {"
                        + "    id: 'left',"
                        + "    name: 'Go Left'"
                        + "  },"
                        + "  {"
                        + "    id: 'right',"
                        + "    name: 'Go Right'"
                        + "  },"
                        + "  {"
                        + "    id: 'up',"
                        + "    name: 'Go Up'"
                        + "  },"
                        + "  {"
                        + "    id: 'down',"
                        + "    name: 'Go Down'"
                        + "   } ]"
                        + "} ]"
                        + "}"
                );

        response = executeRequest(new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA) + "?processDefinitionId=" + processInstance
                                .getProcessDefinitionId()),
                HttpStatus.SC_OK);

        // Check resulting task
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "formProperties: [ {"
                        + "  id: 'number',"
                        + "  name: 'Number',"
                        + "  type: 'long',"
                        + "  value: null,"
                        + "  readable: true,"
                        + "  writable: true,"
                        + "  required: false"
                        + "},"
                        + "{"
                        + "  id: 'description',"
                        + "  name: 'Description',"
                        + "  type: null,"
                        + "  value: null,"
                        + "  readable: true,"
                        + "  writable: true,"
                        + "  required: false"
                        + "} ]"
                        + "}"
                );

        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA) + "?processDefinitionId=123"),
                HttpStatus.SC_NOT_FOUND));

        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_FORM_DATA) + "?processDefinitionId2=123"),
                HttpStatus.SC_BAD_REQUEST));
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
        assertThat(task).isNull();
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(processInstance).isNull();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list();
        Map<String, HistoricVariableInstance> historyMap = new HashMap<>();
        for (HistoricVariableInstance historicVariableInstance : variables) {
            historyMap.put(historicVariableInstance.getVariableName(), historicVariableInstance);
        }

        assertThat(historyMap.get("room").getValue()).isEqualTo("123");
        assertThat(historyMap.get("room").getProcessInstanceId()).isEqualTo(processInstanceId);

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
        assertThat(task).isNull();
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(processInstance).isNull();
        variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list();
        historyMap.clear();
        for (HistoricVariableInstance historicVariableInstance : variables) {
            historyMap.put(historicVariableInstance.getVariableName(), historicVariableInstance);
        }

        assertThat(historyMap.get("room").getValue()).isEqualTo("123");
        assertThat(historyMap.get("room").getProcessInstanceId()).isEqualTo(processInstanceId);
        assertThat(historyMap.get("direction").getValue()).isEqualTo("up");

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
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '${json-unit.any-string}',"
                        + "processDefinitionId: '" + processDefinitionId + "'"
                        + "}");
        task = taskService.createTaskQuery().processInstanceId(responseNode.get("id").asText()).singleResult();
        assertThat(task).isNotNull();
    }
}
