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

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import net.javacrumbs.jsonunit.core.Option;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to the Task collection resource.
 *
 * @author Tijs Rademakers
 * @author Christopher Welsch
 */
public class TaskCollectionResourceTest extends BaseSpringRestTestCase {
    
    protected CaseInstance preparedCaseInstance;
    protected PlanItemInstance preparedPlanItemInstance;
    protected Task preparedAdhocTask;
    protected Task preparedCaseTask;
    
    protected Calendar inBetweenTaskCreation;
    protected Calendar adhocTaskCreateTime;
    protected Calendar processTaskCreateTime;

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
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION));
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
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION));
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
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetTasks() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;
        try {
            deployment = prepareTasks();

            // Check filter-less to fetch all tasks
            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION);
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId(), preparedCaseTask.getId());

            // ID filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?taskId=" + encode(preparedAdhocTask.getId());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Name filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?name=" + encode("Name one");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Name like filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?nameLike=" + encode("%one");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());
            
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?nameLike=none";
            assertResultsPresentInDataResponse(url);
            
            // Name like ignore case filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?nameLikeIgnoreCase=" + encode("%ONE");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());
            
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?nameLikeIgnoreCase=none";
            assertResultsPresentInDataResponse(url);

            // Description filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?description=" + encode("Description one");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?description=" + encode("Description two");
            assertEmptyResultsPresentInDataResponse(url);

            // Description like filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?descriptionLike=" + encode("%one");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?descriptionLike=" + encode("%two");
            assertEmptyResultsPresentInDataResponse(url);

            // Priority filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?priority=100";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Minimum Priority filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?minimumPriority=70";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Maximum Priority filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?maximumPriority=70";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Without tenantId filtering before tenant set
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionId=" + preparedCaseInstance.getCaseDefinitionId();
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionId=notExisting";
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKey=oneHumanTaskCase";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKey=notExisting";
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKeyLike=" + encode("%TaskCase");
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKeyLike=" + encode("%notExisting");
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKeyLikeIgnoreCase=" + encode("%taskcase");
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKeyLikeIgnoreCase=" + encode("%notexisting");
            assertEmptyResultsPresentInDataResponse(url);

            // Tenant id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Category filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?category=" + encode("some-category");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?categoryIn=" + encode("non-exisiting,some-category");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?categoryNotIn=" + encode("some-category");
            assertResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?withoutCategory=true";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());
            
            // Without process instance id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?withoutProcessInstanceId=true";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId(), preparedAdhocTask.getId());

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
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetTasksIdentityInfo() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;
        try {
            deployment = prepareTasks();

            // Owner filtering
            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?owner=owner";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?owner=kermit";
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?ownerLike=" + encode("%ner");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?ownerLike=" + encode("kerm%");
            assertEmptyResultsPresentInDataResponse(url);

            // Assignee filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?assignee=gonzo";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?assignee=kermit";
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?assigneeLike=" + encode("gon%");
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?assigneeLike=" + encode("kerm%");
            assertEmptyResultsPresentInDataResponse(url);

            // Unassigned filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?unassigned=true";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Candidate user filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?candidateUser=kermit";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Candidate group filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?candidateGroup=sales";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Involved user filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?involvedUser=misspiggy";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Claim task
            taskService.claim(preparedCaseTask.getId(), "johnDoe");

            // IgnoreAssignee
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?candidateGroup=sales&ignoreAssignee=true";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?candidateGroup=notExisting&ignoreAssignee";
            assertEmptyResultsPresentInDataResponse(url);

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
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetTasksScopeInfo() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;
        try {
            deployment = prepareTasks();

            // Case instance filtering
            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseInstanceId=" + preparedCaseInstance.getId();
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Case instance with children filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseInstanceIdWithChildren=" + preparedCaseInstance.getId();
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Plan item instance id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?planItemInstanceId=" + preparedPlanItemInstance.getId();
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Scope id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?scopeId=" + preparedCaseInstance.getId();
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?scopeIds=someId," + preparedCaseInstance.getId();
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Sub scope id id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?subScopeId=" + preparedPlanItemInstance.getId();
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Scope type filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?scopeType=" + ScopeTypes.CMMN;
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Combination of the three above
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?scopeId=" + preparedCaseInstance.getId() + "&subScopeId=" + preparedPlanItemInstance.getId() + "&scopeType=" + ScopeTypes.CMMN;
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Case instance with children filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseInstanceIdWithChildren=notexisting";
            assertResultsPresentInDataResponse(url);
            
            // Without scope id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?withoutScopeId=true";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

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
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetTasksDateInfo() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;
        try {
            deployment = prepareTasks();

            // CreatedOn filtering
            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdOn=" + getISODateString(adhocTaskCreateTime.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // CreatedAfter filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getISODateString(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getIsoDateStringWithoutMS(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getIsoDateStringWithoutSeconds(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // CreatedBefore filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getISODateString(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getIsoDateStringWithoutSeconds(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getIsoDateStringWithoutMS(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Subtask exclusion
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?excludeSubTasks=true";
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Task definition key filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKey=theTask";
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Task definition key like filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKeyLike=" + encode("theT%");
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Duedate filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getISODateString(adhocTaskCreateTime.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getIsoDateStringWithoutSeconds(
                    adhocTaskCreateTime.getTime());
            assertResultsPresentInDataResponse(url);
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getIsoDateStringWithoutMS(adhocTaskCreateTime.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

            // Due after filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getIsoDateStringWithoutSeconds(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getIsoDateStringWithoutMS(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedCaseTask.getId());

            // Due before filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueBefore=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, preparedAdhocTask.getId());

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

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/PropagatedStageInstanceId.cmmn" })
    public void testQueryWithPropagatedStageId() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("propagatedStageInstanceId").start();
        Task task1 = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        PlanItemInstance stageInstanceId1 = runtimeService.createPlanItemInstanceQuery()
            .onlyStages()
            .caseInstanceId(caseInstance.getId())
            .planItemDefinitionId("expandedStage2")
            .singleResult();
        assertThat(stageInstanceId1).isNotNull();

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?propagatedStageInstanceId=wrong";
        assertEmptyResultsPresentInDataResponse(url);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?propagatedStageInstanceId=" + stageInstanceId1.getId();
        assertResultsPresentInDataResponse(url, task1.getId());

        taskService.complete(task1.getId());
        Task task2 = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        PlanItemInstance stageInstanceId2 = runtimeService.createPlanItemInstanceQuery()
            .onlyStages()
            .caseInstanceId(caseInstance.getId())
            .planItemDefinitionId("expandedStage3")
            .singleResult();
        assertThat(stageInstanceId2).isNotNull();

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?propagatedStageInstanceId=" + stageInstanceId2.getId();
        assertResultsPresentInDataResponse(url, task2.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?propagatedStageInstanceId=" + stageInstanceId1.getId();
        assertEmptyResultsPresentInDataResponse(url);
    }

    @Test
    public void testBulkUpdateTaskAssignee() throws IOException {

        taskService.createTaskBuilder().id("taskID1").create();
        taskService.createTaskBuilder().id("taskID2").create();
        taskService.createTaskBuilder().id("taskID3").create();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("assignee", "admin");
        requestNode.putArray("taskIds").add("taskID1").add("taskID3");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION));
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

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION));
        httpPut.setEntity(null);
        executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST);

        httpPut = new HttpPut(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION));
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
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/rest/service/api/runtime/simpleCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithHumanTasksAndCaseTask.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/oneHumanTaskCase.cmmn"
    })
    public void testQueryByRootScopeId() throws IOException {
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();

        PlanItemInstance oneTaskCasePlanItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        Task oneTaskCaseTask1 = taskService.createTaskQuery().caseInstanceId(oneTaskCasePlanItemInstance.getReferenceId()).singleResult();

        PlanItemInstance caseTaskSimpleCaseWithCaseTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskSimpleCaseWithCaseTasks").singleResult();

        Task caseTaskSimpleCaseWithCaseTasksTask = taskService.createTaskQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId()).singleResult();

        PlanItemInstance caseTaskWithHumanTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskCaseWithHumanTasks").singleResult();

        List<Task> twoHumanTasks = taskService.createTaskQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId()).list();

        PlanItemInstance oneTaskCase2PlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();
        Task oneTaskCaseTask2 = taskService.createTaskQuery().caseInstanceId(oneTaskCase2PlanItemInstance.getReferenceId()).singleResult();

        String url = SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?rootScopeId="
                + caseInstance.getId();

        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + oneTaskCaseTask1.getId() + "' },"
                        + "    { id: '" + oneTaskCaseTask2.getId() + "' },"
                        + "    { id: '" + twoHumanTasks.get(0).getId() + "' },"
                        + "    { id: '" + twoHumanTasks.get(1).getId() + "' },"
                        + "    { id: '" + caseTaskSimpleCaseWithCaseTasksTask.getId() + "' }"
                        + "  ]"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/rest/service/api/runtime/simpleCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/simpleInnerCaseWithHumanTasksAndCaseTask.cmmn",
            "org/flowable/cmmn/rest/service/api/runtime/oneHumanTaskCase.cmmn"
    })
    public void testQueryByParentScopeId() throws IOException {
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();

        PlanItemInstance caseTaskSimpleCaseWithCaseTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskSimpleCaseWithCaseTasks").singleResult();

        PlanItemInstance caseTaskWithHumanTasksPlanItemInstance = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskCaseWithHumanTasks").singleResult();

        List<PlanItemInstance> planItemInstances = runtimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId()).list();

        String url = SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?parentScopeId="
                + caseTaskWithHumanTasksPlanItemInstance.getReferenceId();

        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + planItemInstances.get(0).getReferenceId() + "' },"
                        + "    { id: '" + planItemInstances.get(1).getReferenceId() + "' }"
                        + "  ]"
                        + "}");
    }

    protected org.flowable.cmmn.api.repository.CmmnDeployment prepareTasks() {
        adhocTaskCreateTime = Calendar.getInstance();
        adhocTaskCreateTime.set(Calendar.SECOND, 14);
        adhocTaskCreateTime.set(Calendar.MILLISECOND, 0);

        processTaskCreateTime = Calendar.getInstance();
        processTaskCreateTime.add(Calendar.HOUR, 2);
        processTaskCreateTime.set(Calendar.SECOND, 15);
        processTaskCreateTime.set(Calendar.MILLISECOND, 0);

        inBetweenTaskCreation = Calendar.getInstance();
        inBetweenTaskCreation.add(Calendar.HOUR, 1);
        inBetweenTaskCreation.set(Calendar.SECOND, 21);

        cmmnEngineConfiguration.getClock().setCurrentTime(adhocTaskCreateTime.getTime());
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

        cmmnEngineConfiguration.getClock().setCurrentTime(processTaskCreateTime.getTime());

        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeployment().addClasspathResource(
                "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();

        preparedCaseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase")
                .businessKey("myBusinessKey").tenantId("myTenant").start();
        preparedPlanItemInstance = runtimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).singleResult();

        preparedCaseTask = taskService.createTaskQuery().caseInstanceId(preparedCaseInstance.getId()).singleResult();
        preparedCaseTask.setParentTaskId(preparedAdhocTask.getId());
        preparedCaseTask.setPriority(50);
        preparedCaseTask.setDueDate(processTaskCreateTime.getTime());
        taskService.saveTask(preparedCaseTask);
        taskService.unclaim(preparedCaseTask.getId());
        taskService.addUserIdentityLink(preparedCaseTask.getId(), "kermit", IdentityLinkType.CANDIDATE);
        taskService.addGroupIdentityLink(preparedCaseTask.getId(), "sales", IdentityLinkType.CANDIDATE);
        runtimeService.setVariable(preparedCaseInstance.getId(), "variable", "globaltest");
        taskService.setVariableLocal(preparedCaseTask.getId(), "localVariable", "localtest");
        
        return deployment;
    }
}
