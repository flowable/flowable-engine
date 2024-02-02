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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.http.bpmn.HttpServiceTaskTestServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author martin.grofcik
 */
public class CmmnHttpTaskTest {

    @Rule
    public FlowableCmmnRule cmmnRule;

    public CmmnHttpTaskTest() {
        this("org/flowable/http/cmmn/CmmnHttpTaskTest.cfg.xml");
    }

    protected CmmnHttpTaskTest(String configurationResource) {
        this.cmmnRule = new FlowableCmmnRule(configurationResource);
    }

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

        assertThat(caseInstance).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testGetWithVariableName.cmmn")
    public void testGetWithVariableName() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat((String) cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "test")).contains("John");

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testGetWithoutVariableName.cmmn")
    public void testGetWithoutVariableName() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat((String) cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "httpGetResponseBody")).contains("John");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testGetWithResponseHandler.cmmn")
    public void testGetWithResponseHandler() {
        CaseInstance caseInstance = createCaseInstance();

        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        Map<String, String> names = new HashMap<>();
        names.put("firstName", "John");
        names.put("lastName", "Doe");
        assertThat(variables)
                .containsExactlyInAnyOrderEntriesOf(names);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testGetWithRequestHandler.cmmn")
    public void testGetWithRequestHandler() {
        CaseInstance caseInstance = createCaseInstance();
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat(variables).hasSize(1);
        assertThat((String) variables.get("httpGetResponseBody")).contains("John");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHttpsSelfSigned.cmmn")
    public void testHttpsSelfSigned() {
        assertThat(createCaseInstance()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testConnectTimeout.cmmn")
    public void testConnectTimeout() {
        assertThatThrownBy(() -> createCaseInstance())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("IO exception occurred");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testRequestTimeout.cmmn")
    public void testRequestTimeout() {
        assertThatThrownBy(() -> createCaseInstance())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("IO exception occurred")
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testDisallowRedirects.cmmn")
    public void testDisallowRedirects() {
        assertThatThrownBy(() -> createCaseInstance())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("HTTP302");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testFailStatusCodes.cmmn")
    public void testFailStatusCodes() {
        assertThatThrownBy(() -> createCaseInstance())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("HTTP400");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHandleStatusCodes.cmmn")
    public void testHandleStatusCodes() {
        assertThat(createCaseInstance()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testIgnoreException.cmmn")
    public void testIgnoreException() {
        assertThat(createCaseInstance()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testMapException.cmmn")
    public void testMapException() {
        //exception mapping is not implemented yet in Cmmn
        assertThatThrownBy(() -> createCaseInstance())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHttpGet3XX.cmmn")
    public void testHttpGet3XX() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance).isNotNull();
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat(variables).containsEntry("httpGetResponseStatusCode", 302);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHttpGet4XX.cmmn")
    public void testHttpGet4XX() {
        assertThatThrownBy(() -> createCaseInstance())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("HTTP404");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHttpGet5XX.cmmn")
    public void testHttpGet5XX() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance).isNotNull();
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat(variables)
                .contains(
                        entry("get500RequestMethod", "GET"),
                        entry("get500RequestUrl", "https://localhost:9799/api?code=500"),
                        entry("get500RequestHeaders", "Accept: application/json"),
                        entry("get500RequestTimeout", 5000),
                        entry("get500HandleStatusCodes", "4XX, 5XX"),
                        entry("get500SaveRequestVariables", true),
                        entry("get500ResponseStatusCode", 500),
                        entry("get500ResponseReason", "Server Error")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHttpPost2XX.cmmn")
    public void testHttpPost2XX() throws Exception {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance).isNotNull();
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat(variables)
                .contains(
                        entry("httpPostRequestMethod", "POST"),
                        entry("httpPostRequestUrl", "https://localhost:9799/api?code=201"),
                        entry("httpPostRequestHeaders", "Content-Type: application/json"),
                        entry("httpPostRequestBody", "{\"test\":\"sample\",\"result\":true}"),
                        entry("httpPostResponseStatusCode", 201),
                        entry("httpPostResponseReason", "Created")
                );
        assertThat((String) variables.get("httpPostResponseBody")).contains("\"body\":\"{\\\"test\\\":\\\"sample\\\",\\\"result\\\":true}\"");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHttpPost3XX.cmmn")
    public void testHttpPost3XX() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance).isNotNull();
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat(variables)
                .contains(
                        entry("httpPostResponseStatusCode", 302)
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHttpDelete4XX.cmmn")
    public void testHttpDelete4XX() {
        CaseInstance caseInstance = createCaseInstance();

        assertThat(caseInstance).isNotNull();
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat(variables)
                .contains(
                        entry("httpDeleteResponseStatusCode", 400),
                        entry("httpDeleteResponseReason", "Bad Request")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/http/cmmn/CmmnHttpTaskTest.testHttpPut5XX.cmmn")
    public void testHttpPut5XX() throws Exception {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("prefix", "httpPost")
                .start();

        assertThat(caseInstance).isNotNull();
        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat(variables)
                .contains(
                        entry("httpPostRequestMethod", "PUT"),
                        entry("httpPostRequestUrl", "https://localhost:9799/api?code=500"),
                        entry("httpPostRequestHeaders",
                                "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000\nTest: test"),
                        entry("httpPostRequestBody", "test"),
                        entry("httpPostResponseStatusCode", 500),
                        entry("httpPostResponseReason", "Server Error")
                );

        Map<String, String> headerMap = HttpServiceTaskTestServer.HttpServiceTaskTestServlet.headerMap;
        assertThat(headerMap)
                .contains(
                        entry("Content-Type", "text/plain"),
                        entry("X-Request-ID", "623b94fc-14b8-4ee6-aed7-b16b9321e29f"),
                        entry("Host", "localhost:7000"),
                        entry("Test", "test")
                );
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/http/cmmn/CmmnHttpTaskTest.testExpressions.cmmn" }
    )
    public void testExpressions() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("method", "PUT");
        variables.put("url", "https://localhost:9799/api?code=500");
        variables.put("headers", "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000\nTest: test");
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

        assertThat(caseInstance).isNotNull();
        Map<String, Object> outputVariables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertThat(outputVariables)
                .contains(
                        entry("httpPostRequestMethod", "PUT"),
                        entry("httpPostRequestUrl", "https://localhost:9799/api?code=500"),
                        entry("httpPostRequestHeaders",
                                "Content-Type: text/plain\nX-Request-ID: 623b94fc-14b8-4ee6-aed7-b16b9321e29f\nhost:localhost:7000\nTest: test"),
                        entry("httpPostRequestBody", "test"),
                        entry("httpPostResponseStatusCode", 500),
                        entry("httpPostResponseReason", "Server Error")
                );

        Map<String, String> headerMap = HttpServiceTaskTestServer.HttpServiceTaskTestServlet.headerMap;
        assertThat(headerMap)
                .contains(
                        entry("Content-Type", "text/plain"),
                        entry("X-Request-ID", "623b94fc-14b8-4ee6-aed7-b16b9321e29f"),
                        entry("Host", "localhost:7000"),
                        entry("Test", "test")
                );
    }

    protected CaseInstance createCaseInstance() {
        return cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
    }

}
