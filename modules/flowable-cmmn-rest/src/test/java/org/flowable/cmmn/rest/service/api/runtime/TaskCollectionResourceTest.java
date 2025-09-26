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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to the Task collection resource.
 *
 * @author Tijs Rademakers
 * @author Christopher Welsch
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
            String createdTaskId = responseNode.get("id").asText();

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
            Calendar adhocTaskCreate = Calendar.getInstance();
            adhocTaskCreate.set(Calendar.SECOND, 14);
            adhocTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar processTaskCreate = Calendar.getInstance();
            processTaskCreate.add(Calendar.HOUR, 2);
            processTaskCreate.set(Calendar.SECOND, 15);
            processTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar inBetweenTaskCreation = Calendar.getInstance();
            inBetweenTaskCreation.add(Calendar.HOUR, 1);
            inBetweenTaskCreation.set(Calendar.SECOND, 21);

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
                    "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();

            CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase")
                    .businessKey("myBusinessKey").tenantId("myTenant").start();
            PlanItemInstance planItemInstance = runtimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).singleResult();

            Task caseTask = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            caseTask.setParentTaskId(adhocTask.getId());
            caseTask.setPriority(50);
            caseTask.setDueDate(processTaskCreate.getTime());
            taskService.saveTask(caseTask);
            taskService.unclaim(caseTask.getId());
            taskService.addUserIdentityLink(caseTask.getId(), "kermit", IdentityLinkType.CANDIDATE);
            taskService.addGroupIdentityLink(caseTask.getId(), "sales", IdentityLinkType.CANDIDATE);
            runtimeService.setVariable(caseInstance.getId(), "variable", "globaltest");
            taskService.setVariableLocal(caseTask.getId(), "localVariable", "localtest");

            // Check filter-less to fetch all tasks
            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION);
            assertResultsPresentInDataResponse(url, adhocTask.getId(), caseTask.getId());

            // ID filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?taskId=" + encode(adhocTask.getId());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Name filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?name=" + encode("Name one");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Name like filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?nameLike=" + encode("%one");
            assertResultsPresentInDataResponse(url, adhocTask.getId());
            
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?nameLike=none";
            assertResultsPresentInDataResponse(url);
            
            // Name like ignore case filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?nameLikeIgnoreCase=" + encode("%ONE");
            assertResultsPresentInDataResponse(url, adhocTask.getId());
            
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?nameLikeIgnoreCase=none";
            assertResultsPresentInDataResponse(url);

            // Description filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?description=" + encode("Description one");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?description=" + encode("Description two");
            assertEmptyResultsPresentInDataResponse(url);

            // Description like filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?descriptionLike=" + encode("%one");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?descriptionLike=" + encode("%two");
            assertEmptyResultsPresentInDataResponse(url);

            // Priority filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?priority=100";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Minimum Priority filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?minimumPriority=70";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Maximum Priority filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?maximumPriority=70";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Owner filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?owner=owner";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?owner=kermit";
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?ownerLike=" + encode("%ner");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?ownerLike=" + encode("kerm%");
            assertEmptyResultsPresentInDataResponse(url);

            // Assignee filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?assignee=gonzo";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?assignee=kermit";
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?assigneeLike=" + encode("gon%");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?assigneeLike=" + encode("kerm%");
            assertEmptyResultsPresentInDataResponse(url);

            // Unassigned filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?unassigned=true";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Candidate user filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?candidateUser=kermit";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Candidate group filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?candidateGroup=sales";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Involved user filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?involvedUser=misspiggy";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Claim task
            taskService.claim(caseTask.getId(), "johnDoe");

            // IgnoreAssignee
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?candidateGroup=sales&ignoreAssignee=true";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?candidateGroup=notExisting&ignoreAssignee";
            assertEmptyResultsPresentInDataResponse(url);

            // Case instance filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseInstanceId=" + caseInstance.getId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Case instance with children filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseInstanceIdWithChildren=" + caseInstance.getId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Plan item instance id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?planItemInstanceId=" + planItemInstance.getId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Scope id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?scopeId=" + caseInstance.getId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?scopeIds=someId," + caseInstance.getId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Sub scope id id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?subScopeId=" + planItemInstance.getId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Scope type filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?scopeType=" + ScopeTypes.CMMN;
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Combination of the three above
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?scopeId=" + caseInstance.getId() + "&subScopeId=" + planItemInstance.getId() + "&scopeType=" + ScopeTypes.CMMN;
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Case instance with children filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseInstanceIdWithChildren=notexisting";
            assertResultsPresentInDataResponse(url);
            
            // Without scope id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?withoutScopeId=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // CreatedOn filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdOn=" + getISODateString(adhocTaskCreate.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // CreatedAfter filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getISODateString(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, caseTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getIsoDateStringWithoutMS(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, caseTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdAfter=" + getIsoDateStringWithoutSeconds(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // CreatedBefore filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getISODateString(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getIsoDateStringWithoutSeconds(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?createdBefore=" + getIsoDateStringWithoutMS(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Subtask exclusion
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?excludeSubTasks=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Task definition key filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKey=theTask";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Task definition key like filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?taskDefinitionKeyLike=" + encode("theT%");
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Duedate filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getISODateString(adhocTaskCreate.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getIsoDateStringWithoutSeconds(
                    adhocTaskCreate.getTime());
            assertResultsPresentInDataResponse(url);
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueDate=" + getIsoDateStringWithoutMS(adhocTaskCreate.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Due after filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, caseTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getIsoDateStringWithoutSeconds(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, caseTask.getId());
            assertResultsPresentInDataResponse(url, caseTask.getId());
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueAfter=" + getIsoDateStringWithoutMS(
                    inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Due before filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?dueBefore=" + getISODateString(inBetweenTaskCreation.getTime());
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            // Without tenantId filtering before tenant set
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId();
            assertResultsPresentInDataResponse(url, caseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionId=notExisting";
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKey=oneHumanTaskCase";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKey=notExisting";
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKeyLike=" + encode("%TaskCase");
            assertResultsPresentInDataResponse(url, caseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKeyLike=" + encode("%notExisting");
            assertEmptyResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKeyLikeIgnoreCase=" + encode("%taskcase");
            assertResultsPresentInDataResponse(url, caseTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?caseDefinitionKeyLikeIgnoreCase=" + encode("%notexisting");
            assertEmptyResultsPresentInDataResponse(url);

            // Tenant id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, caseTask.getId());

            // Category filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?category=" + encode("some-category");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?categoryIn=" + encode("non-exisiting,some-category");
            assertResultsPresentInDataResponse(url, adhocTask.getId());

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?categoryNotIn=" + encode("some-category");
            assertResultsPresentInDataResponse(url);

            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?withoutCategory=true";
            assertResultsPresentInDataResponse(url, caseTask.getId());
            
            // Without process instance id filtering
            url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_COLLECTION) + "?withoutProcessInstanceId=true";
            assertResultsPresentInDataResponse(url, caseTask.getId(), adhocTask.getId());

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

}
