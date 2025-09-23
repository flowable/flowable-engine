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
import static org.assertj.core.api.Assertions.entry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.HttpMultipartHelper;
import org.flowable.rest.service.api.RestUrls;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to Process instance variables.
 * 
 * @author Frederik Heremans
 */
public class ProcessInstanceVariablesCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all process variables. GET runtime/process-instances/{processInstanceId}/variables
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testGetProcessVariables() throws Exception {

        Calendar cal = Calendar.getInstance();

        // Start process with all types of variables
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringProcVar", "This is a ProcVariable");
        processVariables.put("intProcVar", 123);
        processVariables.put("longProcVar", 1234L);
        processVariables.put("shortProcVar", (short) 123);
        processVariables.put("doubleProcVar", 99.99);
        processVariables.put("booleanProcVar", Boolean.TRUE);
        processVariables.put("dateProcVar", cal.getTime());
        processVariables.put("byteArrayProcVar", "Some raw bytes".getBytes());
        processVariables.put("overlappingVariable", "process-value");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);

        // Request all variables (no scope provides) which include global an local
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId())),
                HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(9);
    }

    /**
     * Test creating a single process variable. POST runtime/process-instance/{processInstanceId}/variables
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateSingleProcessInstanceVariable() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "myVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        // Create a new local variable
        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent()).get(0);
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "name: 'myVariable',"
                        + "value: 'simple string value',"
                        + "scope: 'local',"
                        + "type: 'string'"
                        + "}");

        assertThat(runtimeService.hasVariableLocal(processInstance.getId(), "myVariable")).isTrue();
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "myVariable")).isEqualTo("simple string value");
    }

    /**
     * Test creating a single process variable using a binary stream. POST runtime/process-instances/{processInstanceId}/variables
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateSingleBinaryProcessVariable() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

        // Add name, type and scope
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "binaryVariable");
        additionalFields.put("type", "binary");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "name: 'binaryVariable',"
                        + "value: null,"
                        + "scope: 'local',"
                        + "type: 'binary',"
                        + "valueUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "binaryVariable") + "'"
                        + "}");

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariableLocal(processInstance.getId(), "binaryVariable");
        assertThat(variableValue).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content");
    }

    /**
     * Test creating a single process variable using a binary stream containing a serializable. POST runtime/process-instances/{processInstanceId}/variables
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateSingleSerializableProcessVariable() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        TestSerializableVariable serializable = new TestSerializableVariable();
        serializable.setSomeField("some value");

        // Serialize object to readable stream for representation
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        output.writeObject(serializable);
        output.close();

        InputStream binaryContent = new ByteArrayInputStream(buffer.toByteArray());

        // Add name, type and scope
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "serializableVariable");
        additionalFields.put("type", "serializable");

        // Upload a valid BPMN-file using multipart-data
        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "name: 'serializableVariable',"
                        + "value: null,"
                        + "scope: 'local',"
                        + "type: 'serializable',"
                        + "valueUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "serializableVariable") + "'"
                        + "}");

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariableLocal(processInstance.getId(), "serializableVariable");
        assertThat(variableValue).isInstanceOf(TestSerializableVariable.class);
        assertThat(((TestSerializableVariable) variableValue).getSomeField()).isEqualTo("some value");
    }

    /**
     * Test creating a single process variable, testing edge case exceptions. POST runtime/process-instances/{processInstanceId}/variables
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateSingleProcessVariableEdgeCases() throws Exception {
        // Test adding variable to unexisting execution
        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "existingVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, "unexisting"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

        // Test trying to create already existing variable
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariable(processInstance.getId(), "existingVariable", "I already exist");

        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CONFLICT));

        // Test creating nameless variable
        variableNode.removeAll();
        variableNode.put("value", "simple string value");

        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test passing in empty array
        requestNode.removeAll();
        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test passing in object instead of array
        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(objectMapper.createObjectNode().toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test creating a single process variable, testing default types when omitted. POST runtime/process-instances/{processInstanceId}/variables
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateSingleProcessVariableDefaultTypes() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // String type detection
        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode varNode = requestNode.addObject();
        varNode.put("name", "stringVar");
        varNode.put("value", "String value");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertThat(runtimeService.getVariable(processInstance.getId(), "stringVar")).isEqualTo("String value");

        // Integer type detection
        varNode.put("name", "integerVar");
        varNode.put("value", 123);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertThat(runtimeService.getVariable(processInstance.getId(), "integerVar")).isEqualTo(123);

        // Double type detection
        varNode.put("name", "doubleVar");
        varNode.put("value", 123.456);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertThat(runtimeService.getVariable(processInstance.getId(), "doubleVar")).isEqualTo(123.456);

        // Boolean type detection
        varNode.put("name", "booleanVar");
        varNode.put("value", Boolean.TRUE);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertThat(runtimeService.getVariable(processInstance.getId(), "booleanVar")).isEqualTo(Boolean.TRUE);
    }

    /**
     * Test creating multiple process variables in a single call. POST runtime/process-instance/{processInstanceId}/variables
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateMultipleProcessVariables() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        ArrayNode requestNode = objectMapper.createArrayNode();

        // String variable
        ObjectNode stringVarNode = requestNode.addObject();
        stringVarNode.put("name", "stringVariable");
        stringVarNode.put("value", "simple string value");
        stringVarNode.put("type", "string");

        // Integer
        ObjectNode integerVarNode = requestNode.addObject();
        integerVarNode.put("name", "integerVariable");
        integerVarNode.put("value", 1234);
        integerVarNode.put("type", "integer");

        // Short
        ObjectNode shortVarNode = requestNode.addObject();
        shortVarNode.put("name", "shortVariable");
        shortVarNode.put("value", 123);
        shortVarNode.put("type", "short");

        // Long
        ObjectNode longVarNode = requestNode.addObject();
        longVarNode.put("name", "longVariable");
        longVarNode.put("value", 4567890L);
        longVarNode.put("type", "long");

        // Double
        ObjectNode doubleVarNode = requestNode.addObject();
        doubleVarNode.put("name", "doubleVariable");
        doubleVarNode.put("value", 123.456);
        doubleVarNode.put("type", "double");

        // Boolean
        ObjectNode booleanVarNode = requestNode.addObject();
        booleanVarNode.put("name", "booleanVariable");
        booleanVarNode.put("value", Boolean.TRUE);
        booleanVarNode.put("type", "boolean");

        // Date
        Calendar varCal = Calendar.getInstance();
        String isoString = getISODateString(varCal.getTime());

        ObjectNode dateVarNode = requestNode.addObject();
        dateVarNode.put("name", "dateVariable");
        dateVarNode.put("value", isoString);
        dateVarNode.put("type", "date");

        // Create local variables with a single request
        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(7);

        // Check if engine has correct variables set
        Map<String, Object> variables = runtimeService.getVariablesLocal(processInstance.getId());
        assertThat(variables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("integerVariable", 1234),
                        entry("shortVariable", (short) 123),
                        entry("longVariable", 4567890L),
                        entry("doubleVariable", 123.456),
                        entry("booleanVariable", true),
                        entry("dateVariable", getDateFromISOString(isoString))
                );
    }

    /**
     * Test creating multiple process variables in a single call. POST runtime/process-instance/{processInstanceId}/variables?override=true
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateMultipleProcessVariablesWithOverride() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariable(processInstance.getId(), "stringVariable", "initialValue");
        ArrayNode requestNode = objectMapper.createArrayNode();

        // String variable
        ObjectNode stringVarNode = requestNode.addObject();
        stringVarNode.put("name", "stringVariable");
        stringVarNode.put("value", "simple string value");
        stringVarNode.put("type", "string");

        ObjectNode anotherVariable = requestNode.addObject();
        anotherVariable.put("name", "stringVariable2");
        anotherVariable.put("value", "another string value");
        anotherVariable.put("type", "string");

        // Create local variables with a single request
        HttpPut httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(2);

        // Check if engine has correct variables set
        Map<String, Object> variables = runtimeService.getVariablesLocal(processInstance.getId());
        assertThat(variables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("stringVariable2", "another string value")
                );
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateSingleProcessInstanceVariableAsync() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "myVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        // Create a new local variable
        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_ASYNC_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);

        assertThat(runtimeService.hasVariable(processInstance.getId(), "myVariable")).isFalse();
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        
        managementService.executeJob(job.getId());
        
        assertThat(runtimeService.hasVariable(processInstance.getId(), "myVariable")).isTrue();
        assertThat(runtimeService.getVariableLocal(processInstance.getId(), "myVariable")).isEqualTo("simple string value");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateSingleBinaryProcessVariableAsync() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

        // Add name, type and scope
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "binaryVariable");
        additionalFields.put("type", "binary");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_ASYNC_COLLECTION, processInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);
        
        assertThat(runtimeService.hasVariable(processInstance.getId(), "binaryVariable")).isFalse();
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        
        managementService.executeJob(job.getId());
        
        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariable(processInstance.getId(), "binaryVariable");
        assertThat(variableValue).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testCreateMultipleProcessVariablesAsync() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        ArrayNode requestNode = objectMapper.createArrayNode();

        // String variable
        ObjectNode stringVarNode = requestNode.addObject();
        stringVarNode.put("name", "stringVariable");
        stringVarNode.put("value", "simple string value");
        stringVarNode.put("type", "string");

        // Integer
        ObjectNode integerVarNode = requestNode.addObject();
        integerVarNode.put("name", "integerVariable");
        integerVarNode.put("value", 1234);
        integerVarNode.put("type", "integer");

        // Short
        ObjectNode shortVarNode = requestNode.addObject();
        shortVarNode.put("name", "shortVariable");
        shortVarNode.put("value", 123);
        shortVarNode.put("type", "short");

        // Long
        ObjectNode longVarNode = requestNode.addObject();
        longVarNode.put("name", "longVariable");
        longVarNode.put("value", 4567890L);
        longVarNode.put("type", "long");

        // Double
        ObjectNode doubleVarNode = requestNode.addObject();
        doubleVarNode.put("name", "doubleVariable");
        doubleVarNode.put("value", 123.456);
        doubleVarNode.put("type", "double");

        // Boolean
        ObjectNode booleanVarNode = requestNode.addObject();
        booleanVarNode.put("name", "booleanVariable");
        booleanVarNode.put("value", Boolean.TRUE);
        booleanVarNode.put("type", "boolean");

        // Date
        Calendar varCal = Calendar.getInstance();
        String isoString = getISODateString(varCal.getTime());

        ObjectNode dateVarNode = requestNode.addObject();
        dateVarNode.put("name", "dateVariable");
        dateVarNode.put("value", isoString);
        dateVarNode.put("type", "date");

        // Create local variables with a single request
        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_ASYNC_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);
        
        assertThat(runtimeService.hasVariable(processInstance.getId(), "stringVariable")).isFalse();
        assertThat(runtimeService.hasVariable(processInstance.getId(), "integerVariable")).isFalse();
        
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        
        managementService.executeJob(job.getId());

        // Check if engine has correct variables set
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("integerVariable", 1234),
                        entry("shortVariable", (short) 123),
                        entry("longVariable", 4567890L),
                        entry("doubleVariable", 123.456),
                        entry("booleanVariable", true),
                        entry("dateVariable", getDateFromISOString(isoString))
                );
    }

    /**
     * Test deleting all process variables. DELETE runtime/process-instance/{processInstanceId}/variables
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml" })
    public void testDeleteAllProcessVariables() throws Exception {

        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("var1", "This is a ProcVariable");
        processVariables.put("var2", 123);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);

        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

        // Check if local variables are gone and global remain unchanged
        assertThat(runtimeService.getVariablesLocal(processInstance.getId())).isEmpty();
    }
}
