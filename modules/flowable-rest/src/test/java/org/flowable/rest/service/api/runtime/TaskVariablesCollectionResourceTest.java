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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.HttpMultipartHelper;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to Task variables.
 *
 * @author Frederik Heremans
 * @author Filip Hrisafov
 */
public class TaskVariablesCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all task variables. GET runtime/tasks/{taskId}/variables
     */
    @Test
    @Deployment
    public void testGetTaskVariables() throws Exception {

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
        processVariables.put("instantProcVar", Instant.parse("2019-12-13T12:32:45.583345Z"));
        processVariables.put("localDateProcVar", LocalDate.parse("2020-01-18"));
        processVariables.put("localDateTimeProcVar", LocalDateTime.parse("2020-01-18T16:18:45"));
        processVariables.put("byteArrayProcVar", "Some raw bytes".getBytes());
        processVariables.put("overlappingVariable", "process-value");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);

        // Set local task variables, including one that has the same name as one
        // that is defined in the parent scope (process instance)
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("stringTaskVar", "This is a TaskVariable");
        taskVariables.put("intTaskVar", 123);
        taskVariables.put("longTaskVar", 1234L);
        taskVariables.put("shortTaskVar", (short) 123);
        taskVariables.put("doubleTaskVar", 99.99);
        taskVariables.put("booleanTaskVar", Boolean.TRUE);
        taskVariables.put("dateTaskVar", cal.getTime());
        taskVariables.put("instantTaskVar", Instant.parse("2019-12-13T12:32:45.583345Z"));
        taskVariables.put("localDateTaskVar", LocalDate.parse("2020-01-18"));
        taskVariables.put("localDateTimeTaskVar", LocalDateTime.parse("2020-01-18T16:18:45"));
        taskVariables.put("byteArrayTaskVar", "Some raw bytes".getBytes());
        taskVariables.put("overlappingVariable", "task-value");
        taskService.setVariablesLocal(task.getId(), taskVariables);

        // Request all variables (no scope provides) which include global an
        // local
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId())), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("["
                        + "  { name: 'stringProcVar', type: 'string', value: 'This is a ProcVariable', scope: 'global' },"
                        + "  { name: 'intProcVar', type: 'integer', value: 123, scope: 'global' },"
                        + "  { name: 'longProcVar', type: 'long', value: 1234, scope: 'global' },"
                        + "  { name: 'shortProcVar', type: 'short', value: 123, scope: 'global' },"
                        + "  { name: 'doubleProcVar', type: 'double', value: 99.99, scope: 'global' },"
                        + "  { name: 'booleanProcVar', type: 'boolean', value: true, scope: 'global' },"
                        + "  { name: 'dateProcVar', type: 'date', value: '${json-unit.any-string}', scope: 'global' },"
                        + "  { name: 'instantProcVar', type: 'instant', value: '2019-12-13T12:32:45.583Z', scope: 'global' },"
                        + "  { name: 'localDateProcVar', type: 'localDate', value: '2020-01-18', scope: 'global' },"
                        + "  { name: 'localDateTimeProcVar', type: 'localDateTime', value: '2020-01-18T16:18:45', scope: 'global' },"
                        + "  { name: 'byteArrayProcVar', type: 'binary', value: null, scope: 'global' },"

                        + "  { name: 'stringTaskVar', type: 'string', value: 'This is a TaskVariable', scope: 'local' },"
                        + "  { name: 'intTaskVar', type: 'integer', value: 123, scope: 'local' },"
                        + "  { name: 'longTaskVar', type: 'long', value: 1234, scope: 'local' },"
                        + "  { name: 'shortTaskVar', type: 'short', value: 123, scope: 'local' },"
                        + "  { name: 'doubleTaskVar', type: 'double', value: 99.99, scope: 'local' },"
                        + "  { name: 'booleanTaskVar', type: 'boolean', value: true, scope: 'local' },"
                        + "  { name: 'dateTaskVar', type: 'date', value: '${json-unit.any-string}', scope: 'local' },"
                        + "  { name: 'instantTaskVar', type: 'instant', value: '2019-12-13T12:32:45.583Z', scope: 'local' },"
                        + "  { name: 'localDateTaskVar', type: 'localDate', value: '2020-01-18', scope: 'local' },"
                        + "  { name: 'localDateTimeTaskVar', type: 'localDateTime', value: '2020-01-18T16:18:45', scope: 'local' },"
                        + "  { name: 'byteArrayTaskVar', type: 'binary', value: null, scope: 'local' },"
                        + "  { name: 'overlappingVariable', type: 'string', value: 'task-value', scope: 'local' }"
                        + "]");
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(23);

        // Overlapping variable should contain task-value AND be defined as "local"
        boolean foundOverlapping = false;
        for (int i = 0; i < responseNode.size(); i++) {
            JsonNode var = responseNode.get(i);
            if (var.get("name") != null && "overlappingVariable".equals(var.get("name").asText())) {
                foundOverlapping = true;
                assertThat(var.get("value").asText()).isEqualTo("task-value");
                assertThat(var.get("scope").asText()).isEqualTo("local");
                break;
            }
        }
        assertThat(foundOverlapping).isTrue();

        // Check local variables filtering
        response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()) + "?scope=local"),
                HttpStatus.SC_OK);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(12);

        for (int i = 0; i < responseNode.size(); i++) {
            JsonNode var = responseNode.get(i);
            assertThat(var.get("scope").asText()).isEqualTo("local");
        }

        // Check global variables filtering
        response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()) + "?scope=global"),
                HttpStatus.SC_OK);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(12);

        foundOverlapping = false;
        for (int i = 0; i < responseNode.size(); i++) {
            JsonNode var = responseNode.get(i);
            assertThat(var.get("scope").asText()).isEqualTo("global");
            if ("overlappingVariable".equals(var.get("name").asText())) {
                foundOverlapping = true;
                assertThat(var.get("value").asText()).isEqualTo("process-value");
            }
        }
        assertThat(foundOverlapping).isTrue();
    }

    /**
     * Test creating a single task variable. POST runtime/tasks/{taskId}/variables
     */
    @Test
    @Deployment
    public void testCreateSingleTaskVariable() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        ArrayNode requestNode = objectMapper.createArrayNode();
        ObjectNode variableNode = requestNode.addObject();
        variableNode.put("name", "myVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("scope", "local");
        variableNode.put("type", "string");

        // Create a new local variable
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent()).get(0);
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "name : 'myVariable',"
                        + "value: 'simple string value',"
                        + "scope: 'local',"
                        + "type: 'string'"
                        + "}");

        assertThat(taskService.hasVariableLocal(task.getId(), "myVariable")).isTrue();
        assertThat(taskService.getVariableLocal(task.getId(), "myVariable")).isEqualTo("simple string value");

        // Create a new global variable
        variableNode.put("name", "myVariable");
        variableNode.put("value", "Another simple string value");
        variableNode.put("scope", "global");
        variableNode.put("type", "string");

        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        responseNode = objectMapper.readTree(response.getEntity().getContent()).get(0);
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "name : 'myVariable',"
                        + "value: 'Another simple string value',"
                        + "scope: 'global',"
                        + "type: 'string'"
                        + "}");

        assertThat(runtimeService.hasVariable(task.getExecutionId(), "myVariable")).isTrue();

        // Create a new scope-less variable, which defaults to local variables
        variableNode.removeAll();
        variableNode.put("name", "scopelessVariable");
        variableNode.put("value", "simple string value");
        variableNode.put("type", "string");

        httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        responseNode = objectMapper.readTree(response.getEntity().getContent()).get(0);
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "name : 'scopelessVariable',"
                        + "value: 'simple string value',"
                        + "scope: 'local',"
                        + "type: 'string'"
                        + "}");

        assertThat(taskService.hasVariableLocal(task.getId(), "scopelessVariable")).isTrue();
        assertThat(taskService.getVariableLocal(task.getId(), "scopelessVariable")).isEqualTo("simple string value");
    }

    /**
     * Test creating a single task variable using a binary stream. POST runtime/tasks/{taskId}/variables
     */
    @Test
    public void testCreateSingleBinaryTaskVariable() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

            // Add name, type and scope
            Map<String, String> additionalFields = new HashMap<>();
            additionalFields.put("name", "binaryVariable");
            additionalFields.put("type", "binary");
            additionalFields.put("scope", "local");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "name : 'binaryVariable',"
                            + "value: null,"
                            + "scope: 'local',"
                            + "type: 'binary',"
                            + "valueUrl: '" + SERVER_URL_PREFIX + RestUrls
                            .createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLE_DATA, task.getId(), "binaryVariable") + "'"
                            + "}");

            // Check actual value of variable in engine
            Object variableValue = taskService.getVariableLocal(task.getId(), "binaryVariable");
            assertThat(variableValue).isInstanceOf(byte[].class);
            assertThat(new String((byte[]) variableValue)).isEqualTo("This is binary content");

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test creating a single task variable using a binary stream. POST runtime/tasks/{taskId}/variables
     */
    @Test
    public void testCreateSingleSerializableTaskVariable() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);
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
            additionalFields.put("scope", "local");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object", binaryContent, additionalFields));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);

            // Check "CREATED" status
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "name : 'serializableVariable',"
                            + "value: null,"
                            + "scope: 'local',"
                            + "type: 'serializable',"
                            + "valueUrl: '" + SERVER_URL_PREFIX + RestUrls
                            .createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLE_DATA, task.getId(), "serializableVariable") + "'"
                            + "}");

            // Check actual value of variable in engine
            Object variableValue = taskService.getVariableLocal(task.getId(), "serializableVariable");
            assertThat(variableValue).isInstanceOf(TestSerializableVariable.class);
            assertThat(((TestSerializableVariable) variableValue).getSomeField()).isEqualTo("some value");
        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test creating a single task variable, testing edge case exceptions. POST runtime/tasks/{taskId}/variables
     */
    @Test
    public void testCreateSingleTaskVariableEdgeCases() throws Exception {
        try {
            // Test adding variable to unexisting task
            ArrayNode requestNode = objectMapper.createArrayNode();
            ObjectNode variableNode = requestNode.addObject();
            variableNode.put("name", "existingVariable");
            variableNode.put("value", "simple string value");
            variableNode.put("scope", "local");
            variableNode.put("type", "string");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, "unexisting"));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

            // Test trying to create already existing variable
            Task task = taskService.newTask();
            taskService.saveTask(task);
            taskService.setVariable(task.getId(), "existingVariable", "Value 1");

            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_CONFLICT));

            // Test setting global variable on standalone task
            variableNode.put("name", "myVariable");
            variableNode.put("value", "simple string value");
            variableNode.put("scope", "global");
            variableNode.put("type", "string");

            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

            // Test creating nameless variable
            variableNode.removeAll();
            variableNode.put("value", "simple string value");

            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

            // Test passing in empty array
            requestNode.removeAll();
            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

            // Test passing in object instead of array
            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(objectMapper.createObjectNode().toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test creating a single task variable, testing default types when omitted. POST runtime/tasks/{taskId}/variables
     */
    @Test
    public void testCreateSingleTaskVariableDefaultTypes() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            // String type detection
            ArrayNode requestNode = objectMapper.createArrayNode();
            ObjectNode varNode = requestNode.addObject();
            varNode.put("name", "stringVar");
            varNode.put("value", "String value");
            varNode.put("scope", "local");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

            assertThat(taskService.getVariable(task.getId(), "stringVar")).isEqualTo("String value");

            // Integer type detection
            varNode.put("name", "integerVar");
            varNode.put("value", 123);
            varNode.put("scope", "local");

            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

            assertThat(taskService.getVariable(task.getId(), "integerVar")).isEqualTo(123);

            // Double type detection
            varNode.put("name", "doubleVar");
            varNode.put("value", 123.456);
            varNode.put("scope", "local");

            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

            assertThat(taskService.getVariable(task.getId(), "doubleVar")).isEqualTo(123.456);

            // Boolean type detection
            varNode.put("name", "booleanVar");
            varNode.put("value", Boolean.TRUE);
            varNode.put("scope", "local");

            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_CREATED));

            assertThat(taskService.getVariable(task.getId(), "booleanVar")).isEqualTo(Boolean.TRUE);

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test creating a multiple task variable in a single call. POST runtime/tasks/{taskId}/variables
     */
    @Test
    public void testCreateMultipleTaskVariables() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            ArrayNode requestNode = objectMapper.createArrayNode();

            // String variable
            ObjectNode stringVarNode = requestNode.addObject();
            stringVarNode.put("name", "stringVariable");
            stringVarNode.put("value", "simple string value");
            stringVarNode.put("scope", "local");
            stringVarNode.put("type", "string");

            // Integer
            ObjectNode integerVarNode = requestNode.addObject();
            integerVarNode.put("name", "integerVariable");
            integerVarNode.put("value", 1234);
            integerVarNode.put("scope", "local");
            integerVarNode.put("type", "integer");

            // Short
            ObjectNode shortVarNode = requestNode.addObject();
            shortVarNode.put("name", "shortVariable");
            shortVarNode.put("value", 123);
            shortVarNode.put("scope", "local");
            shortVarNode.put("type", "short");

            // Long
            ObjectNode longVarNode = requestNode.addObject();
            longVarNode.put("name", "longVariable");
            longVarNode.put("value", 4567890L);
            longVarNode.put("scope", "local");
            longVarNode.put("type", "long");

            // Double
            ObjectNode doubleVarNode = requestNode.addObject();
            doubleVarNode.put("name", "doubleVariable");
            doubleVarNode.put("value", 123.456);
            doubleVarNode.put("scope", "local");
            doubleVarNode.put("type", "double");

            // Boolean
            ObjectNode booleanVarNode = requestNode.addObject();
            booleanVarNode.put("name", "booleanVariable");
            booleanVarNode.put("value", Boolean.TRUE);
            booleanVarNode.put("scope", "local");
            booleanVarNode.put("type", "boolean");

            // Date
            Calendar varCal = Calendar.getInstance();
            String isoString = getISODateString(varCal.getTime());

            ObjectNode dateVarNode = requestNode.addObject();
            dateVarNode.put("name", "dateVariable");
            dateVarNode.put("value", isoString);
            dateVarNode.put("scope", "local");
            dateVarNode.put("type", "date");

            // Create local variables with a single request
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThat(responseNode.isArray()).isTrue();
            assertThat(responseNode).hasSize(7);

            // Check if engine has correct variables set
            Map<String, Object> taskVariables = taskService.getVariablesLocal(task.getId());
            assertThat(taskVariables)
                    .containsOnly(
                            entry("stringVariable", "simple string value"),
                            entry("integerVariable", 1234),
                            entry("shortVariable", (short) 123),
                            entry("longVariable", 4567890L),
                            entry("doubleVariable", 123.456),
                            entry("booleanVariable", Boolean.TRUE),
                            entry("dateVariable", getDateFromISOString(isoString))
                    );
        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test deleting all local task variables. DELETE runtime/tasks/{taskId}/variables
     */
    @Test
    @Deployment
    public void testDeleteAllLocalVariables() throws Exception {
        // Start process with all types of variables
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("var1", "This is a ProcVariable");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);

        // Set local task variables
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Map<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("var1", "This is a TaskVariable");
        taskVariables.put("var2", 123);
        taskService.setVariablesLocal(task.getId(), taskVariables);
        assertThat(taskService.getVariablesLocal(task.getId())).hasSize(2);

        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
        closeResponse(executeBinaryRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

        // Check if local variables are gone and global remain unchanged
        assertThat(taskService.getVariablesLocal(task.getId())).isEmpty();
        assertThat(taskService.getVariables(task.getId())).hasSize(1);
    }
}
