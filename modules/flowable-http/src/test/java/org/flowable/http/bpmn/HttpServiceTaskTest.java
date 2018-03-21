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

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.http.bpmn.HttpServiceTaskTestServer.HttpServiceTaskTestServlet;
import org.flowable.variable.api.history.HistoricVariableInstance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskTest extends HttpServiceTaskTestCase {

    private ObjectMapper mapper = new ObjectMapper();

    @Deployment
    public void testSimpleGetOnly() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        assertProcessEnded(procId);
    }

    @Deployment
    public void testGetWithVariableName() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();
        assertEquals(1, variables.size());
        assertEquals("test", variables.get(0).getVariableName());
        String variableValue = variables.get(0).getValue().toString();
        assertTrue(variableValue.contains("firstName") && variableValue.contains("John"));
        assertProcessEnded(procId);
    }

    @Deployment
    public void testGetWithoutVariableName() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();
        assertEquals(1, variables.size());
        assertEquals("httpGetResponseBody", variables.get(0).getVariableName());
        String variableValue = variables.get(0).getValue().toString();
        assertTrue(variableValue.contains("firstName") && variableValue.contains("John"));
        assertProcessEnded(procId);
    }

    @Deployment
    public void testGetWithResponseHandler() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();
        assertEquals(2, variables.size());
        String firstName = null;
        String lastName = null;

        for (HistoricVariableInstance historicVariableInstance : variables) {
            if ("firstName".equals(historicVariableInstance.getVariableName())) {
                firstName = (String) historicVariableInstance.getValue();
            } else if ("lastName".equals(historicVariableInstance.getVariableName())) {
                lastName = (String) historicVariableInstance.getValue();
            }
        }

        assertEquals("John", firstName);
        assertEquals("Doe", lastName);
        assertProcessEnded(procId);
    }

    @Deployment
    public void testGetWithRequestHandler() {
        String procId = runtimeService.startProcessInstanceByKey("simpleGetOnly").getId();
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(procId).list();
        assertEquals(1, variables.size());
        assertEquals("httpGetResponseBody", variables.get(0).getVariableName());
        String variableValue = variables.get(0).getValue().toString();
        assertTrue(variableValue.contains("firstName") && variableValue.contains("John"));
        assertProcessEnded(procId);
    }

    @Deployment
    public void testHttpsSelfSigned() {
        String procId = runtimeService.startProcessInstanceByKey("httpsSelfSigned").getId();
        assertProcessEnded(procId);
    }

    @Deployment
    public void testConnectTimeout() {
        try {
            runtimeService.startProcessInstanceByKey("connectTimeout");
            fail("FlowableException expected");
        } catch (final Exception e) {
            assertTrue(e instanceof FlowableException);
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Deployment
    public void testRequestTimeout() {
        try {
            runtimeService.startProcessInstanceByKey("requestTimeout");
            fail("FlowableException expected");
        } catch (final Exception e) {
            assertTrue(e instanceof FlowableException);
            assertTrue(e.getCause() instanceof SocketException);
        }
    }

    @Deployment(resources = "org/flowable/http/bpmn/HttpServiceTaskTest.testRequestTimeout2.bpmn20.xml" )
    public void testRequestTimeoutFromProcessModelHasPrecedence() {
        // set up timeout for test
        int defaultSocketTimeout = this.processEngineConfiguration.getHttpClientConfig().getSocketTimeout();
        int defaultConnectTimeOut = this.processEngineConfiguration.getHttpClientConfig().getConnectTimeout();
        int defaultRequestTimeOut = this.processEngineConfiguration.getHttpClientConfig().getConnectionRequestTimeout();

        this.processEngineConfiguration.getHttpClientConfig().setSocketTimeout(15000);
        this.processEngineConfiguration.getHttpClientConfig().setConnectTimeout(15000);
        this.processEngineConfiguration.getHttpClientConfig().setConnectionRequestTimeout(5000);

        // execute test
        try {
            runtimeService.startProcessInstanceByKey("requestTimeout");
            fail("Expected timeout exception");
        } catch(Exception e) {
            // timeout exception expected
        }
        
        // restore timeouts
        this.processEngineConfiguration.getHttpClientConfig().setSocketTimeout(defaultSocketTimeout);
        this.processEngineConfiguration.getHttpClientConfig().setConnectTimeout(defaultConnectTimeOut);
        this.processEngineConfiguration.getHttpClientConfig().setConnectionRequestTimeout(defaultRequestTimeOut);
    }
    
    @Deployment(resources = "org/flowable/http/bpmn/HttpServiceTaskTest.testRequestTimeout3.bpmn20.xml" )
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

    @Deployment
    public void testDisallowRedirects() {
        try {
            runtimeService.startProcessInstanceByKey("disallowRedirects");
            fail("FlowableException expected");
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertEquals("HTTP302", e.getMessage());
        }
    }

    @Deployment
    public void testFailStatusCodes() {
        ProcessInstance process = null;
        try {
            process = runtimeService.startProcessInstanceByKey("failStatusCodes");
            fail("FlowableException expected");
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertEquals("HTTP400", e.getMessage());
        }
        assertNull("Process instance was not started.", process);
    }

    @Deployment
    public void testHandleStatusCodes() {
        String procId = runtimeService.startProcessInstanceByKey("handleStatusCodes").getId();
        assertProcessEnded(procId);
    }

    @Deployment
    public void testIgnoreException() {
        String procId = runtimeService.startProcessInstanceByKey("ignoreException").getId();
        assertProcessEnded(procId);
    }

    @Deployment
    public void testMapException() {
        String procId = runtimeService.startProcessInstanceByKey("mapException").getId();
        assertProcessEnded(procId);
    }

    @Deployment
    public void testHttpGet2XX() throws Exception {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpGet2XX");
        assertFalse(process.isEnded());
        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("resultRequestMethod", "GET");
        request.put("resultRequestUrl", "https://localhost:9799/api?code=200");
        request.put("resultRequestHeaders", "Accept: application/json");
        request.put("resultRequestTimeout", 2000);
        request.put("resultIgnoreException", true);
        assertEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("resultResponseStatusCode", 200);
        response.put("resultResponseHeaders", "Content-Type: application/json");
        assertEquals(process.getId(), response);
        // Response body assertions
        String body = (String) runtimeService.getVariable(process.getId(), "resultResponseBody");
        assertNotNull(body);
        JsonNode jsonNode = mapper.readValue(body, JsonNode.class);
        mapper.convertValue(jsonNode, HttpTestData.class);
        continueProcess(process);
    }

    @Deployment
    public void testHttpGet3XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpGet3XX");
        assertFalse(process.isEnded());
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpGetResponseStatusCode", 302);
        assertEquals(process.getId(), response);
        continueProcess(process);
    }

    @Deployment
    public void testHttpGet4XX() {
        try {
            runtimeService.startProcessInstanceByKey("testHttpGet4XX");
            fail("FlowableException expected");
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertEquals("HTTP404", e.getMessage());
        }
    }

    @Deployment
    public void testHttpGet5XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpGet5XX");
        assertFalse(process.isEnded());
        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("get500RequestMethod", "GET");
        request.put("get500RequestUrl", "https://localhost:9799/api?code=500");
        request.put("get500RequestHeaders", "Accept: application/json");
        request.put("get500RequestTimeout", 5000);
        request.put("get500HandleStatusCodes", "4XX, 5XX");
        request.put("get500SaveRequestVariables", true);
        assertEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("get500ResponseStatusCode", 500);
        response.put("get500ResponseReason", "Server Error");
        assertEquals(process.getId(), response);
        continueProcess(process);
    }

    @Deployment
    public void testHttpPost2XX() throws Exception {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPost2XX");
        assertFalse(process.isEnded());

        String body = "{\"test\":\"sample\",\"result\":true}";
        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("httpPostRequestMethod", "POST");
        request.put("httpPostRequestUrl", "https://localhost:9799/api?code=201");
        request.put("httpPostRequestHeaders", "Content-Type: application/json");
        request.put("httpPostRequestBody", body);
        assertEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPostResponseStatusCode", 201);
        assertEquals(process.getId(), response);
        // Response body assertions
        String responseBody = (String) runtimeService.getVariable(process.getId(), "httpPostResponseBody");
        assertNotNull(responseBody);
        JsonNode jsonNode = mapper.readValue(responseBody, JsonNode.class);
        HttpTestData testData = mapper.convertValue(jsonNode, HttpTestData.class);
        assertEquals(body, testData.getBody());
        continueProcess(process);
    }

    @Deployment
    public void testHttpPost3XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPost3XX");
        assertFalse(process.isEnded());
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPostResponseStatusCode", 302);
        assertEquals(process.getId(), response);
        continueProcess(process);
    }

    @Deployment
    public void testHttpDelete4XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpDelete4XX");
        assertFalse(process.isEnded());
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpDeleteResponseStatusCode", 400);
        response.put("httpDeleteResponseReason", "Bad Request");
        assertEquals(process.getId(), response);
        continueProcess(process);
    }

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
        assertFalse(process.isEnded());

        Map<String, String> headerMap = HttpServiceTaskTestServlet.headerMap;
        assertEquals("text/plain", headerMap.get("Content-Type"));
        assertEquals("623b94fc-14b8-4ee6-aed7-b16b9321e29f", headerMap.get("X-Request-ID"));
        assertEquals("localhost:7000", headerMap.get("Host"));
        assertEquals(null, headerMap.get("Test"));

        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("httpPost500RequestMethod", "POST");
        request.put("httpPost500RequestUrl", "https://localhost:9799/api?code=500");
        request.put("httpPost500RequestHeaders", "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000");
        request.put("httpPost500RequestBody", body);
        assertEquals(process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPost500ResponseStatusCode", 500);
        assertEquals(process.getId(), response);
        continueProcess(process);
    }
    
    @Deployment
    public void testTransientJsonResponseVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testTransientJsonResponseVariable");
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        
        // There should be only one response variable from the second http task (the first one uses a transient variable)
        assertEquals(1, variables.size());
        assertTrue(variables.get("postResponse") instanceof JsonNode);
        assertEquals("Hello John", ((JsonNode) variables.get("postResponse")).get("result").asText());
    }

    private void assertEquals(final String processInstanceId, final Map<String, Object> vars) {
        for (String key : vars.keySet()) {
            if (key.contains("Headers")) {
                assertTextPresent((String) vars.get(key), (String) runtimeService.getVariable(processInstanceId, key));
            } else {
                assertEquals(vars.get(key), runtimeService.getVariable(processInstanceId, key));
            }
        }
    }

    private void continueProcess(final ProcessInstance processInstance) {
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .activityId("wait")
                .singleResult();
        assertNotNull(execution);
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }
}
