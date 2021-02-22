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

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to a single plan item instance resource.
 *
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class PlanItemInstanceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single plan item instance.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetPlanItemInstance() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();

        List<PlanItemInstance> planItems = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItems).hasSize(1);
        PlanItemInstance planItem = planItems.get(0);

        String url = buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE, planItem.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        // Check resulting instance
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "  id: '" + planItem.getId() + "',"
                        + "  url: '" + buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE, planItem.getId()) + "',"
                        + "  name: 'The Task',"
                        + "  caseInstanceId: '" + caseInstance.getId() + "',"
                        + "  caseInstanceUrl: '" + buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, planItem.getCaseInstanceId()) + "',"
                        + "  caseDefinitionId: '" + caseInstance.getCaseDefinitionId() + "',"
                        + "  caseDefinitionUrl: '" + buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, planItem.getCaseDefinitionId()) + "',"
                        + "  derivedCaseDefinitionId: null,"
                        + "  derivedCaseDefinitionUrl: null,"
                        + "  stageInstanceId: null,"
                        + "  stageInstanceUrl: null,"
                        + "  planItemDefinitionId: 'theTask',"
                        + "  planItemDefinitionType: 'humantask',"
                        + "  state: 'active',"
                        + "  stage: false,"
                        + "  elementId: 'planItem1',"
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
                        + "  startUserId: null,"
                        + "  referenceId: '${json-unit.any-string}',"
                        + "  referenceType: 'cmmn-1.1-to-cmmn-1.1-child-human-task',"
                        + "  completable: false,"
                        + "  entryCriterionId: null,"
                        + "  exitCriterionId: null,"
                        + "  formKey: null,"
                        + "  extraValue: null,"
                        + "  tenantId: ''"
                        + "}");
    }

    /**
     * Test getting an unexisting plan item instance.
     */
    public void testGetUnexistingPlanItemInstance() {
        closeResponse(
                executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE, "unexistingpi")),
                        HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test action on a single plan item instance.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/oneManualActivationHumanTaskCase.cmmn" })
    public void testEnablePlanItem() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();

        PlanItemInstance planItem = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();

        String url = buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE, planItem.getId());
        HttpPut httpPut = new HttpPut(url);

        httpPut.setEntity(new StringEntity("{\"action\": \"enable\"}"));
        executeRequest(httpPut, HttpStatus.SC_OK);

        planItem = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItem.getState()).isEqualTo("active");
    }

    /**
     * Test action on a single plan item instance.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/oneManualActivationHumanTaskCase.cmmn" })
    public void testDisablePlanItem() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();

        PlanItemInstance planItem = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();

        String url = buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE, planItem.getId());
        HttpPut httpPut = new HttpPut(url);

        httpPut.setEntity(new StringEntity("{\"action\": \"disable\"}"));
        executeRequest(httpPut, HttpStatus.SC_NO_CONTENT);

        planItem = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItem).isNull();
    }

}
