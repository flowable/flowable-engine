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

package org.flowable.rest.service.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Frederik Heremans
 */
public class TaskCommentResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all comments for a task. GET runtime/tasks/{taskId}/comments
     */
    @Test
    public void testGetComments() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            // Add a comment as "kermit"
            identityService.setAuthenticatedUserId("kermit");
            Comment comment = taskService.addComment(task.getId(), null, "This is a comment...");
            identityService.setAuthenticatedUserId(null);

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT_COLLECTION, task.getId()));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("[ {"
                            + "id: '" + comment.getId() + "',"
                            + "author: 'kermit',"
                            + "message: 'This is a comment...',"
                            + "taskId: '" + task.getId() + "',"
                            + "taskUrl: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), comment.getId())
                            + "',"
                            + "processInstanceId: null,"
                            + "processInstanceUrl: null"
                            + "} ]");

            // Test with unexisting task
            httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT_COLLECTION, "unexistingtask"));
            closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test creating a comment for a task. POST runtime/tasks/{taskId}/comments
     */
    @Test
    public void testCreateComment() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("message", "This is a comment...");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT_COLLECTION, task.getId()));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
            List<Comment> commentsOnTask = taskService.getTaskComments(task.getId());
            assertThat(commentsOnTask).hasSize(1);

            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "id: '" + commentsOnTask.get(0).getId() + "',"
                            + "author: 'kermit',"
                            + "message: 'This is a comment...',"
                            + "taskId: '" + task.getId() + "',"
                            + "taskUrl: '" + SERVER_URL_PREFIX + RestUrls
                            .createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), commentsOnTask.get(0).getId()) + "',"
                            + "processInstanceId: null,"
                            + "processInstanceUrl: null"
                            + "}");

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml" })
    public void testCreateCommentWithProcessInstanceId() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().singleResult();

        ObjectNode requestNode = objectMapper.createObjectNode();
        String message = "test";
        requestNode.put("message", message);
        requestNode.put("saveProcessInstanceId", true);

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT_COLLECTION, task.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        List<Comment> commentsOnTask = taskService.getTaskComments(task.getId());
        assertThat(commentsOnTask).hasSize(1);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "message: '" + message + "',"
                        + "time: '${json-unit.any-string}',"
                        + "taskId: '" + task.getId() + "',"
                        + "taskUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), commentsOnTask.get(0).getId()) + "',"
                        + "processInstanceId: '" + processInstance.getId() + "',"
                        + "processInstanceUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, processInstance.getId(),
                                commentsOnTask.get(0).getId()) + "'"
                        + "}");
    }

    /**
     * Test getting a comment for a task. GET runtime/tasks/{taskId}/comments/{commentId}
     */
    @Test
    public void testGetComment() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            // Add a comment as "kermit"
            identityService.setAuthenticatedUserId("kermit");
            Comment comment = taskService.addComment(task.getId(), null, "This is a comment...");
            identityService.setAuthenticatedUserId(null);

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), comment.getId()));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "id: '" + comment.getId() + "',"
                            + "author: 'kermit',"
                            + "message: 'This is a comment...',"
                            + "taskId: '" + task.getId() + "',"
                            + "taskUrl: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), comment.getId())
                            + "',"
                            + "processInstanceId: null,"
                            + "processInstanceUrl: null"
                            + "}");

            // Test with unexisting task
            httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, "unexistingtask", "123"));
            closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));

            // Test with unexisting comment
            httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), "unexistingcomment"));
            closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test deleting a comment for a task. DELETE runtime/tasks/{taskId}/comments/{commentId}
     */
    @Test
    public void testDeleteComment() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            // Add a comment as "kermit"
            identityService.setAuthenticatedUserId("kermit");
            Comment comment = taskService.addComment(task.getId(), null, "This is a comment...");
            identityService.setAuthenticatedUserId(null);

            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), comment.getId()));
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            // Test with unexisting task
            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, "unexistingtask", "123"));
            closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));

            // Test with unexisting comment
            httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), "unexistingcomment"));
            closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    /**
     * Test getting a comment for a completed task. GET runtime/tasks/{taskId}/comments/{commentId}
     */
    @Test
    public void testGetCommentWithCompletedTask() throws Exception {
        try {
            Task task = taskService.newTask();
            taskService.saveTask(task);

            // Add a comment as "kermit"
            identityService.setAuthenticatedUserId("kermit");
            Comment comment = taskService.addComment(task.getId(), null, "This is a comment...");
            identityService.setAuthenticatedUserId(null);

            taskService.complete(task.getId());

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), comment.getId()));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "author: 'kermit',"
                            + "message: 'This is a comment...',"
                            + "id: '" + comment.getId() + "',"
                            + "taskId: '" + task.getId() + "',"
                            + "taskUrl: '" + SERVER_URL_PREFIX + RestUrls
                            .createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT, task.getId(), comment.getId()) + "',"
                            + "processInstanceId: null,"
                            + "processInstanceUrl: null"
                            + "}");

        } finally {
            // Clean adhoc-tasks even if test fails
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().list();
            for (HistoricTaskInstance task : tasks) {
                historyService.deleteHistoricTaskInstance(task.getId());
            }
        }
    }
}
