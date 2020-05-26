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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to the historic task instance identity links resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricTaskInstanceIdentityLinkCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic task instance. GET cmmn-history/historic-task-instances/{taskId}/identitylinks
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/twoHumanTaskCase.cmmn" })
    public void testGetIdentityLinks() throws Exception {
        HashMap<String, Object> caseVariables = new HashMap<>();
        caseVariables.put("stringVar", "Azerty");
        caseVariables.put("intVar", 67890);
        caseVariables.put("booleanVar", false);

        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(caseVariables).start();
        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        taskService.setAssignee(task.getId(), "fozzie");
        taskService.setOwner(task.getId(), "test");

        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_TASK_INSTANCE_IDENTITY_LINKS, task.getId());

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode linksArray = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        String taskId = task.getId();
        assertThatJson(linksArray)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo("["
                + "  {"
                + "    type: 'assignee',"
                + "    userId: 'fozzie',"
                + "    groupId: null,"
                + "    taskId: '" + taskId + "',"
                + "    taskUrl: '${json-unit.any-string}',"
                + "    caseInstanceId: null,"
                + "    caseInstanceUrl: null"
                + "  },"
                + "  {"
                + "    type: 'owner',"
                + "    userId: 'test',"
                + "    groupId: null,"
                + "    taskId: '" + taskId + "',"
                + "    taskUrl: '${json-unit.any-string}',"
                + "    caseInstanceId: null,"
                + "    caseInstanceUrl: null"
                + "  },"
                + "  {"
                + "    type: 'candidate',"
                + "    userId: 'test',"
                + "    groupId: null,"
                + "    taskId: '" + taskId + "',"
                + "    taskUrl: '${json-unit.any-string}',"
                + "    caseInstanceId: null,"
                + "    caseInstanceUrl: null"
                + "  },"
                + "  {"
                + "    type: 'candidate',"
                + "    userId: 'test2',"
                + "    groupId: null,"
                + "    taskId: '" + taskId + "',"
                + "    taskUrl: '${json-unit.any-string}',"
                + "    caseInstanceId: null,"
                + "    caseInstanceUrl: null"
                + "  },"
                + "  {"
                + "    type: 'candidate',"
                + "    userId: null,"
                + "    groupId: 'test',"
                + "    taskId: '" + taskId + "',"
                + "    taskUrl: '${json-unit.any-string}',"
                + "    caseInstanceId: null,"
                + "    caseInstanceUrl: null"
                + "  }"
                + "]");
    }
}
