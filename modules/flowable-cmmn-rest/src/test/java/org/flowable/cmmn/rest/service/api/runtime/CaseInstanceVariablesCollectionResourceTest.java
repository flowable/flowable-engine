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
import org.flowable.cmmn.rest.service.api.RestUrls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to Case instance variables.
 * 
 * @author Tijs Rademakers
 */
public class CaseInstanceVariablesCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all case variables. GET cmmn-runtime/case-instances/{caseInstanceId}/variables
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
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
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(
                        RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId())), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertTrue(responseNode.isArray());
        assertEquals(9, responseNode.size());
    }

    /**
     * Test creating a single case variable. POST cmmn-runtime/case-instance/{caseInstanceId}/variables
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testCreateSingleProcessInstanceVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "myVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        // Create a new local variable
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent()).get(0);
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals("myVariable", responseNode.get("name").asText());
        assertEquals("simple string value", responseNode.get("value").asText());
        assertEquals("global", responseNode.get("scope").asText());
        assertEquals("string", responseNode.get("type").asText());
        assertNull(responseNode.get("valueUrl"));

        assertTrue(runtimeService.hasVariable(caseInstance.getId(), "myVariable"));
        assertEquals("simple string value", runtimeService.getVariable(caseInstance.getId(), "myVariable"));
    }

    /**
     * Test creating a single case variable using a binary stream. POST cmmn-runtime/case-instances/{caseInstanceId}/variables
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testCreateSingleBinaryProcessVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

        // Add name, type and scope
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "binaryVariable");
        additionalFields.put("type", "binary");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals("binaryVariable", responseNode.get("name").asText());
        assertTrue(responseNode.get("value").isNull());
        assertEquals("local", responseNode.get("scope").asText());
        assertEquals("binary", responseNode.get("type").asText());
        assertFalse(responseNode.get("valueUrl").isNull());
        assertTrue(responseNode.get("valueUrl").asText().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "binaryVariable")));

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariable(caseInstance.getId(), "binaryVariable");
        assertNotNull(variableValue);
        assertTrue(variableValue instanceof byte[]);
        assertEquals("This is binary content", new String((byte[]) variableValue));
    }

    /**
     * Test creating a single process variable using a binary stream containing a serializable. POST runtime/process-instances/{processInstanceId}/variables
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testCreateSingleSerializableProcessVariable() throws Exception {
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
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals("serializableVariable", responseNode.get("name").asText());
        assertTrue(responseNode.get("value").isNull());
        assertEquals("local", responseNode.get("scope").asText());
        assertEquals("serializable", responseNode.get("type").asText());
        assertFalse(responseNode.get("valueUrl").isNull());
        assertTrue(responseNode.get("valueUrl").asText().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "serializableVariable")));

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariable(caseInstance.getId(), "serializableVariable");
        assertNotNull(variableValue);
        assertTrue(variableValue instanceof TestSerializableVariable);
        assertEquals("some value", ((TestSerializableVariable) variableValue).getSomeField());
    }

    /**
     * Test creating a single case variable, testing edge case exceptions. POST cmmn-runtime/case-instances/{caseInstanceId}/variables
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testCreateSingleCaseVariableEdgeCases() throws Exception {
        // Test adding variable to unexisting execution
        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "existingVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, "unexisting"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

        // Test trying to create already existing variable
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.setVariable(caseInstance.getId(), "existingVariable", "I already exist");

        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        // Test creating nameless variable
        variableNode.removeAll();
        variableNode.put("value", "simple string value");

        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test passing in empty array
        requestNode.removeAll();
        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test passing in object instead of array
        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(objectMapper.createObjectNode().toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test creating a single case variable, testing default types when omitted. POST cmmn-runtime/case-instances/{caseInstanceId}/variables
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testCreateSingleProcessVariableDefaultTypes() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        // String type detection
        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode varNode = requestNode.addObject();
        varNode.put("name", "stringVar");
        varNode.put("value", "String value");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertEquals("String value", runtimeService.getVariable(caseInstance.getId(), "stringVar"));

        // Integer type detection
        varNode.put("name", "integerVar");
        varNode.put("value", 123);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertEquals(123, runtimeService.getVariable(caseInstance.getId(), "integerVar"));

        // Double type detection
        varNode.put("name", "doubleVar");
        varNode.put("value", 123.456);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertEquals(123.456, runtimeService.getVariable(caseInstance.getId(), "doubleVar"));

        // Boolean type detection
        varNode.put("name", "booleanVar");
        varNode.put("value", Boolean.TRUE);

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

        assertEquals(Boolean.TRUE, runtimeService.getVariable(caseInstance.getId(), "booleanVar"));
    }

    /**
     * Test creating multiple case variables in a single call. POST cmmn-runtime/case-instance/{caseInstanceId}/variables
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testCreateMultipleProcessVariables() throws Exception {
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
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertTrue(responseNode.isArray());
        assertEquals(7, responseNode.size());

        // Check if engine has correct variables set
        Map<String, Object> variables = runtimeService.getVariables(caseInstance.getId());
        assertEquals(7, variables.size());

        assertEquals("simple string value", variables.get("stringVariable"));
        assertEquals(1234, variables.get("integerVariable"));
        assertEquals((short) 123, variables.get("shortVariable"));
        assertEquals(4567890L, variables.get("longVariable"));
        assertEquals(123.456, variables.get("doubleVariable"));
        assertEquals(Boolean.TRUE, variables.get("booleanVariable"));
        assertEquals(longDateFormat.parse(isoString), variables.get("dateVariable"));
    }

    /**
     * Test creating multiple case variables in a single call. POST cmmn-runtime/case-instance/{caseInstanceId}/variables?override=true
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
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
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertTrue(responseNode.isArray());
        assertEquals(2, responseNode.size());

        // Check if engine has correct variables set
        Map<String, Object> variables = runtimeService.getVariables(caseInstance.getId());
        assertEquals(2, variables.size());

        assertEquals("simple string value", variables.get("stringVariable"));
        assertEquals("another string value", variables.get("stringVariable2"));
    }

    /**
     * Test deleting all case variables. DELETE cmmn-runtime/case-instance/{caseInstanceId}/variables
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testDeleteAllCasesVariables() throws Exception {
        Map<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("var1", "This is a CaseVariable");
        caseVariables.put("var2", 123);
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();

        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

        // Check if local variables are gone and global remain unchanged
        assertEquals(0, runtimeService.getVariables(caseInstance.getId()).size());
    }
}
