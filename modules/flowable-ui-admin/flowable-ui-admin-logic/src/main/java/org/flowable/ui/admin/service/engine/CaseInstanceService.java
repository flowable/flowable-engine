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
import java.util.Collections;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class CaseInstanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseInstanceService.class);

    public static final String HISTORIC_CASE_INSTANCE_URL = "cmmn-history/historic-case-instances/{0}";
    public static final String HISTORIC_TASK_LIST_URL = "cmmn-history/historic-task-instances";
    public static final String HISTORIC_VARIABLE_INSTANCE_LIST_URL = "cmmn-history/historic-variable-instances";
    public static final String HISTORIC_ACTIVITY_INSTANCE_LIST_URL = "cmmn-history/historic-activity-instances";
    public static final String HISTORIC_DECISION_EXECUTION_LIST_URL = "dmn-history/historic-decision-executions";
    public static final String RUNTIME_CASE_INSTANCE_URL = "cmmn-runtime/case-instances/{0}";
    public static final String RUNTIME_CASE_INSTANCE_VARIABLES = "cmmn-runtime/case-instances/{0}/variables";
    public static final String RUNTIME_CASE_INSTANCE_VARIABLE_URL = "cmmn-runtime/case-instances/{0}/variables/{1}";
    
    private static final String DEFAULT_SUBTASK_RESULT_SIZE = "1024";
    private static final String DEFAULT_CASEINSTANCE_SIZE = "100";
    private static final String DEFAULT_VARIABLE_RESULT_SIZE = "1024";

    @Autowired
    protected FlowableClientService clientUtil;

    @Autowired
    protected CmmnJobService jobService;

    @Autowired
    protected ObjectMapper objectMapper;

    public JsonNode listCaseInstances(ObjectNode bodyNode, ServerConfig serverConfig) {
        JsonNode resultNode = null;
        try {
            URIBuilder builder = new URIBuilder("cmmn-query/historic-case-instances");

            String uri = clientUtil.getUriWithPagingAndOrderParameters(builder, bodyNode);
            HttpPost post = clientUtil.createPost(uri, serverConfig);

            post.setEntity(clientUtil.createStringEntity(bodyNode.toString()));
            resultNode = clientUtil.executeRequest(post, serverConfig);
        } catch (Exception e) {
            throw new FlowableServiceException(e.getMessage(), e);
        }
        return resultNode;
    }

    public JsonNode listCaseInstancesForCaseDefinition(ObjectNode bodyNode, ServerConfig serverConfig) {
        JsonNode resultNode = null;
        try {
            URIBuilder builder = new URIBuilder("cmmn-query/historic-case-instances");

            builder.addParameter("size", DEFAULT_CASEINSTANCE_SIZE);
            builder.addParameter("sort", "startTime");
            builder.addParameter("order", "desc");

            String uri = clientUtil.getUriWithPagingAndOrderParameters(builder, bodyNode);
            HttpPost post = clientUtil.createPost(uri, serverConfig);

            post.setEntity(clientUtil.createStringEntity(bodyNode.toString()));
            resultNode = clientUtil.executeRequest(post, serverConfig);
        } catch (Exception e) {
            throw new FlowableServiceException(e.getMessage(), e);
        }
        return resultNode;
    }

    public JsonNode getCaseInstance(ServerConfig serverConfig, String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new IllegalArgumentException("Case instance id is required");
        }

        URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(HISTORIC_CASE_INSTANCE_URL, caseInstanceId));
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getTasks(ServerConfig serverConfig, String caseInstanceId) {
        URIBuilder builder = clientUtil.createUriBuilder(HISTORIC_TASK_LIST_URL);
        builder.addParameter("caseInstanceId", caseInstanceId);
        builder.addParameter("size", DEFAULT_SUBTASK_RESULT_SIZE);

        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getDecisionExecutions(ServerConfig serverConfig, String caseInstanceId) {
        URIBuilder builder = clientUtil.createUriBuilder(HISTORIC_DECISION_EXECUTION_LIST_URL);
        builder.addParameter("instanceId", caseInstanceId);
        builder.addParameter("scopeType", ScopeTypes.CMMN);
        builder.addParameter("size", DEFAULT_SUBTASK_RESULT_SIZE);

        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getVariables(ServerConfig serverConfig, String caseInstanceId) {
        URIBuilder builder = clientUtil.createUriBuilder(HISTORIC_VARIABLE_INSTANCE_LIST_URL);
        builder.addParameter("caseInstanceId", caseInstanceId);
        builder.addParameter("size", DEFAULT_VARIABLE_RESULT_SIZE);
        builder.addParameter("sort", "variableName");
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public void updateVariable(ServerConfig serverConfig, String caseInstanceId, String variableName, ObjectNode objectNode) {
        URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(RUNTIME_CASE_INSTANCE_VARIABLE_URL, caseInstanceId, variableName));
        HttpPut put = clientUtil.createPut(builder, serverConfig);
        put.setEntity(clientUtil.createStringEntity(objectNode.toString()));
        clientUtil.executeRequest(put, serverConfig);
    }

    public void createVariable(ServerConfig serverConfig, String caseInstanceId, ObjectNode objectNode) {
        URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(RUNTIME_CASE_INSTANCE_VARIABLES, caseInstanceId));
        HttpPost post = clientUtil.createPost(builder, serverConfig);
        ArrayNode variablesNode = objectMapper.createArrayNode();
        variablesNode.add(objectNode);

        post.setEntity(clientUtil.createStringEntity(variablesNode.toString()));
        clientUtil.executeRequest(post, serverConfig, 201);
    }

    public void deleteVariable(ServerConfig serverConfig, String caseInstanceId, String variableName) {
        URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(RUNTIME_CASE_INSTANCE_VARIABLE_URL, caseInstanceId, variableName));
        HttpDelete delete = clientUtil.createDelete(builder, serverConfig);
        clientUtil.executeRequestNoResponseBody(delete, serverConfig, 204);
    }

    public void executeAction(ServerConfig serverConfig, String caseInstanceId, JsonNode actionBody) throws FlowableServiceException {
        boolean validAction = false;

        if (actionBody.has("action")) {
            String action = actionBody.get("action").asText();

            if ("delete".equals(action)) {
                validAction = true;

                // Delete historic instance
                URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(HISTORIC_CASE_INSTANCE_URL, caseInstanceId));
                HttpDelete delete = new HttpDelete(clientUtil.getServerUrl(serverConfig, builder));
                clientUtil.executeRequestNoResponseBody(delete, serverConfig, HttpStatus.SC_NO_CONTENT);

            } else if ("terminate".equals(action)) {
                validAction = true;

                // Delete runtime instance
                URIBuilder builder = clientUtil.createUriBuilder(MessageFormat.format(RUNTIME_CASE_INSTANCE_URL, caseInstanceId));
                if (actionBody.has("deleteReason")) {
                    builder.addParameter("deleteReason", actionBody.get("deleteReason").asText());
                }

                HttpDelete delete = new HttpDelete(clientUtil.getServerUrl(serverConfig, builder));
                clientUtil.executeRequestNoResponseBody(delete, serverConfig, HttpStatus.SC_NO_CONTENT);
            }
        }

        if (!validAction) {
            throw new BadRequestException("Action is missing in the request body or the given action is not supported.");
        }
    }

    public JsonNode getJobs(ServerConfig serverConfig, String caseInstanceId) {
        return jobService.listJobs(serverConfig, Collections.singletonMap("caseInstanceId", new String[]{caseInstanceId}));
    }
}
