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

import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to the Task collection resource.
 * 
 * @author Tijs Rademakers
 */
public class TaskCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test creating a task. POST runtime/tasks
     */
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
            assertEquals(longDateFormat.parse(dueDateString), task.getDueDate());
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
    @CmmnDeployment(resources = {"org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn"})
    public void testGetTasks() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;
        try {
            Calendar adhocTaskCreate = Calendar.getInstance();
            adhocTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar processTaskCreate = Calendar.getInstance();
            processTaskCreate.add(Calendar.HOUR, 2);
            processTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar inBetweenTaskCreation = Calendar.getInstance();
            inBetweenTaskCreation.add(Calendar.HOUR, 1);

            cmmnEngineConfiguration.getClock().setCurrentTime(adhocTaskCreate.getTime());
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

            cmmnEngineConfiguration.getClock().setCurrentTime(processTaskCreate.getTime());
            
            deployment = repositoryService.createDeployment().addClasspathResource(
                            "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();
            
            CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase")
                            .businessKey("myBusinessKey").tenantId("myTenant").start();
            Task caseTask = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            caseTask.setParentTaskId(adhocTask.getId());
            caseTask.setPriority(50);
            caseTask.setDueDate(processTaskCreate.getTime());
            taskService.saveTask(caseTask);
            runtimeService.setVariable(caseInstance.getId(), "variable", "globaltest");
            taskService.setVariableLocal(caseTask.getId(), "localVariable", "localtest");

            // Check filter-less to fetch all tasks
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION);
            assertResultsPresentInDataResponse(url, adhocTask.getId(), caseTask.getId());

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
            assertResultsPresentInDataResponse(url, caseTask.getId());

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
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Candidate user filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateUser=kermit";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Candidate group filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?candidateGroup=sales";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Involved user filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?involvedUser=misspiggy";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Case instance filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?caseInstanceId=" + caseInstance.getId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // CreatedOn filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdOn=" + getISODateString(adhocTaskCreate.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // CreatedAfter filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // CreatedBefore filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Subtask exclusion
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?excludeSubTasks=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Task definition key filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKey=theTask";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Task definition key like filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKeyLike=" + encode("theT%");
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Duedate filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getISODateString(adhocTaskCreate.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Due after filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Due before filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?dueBefore=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Without tenantId filtering before tenant set
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Tenant id filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Category filtering
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COLLECTION) + "?category=" + encode("some-category");
            assertResultsPresentInDataResponse(url, adhocTask.getId());
            
        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                if (task.getExecutionId() == null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
            
            if (deployment != null) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }
}
