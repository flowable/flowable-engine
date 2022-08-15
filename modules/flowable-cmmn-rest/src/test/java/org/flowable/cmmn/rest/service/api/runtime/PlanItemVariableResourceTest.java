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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single plan item variable resource.
 *
 * @author Christopher Welsch
 */
public class PlanItemVariableResourceTest extends BaseSpringRestTestCase {

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testUpdatePlanItemInstanceVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();

        List<PlanItemInstance> planItems = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItems).hasSize(1);
        PlanItemInstance planItem = planItems.get(0);

        runtimeService.setLocalVariable(planItem.getId(), "testLocalVar", "testVarValue");

        String url = buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE, planItem.getId(), "testLocalVar");
        ObjectNode body = objectMapper.createObjectNode();

        body.put("name", "testLocalVar");
        body.put("value", "testVarValue");
        body.put("type", "string");

        HttpPut putRequest = new HttpPut(url);
        putRequest.setEntity(new StringEntity(body.toString()));
        CloseableHttpResponse response = executeRequest(putRequest, HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   value: 'testVarValue',"
                        + "   scope: 'local'"
                        + "}");

        // Check resulting instance
        assertThat(runtimeService.getLocalVariable(planItem.getId(), "testLocalVar")).isEqualTo("testVarValue");

    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testUpdatePlanItemInstanceVariableExceptions() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();

        List<PlanItemInstance> planItems = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItems).hasSize(1);
        PlanItemInstance planItem = planItems.get(0);

        runtimeService.setLocalVariable(planItem.getId(), "testLocalVar", "testVarValue");

        ObjectNode requestNode = objectMapper.createObjectNode();

        requestNode.put("name", "unexistingVariable");
        requestNode.put("value", "testVarValue");
        requestNode.put("type", "string");

        HttpPut httpPut = new HttpPut(buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE, planItem.getId(), "testLocalVar"));

        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST);
        closeResponse(response);

        httpPut = new HttpPut(buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE, planItem.getId(), "unexistingVariable"));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPut, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
        // Check resulting instance

    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDeleteExecutionVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();

        List<PlanItemInstance> planItems = runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItems).hasSize(1);
        PlanItemInstance planItem = planItems.get(0);

        runtimeService.setLocalVariable(planItem.getId(), "testLocalVar", "testVarValue");

        assertThat(runtimeService.hasLocalVariable(planItem.getId(), "testLocalVar")).isTrue();

        HttpDelete httpDelete = new HttpDelete(buildUrl(CmmnRestUrls.URL_PLAN_ITEM_INSTANCE_VARIABLE, planItem.getId(), "testLocalVar"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        assertThat(runtimeService.hasLocalVariable(planItem.getId(), "testLocalVar")).isFalse();

        // Run the same delete again, variable is not there so 404 should be returned
        response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
