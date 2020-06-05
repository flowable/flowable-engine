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
import static org.assertj.core.api.Assertions.tuple;

import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormInstance;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Task resource.
 *
 * @author Tijs Rademakers
 */
public class TaskResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single task, spawned by a case. GET cmmn-runtime/tasks/{taskId}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseTask() throws Exception {
        Calendar now = Calendar.getInstance();
        cmmnEngineConfiguration.getClock().setCurrentTime(now.getTime());

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.setDueDate(task.getId(), now.getTime());
        taskService.setOwner(task.getId(), "owner");
        task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        String url = buildUrl(CmmnRestUrls.URL_TASK, task.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        // Check resulting task
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + task.getId() + "',"
                        + " assignee: '" + task.getAssignee() + "',"
                        + " owner: '" + task.getOwner() + "',"
                        + " formKey: '" + task.getFormKey() + "',"
                        + " description: '" + task.getDescription() + "',"
                        + " name: '" + task.getName() + "',"
                        + " priority: " + task.getPriority() + ","
                        + " parentTaskId: null,"
                        + " delegationState: null,"
                        + " tenantId: \"\","
                        + " dueDate: " + new TextNode(getISODateStringWithTZ(task.getDueDate())) + ","
                        + " createTime: " + new TextNode(getISODateStringWithTZ(task.getCreateTime())) + ","
                        + " caseInstanceUrl: '" + buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, task.getScopeId()) + "',"
                        + " caseDefinitionUrl: '" + buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, task.getScopeDefinitionId()) + "',"
                        + " url: '" + url + "'"
                        + "}");
    }

    /**
     * Test getting a single task, created using the API. GET runtime/tasks/{taskId}
     */
    public void testGetProcessAdhoc() throws Exception {
        try {

            Calendar now = Calendar.getInstance();
            cmmnEngineConfiguration.getClock().setCurrentTime(now.getTime());

            Task parentTask = taskService.newTask();
            taskService.saveTask(parentTask);

            Task task = taskService.newTask();
            task.setParentTaskId(parentTask.getId());
            task.setName("Task name");
            task.setDescription("Descriptions");
            task.setAssignee("kermit");
            task.setDelegationState(DelegationState.RESOLVED);
            task.setDescription("Description");
            task.setDueDate(now.getTime());
            task.setOwner("owner");
            task.setPriority(20);
            taskService.saveTask(task);

            String url = buildUrl(CmmnRestUrls.URL_TASK, task.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

            // Check resulting task
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + " id: '" + task.getId() + "',"
                            + " assignee: '" + task.getAssignee() + "',"
                            + " owner: '" + task.getOwner() + "',"
                            + " description: '" + task.getDescription() + "',"
                            + " name: '" + task.getName() + "',"
                            + " dueDate: " + new TextNode(getISODateStringWithTZ(task.getDueDate())) + ","
                            + " createTime: " + new TextNode(getISODateStringWithTZ(task.getCreateTime())) + ","
                            + " priority: " + task.getPriority() + ","
                            + " delegationState: 'resolved',"
                            + " caseInstanceId: null,"
                            + " caseDefinitionId: null,"
                            + " parentTaskUrl: '" + buildUrl(CmmnRestUrls.URL_TASK, parentTask.getId()) + "',"
                            + " url: '" + url + "'"
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
    public void testUpdateTaskNoOverrides() throws Exception {
        try {
            Calendar now = Calendar.getInstance();
            Task parentTask = taskService.newTask();
            taskService.saveTask(parentTask);

            Task task = taskService.newTask();
            task.setParentTaskId(parentTask.getId());
            task.setName("Task name");
            task.setDescription("Description");
            task.setAssignee("kermit");
            task.setDelegationState(DelegationState.RESOLVED);
            task.setDescription("Description");
            task.setDueDate(now.getTime());
            task.setOwner("owner");
            task.setPriority(20);
            taskService.saveTask(task);

            ObjectNode requestNode = objectMapper.createObjectNode();

            // Execute the request with an empty request JSON-object
            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, task.getId()));
            httpPut.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("Task name");
            assertThat(task.getDescription()).isEqualTo("Description");
            assertThat(task.getAssignee()).isEqualTo("kermit");
            assertThat(task.getOwner()).isEqualTo("owner");
            assertThat(task.getPriority()).isEqualTo(20);
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);
            assertThat(task.getDueDate()).isEqualTo(now.getTime());
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
    public void testUpdateTask() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            Task parentTask = taskService.newTask();
            taskService.saveTask(parentTask);

            ObjectNode requestNode = objectMapper.createObjectNode();

            Calendar dueDate = Calendar.getInstance();
            String dueDateString = getISODateString(dueDate.getTime());

            requestNode.put("name", "New task name");
            requestNode.put("description", "New task description");
            requestNode.put("assignee", "assignee");
            requestNode.put("owner", "owner");
            requestNode.put("priority", 20);
            requestNode.put("delegationState", "resolved");
            requestNode.put("dueDate", dueDateString);
            requestNode.put("parentTaskId", parentTask.getId());

            // Execute the request
            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, task.getId()));
            httpPut.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task.getName()).isEqualTo("New task name");
            assertThat(task.getDescription()).isEqualTo("New task description");
            assertThat(task.getAssignee()).isEqualTo("assignee");
            assertThat(task.getOwner()).isEqualTo("owner");
            assertThat(task.getPriority()).isEqualTo(20);
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);
            assertThat(task.getDueDate()).isEqualTo(longDateFormat.parse(dueDateString));
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
    public void testUpdateUnexistingTask() throws Exception {
        ObjectNode requestNode = objectMapper.createObjectNode();

        // Execute the request with an empty request JSON-object
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, "unexistingtask"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single task. DELETE runtime/tasks/{taskId}
     */
    public void testDeleteTask() throws Exception {
        try {

            // 1. Simple delete
            Task task = taskService.newTask();
            taskService.saveTask(task);
            String taskId = task.getId();

            // Execute the request
            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task).isNull();

            if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                // Check that the historic task has not been deleted
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNotNull();
            }

            // 2. Cascade delete
            task = taskService.newTask();
            taskService.saveTask(task);
            taskId = task.getId();

            // Execute the request
            httpDelete = new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId) + "?cascadeHistory=true");
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task).isNull();

            if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                // Check that the historic task has been deleted
                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNull();
            }

            // 3. Delete with reason
            task = taskService.newTask();
            taskService.saveTask(task);
            taskId = task.getId();

            // Execute the request
            httpDelete = new HttpDelete(
                    SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId) + "?deleteReason=fortestingpurposes");
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(task).isNull();

            if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
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
     * Test updating an unexisting task. PUT runtime/tasks/{taskId}
     */
    public void testDeleteUnexistingTask() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, "unexistingtask"));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test updating a task that is part of a case. PUT cmmn-runtime/tasks/{taskId}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDeleteTaskInCase() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, task.getId()));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_FORBIDDEN));
    }

    /**
     * Test completing a single task. POST cmmn-runtime/tasks/{taskId}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCompleteTask() throws Exception {
        try {

            Task task = taskService.newTask();
            taskService.saveTask(task);
            String taskId = task.getId();

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "complete");
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

            // Task shouldn't exist anymore
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNull();

            // Test completing with variables
            CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
            task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
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

            httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNull();

            if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
                assertThat(historicTaskInstance).isNotNull();
                List<HistoricVariableInstance> updates = historyService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list();
                assertThat(updates).isNotNull();
                assertThat(updates)
                        .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                        .containsExactlyInAnyOrder(
                                tuple("var1", "First value"),
                                tuple("var2", "Second value")
                        );
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

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/oneHumanTaskWithFormCase.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simple.form" })
    public void testCompleteTaskWithForm() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();
        try {
            FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
            assertThat(formDefinition).isNotNull();

            CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
            Task task = taskService.createTaskQuery().scopeId(caseInstance.getId()).singleResult();
            String taskId = task.getId();

            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_FORM, taskId);
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + " id: '" + formDefinition.getId() + "',"
                            + " key: '" + formDefinition.getKey() + "',"
                            + " name: '" + formDefinition.getName() + "'"
                            + "}");
            assertThat(responseNode.get("fields")).hasSize(2);

            ObjectNode requestNode = objectMapper.createObjectNode();
            ArrayNode variablesNode = objectMapper.createArrayNode();
            requestNode.put("action", "complete");
            requestNode.put("formDefinitionId", formDefinition.getId());
            requestNode.set("variables", variablesNode);

            ObjectNode var1 = objectMapper.createObjectNode();
            variablesNode.add(var1);
            var1.put("name", "user");
            var1.put("value", "First value");
            ObjectNode var2 = objectMapper.createObjectNode();
            variablesNode.add(var2);
            var2.put("name", "number");
            var2.put("value", 789);

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNull();

            FormInstance formInstance = formEngineFormService.createFormInstanceQuery().taskId(taskId).singleResult();
            assertThat(formInstance).isNotNull();
            byte[] valuesBytes = formEngineFormService.getFormInstanceValues(formInstance.getId());
            assertThat(valuesBytes).isNotNull();
            JsonNode instanceNode = objectMapper.readTree(valuesBytes);
            JsonNode valuesNode = instanceNode.get("values");
            assertThatJson(valuesNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + " user: 'First value',"
                            + " number: '789'"
                            + "}");

            if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
                assertThat(historicTaskInstance).isNotNull();
                List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list();
                assertThat(variables).isNotNull();
                assertThat(variables)
                        .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                        .containsExactlyInAnyOrder(
                                tuple("user", "First value"),
                                tuple("number", 789)
                        );
            }

        } finally {
            formEngineFormService.deleteFormInstancesByScopeDefinition(caseDefinition.getId());

            List<FormDeployment> formDeployments = formRepositoryService.createDeploymentQuery().list();
            for (FormDeployment formDeployment : formDeployments) {
                formRepositoryService.deleteDeployment(formDeployment.getId(), true);
            }
        }
    }

    /**
     * Test claiming a single task and all exceptional cases related to claiming. POST runtime/tasks/{taskId}
     */
    public void testClaimTask() throws Exception {
        try {

            Task task = taskService.newTask();
            taskService.saveTask(task);
            taskService.addUserIdentityLink(task.getId(), "newAssignee", IdentityLinkType.CANDIDATE);

            assertThat(taskService.createTaskQuery().taskCandidateUser("newAssignee").count()).isEqualTo(1);
            // Add candidate group
            String taskId = task.getId();

            task.setAssignee("fred");
            // Claiming without assignee should set assignee to null
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "claim");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
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

            // Claiming with the same user shouldn't cause an exception
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

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testReclaimTask() throws Exception {

        // Start case instance
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.unclaim(task.getId());

        // Get task id
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION);
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        String taskId = ((ArrayNode) dataNode).get(0).get("id").asText();
        assertThat(taskId).isNotNull();

        // Claim
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isZero();
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "claim");
        requestNode.put("assignee", "kermit");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX +
                CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isEqualTo(1);

        // Unclaim
        requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "claim");
        httpPost = new HttpPost(SERVER_URL_PREFIX +
                CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isZero();

        // Claim again
        requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "claim");
        requestNode.put("assignee", "kermit");
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isEqualTo(1);
    }

    /**
     * Test delegating a single task and all exceptional cases related to delegation. POST runtime/tasks/{taskId}
     */
    public void testDelegateTask() throws Exception {
        try {

            Task task = taskService.newTask();
            task.setAssignee("initialAssignee");
            taskService.saveTask(task);
            String taskId = task.getId();

            // Delegating without assignee fails
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "delegate");
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
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

            // Delegating again shouldn't cause an exception and should delegate
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
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));

            task = taskService.createTaskQuery().taskId(taskId).singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isEqualTo("initialAssignee");
            assertThat(task.getOwner()).isEqualTo("initialAssignee");
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

            // Resolving again shouldn't cause an exception
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
    public void testInvalidTaskAction() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);
            String taskId = task.getId();

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("action", "unexistingaction");
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, taskId));
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
    public void testActionsUnexistingTask() throws Exception {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "complete");
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK, "unexisting"));
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
