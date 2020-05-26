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

package org.flowable.cmmn.rest.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to sub tasks.
 * 
 * @author Tijs Rademakers
 */
public class TaskSubTaskCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all sub tasks. GET runtime/tasks/{taskId}/subtasks
     */
    public void testGetSubTasks() throws Exception {

        Task parentTask = taskService.newTask();
        parentTask.setName("parent task");
        taskService.saveTask(parentTask);

        Task subTask = taskService.newTask();
        subTask.setName("sub task 1");
        subTask.setParentTaskId(parentTask.getId());
        taskService.saveTask(subTask);

        Task subTask2 = taskService.newTask();
        subTask2.setName("sub task 2");
        subTask2.setParentTaskId(parentTask.getId());
        taskService.saveTask(subTask2);

        // Request all sub tasks
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_SUBTASKS_COLLECTION, parentTask.getId())),
                HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "{"
                        + "    name: 'sub task 1',"
                        + "    id: '" + subTask.getId() + "'"
                        + "},"
                        + "{"
                        + "    name: 'sub task 2',"
                        + "    id: '" + subTask2.getId() + "'"
                        + "}"
                        + "]");

        taskService.deleteTask(parentTask.getId());
        taskService.deleteTask(subTask.getId());
        taskService.deleteTask(subTask2.getId());

        historyService.deleteHistoricTaskInstance(parentTask.getId());
        historyService.deleteHistoricTaskInstance(subTask.getId());
        historyService.deleteHistoricTaskInstance(subTask2.getId());
    }
}
