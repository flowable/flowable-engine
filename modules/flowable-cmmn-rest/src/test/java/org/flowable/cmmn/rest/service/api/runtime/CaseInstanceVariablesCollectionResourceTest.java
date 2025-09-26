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
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.HttpMultipartHelper;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to Case instance variables.
 *
 * @author Tijs Rademakers
 */
public class CaseInstanceVariablesCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all case variables. GET cmmn-runtime/case-instances/{caseInstanceId}/variables
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseVariables() throws Exception {
        Calendar cal = Calendar.getInstance();

        // Start process with all types of variables
        Map<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringProcVar", "This is a ProcVariable");
        caseVariables.put("intProcVar", 123);
        caseVariables.put("longProcVar", 1234L);
        caseVariables.put("shortProcVar", (short) 123);
        caseVariables.put("doubleProcVar", 99.99);
        caseVariables.put("booleanProcVar", Boolean.TRUE);
        caseVariables.put("dateProcVar", cal.getTime());
        caseVariables.put("byteArrayProcVar", "Some raw bytes".getBytes());
        caseVariables.put("overlappingVariable", "process-value");

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();

        // Request all variables (no scope provides) which include global an local
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(
                CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId())), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(9);
    }

    /**
     * Test creating a single case variable. POST cmmn-runtime/case-instance/{caseInstanceId}/variables
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateSingleCaseInstanceVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "myVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        // Create a new local variable
        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent()).get(0);
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  name: 'myVariable',"
                        + "  value: 'simple string value',"
                        + "  scope: 'global',"
                        + "  type: 'string'"
                        + "}");

        assertThat(runtimeService.hasVariable(caseInstance.getId(), "myVariable")).isTrue();
        assertThat(runtimeService.getVariable(caseInstance.getId(), "myVariable")).isEqualTo("simple string value");
    }

    /**
     * Test creating a single case variable using a binary stream. POST cmmn-runtime/case-instances/{caseInstanceId}/variables
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateSingleBinaryCaseVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

        // Add name, type and scope
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "binaryVariable");
        additionalFields.put("type", "binary");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  name: 'binaryVariable',"
                        + "  value: null,"
                        + "  scope: 'global',"
                        + "  type: 'binary',"
                        + "  valueUrl: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "binaryVariable") + "'"
                        + "}");

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariable(caseInstance.getId(), "binaryVariable");
        assertThat(variableValue).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content");
    }

    /**
     * Test creating a single process variable using a binary stream containing a serializable. POST runtime/process-instances/{processInstanceId}/variables
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateSingleSerializableCaseVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

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
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  name: 'serializableVariable',"
                        + "  value: null,"
                        + "  scope: 'global',"
                        + "  type: 'serializable',"
                        + "  valueUrl: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "serializableVariable") + "'"
                        + "}");

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariable(caseInstance.getId(), "serializableVariable");
        assertThat(variableValue).isInstanceOf(TestSerializableVariable.class);
        assertThat(((TestSerializableVariable) variableValue).getSomeField()).isEqualTo("some value");
    }

    /**
     * Test creating a single case variable, testing edge case exceptions. POST cmmn-runtime/case-instances/{caseInstanceId}/variables
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateSingleCaseVariableEdgeCases() throws Exception {
        // Test adding variable to unexisting execution
        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "existingVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, "unexisting"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

        // Test trying to create already existing variable
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.setVariable(caseInstance.getId(), "existingVariable", "I already exist");

        httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        // Test creating nameless variable
        variableNode.removeAll();
        variableNode.put("value", "simple string value");

        httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test passing in empty array
        requestNode.removeAll();
        httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test passing in object instead of array
        httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(objectMapper.createObjectNode().toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test creating a single case variable, testing default types when omitted. POST cmmn-runtime/case-instances/{caseInstanceId}/variables
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateSingleCaseVariableDefaultTypes() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        // String type detection
        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode varNode = requestNode.addObject();
        varNode.put("name", "stringVar");
        varNode.put("value", "String value");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertThat(runtimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("String value");

        // Integer type detection
        varNode.put("name", "integerVar");
        varNode.put("value", 123);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertThat(runtimeService.getVariable(caseInstance.getId(), "integerVar")).isEqualTo(123);

        // Double type detection
        varNode.put("name", "doubleVar");
        varNode.put("value", 123.456);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertThat(runtimeService.getVariable(caseInstance.getId(), "doubleVar")).isEqualTo(123.456);

        // Boolean type detection
        varNode.put("name", "booleanVar");
        varNode.put("value", Boolean.TRUE);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertThat(runtimeService.getVariable(caseInstance.getId(), "booleanVar")).isEqualTo(Boolean.TRUE);
    }

    /**
     * Test creating multiple case variables in a single call. POST cmmn-runtime/case-instance/{caseInstanceId}/variables
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateMultipleCaseVariables() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

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
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(7);

        // Check if engine has correct variables set
        Map<String, Object> variables = runtimeService.getVariables(caseInstance.getId());

        assertThat(variables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("integerVariable", 1234),
                        entry("shortVariable", (short) 123),
                        entry("longVariable", 4567890L),
                        entry("doubleVariable", 123.456),
                        entry("booleanVariable", Boolean.TRUE),
                        entry("dateVariable", getDateFromISOString(isoString))
                );
    }

    /**
     * Test creating multiple case variables in a single call. POST cmmn-runtime/case-instance/{caseInstanceId}/variables?override=true
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateMultipleCaseVariablesWithOverride() throws Exception {

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.setVariable(caseInstance.getId(), "stringVariable", "initialValue");
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
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(2);

        // Check if engine has correct variables set
        Map<String, Object> variables = runtimeService.getVariables(caseInstance.getId());
        assertThat(variables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("stringVariable2", "another string value")
                );
    }
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateSingleCaseInstanceVariableAsync() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "myVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        // Create a new local variable
        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_ASYNC_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);
        
        assertThat(runtimeService.hasVariable(caseInstance.getId(), "myVariable")).isFalse();
        
        Job job = managementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        
        managementService.executeJob(job.getId());
        
        assertThat(runtimeService.hasVariable(caseInstance.getId(), "myVariable")).isTrue();
        assertThat(runtimeService.getVariable(caseInstance.getId(), "myVariable")).isEqualTo("simple string value");
    }
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateSingleBinaryCaseVariableAsync() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

        // Add name, type and scope
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "binaryVariable");
        additionalFields.put("type", "binary");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_ASYNC_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);
        
        assertThat(runtimeService.hasVariable(caseInstance.getId(), "binaryVariable")).isFalse();
        
        Job job = managementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        
        managementService.executeJob(job.getId());
        
        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariable(caseInstance.getId(), "binaryVariable");
        assertThat(variableValue).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content");
    }
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateMultipleCaseVariablesAsync() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

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
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_ASYNC_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        closeResponse(response);
        
        assertThat(runtimeService.hasVariable(caseInstance.getId(), "stringVariable")).isFalse();
        assertThat(runtimeService.hasVariable(caseInstance.getId(), "integerVariable")).isFalse();
        
        Job job = managementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        
        managementService.executeJob(job.getId());

        // Check if engine has correct variables set
        Map<String, Object> variables = runtimeService.getVariables(caseInstance.getId());

        assertThat(variables)
                .containsOnly(
                        entry("stringVariable", "simple string value"),
                        entry("integerVariable", 1234),
                        entry("shortVariable", (short) 123),
                        entry("longVariable", 4567890L),
                        entry("doubleVariable", 123.456),
                        entry("booleanVariable", Boolean.TRUE),
                        entry("dateVariable", getDateFromISOString(isoString))
                );
    }

    /**
     * Test deleting all case variables. DELETE cmmn-runtime/case-instance/{caseInstanceId}/variables
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDeleteAllCasesVariables() throws Exception {
        Map<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("var1", "This is a CaseVariable");
        caseVariables.put("var2", 123);
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();

        HttpDelete httpDelete = new HttpDelete(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

        // Check if local variables are gone and global remain unchanged
        assertThat(runtimeService.getVariables(caseInstance.getId())).isEmpty();
    }
}
