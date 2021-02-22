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
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.HttpMultipartHelper;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single execution variable.
 *
 * @author Frederik Heremans
 */
public class ExecutionVariableResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting an execution variable. GET
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testGetExecutionVariable() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne");
        runtimeService.setVariable(processInstance.getId(), "variable", "processValue");

        Execution childExecution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertThat(childExecution).isNotNull();
        runtimeService.setVariableLocal(childExecution.getId(), "variable", "childValue");

        // Get local scope variable
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, childExecution.getId(), "variable")),
                HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "   name: 'variable',"
                        + "   type: 'string',"
                        + "   value: 'childValue',"
                        + "   scope: 'local'"
                        + "}");

        // Get global scope variable
        response =

                executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE,
                        childExecution.getId(), "variable") + "?scope=global"),
                        HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().

                getContent());

        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "   name: 'variable',"
                        + "   type: 'string',"
                        + "   value: 'processValue',"
                        + "   scope: 'global'"
                        + "}");

        // Illegal scope
        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE,
                processInstance.getId(), "variable") + "?scope=illegal"), HttpStatus.SC_BAD_REQUEST);
        closeResponse(response);

        // Unexisting process
        response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, "unexisting", "variable")),
                HttpStatus.SC_NOT_FOUND);
        closeResponse(response);

        // Unexisting variable
        response = executeRequest(new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, processInstance.getId(), "unexistingVariable")),
                HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test getting execution variable data.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testGetExecutionVariableData() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne");
        runtimeService.setVariableLocal(processInstance.getId(), "var", "This is a binary piece of text".getBytes());

        Execution childExecution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertThat(childExecution).isNotNull();
        runtimeService.setVariableLocal(childExecution.getId(), "var", "This is a binary piece of text in the child execution".getBytes());

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, childExecution.getId(), "var")),
                HttpStatus.SC_OK);
        String actualResponseBytesAsText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(actualResponseBytesAsText).isEqualTo("This is a binary piece of text in the child execution");
        assertThat(response.getEntity().getContentType().getValue()).isEqualTo("application/octet-stream");

        // Test global scope
        response = executeRequest(new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, childExecution.getId(), "var") + "?scope=global"),
                HttpStatus.SC_OK);
        actualResponseBytesAsText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(actualResponseBytesAsText).isEqualTo("This is a binary piece of text");
        assertThat(response.getEntity().getContentType().getValue()).isEqualTo("application/octet-stream");
    }

    /**
     * Test getting an execution variable data.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testGetExecutionVariableDataSerializable() throws Exception {

        TestSerializableVariable originalSerializable = new TestSerializableVariable();
        originalSerializable.setSomeField("This is some field");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne");
        runtimeService.setVariableLocal(processInstance.getId(), "var", originalSerializable);

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, processInstance.getId(), "var")),
                HttpStatus.SC_OK);

        // Read the serializable from the stream
        ObjectInputStream stream = new ObjectInputStream(response.getEntity().getContent());
        Object readSerializable = stream.readObject();

        closeResponse(response);

        assertThat(readSerializable).isInstanceOf(TestSerializableVariable.class);
        assertThat(((TestSerializableVariable) readSerializable).getSomeField()).isEqualTo("This is some field");
        assertThat(response.getEntity().getContentType().getValue()).isEqualTo("application/x-java-serialized-object");
    }

    /**
     * Test getting an execution variable, for illegal vars.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testGetExecutionDataForIllegalVariables() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne");
        runtimeService.setVariableLocal(processInstance.getId(), "localTaskVariable", "this is a plain string variable");

        // Try getting data for non-binary variable
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, processInstance.getId(), "localTaskVariable")), HttpStatus.SC_NOT_FOUND);
        closeResponse(response);

        // Try getting data for unexisting property
        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, processInstance.getId(), "unexistingVariable")),
                HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test deleting a single execution variable, including "not found" check.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testDeleteExecutionVariable() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("processOne", Collections.singletonMap("myVariable", (Object) "processValue"));

        Execution childExecution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertThat(childExecution).isNotNull();
        runtimeService.setVariableLocal(childExecution.getId(), "myVariable", "childValue");

        // Delete variable local
        HttpDelete httpDelete = new HttpDelete(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, childExecution.getId(), "myVariable"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        assertThat(runtimeService.hasVariableLocal(childExecution.getId(), "myVariable")).isFalse();
        // Global variable should remain unaffected
        assertThat(runtimeService.hasVariable(childExecution.getId(), "myVariable")).isTrue();

        // Delete variable global
        httpDelete = new HttpDelete(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, childExecution.getId(), "myVariable")
                        + "?scope=global");
        response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        assertThat(runtimeService.hasVariableLocal(childExecution.getId(), "myVariable")).isFalse();
        assertThat(runtimeService.hasVariable(childExecution.getId(), "myVariable")).isFalse();

        // Run the same delete again, variable is not there so 404 should be
        // returned
        response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test updating a single execution variable, including "not found" check.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testUpdateExecutionVariable() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", Collections.singletonMap("overlappingVariable", (Object) "processValue"));
        runtimeService.setVariableLocal(processInstance.getId(), "myVar", "processValue");

        Execution childExecution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertThat(childExecution).isNotNull();
        runtimeService.setVariableLocal(childExecution.getId(), "myVar", "childValue");

        // Update variable local
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "myVar");
        requestNode.put("value", "updatedValue");
        requestNode.put("type", "string");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, childExecution.getId(), "myVar"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   value: 'updatedValue',"
                        + "   scope: 'local'"
                        + "}");

        // Global value should be unaffected
        assertThat(runtimeService.getVariable(processInstance.getId(), "myVar")).isEqualTo("processValue");
        assertThat(runtimeService.getVariableLocal(childExecution.getId(), "myVar")).isEqualTo("updatedValue");

        // Update variable global
        requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "myVar");
        requestNode.put("value", "updatedValueGlobal");
        requestNode.put("type", "string");
        requestNode.put("scope", "global");

        httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, childExecution.getId(), "myVar"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPut, HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   value: 'updatedValueGlobal',"
                        + "   scope: 'global'"
                        + "}");

        // Local value should be unaffected
        assertThat(runtimeService.getVariable(processInstance.getId(), "myVar")).isEqualTo("updatedValueGlobal");
        assertThat(runtimeService.getVariableLocal(childExecution.getId(), "myVar")).isEqualTo("updatedValue");

        requestNode.put("name", "unexistingVariable");

        httpPut.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST);
        closeResponse(response);

        httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, childExecution.getId(), "unexistingVariable"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPut, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test updating a single execution variable using a binary stream.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testUpdateBinaryExecutionVariable() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", Collections.singletonMap("overlappingVariable", (Object) "processValue"));
        runtimeService.setVariableLocal(processInstance.getId(), "binaryVariable", "Initial binary value".getBytes());

        Execution childExecution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertThat(childExecution).isNotNull();
        runtimeService.setVariableLocal(childExecution.getId(), "binaryVariable", "Initial binary value child".getBytes());

        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

        // Add name and type
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "binaryVariable");
        additionalFields.put("type", "binary");

        HttpPut httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, childExecution.getId(), "binaryVariable"));
        httpPut.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPut, HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "   name: 'binaryVariable',"
                        + "   type: 'binary',"
                        + "   value: null,"
                        + "   scope: 'local',"
                        + "   valueUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, childExecution.getId(), "binaryVariable") + "'"
                        + "}");

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariableLocal(childExecution.getId(), "binaryVariable");
        assertThat(variableValue).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content");

        // Update variable in global scope
        additionalFields.put("scope", "global");
        binaryContent = new ByteArrayInputStream("This is binary content global".getBytes());

        httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE, childExecution.getId(), "binaryVariable"));
        httpPut.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        response = executeBinaryRequest(httpPut, HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "   name: 'binaryVariable',"
                        + "   type: 'binary',"
                        + "   value: null,"
                        + "   scope: 'global',"
                        + "   valueUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_EXECUTION_VARIABLE_DATA, childExecution.getId(), "binaryVariable") + "'"
                        + "}");

        // Check actual global value of variable in engine
        variableValue = runtimeService.getVariableLocal(processInstance.getId(), "binaryVariable");
        assertThat(variableValue).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content global");

        // local value should remain unchanged
        variableValue = runtimeService.getVariableLocal(childExecution.getId(), "binaryVariable");
        assertThat(variableValue).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content");
    }
}
