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
import java.io.InputStream;
import java.io.ObjectInputStream;
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
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.HttpMultipartHelper;
import org.flowable.cmmn.rest.service.api.RestUrls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to a single task variable.
 * 
 * @author Tijs Rademakers
 */
public class CaseInstanceVariableResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a process instance variable. GET cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstanceVariable() throws Exception {

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.setVariable(caseInstance.getId(), "variable", "caseValue");

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE, 
                        caseInstance.getId(), "variable")), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals("caseValue", responseNode.get("value").asText());
        assertEquals("variable", responseNode.get("name").asText());
        assertEquals("string", responseNode.get("type").asText());

        // Unexisting case
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE, "unexisting", "variable")), HttpStatus.SC_NOT_FOUND));

        // Unexisting variable
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE, 
                        caseInstance.getId(), "unexistingVariable")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test getting a case instance variable data. GET cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstanceVariableData() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.setVariable(caseInstance.getId(), "var", "This is a binary piece of text".getBytes());

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, 
                        caseInstance.getId(), "var")), HttpStatus.SC_OK);

        String actualResponseBytesAsText = IOUtils.toString(response.getEntity().getContent(), "utf-8");
        closeResponse(response);
        assertEquals("This is a binary piece of text", actualResponseBytesAsText);
        assertEquals("application/octet-stream", response.getEntity().getContentType().getValue());
    }

    /**
     * Test getting a case instance variable data. GET cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstanceVariableDataSerializable() throws Exception {

        TestSerializableVariable originalSerializable = new TestSerializableVariable();
        originalSerializable.setSomeField("This is some field");

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.setVariable(caseInstance.getId(), "var", originalSerializable);

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, 
                        caseInstance.getId(), "var")), HttpStatus.SC_OK);

        // Read the serializable from the stream
        ObjectInputStream stream = new ObjectInputStream(response.getEntity().getContent());
        Object readSerializable = stream.readObject();
        assertNotNull(readSerializable);
        assertTrue(readSerializable instanceof TestSerializableVariable);
        assertEquals("This is some field", ((TestSerializableVariable) readSerializable).getSomeField());
        assertEquals("application/x-java-serialized-object", response.getEntity().getContentType().getValue());
        closeResponse(response);
    }

    /**
     * Test getting a case instance variable, for illegal vars. GET cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstanceVariableDataForIllegalVariables() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.setVariable(caseInstance.getId(), "localTaskVariable", "this is a plain string variable");

        // Try getting data for non-binary variable
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, 
                        caseInstance.getId(), "localTaskVariable")), HttpStatus.SC_NOT_FOUND));

        // Try getting data for unexisting property
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, 
                        caseInstance.getId(), "unexistingVariable")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single case variable in, including "not found" check.
     * 
     * DELETE cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testDeleteProcessVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(Collections.singletonMap("myVariable", (Object) "processValue")).start();
        
        // Delete variable
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE, 
                        caseInstance.getId(), "myVariable")), HttpStatus.SC_NO_CONTENT));

        assertFalse(runtimeService.hasVariable(caseInstance.getId(), "myVariable"));

        // Run the same delete again, variable is not there so 404 should be returned
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE, 
                        caseInstance.getId(), "myVariable")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test updating a single process variable, including "not found" check.
     * 
     * PUT cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testUpdateProcessVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(Collections.singletonMap("overlappingVariable", (Object) "processValue")).start();
        runtimeService.setVariable(caseInstance.getId(), "myVar", "value");

        // Update variable
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "myVar");
        requestNode.put("value", "updatedValue");
        requestNode.put("type", "string");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE, caseInstance.getId(), "myVar"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals("updatedValue", responseNode.get("value").asText());

        // Try updating with mismatch between URL and body variableName
        requestNode.put("name", "unexistingVariable");
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST));

        // Try updating unexisting property
        requestNode.put("name", "unexistingVariable");
        httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE, caseInstance.getId(), "unexistingVariable"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));
    }

    /**
     * Test updating a single case variable using a binary stream. PUT cmmn-runtime/case-instances/{caseInstanceId}/variables/{variableName}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testUpdateBinaryCaseVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(Collections.singletonMap("overlappingVariable", (Object) "processValue")).start();
        runtimeService.setVariable(caseInstance.getId(), "binaryVariable", "Initial binary value".getBytes());

        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

        // Add name and type
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "binaryVariable");
        additionalFields.put("type", "binary");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE, caseInstance.getId(), "binaryVariable"));
        httpPut.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPut, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals("binaryVariable", responseNode.get("name").asText());
        assertTrue(responseNode.get("value").isNull());
        assertEquals("binary", responseNode.get("type").asText());
        assertNotNull(responseNode.get("valueUrl").isNull());
        assertTrue(responseNode.get("valueUrl").asText().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "binaryVariable")));

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariable(caseInstance.getId(), "binaryVariable");
        assertNotNull(variableValue);
        assertTrue(variableValue instanceof byte[]);
        assertEquals("This is binary content", new String((byte[]) variableValue));
    }
}
