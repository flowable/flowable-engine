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

package org.flowable.rest.service.api.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for REST-operation related to the historic process instance query resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricProcessInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic process instance based on variables. GET history/historic-process-instances
     */
    @Test
    @Deployment
    public void testQueryProcessInstances() throws Exception {
        Calendar startTime = Calendar.getInstance();
        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());

        HashMap<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringVar", "Azerty");
        processVariables.put("intVar", 67890);
        processVariables.put("booleanVar", false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey", processVariables);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        startTime.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "businessKey2");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCES);

        assertResultsPresentInDataResponse(url + "?finished=true", processInstance.getId());

        assertResultsPresentInDataResponse(url + "?finished=false", processInstance2.getId());

        assertResultsPresentInDataResponse(url + "?processDefinitionId=" + processInstance.getProcessDefinitionId(), processInstance.getId(), processInstance2.getId());

        assertResultsPresentInDataResponse(url + "?processDefinitionId=" + processInstance.getProcessDefinitionId() + "&finished=true", processInstance.getId());

        assertResultsPresentInDataResponse(url + "?processDefinitionKey=oneTaskProcess", processInstance.getId(), processInstance2.getId());
        
        assertResultsPresentInDataResponse(url + "?businessKey=businessKey", processInstance.getId());
        assertResultsPresentInDataResponse(url + "?businessKey=businessKey2", processInstance2.getId());
        
        assertResultsPresentInDataResponse(url + "?businessKeyLike=" + encode("business%"), processInstance.getId(), processInstance2.getId());

        
        // includeProcessVariables
        assertVariablesPresentInPostDataResponse(url, "?includeProcessVariables=false&processInstanceId=" + processInstance.getId(), processInstance.getId(), new HashMap<>());
        assertVariablesPresentInPostDataResponse(url, "?includeProcessVariables=true&processInstanceId=" + processInstance.getId(), processInstance.getId(), processVariables);

        // Without tenant ID, before setting tenant
        assertResultsPresentInDataResponse(url + "?withoutTenantId=true", processInstance.getId(), processInstance2.getId());

        // Set tenant on deployment
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));
        startTime.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", "myTenant");

        // Without tenant ID, after setting tenant
        assertResultsPresentInDataResponse(url + "?withoutTenantId=true", processInstance.getId(), processInstance2.getId());

        // Tenant id
        assertResultsPresentInDataResponse(url + "?tenantId=myTenant", processInstance3.getId());
        assertResultsPresentInDataResponse(url + "?tenantId=anotherTenant");

        // Tenant id like
        assertResultsPresentInDataResponse(url + "?tenantIdLike=" + encode("%enant"), processInstance3.getId());
        assertResultsPresentInDataResponse(url + "?tenantIdLike=anotherTenant");

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url + "?processDefinitionKey=oneTaskProcess&sort=startTime"), 200);

        // Check status and size
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertEquals(3, dataNode.size());
        assertEquals(processInstance.getId(), dataNode.get(0).get("id").asText());
        assertEquals(processInstance2.getId(), dataNode.get(1).get("id").asText());
        assertEquals(processInstance3.getId(), dataNode.get(2).get("id").asText());
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
            String id = it.next().get("id").textValue();
            toBeFound.remove(id);
        }
        assertTrue("Not all process instances have been found in result, missing: " + StringUtils.join(toBeFound, ", "), toBeFound.isEmpty());
    }

    private void assertVariablesPresentInPostDataResponse(String url, String queryParameters, String processInstanceId, Map<String, Object> expectedVariables) throws IOException {

        HttpGet httpPost = new HttpGet(SERVER_URL_PREFIX + url + queryParameters);
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertEquals(1, dataNode.size());
        JsonNode valueNode = dataNode.get(0);
        assertEquals(processInstanceId, valueNode.get("id").asText());

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
