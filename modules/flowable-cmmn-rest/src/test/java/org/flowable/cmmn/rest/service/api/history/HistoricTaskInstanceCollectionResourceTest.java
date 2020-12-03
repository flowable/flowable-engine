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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for REST-operation related to the historic task instance query resource.
 *
 * @author Tijs Rademakers
 */
public class HistoricTaskInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic task instance. GET cmmn-history/historic-task-instances
     */
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

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").businessKey("myBusinessKey").variables(caseVariables)
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

            assertResultsPresentInDataResponse(url, 3, task.getId(), task2.getId());

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

            assertResultsPresentInDataResponse(url + "?taskAssignee=kermit", 2, task2.getId());

            assertResultsPresentInDataResponse(url + "?taskAssigneeLike=" + encode("%mit"), 2, task2.getId());

            assertResultsPresentInDataResponse(url + "?taskAssignee=fozzie", 1, task.getId());

            assertResultsPresentInDataResponse(url + "?taskOwner=test", 1, task.getId());

            assertResultsPresentInDataResponse(url + "?taskOwnerLike=" + encode("t%"), 1, task.getId());

            assertResultsPresentInDataResponse(url + "?taskInvolvedUser=test", 1, task.getId());

            assertResultsPresentInDataResponse(url + "?dueDateAfter=" + longDateFormat.format(new GregorianCalendar(2010, 0, 1).getTime()), 1, task.getId());

            assertResultsPresentInDataResponse(url + "?dueDateAfter=" + longDateFormat.format(new GregorianCalendar(2013, 4, 1).getTime()), 0);

            assertResultsPresentInDataResponse(url + "?dueDateBefore=" + longDateFormat.format(new GregorianCalendar(2010, 0, 1).getTime()), 0);

            assertResultsPresentInDataResponse(url + "?dueDateBefore=" + longDateFormat.format(new GregorianCalendar(2013, 4, 1).getTime()), 1, task.getId());

            created.set(Calendar.YEAR, 2002);
            assertResultsPresentInDataResponse(url + "?taskCreatedBefore=" + longDateFormat.format(created.getTime()), 1, task1.getId());

            created.set(Calendar.YEAR, 2000);
            assertResultsPresentInDataResponse(url + "?taskCreatedAfter=" + longDateFormat.format(created.getTime()), 3, task1.getId(), task2.getId());

            // Without tenant id
            assertResultsPresentInDataResponse(url + "?withoutTenantId=true", 2, task.getId(), task1.getId());

            // Tenant id
            assertResultsPresentInDataResponse(url + "?tenantId=myTenant", 1, task2.getId());
            assertResultsPresentInDataResponse(url + "?tenantId=anotherTenant", 0);

            // Tenant id like
            assertResultsPresentInDataResponse(url + "?tenantIdLike=" + encode("%enant"), 1, task2.getId());
            assertResultsPresentInDataResponse(url + "?tenantIdLike=anotherTenant", 0);

        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

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
