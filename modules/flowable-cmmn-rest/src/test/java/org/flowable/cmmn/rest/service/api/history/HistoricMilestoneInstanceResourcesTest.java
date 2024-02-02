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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for REST-operation related to get historic milestone
 *
 * @author DennisFederico
 */
public class HistoricMilestoneInstanceResourcesTest extends BaseSpringRestTestCase {

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/history/caseWithOneMilestone.cmmn" })
    public void testHistoricMilestoneInstanceResource() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();

        PlanItemInstance activateMilestoneEvent = runtimeService.createPlanItemInstanceQuery().planItemInstanceElementId("activateMilestoneEvent")
                .singleResult();
        assertThat(activateMilestoneEvent).isNotNull();
        runtimeService.triggerPlanItemInstance(activateMilestoneEvent.getId());

        HistoricMilestoneInstance runtimeMilestone = historyService.createHistoricMilestoneInstanceQuery().singleResult();
        assertThat(runtimeMilestone).isNotNull();
        HttpGet milestoneHttpGet = new HttpGet(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCE, runtimeMilestone.getId()));
        CloseableHttpResponse response = executeRequest(milestoneHttpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertHistoricMilestoneValues(runtimeMilestone, responseNode);

        //Finish the case
        runtimeService
                .triggerPlanItemInstance(runtimeService.createPlanItemInstanceQuery().planItemInstanceElementId("finishCaseEvent").singleResult().getId());
        assertCaseEnded(caseInstance.getId());
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/history/caseWithTwoMilestones.cmmn" })
    public void testHistoricMilestoneInstanceCollectionResource() throws Exception {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithTwoMilestones").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithTwoMilestones").start();

        final String baseUrl = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCES);

        //At first the history is empty until the a milestone is reach
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl);
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).isEmpty();

        //Case setup... two milestones, each waiting for a user event
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.DECEMBER, 12, 1, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //Trigger the events at different times, interleaved by case
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        PlanItemInstance event = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance1.getId())
                .planItemInstanceElementId("activateMilestone1").singleResult();
        runtimeService.triggerPlanItemInstance(event.getId());

        calendar.set(Calendar.HOUR, 2);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        event = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance2.getId()).planItemInstanceElementId("activateMilestone2")
                .singleResult();
        runtimeService.triggerPlanItemInstance(event.getId());

        calendar.set(Calendar.HOUR, 3);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        PlanItemInstance activateMilestone2 = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance1.getId())
                .planItemInstanceElementId("activateMilestone2").singleResult();
        runtimeService.triggerPlanItemInstance(activateMilestone2.getId());

        calendar.set(Calendar.HOUR, 4);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        event = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance2.getId()).planItemInstanceElementId("activateMilestone1")
                .singleResult();
        runtimeService.triggerPlanItemInstance(event.getId());

        //There should be two milestones completed by case instance
        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl + "?caseInstanceId=" + caseInstance1.getId());
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(2);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("caseInstanceId").asText()).isEqualTo(caseInstance1.getId()));

        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl + "?caseInstanceId=" + caseInstance2.getId());
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(2);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("caseInstanceId").asText()).isEqualTo(caseInstance2.getId()));

        //There should be 4 milestones in general
        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl);
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(4);

        //sorted by timestamp
        List<HistoricMilestoneInstance> expected = historyService.createHistoricMilestoneInstanceQuery().orderByTimeStamp().asc().list();
        assertHistoricMilestoneValues(expected, responseNode.get("data"));

        //Check using before/after //DATE SHOULD BE SENT AS UTC
        calendar.set(2017, Calendar.DECEMBER, 12, 1, 30, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date reachAfter = calendar.getTime();
        calendar.set(Calendar.HOUR, 3);
        Date reachBefore = calendar.getTime();

        httpGet = new HttpGet(SERVER_URL_PREFIX + baseUrl
                + "?reachedBefore=" + getISODateString(reachBefore)
                + "&reachedAfter=" + getISODateString(reachAfter)
        );
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(2);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("elementId").asText()).isEqualTo("milestonePlanItem2"));

        assertCaseEnded(caseInstance1.getId());
        assertCaseEnded(caseInstance2.getId());
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/history/caseWithTwoMilestones.cmmn" })
    public void testHistoricMilestoneInstanceQueryResource() throws Exception {
        CaseInstance caseInstance1 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithTwoMilestones").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithTwoMilestones").start();

        //At first the history is empty until the a milestone is reach
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCE_QUERY));
        ObjectNode requestNode = objectMapper.createObjectNode();
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).isEmpty();

        //Case setup... two milestones, each waiting for a user event
        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, Calendar.DECEMBER, 12, 1, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        //Trigger the events at different times, interleaved by case
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        PlanItemInstance event = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance1.getId())
                .planItemInstanceElementId("activateMilestone1").singleResult();
        runtimeService.triggerPlanItemInstance(event.getId());

        calendar.set(Calendar.HOUR, 2);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        event = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance2.getId()).planItemInstanceElementId("activateMilestone2")
                .singleResult();
        runtimeService.triggerPlanItemInstance(event.getId());

        calendar.set(Calendar.HOUR, 3);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        PlanItemInstance activateMilestone2 = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance1.getId())
                .planItemInstanceElementId("activateMilestone2").singleResult();
        runtimeService.triggerPlanItemInstance(activateMilestone2.getId());

        calendar.set(Calendar.HOUR, 4);
        cmmnEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        event = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance2.getId()).planItemInstanceElementId("activateMilestone1")
                .singleResult();
        runtimeService.triggerPlanItemInstance(event.getId());

        //There should be two milestones completed by case instance
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance1.getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(2);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("caseInstanceId").asText()).isEqualTo(caseInstance1.getId()));

        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance2.getId());
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(2);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("caseInstanceId").asText()).isEqualTo(caseInstance2.getId()));

        //There should be 4 milestones in the history
        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(4);

        //sorted by timestamp
        List<HistoricMilestoneInstance> expected = historyService.createHistoricMilestoneInstanceQuery().orderByTimeStamp().asc().list();
        assertHistoricMilestoneValues(expected, responseNode.get("data"));

        //Check using before/after //DATE SHOULD BE SENT AS UTC
        calendar.set(2017, Calendar.DECEMBER, 12, 1, 30, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date reachAfter = calendar.getTime();
        calendar.set(Calendar.HOUR, 3);
        Date reachBefore = calendar.getTime();

        httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCE_QUERY));
        requestNode = objectMapper.createObjectNode();
        requestNode.put("reachedBefore", getISODateString(reachBefore));
        requestNode.put("reachedAfter", getISODateString(reachAfter));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("data")).hasSize(2);
        StreamSupport.stream(responseNode.get("data").spliterator(), false)
                .forEach(n -> assertThat(n.get("elementId").asText()).isEqualTo("milestonePlanItem2"));

        assertCaseEnded(caseInstance1.getId());
        assertCaseEnded(caseInstance2.getId());

    }

    private void assertHistoricMilestoneValues(List<HistoricMilestoneInstance> expected, JsonNode actual) {
        if (expected.size() != actual.size()) {
            for (int i = 0; i < expected.size(); i++) {
                assertHistoricMilestoneValues(expected.get(i), actual.get(i));
            }
        }
    }

    private void assertHistoricMilestoneValues(HistoricMilestoneInstance expected, JsonNode actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.get("id").textValue()).isEqualTo(expected.getId());
        assertThat(actual.get("name").textValue()).isEqualTo(expected.getName());
        assertThat(actual.get("elementId").textValue()).isEqualTo(expected.getElementId());
        assertThat(actual.get("timestamp").asText()).isEqualTo(getISODateStringWithTZ(expected.getTimeStamp()));
        assertThat(actual.get("caseInstanceId").textValue()).isEqualTo(expected.getCaseInstanceId());
        assertThat(actual.get("caseDefinitionId").textValue()).isEqualTo(expected.getCaseDefinitionId());

        assertThatCode(() -> {
            assertThat(actual.get("url").textValue()).isNotNull();
            String url = URI.create(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_MILESTONE_INSTANCE, expected.getId()))
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
