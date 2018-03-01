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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for REST-operation related to the historic task instance query resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricTaskInstanceQueryResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic task instance. POST cmmn-query/historic-task-instances
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testQueryTaskInstances() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").businessKey("myBusinessKey").start();
        cmmnEngineConfiguration.getClock().setCurrentTime(new GregorianCalendar(2018, 0, 1).getTime());
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        Task finishedTaskCase1 = task;
        taskService.complete(task.getId());
        cmmnEngineConfiguration.getClock().setCurrentTime(null);
        task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        HashMap<String, Object> taskVariables = new HashMap<>();
        taskVariables.put("stringVar", "Azerty");
        taskVariables.put("intVar", 67890);
        taskVariables.put("booleanVar", false);
        taskVariables.put("local", "test");
        taskService.setVariablesLocal(task.getId(), taskVariables);
        taskService.setOwner(task.getId(), "test");
        taskService.setDueDate(task.getId(), new GregorianCalendar(2018, 0, 1).getTime());

        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task2 = taskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();
        taskService.setVariablesLocal(task2.getId(), taskVariables);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_INSTANCE_QUERY);

        // Process variables
        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode variableArray = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableArray.add(variableNode);
        requestNode.set("taskVariables", variableArray);

        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId(), task2.getId());

        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId(), task2.getId());

        variableNode.put("name", "intVar");
        variableNode.put("value", 67891);
        variableNode.put("operation", "lessThan");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId(), task2.getId());

        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "lessThan");
        assertResultsPresentInPostDataResponse(url, requestNode);

        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "lessThanOrEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId(), task2.getId());

        variableNode.put("name", "intVar");
        variableNode.put("value", 67889);
        variableNode.put("operation", "greaterThan");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId(), task2.getId());

        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "greaterThan");
        assertResultsPresentInPostDataResponse(url, requestNode);

        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "greaterThanOrEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId(), task2.getId());

        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azer%");
        variableNode.put("operation", "like");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId(), task2.getId());

        variableNode.put("name", "local");
        variableNode.put("value", "test");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId(), task2.getId());

        requestNode = objectMapper.createObjectNode();
        assertResultsPresentInPostDataResponse(url, requestNode, 3, task.getId(), task2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance.getId());
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("caseInstanceId", caseInstance2.getId());
        assertResultsPresentInPostDataResponse(url, requestNode, 1, task2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskAssignee", "kermit");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskAssigneeLike", "%mit");
        assertResultsPresentInPostDataResponse(url, requestNode, 2, task2.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskAssignee", "fozzie");
        assertResultsPresentInPostDataResponse(url, requestNode, 0);

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskOwner", "test");
        assertResultsPresentInPostDataResponse(url, requestNode, 1, task.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskOwnerLike", "t%");
        assertResultsPresentInPostDataResponse(url, requestNode, 1, task.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskInvolvedUser", "test");
        assertResultsPresentInPostDataResponse(url, requestNode, 1, task.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("dueDateAfter", longDateFormat.format(new GregorianCalendar(2015, 0, 1).getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, 1, task.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("dueDateAfter", longDateFormat.format(new GregorianCalendar(2018, 4, 1).getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, 0);

        requestNode = objectMapper.createObjectNode();
        requestNode.put("dueDateBefore", longDateFormat.format(new GregorianCalendar(2015, 0, 1).getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, 0);

        requestNode = objectMapper.createObjectNode();
        requestNode.put("dueDateBefore", longDateFormat.format(new GregorianCalendar(2018, 4, 1).getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, 1, task.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskCompletedAfter", longDateFormat.format(new GregorianCalendar(2015, 0, 1).getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, 1, finishedTaskCase1.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskCompletedAfter", longDateFormat.format(new GregorianCalendar(2018, 4, 1).getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, 0);

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskCompletedBefore", longDateFormat.format(new GregorianCalendar(2015, 0, 1).getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, 0);

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskCompletedAfter", longDateFormat.format(new GregorianCalendar(2015, 3, 1).getTime()));
        assertResultsPresentInPostDataResponse(url, requestNode, 1, finishedTaskCase1.getId());

        requestNode = objectMapper.createObjectNode();
        requestNode.put("taskDefinitionKey", "task1");
        assertResultsPresentInPostDataResponse(url, requestNode, finishedTaskCase1.getId(), task2.getId());
    }

    protected void assertResultsPresentInPostDataResponse(String url, ObjectNode body, int numberOfResultsExpected, String... expectedTaskIds) throws JsonProcessingException, IOException {
        // Do the actual call
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + url);
        httpPost.setEntity(new StringEntity(body.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertEquals(numberOfResultsExpected, dataNode.size());

        // Check presence of ID's
        if (expectedTaskIds != null) {
            List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedTaskIds));
            Iterator<JsonNode> it = dataNode.iterator();
            while (it.hasNext()) {
                String id = it.next().get("id").textValue();
                toBeFound.remove(id);
            }
            assertTrue("Not all entries have been found in result, missing: " + StringUtils.join(toBeFound, ", "), toBeFound.isEmpty());
        }
    }
}
