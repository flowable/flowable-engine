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
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormService;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Historic task instance resource.
 */
@MockitoSettings
public class HistoricTaskInstanceResourceTest extends BaseSpringRestTestCase {

    @Mock
    protected FormEngineConfigurationApi formEngineConfiguration;

    @Mock
    protected FormService formEngineFormService;

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
     * Test getting a single task, spawned by a process. GET history/historic-task-instances/{taskId}
     */
    @Test
    @Deployment
    public void testGetProcessTask() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            Date now = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
            processEngineConfiguration.getClock().setCurrentTime(now);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.setDueDate(task.getId(), now);
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
                            + " dueDate: '" + getISODateString(task.getDueDate()) + "',"
                            + " startTime: '" + getISODateString(task.getCreateTime()) + "',"
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

                Date now = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
                processEngineConfiguration.getClock().setCurrentTime(now);

                Task parentTask = taskService.newTask();
                taskService.saveTask(parentTask);

                Task task = taskService.newTask();
                task.setParentTaskId(parentTask.getId());
                task.setName("Task name");
                task.setDescription("Descriptions");
                task.setAssignee("kermit");
                task.setDelegationState(DelegationState.RESOLVED);
                task.setDescription("Description");
                task.setDueDate(now);
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
                                + " dueDate: '" + getISODateString(task.getDueDate()) + "',"
                                + " startTime: '" + getISODateString(task.getCreateTime()) + "',"
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
                taskService.complete(taskId);

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
            taskService.complete(task.getId());

            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId()));
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult()).isNull();
        }
    }

    @Test
    @Deployment
    public void testCompletedTaskForm() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
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

            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_FORM, taskId);
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                    .isEqualTo("{"
                            + "id: 'formDefId',"
                            + "name: 'Form Definition Name',"
                            + "key: 'formDefKey',"
                            + "type: 'historicTaskForm',"
                            + "historicTaskId: '" + taskId +"'"
                            + "}");
        }
    }
}
