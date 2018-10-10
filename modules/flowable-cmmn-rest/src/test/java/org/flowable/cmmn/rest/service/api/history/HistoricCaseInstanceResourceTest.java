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
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(
                        CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId())), HttpStatus.SC_OK);

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(caseInstance.getId(), responseNode.get("id").textValue());

        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        response = executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE, caseInstance.getId())), HttpStatus.SC_NO_CONTENT);
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
        closeResponse(response);
    }

    @CmmnDeployment
    public void testStageOverview() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageOverview").start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertEquals(4, stageOverviewResponse.size());
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertEquals(4, stageOverviewResponse.size());
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertEquals(4, stageOverviewResponse.size());
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, true);
        assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);
    }
    
    @CmmnDeployment
    public void testStageOverviewWithOnlyRuntimeData() throws Exception {
        HistoryLevel historyLevel = cmmnEngineConfiguration.getHistoryLevel();
        cmmnEngineConfiguration.setHistoryLevel(HistoryLevel.NONE);
        try {
            CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageOverview").start();
    
            cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));
    
            ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
            assertEquals(4, stageOverviewResponse.size());
            assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
            assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
            assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
            assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);
    
            // We're doing a wrong time ordering, to test that the display order has precedence over the end time
            cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
            taskService.complete(taskService.createTaskQuery().singleResult().getId());
    
            stageOverviewResponse = getStageOverviewResponse(caseInstance);
            assertEquals(4, stageOverviewResponse.size());
            assertStage(stageOverviewResponse.get(0), "Stage one", false, false);
            assertStage(stageOverviewResponse.get(1), "Stage two", false, true);
            assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
            assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);
    
            cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
            taskService.complete(taskService.createTaskQuery().singleResult().getId());
    
            stageOverviewResponse = getStageOverviewResponse(caseInstance);
            assertEquals(4, stageOverviewResponse.size());
            assertStage(stageOverviewResponse.get(0), "Stage one", false, false);
            assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
            assertStage(stageOverviewResponse.get(2), "Stage three", false, true);
            assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);
        } finally {
            cmmnEngineConfiguration.setHistoryLevel(historyLevel);
        }
    }

    @CmmnDeployment
    public void testStageOverviewWithoutDisplayOrder() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageOverview").start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertEquals(4, stageOverviewResponse.size());
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertEquals(4, stageOverviewResponse.size());
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(9, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertEquals(4, stageOverviewResponse.size());
        assertStage(stageOverviewResponse.get(0), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, true);
        assertStage(stageOverviewResponse.get(3), "Stage four", false,  false);
    }

    protected ArrayNode getStageOverviewResponse(CaseInstance caseInstance) throws IOException {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(
            CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_STAGE_OVERVIEW, caseInstance.getId())), HttpStatus.SC_OK);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertTrue(responseNode.isArray());
        return (ArrayNode) responseNode;
    }

    protected void assertStage(JsonNode jsonNode, String name, boolean isEnded, boolean isCurrent) {
        assertEquals(name, jsonNode.get("name").asText());
        assertEquals("ended boolean is wrong", isEnded, jsonNode.get("ended").asBoolean());
        assertEquals("current boolean is wrong", isCurrent, jsonNode.get("current").asBoolean());
    }

}
