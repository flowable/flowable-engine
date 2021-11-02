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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
 * Test for all REST-operations related to a single task variable.
 *
 * @author Frederik Heremans
 * @author Filip Hrisafov
 */
public class ProcessInstanceVariableResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a process instance variable. GET runtime/process-instances/{processInstanceId}/variables/{variableName}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testGetProcessInstanceVariable() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariable(processInstance.getId(), "variable", "processValue");

        CloseableHttpResponse response = executeRequest(new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "variable")),
                HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "name: 'variable',"
                        + "type: 'string',"
                        + "value: 'processValue',"
                        + "scope: null"
                        + "}");

        // Illegal scope
        closeResponse(executeRequest(new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "variable")
                                + "?scope=illegal"),
                HttpStatus.SC_BAD_REQUEST));

        // Unexisting process
        closeResponse(executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, "unexisting", "variable")),
                HttpStatus.SC_NOT_FOUND));

        // Unexisting variable
        closeResponse(executeRequest(new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "unexistingVariable")),
                HttpStatus.SC_NOT_FOUND));
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testGetProcessInstanceInstantVariable() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Instant now = Instant.now();
        Instant nowWithoutNanos = now.truncatedTo(ChronoUnit.MILLIS);
        runtimeService.setVariable(processInstance.getId(), "variable", now);

        CloseableHttpResponse response = executeRequest(
                new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "variable")),
                HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());

        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  name: 'variable',"
                        + "  type: 'instant',"
                        + "  value: '" + nowWithoutNanos.toString() + "'"
                        + "}");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testGetProcessInstanceLocalDateVariable() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        LocalDate now = LocalDate.now();
        runtimeService.setVariable(processInstance.getId(), "variable", now);

        CloseableHttpResponse response = executeRequest(
                new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "variable")),
                HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());

        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  name: 'variable',"
                        + "  type: 'localDate',"
                        + "  value: '" + now + "'"
                        + "}");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testGetProcessInstanceLocalDateTimeVariable() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowWithoutNanos = now.truncatedTo(ChronoUnit.MILLIS);
        runtimeService.setVariable(processInstance.getId(), "variable", now);

        CloseableHttpResponse response = executeRequest(
                new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "variable")),
                HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());

        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  name: 'variable',"
                        + "  type: 'localDateTime',"
                        + "  value: '" + nowWithoutNanos + "'"
                        + "}");
    }

    /**
     * Test getting a process instance variable data. GET runtime/process-instances/{processInstanceId}/variables/{variableName}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testGetProcessInstanceVariableData() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariableLocal(processInstance.getId(), "var", "This is a binary piece of text".getBytes());

        CloseableHttpResponse response = executeRequest(new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "var")),
                HttpStatus.SC_OK);

        String actualResponseBytesAsText = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(actualResponseBytesAsText).isEqualTo("This is a binary piece of text");
        assertThat(response.getEntity().getContentType().getValue()).isEqualTo("application/octet-stream");
    }

    /**
     * Test getting a process instance variable data. GET runtime/process-instances/{processInstanceId}/variables/{variableName}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testGetProcessInstanceVariableDataSerializable() throws Exception {

        TestSerializableVariable originalSerializable = new TestSerializableVariable();
        originalSerializable.setSomeField("This is some field");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariableLocal(processInstance.getId(), "var", originalSerializable);

        CloseableHttpResponse response = executeRequest(new HttpGet(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "var")),
                HttpStatus.SC_OK);

        // Read the serializable from the stream
        ObjectInputStream stream = new ObjectInputStream(response.getEntity().getContent());
        Object readSerializable = stream.readObject();
        assertThat(readSerializable).isInstanceOf(TestSerializableVariable.class);
        assertThat(((TestSerializableVariable) readSerializable).getSomeField()).isEqualTo("This is some field");
        assertThat(response.getEntity().getContentType().getValue()).isEqualTo("application/x-java-serialized-object");
        closeResponse(response);
    }

    /**
     * Test getting a process instance variable, for illegal vars. GET runtime/process-instances/{processInstanceId}/variables/{variableName}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testGetProcessInstanceVariableDataForIllegalVariables() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.setVariableLocal(processInstance.getId(), "localTaskVariable", "this is a plain string variable");

        // Try getting data for non-binary variable
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "localTaskVariable")),
                HttpStatus.SC_NOT_FOUND));

        // Try getting data for unexisting property
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "unexistingVariable")),
                HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single process variable in, including "not found" check.
     * <p>
     * DELETE runtime/process-instances/{processInstanceId}/variables/{variableName}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testDeleteProcessVariable() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("myVariable", (Object) "processValue"));

        // Delete variable
        closeResponse(executeRequest(new HttpDelete(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "myVariable")),
                HttpStatus.SC_NO_CONTENT));

        assertThat(runtimeService.hasVariable(processInstance.getId(), "myVariable")).isFalse();

        // Run the same delete again, variable is not there so 404 should be returned
        closeResponse(executeRequest(new HttpDelete(
                        SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "myVariable")),
                HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test updating a single process variable, including "not found" check.
     * <p>
     * PUT runtime/process-instances/{processInstanceId}/variables/{variableName}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testUpdateProcessVariable() throws Exception {
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("overlappingVariable", (Object) "processValue"));
        runtimeService.setVariable(processInstance.getId(), "myVar", "value");

        // Update variable
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "myVar");
        requestNode.put("value", "updatedValue");
        requestNode.put("type", "string");

        HttpPut httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "myVar"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "value: 'updatedValue'"
                        + "}");

        // Try updating with mismatch between URL and body variableName
        requestNode.put("name", "unexistingVariable");
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST));

        // Try updating unexisting property
        requestNode.put("name", "unexistingVariable");
        httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "unexistingVariable"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_NOT_FOUND));
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testUpdateInstantProcessVariable() throws Exception {
        Instant initial = Instant.parse("2019-12-03T12:32:45.583345Z");
        Instant tenDaysLater = initial.plus(10, ChronoUnit.DAYS);
        Instant tenDaysLaterWithoutNanos = tenDaysLater.truncatedTo(ChronoUnit.MILLIS);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("overlappingVariable", (Object) "processValue"));
        runtimeService.setVariable(processInstance.getId(), "myVar", initial);

        // Update variable
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "myVar");
        requestNode.put("value", "2019-12-13T12:32:45.583345Z");
        requestNode.put("type", "instant");

        HttpPut httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "myVar"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        assertThat(runtimeService.getVariable(processInstance.getId(), "myVar")).isEqualTo(tenDaysLaterWithoutNanos);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  value: '2019-12-13T12:32:45.583345Z'"
                        + "}");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testUpdateLocalDateProcessVariable() throws Exception {
        LocalDate initial = LocalDate.parse("2020-01-18");
        LocalDate tenDaysLater = initial.plusDays(10);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("overlappingVariable", (Object) "processValue"));
        runtimeService.setVariable(processInstance.getId(), "myVar", initial);

        // Update variable
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "myVar");
        requestNode.put("value", "2020-01-28");
        requestNode.put("type", "localDate");

        HttpPut httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "myVar"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        assertThat(runtimeService.getVariable(processInstance.getId(), "myVar")).isEqualTo(tenDaysLater);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  value: '2020-01-28'"
                        + "}");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testUpdateLocalDateTimeProcessVariable() throws Exception {
        LocalDateTime initial = LocalDateTime.parse("2020-01-18T12:32:45");
        LocalDateTime tenDaysLater = initial.plus(10, ChronoUnit.DAYS);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("overlappingVariable", (Object) "processValue"));
        runtimeService.setVariable(processInstance.getId(), "myVar", initial);

        // Update variable
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "myVar");
        requestNode.put("value", "2020-01-28T12:32:45");
        requestNode.put("type", "localDateTime");

        HttpPut httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "myVar"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        assertThat(runtimeService.getVariable(processInstance.getId(), "myVar"))
                .isEqualTo(tenDaysLater);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  value: '2020-01-28T12:32:45'"
                        + "}");
    }

    /**
     * Test updating a single process variable using a binary stream. PUT runtime/process-instances/{processInstanceId}/variables/{variableName}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceVariableResourceTest.testProcess.bpmn20.xml" })
    public void testUpdateBinaryProcessVariable() throws Exception {

        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("overlappingVariable", (Object) "processValue"));
        runtimeService.setVariable(processInstance.getId(), "binaryVariable", "Initial binary value".getBytes());

        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

        // Add name and type
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "binaryVariable");
        additionalFields.put("type", "binary");

        HttpPut httpPut = new HttpPut(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE, processInstance.getId(), "binaryVariable"));
        httpPut.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
        CloseableHttpResponse response = executeBinaryRequest(httpPut, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "name: 'binaryVariable',"
                        + "type : 'binary',"
                        + "value : null,"
                        + "valueUrl : '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_DATA, processInstance.getId(), "binaryVariable") + "',"
                        + "scope : 'local'"
                        + "}");

        // Check actual value of variable in engine
        Object variableValue = runtimeService.getVariableLocal(processInstance.getId(), "binaryVariable");
        assertThat(variableValue).isInstanceOf(byte[].class);
        assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content");
    }
}
