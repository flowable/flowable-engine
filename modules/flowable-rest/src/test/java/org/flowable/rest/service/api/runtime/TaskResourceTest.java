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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Task resource.
 *
 * @author Frederik Heremans
 */
@MockitoSettings
public class TaskResourceTest extends BaseSpringRestTestCase {

    @Mock
    protected FormEngineConfigurationApi formEngineConfiguration;

    @Mock
    protected FormService formEngineFormService;

    @Mock
    protected FormRepositoryService formRepositoryService;

    @BeforeEach
    public void initializeMocks() {
        Map engineConfigurations = processEngineConfiguration.getEngineConfigurations();
        engineConfigurations.put(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration);
    }

    @AfterEach
    public void resetMocks() {
        processEngineConfiguration.getEngineConfigurations().remove(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
    }

    /**
     * Test getting a single task, spawned by a process. GET runtime/tasks/{taskId}
     */
    @Test
    @Deployment
    public void testGetProcessTask() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        processEngineConfiguration.getClock().setCurrentTime(Date.from(now));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setDueDate(task.getId(), Date.from(now));
        taskService.setOwner(task.getId(), "owner");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        String url = buildUrl(RestUrls.URL_TASK, task.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        // Check resulting task
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id : '" + task.getId() + "',"
                        + "assignee: '" + task.getAssignee() + "',"
                        + "owner: '" + task.getOwner() + "',"
                        + "formKey: '" + task.getFormKey() + "',"
                        + "description: '" + task.getDescription() + "',"
                        + "name: '" + task.getName() + "',"
                        + "priority: " + task.getPriority() + ","
                        + "parentTaskId: null,"
                        + "delegationState: null,"
                        + "tenantId: \"\","
                        + "executionUrl: '" + buildUrl(RestUrls.URL_EXECUTION, task.getExecutionId()) + "',"
                        + "processInstanceUrl: '" + buildUrl(RestUrls.URL_PROCESS_INSTANCE, task.getProcessInstanceId()) + "',"
                        + "processDefinitionUrl: '" + buildUrl(RestUrls.URL_PROCESS_DEFINITION, task.getProcessDefinitionId()) + "',"
                        + "url: '" + url + "',"
                        + "dueDate: '" + getISODateString(task.getDueDate()) + "',"
                        + "createTime: '" + getISODateString(task.getCreateTime()) + "'"
                        + "}");

        // Set tenant on deployment
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));

        response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode.get("tenantId").asText()).isEqualTo("myTenant");
    }

    /**
     * Test getting a single task, created using the API. GET runtime/tasks/{taskId}
     */
    @Test
    public void testGetProcessAdhoc() throws Exception {
        try {

            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            processEngineConfiguration.getClock().setCurrentTime(Date.from(now));

            Task parentTask = taskService.newTask();
            taskService.saveTask(parentTask);

            Task task = taskService.newTask();
            task.setParentTaskId(parentTask.getId());
            task.setName("Task name");
            task.setDescription("Descriptions");
            task.setAssignee("kermit");
            task.setDelegationState(DelegationState.RESOLVED);
            task.setDescription("Description");
            task.setDueDate(Date.from(now));
            task.setOwner("owner");
            task.setPriority(20);
            taskService.saveTask(task);

            String url = buildUrl(RestUrls.URL_TASK, task.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

            // Check resulting task
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "id : '" + task.getId() + "',"
                            + "assignee: '" + task.getAssignee() + "',"
                            + "owner: '" + task.getOwner() + "',"
                            + "description: '" + task.getDescription() + "',"
                            + "name: '" + task.getName() + "',"
                            + "priority: " + task.getPriority() + ","
                            + "delegationState: 'resolved',"
                            + "executionId: null,"
                            + "processInstanceId: null,"
                            + "processDefinitionId: null,"
                            + "url: '" + url + "',"
                            + "parentTaskUrl: '" + buildUrl(RestUrls.URL_TASK, parentTask.getId()) + "',"
                            + "dueDate: '" + getISODateString(task.getDueDate()) + "',"
                            + "createTime: '" + getISODateString(task.getCreateTime()) + "'"
                            + "}");

        } finally {

            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test updating a single task without passing in any value, no values should be altered. PUT runtime/tasks/{taskId}
     */
    @Test
    public void testUpdateTaskNoOverrides() throws Exception {
        try {
            Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            Task parentTask = taskService.newTask();
            taskService.saveTask(parentTask);

            Task task = taskService.newTask();
            task.setParentTaskId(parentTask.getId());
            task.setName("Task name");
            task.setDescription("Description");
            task.setAssignee("kermit");
            task.setDelegationState(DelegationState.RESOLVED);
            task.setDescription("Description");
            task.setDueDate(Date.from(now));
            task.setOwner("owner");
            task.setPriority(20);
            taskService.saveTask(task);

            ObjectNode requestNode = objectMapper.createObjectNode();

            // Execute the request with an empty request JSON-object
            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, task.getId()));
            httpPut.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("Task name");
            assertThat(task.getDescription()).isEqualTo("Description");
            assertThat(task.getAssignee()).isEqualTo("kermit");
            assertThat(task.getOwner()).isEqualTo("owner");
            assertThat(task.getPriority()).isEqualTo(20);
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);
            assertThat(task.getDueDate()).isEqualTo(Date.from(now));
            assertThat(task.getParentTaskId()).isEqualTo(parentTask.getId());

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test updating a single task. PUT runtime/tasks/{taskId}
     */
    @Test
    public void testUpdateTask() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            Task parentTask = taskService.newTask();
            taskService.saveTask(parentTask);

            ObjectNode requestNode = objectMapper.createObjectNode();

            Instant dueDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            String dueDateString = getISODateString(Date.from(dueDate));

            requestNode.put("name", "New task name");
            requestNode.put("description", "New task description");
            requestNode.put("assignee", "assignee");
            requestNode.put("owner", "owner");
            requestNode.put("priority", 20);
            requestNode.put("delegationState", "resolved");
            requestNode.put("dueDate", dueDateString);
            requestNode.put("parentTaskId", parentTask.getId());

            // Execute the request
            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, task.getId()));
            httpPut.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("New task name");
            assertThat(task.getDescription()).isEqualTo("New task description");
            assertThat(task.getAssignee()).isEqualTo("assignee");
            assertThat(task.getOwner()).isEqualTo("owner");
            assertThat(task.getPriority()).isEqualTo(20);
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);
            assertThat(task.getDueDate()).isEqualTo(getDateFromISOString(dueDateString));
            assertThat(task.getParentTaskId()).isEqualTo(parentTask.getId());

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test updating an unexisting task. PUT runtime/tasks/{taskId}
     */
    @Test
    public void testUpdateUnexistingTask() throws Exception {
        ObjectNode requestNode = objectMapper.createObjectNode();

        // Execute the request with an empty request JSON-object
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, "unexistingtask"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single task. DELETE runtime/tasks/{taskId}
     */
    @Test
    public void testDeleteTask() throws Exception {
        try {

            // 1. Simple delete
            Task task = taskService.newTask();
            taskService.saveTask(task);
            String taskId = task.getId();

            // Execute the request
            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task).isNull();

            if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                // Check that the historic task has not been deleted
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNotNull();
            }

            // 2. Cascade delete
            task = taskService.newTask();
            taskService.saveTask(task);
            taskId = task.getId();

            // Execute the request
            httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId) + "?cascadeHistory=true");
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task).isNull();

            if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                // Check that the historic task has been deleted
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNull();
            }

            // 3. Delete with reason
            task = taskService.newTask();
            taskService.saveTask(task);
            taskId = task.getId();

            // Execute the request
            httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId) + "?deleteReason=fortestingpurposes");
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task).isNull();

            if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                // Check that the historic task has been deleted and
                // delete-reason has been set
                HistoricTaskInstance instance = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
                assertThat(instance).isNotNull();
                assertThat(instance.getDeleteReason()).isEqualTo("fortestingpurposes");
            }

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }

            // Clean historic tasks with no runtime-counterpart
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().list();
            for (HistoricTaskInstance task : historicTasks) {
                historyService.deleteHistoricTaskInstance(task.getId());
            }
        }
    }

    /**
     * Test deleting a single task linked with a process instance. DELETE runtime/tasks/{taskId}
     */
    @Test
    @Deployment(resources = "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml")
    public void testDeleteTaskLinkedWithAProcessInstance() throws Exception {
        String processInstanceId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start()
                .getId();

        // 1. Simple delete
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(task.getExecutionId()).as("task executionId").isNotNull();
        String taskId = task.getId();

        // Execute the request
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_FORBIDDEN);
        JsonNode responseNode = readContent(response);
        closeResponse(response);

        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "message: 'Forbidden',"
                        + "exception: 'Cannot delete a task that is part of a process instance.'"
                        + "}");

        assertThat(taskService.createTaskQuery().taskId(task.getId()).singleResult()).isNotNull();

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            // Check that the historic task has not been deleted
            assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNotNull();
        }

        // 2. Cascade delete
        // Execute the request
        httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId) + "?cascadeHistory=true");
        response = executeRequest(httpDelete, HttpStatus.SC_FORBIDDEN);
        responseNode = readContent(response);
        closeResponse(response);

        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "message: 'Forbidden',"
                        + "exception: 'Cannot delete a task that is part of a process instance.'"
                        + "}");

        assertThat(taskService.createTaskQuery().taskId(task.getId()).singleResult()).isNotNull();

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            // Check that the historic task has been deleted
            assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNotNull();
        }

        // 3. Delete with reason
        // Execute the request
        httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId) + "?deleteReason=fortestingpurposes");
        response = executeRequest(httpDelete, HttpStatus.SC_FORBIDDEN);
        responseNode = readContent(response);
        closeResponse(response);

        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "message: 'Forbidden',"
                        + "exception: 'Cannot delete a task that is part of a process instance.'"
                        + "}");

        assertThat(taskService.createTaskQuery().taskId(task.getId()).singleResult()).isNotNull();

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            // Check that the historic task has been deleted and
            // delete-reason has been set
            assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNotNull();
        }
    }

    /**
     * Test updating an unexisting task. PUT runtime/tasks/{taskId}
     */
    @Test
    public void testDeleteUnexistingTask() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, "unexistingtask"));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test updating a task that is part of a process. PUT runtime/tasks/{taskId}
     */
    @Test
    @Deployment
    public void testDeleteTaskInProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, task.getId()));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_FORBIDDEN));
    }

    /**
     * Test completing a single task. POST runtime/tasks/{taskId}
     */
    @Test
    @Deployment
    public void testCompleteTask() throws Exception {
        try {

            Task task = taskService.newTask();
            taskService.saveTask(task);
            String taskId = task.getId();

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "complete");
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

            // Task should not exist anymore
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNull();

            // Test completing with variables
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskId = task.getId();

            requestNode = objectMapper.createObjectNode();
            ArrayNode variablesNode = objectMapper.createArrayNode();
            requestNode.put("action", "complete");
            requestNode.set("variables", variablesNode);

            ObjectNode var1 = objectMapper.createObjectNode();
            variablesNode.add(var1);
            var1.put("name", "var1");
            var1.put("value", "First value");
            ObjectNode var2 = objectMapper.createObjectNode();
            variablesNode.add(var2);
            var2.put("name", "var2");
            var2.put("value", "Second value");

            httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNull();

            if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
                assertThat(historicTaskInstance).isNotNull();
                List<HistoricVariableInstance> updates = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
                assertThat(updates).isNotNull();

                assertThat(updates)
                        .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                        .containsExactlyInAnyOrder(
                                tuple("var1", "First value"),
                                tuple("var2", "Second value"));
            }

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }

            // Clean historic tasks with no runtime-counterpart
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().list();
            for (HistoricTaskInstance task : historicTasks) {
                historyService.deleteHistoricTaskInstance(task.getId());
            }
        }
    }

    @Test
    @Deployment
    public void testCompleteTaskWithForm() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String taskId = task.getId();

        FormInfo formInfo = new FormInfo();
        formInfo.setId("formDefId");
        formInfo.setKey("formDefKey");
        formInfo.setName("Form Definition Name");

        when(formEngineConfiguration.getFormService()).thenReturn(formEngineFormService);
        when(formEngineFormService.getFormModelWithVariablesByKeyAndParentDeploymentId("form1", processInstance.getDeploymentId(), taskId,
                Collections.emptyMap(), task.getTenantId(), processEngineConfiguration.isFallbackToDefaultTenant()))
                .thenReturn(formInfo);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_FORM, taskId);
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: 'formDefId',"
                        + "name: 'Form Definition Name',"
                        + "key: 'formDefKey',"
                        + "type: 'taskForm',"
                        + "taskId: '" + taskId + "'"
                        + "}");

        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode variablesNode = objectMapper.createArrayNode();
        requestNode.put("action", "complete");
        requestNode.put("formDefinitionId", "formDefId");
        requestNode.set("variables", variablesNode);

        ObjectNode var1 = objectMapper.createObjectNode();
        variablesNode.add(var1);
        var1.put("name", "user");
        var1.put("value", "First value");
        ObjectNode var2 = objectMapper.createObjectNode();
        variablesNode.add(var2);
        var2.put("name", "number");
        var2.put("value", 789);

        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        when(formEngineFormService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "userTask", processInstance.getId(),
                processInstance.getProcessDefinitionId(), ScopeTypes.BPMN, formInfo, Map.of("user", "First value", "number", 789), null))
                .thenReturn(Map.of("user", "First value return", "number", 789L));

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task).isNull();

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
            assertThat(historicTaskInstance).isNotNull();
            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId())
                    .list();
            assertThat(variables).isNotNull();

            assertThat(variables)
                    .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                    .containsExactlyInAnyOrder(
                            tuple("user", "First value return"),
                            tuple("number", 789L));
        }

    }

    /**
     * Test claiming a single task and all exceptional cases related to claiming. POST runtime/tasks/{taskId}
     */
    @Test
    public void testClaimTask() throws Exception {
        try {

            Task task = taskService.newTask();
            taskService.saveTask(task);
            taskService.addCandidateUser(task.getId(), "newAssignee");

            assertThat(taskService.createTaskQuery().taskCandidateUser("newAssignee").count()).isEqualTo(1);
            // Add candidate group
            String taskId = task.getId();

            task.setAssignee("fred");
            // Claiming without assignee should set assignee to null
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "claim");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isNull();
            assertThat(taskService.createTaskQuery().taskCandidateUser("newAssignee").count()).isEqualTo(1);

            // Claim the task and check result
            requestNode.put("assignee", "newAssignee");
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isEqualTo("newAssignee");
            assertThat(taskService.createTaskQuery().taskCandidateUser("newAssignee").count()).isZero();

            // Claiming with the same user should not cause an exception
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isEqualTo("newAssignee");
            assertThat(taskService.createTaskQuery().taskCandidateUser("newAssignee").count()).isZero();

            // Claiming with another user should cause exception
            requestNode.put("assignee", "anotherUser");
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_CONFLICT));

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }

            // Clean historic tasks with no runtime-counterpart
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().list();
            for (HistoricTaskInstance task : historicTasks) {
                historyService.deleteHistoricTaskInstance(task.getId());
            }
        }
    }

    @Test
    @Deployment
    public void testReclaimTask() throws Exception {

        // Start process instance
        runtimeService.startProcessInstanceByKey("reclaimTest");

        // Get task id
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION);
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThatJson(dataNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("[ {"
                        + "      id: '${json-unit.any-string}'"
                        + "} ]");

        // Claim
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isZero();
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "claim");
        requestNode.put("assignee", "kermit");

        String taskId = ((ArrayNode) dataNode).get(0).get("id").asText();
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX +
                RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isEqualTo(1);

        // Unclaim
        requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "claim");
        httpPost = new HttpPost(SERVER_URL_PREFIX +
                RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isZero();

        // Claim again
        requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "claim");
        requestNode.put("assignee", "kermit");
        httpPost = new HttpPost(SERVER_URL_PREFIX +
                RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isEqualTo(1);
    }

    /**
     * Test delegating a single task and all exceptional cases related to delegation. POST runtime/tasks/{taskId}
     */
    @Test
    public void testDelegateTask() throws Exception {
        try {

            Task task = taskService.newTask();
            task.setAssignee("initialAssignee");
            taskService.saveTask(task);
            String taskId = task.getId();

            // Delegating without assignee fails
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "delegate");
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

            // Delegate the task and check result
            requestNode.put("assignee", "newAssignee");
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isEqualTo("newAssignee");
            assertThat(task.getOwner()).isEqualTo("initialAssignee");
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.PENDING);

            // Delegating again should not cause an exception and should delegate
            // to user without affecting initial delegator (owner)
            requestNode.put("assignee", "anotherAssignee");
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isEqualTo("anotherAssignee");
            assertThat(task.getOwner()).isEqualTo("initialAssignee");
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.PENDING);

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test resolving a single task and all exceptional cases related to resolution. POST runtime/tasks/{taskId}
     */
    @Test
    public void testResolveTask() throws Exception {
        try {
            Task task = taskService.newTask();
            task.setAssignee("initialAssignee");
            taskService.saveTask(task);
            taskService.delegateTask(task.getId(), "anotherUser");
            String taskId = task.getId();

            // Resolve the task and check result
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "resolve");
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isEqualTo("initialAssignee");
            assertThat(task.getOwner()).isEqualTo("initialAssignee");
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

            // Resolving again should not cause an exception
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isEqualTo("initialAssignee");
            assertThat(task.getOwner()).isEqualTo("initialAssignee");
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test executing an invalid action on a single task. POST runtime/tasks/{taskId}
     */
    @Test
    public void testInvalidTaskAction() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);
            String taskId = task.getId();

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "unexistingaction");
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, taskId));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }

            // Clean historic tasks with no runtime-counterpart
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().list();
            for (HistoricTaskInstance task : historicTasks) {
                historyService.deleteHistoricTaskInstance(task.getId());
            }
        }
    }

    /**
     * Test actions on an unexisting task. POST runtime/tasks/{taskId}
     */
    @Test
    public void testActionsUnexistingTask() throws Exception {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "complete");
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK, "unexisting"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

        requestNode.put("action", "claim");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

        requestNode.put("action", "delegate");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

        requestNode.put("action", "resolve");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));
    }
}
