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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.conf.ApplicationWithTaskLogging;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Luis Belloch
 */
@SpringJUnitWebConfig(ApplicationWithTaskLogging.class)
public class HistoricTaskLogCollectionResourceTest extends BaseSpringRestTestCase {

    @Test
    @Deployment(resources = { "org/flowable/rest/api/history/HistoricTaskLogCollectionResourceTest.bpmn20.xml" })
    public void itCanQueryByTaskId() throws Exception {
        Calendar startTime = Calendar.getInstance();
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "testBusinessKey");

        Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.setVariableLocal(task1.getId(), "local", "foo");
        taskService.setOwner(task1.getId(), "test");
        taskService.setDueDate(task1.getId(), startTime.getTime());

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "testBusinessKey");
        Task task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.setOwner(task2.getId(), "test");

        JsonNode list = queryTaskLogEntries("?taskId=" + encode(task1.getId()) + "&sort=logNumber&order=asc");
        expectSequence(list,
                asList(task1.getId(), task1.getId(), task1.getId()),
                asList("USER_TASK_CREATED", "USER_TASK_OWNER_CHANGED", "USER_TASK_DUEDATE_CHANGED"));
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/api/history/HistoricTaskLogCollectionResourceTest.bpmn20.xml" })
    public void whenTaskIdDoesNotExistItReturnsEmptyList() throws Exception {
        JsonNode list = queryTaskLogEntries("?taskId=FOOBAR_4242");
        assertThat(list).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/api/history/HistoricTaskLogCollectionResourceTest.bpmn20.xml" })
    public void itCanQueryByProcessInstanceId() throws Exception {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "testBusinessKey");
        Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.complete(task1.getId());
        Task task2 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "testBusinessKey");
        Task task3 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();

        JsonNode list1 = queryTaskLogEntries("?processInstanceId=" + encode(processInstance1.getId()) + "&sort=logNumber&order=asc");
        expectSequence(list1, asList(task1.getId(), task1.getId(), task2.getId()), asList("USER_TASK_CREATED", "USER_TASK_COMPLETED", "USER_TASK_CREATED"));

        JsonNode list2 = queryTaskLogEntries("?processInstanceId=" + encode(processInstance2.getId()) + "&sort=logNumber&order=asc");
        expectSequence(list2, asList(task3.getId()), asList("USER_TASK_CREATED"));
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/api/history/HistoricTaskLogCollectionResourceTest.bpmn20.xml" })
    public void itCanQueryUsingFromToDates() throws IOException {
        Calendar startTime = Calendar.getInstance();
        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());

        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "testBusinessKey");

        Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.setDueDate(task1.getId(), startTime.getTime());

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", "testBusinessKey");
        Task task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.setOwner(task2.getId(), "test");

        startTime.add(Calendar.DAY_OF_YEAR, 2);
        processEngineConfiguration.getClock().setCurrentTime(startTime.getTime());

        taskService.setOwner(task1.getId(), "test");

        processEngineConfiguration.getClock().reset();

        JsonNode listAfter = queryTaskLogEntries(
                "?taskId=" + encode(task1.getId()) + "&sort=logNumber&order=asc&from=" + getISODateString(startTime.getTime()));
        expectSequence(listAfter,
                asList(task1.getId()),
                asList("USER_TASK_OWNER_CHANGED"));

        startTime.add(Calendar.DAY_OF_YEAR, -1);
        JsonNode listBefore = queryTaskLogEntries(
                "?taskId=" + encode(task1.getId()) + "&sort=logNumber&order=asc&to=" + getISODateString(startTime.getTime()));
        expectSequence(listBefore,
                asList(task1.getId(), task1.getId()),
                asList("USER_TASK_CREATED", "USER_TASK_DUEDATE_CHANGED"));
    }

    protected JsonNode queryTaskLogEntries(String queryString) throws IOException {
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_TASK_LOG_ENTRIES) + queryString;
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        assertThat(rootNode.has("data")).isTrue();
        return rootNode.get("data");
    }

    protected void expectSequence(JsonNode list, List<String> ids, List<String> types) {
        assertThat(list).hasSameSizeAs(ids);
        List<String> resultingIds = list.findValuesAsText("taskId");
        assertThat(resultingIds).isEqualTo(ids);
        List<String> resultingTypes = list.findValuesAsText("type");
        assertThat(resultingTypes).isEqualTo(types);
    }
}
