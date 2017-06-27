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
package org.flowable.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.history.HistoricVariableInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskTest extends HttpServiceTaskTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServiceTaskTest.class);

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
        assertEquals("httpGet.responseBody", variables.get(0).getVariableName());
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
        assertEquals("httpGet.responseBody", variables.get(0).getVariableName());
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
        } catch (final Exception e) {
            assertTrue(e instanceof FlowableException);
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Deployment
    public void testRequestTimeout() {
        try {
            runtimeService.startProcessInstanceByKey("requestTimeout");
        } catch (final Exception e) {
            assertTrue(e instanceof FlowableException);
            assertTrue(e.getCause() instanceof SocketException);
        }
    }

    @Deployment
    public void testDisallowRedirects() {
        try {
            runtimeService.startProcessInstanceByKey("disallowRedirects");
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
        } catch (Exception e) {
            assertTrue(e instanceof FlowableException);
            assertEquals("HTTP400", e.getMessage());
        }
        assertProcessEnded(process.getId());
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
        request.put("result.requestMethod", "GET");
        request.put("result.requestUrl", "https://localhost:9799/api?code=200");
        request.put("result.requestHeaders", "Accept: application/json");
        request.put("result.requestTimeout", 2000);
        request.put("result.ignoreException", true);
        assertEquals(runtimeService, process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("result.responseStatusCode", 200);
        response.put("result.responseHeaders", "Content-Type: application/json");
        assertEquals(runtimeService, process.getId(), response);
        // Response body assertions
        String body = (String) runtimeService.getVariable(process.getId(), "result.responseBody");
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
        response.put("httpGet.responseStatusCode", 302);
        assertEquals(runtimeService, process.getId(), response);
        continueProcess(process);
    }

    @Deployment
    public void testHttpGet4XX() {
        try {
            runtimeService.startProcessInstanceByKey("testHttpGet4XX");
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
        request.put("get500.requestMethod", "GET");
        request.put("get500.requestUrl", "https://localhost:9799/api?code=500");
        request.put("get500.requestHeaders", "Accept: application/json");
        request.put("get500.requestTimeout", 5000);
        request.put("get500.handleStatusCodes", "4XX, 5XX");
        request.put("get500.saveRequestVariables", true);
        assertEquals(runtimeService, process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("get500.responseStatusCode", 500);
        response.put("get500.responseReason", "Server Error");
        assertEquals(runtimeService, process.getId(), response);
        continueProcess(process);
    }

    @Deployment
    public void testHttpPost2XX() throws Exception {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPost2XX");
        assertFalse(process.isEnded());

        String body = "{\"test\":\"sample\",\"result\":true}";
        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("httpPost.requestMethod", "POST");
        request.put("httpPost.requestUrl", "https://localhost:9799/api?code=201");
        request.put("httpPost.requestHeaders", "Content-Type: application/json");
        request.put("httpPost.requestBody", body);
        assertEquals(runtimeService, process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPost.responseStatusCode", 201);
        assertEquals(runtimeService, process.getId(), response);
        // Response body assertions
        String responseBody = (String) runtimeService.getVariable(process.getId(), "httpPost.responseBody");
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
        response.put("httpPost.responseStatusCode", 302);
        assertEquals(runtimeService, process.getId(), response);
        continueProcess(process);
    }

    @Deployment
    public void testHttpDelete4XX() {
        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpDelete4XX");
        assertFalse(process.isEnded());
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpDelete.responseStatusCode", 400);
        response.put("httpDelete.responseReason", "Bad Request");
        assertEquals(runtimeService, process.getId(), response);
        continueProcess(process);
    }

    @Deployment
    public void testHttpPut5XX() throws Exception {

        String body = "test";

        Map<String, Object> variables = new HashMap<>();
        variables.put("method", "POST");
        variables.put("url", "https://localhost:9799/api?code=500");
        variables.put("headers", "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f");
        variables.put("body", body);
        variables.put("timeout", 2000);
        variables.put("ignore", true);
        variables.put("fail", "400, 404");
        variables.put("save", true);
        variables.put("response", true);
        variables.put("prefix", "httpPost500");

        ProcessInstance process = runtimeService.startProcessInstanceByKey("testHttpPut5XX", variables);
        assertFalse(process.isEnded());

        // Request assertions
        Map<String, Object> request = new HashMap<>();
        request.put("httpPost500.requestMethod", "POST");
        request.put("httpPost500.requestUrl", "https://localhost:9799/api?code=500");
        request.put("httpPost500.requestHeaders", "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f");
        request.put("httpPost500.requestBody", body);
        assertEquals(runtimeService, process.getId(), request);
        // Response assertions
        Map<String, Object> response = new HashMap<>();
        response.put("httpPost500.responseStatusCode", 500);
        assertEquals(runtimeService, process.getId(), response);
        continueProcess(process);
    }


    private void assertEquals(final RuntimeService runtimeService, final String processId, final Map<String, Object> vars) {
        for (String key : vars.keySet()) {
            if (key.contains("Headers")) {
                assertTextPresent((String) vars.get(key), (String) runtimeService.getVariable(processId, key));
            } else {
                assertEquals(vars.get(key), runtimeService.getVariable(processId, key));
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
