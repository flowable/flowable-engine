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

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.*;

/**
 * Test for all REST-operations related to the Task collection resource.
 * 
 * @author Frederik Heremans
 */
public class TaskCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test creating a task. POST runtime/tasks
     */
    @Test
    public void testCreateTask() throws Exception {
        try {
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
            requestNode.put("formKey", "testKey");
            requestNode.put("tenantId", "test");

            // Execute the request
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            String createdTaskId = responseNode.get("id").asText();

            // Check if task is created with right arguments
            Task task = taskService.createTaskQuery().taskId(createdTaskId).singleResult();
            assertEquals("New task name", task.getName());
            assertEquals("New task description", task.getDescription());
            assertEquals("assignee", task.getAssignee());
            assertEquals("owner", task.getOwner());
            assertEquals(20, task.getPriority());
            assertEquals(DelegationState.RESOLVED, task.getDelegationState());
            assertEquals(dateFormat.parse(dueDateString), task.getDueDate());
            assertEquals(parentTask.getId(), task.getParentTaskId());
            assertEquals("testKey", task.getFormKey());
            assertEquals("test", task.getTenantId());

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test creating a task. POST runtime/tasks
     */
    @Test
    public void testCreateTaskNoBody() throws Exception {
        try {
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION));
            httpPost.setEntity(null);
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
     * Test getting a collection of tasks. GET runtime/tasks
     */
    @Test
    @Deployment
    public void testGetTasks() throws Exception {
        try {
            Calendar adhocTaskCreate = Calendar.getInstance();
            adhocTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar processTaskCreate = Calendar.getInstance();
            processTaskCreate.add(Calendar.HOUR, 2);
            processTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar inBetweenTaskCreation = Calendar.getInstance();
            inBetweenTaskCreation.add(Calendar.HOUR, 1);

            processEngineConfiguration.getClock().setCurrentTime(adhocTaskCreate.getTime());
            Task adhocTask = taskService.newTask();
            adhocTask.setAssignee("gonzo");
            adhocTask.setOwner("owner");
            adhocTask.setDelegationState(DelegationState.PENDING);
            adhocTask.setDescription("Description one");
            adhocTask.setName("Name one");
            adhocTask.setDueDate(adhocTaskCreate.getTime());
            adhocTask.setPriority(100);
            adhocTask.setCategory("some-category");
            taskService.saveTask(adhocTask);
            taskService.addUserIdentityLink(adhocTask.getId(), "misspiggy", IdentityLinkType.PARTICIPANT);

            processEngineConfiguration.getClock().setCurrentTime(processTaskCreate.getTime());
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "myBusinessKey");
            Task processTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            processTask.setParentTaskId(adhocTask.getId());
            processTask.setPriority(50);
            processTask.setDueDate(processTaskCreate.getTime());
            taskService.saveTask(processTask);
            runtimeService.setVariable(processInstance.getId(), "variable", "globaltest");
            taskService.setVariableLocal(processTask.getId(), "localVariable", "localtest");

            // Check filter-less to fetch all tasks
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION);
            assertResultsPresentInDataResponse(url, adhocTask.getId(), processTask.getId());

            // Name filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?name=" + encode("Name one");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Name like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?nameLike=" + encode("%one");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Description filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?description=" + encode("Description one");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?description=" + encode("Description two");
            assertEmptyResultsPresentInDataResponse(url);

            // Description like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?descriptionLike=" + encode("%one");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?descriptionLike=" + encode("%two");
            assertEmptyResultsPresentInDataResponse(url);

            // Priority filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?priority=100";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Mininmum Priority filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?minimumPriority=70";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Maximum Priority filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?maximumPriority=70";
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Owner filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?owner=owner";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?owner=kermit";
            assertEmptyResultsPresentInDataResponse(url);

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?ownerLike=" + encode("%ner");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?ownerLike=" + encode("kerm%");
            assertEmptyResultsPresentInDataResponse(url);

            // Assignee filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?assignee=gonzo";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?assignee=kermit";
            assertEmptyResultsPresentInDataResponse(url);

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?assigneeLike=" + encode("gon%");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?assigneeLike=" + encode("kerm%");
            assertEmptyResultsPresentInDataResponse(url);

            // Unassigned filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?unassigned=true";
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Delegation state filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?delegationState=pending";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Candidate user filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateUser=kermit";
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Candidate group filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateGroup=sales";
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Involved user filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?involvedUser=misspiggy";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Process instance filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processInstanceId=" + processInstance.getId();
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Execution filtering
            Execution taskExecution = runtimeService.createExecutionQuery().activityId("processTask").singleResult();
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?executionId=" + taskExecution.getId();
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Process instance businesskey filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processInstanceBusinessKey=myBusinessKey";
            assertResultsPresentInDataResponse(url, processTask.getId());

            // CreatedOn filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdOn=" + getISODateString(adhocTaskCreate.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // CreatedAfter filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, processTask.getId());

            // CreatedBefore filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Subtask exclusion
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?excludeSubTasks=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Task definition key filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKey=processTask";
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Task definition key like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKeyLike=" + encode("process%");
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Duedate filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getISODateString(adhocTaskCreate.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Due after filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Due before filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueBefore=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Without tenantId filtering before tenant set
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId(), processTask.getId());

            // Process definition
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processDefinitionKey=" + processInstance.getProcessDefinitionKey();
            assertResultsPresentInDataResponse(url, processTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processDefinitionId=" + processInstance.getProcessDefinitionId();
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Set tenant on deployment
            managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));

            // Without tenantId filtering after tenant set, only adhoc task
            // should remain
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Tenant id filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Tenant id like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?tenantIdLike=" + encode("%enant");
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Category filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?category=" + encode("some-category");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Suspend process-instance to have a suspended task
            runtimeService.suspendProcessInstanceById(processInstance.getId());

            // Suspended filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?active=false";
            assertResultsPresentInDataResponse(url, processTask.getId());

            // Active filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?active=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId());
            
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?includeTaskLocalVariables=true";
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

            // Check status and size
            JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            Assert.assertEquals(2, dataNode.size());
            
            Map<String, JsonNode> taskNodeMap = new HashMap<>();
            for (JsonNode taskNode : dataNode) {
                taskNodeMap.put(taskNode.get("id").asText(), taskNode);
            }
            
            Assert.assertTrue(taskNodeMap.containsKey(processTask.getId()));
            JsonNode processTaskNode = taskNodeMap.get(processTask.getId());
            JsonNode variablesNode = processTaskNode.get("variables");
            assertEquals(1, variablesNode.size());
            JsonNode variableNode = variablesNode.get(0);
            assertEquals("localVariable", variableNode.get("name").asText());
            assertEquals("local", variableNode.get("scope").asText());
            assertEquals("localtest", variableNode.get("value").asText());
            
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?includeTaskLocalVariables=true&includeProcessVariables=true";
            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

            // Check status and size
            dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            Assert.assertEquals(2, dataNode.size());
            
            taskNodeMap = new HashMap<>();
            for (JsonNode taskNode : dataNode) {
                taskNodeMap.put(taskNode.get("id").asText(), taskNode);
            }
            
            Assert.assertTrue(taskNodeMap.containsKey(processTask.getId()));
            processTaskNode = taskNodeMap.get(processTask.getId());
            variablesNode = processTaskNode.get("variables");
            assertEquals(2, variablesNode.size());
            Map<String, JsonNode> variableMap = new HashMap<>();
            for (JsonNode variableResponseNode : variablesNode) {
                variableMap.put(variableResponseNode.get("name").asText(), variableResponseNode);
            }
            
            variableNode = variableMap.get("localVariable");
            assertEquals("localVariable", variableNode.get("name").asText());
            assertEquals("local", variableNode.get("scope").asText());
            assertEquals("localtest", variableNode.get("value").asText());
            
            variableNode = variableMap.get("variable");
            assertEquals("variable", variableNode.get("name").asText());
            assertEquals("global", variableNode.get("scope").asText());
            assertEquals("globaltest", variableNode.get("value").asText());

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                if (task.getExecutionId() == null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }
}
