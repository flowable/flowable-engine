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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
            assertNotNull(task);
    
            String url = buildUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId());
            CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);
    
            // Check resulting task
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertEquals(task.getId(), responseNode.get("id").asText());
            assertEquals(task.getAssignee(), responseNode.get("assignee").asText());
            assertEquals(task.getOwner(), responseNode.get("owner").asText());
            assertEquals(task.getFormKey(), responseNode.get("formKey").asText());
            assertEquals(task.getExecutionId(), responseNode.get("executionId").asText());
            assertEquals(task.getDescription(), responseNode.get("description").asText());
            assertEquals(task.getName(), responseNode.get("name").asText());
            assertEquals(task.getDueDate(), getDateFromISOString(responseNode.get("dueDate").asText()));
            assertEquals(task.getCreateTime(), getDateFromISOString(responseNode.get("startTime").asText()));
            assertEquals(task.getPriority(), responseNode.get("priority").asInt());
            assertTrue(responseNode.get("endTime").isNull());
            assertTrue(responseNode.get("parentTaskId").isNull());
            assertEquals("", responseNode.get("tenantId").textValue());
    
            assertEquals(buildUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE, task.getProcessInstanceId()), responseNode.get("processInstanceUrl").asText());
            assertEquals(buildUrl(RestUrls.URL_PROCESS_DEFINITION, task.getProcessDefinitionId()), responseNode.get("processDefinitionUrl").asText());
            assertEquals(responseNode.get("url").asText(), url);
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
                assertEquals(task.getId(), responseNode.get("id").asText());
                assertEquals(task.getAssignee(), responseNode.get("assignee").asText());
                assertEquals(task.getOwner(), responseNode.get("owner").asText());
                assertEquals(task.getDescription(), responseNode.get("description").asText());
                assertEquals(task.getName(), responseNode.get("name").asText());
                assertEquals(task.getDueDate(), getDateFromISOString(responseNode.get("dueDate").asText()));
                assertEquals(task.getCreateTime(), getDateFromISOString(responseNode.get("startTime").asText()));
                assertEquals(task.getPriority(), responseNode.get("priority").asInt());
                assertEquals(task.getParentTaskId(), responseNode.get("parentTaskId").asText());
                assertTrue(responseNode.get("executionId").isNull());
                assertTrue(responseNode.get("processInstanceId").isNull());
                assertTrue(responseNode.get("processDefinitionId").isNull());
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

    /**
     * Test deleting a task that is part of a process. DELETE history/historic-task-instances/{taskId}
     */
    @Test
    @Deployment
    public void testDeleteTaskInProcess() throws Exception {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertNotNull(task);
    
            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE, task.getId()));
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));
            
            assertNull(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult());
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
                assertNotNull(formDefinition);
                
                ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
                Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
                String taskId = task.getId();
                
                String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_FORM, taskId);
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
                
                assertNull(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());
    
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
                formEngineFormService.deleteFormInstancesByProcessDefinition(processDefinition.getId());
                
                List<FormDeployment> formDeployments = formRepositoryService.createDeploymentQuery().list();
                for (FormDeployment formDeployment : formDeployments) {
                    formRepositoryService.deleteDeployment(formDeployment.getId());
                }
            }
        }
    }
}
