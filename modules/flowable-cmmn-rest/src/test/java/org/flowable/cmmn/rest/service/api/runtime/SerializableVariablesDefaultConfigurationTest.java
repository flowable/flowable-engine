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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.HttpMultipartHelper;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
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
        VariableType serializableType = cmmnEngineConfiguration.getVariableTypes().getVariableType(SerializableType.TYPE_NAME);
        assertThat(serializableType).isInstanceOf(SerializableType.class);
        assertThat(((SerializableType) serializableType).getMode()).isEqualTo(SerializableVariableTypeMode.DISABLED);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
    public void testCreateSerializableVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertSerializedObjectsNotAllowed(createSerializableVariableRequest(
                CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId())));
        assertSerializedObjectsNotAllowed(createSerializableVariableRequest(
                CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId())));

        assertThat(runtimeService.hasVariable(caseInstance.getId(), "serializableVariable")).isFalse();
        assertThat(taskService.hasVariable(task.getId(), "serializableVariable")).isFalse();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
    public void testGetSerializableVariableData() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        PlanItemInstance planItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        // Existing serializable variables, e.g. created before serializable variables were disabled
        withSerializableVariablesEnabled(() -> {
            runtimeService.setVariable(caseInstance.getId(), "serializableVariable", createSerializableVariable());
            runtimeService.setLocalVariable(planItemInstance.getId(), "planItemSerializableVariable", createSerializableVariable());
            taskService.setVariableLocal(task.getId(), "localSerializableVariable", createSerializableVariable());
        });

        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "serializableVariable")));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE_DATA, planItemInstance.getId(),
                "planItemSerializableVariable")));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLE_DATA, task.getId(), "serializableVariable") + "?scope=global"));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLE_DATA, task.getId(), "localSerializableVariable")));

        String variableInstanceId = runtimeService.createVariableInstanceQuery().caseInstanceId(caseInstance.getId())
                .variableName("serializableVariable").excludeVariableInitialization().singleResult().getId();
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_VARIABLE_INSTANCE_DATA, variableInstanceId)));

        String historicVariableInstanceId = historyService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId())
                .variableName("serializableVariable").excludeVariableInitialization().singleResult().getId();
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_VARIABLE_INSTANCE_DATA, historicVariableInstanceId)));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "serializableVariable")));
        assertSerializedObjectsNotAllowed(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE_VARIABLE_DATA, task.getId(), "localSerializableVariable")));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
    public void testGetSerializableVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        withSerializableVariablesEnabled(() -> runtimeService.setVariable(caseInstance.getId(), "serializableVariable", createSerializableVariable()));

        // The variable is returned without its value
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE, caseInstance.getId(), "serializableVariable")),
                HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode.path("name").asString()).isEqualTo("serializableVariable");
        assertThat(responseNode.path("type").asString()).isEqualTo("serializable");
        assertThat(responseNode.path("valueUnresolvable").asBoolean()).isTrue();
        assertThat(responseNode.path("value").isMissingNode() || responseNode.path("value").isNull()).isTrue();
    }
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
    public void testCreateVariableWithUntypedJsonValue() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        // A JSON object or array without a type would have to be stored using Java serialization
        for (String value : List.of("{ \"name\": \"kermit\" }", "[ \"kermit\" ]")) {
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
            httpPost.setEntity(new StringEntity("[ { \"name\": \"untypedVariable\", \"value\": " + value + " } ]"));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode.path("exception").asString())
                    .isEqualTo("Variable 'untypedVariable' has a JSON object or array as value, but no type. Set the type of the variable to 'json'.");
        }
        assertThat(runtimeService.hasVariable(caseInstance.getId(), "untypedVariable")).isFalse();

        // With the json type the value is stored as JSON
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity("[ { \"name\": \"typedVariable\", \"type\": \"json\", \"value\": { \"name\": \"kermit\" } } ]"));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));
        assertThat(runtimeService.getVariableInstance(caseInstance.getId(), "typedVariable").getTypeName()).isEqualTo("json");
    }

    protected void assertSerializedObjectsNotAllowed(HttpUriRequest request) throws IOException {
        // No JSON content type is added, the request can be a multipart request
        CloseableHttpResponse response = internalExecuteRequest(request, HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, false);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
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
        VariableTypes variableTypes = cmmnEngineConfiguration.getVariableTypes();
        VariableType disabledType = variableTypes.getVariableType(SerializableType.TYPE_NAME);
        int typeIndex = variableTypes.getTypeIndex(disabledType);
        SerializableType enabledType = new SerializableType(false, cmmnEngineConfiguration.getVariableLengthVerifier(),
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
