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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Test for REST-operation related to get historic plan item instances
 *
 * @author DennisFederico
 */
public class HistoricPlanItemInstanceResourcesTest extends BaseSpringRestTestCase {

    @CmmnDeployment(resources = {"org/flowable/cmmn/rest/service/api/history/caseWithOneMilestone.cmmn"})
    public void testSimpleHistoricPlanItemInstanceFlow() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();

        //Case should already contain history for 3 planItemInstances - CollectionResource
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCES));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(jsonResponse.get("data"));
        JsonNode jsonData = jsonResponse.get("data");
        assertEquals(3, jsonData.size());
        StreamSupport.stream(jsonData.spliterator(), false).forEach(n -> assertEquals(PlanItemInstanceState.AVAILABLE, n.get("state").asText()));

        //Trigger the event and check the history after the case ends
        PlanItemInstance activateMilestoneEvent = runtimeService.createPlanItemInstanceQuery().planItemInstanceElementId("activateMilestoneEvent").singleResult();
        assertNotNull(activateMilestoneEvent);
        runtimeService.triggerPlanItemInstance(activateMilestoneEvent.getId());

        //Check each planItem individually
        Map<String, JsonNode> planItemsByElementId = mapNodesBy("elementId", jsonData);
        String planItemInstanceId = planItemsByElementId.get("activateMilestoneEvent").get("id").asText();
        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, planItemInstanceId));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        JsonNode planItemInstanceNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(planItemInstanceNode);
        assertEquals(PlanItemInstanceState.COMPLETED, planItemInstanceNode.get("state").asText());

        planItemInstanceId = planItemsByElementId.get("milestonePlanItem1").get("id").asText();
        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, planItemInstanceId));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        planItemInstanceNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(planItemInstanceNode);
        assertEquals(PlanItemInstanceState.COMPLETED, planItemInstanceNode.get("state").asText());

        planItemInstanceId = planItemsByElementId.get("finishCaseEvent").get("id").asText();
        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, planItemInstanceId));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        planItemInstanceNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(planItemInstanceNode);
        assertEquals(PlanItemInstanceState.AVAILABLE, planItemInstanceNode.get("state").asText());

        //Finish the case
        runtimeService.triggerPlanItemInstance(runtimeService.createPlanItemInstanceQuery().planItemInstanceElementId("finishCaseEvent").singleResult().getId());
        assertCaseEnded(caseInstance.getId());

        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCES));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        jsonResponse = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(jsonResponse.get("data"));
        jsonData = jsonResponse.get("data");
        assertEquals(3, jsonData.size());
        StreamSupport.stream(jsonData.spliterator(), false).forEach(n -> assertEquals(PlanItemInstanceState.COMPLETED, n.get("state").asText()));
    }

    @CmmnDeployment(resources = {"org/flowable/cmmn/rest/service/api/history/caseWithStage.cmmn"})
    public void testHistoricPlanItemInstanceResource() {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithStage").start();

        //There are 3 planItems... check them by Id
        List<HistoricPlanItemInstance> historicPlanItems = historyService.createHistoricPlanItemInstanceQuery().list();
        assertEquals(3, historicPlanItems.size());
        historicPlanItems.forEach(p -> {
            try {
                HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, p.getId()));
                CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
                closeResponse(response);
                assertHistoricPlanItemValues(p, responseNode);
                String state = responseNode.get("state").asText();
                assertTrue(PlanItemInstanceState.ACTIVE.equals(state) || PlanItemInstanceState.AVAILABLE.equals(state));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });

        //Complete the task
        taskService.complete(taskService.createTaskQuery().active().singleResult().getId());

        //Check that plan item in history are completed
        historicPlanItems = historyService.createHistoricPlanItemInstanceQuery().list();
        assertEquals(3, historicPlanItems.size());
        historicPlanItems.forEach(p -> {
            try {
                HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, p.getId()));
                CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
                closeResponse(response);
                assertHistoricPlanItemValues(p, responseNode);
                assertEquals(PlanItemInstanceState.COMPLETED, responseNode.get("state").asText());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
        assertCaseEnded(caseInstance.getId());
    }

    @CmmnDeployment(resources = {"org/flowable/cmmn/rest/service/api/history/caseWithStage.cmmn"})
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
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(6, responseNode.get("data").size());

        //Three for each case Instance
        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl + "?caseInstanceId=" + caseInstance1.getId());
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(3, responseNode.get("data").size());
        StreamSupport.stream(responseNode.get("data").spliterator(), false).forEach(n -> assertEquals(caseInstance1.getId(), n.get("caseInstanceId").asText()));

        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl + "?caseInstanceId=" + caseInstance2.getId());
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(3, responseNode.get("data").size());
        StreamSupport.stream(responseNode.get("data").spliterator(), false).forEach(n -> assertEquals(caseInstance2.getId(), n.get("caseInstanceId").asText()));

        //End case instance one "normally"
        calendar.set(Calendar.HOUR_OF_DAY, 3);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        taskService.complete(taskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).active().singleResult().getId());
        assertCaseEnded(caseInstance1.getId());

        //Abort stage of case instance two
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        String abortEventId = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance2.getId()).planItemDefinitionId("abortStageEvent").singleResult().getId();
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
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(1, responseNode.get("data").size());

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
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(2, responseNode.get("data").size());
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertEquals(PlanItemInstanceState.TERMINATED, n.get("state").asText()));

        //For the sake of completeness, fetch all the planItems in "complete" state
        //and compare each with the result of the api call, default sort is by creation timestamp
        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl
                + "?planItemInstanceState=" + PlanItemInstanceState.COMPLETED
        );
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(4, responseNode.get("data").size());

        List<HistoricPlanItemInstance> listOfCompleted = historyService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.COMPLETED).list();
        assertHistoricPlanItemValues(listOfCompleted, responseNode.get("data"));

        assertCaseEnded(caseInstance1.getId());
        assertCaseEnded(caseInstance2.getId());
    }

    //Same as the previous test, but using query post
    @CmmnDeployment(resources = {"org/flowable/cmmn/rest/service/api/history/caseWithStage.cmmn"})
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

        final String baseUrl = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCES);

        //There should be 6 planItems
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        ObjectNode requestNode = objectMapper.createObjectNode();
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(6, responseNode.get("data").size());

        //Three for each case Instance
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance1.getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(3, responseNode.get("data").size());
        StreamSupport.stream(responseNode.get("data").spliterator(), false).forEach(n -> assertEquals(caseInstance1.getId(), n.get("caseInstanceId").asText()));

        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance2.getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(3, responseNode.get("data").size());
        StreamSupport.stream(responseNode.get("data").spliterator(), false).forEach(n -> assertEquals(caseInstance2.getId(), n.get("caseInstanceId").asText()));

        //End case instance one "normally"
        calendar.set(Calendar.HOUR_OF_DAY, 3);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        taskService.complete(taskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).active().singleResult().getId());
        assertCaseEnded(caseInstance1.getId());

        //Abort stage of case instance two
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        String abortEventId = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance2.getId()).planItemDefinitionId("abortStageEvent").singleResult().getId();
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
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(1, responseNode.get("data").size());

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
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(2, responseNode.get("data").size());
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertEquals(PlanItemInstanceState.TERMINATED, n.get("state").asText()));

        //For the sake of completeness, fetch all the planItems in "complete" state
        //and compare each with the result of the api call, default sort is by creation timestamp
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("planItemInstanceState", PlanItemInstanceState.COMPLETED);
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(4, responseNode.get("data").size());

        List<HistoricPlanItemInstance> listOfCompleted = historyService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.COMPLETED).list();
        assertHistoricPlanItemValues(listOfCompleted, responseNode.get("data"));

        assertCaseEnded(caseInstance1.getId());
        assertCaseEnded(caseInstance2.getId());
    }


    private Map<String, JsonNode> mapNodesBy(String attribute, JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false)
                .collect(Collectors.toMap(o -> o.get(attribute).asText(), o -> o));
    }

    private void assertHistoricPlanItemValues(List<HistoricPlanItemInstance> expected, JsonNode actual) {
        if (expected.size() != actual.size()) {
            for (int i = 0; i < expected.size(); i++) {
                assertHistoricPlanItemValues(expected.get(i), actual.get(i));
            }
        }
    }

    private void assertHistoricPlanItemValues(HistoricPlanItemInstance expected, JsonNode actual) {
        assertNotNull(actual);

        assertEquals(expected.getId(), actual.get("id").textValue());
        assertEquals(expected.getName(), actual.get("name").textValue());
        assertEquals(expected.getState(), actual.get("state").textValue());
        assertEquals(expected.getCaseDefinitionId(), actual.get("caseDefinitionId").textValue());
        assertEquals(expected.getCaseInstanceId(), actual.get("caseInstanceId").textValue());
        assertEquals(expected.getStageInstanceId(), actual.get("stageInstanceId").textValue());
        assertEquals(expected.getElementId(), actual.get("elementId").textValue());
        assertEquals(expected.getPlanItemDefinitionId(), actual.get("planItemDefinitionId").textValue());
        assertEquals(expected.getPlanItemDefinitionType(), actual.get("planItemDefinitionType").textValue());
        assertEquals(getISODateStringWithTZ(expected.getCreatedTime()), actual.get("createdTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getLastAvailableTime()), actual.get("lastAvailableTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getLastEnabledTime()), actual.get("lastEnabledTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getLastDisabledTime()), actual.get("lastDisabledTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getLastStartedTime()), actual.get("lastStartedTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getLastSuspendedTime()), actual.get("lastSuspendedTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getCompletedTime()), actual.get("completedTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getOccurredTime()), actual.get("occurredTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getTerminatedTime()), actual.get("terminatedTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getExitTime()), actual.get("exitTime").textValue());
        assertEquals(getISODateStringWithTZ(expected.getEndedTime()), actual.get("endedTime").textValue());
        assertEquals(expected.getStartUserId(), actual.get("startUserId").textValue());
        assertEquals(expected.getReferenceId(), actual.get("referenceId").textValue());
        assertEquals(expected.getReferenceType(), actual.get("referenceType").textValue());
        assertEquals(expected.getTenantId(), actual.get("tenantId").textValue());

        try {
            assertNotNull(actual.get("url").textValue());
            String url = URI.create(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_PLANITEM_INSTANCE, expected.getId())).toURL().toString();
            assertEquals(url, actual.get("url").textValue());
        } catch (MalformedURLException e) {
            fail("Cannot create url");
        }

        try {
            assertNotNull(actual.get("historicCaseInstanceUrl").textValue());
            CloseableHttpResponse response = executeRequest(new HttpGet(new URI(actual.get("historicCaseInstanceUrl").textValue())), HttpStatus.SC_OK);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            closeResponse(response);
        } catch (URISyntaxException e) {
            fail("Invalid historicCaseInstanceUrl: " + e.getMessage());
        }

        try {
            assertNotNull(actual.get("caseDefinitionUrl").textValue());
            CloseableHttpResponse response = executeRequest(new HttpGet(new URI(actual.get("caseDefinitionUrl").textValue())), HttpStatus.SC_OK);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            closeResponse(response);
        } catch (URISyntaxException e) {
            fail("Invalid caseDefinitionUrl: " + e.getMessage());
        }
    }
}
