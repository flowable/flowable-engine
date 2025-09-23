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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to the historic task instance query resource.
 *
 * @author Tijs Rademakers
 */
public class HistoricTaskInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic task instance. GET cmmn-history/historic-task-instances
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryTaskInstances() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        Calendar created = Calendar.getInstance();
        created.set(Calendar.YEAR, 2001);
        created.set(Calendar.MILLISECOND, 0);
        cmmnEngineConfiguration.getClock().setCurrentTime(created.getTime());

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase")
                .businessKey("myBusinessKey")
                .variables(caseVariables)
                .start();
        cmmnEngineConfiguration.getClock().reset();
        Task task1 = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task1.getId());
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.setVariableLocal(task.getId(), "local", "test");
        taskService.setAssignee(task.getId(), "fozzie");
        taskService.setOwner(task.getId(), "test");
        taskService.setDueDate(task.getId(), new GregorianCalendar(2013, 0, 1).getTime());

        // Set tenant on deployment
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeployment().addClasspathResource(
                "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn").tenantId("myTenant").deploy();

        try {
            CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").tenantId("myTenant").variables(caseVariables)
                    .start();
            Task task2 = taskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();

            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES);

            assertResultsPresentInDataResponse(url, 3, task.getId(), task1.getId(), task2.getId());

            assertResultsPresentInDataResponse(url + "?taskMinPriority=" + "0", 3, task.getId());

            assertResultsPresentInDataResponse(url + "?taskMaxPriority=" + "60", 3, task.getId());

            assertResultsPresentInDataResponse(url + "?caseInstanceId=" + caseInstance.getId(), 2, task.getId());

            assertResultsPresentInDataResponse(url + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId(), 2, task.getId());

            assertResultsPresentInDataResponse(url + "?caseDefinitionkey=myCase", 3, task.getId());
            assertResultsPresentInDataResponse(url + "?caseDefinitionkeyLike=" + encode("%Case"), 3, task.getId());
            assertResultsPresentInDataResponse(url + "?caseDefinitionKeyLikeIgnoreCase=" + encode("%case"), 3, task.getId());

            assertResultsPresentInDataResponse(url + "?caseInstanceId=" + caseInstance2.getId(), 1, task2.getId());

            assertResultsPresentInDataResponse(url + "?caseInstanceIdWithChildren=" + caseInstance.getId(), 2, task.getId());

            assertResultsPresentInDataResponse(url + "?caseInstanceIdWithChildren=nonexisting", 0);

            assertResultsPresentInDataResponse(url + "?scopeId=" + caseInstance.getId(), 2, task1.getId(), task.getId());
            assertResultsPresentInDataResponse(url + "?scopeIds=someId," + caseInstance.getId(), 2, task1.getId(), task.getId());
            
            // Without scope id
            assertResultsPresentInDataResponse(url + "?withoutScopeId=true", 0);

            assertResultsPresentInDataResponse(url + "?taskAssignee=kermit", 2, task2.getId());

            assertResultsPresentInDataResponse(url + "?taskAssigneeLike=" + encode("%mit"), 2, task2.getId());

            assertResultsPresentInDataResponse(url + "?taskAssignee=fozzie", 1, task.getId());

            assertResultsPresentInDataResponse(url + "?taskOwner=test", 1, task.getId());

            assertResultsPresentInDataResponse(url + "?taskOwnerLike=" + encode("t%"), 1, task.getId());

            assertResultsPresentInDataResponse(url + "?taskInvolvedUser=test", 1, task.getId());
            
            assertResultsPresentInDataResponse(url + "?taskName=" + encode("Task One"), task1.getId(), task2.getId());
            assertResultsPresentInDataResponse(url + "?taskName=none");
            
            assertResultsPresentInDataResponse(url + "?taskNameLike=" + encode("Task%"), task1.getId(), task2.getId());
            assertResultsPresentInDataResponse(url + "?taskNameLike=none");
            
            assertResultsPresentInDataResponse(url + "?taskNameLikeIgnoreCase=" + encode("TASK%"), task1.getId(), task2.getId());
            assertResultsPresentInDataResponse(url + "?taskNameLikeIgnoreCase=NONE");

            assertResultsPresentInDataResponse(url + "?dueDateAfter=" + getISODateString(new GregorianCalendar(2010, 0, 1).getTime()) , 1, task.getId());
            assertResultsPresentInDataResponse(url + "?dueDateAfter=" + getIsoDateStringWithoutSeconds(new GregorianCalendar(2010, 0, 1).getTime()) , 1, task.getId());
            assertResultsPresentInDataResponse(url + "?dueDateAfter=" + getIsoDateStringWithoutMS(new GregorianCalendar(2010, 0, 1).getTime()) , 1, task.getId());

            assertResultsPresentInDataResponse(url + "?dueDateAfter=" + getISODateString(new GregorianCalendar(2013, 4, 1).getTime()), 0);

            assertResultsPresentInDataResponse(url + "?dueDateBefore=" + getISODateString(new GregorianCalendar(2010, 0, 1).getTime()), 0);
            assertResultsPresentInDataResponse(url + "?dueDateBefore=" + getIsoDateStringWithoutSeconds(new GregorianCalendar(2010, 0, 1).getTime()), 0);
            assertResultsPresentInDataResponse(url + "?dueDateBefore=" + getIsoDateStringWithoutMS(new GregorianCalendar(2010, 0, 1).getTime()), 0);

            assertResultsPresentInDataResponse(url + "?dueDateBefore=" + getISODateString(new GregorianCalendar(2013, 4, 1).getTime()), 1, task.getId());

            created.set(Calendar.YEAR, 2002);
            assertResultsPresentInDataResponse(url + "?taskCreatedBefore=" + getISODateString(created.getTime()), 1, task1.getId());
            assertResultsPresentInDataResponse(url + "?taskCreatedBefore=" +getIsoDateStringWithoutSeconds(created.getTime()), 1, task1.getId());
            assertResultsPresentInDataResponse(url + "?taskCreatedBefore=" +getIsoDateStringWithoutMS(created.getTime()), 1, task1.getId());

            created.set(Calendar.YEAR, 2000);
            assertResultsPresentInDataResponse(url + "?taskCreatedAfter=" + getISODateString(created.getTime()), 3, task1.getId(), task2.getId());
            assertResultsPresentInDataResponse(url + "?taskCreatedAfter=" + getIsoDateStringWithoutSeconds(created.getTime()), 3, task1.getId(), task2.getId());
            assertResultsPresentInDataResponse(url + "?taskCreatedAfter=" + getIsoDateStringWithoutMS(created.getTime()), 3, task1.getId(), task2.getId());

            // Without tenant id
            assertResultsPresentInDataResponse(url + "?withoutTenantId=true", 2, task.getId(), task1.getId());

            // Tenant id
            assertResultsPresentInDataResponse(url + "?tenantId=myTenant", 1, task2.getId());
            assertResultsPresentInDataResponse(url + "?tenantId=anotherTenant", 0);

            // Tenant id like
            assertResultsPresentInDataResponse(url + "?tenantIdLike=" + encode("%enant"), 1, task2.getId());
            assertResultsPresentInDataResponse(url + "?tenantIdLike=anotherTenant", 0);
            
            // Without process instance id
            assertResultsPresentInDataResponse(url + "?withoutProcessInstanceId=true", 3, task.getId(), task1.getId(), task2.getId());

            assertResultsPresentInDataResponse(url + "?planItemInstanceId=" + task1.getSubScopeId(), 1, task1.getId());

        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testQueryTaskByCategory() throws Exception {
        Task t1 = taskService.newTask();
        t1.setName("t1");
        t1.setCategory("Cat 1");
        taskService.saveTask(t1);

        Task t2 = taskService.newTask();
        t2.setName("t2");
        t2.setCategory("Cat 2");
        taskService.saveTask(t2);

        Task t3 = taskService.newTask();
        t3.setName("t3");
        taskService.saveTask(t3);

        try {
            String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES);
            assertResultsPresentInDataResponse(url + "?taskCategory=" + encode("Cat 1"), 1, t1.getId());
            assertResultsPresentInDataResponse(url + "?taskCategoryIn=" + encode("Cat 1"), 1, t1.getId());
            assertResultsPresentInDataResponse(url + "?taskCategoryNotIn=" + encode("Cat 1"), 1, t2.getId());
            assertResultsPresentInDataResponse(url + "?taskWithoutCategory=true", 1, t3.getId());

        } finally {
            taskService.deleteTask(t1.getId(), true);
            taskService.deleteTask(t2.getId(), true);
            taskService.deleteTask(t3.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryTaskInstancesWithCandidateGroup() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES);

        assertResultsPresentInDataResponse(url + "?taskCandidateGroup=test", 1, task.getId());
        assertEmptyResultsPresentInDataResponse(url + "?taskCandidateGroup=notExisting");

        taskService.claim(task.getId(), "johnDoe");
        assertEmptyResultsPresentInDataResponse(url + "?taskCandidateGroup=test");
        assertResultsPresentInDataResponse(url + "?taskCandidateGroup=test&ignoreTaskAssignee=true", 1, task.getId());
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/PropagatedStageInstanceId.cmmn" })
    public void testQueryWithPropagatedStageId() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("propagatedStageInstanceId").start();
        HistoricTaskInstance task1 = historyService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();

        PlanItemInstance stageInstanceId1 = runtimeService.createPlanItemInstanceQuery()
            .onlyStages()
            .caseInstanceId(caseInstance.getId())
            .planItemDefinitionId("expandedStage2")
            .singleResult();
        assertThat(stageInstanceId1).isNotNull();

        taskService.complete(task1.getId());
        HistoricTaskInstance task2 = historyService.createHistoricTaskInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .unfinished()
            .singleResult();

        PlanItemInstance stageInstanceId2 = runtimeService.createPlanItemInstanceQuery()
            .onlyStages()
            .caseInstanceId(caseInstance.getId())
            .planItemDefinitionId("expandedStage3")
            .singleResult();
        assertThat(stageInstanceId2).isNotNull();

        taskService.complete(task2.getId());
        assertCaseEnded(caseInstance.getId());


        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES) + "?propagatedStageInstanceId=wrong";
        assertEmptyResultsPresentInDataResponse(url);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES) + "?propagatedStageInstanceId=" + stageInstanceId1.getId();
        assertResultsPresentInDataResponse(url, task1.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES) + "?propagatedStageInstanceId=" + stageInstanceId2.getId();
        assertResultsPresentInDataResponse(url, task2.getId());

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

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));

        String url = SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES) + "?rootScopeId="
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

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));


        String url = SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCES) + "?parentScopeId="
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


    protected void assertResultsPresentInDataResponse(String url, int numberOfResultsExpected, String... expectedTaskIds)
            throws JsonProcessingException, IOException {

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(dataNode).hasSize(numberOfResultsExpected);

        // Check presence of ID's
        if (expectedTaskIds != null) {
            List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedTaskIds));
            Iterator<JsonNode> it = dataNode.iterator();
            while (it.hasNext()) {
                String id = it.next().get("id").textValue();
                toBeFound.remove(id);
            }
            assertThat(toBeFound).as("Not all entries have been found in result, missing: " + StringUtils.join(toBeFound, ", ").isEmpty());
        }
    }
}
