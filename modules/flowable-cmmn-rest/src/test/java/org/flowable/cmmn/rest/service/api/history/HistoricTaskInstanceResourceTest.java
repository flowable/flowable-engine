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
            assertNotNull(task);
    
            String url = buildUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);
    
            // Check resulting task
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertEquals(task.getId(), responseNode.get("id").asText());
            assertEquals(task.getAssignee(), responseNode.get("assignee").asText());
            assertEquals(task.getOwner(), responseNode.get("owner").asText());
            assertEquals(task.getFormKey(), responseNode.get("formKey").asText());
            assertEquals(task.getDescription(), responseNode.get("description").asText());
            assertEquals(task.getName(), responseNode.get("name").asText());
            assertEquals(task.getDueDate(), getDateFromISOString(responseNode.get("dueDate").asText()));
            assertEquals(task.getCreateTime(), getDateFromISOString(responseNode.get("startTime").asText()));
            assertEquals(task.getPriority(), responseNode.get("priority").asInt());
            assertTrue(responseNode.get("parentTaskId").isNull());
            assertEquals("", responseNode.get("tenantId").textValue());
    
            assertEquals(responseNode.get("caseInstanceUrl").asText(), buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, task.getScopeId()));
            assertEquals(responseNode.get("caseDefinitionUrl").asText(), buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, task.getScopeDefinitionId()));
            assertEquals(responseNode.get("url").asText(), url);
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
                assertEquals(task.getId(), responseNode.get("id").asText());
                assertEquals(task.getAssignee(), responseNode.get("assignee").asText());
                assertEquals(task.getOwner(), responseNode.get("owner").asText());
                assertEquals(task.getDescription(), responseNode.get("description").asText());
                assertEquals(task.getName(), responseNode.get("name").asText());
                assertEquals(task.getDueDate(), getDateFromISOString(responseNode.get("dueDate").asText()));
                assertEquals(task.getCreateTime(), getDateFromISOString(responseNode.get("startTime").asText()));
                assertEquals(task.getPriority(), responseNode.get("priority").asInt());
                assertTrue(responseNode.get("caseInstanceId").isNull());
                assertTrue(responseNode.get("caseDefinitionId").isNull());
                assertEquals("", responseNode.get("tenantId").textValue());
    
                assertEquals(responseNode.get("url").asText(), url);
    
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
                HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE, taskId));
                closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));
    
                assertNull(historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult());
    
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
                    "org/flowable/cmmn/rest/service/api/runtime/simple.form"})
    public void testCompletedTaskForm() throws Exception {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();
            try {
                FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
                assertNotNull(formDefinition);
                
                CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
                Task task = taskService.createTaskQuery().scopeId(caseInstance.getId()).singleResult();
                String taskId = task.getId();
                
                String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE_FORM, taskId);
                CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
                JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
                closeResponse(response);
                assertEquals(formDefinition.getId(), responseNode.get("id").asText());
                assertEquals(formDefinition.getKey(), responseNode.get("key").asText());
                assertEquals(formDefinition.getName(), responseNode.get("name").asText());
                assertEquals(2, responseNode.get("fields").size());
                
                Map<String, Object> variables = new HashMap<>();
                variables.put("user", "First value");
                variables.put("number", 789);
                taskService.completeTaskWithForm(taskId, formDefinition.getId(), null, variables);
                
                assertNull(taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult());
    
                response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
                responseNode = objectMapper.readTree(response.getEntity().getContent());
                closeResponse(response);
                assertEquals(formDefinition.getId(), responseNode.get("id").asText());
                assertEquals(formDefinition.getKey(), responseNode.get("key").asText());
                assertEquals(formDefinition.getName(), responseNode.get("name").asText());
                assertEquals(2, responseNode.get("fields").size());
                
                JsonNode fieldNode = responseNode.get("fields").get(0);
                assertEquals("user", fieldNode.get("id").asText());
                assertEquals("First value", fieldNode.get("value").asText());
                
                fieldNode = responseNode.get("fields").get(1);
                assertEquals("number", fieldNode.get("id").asText());
                assertEquals(789, fieldNode.get("value").asInt());
    
            } finally {
                formEngineFormService.deleteFormInstancesByScopeDefinition(caseDefinition.getId());
                
                List<FormDeployment> formDeployments = formRepositoryService.createDeploymentQuery().list();
                for (FormDeployment formDeployment : formDeployments) {
                    formRepositoryService.deleteDeployment(formDeployment.getId());
                }
            }
        }
    }
}
