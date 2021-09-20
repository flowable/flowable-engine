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
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
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
 * Test for REST-operation related to get and delete a historic process instance.
 *
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class HistoricProcessInstanceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test retrieval of historic process instance. GET history/historic-process-instances/{processInstanceId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetProcessInstance() throws Exception {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .businessKey("myBusinessKey")
                .callbackId("testCallbackId")
                .callbackType("testCallbackType")
                .referenceId("testReferenceId")
                .referenceType("testReferenceType")
                .stageInstanceId("testStageInstanceId")
                .start();
        
        runtimeService.updateBusinessStatus(processInstance.getId(), "myBusinessStatus");

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE, processInstance.getId())),
                HttpStatus.SC_OK);

        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processInstance.getId() + "',"
                        + "businessKey: 'myBusinessKey',"
                        + "businessStatus: 'myBusinessStatus',"
                        + "callbackId: 'testCallbackId',"
                        + "callbackType: 'testCallbackType',"
                        + "referenceId: 'testReferenceId',"
                        + "referenceType: 'testReferenceType',"
                        + "propagatedStageInstanceId: 'testStageInstanceId'"
                        + "}");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        response = executeRequest(
                new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE, processInstance.getId())),
                HttpStatus.SC_NO_CONTENT);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
        closeResponse(response);
    }
}
