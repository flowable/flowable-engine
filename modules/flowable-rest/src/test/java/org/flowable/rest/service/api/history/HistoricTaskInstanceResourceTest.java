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

package org.flowable.rest.service.api.history;

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
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Historic task instance resource.
 */
public class HistoricTaskInstanceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single task, spawned by a process. GET history/historic-task-instances/{taskId}
     */
    @Test
    @Deployment
    public void testGetProcessTask() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            Calendar now = Calendar.getInstance();
            processEngineConfiguration.getClock().setCurrentTime(now.getTime());

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.setDueDate(task.getId(), now.getTime());
            taskService.setOwner(task.getId(), "owner");
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            String url = buildUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId());
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
                            + " executionId: '" + task.getExecutionId() + "',"
                            + " description: '" + task.getDescription() + "',"
                            + " name: '" + task.getName() + "',"
                            + " dueDate: " + new TextNode(getISODateStringWithTZ(task.getDueDate())) + ","
                            + " startTime: " + new TextNode(getISODateStringWithTZ(task.getCreateTime())) + ","
                            + " priority: " + task.getPriority() + ","
                            + " endTime: null,"
                            + " parentTaskId: null,"
                            + " tenantId: \"\","
                            + " processInstanceUrl: '" + buildUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE, task.getProcessInstanceId()) + "',"
                            + " processDefinitionUrl: '" + buildUrl(RestUrls.URL_PROCESS_DEFINITION, task.getProcessDefinitionId()) + "',"
                            + " url: '" + url + "'"
                            + "}");
        }
    }

    /**
     * Test getting a single task, created using the API. GET history/historic-task-instances/{taskId}
     */
    @Test
    public void testGetProcessAdhoc() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            try {

                Calendar now = Calendar.getInstance();
                processEngineConfiguration.getClock().setCurrentTime(now.getTime());

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

                String url = buildUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId());
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
                                + " startTime: " + new TextNode(getISODateStringWithTZ(task.getCreateTime())) + ","
                                + " priority: " + task.getPriority() + ","
                                + " parentTaskId: '" + task.getParentTaskId() + "',"
                                + " executionId: null,"
                                + " processInstanceId: null,"
                                + " processDefinitionId: null,"
                                + " tenantId: \"\","
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
    }

    /**
     * Test deleting a single task. DELETE history/historic-task-instances/{taskId}
     */
    @Test
    public void testDeleteTask() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            try {
                // 1. Simple delete
                Task task = taskService.newTask();
                taskService.saveTask(task);
                String taskId = task.getId();

                // Execute the request
                HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, taskId));
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

    /**
     * Test deleting a task that is part of a process. DELETE history/historic-task-instances/{taskId}
     */
    @Test
    @Deployment
    public void testDeleteTaskInProcess() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId()));
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult()).isNull();
        }
    }

    @Test
    @Deployment
    public void testCompletedTaskForm() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();
            try {
                formRepositoryService.createDeployment().addClasspathResource("org/flowable/rest/service/api/runtime/simple.form").deploy();

                FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
                assertThat(formDefinition).isNotNull();

                ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
                Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
                String taskId = task.getId();

                String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_FORM, taskId);
                CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
                JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
                closeResponse(response);
                assertThatJson(responseNode)
                        .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                        .isEqualTo("{"
                                + "id: '" + formDefinition.getId() + "',"
                                + "name: '" + formDefinition.getName() + "',"
                                + "key: '" + formDefinition.getKey() + "',"
                                + "fields: [ {  },"
                                + "          {  }"
                                + "        ]"
                                + "}");

                Map<String, Object> variables = new HashMap<>();
                variables.put("user", "First value");
                variables.put("number", 789);
                taskService.completeTaskWithForm(taskId, formDefinition.getId(), null, variables);

                assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult()).isNull();

                response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
                responseNode = objectMapper.readTree(response.getEntity().getContent());
                closeResponse(response);
                assertThatJson(responseNode)
                        .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                        .isEqualTo("{"
                                + "id: '" + formDefinition.getId() + "',"
                                + "name: '" + formDefinition.getName() + "',"
                                + "key: '" + formDefinition.getKey() + "',"
                                + "fields: [ { "
                                + "            id: 'user',"
                                + "            value: 'First value'"
                                + "          },"
                                + "          { "
                                + "            id: 'number',"
                                + "            value: '789'"
                                + "          }"
                                + "        ]"
                                + "}");

            } finally {
                formEngineFormService.deleteFormInstancesByProcessDefinition(processDefinition.getId());

                List<FormDeployment> formDeployments = formRepositoryService.createDeploymentQuery().list();
                for (FormDeployment formDeployment : formDeployments) {
                    formRepositoryService.deleteDeployment(formDeployment.getId(), true);
                }
            }
        }
    }
}
