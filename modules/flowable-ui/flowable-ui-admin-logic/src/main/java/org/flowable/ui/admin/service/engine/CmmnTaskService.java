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
package org.flowable.ui.admin.service.engine;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service for invoking Flowable REST services.
 */
@Service
public class CmmnTaskService {

    public static final String HISTORIC_TASK_LIST_URL = "cmmn-history/historic-task-instances";
    public static final String HISTORIC_TASK_QUERY_URL = "cmmn-query/historic-task-instances";
    public static final String HISTORIC_TASK_URL = "cmmn-history/historic-task-instances/{0}";
    public static final String RUNTIME_TASK_URL = "cmmn-runtime/tasks/{0}";
    public static final String HISTORIC_VARIABLE_INSTANCE_LIST_URL = "cmmn-history/historic-variable-instances";
    public static final String HISTORIC_TASK_IDENTITY_LINK_LIST_URL = "cmmn-history/historic-task-instances/{0}/identitylinks";

    public static final String[] TASK_FILTERS = new String[] { "taskNameLike", "taskAssigneeLike", "taskOwner", "finished",
            "dueDateAfter", "dueDateBefore", "taskCompletedAfter", "taskCompletedBefore", "taskCreatedAfter", "taskCreatedBefore", "parentTaskId", "caseInstanceId" };

    private static final String DEFAULT_SUBTASK_RESULT_SIZE = "1024";
    private static final String DEFAULT_VARIABLE_RESULT_SIZE = "1024";

    @Autowired
    protected FlowableClientService clientUtil;

    @Autowired
    protected ObjectMapper objectMapper;

    public JsonNode listTasks(ServerConfig serverConfig, ObjectNode bodyNode) {

        JsonNode resultNode = null;
        try {
            URIBuilder builder = clientUtil.createUriBuilder(HISTORIC_TASK_QUERY_URL);

            String uri = clientUtil.getUriWithPagingAndOrderParameters(builder, bodyNode);
            HttpPost post = clientUtil.createPost(uri, serverConfig);

            post.setEntity(clientUtil.createStringEntity(bodyNode.toString()));
            resultNode = clientUtil.executeRequest(post, serverConfig);
        } catch (Exception e) {
            throw new FlowableServiceException(e.getMessage(), e);
        }
        return resultNode;
    }

    public JsonNode getTask(ServerConfig serverConfig, String taskId, boolean runtime) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task id is required");
        }

        URIBuilder builder = null;
        if (runtime) {
            builder = clientUtil.createUriBuilder(MessageFormat.format(RUNTIME_TASK_URL, taskId));
        } else {
            builder = clientUtil.createUriBuilder(MessageFormat.format(HISTORIC_TASK_URL, taskId));
        }

        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder));
        return clientUtil.executeRequest(get, serverConfig);
    }

    /**
     * Delete a task.
     */
    public void deleteTask(ServerConfig serverConfig, String taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task id is required");
        }

        JsonNode taskNode = getTask(serverConfig, taskId, false);
        if (taskNode.has("endTime")) {

            JsonNode endTimeNode = taskNode.get("endTime");
            if (endTimeNode != null && !endTimeNode.isNull() && StringUtils.isNotEmpty(endTimeNode.asText())) {

                // Completed task
                URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(HISTORIC_TASK_URL, taskId));
                HttpDelete delete = new HttpDelete(clientUtil.getServerUrl(serverConfig, builder));
                clientUtil.executeRequestNoResponseBody(delete, serverConfig, HttpStatus.SC_NO_CONTENT);

            } else {

                // Not completed task
                URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(RUNTIME_TASK_URL, taskId) + "?cascadeHistory=true");
                HttpDelete delete = new HttpDelete(clientUtil.getServerUrl(serverConfig, builder));
                clientUtil.executeRequestNoResponseBody(delete, serverConfig, HttpStatus.SC_NO_CONTENT);

            }
        }
    }

    public void executeTaskAction(ServerConfig serverConfig, String taskId, JsonNode actionRequest) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task id is required");
        }

        URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(RUNTIME_TASK_URL, taskId));
        HttpPost post = clientUtil.createPost(builder, serverConfig);

        post.setEntity(clientUtil.createStringEntity(actionRequest));
        clientUtil.executeRequestNoResponseBody(post, serverConfig, HttpStatus.SC_OK);
    }

    public void updateTask(ServerConfig serverConfig, String taskId, JsonNode actionRequest) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task id is required");
        }
        URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(RUNTIME_TASK_URL, taskId));
        HttpPut put = clientUtil.createPut(builder, serverConfig);
        put.setEntity(clientUtil.createStringEntity(actionRequest));
        clientUtil.executeRequestNoResponseBody(put, serverConfig, HttpStatus.SC_OK);
    }

    public JsonNode getSubTasks(ServerConfig serverConfig, String taskId) {
        URIBuilder builder = clientUtil.createUriBuilder(HISTORIC_TASK_LIST_URL);
        builder.addParameter("parentTaskId", taskId);
        builder.addParameter("size", DEFAULT_SUBTASK_RESULT_SIZE);

        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getVariables(ServerConfig serverConfig, String taskId) {
        URIBuilder builder = clientUtil.createUriBuilder(HISTORIC_VARIABLE_INSTANCE_LIST_URL);
        builder.addParameter("taskId", taskId);
        builder.addParameter("size", DEFAULT_VARIABLE_RESULT_SIZE);
        builder.addParameter("sort", "variableName");

        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getIdentityLinks(ServerConfig serverConfig, String taskId) {
        URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(HISTORIC_TASK_IDENTITY_LINK_LIST_URL, taskId));

        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder));
        return clientUtil.executeRequest(get, serverConfig);
    }
}
