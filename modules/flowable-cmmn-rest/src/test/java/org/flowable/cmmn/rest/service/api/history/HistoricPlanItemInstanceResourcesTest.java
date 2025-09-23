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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to get historic plan item instances
 *
 * @author DennisFederico
 * @author Filip Hrisafov
 */
public class HistoricPlanItemInstanceResourcesTest extends BaseSpringRestTestCase {

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/history/caseWithOneMilestone.cmmn" })
    public void testSimpleHistoricPlanItemInstanceFlow() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();

        //Case should already contain history for 3 planItemInstances - CollectionResource
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCES));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(jsonResponse.get("data")).isNotNull();
        JsonNode jsonData = jsonResponse.get("data");
        assertThat(jsonData).hasSize(3);
        StreamSupport.stream(jsonData.spliterator(), false).forEach(n -> assertThat(n.get("state").asText()).isEqualTo(PlanItemInstanceState.AVAILABLE));

        //Trigger the event and check the history after the case ends
        PlanItemInstance activateMilestoneEvent = runtimeService.createPlanItemInstanceQuery().planItemInstanceElementId("activateMilestoneEvent")
                .singleResult();
        assertThat(activateMilestoneEvent).isNotNull();
        runtimeService.triggerPlanItemInstance(activateMilestoneEvent.getId());

        //Check each planItem individually
        Map<String, JsonNode> planItemsByElementId = mapNodesBy("elementId", jsonData);
        String planItemInstanceId = planItemsByElementId.get("activateMilestoneEvent").get("id").asText();
        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, planItemInstanceId));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode planItemInstanceNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(planItemInstanceNode).isNotNull();
        assertThat(planItemInstanceNode.get("state").asText()).isEqualTo(PlanItemInstanceState.COMPLETED);

        planItemInstanceId = planItemsByElementId.get("milestonePlanItem1").get("id").asText();
        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, planItemInstanceId));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        planItemInstanceNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(planItemInstanceNode).isNotNull();
        assertThat(planItemInstanceNode.get("state").asText()).isEqualTo(PlanItemInstanceState.COMPLETED);

        planItemInstanceId = planItemsByElementId.get("finishCaseEvent").get("id").asText();
        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, planItemInstanceId));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        planItemInstanceNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(planItemInstanceNode).isNotNull();
        assertThat(planItemInstanceNode.get("state").asText()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //Finish the case
        runtimeService.triggerPlanItemInstance(runtimeService.createPlanItemInstanceQuery().planItemInstanceElementId("finishCaseEvent").singleResult().getId());
        assertCaseEnded(caseInstance.getId());

        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCES));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        jsonResponse = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(jsonResponse.get("data")).isNotNull();
        jsonData = jsonResponse.get("data");
        assertThat(jsonData).hasSize(3);
        StreamSupport.stream(jsonData.spliterator(), false).forEach(n -> assertThat(n.get("state").asText()).isEqualTo(PlanItemInstanceState.COMPLETED));
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/history/caseWithStage.cmmn" })
    public void testHistoricPlanItemInstanceResource() {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithStage").start();

        HistoricPlanItemInstance abortStageEvent = historyService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionId("abortStageEvent")
                .singleResult();

        assertThat(abortStageEvent).isNotNull();

        String abortStagePlanItemInstanceId = abortStageEvent.getId();
        JsonNode responseNode = getHistoricPlanItemInstanceResponse(abortStagePlanItemInstanceId);

        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "  id: '" + abortStagePlanItemInstanceId + "',"
                        + "  url: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, abortStagePlanItemInstanceId) + "',"
                        + "  name: null,"
                        + "  caseInstanceId: '" + caseInstance.getId() + "',"
                        + "  caseInstanceUrl: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId()) + "',"
                        + "  caseDefinitionId: '" + caseInstance.getCaseDefinitionId() + "',"
                        + "  caseDefinitionUrl: '" + buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()) + "',"
                        + "  derivedCaseDefinitionId: null,"
                        + "  derivedCaseDefinitionUrl: null,"
                        + "  stageInstanceId: null,"
                        + "  stageInstanceUrl: null,"
                        + "  planItemDefinitionId: 'abortStageEvent',"
                        + "  planItemDefinitionType: 'usereventlistener',"
                        + "  state: 'available',"
                        + "  stage: false,"
                        + "  elementId: 'planItemAbortStage',"
                        + "  createTime: '${json-unit.any-string}',"
                        + "  lastAvailableTime: '${json-unit.any-string}',"
                        + "  lastEnabledTime: null,"
                        + "  lastDisabledTime: null,"
                        + "  lastStartedTime: null,"
                        + "  lastSuspendedTime: null,"
                        + "  completedTime: null,"
                        + "  occurredTime: null,"
                        + "  terminatedTime: null,"
                        + "  exitTime: null,"
                        + "  endedTime: null,"
                        + "  lastUpdatedTime: '${json-unit.any-string}',"
                        + "  startUserId: null,"
                        + "  assignee: null,"
                        + "  completedBy: null,"
                        + "  referenceId: null,"
                        + "  referenceType: null,"
                        + "  entryCriterionId: null,"
                        + "  exitCriterionId: null,"
                        + "  formKey: null,"
                        + "  extraValue: null,"
                        + "  showInOverview: false,"
                        + "  tenantId: '',"
                        + "  localVariables: []"
                        + "}");

        HistoricPlanItemInstance stageOne = historyService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionId("stageOne")
                .singleResult();

        assertThat(stageOne).isNotNull();

        String stageOnePlanItemInstanceId = stageOne.getId();
        responseNode = getHistoricPlanItemInstanceResponse(stageOnePlanItemInstanceId);

        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "  id: '" + stageOnePlanItemInstanceId + "',"
                        + "  url: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, stageOnePlanItemInstanceId) + "',"
                        + "  name: null,"
                        + "  caseInstanceId: '" + caseInstance.getId() + "',"
                        + "  caseInstanceUrl: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId()) + "',"
                        + "  caseDefinitionId: '" + caseInstance.getCaseDefinitionId() + "',"
                        + "  caseDefinitionUrl: '" + buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()) + "',"
                        + "  derivedCaseDefinitionId: null,"
                        + "  derivedCaseDefinitionUrl: null,"
                        + "  stageInstanceId: null,"
                        + "  stageInstanceUrl: null,"
                        + "  planItemDefinitionId: 'stageOne',"
                        + "  planItemDefinitionType: 'stage',"
                        + "  state: 'active',"
                        + "  stage: true,"
                        + "  elementId: 'planItemStageOne',"
                        + "  createTime: '${json-unit.any-string}',"
                        + "  lastAvailableTime: '${json-unit.any-string}',"
                        + "  lastEnabledTime: null,"
                        + "  lastDisabledTime: null,"
                        + "  lastStartedTime: '${json-unit.any-string}',"
                        + "  lastSuspendedTime: null,"
                        + "  completedTime: null,"
                        + "  occurredTime: null,"
                        + "  terminatedTime: null,"
                        + "  exitTime: null,"
                        + "  endedTime: null,"
                        + "  lastUpdatedTime: '${json-unit.any-string}',"
                        + "  startUserId: null,"
                        + "  assignee: null,"
                        + "  completedBy: null,"
                        + "  referenceId: null,"
                        + "  referenceType: null,"
                        + "  entryCriterionId: null,"
                        + "  exitCriterionId: null,"
                        + "  formKey: null,"
                        + "  extraValue: null,"
                        + "  showInOverview: true,"
                        + "  tenantId: '',"
                        + "  localVariables: []"
                        + "}");

        HistoricPlanItemInstance manualTask = historyService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionId("myManualTask")
                .singleResult();

        assertThat(manualTask).isNotNull();

        String manualTaskPlanItemInstanceId = manualTask.getId();
        responseNode = getHistoricPlanItemInstanceResponse(manualTaskPlanItemInstanceId);

        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "  id: '" + manualTaskPlanItemInstanceId + "',"
                        + "  url: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, manualTaskPlanItemInstanceId) + "',"
                        + "  name: null,"
                        + "  caseInstanceId: '" + caseInstance.getId() + "',"
                        + "  caseInstanceUrl: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId()) + "',"
                        + "  caseDefinitionId: '" + caseInstance.getCaseDefinitionId() + "',"
                        + "  caseDefinitionUrl: '" + buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()) + "',"
                        + "  derivedCaseDefinitionId: null,"
                        + "  derivedCaseDefinitionUrl: null,"
                        + "  stageInstanceId: '" + stageOnePlanItemInstanceId + "',"
                        + "  stageInstanceUrl: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, stageOnePlanItemInstanceId) + "',"
                        + "  planItemDefinitionId: 'myManualTask',"
                        + "  planItemDefinitionType: 'humantask',"
                        + "  state: 'active',"
                        + "  stage: false,"
                        + "  elementId: 'planItemTask',"
                        + "  createTime: '${json-unit.any-string}',"
                        + "  lastAvailableTime: '${json-unit.any-string}',"
                        + "  lastEnabledTime: null,"
                        + "  lastDisabledTime: null,"
                        + "  lastStartedTime: '${json-unit.any-string}',"
                        + "  lastSuspendedTime: null,"
                        + "  completedTime: null,"
                        + "  occurredTime: null,"
                        + "  terminatedTime: null,"
                        + "  exitTime: null,"
                        + "  endedTime: null,"
                        + "  lastUpdatedTime: '${json-unit.any-string}',"
                        + "  startUserId: null,"
                        + "  assignee: null,"
                        + "  completedBy: null,"
                        + "  referenceId: '${json-unit.any-string}',"
                        + "  referenceType: 'cmmn-1.1-to-cmmn-1.1-child-human-task',"
                        + "  entryCriterionId: null,"
                        + "  exitCriterionId: null,"
                        + "  formKey: null,"
                        + "  extraValue: null,"
                        + "  showInOverview: false,"
                        + "  tenantId: '',"
                        + "  localVariables: []"
                        + "}");

        //Complete the task
        taskService.complete(taskService.createTaskQuery().active().singleResult().getId());

        //Check that plan items in history are completed
        responseNode = getHistoricPlanItemInstanceResponse(abortStagePlanItemInstanceId);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + abortStagePlanItemInstanceId + "',"
                        + "  url: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, abortStagePlanItemInstanceId) + "',"
                        + "  state: 'terminated',"
                        + "  elementId: 'planItemAbortStage',"
                        + "  createTime: '${json-unit.any-string}',"
                        + "  lastAvailableTime: '${json-unit.any-string}',"
                        + "  lastEnabledTime: null,"
                        + "  lastDisabledTime: null,"
                        + "  lastStartedTime: null,"
                        + "  lastSuspendedTime: null,"
                        + "  completedTime: null,"
                        + "  occurredTime: null,"
                        + "  terminatedTime: '${json-unit.any-string}',"
                        + "  exitTime: null,"
                        + "  endedTime: '${json-unit.any-string}',"
                        + "  lastUpdatedTime: '${json-unit.any-string}'"
                        + "}");

        responseNode = getHistoricPlanItemInstanceResponse(stageOnePlanItemInstanceId);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + stageOnePlanItemInstanceId + "',"
                        + "  url: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, stageOnePlanItemInstanceId) + "',"
                        + "  state: 'completed',"
                        + "  elementId: 'planItemStageOne',"
                        + "  createTime: '${json-unit.any-string}',"
                        + "  lastAvailableTime: '${json-unit.any-string}',"
                        + "  lastEnabledTime: null,"
                        + "  lastDisabledTime: null,"
                        + "  lastStartedTime: '${json-unit.any-string}',"
                        + "  lastSuspendedTime: null,"
                        + "  completedTime: '${json-unit.any-string}',"
                        + "  occurredTime: null,"
                        + "  terminatedTime: null,"
                        + "  exitTime: null,"
                        + "  endedTime: '${json-unit.any-string}',"
                        + "  lastUpdatedTime: '${json-unit.any-string}'"
                        + "}");

        responseNode = getHistoricPlanItemInstanceResponse(manualTaskPlanItemInstanceId);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + manualTaskPlanItemInstanceId + "',"
                        + "  url: '" + buildUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, manualTaskPlanItemInstanceId) + "',"
                        + "  state: 'completed',"
                        + "  elementId: 'planItemTask',"
                        + "  createTime: '${json-unit.any-string}',"
                        + "  lastAvailableTime: '${json-unit.any-string}',"
                        + "  lastEnabledTime: null,"
                        + "  lastDisabledTime: null,"
                        + "  lastStartedTime: '${json-unit.any-string}',"
                        + "  lastSuspendedTime: null,"
                        + "  completedTime: '${json-unit.any-string}',"
                        + "  occurredTime: null,"
                        + "  terminatedTime: null,"
                        + "  exitTime: null,"
                        + "  endedTime: '${json-unit.any-string}',"
                        + "  lastUpdatedTime: '${json-unit.any-string}'"
                        + "}");

        assertCaseEnded(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/history/caseWithStage.cmmn" })
    public void testHistoricPlanItemInstanceCollectionResource() throws Exception {
        //Set the clock for the first instance
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.DECEMBER, 12, 1, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithStage").start();

        calendar.set(Calendar.HOUR_OF_DAY, 2);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithStage").start();

        final String baseUrl = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCES);

        //There should be 6 planItems
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl);
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(6);

        //Three for each case Instance
        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl + "?caseInstanceId=" + caseInstance1.getId());
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(3);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("caseInstanceId").asText()).isEqualTo(caseInstance1.getId()));

        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl + "?caseInstanceId=" + caseInstance2.getId());
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(3);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("caseInstanceId").asText()).isEqualTo(caseInstance2.getId()));

        //End case instance one "normally"
        calendar.set(Calendar.HOUR_OF_DAY, 3);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        taskService.complete(taskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).active().singleResult().getId());
        assertCaseEnded(caseInstance1.getId());

        //Abort stage of case instance two
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        String abortEventId = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance2.getId()).planItemDefinitionId("abortStageEvent")
                .singleResult().getId();
        runtimeService.triggerPlanItemInstance(abortEventId);
        assertCaseEnded(caseInstance2.getId());

        //Check using before/after
        calendar.set(2017, Calendar.DECEMBER, 12, 0, 30, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date occurredAfter = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        Date occurredBefore = calendar.getTime();

        //Only one planItemInstance "occurred"
        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl
                + "?occurredBefore=" + getISODateString(occurredBefore)
                + "&occurredAfter=" + getISODateString(occurredAfter)
        );
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(1);

        //Two planItems exit with terminate state when the exitCriteria for the stage of the second case instance was met
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        Date exitAfter = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        Date exitBefore = calendar.getTime();
        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl
                + "?exitAfter=" + getISODateString(exitAfter)
                + "&exitBefore=" + getISODateString(exitBefore)
        );
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(2);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("state").asText()).isEqualTo(PlanItemInstanceState.TERMINATED));

        //For the sake of completeness, fetch all the planItems in "complete" state
        //and compare each with the result of the api call, default sort is by creation timestamp
        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl
                + "?planItemInstanceState=" + PlanItemInstanceState.COMPLETED
        );
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(3);

        List<HistoricPlanItemInstance> listOfCompleted = historyService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.COMPLETED).list();
        assertHistoricPlanItemValues(listOfCompleted, responseNode.get("data"));

        assertCaseEnded(caseInstance1.getId());
        assertCaseEnded(caseInstance2.getId());


        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl
                + "?caseInstanceIds=someCaseInstance," + caseInstance1.getId()
        );
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        assertThatJson(responseNode.get("data")).when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS).isEqualTo(
                """
                        [
                            {
                            caseInstanceId: "%s", "elementId": "planItemStageOne"},
                            {
                            caseInstanceId: "%s",  "elementId": "planItemAbortStage"},
                            {
                            caseInstanceId: "%s",  "elementId": "planItemTask"}
                        ]
                        """.replace("%s",caseInstance1.getId())
        );


    }

    /**
     * Test querying plan item instance and return local variables. POST query/planitem-instances
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryHistoricPlanItemInstancesWithLocalVariables() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();

        PlanItemInstance planItem = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        runtimeService.setLocalVariable(planItem.getId(), "someLocalVariable", "someLocalValue");
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY);

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("planItemInstanceId", planItem.getId());
        HttpPost post = new HttpPost(SERVER_URL_PREFIX + url);
        post.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(post, 200);
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);

        assertThatJson(dataNode).when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER).isEqualTo("["
                + "     {"
                + "         id : '" + planItem.getId() + "',"
                + "         caseInstanceId : '" + caseInstance.getId() + "',"
                + "         localVariables:["
                + "         ]"
                + "     }"
                + "]");

        requestNode.put("includeLocalVariables", true);
        post = new HttpPost(SERVER_URL_PREFIX + url);
        post.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(post, 200);
        dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);

        assertThatJson(dataNode).when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER).isEqualTo("["
                + "     {"
                + "         id : '" + planItem.getId() + "',"
                + "         caseInstanceId : '" + caseInstance.getId() + "',"
                + "         localVariables:[{"
                + "             name:'someLocalVariable',"
                + "             value:'someLocalValue',"
                + "             scope:'local'"
                + "         }]"
                + "     }"
                + "]");
    }

    //Same as the previous test, but using query post
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/history/caseWithStage.cmmn" })
    public void testHistoricPlanItemInstanceQueryResource() throws Exception {
        //Set the clock for the first instance
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.DECEMBER, 12, 1, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithStage").start();

        calendar.set(Calendar.HOUR_OF_DAY, 2);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithStage").start();

        //There should be 6 planItems
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        ObjectNode requestNode = objectMapper.createObjectNode();
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(6);

        //Three for each case Instance
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance1.getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(3);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("caseInstanceId").asText()).isEqualTo(caseInstance1.getId()));

        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance2.getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(3);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("caseInstanceId").asText()).isEqualTo(caseInstance2.getId()));

        //End case instance one "normally"
        calendar.set(Calendar.HOUR_OF_DAY, 3);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        taskService.complete(taskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).active().singleResult().getId());
        assertCaseEnded(caseInstance1.getId());

        //Abort stage of case instance two
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        String abortEventId = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance2.getId()).planItemDefinitionId("abortStageEvent")
                .singleResult().getId();
        runtimeService.triggerPlanItemInstance(abortEventId);
        assertCaseEnded(caseInstance2.getId());

        //Check using before/after
        calendar.set(2017, Calendar.DECEMBER, 12, 0, 30, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date occurredAfter = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        Date occurredBefore = calendar.getTime();

        //Only one planItemInstance "occurred"
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("occurredBefore", getISODateString(occurredBefore));
        requestNode.put("occurredAfter", getISODateString(occurredAfter));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(1);

        //Two planItems exit with terminate state when the exitCriteria for the stage of the second case instance was met
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        Date exitAfter = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        Date exitBefore = calendar.getTime();
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("exitAfter", getISODateString(exitAfter));
        requestNode.put("exitBefore", getISODateString(exitBefore));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(2);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("state").asText()).isEqualTo(PlanItemInstanceState.TERMINATED));

        //For the sake of completeness, fetch all the planItems in "complete" state
        //and compare each with the result of the api call, default sort is by creation timestamp
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("planItemInstanceState", PlanItemInstanceState.COMPLETED);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(3);

        List<HistoricPlanItemInstance> listOfCompleted = historyService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.COMPLETED).list();
        assertHistoricPlanItemValues(listOfCompleted, responseNode.get("data"));

        assertCaseEnded(caseInstance1.getId());
        assertCaseEnded(caseInstance2.getId());

        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.putArray("caseInstanceIds").add("someCaseInstanceID").add(caseInstance1.getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode.get("data")).when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS).isEqualTo(
                """
                        [
                            {
                            caseInstanceId: "%s", "elementId": "planItemStageOne"},
                            {
                            caseInstanceId: "%s",  "elementId": "planItemAbortStage"},
                            {
                            caseInstanceId: "%s",  "elementId": "planItemTask"}
                        ]
                        """.replace("%s",caseInstance1.getId())
        );


    }

    private Map<String, JsonNode> mapNodesBy(String attribute, JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false)
                .collect(Collectors.toMap(o -> o.get(attribute).asText(), o -> o));
    }

    protected JsonNode getHistoricPlanItemInstanceResponse(String planItemInstanceId) {
        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, planItemInstanceId));
        try (CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK)) {

            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            assertThat(response).isNotNull();
            return responseNode;
        } catch (IOException e) {
            throw new AssertionError("IO Exception for HTTP Connection", e);
        }
    }

    private void assertHistoricPlanItemValues(List<HistoricPlanItemInstance> expected, JsonNode actual) {
        if (expected.size() != actual.size()) {
            for (int i = 0; i < expected.size(); i++) {
                assertHistoricPlanItemValues(expected.get(i), actual.get(i));
            }
        }
    }

    private void assertHistoricPlanItemValues(HistoricPlanItemInstance expected, JsonNode actual) {
        assertThat(actual).isNotNull();

        assertThatJson(actual)
                .isEqualTo("{"
                        + "  id: '" + expected.getId() + "',"
                        + "  name: '" + expected.getName() + "',"
                        + "  state: '" + expected.getState() + "',"
                        + "  caseDefinitionId: '" + expected.getCaseDefinitionId() + "',"
                        + "  caseInstanceId: '" + expected.getCaseInstanceId() + "',"
                        + "  stageInstanceId: '" + expected.getStageInstanceId() + "',"
                        + "  elementId: '" + expected.getElementId() + "',"
                        + "  planItemDefinitionId: '" + expected.getPlanItemDefinitionId() + "',"
                        + "  planItemDefinitionType: '" + expected.getPlanItemDefinitionType() + "',"
                        + "  startUserId: '" + expected.getStartUserId() + "',"
                        + "  referenceId: '" + expected.getReferenceId() + "',"
                        + "  referenceType: '" + expected.getReferenceType() + "',"
                        + "  tenantId: '" + expected.getTenantId() + "',"
                        + "  createTime: '" + getISODateString(expected.getCreateTime()) + "',"
                        + "  lastAvailableTime: '" + getISODateString(expected.getLastAvailableTime()) + "',"
                        + "  lastEnabledTime: '" + getISODateString(expected.getLastEnabledTime()) + "',"
                        + "  lastDisabledTime: '" + getISODateString(expected.getLastDisabledTime()) + "',"
                        + "  lastStartedTime: '" + getISODateString(expected.getLastStartedTime()) + "',"
                        + "  lastSuspendedTime: '" + getISODateString(expected.getLastSuspendedTime()) + "',"
                        + "  completedTime: '" + getISODateString(expected.getCompletedTime()) + "',"
                        + "  occurredTime: '" + getISODateString(expected.getOccurredTime()) + "',"
                        + "  terminatedTime: '" + getISODateString(expected.getTerminatedTime()) + "',"
                        + "  exitTime: '" + getISODateString(expected.getExitTime()) + "',"
                        + "  endedTime: '" + getISODateString(expected.getEndedTime()) + "'"
                        + "}");

        assertThatCode(() -> {
            assertThat(actual.get("url").textValue()).isNotNull();
            String url = URI.create(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, expected.getId()))
                    .toURL().toString();
            assertThat(actual.get("url").textValue()).isEqualTo(url);
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            assertThat(actual.get("historicCaseInstanceUrl").textValue()).isNotNull();
            CloseableHttpResponse response = executeRequest(new HttpGet(new URI(actual.get("historicCaseInstanceUrl").textValue())), HttpStatus.SC_OK);
            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            closeResponse(response);
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            assertThat(actual.get("caseDefinitionUrl").textValue()).isNotNull();
            CloseableHttpResponse response = executeRequest(new HttpGet(new URI(actual.get("caseDefinitionUrl").textValue())), HttpStatus.SC_OK);
            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
            closeResponse(response);
        }).doesNotThrowAnyException();
    }
}
