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
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.identity.Authentication;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single case instance resource.
 *
 * @author Tijs Rademakers
 */
public class CaseInstanceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single case instance.
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstance() throws Exception {
        Authentication.setAuthenticatedUserId("getCaseUser");
        cmmnEngineConfiguration.getClock()
                .setCurrentTime(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)));
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .businessKey("myBusinessKey")
                .referenceId("testReferenceId")
                .referenceType("testReferenceType")
                .callbackId("testCallbackId")
                .callbackType("testCallbackType")
                .start();
        
        runtimeService.updateBusinessStatus(caseInstance.getId(), "myBusinessStatus");

        String url = buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        // Check resulting instance
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
                        + " callbackType: 'testCallbackType',"
                        + " tenantId: '',"
                        + " startTime: '" + getISODateString(caseInstance.getStartTime()) + "',"
                        + " startUserId: 'getCaseUser',"
                        + " url: '" + url + "',"
                        + " caseDefinitionUrl: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()) + "'"
                        + "}");

        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeployment().addClasspathResource(
                "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();

        try {
            caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").tenantId("myTenant")
                    .start();
            response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId())),
                    HttpStatus.SC_OK);

            // Check resulting instance tenant id
            responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + " tenantId: 'myTenant'"
                            + "}");

        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    /**
     * Test getting an unexisting case instance.
     */
    public void testGetUnexistingCaseInstance() {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, "unexistingpi")),
                HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test terminating a single case instance.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testTerminateCaseInstance() {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();
        closeResponse(
                executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId())),
                        HttpStatus.SC_NO_CONTENT));

        // Check if process-instance is gone
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    /**
     * Test terminating an unexisting case instance.
     */
    public void testTerminateUnexistingCaseInstance() {
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, "unexisting")),
                HttpStatus.SC_NOT_FOUND));
    }
    
    /**
     * Test deleting a single case instance.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDeleteCaseInstance() {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();
        closeResponse(
                executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId(), "delete")),
                        HttpStatus.SC_NO_CONTENT));

        // Check if process-instance is gone
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    /**
     * Test deleting an unexisting case instance.
     */
    public void testDeleteUnexistingCaseInstance() {
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, "unexisting", "delete")),
                HttpStatus.SC_NOT_FOUND));
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/testManualEvaluateCriteria.cmmn" })
    public void testEvaluateCriteria() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testManualEvaluateCriteria")
                .variable("someBean", new TestBean())
                .start();

        // Triggering the evaluation twice will satisfy the entry criterion for B
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(1);

        String url = buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId());
        HttpPut httpPut = new HttpPut(url);

        httpPut.setEntity(new StringEntity("{\"action\": \"evaluateCriteria\"}"));
        executeRequest(httpPut, HttpStatus.SC_OK);

        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(1);

        TestBean.RETURN_VALUE = true;
        executeRequest(httpPut, HttpStatus.SC_OK);

        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count()).isEqualTo(2);
    }

    @CmmnDeployment
    public void testStageOverview() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testStageOverview")
                .variable("includeInOverview", true)
                .variable("includeInOverviewNegative", false)
                .start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(2), "Stage four", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(2), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage four", false, false);
    }

    @CmmnDeployment
    public void testStageOverviewWithExclusions() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageOverview").start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(2);
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage four", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
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
        assertStage(stageOverviewResponse.get(1), "Stage four", false, false);
    }

    @CmmnDeployment
    public void testStageOverviewWithoutDisplayOrder() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageOverview").start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(4);
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(4);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);
        assertStage(stageOverviewResponse.get(3), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(9, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(4);
        assertStage(stageOverviewResponse.get(0), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, true);
        assertStage(stageOverviewResponse.get(3), "Stage four", false, false);
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
        assertThat(stageOverviewResponse).hasSize(6);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(4), "Milestone four", false, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(6);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(4), "Milestone four", false, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(6);
        assertStage(stageOverviewResponse.get(0), "Milestone one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(2), "Milestone two", true, false);
        assertStage(stageOverviewResponse.get(3), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(4), "Milestone four", true, false);
        assertStage(stageOverviewResponse.get(5), "Stage four", false, false);
    }

    @CmmnDeployment
    public void testStageOverviewWithRepetition() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageOverviewWithRepetition").start();

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(5, ChronoUnit.DAYS)));

        ArrayNode stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", false, true);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);

        // We're doing a wrong time ordering, to test that the display order has precedence over the end time
        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", false, true);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, false);

        cmmnEngineConfiguration.getClock().setCurrentTime(Date.from(Instant.now().minus(9, ChronoUnit.DAYS)));
        taskService.complete(taskService.createTaskQuery().singleResult().getId());

        stageOverviewResponse = getStageOverviewResponse(caseInstance);
        assertThat(stageOverviewResponse).hasSize(3);
        assertStage(stageOverviewResponse.get(0), "Stage one", true, false);
        assertStage(stageOverviewResponse.get(1), "Stage two", true, false);
        assertStage(stageOverviewResponse.get(2), "Stage three", false, true);
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/oneHumanTaskCaseWithStartForm.cmmn" })
    public void testUpdateCaseInstance() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getName()).isNull();
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getBusinessKey()).isNull();

        // Triggering the evaluation twice will satisfy the entry criterion for B
        String url = buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId());

        HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity("{\"name\": \"test name one\"}"));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getName()).isEqualTo("test name one");
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getBusinessKey()).isNull();

        httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity("{\"businessKey\": \"test business key\"}"));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getName()).isEqualTo("test name one");
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getBusinessKey())
                .isEqualTo("test business key");

        httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity("{\"name\": \"test name two\"}"));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getName()).isEqualTo("test name two");
        assertThat(runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getBusinessKey())
                .isEqualTo("test business key");
    }
    
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/changeStateCase.cmmn" })
    public void testChangeStateCaseInstance() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .start();

        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(4);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        String url = buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_CHANGE_STATE, caseInstance.getId());
        
        ObjectNode changeStateNode = objectMapper.createObjectNode();
        ArrayNode definitionIds = changeStateNode.putArray("activatePlanItemDefinitionIds");
        definitionIds.add("stage1");

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(changeStateNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(4);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        changeStateNode = objectMapper.createObjectNode();
        ArrayNode terminateDefinitionIds = changeStateNode.putArray("terminatePlanItemDefinitionIds");
        terminateDefinitionIds.add("stageTask");
        
        httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(changeStateNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list()).hasSize(5);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").includeEnded().singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask").includeEnded().singleResult().getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
        
        changeStateNode = objectMapper.createObjectNode();
        ArrayNode availableDefinitionIds = changeStateNode.putArray("moveToAvailablePlanItemDefinitionIds");
        availableDefinitionIds.add("userEventListener1");
        availableDefinitionIds.add("stage1");
        
        httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(changeStateNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(4);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        UserEventListenerInstance userEventListener = runtimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        runtimeService.completeUserEventListenerInstance(userEventListener.getId());
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(4);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").list()).hasSize(0);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(1);
        
        changeStateNode = objectMapper.createObjectNode();
        ArrayNode addRepetitionDefinitionIds = changeStateNode.putArray("addWaitingForRepetitionPlanItemDefinitionIds");
        addRepetitionDefinitionIds.add("stage1");
        
        httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(changeStateNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(5);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(1);
        
        changeStateNode = objectMapper.createObjectNode();
        availableDefinitionIds = changeStateNode.putArray("moveToAvailablePlanItemDefinitionIds");
        availableDefinitionIds.add("userEventListener1");
        
        httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(changeStateNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(6);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(1);
        
        userEventListener = runtimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        runtimeService.completeUserEventListenerInstance(userEventListener.getId());
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(6);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").list()).hasSize(0);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).list()).hasSize(0);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
    }
    
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/changeStateRepetitionCase.cmmn" })
    public void testChangeStateCaseInstanceWithRepetition() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .start();

        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(4);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        UserEventListenerInstance userEventListener = runtimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        runtimeService.completeUserEventListenerInstance(userEventListener.getId());
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(6);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").planItemInstanceState(PlanItemInstanceState.AVAILABLE).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).list()).hasSize(1);
        
        userEventListener = runtimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        runtimeService.completeUserEventListenerInstance(userEventListener.getId());
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(8);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").planItemInstanceState(PlanItemInstanceState.AVAILABLE).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
        
        String url = buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_CHANGE_STATE, caseInstance.getId());
        
        ObjectNode changeStateNode = objectMapper.createObjectNode();
        ArrayNode removeRepetitionDefinitionIds = changeStateNode.putArray("removeWaitingForRepetitionPlanItemDefinitionIds");
        removeRepetitionDefinitionIds.add("stage1");
        
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(changeStateNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_OK));
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(7);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").planItemInstanceState(PlanItemInstanceState.AVAILABLE).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).list()).hasSize(0);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
        
        userEventListener = runtimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        runtimeService.completeUserEventListenerInstance(userEventListener.getId());
        
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(7);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").planItemInstanceState(PlanItemInstanceState.AVAILABLE).list()).hasSize(1);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).list()).hasSize(0);
        assertThat(runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask").planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
    }

    protected ArrayNode getStageOverviewResponse(CaseInstance caseInstance) throws IOException {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(
                CmmnRestUrls.URL_CASE_INSTANCE_STAGE_OVERVIEW, caseInstance.getId())), HttpStatus.SC_OK);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThat(responseNode.isArray()).isTrue();
        return (ArrayNode) responseNode;
    }

    protected void assertStage(JsonNode jsonNode, String name, boolean isEnded, boolean isCurrent) {
        assertThat(jsonNode.get("name").asText()).isEqualTo(name);
        assertThat(jsonNode.get("ended").asBoolean()).as("'ended' boolean is wrong").isEqualTo(isEnded);
        assertThat(jsonNode.get("current").asBoolean()).as("'current' boolean is wrong").isEqualTo(isCurrent);
    }

    public static class TestBean implements Serializable {

        public static boolean RETURN_VALUE;

        public boolean isSatisfied() {
            return RETURN_VALUE;
        }

    }

}
