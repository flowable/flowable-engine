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
package org.flowable.rest.app;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.spring.impl.test.FlowableCmmnSpringExtension;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import tools.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({
        FlowableCmmnSpringExtension.class,
        FlowableSpringExtension.class,
})
@AutoConfigureTestRestTemplate
public class CrossEngineRestQueryTest {

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    protected CmmnTaskService cmmnTaskService;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Test
    @Deployment(resources = "processWithCaseTask.bpmn20.xml")
    @CmmnDeployment(resources = "oneHumanTaskCase.cmmn")
    void queryCaseInstancesByParentProcessInstanceId() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processWithCaseTask");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .parentProcessInstanceId(processInstance.getId())
                .singleResult();
        assertThat(caseInstance).isNotNull();

        ResponseEntity<JsonNode> response = restTemplate.withBasicAuth("rest-admin", "test")
                .getForEntity("/cmmn-api/cmmn-runtime/case-instances?parentProcessInstanceId={id}",
                        JsonNode.class, processInstance.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + caseInstance.getId() + "' }"
                        + "  ]"
                        + "}");

        response = restTemplate.withBasicAuth("rest-admin", "test")
                .getForEntity("/cmmn-api/cmmn-runtime/case-instances?parentProcessInstanceId=nonExisting",
                        JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{ data: [] }");

        response = restTemplate.withBasicAuth("rest-admin", "test")
                .getForEntity("/cmmn-api/cmmn-history/historic-case-instances?parentProcessInstanceId={id}",
                        JsonNode.class, processInstance.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + caseInstance.getId() + "' }"
                        + "  ]"
                        + "}");

        Task caseTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(caseTask.getId());

        response = restTemplate.withBasicAuth("rest-admin", "test")
                .getForEntity("/cmmn-api/cmmn-history/historic-case-instances?parentProcessInstanceId={id}",
                        JsonNode.class, processInstance.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + caseInstance.getId() + "' }"
                        + "  ]"
                        + "}");
    }

    @Test
    @Deployment(resources = "oneTaskProcess.bpmn20.xml")
    @CmmnDeployment(resources = "caseWithProcessTask.cmmn")
    void queryProcessInstancesByParentCaseInstanceId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseWithProcessTask")
                .start();

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .parentCaseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(processInstance).isNotNull();

        ResponseEntity<JsonNode> response = restTemplate.withBasicAuth("rest-admin", "test")
                .getForEntity("/service/runtime/process-instances?parentCaseInstanceId={id}",
                        JsonNode.class, caseInstance.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + processInstance.getId() + "' }"
                        + "  ]"
                        + "}");

        response = restTemplate.withBasicAuth("rest-admin", "test")
                .getForEntity("/service/runtime/process-instances?parentCaseInstanceId=nonExisting",
                        JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{ data: [] }");

        response = restTemplate.withBasicAuth("rest-admin", "test")
                .getForEntity("/service/history/historic-process-instances?parentCaseInstanceId={id}",
                        JsonNode.class, caseInstance.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + processInstance.getId() + "' }"
                        + "  ]"
                        + "}");

        Task processTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(processTask.getId());

        response = restTemplate.withBasicAuth("rest-admin", "test")
                .getForEntity("/service/history/historic-process-instances?parentCaseInstanceId={id}",
                        JsonNode.class, caseInstance.getId());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.getBody())
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  data: ["
                        + "    { id: '" + processInstance.getId() + "' }"
                        + "  ]"
                        + "}");
    }
}
