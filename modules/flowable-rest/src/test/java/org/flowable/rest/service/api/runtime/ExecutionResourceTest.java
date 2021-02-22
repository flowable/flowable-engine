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
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single execution resource.
 *
 * @author Frederik Heremans
 */
public class ExecutionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single execution.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testGetExecution() throws Exception {
        Execution processInstanceExecution = runtimeService.startProcessInstanceByKey("processOne");

        Execution subProcessExecution = runtimeService.createExecutionQuery().activityId("subProcess").singleResult();
        assertThat(subProcessExecution).isNotNull();

        Execution childExecution = runtimeService.createExecutionQuery().activityId("processTask").singleResult();
        assertThat(childExecution).isNotNull();

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, processInstanceExecution.getId())),
                HttpStatus.SC_OK);

        // Check resulting parent execution
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + processInstanceExecution.getId() + "',"
                        + " activityId: null,"
                        + " suspended: false,"
                        + " parentUrl: null,"
                        + " url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, processInstanceExecution.getId()) + "',"
                        + " processInstanceUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstanceExecution.getId()) + "'"
                        + "}");

        // Check resulting child execution
        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, childExecution.getId())),
                HttpStatus.SC_OK);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + childExecution.getId() + "',"
                        + " activityId: 'processTask',"
                        + " suspended: false,"
                        + " parentUrl: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, subProcessExecution.getId()) + "',"
                        + " url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, childExecution.getId()) + "',"
                        + " processInstanceUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstanceExecution.getId()) + "'"
                        + "}");
    }

    /**
     * Test getting an unexisting execution.
     */
    @Test
    public void testGetUnexistingExecution() throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, "unexisting")), HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test signalling a single execution, without signal name.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-signal.bpmn20.xml" })
    public void testSignalExecution() throws Exception {
        runtimeService.startProcessInstanceByKey("processOne");

        Execution signalExecution = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(signalExecution).isNotNull();
        assertThat(signalExecution.getActivityId()).isEqualTo("waitState");

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "signal");

        // Signalling one causes process to move on to second signal and
        // execution is not finished yet
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, signalExecution.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "activityId: 'anotherWaitState'"
                        + "}");
        assertThat(runtimeService.createExecutionQuery().executionId(signalExecution.getId()).singleResult().getActivityId()).isEqualTo("anotherWaitState");

        // Signalling again causes process to end
        response = executeRequest(httpPut, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        // Check if process is actually ended
        assertThat(runtimeService.createExecutionQuery().executionId(signalExecution.getId()).singleResult()).isNull();
    }

    /**
     * Test signalling a single execution, without signal name.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-signal-event.bpmn20.xml" })
    public void testSignalEventExecution() throws Exception {
        Execution signalExecution = runtimeService.startProcessInstanceByKey("processOne");
        assertThat(signalExecution).isNotNull();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "signalEventReceived");
        requestNode.put("signalName", "unexisting");

        Execution waitingExecution = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(waitingExecution).isNotNull();

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, waitingExecution.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        closeResponse(response);

        requestNode.put("signalName", "alert");

        // Sending signal event causes the execution to end (scope-execution for
        // the catching event)
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPut, HttpStatus.SC_OK);
        closeResponse(response);

        // Check if process is moved on to the other wait-state
        waitingExecution = runtimeService.createExecutionQuery().activityId("anotherWaitState").singleResult();
        assertThat(waitingExecution).isNotNull();

    }

    /**
     * Test signalling a single execution, with signal event.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-signal-event.bpmn20.xml" })
    public void testSignalEventExecutionWithvariables() throws Exception {
        Execution signalExecution = runtimeService.startProcessInstanceByKey("processOne");
        assertThat(signalExecution).isNotNull();

        ArrayNode variables = objectMapper.createArrayNode();
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "signalEventReceived");
        requestNode.put("signalName", "alert");
        requestNode.set("variables", variables);

        ObjectNode varNode = objectMapper.createObjectNode();
        variables.add(varNode);
        varNode.put("name", "myVar");
        varNode.put("value", "Variable set when signal event is received");

        Execution waitingExecution = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(waitingExecution).isNotNull();

        // Sending signal event causes the execution to end (scope-execution for
        // the catching event)
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, waitingExecution.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
        closeResponse(response);

        // Check if process is moved on to the other wait-state
        waitingExecution = runtimeService.createExecutionQuery().activityId("anotherWaitState").singleResult();
        assertThat(waitingExecution).isNotNull();

        Map<String, Object> vars = runtimeService.getVariables(waitingExecution.getId());

        assertThat(vars)
                .containsOnly(entry("myVar", "Variable set when signal event is received"));
    }

    /**
     * Test signalling a single execution, without signal event and variables.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-message-event.bpmn20.xml" })
    public void testMessageEventExecution() throws Exception {
        Execution execution = runtimeService.startProcessInstanceByKey("processOne");
        assertThat(execution).isNotNull();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "messageEventReceived");
        requestNode.put("messageName", "unexisting");
        Execution waitingExecution = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(waitingExecution).isNotNull();

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, waitingExecution.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        closeResponse(response);

        requestNode.put("messageName", "paymentMessage");

        // Sending signal event causes the execution to end (scope-execution for
        // the catching event)
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPut, HttpStatus.SC_OK);
        closeResponse(response);

        // Check if process is moved on to the other wait-state
        waitingExecution = runtimeService.createExecutionQuery().activityId("anotherWaitState").singleResult();
        assertThat(waitingExecution).isNotNull();
    }

    /**
     * Test messaging a single execution with variables.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-message-event.bpmn20.xml" })
    public void testMessageEventExecutionWithvariables() throws Exception {
        Execution signalExecution = runtimeService.startProcessInstanceByKey("processOne");
        assertThat(signalExecution).isNotNull();

        ArrayNode variables = objectMapper.createArrayNode();
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "messageEventReceived");
        requestNode.put("messageName", "paymentMessage");
        requestNode.set("variables", variables);

        ObjectNode varNode = objectMapper.createObjectNode();
        variables.add(varNode);
        varNode.put("name", "myVar");
        varNode.put("value", "Variable set when signal event is received");

        Execution waitingExecution = runtimeService.createExecutionQuery().activityId("waitState").singleResult();
        assertThat(waitingExecution).isNotNull();

        // Sending signal event causes the execution to end (scope-execution for
        // the catching event)
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, waitingExecution.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
        closeResponse(response);

        // Check if process is moved on to the other wait-state
        waitingExecution = runtimeService.createExecutionQuery().activityId("anotherWaitState").singleResult();
        assertThat(waitingExecution).isNotNull();

        Map<String, Object> vars = runtimeService.getVariables(waitingExecution.getId());

        assertThat(vars)
                .containsOnly(entry("myVar", "Variable set when signal event is received"));
    }

    /**
     * Test executing an illegal action on an execution.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ExecutionResourceTest.process-with-subprocess.bpmn20.xml" })
    public void testIllegalExecutionAction() throws Exception {
        Execution execution = runtimeService.startProcessInstanceByKey("processOne");
        assertThat(execution).isNotNull();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "badaction");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_EXECUTION, execution.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST);
        closeResponse(response);
    }
}
