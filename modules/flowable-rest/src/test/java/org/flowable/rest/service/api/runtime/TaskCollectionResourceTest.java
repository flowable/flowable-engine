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

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import org.junit.jupiter.api.Test;

import net.javacrumbs.jsonunit.core.Option;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to the Task collection resource.
 *
 * @author Frederik Heremans
 * @author Christopher Welsch
 */
public class TaskCollectionResourceTest extends BaseSpringRestTestCase {
    
    protected ProcessInstance preparedProcessInstance;
    protected Task preparedAdhocTask;
    protected Task preparedProcessTask;
    
    protected Calendar inBetweenTaskCreation;
    protected Calendar adhocTaskCreateTime;

    /**
     * Test creating a task. POST runtime/tasks
     */
    @Test
    public void testCreateTask() throws Exception {
        try {
            Task parentTask = taskService.newTask();
            taskService.saveTask(parentTask);

            ObjectNode requestNode = objectMapper.createObjectNode();

            // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
            String dueDateString = Instant.now().with(ChronoField.MILLI_OF_SECOND, 83).toString();

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
            String createdTaskId = responseNode.get("id").asString();

            // Check if task is created with right arguments
            Task task = taskService.createTaskQuery().taskId(createdTaskId).singleResult();
            assertThat(task.getName()).isEqualTo("New task name");
            assertThat(task.getDescription()).isEqualTo("New task description");
            assertThat(task.getAssignee()).isEqualTo("assignee");
            assertThat(task.getOwner()).isEqualTo("owner");
            assertThat(task.getPriority()).isEqualTo(20);
            assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);
            assertThat(task.getDueDate()).isEqualTo(getDateFromISOString(dueDateString));
            assertThat(task.getParentTaskId()).isEqualTo(parentTask.getId());
            assertThat(task.getFormKey()).isEqualTo("testKey");
            assertThat(task.getTenantId()).isEqualTo("test");

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
            
            prepareTasks();
            
            // Check filter-less to fetch all tasks
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION);
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId(), preparedProcessTask.getId());

            // ID filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?taskId=" + encode(preparedAdhocTask.getId());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Name filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?name=" + encode("Name one");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Name like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?nameLike=" + encode("%one");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());
            
            // Name like ignore case filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?nameLikeIgnoreCase=" + encode("%ONE");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Description filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?description=" + encode("Description one");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?description=" + encode("Description two");
            assertEmptyResultsPresentInDataResponse(url);

            // Description like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?descriptionLike=" + encode("%one");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?descriptionLike=" + encode("%two");
            assertEmptyResultsPresentInDataResponse(url);

            // Priority filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?priority=100";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Minimum Priority filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?minimumPriority=70";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Maximum Priority filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?maximumPriority=70";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Without tenantId filtering before tenant set
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId(), preparedProcessTask.getId());

            // Process definition
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processDefinitionKey=" + preparedProcessInstance.getProcessDefinitionKey();
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processDefinitionId=" + preparedProcessInstance.getProcessDefinitionId();
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Set tenant on deployment
            managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));

            // Without tenantId filtering after tenant set, only adhoc task
            // should remain
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Tenant id filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Tenant id like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?tenantIdLike=" + encode("%enant");
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Category filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?category=" + encode("some-category");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?categoryIn=" + encode("some-other-category,some-category");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?categoryNotIn=" + encode("some-category");
            assertResultsPresentInDataResponse(url);

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutCategory=true";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Suspend process-instance to have a suspended task
            runtimeService.suspendProcessInstanceById(preparedProcessInstance.getId());

            // Suspended filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?active=false";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Active filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?active=true";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?includeTaskLocalVariables=true";
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
            
            // Without scope id
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutScopeId=true";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId(), preparedAdhocTask.getId());

            // Check status and size
            JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThat(dataNode).hasSize(2);

            Map<String, JsonNode> taskNodeMap = new HashMap<>();
            for (JsonNode taskNode : dataNode) {
                taskNodeMap.put(taskNode.get("id").asString(), taskNode);
            }

            assertThat(taskNodeMap).containsKey(preparedProcessTask.getId());

            JsonNode processTaskNode = taskNodeMap.get(preparedProcessTask.getId());
            assertThatJson(processTaskNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "variables: [ {"
                            + "name: 'localVariable',"
                            + "value: 'localtest',"
                            + "scope: 'local'"
                            + "} ]"
                            + "}");

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?includeTaskLocalVariables=true&includeProcessVariables=true";
            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

            // Check status and size
            dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThat(dataNode).hasSize(2);

            taskNodeMap = new HashMap<>();
            for (JsonNode taskNode : dataNode) {
                taskNodeMap.put(taskNode.get("id").asString(), taskNode);
            }

            assertThat(taskNodeMap).containsKey(preparedProcessTask.getId());
            processTaskNode = taskNodeMap.get(preparedProcessTask.getId());
            assertThatJson(processTaskNode)
                    .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                    .isEqualTo("{"
                            + "variables: [ {"
                            + "    name: 'variable',"
                            + "    value: 'globaltest',"
                            + "    scope: 'global'"
                            + "  }, {"
                            + "    name: 'localVariable',"
                            + "    value: 'localtest',"
                            + "    scope: 'local'"
                            + "  } ]"
                            + "}");

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
    
    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/TaskCollectionResourceTest.testGetTasks.bpmn20.xml")
    public void testGetTasksIdentityInfo() throws Exception {
        try {
            
            prepareTasks();
            
            // Owner filtering
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?owner=owner";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?owner=kermit";
            assertEmptyResultsPresentInDataResponse(url);

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?ownerLike=" + encode("%ner");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?ownerLike=" + encode("kerm%");
            assertEmptyResultsPresentInDataResponse(url);

            // Assignee filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?assignee=gonzo";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?assignee=kermit";
            assertEmptyResultsPresentInDataResponse(url);

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?assigneeLike=" + encode("gon%");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?assigneeLike=" + encode("kerm%");
            assertEmptyResultsPresentInDataResponse(url);

            // Unassigned filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?unassigned=true";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Delegation state filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?delegationState=pending";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Candidate user filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateUser=kermit";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateUser=notExisting";
            assertEmptyResultsPresentInDataResponse(url);

            // Candidate group filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateGroup=sales";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateGroup=notExisting";
            assertEmptyResultsPresentInDataResponse(url);
            
            // Candidate user with group filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateUser=aSalesUser";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateUser=notExisting";
            assertEmptyResultsPresentInDataResponse(url);

            // Involved user filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?involvedUser=misspiggy";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Claim task
            taskService.claim(preparedProcessTask.getId(), "johnDoe");

            // IgnoreAssignee
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateGroup=sales&ignoreAssignee=true";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateGroup=notExisting&ignoreAssignee";
            assertEmptyResultsPresentInDataResponse(url);

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
    
    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/TaskCollectionResourceTest.testGetTasks.bpmn20.xml")
    public void testGetTasksScopeInfo() throws Exception {
        try {
            
            prepareTasks();
            
            // Process instance filtering
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processInstanceId=" + preparedProcessInstance.getId();
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());
            
            // Process instance with children filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processInstanceIdWithChildren=" + preparedProcessInstance.getId();
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());
            
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processInstanceIdWithChildren=nonexisting";
            assertResultsPresentInDataResponse(url);
            
            // Without process instance id
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutProcessInstanceId=true";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());
            
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutProcessInstanceId=false";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId(), preparedAdhocTask.getId());

            // Execution filtering
            Execution taskExecution = runtimeService.createExecutionQuery().activityId("processTask").singleResult();
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?executionId=" + taskExecution.getId();
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Process instance businesskey filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?processInstanceBusinessKey=myBusinessKey";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

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
    
    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/TaskCollectionResourceTest.testGetTasks.bpmn20.xml")
    public void testGetTasksDateInfo() throws Exception {
        try {
            
            prepareTasks();
            
            // CreatedOn filtering
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdOn=" + getISODateString(adhocTaskCreateTime.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // CreatedAfter filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // CreatedBefore filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Subtask exclusion
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?excludeSubTasks=true";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Task definition key filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKey=processTask";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Task definition key like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKeyLike=" + encode("process%");
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Task definition keys filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKeys=processTask,invalidTask";
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Duedate filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getISODateString(adhocTaskCreateTime.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Due after filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedProcessTask.getId());

            // Due before filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueBefore=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

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
    
    @Test
    public void testBulkUpdateTaskAssignee() throws IOException {

        taskService.createTaskBuilder().id("taskID1").create();
        taskService.createTaskBuilder().id("taskID2").create();
        taskService.createTaskBuilder().id("taskID3").create();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("assignee", "admin");
        requestNode.putArray("taskIds").add("taskID1").add("taskID3");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPut, HttpStatus.SC_OK);

        assertThat(taskService.createTaskQuery().taskId("taskID1").singleResult().getAssignee()).isEqualTo("admin");
        assertThat(taskService.createTaskQuery().taskId("taskID2").singleResult().getAssignee()).isNull();
        assertThat(taskService.createTaskQuery().taskId("taskID3").singleResult().getAssignee()).isEqualTo("admin");

        taskService.deleteTask("taskID1", true);
        taskService.deleteTask("taskID2", true);
        taskService.deleteTask("taskID3", true);

    }
    @Test
    public void testInvalidBulkUpdateTasks() throws IOException {
        ObjectNode requestNode = objectMapper.createObjectNode();

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION));
        httpPut.setEntity(null);
        executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST);

        httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION));
        requestNode.put("name", "testName");
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "message:'Bad request',"
                        + "exception:'taskIds can not be null for bulk update tasks requests'"
                        + "}");

        requestNode.putArray("taskIds").add("invalidId");

        httpPut.setEntity(new StringEntity(requestNode.toString()));
        executeRequest(httpPut, HttpStatus.SC_NOT_FOUND);

    }

    @Test
    @Deployment(resources = {
            "org/flowable/rest/service/api/runtime/simpleParallelCallActivity.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleInnerCallActivity.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleProcessWithUserTasks.bpmn20.xml",
            "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml"
    })
    public void testQueryByRootScopeId() throws IOException {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");

        List<String> taskExecutionIds = runtimeService.createExecutionQuery().rootProcessInstanceId(processInstance.getId())
                .processDefinitionKey("oneTaskProcess").activityId("theTask").list().stream().map(Execution::getId).toList();

        Task task1 = taskService.createTaskQuery().executionId(taskExecutionIds.get(0)).singleResult();
        Task task2 = taskService.createTaskQuery().executionId(taskExecutionIds.get(1)).singleResult();
        Task task3 = taskService.createTaskQuery().executionId(taskExecutionIds.get(2)).singleResult();

        Execution formTask1Execution = runtimeService.createExecutionQuery().rootProcessInstanceId(processInstance.getId()).activityId("formTask1")
                .singleResult();
        Task formTask1 = taskService.createTaskQuery().executionId(formTask1Execution.getId()).singleResult();

        Execution taskForm2Execution = runtimeService.createExecutionQuery().rootProcessInstanceId(processInstance.getId()).activityId("formTask2")
                .singleResult();
        Task formTask2 = taskService.createTaskQuery().executionId(taskForm2Execution.getId()).singleResult();

        String url = SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?rootScopeId="
                + processInstance.getId();

        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + task1.getId() + "' },"
                        + "    { id: '" + task2.getId() + "' },"
                        + "    { id: '" + task3.getId() + "' },"
                        + "    { id: '" + formTask1.getId() + "' },"
                        + "    { id: '" + formTask2.getId() + "' }"
                        + "  ]"
                        + "}");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/rest/service/api/runtime/simpleParallelCallActivity.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleInnerCallActivity.bpmn20.xml",
            "org/flowable/rest/service/api/runtime/simpleProcessWithUserTasks.bpmn20.xml",
            "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml"
    })
    public void testQueryByParentScopeId() throws IOException {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");

        Execution formTask1Execution = runtimeService.createExecutionQuery().rootProcessInstanceId(processInstance.getId()).activityId("formTask1")
                .singleResult();
        Task formTask1 = taskService.createTaskQuery().executionId(formTask1Execution.getId()).singleResult();

        Execution taskForm2Execution = runtimeService.createExecutionQuery().rootProcessInstanceId(processInstance.getId()).activityId("formTask2")
                .singleResult();
        Task formTask2 = taskService.createTaskQuery().executionId(taskForm2Execution.getId()).singleResult();


        String url = SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?parentScopeId="
                + taskForm2Execution.getProcessInstanceId();

        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + formTask1.getId() + "' },"
                        + "    { id: '" + formTask2.getId() + "' }"

                        + "  ]"
                        + "}");

    }
    
    protected void prepareTasks() {
        adhocTaskCreateTime = Calendar.getInstance();
        adhocTaskCreateTime.set(Calendar.MILLISECOND, 0);

        Calendar processTaskCreate = Calendar.getInstance();
        processTaskCreate.add(Calendar.HOUR, 2);
        processTaskCreate.set(Calendar.MILLISECOND, 0);

        inBetweenTaskCreation = Calendar.getInstance();
        inBetweenTaskCreation.add(Calendar.HOUR, 1);

        processEngineConfiguration.getClock().setCurrentTime(adhocTaskCreateTime.getTime());
        preparedAdhocTask = taskService.newTask();
        preparedAdhocTask.setAssignee("gonzo");
        preparedAdhocTask.setOwner("owner");
        preparedAdhocTask.setDelegationState(DelegationState.PENDING);
        preparedAdhocTask.setDescription("Description one");
        preparedAdhocTask.setName("Name one");
        preparedAdhocTask.setDueDate(adhocTaskCreateTime.getTime());
        preparedAdhocTask.setPriority(100);
        preparedAdhocTask.setCategory("some-category");
        taskService.saveTask(preparedAdhocTask);
        taskService.addUserIdentityLink(preparedAdhocTask.getId(), "misspiggy", IdentityLinkType.PARTICIPANT);

        processEngineConfiguration.getClock().setCurrentTime(processTaskCreate.getTime());
        preparedProcessInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "myBusinessKey");
        preparedProcessTask = taskService.createTaskQuery().processInstanceId(preparedProcessInstance.getId()).singleResult();
        preparedProcessTask.setParentTaskId(preparedAdhocTask.getId());
        preparedProcessTask.setPriority(50);
        preparedProcessTask.setDueDate(processTaskCreate.getTime());
        taskService.saveTask(preparedProcessTask);
        runtimeService.setVariable(preparedProcessInstance.getId(), "variable", "globaltest");
        taskService.setVariableLocal(preparedProcessTask.getId(), "localVariable", "localtest");
    }

}
