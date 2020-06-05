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

package org.flowable.cmmn.rest.service.api.history;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Task resource.
 *
 * @author Tijs Rademakers
 */
public class HistoricTaskInstanceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single task, spawned by a case. GET cmmn-history/historic-task-instances/{taskId}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseTask() throws Exception {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            Calendar now = Calendar.getInstance();
            cmmnEngineConfiguration.getClock().setCurrentTime(now.getTime());

            CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
            Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            taskService.setDueDate(task.getId(), now.getTime());
            taskService.setOwner(task.getId(), "owner");
            task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            String url = buildUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId());
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
                            + " tenantId: \"\","
                            + " caseInstanceUrl: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, task.getScopeId()) + "',"
                            + " caseDefinitionUrl: '" + buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, task.getScopeDefinitionId()) + "',"
                            + " url: '" + url + "'"
                            + "}");

            assertThat(getDateFromISOString(responseNode.get("dueDate").asText())).isEqualTo(task.getDueDate());
            assertThat(getDateFromISOString(responseNode.get("startTime").asText())).isEqualTo(task.getCreateTime());
        }
    }

    /**
     * Test getting a single task, created using the API. GET cmmn-history/historic-task-instances/{taskId}
     */
    public void testGetTaskAdhoc() throws Exception {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
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

                String url = buildUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId());
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
                                + " priority: " + task.getPriority() + ","
                                + " caseInstanceId: null,"
                                + " caseDefinitionId: null,"
                                + " tenantId: \"\","
                                + " url: '" + url + "'"
                                + "}");

                assertThat(getDateFromISOString(responseNode.get("dueDate").asText())).isEqualTo(task.getDueDate());
                assertThat(getDateFromISOString(responseNode.get("startTime").asText())).isEqualTo(task.getCreateTime());

            } finally {

                // Clean adhoc-tasks even if test fails
                List<Task> tasks = taskService.createTaskQuery().list();
                for (Task task : tasks) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }

    /**
     * Test deleting a single task. DELETE cmmn-history/historic-task-instances/{taskId}
     */
    public void testDeleteTask() throws Exception {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            try {

                // 1. Simple delete
                Task task = taskService.newTask();
                taskService.saveTask(task);
                String taskId = task.getId();

                // Execute the request
                HttpDelete httpDelete = new HttpDelete(
                        SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE, taskId));
                closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

                assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult()).isNull();

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
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/oneHumanTaskWithFormCase.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simple.form" })
    public void testCompletedTaskForm() throws Exception {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();
            try {
                FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
                assertThat(formDefinition).isNotNull();

                CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
                Task task = taskService.createTaskQuery().scopeId(caseInstance.getId()).singleResult();
                String taskId = task.getId();

                String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE_FORM, taskId);
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

                Map<String, Object> variables = new HashMap<>();
                variables.put("user", "First value");
                variables.put("number", 789);
                taskService.completeTaskWithForm(taskId, formDefinition.getId(), null, variables);

                assertThat(taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNull();

                response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
                responseNode = objectMapper.readTree(response.getEntity().getContent());
                closeResponse(response);
                assertThatJson(responseNode)
                        .when(Option.IGNORING_EXTRA_FIELDS)
                        .isEqualTo("{"
                                + " id: '" + formDefinition.getId() + "',"
                                + " key: '" + formDefinition.getKey() + "',"
                                + " name: '" + formDefinition.getName() + "'"
                                + "}");

                assertThat(responseNode.get("fields")).hasSize(2);

                JsonNode fields = responseNode.get("fields");
                assertThatJson(fields)
                        .when(Option.IGNORING_EXTRA_FIELDS)
                        .isEqualTo("["
                                + "  {"
                                + "    id: 'user',"
                                + "    value: 'First value'"
                                + "  },"
                                + "  {"
                                + "    id: 'number',"
                                + "    value: 789"
                                + "  }"
                                + "]");

            } finally {
                formEngineFormService.deleteFormInstancesByScopeDefinition(caseDefinition.getId());

                List<FormDeployment> formDeployments = formRepositoryService.createDeploymentQuery().list();
                for (FormDeployment formDeployment : formDeployments) {
                    formRepositoryService.deleteDeployment(formDeployment.getId(), true);
                }
            }
        }
    }
}
