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
package org.flowable.rest.app.variable;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.spring.impl.test.FlowableCmmnSpringExtension;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({
        FlowableCmmnSpringExtension.class,
        FlowableSpringExtension.class,
})
@AutoConfigureTestRestTemplate
public class RestJsonVariableTest {

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    @Deployment(resources = "oneTaskProcess.bpmn20.xml")
    void createProcessWithJsonVariable() {
        ObjectNode request = objectMapper.createObjectNode()
                .put("processDefinitionKey", "oneTaskProcess")
                .put("returnVariables", true);
        ArrayNode variables = request.putArray("variables");
        variables.addObject()
                .put("name", "customer")
                .put("type", "json")
                .putObject("value")
                .put("name", "Kermit");
        ResponseEntity<JsonNode> response = restTemplate.withBasicAuth("rest-admin", "test")
                .postForEntity("/service/runtime/process-instances", request, JsonNode.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.CREATED);

        assertThatJson(response.getBody())
                .when(Option.IGNORING_ARRAY_ORDER)
                .inPath("variables")
                .isEqualTo("""
                        [
                          {
                            name: 'customer',
                            type: 'json',
                            value: {
                              'name': 'Kermit'
                            },
                            scope: 'local'
                          }
                        ]
                        """);
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("oneTaskProcess")
                .includeProcessVariables()
                .singleResult();

        assertThat(processInstance.getProcessVariables())
                .extractingByKey("customer")
                .isInstanceOf(ObjectNode.class);
    }

    @Test
    @CmmnDeployment(resources = "oneHumanTaskCase.cmmn")
    void createCaseWithJsonVariable() {
        ObjectNode request = objectMapper.createObjectNode()
                .put("caseDefinitionKey", "oneHumanTaskCase")
                .put("returnVariables", true);
        ArrayNode variables = request.putArray("variables");
        variables.addObject()
                .put("name", "customer")
                .put("type", "json")
                .putObject("value")
                .put("name", "Kermit");
        ResponseEntity<JsonNode> response = restTemplate.withBasicAuth("rest-admin", "test")
                .postForEntity("/cmmn-api/cmmn-runtime/case-instances", request, JsonNode.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.CREATED);

        assertThatJson(response.getBody())
                .when(Option.IGNORING_ARRAY_ORDER)
                .inPath("variables")
                .isEqualTo("""
                        [
                          {
                            name: 'customer',
                            type: 'json',
                            value: {
                              'name': 'Kermit'
                            },
                            scope: 'local'
                          }
                        ]
                        """);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("oneHumanTaskCase")
                .includeCaseVariables()
                .singleResult();

        assertThat(caseInstance.getCaseVariables())
                .extractingByKey("customer")
                .isInstanceOf(ObjectNode.class);
    }

}
