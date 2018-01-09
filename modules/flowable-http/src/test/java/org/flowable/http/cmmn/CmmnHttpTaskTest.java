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
package org.flowable.http.cmmn;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.http.bpmn.HttpServiceTaskTestServer;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author martin.grofcik
 */
public class CmmnHttpTaskTest {

    @Rule
    public FlowableCmmnRule cmmnRule = new FlowableCmmnRule("org/flowable/http/cmmn/CmmnHttpTaskTest.cfg.xml");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
       HttpServiceTaskTestServer.setUp();
    }


    @Test
    @CmmnDeployment(
            resources = { "org/flowable/http/cmmn/CmmnHttpTaskTest.testSimpleGet.cmmn"
            }
    )
    public void testDecisionServiceTask() {
        CaseInstance caseInstance = createCaseInstance();

        assertNotNull(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testGetWithVariableName() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat((String) cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "test"), containsString("John"));

    }

    @Test
    @CmmnDeployment
    public void testGetWithoutVariableName() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat((String) cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "httpGet.responseBody"),
                containsString("John"));
    }

    @Test
    @CmmnDeployment
    public void testGetWithResponseHandler() {
        CaseInstance caseInstance = createCaseInstance();

        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertEquals(2, variables.size());
        String firstName = null;
        String lastName = null;

        for (Map.Entry<String,Object> variable : variables.entrySet()) {
            if ("firstName".equals(variable.getKey())) {
                firstName = (String) variable.getValue();
            } else if ("lastName".equals(variable.getKey())) {
                lastName = (String) variable.getValue();
            }
        }

        assertEquals("John", firstName);
        assertEquals("Doe", lastName);
    }

    @Test
    @CmmnDeployment
    public void testGetWithRequestHandler() {
        CaseInstance caseInstance = createCaseInstance();
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertEquals(1, variables.size());
        assertThat((String) variables.get("httpGet.responseBody"), containsString("John"));
    }

    @Test
    @CmmnDeployment
    public void testHttpsSelfSigned() {
        Assert.assertThat( createCaseInstance(), is(notNullValue()));
    }

    @Test
    @CmmnDeployment
    public void testConnectTimeout() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(IOException.class));

        createCaseInstance();
    }

    @Test
    @CmmnDeployment
    public void testRequestTimeout() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectCause(IsInstanceOf.<Throwable>instanceOf(SocketException.class));

        createCaseInstance();
    }

    @Test
    @CmmnDeployment
    public void testDisallowRedirects() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("HTTP302");

        createCaseInstance();
    }

    @Test
    @CmmnDeployment
    public void testFailStatusCodes() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("HTTP400");

        createCaseInstance();
    }

    @Test
    @CmmnDeployment
    public void testHandleStatusCodes() {
        Assert.assertThat(createCaseInstance(), is(notNullValue()));
    }

    @Test
    @CmmnDeployment
    public void testIgnoreException() {
        Assert.assertThat(createCaseInstance(), is(notNullValue()));
    }

    @Test
    @CmmnDeployment
    public void testMapException() {
        //exception mapping is not implemented yet in Cmmn
        this.expectedException.expect(RuntimeException.class);

        createCaseInstance();
    }

    @Test
    @CmmnDeployment
    public void testHttpGet3XX() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance, is(notNullValue()));
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat((Integer) variables.get("httpGet.responseStatusCode"), is( 302));
    }

    @Test
    @CmmnDeployment
    public void testHttpGet4XX() {
        this.expectedException.expect(FlowableException.class);
        this.expectedException.expectMessage("HTTP404");

        createCaseInstance();
    }

    @Test
    @CmmnDeployment
    public void testHttpGet5XX() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance, is(notNullValue()));
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat((String) variables.get("get500.requestMethod"), is("GET"));
        assertThat((String) variables.get("get500.requestUrl"), is("https://localhost:9799/api?code=500"));
        assertThat((String) variables.get("get500.requestHeaders"), is("Accept: application/json"));
        assertThat((Integer) variables.get("get500.requestTimeout"), is(5000));
        assertThat((String) variables.get("get500.handleStatusCodes"), is("4XX, 5XX"));
        assertThat((Boolean) variables.get("get500.saveRequestVariables"), is(true));

        assertThat((Integer) variables.get("get500.responseStatusCode"), is(500));
        assertThat((String) variables.get("get500.responseReason"), is("Server Error"));
    }

    @Test
    @CmmnDeployment
    public void testHttpPost2XX() throws Exception {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance, is(notNullValue()));
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat((String) variables.get("httpPost.requestMethod"), is("POST"));
        assertThat((String) variables.get("httpPost.requestUrl"), is("https://localhost:9799/api?code=201"));
        assertThat((String) variables.get("httpPost.requestHeaders"), is("Content-Type: application/json"));
        assertThat((String) variables.get("httpPost.requestBody"), is("{\"test\":\"sample\",\"result\":true}"));

        assertThat((Integer) variables.get("httpPost.responseStatusCode"), is(201));
        assertThat((String) variables.get("httpPost.responseReason"), is("Created"));
        assertThat((String) variables.get("httpPost.responseBody"), containsString("\"body\":\"{\\\"test\\\":\\\"sample\\\",\\\"result\\\":true}\""));
    }

    @Test
    @CmmnDeployment
    public void testHttpPost3XX() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance, is(notNullValue()));
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat((Integer) variables.get("httpPost.responseStatusCode"), is(302));
    }

    @Test
    @CmmnDeployment
    public void testHttpDelete4XX() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance, is(notNullValue()));
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat((Integer) variables.get("httpDelete.responseStatusCode"), is(400));
        assertThat((String) variables.get("httpDelete.responseReason"), is("Bad Request"));
    }

    @Test
    @CmmnDeployment
    public void testHttpPut5XX() throws Exception {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("prefix", "httpPost")
                .start();

        assertThat(caseInstance, is(notNullValue()));
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat((String) variables.get("httpPost.requestMethod"), is("PUT"));
        assertThat((String) variables.get("httpPost.requestUrl"), is("https://localhost:9799/api?code=500"));
        assertThat((String) variables.get("httpPost.requestHeaders"), is("Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000\nTest:"));
        assertThat((String) variables.get("httpPost.requestBody"), is("test"));

        assertThat((Integer) variables.get("httpPost.responseStatusCode"), is(500));
        assertThat((String) variables.get("httpPost.responseReason"), is("Server Error"));

        Map<String, String> headerMap = HttpServiceTaskTestServer.HttpServiceTaskTestServlet.headerMap;
        assertEquals("text/plain", headerMap.get("Content-Type"));
        assertEquals("623b94fc-14b8-4ee6-aed7-b16b9321e29f", headerMap.get("X-Request-ID"));
        assertEquals("localhost:7000", headerMap.get("Host"));
        assertEquals(null, headerMap.get("Test"));
    }

    @Test
    @CmmnDeployment(
        resources = {"org/flowable/http/cmmn/CmmnHttpTaskTest.testExpressions.cmmn"}
    )
    public void testExpressions() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("method", "PUT");
        variables.put("url", "https://localhost:9799/api?code=500");
        variables.put("headers", "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000\nTest:");
        variables.put("body", "test");
        variables.put("timeout", 2000);
        variables.put("ignore", true);
        variables.put("fail", "400, 404");
        variables.put("save", true);
        variables.put("response", true);
        variables.put("prefix", "httpPost");

        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variables(variables)
                .start();

        assertThat(caseInstance, is(notNullValue()));
        Map<String, Object> outputVariables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat((String) outputVariables.get("httpPost.requestMethod"), is("PUT"));
        assertThat((String) outputVariables.get("httpPost.requestUrl"), is("https://localhost:9799/api?code=500"));
        assertThat((String) outputVariables.get("httpPost.requestHeaders"), is("Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000\nTest:"));
        assertThat((String) outputVariables.get("httpPost.requestBody"), is("test"));

        assertThat((Integer) outputVariables.get("httpPost.responseStatusCode"), is(500));
        assertThat((String) outputVariables.get("httpPost.responseReason"), is("Server Error"));

        Map<String, String> headerMap = HttpServiceTaskTestServer.HttpServiceTaskTestServlet.headerMap;
        assertEquals("text/plain", headerMap.get("Content-Type"));
        assertEquals("623b94fc-14b8-4ee6-aed7-b16b9321e29f", headerMap.get("X-Request-ID"));
        assertEquals("localhost:7000", headerMap.get("Host"));
        assertEquals(null, headerMap.get("Test"));
    }

    protected CaseInstance createCaseInstance() {
        return cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
    }

}
