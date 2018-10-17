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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.identity.Authentication;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to a single case instance resource.
 * 
 * @author Tijs Rademakers
 */
public class CaseInstanceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single case instance.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseInstance() throws Exception {
        Authentication.setAuthenticatedUserId("getCaseUser");
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();

        String url = buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        // Check resulting instance
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(caseInstance.getId(), responseNode.get("id").textValue());
        assertEquals("myBusinessKey", responseNode.get("businessKey").textValue());
        assertEquals("", responseNode.get("tenantId").textValue());
        assertThat(responseNode.get("startTime").textValue()).as("startTime").isNotNull();
        assertThat(responseNode.get("startUserId").textValue()).as("startUserId").isEqualTo("getCaseUser");

        assertEquals(responseNode.get("url").asText(), url);
        assertEquals(responseNode.get("caseDefinitionUrl").asText(), buildUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseInstance.getCaseDefinitionId()));

        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeployment().addClasspathResource(
                        "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();
        
        try {
            caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").tenantId("myTenant").start();
            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId())), HttpStatus.SC_OK);
    
            // Check resulting instance tenant id
            responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertNotNull(responseNode);
            assertEquals("myTenant", responseNode.get("tenantId").textValue());
            
        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    /**
     * Test getting an unexisting case instance.
     */
    public void testGetUnexistingCaseInstance() {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, "unexistingpi")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single case instance.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDeleteCaseInstance() {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").businessKey("myBusinessKey").start();
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId())), HttpStatus.SC_NO_CONTENT));

        // Check if process-instance is gone
        assertEquals(0, runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
    }

    /**
     * Test deleting an unexisting case instance.
     */
    public void testDeleteUnexistingCaseInstance() {
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE, "unexistini")), HttpStatus.SC_NOT_FOUND));
    }
    
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/runtime/testManualEvaluateCriteria.cmmn" })
    public void testEvaluateCriteria() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testManualEvaluateCriteria")
                .variable("someBean", new TestBean())
                .start();
        
        // Triggering the evaluation twice will satisfy the entry criterion for B
        assertEquals(1, runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        
        String url = buildUrl(CmmnRestUrls.URL_CASE_INSTANCE, caseInstance.getId());
        HttpPut httpPut = new HttpPut(url);
        
        httpPut.setEntity(new StringEntity("{\"action\": \"evaluateCriteria\"}"));
        executeRequest(httpPut, HttpStatus.SC_OK);
        
        assertEquals(1, runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        
        TestBean.RETURN_VALUE = true;
        executeRequest(httpPut, HttpStatus.SC_OK);
        
        assertEquals(2, runtimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
    }
    
    public static class TestBean implements Serializable {
        
        public static boolean RETURN_VALUE;
        
        public boolean isSatisfied() {
            return RETURN_VALUE;
        }
        
    }

}
