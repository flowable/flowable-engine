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
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Frederik Heremans
 */
public class HistoricProcessInstanceCommentResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all comments for a historic process instance. GET history/historic-process-instances/{processInstanceId}/comments
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetComments() throws Exception {
        ProcessInstance pi = null;

        try {
            pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            // Add a comment as "kermit"
            identityService.setAuthenticatedUserId("kermit");
            Comment comment = taskService.addComment(null, pi.getId(), "This is a comment...");
            identityService.setAuthenticatedUserId(null);

            CloseableHttpResponse response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT_COLLECTION, pi.getId())),
                    HttpStatus.SC_OK);

            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);

            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo(" [{"
                            + "   id: '" + comment.getId() + "',"
                            + "   author: 'kermit',"
                            + "   message: 'This is a comment...',"
                            + "   taskId: null,"
                            + "   taskUrl: null,"
                            + "   processInstanceId: '" + pi.getProcessInstanceId() + "',"
                            + "   processInstanceUrl: '" + SERVER_URL_PREFIX + RestUrls
                            .createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, pi.getId(), comment.getId()) + "'"
                            + "}]");

            // Test with unexisting task
            closeResponse(
                    executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_COMMENT_COLLECTION, "unexistingtask")),
                            HttpStatus.SC_NOT_FOUND));

        } finally {
            if (pi != null) {
                List<Comment> comments = taskService.getProcessInstanceComments(pi.getId());
                for (Comment c : comments) {
                    taskService.deleteComment(c.getId());
                }
            }
        }
    }

    /**
     * Test creating a comment for a process instance. POST history/historic-process-instances/{processInstanceId}/comments
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testCreateComment() throws Exception {
        ProcessInstance pi = null;

        try {
            pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            HttpPost httpPost = new HttpPost(
                    SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT_COLLECTION, pi.getId()));
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("message", "This is a comment...");
            httpPost.setEntity(new StringEntity(requestNode.toString()));

            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            List<Comment> commentsOnProcess = taskService.getProcessInstanceComments(pi.getId());
            assertThat(commentsOnProcess).hasSize(1);

            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo(" {"
                            + "   id: '" + commentsOnProcess.get(0).getId() + "',"
                            + "   author: 'kermit',"
                            + "   message: 'This is a comment...',"
                            + "   taskId: null,"
                            + "   taskUrl: null,"
                            + "   processInstanceId: '" + pi.getProcessInstanceId() + "',"
                            + "   processInstanceUrl: '" + SERVER_URL_PREFIX + RestUrls
                            .createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, pi.getId(), commentsOnProcess.get(0).getId()) + "'"
                            + "}");

        } finally {
            if (pi != null) {
                List<Comment> comments = taskService.getProcessInstanceComments(pi.getId());
                for (Comment c : comments) {
                    taskService.deleteComment(c.getId());
                }
            }
        }
    }

    /**
     * Test getting a comment for a historic process instance. GET history/historic -process-instances/{processInstanceId}/comments/{commentId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetComment() throws Exception {
        ProcessInstance pi = null;

        try {
            pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            // Add a comment as "kermit"
            identityService.setAuthenticatedUserId("kermit");
            Comment comment = taskService.addComment(null, pi.getId(), "This is a comment...");
            identityService.setAuthenticatedUserId(null);

            CloseableHttpResponse response = executeRequest(new HttpGet(
                            SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, pi.getId(), comment.getId())),
                    200);

            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);

            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo(" {"
                            + "   id: '" + comment.getId() + "',"
                            + "   author: 'kermit',"
                            + "   message: 'This is a comment...',"
                            + "   taskId: null,"
                            + "   taskUrl: null,"
                            + "   processInstanceId: '" + pi.getProcessInstanceId() + "',"
                            + "   processInstanceUrl: '" + SERVER_URL_PREFIX + RestUrls
                            .createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, pi.getId(), comment.getId()) + "'"
                            + "}");

            // Test with unexisting process-instance
            closeResponse(executeRequest(new HttpGet(
                            SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, "unexistinginstance", "123")),
                    HttpStatus.SC_NOT_FOUND));

            closeResponse(executeRequest(new HttpGet(
                            SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, pi.getId(), "unexistingcomment")),
                    HttpStatus.SC_NOT_FOUND));

        } finally {
            if (pi != null) {
                List<Comment> comments = taskService.getProcessInstanceComments(pi.getId());
                for (Comment c : comments) {
                    taskService.deleteComment(c.getId());
                }
            }
        }
    }

    /**
     * Test deleting a comment for a task. DELETE runtime/tasks/{taskId}/comments/{commentId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testDeleteComment() throws Exception {
        ProcessInstance pi = null;

        try {
            pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            // Add a comment as "kermit"
            identityService.setAuthenticatedUserId("kermit");
            Comment comment = taskService.addComment(null, pi.getId(), "This is a comment...");
            identityService.setAuthenticatedUserId(null);

            closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, pi.getId(), comment.getId())),
                    HttpStatus.SC_NO_CONTENT));

            // Test with unexisting instance
            closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, "unexistinginstance", "123")),
                    HttpStatus.SC_NOT_FOUND));

            // Test with unexisting comment
            closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_PROCESS_INSTANCE_COMMENT, pi.getId(), "unexistingcomment")),
                    HttpStatus.SC_NOT_FOUND));

        } finally {
            if (pi != null) {
                List<Comment> comments = taskService.getProcessInstanceComments(pi.getId());
                for (Comment c : comments) {
                    taskService.deleteComment(c.getId());
                }
            }
        }
    }
}
