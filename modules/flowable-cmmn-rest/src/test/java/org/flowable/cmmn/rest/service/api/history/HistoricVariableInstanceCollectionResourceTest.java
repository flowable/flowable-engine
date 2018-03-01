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
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for REST-operation related to the historic variable instance query resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricVariableInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic variable instance. GET cmmn-history/historic-variable-instances
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryVariableInstances() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(caseVariables).start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.setVariableLocal(task.getId(), "taskVariable", "test");

        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(caseVariables).start();

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_VARIABLE_INSTANCES);

        assertResultsPresentInDataResponse(url + "?variableName=stringVar", 2, "stringVar", "Azerty");

        assertResultsPresentInDataResponse(url + "?variableName=booleanVar", 2, "booleanVar", false);

        assertResultsPresentInDataResponse(url + "?variableName=booleanVar2", 0, null, null);

        assertResultsPresentInDataResponse(url + "?caseInstanceId=" + caseInstance.getId(), 4, "taskVariable", "test");

        assertResultsPresentInDataResponse(url + "?caseInstanceId=" + caseInstance.getId() + "&excludeTaskVariables=true", 3, "intVar", 67890);

        assertResultsPresentInDataResponse(url + "?caseInstanceId=" + caseInstance2.getId(), 3, "stringVar", "Azerty");

        assertResultsPresentInDataResponse(url + "?taskId=" + task.getId(), 1, "taskVariable", "test");

        assertResultsPresentInDataResponse(url + "?taskId=" + task.getId() + "&variableName=booleanVar", 0, null, null);

        assertResultsPresentInDataResponse(url + "?variableNameLike=" + encode("%Var"), 6, "stringVar", "Azerty");

        assertResultsPresentInDataResponse(url + "?variableNameLike=" + encode("%Var2"), 0, null, null);
    }

    protected void assertResultsPresentInDataResponse(String url, int numberOfResultsExpected, String variableName, Object variableValue) throws JsonProcessingException, IOException {

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertEquals(numberOfResultsExpected, dataNode.size());

        // Check presence of ID's
        if (variableName != null) {
            boolean variableFound = false;
            Iterator<JsonNode> it = dataNode.iterator();
            while (it.hasNext()) {
                JsonNode dataElementNode = it.next();
                JsonNode variableNode = dataElementNode.get("variable");
                String name = variableNode.get("name").textValue();
                if (variableName.equals(name)) {
                    variableFound = true;
                    if (variableValue instanceof Boolean) {
                        assertTrue("Variable value is not equal", variableNode.get("value").asBoolean() == (Boolean) variableValue);
                    } else if (variableValue instanceof Integer) {
                        assertEquals("Variable value is not equal", variableNode.get("value").asInt(), (int) (Integer) variableValue);
                    } else {
                        assertEquals("Variable value is not equal", variableNode.get("value").asText(), (String) variableValue);
                    }
                }
            }
            assertTrue("Variable " + variableName + " is missing", variableFound);
        }
    }
}
