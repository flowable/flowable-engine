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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for REST-operation related to the historic process instance identity links resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricProcessInstanceIdentityLinkCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * GET history/historic-process-instances/{processInstanceId}/identitylinks
     */
    @Test
    @Deployment
    public void testGetIdentityLinks() throws Exception {
        HashMap<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringVar", "Azerty");
        processVariables.put("intVar", 67890);
        processVariables.put("booleanVar", false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setOwner(task.getId(), "test");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_IDENTITY_LINKS, processInstance.getId());

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        JsonNode linksArray = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThatJson(linksArray)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("["
                        + "{"
                        + "    type: 'participant',"
                        + "    userId: 'test',"
                        + "    groupId: null,"
                        + "    taskId: null,"
                        + "    taskUrl: null,"
                        + "    processInstanceId: '" + processInstance.getId() + "',"
                        + "    processInstanceUrl: '${json-unit.any-string}'"
                        + "},"
                        + "{"
                        + "    type: 'participant',"
                        + "    userId: 'kermit',"
                        + "    groupId: null,"
                        + "    taskId: null,"
                        + "    taskUrl: null,"
                        + "    processInstanceId: '" + processInstance.getId() + "',"
                        + "    processInstanceUrl: '${json-unit.any-string}'"
                        + "},"
                        + "{"
                        + "    type: 'participant',"
                        + "    userId: 'fozzie',"
                        + "    groupId: null,"
                        + "    taskId: null,"
                        + "    taskUrl: null,"
                        + "    processInstanceId: '" + processInstance.getId() + "',"
                        + "    processInstanceUrl: '${json-unit.any-string}'"
                        + "}"
                        + "]");
    }
}
