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
package org.flowable.http.bpmn;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.http.bpmn.HttpServiceTaskTestServer.HttpServiceTaskTestServlet;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskTest extends HttpServiceTaskTestCase {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Deployment
    public void testSimpleGetOnly() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testGetWithVariableName() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();
        assertThat(variables)
                .extracting(HistoricVariableInstance::getVariableName)
                .containsExactly("test");
        assertThatJson(variables.get(0).getValue())
                .isEqualTo("{ name: { firstName: 'John', lastName: 'Doe' }}");
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testGetWithoutVariableName() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();
        assertThat(variables)
                .extracting(HistoricVariableInstance::getVariableName)
                .containsExactly("httpGetResponseBody");
        assertThatJson(variables.get(0).getValue())
                .isEqualTo("{ name: { firstName: 'John', lastName: 'Doe' }}");
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testGetWithResponseHandler() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();

        assertThat(variables)
                .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                .containsExactlyInAnyOrder(
                        tuple("firstName", "John"),
                        tuple("lastName", "Doe")
                );

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testGetWithParametrizedResponseHandler() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();

        assertThat(variables)
                .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                .containsExactlyInAnyOrder(
                        tuple("firstName", "John"),
                        tuple("lastName", "Doe")
                );

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testGetWithRequestHandler() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();
        assertThat(variables)
                .extracting(HistoricVariableInstance::getVariableName)
                .containsExactly("httpGetResponseBody");
        assertThatJson(variables.get(0).getValue())
                .isEqualTo("{ name: { firstName: 'John', lastName: 'Doe' }}");
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testGetWithBpmnThrowingResponseHandler() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        final HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(procId)
                .singleResult();

        assertThat(processInstance.getEndActivityId()).isEqualTo("theEnd2");
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testGetWithBpmnThrowingRequestHandler() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        final HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(procId)
                .singleResult();

        assertThat(processInstance.getEndActivityId()).isEqualTo("theEnd2");
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testHttpsSelfSigned() {
        String procId = runtimeService.startProcessInstanceByKey("httpsSelfSigned").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testConnectTimeout() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("connectTimeout"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @Deployment
    public void testRequestTimeout() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("requestTimeout"))
                .isExactlyInstanceOf(FlowableException.class)
                .getCause().isInstanceOfAny(SocketTimeoutException.class, SocketException.class);
    }

    @Test
    @Deployment(resources = "org/flowable/http/bpmn/HttpServiceTaskTest.testRequestTimeout2.bpmn20.xml")
    public void testRequestTimeoutFromProcessModelHasPrecedence() {
        // set up timeout for test
        int defaultSocketTimeout = this.processEngineConfiguration.getHttpClientConfig().getSocketTimeout();
        int defaultConnectTimeOut = this.processEngineConfiguration.getHttpClientConfig().getConnectTimeout();
        int defaultRequestTimeOut = this.processEngineConfiguration.getHttpClientConfig().getConnectionRequestTimeout();

        this.processEngineConfiguration.getHttpClientConfig().setSocketTimeout(15000);
        this.processEngineConfiguration.getHttpClientConfig().setConnectTimeout(15000);
        this.processEngineConfiguration.getHttpClientConfig().setConnectionRequestTimeout(5000);

        // execute test
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("requestTimeout"))
                .isExactlyInstanceOf(FlowableException.class);

        // restore timeouts
        this.processEngineConfiguration.getHttpClientConfig().setSocketTimeout(defaultSocketTimeout);
        this.processEngineConfiguration.getHttpClientConfig().setConnectTimeout(defaultConnectTimeOut);
        this.processEngineConfiguration.getHttpClientConfig().setConnectionRequestTimeout(defaultRequestTimeOut);
    }

    @Test
    @Deployment(resources = "org/flowable/http/bpmn/HttpServiceTaskTest.testRequestTimeout3.bpmn20.xml")
    public void testRequestTimeoutFromProcessModelHasPrecedenceSuccess() {
        // set up timeout for test
        int defaultSocketTimeout = this.processEngineConfiguration.getHttpClientConfig().getSocketTimeout();
        int defaultConnectTimeOut = this.processEngineConfiguration.getHttpClientConfig().getConnectTimeout();
        int defaultRequestTimeOut = this.processEngineConfiguration.getHttpClientConfig().getConnectionRequestTimeout();

        this.processEngineConfiguration.getHttpClientConfig().setSocketTimeout(15000);
        this.processEngineConfiguration.getHttpClientConfig().setConnectTimeout(15000);
        this.processEngineConfiguration.getHttpClientConfig().setConnectionRequestTimeout(5000);

        // execute test
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("requestTimeout");
        assertProcessEnded(processInstance.getId());

        // restore timeouts
        this.processEngineConfiguration.getHttpClientConfig().setSocketTimeout(defaultSocketTimeout);
        this.processEngineConfiguration.getHttpClientConfig().setConnectTimeout(defaultConnectTimeOut);
        this.processEngineConfiguration.getHttpClientConfig().setConnectionRequestTimeout(defaultRequestTimeOut);
    }

    @Test
    @Deployment
    public void testDisallowRedirects() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("disallowRedirects"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("HTTP302");
    }

    @Test
    @Deployment
    public void testFailStatusCodes() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("failStatusCodes"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("HTTP400");
    }

    @Test
    @Deployment
    public void testHandleStatusCodes() {
        String procId = runtimeService.startProcessInstanceByKey("handleStatusCodes").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testIgnoreException() {
        String procId = runtimeService.startProcessInstanceByKey("ignoreException").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testMapException() {
        String procId = runtimeService.startProcessInstanceByKey("mapException").getId();
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testHttpGet2XX() throws Exception {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpGet2XX");
        assertThat(process.isEnded()).isFalse();
        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("resultRequestMethod", "GET");
        request.put("resultRequestUrl", "https://localhost:9799/api?code=200");
        request.put("resultRequestHeaders", "Accept: application/json");
        request.put("resultRequestTimeout", 2000);
        request.put("resultIgnoreException", true);
        assertKeysEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("resultResponseStatusCode", 200);
        response.put("resultResponseHeaders", "Content-Type: application/json");
        assertKeysEquals(process.getId(), response);
        // Response body assertions
        String body = (String) runtimeService.getVariable(process.getId(), "resultResponseBody");
        assertThat(body).isNotNull();
        JsonNode jsonNode = mapper.readValue(body, JsonNode.class);
        mapper.convertValue(jsonNode, HttpTestData.class);
        continueProcess(process);
    }

    @Test
    @Deployment
    public void testHttpGet3XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpGet3XX");
        assertThat(process.isEnded()).isFalse();
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpGetResponseStatusCode", 302);
        assertKeysEquals(process.getId(), response);
        continueProcess(process);
    }

    @Test
    @Deployment
    public void testHttpGet4XX() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testHttpGet4XX"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("HTTP404");
    }

    @Test
    @Deployment
    public void testHttpGet5XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpGet5XX");
        assertThat(process.isEnded()).isFalse();
        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("get500RequestMethod", "GET");
        request.put("get500RequestUrl", "https://localhost:9799/api?code=500");
        request.put("get500RequestHeaders", "Accept: application/json");
        request.put("get500RequestTimeout", 5000);
        request.put("get500HandleStatusCodes", "4XX, 5XX");
        request.put("get500SaveRequestVariables", true);
        assertKeysEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("get500ResponseStatusCode", 500);
        response.put("get500ResponseReason", get500ResponseReason());
        assertKeysEquals(process.getId(), response);
        continueProcess(process);
    }

    protected String get500ResponseReason() {
        return "Server Error";
    }

    @Test
    @Deployment
    public void testHttpPost2XX() throws Exception {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPost2XX");
        assertThat(process.isEnded()).isFalse();

        String body = "{\"test\":\"sample\",\"result\":true}";
        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("httpPostRequestMethod", "POST");
        request.put("httpPostRequestUrl", "https://localhost:9799/api?code=201");
        request.put("httpPostRequestHeaders", "Content-Type: application/json");
        request.put("httpPostRequestBody", body);
        assertKeysEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPostResponseStatusCode", 201);
        assertKeysEquals(process.getId(), response);
        // Response body assertions
        String responseBody = (String) runtimeService.getVariable(process.getId(), "httpPostResponseBody");
        assertThat(responseBody).isNotNull();
        JsonNode jsonNode = mapper.readValue(responseBody, JsonNode.class);
        HttpTestData testData = mapper.convertValue(jsonNode, HttpTestData.class);
        assertThat(testData.getBody()).isEqualTo(body);
        continueProcess(process);
    }

    @Test
    @Deployment
    public void testHttpPost3XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPost3XX");
        assertThat(process.isEnded()).isFalse();
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPostResponseStatusCode", 302);
        assertKeysEquals(process.getId(), response);
        continueProcess(process);
    }

    @Test
    @Deployment
    public void testHttpPostBodyEncoding() throws Exception {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPostBodyEncoding");
        assertThat(process.isEnded()).isFalse();

        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("httpPostRequestMethod", "POST");
        request.put("httpPostRequestUrl", "http://localhost:9798/hello");
        request.put("httpPostRequestHeaders", "Content-Type: application/json; charset=utf-8");
        request.put("httpPostRequestBody", "{\"name\":\"Alen Turković\"}");
        assertKeysEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPostResponseStatusCode", 200);
        assertKeysEquals(process.getId(), response);
        // Response body assertions
        String responseBody = (String) runtimeService.getVariable(process.getId(), "httpPostResponseBody");
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.trim()).isEqualTo("{\"result\":\"Hello Alen Turković\"}");
        continueProcess(process);
    }

    @Test
    @Deployment
    public void testHttpDelete4XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpDelete4XX");
        assertThat(process.isEnded()).isFalse();
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpDeleteResponseStatusCode", 400);
        response.put("httpDeleteResponseReason", "Bad Request");
        assertKeysEquals(process.getId(), response);
        continueProcess(process);
    }

    @Test
    @Deployment
    public void testHttpPut5XX() throws Exception {

        String body = "test";

        Map<String, Object> variables = new HashMap<>();
        variables.put("method", "POST");
        variables.put("url", "https://localhost:9799/api?code=500");
        variables.put("headers", "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000\nTest:");
        variables.put("body", body);
        variables.put("timeout", 2000);
        variables.put("ignore", true);
        variables.put("fail", "400, 404");
        variables.put("save", true);
        variables.put("response", true);
        variables.put("prefix", "httpPost500");

        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPut5XX", variables);
        assertThat(process.isEnded()).isFalse();

        Map<String, String> headerMap = HttpServiceTaskTestServlet.headerMap;
        assertThat(headerMap)
                .contains(
                        entry("Content-Type", "text/plain"),
                        entry("X-Request-ID", "623b94fc-14b8-4ee6-aed7-b16b9321e29f"),
                        entry("Host", "localhost:7000")
                );

        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("httpPost500RequestMethod", "POST");
        request.put("httpPost500RequestUrl", "https://localhost:9799/api?code=500");
        request.put("httpPost500RequestHeaders", "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000");
        request.put("httpPost500RequestBody", body);
        assertKeysEquals(process.getId(), request);

        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPost500ResponseStatusCode", 500);
        assertKeysEquals(process.getId(), response);
        continueProcess(process);
    }
    
    @Test
    @Deployment
    public void testHttpPatch2XX() throws Exception {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPatch2XX");
        assertThat(process.isEnded()).isFalse();

        String body = "{\"test\":\"sample\",\"result\":true}";
        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("httpPatchRequestMethod", "PATCH");
        request.put("httpPatchRequestUrl", "https://localhost:9799/api?code=201");
        request.put("httpPatchRequestHeaders", "Content-Type: application/json");
        request.put("httpPatchRequestBody", body);
        assertKeysEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPatchResponseStatusCode", 201);
        assertKeysEquals(process.getId(), response);
        // Response body assertions
        String responseBody = (String) runtimeService.getVariable(process.getId(), "httpPatchResponseBody");
        assertThat(responseBody).isNotNull();
        JsonNode jsonNode = mapper.readValue(responseBody, JsonNode.class);
        HttpTestData testData = mapper.convertValue(jsonNode, HttpTestData.class);
        assertThat(testData.getBody()).isEqualTo(body);
        continueProcess(process);
    }

    @Test
    @Deployment
    public void testTransientJsonResponseVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testTransientJsonResponseVariable");
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());

        // There should be only one response variable from the second http task (the first one uses a transient variable)
        assertThat(variables).hasSize(1);
        assertThat(variables.get("postResponse")).isInstanceOf(JsonNode.class);
        JsonNode node = (JsonNode) variables.get("postResponse");
        assertThatJson(node)
                .isEqualTo("{ result: 'Hello John'}");
    }

    @Test
    @Deployment
    public void testArrayNodeResponse() {
        runtimeService.startProcessInstanceByKey("testArrayNodeResponse");
        List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks).hasSize(3);
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("abc", "def", "ghi");
    }

    @Test
    @Deployment
    public void testDeleteResponseEmptyBody() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("testDeleteResponse").getId();
        assertThat(runtimeService.hasVariable(processInstanceId, "myResponse")).isTrue();
        assertThat(runtimeService.getVariable(processInstanceId, "myResponse")).isNull();
    }

    @Test
    @Deployment
    public void testGetWithVariableNameAndSkipExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        variables.put("skip", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testGetWithVariableNameAndSkipExpression", variables);
        assertThatJson(runtimeService.getVariable(pi.getId(), "result"))
                .isEqualTo("{ name: { firstName: 'John', lastName: 'Doe' }}");
        assertThatJson(runtimeService.getVariable(pi.getId(), "result2"))
                .isEqualTo("{ name: { firstName: 'John', lastName: 'Doe' }}");

        Map<String, Object> variables2 = new HashMap<>();
        variables2.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        variables2.put("skip", true);
        ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("testGetWithVariableNameAndSkipExpression", variables2);
        assertThat(runtimeService.getVariable(pi2.getId(), "result")).isNull();
        assertThatJson(runtimeService.getVariable(pi.getId(), "result2"))
                .isEqualTo("{ name: { firstName: 'John', lastName: 'Doe' }}");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricActivityInstance skipActivityInstance = historyService.createHistoricActivityInstanceQuery().processInstanceId(pi2.getId())
                    .activityId("getHttpTask")
                    .singleResult();
            assertActivityInstancesAreSame(skipActivityInstance,
                    runtimeService.createActivityInstanceQuery().activityInstanceId(skipActivityInstance.getId()).singleResult());

            assertThat(skipActivityInstance).isNotNull();
        }
    }

    @Deployment
    @MethodSource("parametersForGetWithVariableParameters")
    @ParameterizedTest(name = "GET Request with ''{0}'' should be received as ''{1}''")
    public void testGetWithVariableParameters(String requestParam, String expectedRequestParam) {
        String procId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testGetWithVariableParameters")
                .variable("requestParam", requestParam)
                .start()
                .getId();
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(procId)
                .variableName("test")
                .singleResult();
        assertThat(variable).isNotNull();
        assertThatJson(variable.getValue())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  args: {"
                        + "    requestParam: [ '" + expectedRequestParam + "' ]"
                        + "  }"
                        + "}");
        assertProcessEnded(procId);
    }

    static Stream<Arguments> parametersForGetWithVariableParameters() {
        return Stream.of(
                Arguments.arguments("Test+ Plus", "Test+ Plus"),
                Arguments.arguments("Test%2B Plus Encoded", "Test+ Plus Encoded"),
                Arguments.arguments("Test Space", "Test Space"),
                Arguments.arguments("Test%20Space%20Encoded", "Test Space Encoded"),
                Arguments.arguments("Test%25Percent Encoded", "Test%Percent Encoded"),
                Arguments.arguments("Test%23Hash Encoded", "Test#Hash Encoded"),
                Arguments.arguments("Test%26Ampersand Encoded", "Test&Ampersand Encoded"),
                Arguments.arguments("Test=Equals", "Test=Equals"),
                Arguments.arguments("Test%3DEquals Encoded", "Test=Equals Encoded"),
                Arguments.arguments("Test?QMark", "Test?QMark"),
                Arguments.arguments("Test%3FQMark Encoded", "Test?QMark Encoded"),
                Arguments.arguments("Test@At", "Test@At"),
                Arguments.arguments("Test%40At Encoded", "Test@At Encoded"),
                Arguments.arguments("Test/Slash", "Test/Slash"),
                Arguments.arguments("Test%2FSlash Encoded", "Test/Slash Encoded"),
                Arguments.arguments("Test:Colon", "Test:Colon"),
                Arguments.arguments("Test%3AColon Encoded", "Test:Colon Encoded")
        );
    }

    private void assertKeysEquals(final String processInstanceId, final Map<String, Object> vars) {
        for (String key : vars.keySet()) {
            if (key.contains("Headers")) {
                assertThat((String) runtimeService.getVariable(processInstanceId, key)).containsSequence((String) vars.get(key));
            } else {
                assertThat(runtimeService.getVariable(processInstanceId, key)).isEqualTo(vars.get(key));
            }
        }
    }

    private void continueProcess(final ProcessInstance processInstance) {
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .activityId("wait")
                .singleResult();
        assertThat(execution).isNotNull();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }
}
