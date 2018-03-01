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

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for REST-operation related to the historic case instance identity links resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceIdentityLinkCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * GET cmmn-history/historic-case-instances/{caseInstanceId}/identitylinks
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testGetIdentityLinks() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.setOwner(task.getId(), "test");
        taskService.setAssignee(task.getId(), "fozzie");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_CASE_INSTANCE_IDENTITY_LINKS, caseInstance.getId());

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        JsonNode linksArray = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals(3, linksArray.size());
        Map<String, JsonNode> linksMap = new HashMap<>();
        for (JsonNode linkNode : linksArray) {
            linksMap.put(linkNode.get("userId").asText(), linkNode);
        }
        JsonNode participantNode = linksMap.get("kermit");
        assertNotNull(participantNode);
        assertEquals("participant", participantNode.get("type").asText());
        assertEquals("kermit", participantNode.get("userId").asText());
        assertTrue(participantNode.get("groupId").isNull());
        assertTrue(participantNode.get("taskId").isNull());
        assertTrue(participantNode.get("taskUrl").isNull());
        assertEquals(caseInstance.getId(), participantNode.get("caseInstanceId").asText());
        assertNotNull(participantNode.get("caseInstanceUrl").asText());

        participantNode = linksMap.get("fozzie");
        assertNotNull(participantNode);
        assertEquals("participant", participantNode.get("type").asText());
        assertEquals("fozzie", participantNode.get("userId").asText());
        assertTrue(participantNode.get("groupId").isNull());
        assertTrue(participantNode.get("taskId").isNull());
        assertTrue(participantNode.get("taskUrl").isNull());
        assertEquals(caseInstance.getId(), participantNode.get("caseInstanceId").asText());
        assertNotNull(participantNode.get("caseInstanceUrl").asText());

        participantNode = linksMap.get("test");
        assertNotNull(participantNode);
        assertEquals("participant", participantNode.get("type").asText());
        assertEquals("test", participantNode.get("userId").asText());
        assertTrue(participantNode.get("groupId").isNull());
        assertTrue(participantNode.get("taskId").isNull());
        assertTrue(participantNode.get("taskUrl").isNull());
        assertEquals(caseInstance.getId(), participantNode.get("caseInstanceId").asText());
        assertNotNull(participantNode.get("caseInstanceUrl").asText());
    }
}
