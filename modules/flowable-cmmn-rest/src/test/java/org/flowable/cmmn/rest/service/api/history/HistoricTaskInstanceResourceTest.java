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
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Collections;
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
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormService;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Task resource.
 *
 * @author Tijs Rademakers
 */
public class HistoricTaskInstanceResourceTest extends BaseSpringRestTestCase {

    @Mock
    protected FormEngineConfigurationApi formEngineConfiguration;

    @Mock
    protected FormService formEngineFormService;

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
                taskService.complete(taskId);

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

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/oneHumanTaskWithFormCase.cmmn" })
    public void testCompletedTaskForm() throws Exception {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            runUsingMocks(() -> {
                Map engineConfigurations = cmmnEngineConfiguration.getEngineConfigurations();
                engineConfigurations.put(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration);

                CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();
                CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
                Task task = taskService.createTaskQuery().scopeId(caseInstance.getId()).singleResult();
                String taskId = task.getId();

                FormInfo formInfo = new FormInfo();
                formInfo.setId("formDefId");
                formInfo.setKey("formDefKey");
                formInfo.setName("Form Definition Name");

                when(formEngineConfiguration.getFormService()).thenReturn(formEngineFormService);
                when(formEngineFormService.getFormModelWithVariablesByKeyAndParentDeploymentId("form1", caseDefinition.getDeploymentId(), taskId,
                        Collections.emptyMap(), task.getTenantId(), cmmnEngineConfiguration.isFallbackToDefaultTenant()))
                        .thenReturn(formInfo);

                String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE_FORM, taskId);
                CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
                JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
                closeResponse(response);
                assertThatJson(responseNode)
                        .when(Option.IGNORING_EXTRA_FIELDS)
                        .isEqualTo("{"
                                + "  id: 'formDefId',"
                                + "  name: 'Form Definition Name',"
                                + "  key: 'formDefKey',"
                                + "  type: 'historicTaskForm',"
                                + "  historicTaskId: '" + taskId +"'"
                                + "}");
            });

        }
    }
}
