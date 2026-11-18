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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.HttpMultipartHelper;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.impl.types.SerializableType;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/**
 * Tests the default configuration: serializable variables are disabled in the engine and they are not allowed in the REST API.
 *
 * @author Filip Hrisafov
 */
public class SerializableVariablesDefaultConfigurationTest extends BaseSpringRestTestCase {

    @Test
    public void testSerializableVariablesAreDisabledByDefault() {
        VariableType serializableType = processEngineConfiguration.getVariableTypes().getVariableType(SerializableType.TYPE_NAME);
        assertThat(serializableType).isInstanceOf(SerializableType.class);
        assertThat(((SerializableType) serializableType).getMode()).isEqualTo(SerializableVariableTypeMode.DISABLED);
    }

    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml")
    public void testCreateSerializableVariable() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertSerializedObjectsNotAllowed(createSerializableVariableRequest(
                RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId())));
        assertSerializedObjectsNotAllowed(createSerializableVariableRequest(
                RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_COLLECTION, processInstance.getId())));
        assertSerializedObjectsNotAllowed(createSerializableVariableRequest(
                RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId())));

        assertThat(runtimeService.hasVariable(processInstance.getId(), "serializableVariable")).isFalse();
        assertThat(taskService.hasVariable(task.getId(), "serializableVariable")).isFalse();
    }

    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml")
    public void testGetSerializableVariableData() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        // Existing serializable variables, e.g. created before serializable variables were disabled
        withSerializableVariablesEnabled(() -> {
            runtimeService.setVariable(processInstance.getId(), "serializableVariable", createSerializableVariable());
            taskService.setVariableLocal(task.getId(), "localSerializableVariable", createSerializableVariable());
        });

        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "serializableVariable")));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, processInstance.getId(), "serializableVariable")));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLE_DATA, task.getId(), "serializableVariable") + "?scope=global"));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLE_DATA, task.getId(), "localSerializableVariable")));

        String variableInstanceId = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId())
                .variableName("serializableVariable").excludeVariableInitialization().singleResult().getId();
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_VARIABLE_INSTANCE_DATA, variableInstanceId)));

        String historicVariableInstanceId = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId())
                .variableName("serializableVariable").excludeVariableInitialization().singleResult().getId();
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_VARIABLE_INSTANCE_DATA, historicVariableInstanceId)));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "serializableVariable")));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, task.getId(), "serializableVariable")));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, task.getId(), "localSerializableVariable")));

        String historicDetailId = historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId())
                .variableUpdates().excludeTaskDetails().singleResult().getId();
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_DETAIL_VARIABLE_DATA, historicDetailId)));
    }

    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml")
    public void testGetSerializableVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        withSerializableVariablesEnabled(() -> runtimeService.setVariable(processInstance.getId(), "serializableVariable", createSerializableVariable()));

        // The variable is returned without its value
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX
                + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "serializableVariable")),
                HttpStatus.SC_OK);
        JsonNode responseNode = readContent(response);
        closeResponse(response);
        assertThat(responseNode.path("name").asString()).isEqualTo("serializableVariable");
        assertThat(responseNode.path("type").asString()).isEqualTo("serializable");
        assertThat(responseNode.path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(responseNode.path("value").isMissingNode() || responseNode.path("value").isNull()).isTrue();
    }

    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml")
    public void testCreateVariableWithUntypedJsonValue() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // A JSON object or array without a type would have to be stored using Java serialization
        for (String value : List.of("{ \"name\": \"kermit\" }", "[ \"kermit\" ]")) {
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
            httpPost.setEntity(new StringEntity("[ { \"name\": \"untypedVariable\", \"value\": " + value + " } ]"));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode.path("exception").asString())
                    .isEqualTo("Variable 'untypedVariable' has a JSON object or array as value, but no type. Set the type of the variable to 'json'.");
        }
        assertThat(runtimeService.hasVariable(processInstance.getId(), "untypedVariable")).isFalse();

        // With the json type the value is stored as JSON
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity("[ { \"name\": \"typedVariable\", \"type\": \"json\", \"value\": { \"name\": \"kermit\" } } ]"));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));
        assertThat(runtimeService.getVariableInstance(processInstance.getId(), "typedVariable").getTypeName()).isEqualTo("json");
    }

    protected void assertSerializedObjectsNotAllowed(HttpUriRequest request) {
        // No JSON content type is added, the request can be a multipart request
        CloseableHttpResponse response = internalExecuteRequest(request, HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, false);
        JsonNode responseNode = readContent(response);
        closeResponse(response);
        assertThat(responseNode.path("exception").asString()).isEqualTo("Serialized objects are not allowed");
    }

    protected HttpPost createSerializableVariableRequest(String url) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(buffer)) {
            output.writeObject(createSerializableVariable());
        }

        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "serializableVariable");
        additionalFields.put("type", "serializable");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object",
                new ByteArrayInputStream(buffer.toByteArray()), additionalFields));
        return httpPost;
    }

    protected TestSerializableVariable createSerializableVariable() {
        TestSerializableVariable serializable = new TestSerializableVariable();
        serializable.setSomeField("some value");
        return serializable;
    }

    /**
     * Temporarily enables the serializable variable type in the engine, in order to create serializable variables.
     */
    protected void withSerializableVariablesEnabled(Runnable runnable) {
        VariableTypes variableTypes = processEngineConfiguration.getVariableTypes();
        VariableType disabledType = variableTypes.getVariableType(SerializableType.TYPE_NAME);
        int typeIndex = variableTypes.getTypeIndex(disabledType);
        SerializableType enabledType = new SerializableType(false, processEngineConfiguration.getVariableLengthVerifier(),
                SerializableVariableTypeMode.ENABLED);
        variableTypes.removeType(disabledType);
        variableTypes.addType(enabledType, typeIndex);
        try {
            runnable.run();
        } finally {
            variableTypes.removeType(enabledType);
            variableTypes.addType(disabledType, typeIndex);
        }
    }
}
