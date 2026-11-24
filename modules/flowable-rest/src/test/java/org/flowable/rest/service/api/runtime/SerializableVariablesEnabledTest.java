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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.conf.ObjectVariableSerializationEnabledApplicationConfiguration;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.HttpMultipartHelper;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import net.javacrumbs.jsonunit.core.Option;
import tools.jackson.databind.JsonNode;

/**
 * @author Filip Hrisafov
 */
@SpringJUnitWebConfig(ObjectVariableSerializationEnabledApplicationConfiguration.class)
public class SerializableVariablesEnabledTest extends BaseSpringRestTestCase {

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
     * Test getting a task variable. GET runtime/tasks/{taskId}/variables/{variableName}/data
     */
    @Test
    public void testGetTaskVariableDataSerializable() throws Exception {
        try {
            TestSerializableVariable originalSerializable = new TestSerializableVariable();
            originalSerializable.setSomeField("This is some field");

            // Test variable behaviour on standalone tasks
            Task task = taskService.newTask();
            taskService.saveTask(task);
            taskService.setVariableLocal(task.getId(), "localTaskVariable", originalSerializable);

            CloseableHttpResponse response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLE_DATA, task.getId(), "localTaskVariable")),
                    HttpStatus.SC_OK);

            // Read the serializable from the stream
            ObjectInputStream stream = new ObjectInputStream(response.getEntity().getContent());
            Object readSerializable = stream.readObject();
            assertThat(readSerializable).isInstanceOf(TestSerializableVariable.class);
            assertThat(((TestSerializableVariable) readSerializable).getSomeField()).isEqualTo("This is some field");
            assertThat(response.getEntity().getContentType().getValue()).isEqualTo("application/x-java-serialized-object");
            closeResponse(response);

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
}
