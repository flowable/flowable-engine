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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for REST-operation related to the historic case instance query resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic case instance based on variables. GET history/historic-process-instances
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testQueryCaseInstances() throws Exception {
        Calendar startTime = Calendar.getInstance();
        cmmnEngineConfiguration.getClock().setCurrentTime(startTime.getTime());

        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").variables(caseVariables).start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());

        startTime.add(Calendar.DAY_OF_YEAR, 1);
        cmmnEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCES);

        assertResultsPresentInDataResponse(url + "?finished=true", caseInstance.getId());

        assertResultsPresentInDataResponse(url + "?finished=false", caseInstance2.getId());

        assertResultsPresentInDataResponse(url + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId(), caseInstance.getId(), caseInstance2.getId());

        assertResultsPresentInDataResponse(url + "?caseDefinitionId=" + caseInstance.getCaseDefinitionId() + "&finished=true", caseInstance.getId());

        assertResultsPresentInDataResponse(url + "?caseDefinitionKey=oneHumanTaskCase", caseInstance.getId(), caseInstance2.getId());

        assertVariablesPresentInPostDataResponse(url, "?includeCaseVariables=false&caseInstanceId=" + caseInstance.getId(), caseInstance.getId(), new HashMap<>());
        assertVariablesPresentInPostDataResponse(url, "?includeCaseVariables=true&caseInstanceId=" + caseInstance.getId(), caseInstance.getId(), caseVariables);

        // Without tenant ID, before setting tenant
        assertResultsPresentInDataResponse(url + "?withoutTenantId=true", caseInstance.getId(), caseInstance2.getId());

        // Set tenant on deployment
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeployment().addClasspathResource(
                        "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();
        
        try {
            startTime.add(Calendar.DAY_OF_YEAR, 1);
            cmmnEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
            CaseInstance caseInstance3 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").tenantId("myTenant").start();
    
            // Without tenant ID, after setting tenant
            assertResultsPresentInDataResponse(url + "?withoutTenantId=true", caseInstance.getId(), caseInstance2.getId());
    
            // Tenant id
            assertResultsPresentInDataResponse(url + "?tenantId=myTenant", caseInstance3.getId());
            assertResultsPresentInDataResponse(url + "?tenantId=anotherTenant");
    
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url + "?caseDefinitionKey=oneHumanTaskCase&sort=startTime"), 200);
    
            // Check status and size
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertEquals(3, dataNode.size());
            assertEquals(caseInstance.getId(), dataNode.get(0).get("id").asText());
            assertEquals(caseInstance2.getId(), dataNode.get(1).get("id").asText());
            assertEquals(caseInstance3.getId(), dataNode.get(2).get("id").asText());
            
        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Override
    protected void assertResultsPresentInDataResponse(String url, String... expectedResourceIds) throws JsonProcessingException, IOException {
        int numberOfResultsExpected = expectedResourceIds.length;

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), 200);

        // Check status and size
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertEquals(numberOfResultsExpected, dataNode.size());

        // Check presence of ID's
        List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
        Iterator<JsonNode> it = dataNode.iterator();
        while (it.hasNext()) {
            JsonNode jsonNodeEntry = it.next();
            String id = jsonNodeEntry.get("id").textValue();
            String state = jsonNodeEntry.get("state").textValue();
            assertTrue("state is missing on the historic case instance", StringUtils.isNotEmpty(state));
            toBeFound.remove(id);
        }
        assertTrue("Not all process instances have been found in result, missing: " + StringUtils.join(toBeFound, ", "), toBeFound.isEmpty());
    }

    private void assertVariablesPresentInPostDataResponse(String url, String queryParameters, String caseInstanceId, Map<String, Object> expectedVariables) throws IOException {

        HttpGet httpPost = new HttpGet(SERVER_URL_PREFIX + url + queryParameters);
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertEquals(1, dataNode.size());
        JsonNode valueNode = dataNode.get(0);
        assertEquals(caseInstanceId, valueNode.get("id").asText());

        // Check expectec variables
        assertEquals(expectedVariables.size(), valueNode.get("variables").size());

        for(JsonNode node: valueNode.get("variables")) {
            ObjectNode variableNode = (ObjectNode) node;
            String variableName = variableNode.get("name").textValue();
            Object variableValue = objectMapper.convertValue(variableNode.get("value"), Object.class);

            assertTrue(expectedVariables.containsKey(variableName));
            assertEquals(expectedVariables.get(variableName), variableValue);
            assertEquals(expectedVariables.get(variableName).getClass().getSimpleName().toLowerCase(), variableNode.get("type").textValue());
            assertEquals("local", variableNode.get("scope").textValue());
        }

    }
}
