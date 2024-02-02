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
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to historic case instances.
 *
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HistoricCaseInstanceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test retrieval of historic case instances. GET cmmn-history/historic-case-instances/{caseInstanceId}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testGetCaseInstance() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("myBusinessKey")
                .referenceId("testReferenceId")
                .referenceType("testReferenceType")
                .callbackId("testCallbackId")
                .callbackType("testCallbackType")
                .start();
        
        runtimeService.updateBusinessStatus(caseInstance.getId(), "myBusinessStatus");

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(
                CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId())), HttpStatus.SC_OK);

        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + caseInstance.getId() + "',"
                        + " businessKey: 'myBusinessKey',"
                        + " businessStatus: 'myBusinessStatus',"
                        + " referenceId: 'testReferenceId',"
                        + " referenceType: 'testReferenceType',"
                        + " callbackId: 'testCallbackId',"
                        + " callbackType: 'testCallbackType'"
                        + "}");

        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        response = executeRequest(
                new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId())),
                HttpStatus.SC_NO_CONTENT);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
        closeResponse(response);
    }

    @CmmnDeployment
    public void testStageOverview() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testStageOverview")
                .variable("includeInOverview", true)
                .variable("includeInOverviewNegative", false)
                .start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(2), "Stage four", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(9, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(2), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage four", false, true);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage four", true, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(6, ChronoUnit.HOURS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage four", true, false);

        assertThat(historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).unfinished().count()).isZero();
        assertThat(historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).finished().count()).isEqualTo(1);
    }

    @CmmnDeployment
    public void testStageOverviewWithExclusions() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageOverview").start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(2);
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage four", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(9, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(2);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(2);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(2);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage four", false, true);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(2);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage four", true, false);

        assertThat(historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).unfinished().count()).isZero();
        assertThat(historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).finished().count()).isEqualTo(1);
    }

    @CmmnDeployment
    public void testStageOverviewWithoutDisplayOrder() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageOverview").start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(4);
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(9, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(4);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(11, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(4);
        assertStage(stageOverviewResponse.get(0), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, true);
        assertStage(stageOverviewResponse.get(3), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(4);
        assertStage(stageOverviewResponse.get(0), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false, true);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(4);
        assertStage(stageOverviewResponse.get(0), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", true, false);

        assertThat(historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).unfinished().count()).isZero();
        assertThat(historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).finished().count()).isEqualTo(1);
    }

    @CmmnDeployment
    public void testStageAndMilestoneOverview() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testStageOverview")
                .variable("includeInOverview", true)
                .variable("includeInOverviewNegative", false)
                .start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(7);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(4), "Milestone four", false, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", false, false);
        assertStage(stageOverviewResponse.get(6), "Milestone five", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(7);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(4), "Milestone four", false, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", false, false);
        assertStage(stageOverviewResponse.get(6), "Milestone five", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(7);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(4), "Milestone four", true, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", false, false);
        assertStage(stageOverviewResponse.get(6), "Milestone five", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(6);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(4), "Milestone four", true, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", false, true);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(6);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(4), "Milestone four", true, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", true, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(6, ChronoUnit.HOURS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(6);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(4), "Milestone four", true, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", true, false);

        assertThat(historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).unfinished().count()).isZero();
        assertThat(historyService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).finished().count()).isEqualTo(1);
    }

    protected ArrayNode getStageOverviewResponse(CaseInstance caseInstance) throws IOException {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(
                CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_STAGE_OVERVIEW, caseInstance.getId())), HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThat(responseNode.isArray()).isTrue();
        return (ArrayNode) responseNode;
    }

    protected void assertStage(JsonNode jsonNode, String name, boolean isEnded, boolean isCurrent) {
        assertThatJson(jsonNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    name: '" + name + "',"
                        + "    ended: " + isEnded + ","
                        + "    current: " + isCurrent
                        + "}");
    }
}
