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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpStatus;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to the Task collection resource.
 * 
 * @author Tijs Rademakers
 */
public class TaskQueryResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying tasks. GET runtime/tasks
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testQueryTasks() throws Exception {
        try {
            Calendar adhocTaskCreate = Calendar.getInstance();
            adhocTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar caseTaskCreate = Calendar.getInstance();
            caseTaskCreate.add(Calendar.HOUR, 2);
            caseTaskCreate.set(Calendar.MILLISECOND, 0);

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
            adhocTask.setFormKey("myForm.json");
            adhocTask.setCategory("some-category");
            taskService.saveTask(adhocTask);
            taskService.addUserIdentityLink(adhocTask.getId(), "misspiggy", IdentityLinkType.PARTICIPANT);

            cmmnEngineConfiguration.getClock().setCurrentTime(caseTaskCreate.getTime());
            CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();
            Task caseTask = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            caseTask.setParentTaskId(adhocTask.getId());
            caseTask.setPriority(50);
            caseTask.setDueDate(caseTaskCreate.getTime());
            taskService.saveTask(caseTask);

            // Check filter-less to fetch all tasks
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_QUERY);
            ObjectNode requestNode = objectMapper.createObjectNode();
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId(), adhocTask.getId());

            // Name filtering
            requestNode.removeAll();
            requestNode.put("name", "Name one");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Name like filtering
            requestNode.removeAll();
            requestNode.put("nameLike", "%one");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Description filtering
            requestNode.removeAll();
            requestNode.put("description", "Description one");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Description like filtering
            requestNode.removeAll();
            requestNode.put("descriptionLike", "%one");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Priority filtering
            requestNode.removeAll();
            requestNode.put("priority", 100);
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Mininmum Priority filtering
            requestNode.removeAll();
            requestNode.put("minimumPriority", 70);
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Maximum Priority filtering
            requestNode.removeAll();
            requestNode.put("maximumPriority", 70);
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // Owner filtering
            requestNode.removeAll();
            requestNode.put("owner", "owner");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Assignee filtering
            requestNode.removeAll();
            requestNode.put("assignee", "gonzo");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Owner like filtering
            requestNode.removeAll();
            requestNode.put("ownerLike", "owne%");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Assignee like filtering
            requestNode.removeAll();
            requestNode.put("assigneeLike", "%onzo");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Unassigned filtering
            requestNode.removeAll();
            requestNode.put("unassigned", true);
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // Delegation state filtering
            requestNode.removeAll();
            requestNode.put("delegationState", "pending");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Candidate user filtering
            requestNode.removeAll();
            requestNode.put("candidateUser", "kermit");
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // Candidate group filtering
            requestNode.removeAll();
            requestNode.put("candidateGroup", "sales");
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // Candidate group In filtering
            requestNode.removeAll();
            ArrayNode arrayNode = requestNode.arrayNode();

            arrayNode.add("sales");
            arrayNode.add("someOtherGroup");

            requestNode.set("candidateGroupIn", arrayNode);
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // Involved user filtering
            requestNode.removeAll();
            requestNode.put("involvedUser", "misspiggy");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Process instance filtering
            requestNode.removeAll();
            requestNode.put("caseInstanceId", caseInstance.getId());
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // CreatedOn filtering
            requestNode.removeAll();
            requestNode.put("createdOn", getISODateString(adhocTaskCreate.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // CreatedAfter filtering
            requestNode.removeAll();
            requestNode.put("createdAfter", getISODateString(inBetweenTaskCreation.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // CreatedBefore filtering
            requestNode.removeAll();
            requestNode.put("createdBefore", getISODateString(inBetweenTaskCreation.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Subtask exclusion
            requestNode.removeAll();
            requestNode.put("excludeSubTasks", true);
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Task definition key filtering
            requestNode.removeAll();
            requestNode.put("taskDefinitionKey", "theTask");
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // Task definition key like filtering
            requestNode.removeAll();
            requestNode.put("taskDefinitionKeyLike", "theTa%");
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // Duedate filtering
            requestNode.removeAll();
            requestNode.put("dueDate", getISODateString(adhocTaskCreate.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Due after filtering
            requestNode.removeAll();
            requestNode.put("dueAfter", getISODateString(inBetweenTaskCreation.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

            // Due before filtering
            requestNode.removeAll();
            requestNode.put("dueBefore", getISODateString(inBetweenTaskCreation.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Filtering by category
            requestNode.removeAll();
            requestNode.put("category", "some-category");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Filtering without duedate
            requestNode.removeAll();
            requestNode.put("withoutDueDate", true);
            // No response should be returned, no tasks without a duedate yet
            assertResultsPresentInPostDataResponse(url, requestNode);

            caseTask = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            caseTask.setDueDate(null);
            taskService.saveTask(caseTask);
            assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                if (task.getScopeId() == null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }

    /**
     * Test querying tasks using task and case variables. GET cmmn-runtime/tasks
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testQueryTasksWithVariables() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();
        Task caseTask = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Abcdef");
        variables.put("intVar", 12345);
        variables.put("booleanVar", true);
        taskService.setVariablesLocal(caseTask.getId(), variables);

        // Additional tasks to confirm it's filtered out
        runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode variableArray = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableArray.add(variableNode);
        requestNode.set("taskVariables", variableArray);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_QUERY);

        // String equals
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Abcdef");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12345);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "abCDEF");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // String not equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "HIJKLm");
        variableNode.put("operation", "notEqualsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // String equals without value
        variableNode.removeAll();
        variableNode.put("value", "Abcdef");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // Greater than
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12300);
        variableNode.put("operation", "greaterThan");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());
        variableNode.put("value", 12345);
        variableNode.put("operation", "greaterThan");
        assertResultsPresentInPostDataResponse(url, requestNode);

        // Greater than or equal
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12300);
        variableNode.put("operation", "greaterThanOrEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());
        variableNode.put("value", 12345);
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // Less than
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12400);
        variableNode.put("operation", "lessThan");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());
        variableNode.put("value", 12345);
        variableNode.put("operation", "lessThan");
        assertResultsPresentInPostDataResponse(url, requestNode);

        // Less than or equal
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12400);
        variableNode.put("operation", "lessThanOrEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());
        variableNode.put("value", 12345);
        assertResultsPresentInPostDataResponse(url, requestNode, caseTask.getId());

        // Like
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Abcde%");
        variableNode.put("operation", "like");

        // Any other operation but equals without value
        variableNode.removeAll();
        variableNode.put("value", "abcdef");
        variableNode.put("operation", "notEquals");

        assertResultsPresentInPostDataResponseWithStatusCheck(url, requestNode, HttpStatus.SC_BAD_REQUEST);

        // Illegal (but existing) operation
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "abcdef");
        variableNode.put("operation", "operationX");

        assertResultsPresentInPostDataResponseWithStatusCheck(url, requestNode, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test querying tasks. GET cmmn-runtime/tasks
     */
    public void testQueryTasksWithPaging() throws Exception {
        try {
            Calendar adhocTaskCreate = Calendar.getInstance();
            adhocTaskCreate.set(Calendar.MILLISECOND, 0);

            cmmnEngineConfiguration.getClock().setCurrentTime(adhocTaskCreate.getTime());
            List<String> taskIdList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Task adhocTask = taskService.newTask();
                adhocTask.setAssignee("gonzo");
                adhocTask.setOwner("owner");
                adhocTask.setDelegationState(DelegationState.PENDING);
                adhocTask.setDescription("Description one");
                adhocTask.setName("Name one");
                adhocTask.setDueDate(adhocTaskCreate.getTime());
                adhocTask.setPriority(100);
                taskService.saveTask(adhocTask);
                taskService.addUserIdentityLink(adhocTask.getId(), "misspiggy", IdentityLinkType.PARTICIPANT);
                taskIdList.add(adhocTask.getId());
            }
            Collections.sort(taskIdList);

            // Check filter-less to fetch all tasks
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_QUERY);
            ObjectNode requestNode = objectMapper.createObjectNode();
            String[] taskIds = new String[] { taskIdList.get(0), taskIdList.get(1), taskIdList.get(2) };
            assertResultsPresentInPostDataResponse(url + "?size=3&sort=id&order=asc", requestNode, taskIds);

            taskIds = new String[] { taskIdList.get(4), taskIdList.get(5), taskIdList.get(6), taskIdList.get(7) };
            assertResultsPresentInPostDataResponse(url + "?start=4&size=4&sort=id&order=asc", requestNode, taskIds);

            taskIds = new String[] { taskIdList.get(8), taskIdList.get(9) };
            assertResultsPresentInPostDataResponse(url + "?start=8&size=10&sort=id&order=asc", requestNode, taskIds);

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                if (task.getScopeId() == null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }
}
