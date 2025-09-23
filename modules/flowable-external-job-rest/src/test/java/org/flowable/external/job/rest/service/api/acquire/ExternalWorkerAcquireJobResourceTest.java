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
package org.flowable.external.job.rest.service.api.acquire;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.external.job.rest.service.ExternalJobRestSpringBootTest;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Filip Hrisafov
 */
@ExternalJobRestSpringBootTest
class ExternalWorkerAcquireJobResourceTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected ManagementService managementService;

    @Autowired
    protected ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    protected CmmnTaskService cmmnTaskService;

    @Autowired
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    @Test
    void acquireJobsWithInvalidParameters() {
        ObjectNode request = objectMapper.createObjectNode();

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body).isEqualTo("{"
                + "  message: 'Bad request',"
                + "  exception: 'topic is required'"
                + "}");

        request = objectMapper.createObjectNode();
        request.put("topic", "order");

        response = restTemplate.postForEntity("/service/acquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body).isEqualTo("{"
                + "  message: 'Bad request',"
                + "  exception: 'lockDuration is required'"
                + "}");

        request = objectMapper.createObjectNode();
        request.put("topic", "order");
        request.put("lockDuration", "PT10M");

        response = restTemplate.postForEntity("/service/acquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body).isEqualTo("{"
                + "  message: 'Bad request',"
                + "  exception: 'workerId is required'"
                + "}");

        request = objectMapper.createObjectNode();
        request.put("topic", "order");
        request.put("lockDuration", "PT10M");
        request.put("workerId", "testWorker");

        response = restTemplate.postForEntity("/service/acquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body).isEqualTo("[]");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void acquireJobsReturnsProcessVariables() {
        Instant instantVar = Instant.parse("2020-05-04T09:25:45.583Z");
        Date dateVar = Date.from(instantVar.plus(1, ChronoUnit.HOURS));
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .variable("stringVar", "Hello")
                .variable("intVar", 50)
                .variable("longVar", 100L)
                .variable("shortVar", (short) 10)
                .variable("doubleVar", 30.3d)
                .variable("booleanVar", true)
                .variable("dateVar", dateVar)
                .variable("instantVar", instantVar)
                .variable("localDateVar", LocalDate.of(2020, Month.APRIL, 20))
                .variable("localDateTimeVar", LocalDateTime.of(2020, Month.APRIL, 20, 12, 53, 10))
                .variable("jsonVar", objectMapper.createObjectNode().put("name", "Kermit").put("lastName", "the Frog"))
                .start();

        Instant acquireTime = Instant.parse("2020-05-08T09:25:45.583Z");
        processEngineConfiguration.getClock().setCurrentTime(Date.from(acquireTime));

        ObjectNode request = objectMapper.createObjectNode();
        request.put("topic", "simple");
        request.put("lockDuration", "PT10M");
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    processInstanceId: '" + processInstance.getId() + "',"
                        + "    processDefinitionId: '" + processInstance.getProcessDefinitionId() + "',"
                        + "    elementId: 'externalWorkerTask', elementName: 'Simple Task',"
                        + "    retries: 3,"
                        + "    lockOwner: 'testWorker1',"
                        + "    lockExpirationTime: '2020-05-08T09:35:45.583Z',"
                        + "    variables: ["
                        + "      { name: 'stringVar', type: 'string', value: 'Hello' },"
                        + "      { name: 'intVar', type: 'integer', value: 50 },"
                        + "      { name: 'longVar', type: 'long', value: 100 },"
                        + "      { name: 'shortVar', type: 'short', value: 10 },"
                        + "      { name: 'doubleVar', type: 'double', value: 30.3 },"
                        + "      { name: 'booleanVar', type: 'boolean', value: true },"
                        + "      { name: 'dateVar', type: 'date', value: '2020-05-04T10:25:45.583Z' },"
                        + "      { name: 'instantVar', type: 'instant', value: '2020-05-04T09:25:45.583Z' },"
                        + "      { name: 'localDateVar', type: 'localDate', value: '2020-04-20' },"
                        + "      { name: 'localDateTimeVar', type: 'localDateTime', value: '2020-04-20T12:53:10' },"
                        + "      { "
                        + "        name: 'jsonVar', type: 'json', "
                        + "        value: { name: 'Kermit', lastName: 'the Frog' }"
                        + "      }"
                        + "    ]"
                        + "  }"
                        + "]");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void acquireJobsReturnsCaseVariables() {
        Instant instantVar = Instant.parse("2020-05-04T09:25:45.583Z");
        Date dateVar = Date.from(instantVar.plus(1, ChronoUnit.HOURS));
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .variable("stringVar", "Hello")
                .variable("intVar", 50)
                .variable("longVar", 100L)
                .variable("shortVar", (short) 10)
                .variable("doubleVar", 30.3d)
                .variable("booleanVar", true)
                .variable("dateVar", dateVar)
                .variable("instantVar", instantVar)
                .variable("localDateVar", LocalDate.of(2020, Month.APRIL, 20))
                .variable("localDateTimeVar", LocalDateTime.of(2020, Month.APRIL, 20, 12, 53, 10))
                .variable("jsonVar", objectMapper.createObjectNode().put("name", "Kermit").put("lastName", "the Frog"))
                .start();

        Instant acquireTime = Instant.parse("2020-05-08T09:25:45.583Z");
        processEngineConfiguration.getClock().setCurrentTime(Date.from(acquireTime));

        ObjectNode request = objectMapper.createObjectNode();
        request.put("topic", "simple");
        request.put("lockDuration", "PT10M");
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    scopeId: '" + caseInstance.getId() + "',"
                        + "    scopeDefinitionId: '" + caseInstance.getCaseDefinitionId() + "',"
                        + "    scopeType: 'cmmn',"
                        + "    elementId: 'externalWorkerTask', elementName: 'External Worker',"
                        + "    retries: 3,"
                        + "    lockOwner: 'testWorker1',"
                        + "    lockExpirationTime: '2020-05-08T09:35:45.583Z',"
                        + "    variables: ["
                        + "      { name: 'stringVar', type: 'string', value: 'Hello' },"
                        + "      { name: 'intVar', type: 'integer', value: 50 },"
                        + "      { name: 'longVar', type: 'long', value: 100 },"
                        + "      { name: 'shortVar', type: 'short', value: 10 },"
                        + "      { name: 'doubleVar', type: 'double', value: 30.3 },"
                        + "      { name: 'booleanVar', type: 'boolean', value: true },"
                        + "      { name: 'dateVar', type: 'date', value: '2020-05-04T10:25:45.583Z' },"
                        + "      { name: 'instantVar', type: 'instant', value: '2020-05-04T09:25:45.583Z' },"
                        + "      { name: 'localDateVar', type: 'localDate', value: '2020-04-20' },"
                        + "      { name: 'localDateTimeVar', type: 'localDateTime', value: '2020-04-20T12:53:10' },"
                        + "      { "
                        + "        name: 'jsonVar', type: 'json', "
                        + "        value: { name: 'Kermit', lastName: 'the Frog' }"
                        + "      }"
                        + "    ]"
                        + "  }"
                        + "]");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.cmmn")
    void acquireJobsByScopeType() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("externalWorkerJobQueryTest")
                .start();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        Instant acquireTime = Instant.parse("2020-05-08T09:25:45.583Z");
        processEngineConfiguration.getClock().setCurrentTime(Date.from(acquireTime));

        ObjectNode request = objectMapper.createObjectNode();
        request.put("topic", "orderService");
        request.put("lockDuration", "PT30M");
        request.put("workerId", "testWorker1");
        request.put("scopeType", ScopeTypes.CMMN);
        request.put("numberOfTasks", 2);

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    scopeId: '" + caseInstance.getId() + "',"
                        + "    scopeDefinitionId: '" + caseInstance.getCaseDefinitionId() + "',"
                        + "    scopeType: 'cmmn',"
                        + "    elementId: 'externalOrder', elementName: 'Order Service',"
                        + "    retries: 3,"
                        + "    lockOwner: 'testWorker1',"
                        + "    lockExpirationTime: '2020-05-08T09:55:45.583Z',"
                        + "    variables: [ ]"
                        + "  }"
                        + "]");

        request = objectMapper.createObjectNode();
        request.put("topic", "customerService");
        request.put("lockDuration", "PT1H");
        request.put("workerId", "testWorker2");
        request.put("scopeType", ScopeTypes.BPMN);
        request.put("numberOfTasks", 2);

        response = restTemplate.postForEntity("/service/acquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("["
                        + "  {"
                        + "    processInstanceId: '" + processInstance.getId() + "',"
                        + "    processDefinitionId: '" + processInstance.getProcessDefinitionId() + "',"
                        + "    elementId: 'externalCustomer1', elementName: 'Customer Service',"
                        + "    retries: 3,"
                        + "    lockOwner: 'testWorker2',"
                        + "    lockExpirationTime: '2020-05-08T10:25:45.583Z',"
                        + "    variables: [ ]"
                        + "  }"
                        + "]");
    }

    @Test
    void completeJobsWithoutWorkerId() {
        ObjectNode request = objectMapper.createObjectNode();

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Bad request',"
                        + "  exception: 'workerId is required'"
                        + "}");
    }

    @Test
    void completeJobsWithInvalidJobId() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Not found',"
                        + "  exception: \"Could not find external worker job with id 'invalid'.\""
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void completeBpmnJob() {
        runtimeService.startProcessInstanceByKey("simpleExternalWorker");

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker does not hold a lock on the requested job'"
                        + "}");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker2");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker2 does not hold a lock on the requested job'"
                        + "}");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        body = response.getBody();
        assertThat(body).isNull();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000, 300);

        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskAfter");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void completeBpmnJobWithVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleExternalWorker");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        ObjectNode jsonVar = objectMapper.createObjectNode()
                .put("name", "Kermit")
                .put("lastName", "the Frog");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        ArrayNode variables = request.putArray("variables");

        variables.add(createVariableNode("stringVar", "string", "hello"));
        variables.add(createVariableNode("intVar", "integer", 50));
        variables.add(createVariableNode("longVar", "long", 100));
        variables.add(createVariableNode("shortVar", "short", 10));
        variables.add(createVariableNode("doubleVar", "double", 30.3d));
        variables.add(createVariableNode("booleanVar", "boolean", true));
        variables.add(createVariableNode("dateVar", "date", "2020-05-04T10:25:45Z"));
        variables.add(createVariableNode("instantVar", "instant", "2020-05-04T09:25:45.583Z"));
        variables.add(createVariableNode("localDateVar", "localDate", "2020-04-20"));
        variables.add(createVariableNode("localDateTimeVar", "localDateTime", "2020-04-20T12:53:10"));
        variables.add(createVariableNode("jsonVar", "json", jsonVar));

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        String body = response.getBody();
        assertThat(body).isNull();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000, 300);

        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskAfter");

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("stringVar", "hello"),
                        entry("intVar", 50),
                        entry("longVar", 100L),
                        entry("shortVar", (short) 10),
                        entry("doubleVar", 30.3d),
                        entry("booleanVar", true),
                        entry("dateVar", Date.from(Instant.parse("2020-05-04T10:25:45Z"))),
                        entry("instantVar", Instant.parse("2020-05-04T09:25:45.583Z")),
                        entry("localDateVar", LocalDate.of(2020, Month.APRIL, 20)),
                        entry("localDateTimeVar", LocalDateTime.of(2020, Month.APRIL, 20, 12, 53, 10)),
                        entry("jsonVar", jsonVar)
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void completeCmmnJob() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker does not hold a lock on the requested job'"
                        + "}");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker2");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker2 does not hold a lock on the requested job'"
                        + "}");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        body = response.getBody();
        assertThat(body).isNull();

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000, 300, true);

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void completeCmmnJobWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        ObjectNode jsonVar = objectMapper.createObjectNode()
                .put("name", "Kermit")
                .put("lastName", "the Frog");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        ArrayNode variables = request.putArray("variables");

        variables.add(createVariableNode("stringVar", "string", "hello"));
        variables.add(createVariableNode("intVar", "integer", 50));
        variables.add(createVariableNode("longVar", "long", 100));
        variables.add(createVariableNode("shortVar", "short", 10));
        variables.add(createVariableNode("doubleVar", "double", 30.3d));
        variables.add(createVariableNode("booleanVar", "boolean", true));
        variables.add(createVariableNode("dateVar", "date", "2020-05-04T10:25:45Z"));
        variables.add(createVariableNode("instantVar", "instant", "2020-05-04T09:25:45.583Z"));
        variables.add(createVariableNode("localDateVar", "localDate", "2020-04-20"));
        variables.add(createVariableNode("localDateTimeVar", "localDateTime", "2020-04-20T12:53:10"));
        variables.add(createVariableNode("jsonVar", "json", jsonVar));

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/complete", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        String body = response.getBody();
        assertThat(body).isNull();

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000, 300, true);

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerCompleteTask");

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("stringVar", "hello"),
                        entry("intVar", 50),
                        entry("longVar", 100L),
                        entry("shortVar", (short) 10),
                        entry("doubleVar", 30.3d),
                        entry("booleanVar", true),
                        entry("dateVar", Date.from(Instant.parse("2020-05-04T10:25:45Z"))),
                        entry("instantVar", Instant.parse("2020-05-04T09:25:45.583Z")),
                        entry("localDateVar", LocalDate.of(2020, Month.APRIL, 20)),
                        entry("localDateTimeVar", LocalDateTime.of(2020, Month.APRIL, 20, 12, 53, 10)),
                        entry("jsonVar", jsonVar)
                );
    }

    @Test
    void terminateJobsWithoutWorkerId() {
        ObjectNode request = objectMapper.createObjectNode();

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs/{jobId}/cmmnTerminate", request, String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Bad request',"
                        + "  exception: 'workerId is required'"
                        + "}");
    }

    @Test
    void terminateJobsWithInvalidJobId() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs/{jobId}/cmmnTerminate", request, String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Not found',"
                        + "  exception: \"Could not find external worker job with id 'invalid'.\""
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void terminateBpmnJobShouldNotbePossible() {
        runtimeService.startProcessInstanceByKey("simpleExternalWorker");

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/cmmnTerminate", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body).isEqualTo("{"
                + "  message: 'Bad request',"
                + "  exception: \"Can only terminate CMMN external job. Job with id '" + externalWorkerJob.getId() + "' is from scope 'null'\""
                + "}");

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000, 300);

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void terminateCmmnJob() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/cmmnTerminate", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker does not hold a lock on the requested job'"
                        + "}");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker2");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/cmmnTerminate", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker2 does not hold a lock on the requested job'"
                        + "}");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/cmmnTerminate", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        body = response.getBody();
        assertThat(body).isNull();

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000, 300, true);

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerTerminateTask");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void terminateCmmnJobWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        ArrayNode variables = request.putArray("variables");

        variables.add(createVariableNode("terminateReason", "string", "test"));

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/cmmnTerminate", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        String body = response.getBody();
        assertThat(body).isNull();

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000, 300, true);

        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("afterExternalWorkerTerminateTask");

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("terminateReason", "test")
                );
    }

    @Test
    void bpmnErrorJobsWithoutWorkerId() {
        ObjectNode request = objectMapper.createObjectNode();

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs/{jobId}/bpmnError", request, String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Bad request',"
                        + "  exception: 'workerId is required'"
                        + "}");
    }

    @Test
    void bpmnErrorJobsWithInvalidJobId() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs/{jobId}/bpmnError", request, String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Not found',"
                        + "  exception: \"Could not find external worker job with id 'invalid'.\""
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJobWithBpmnError.bpmn20.xml")
    void bpmnErrorBpmnJob() {
        runtimeService.startProcessInstanceByKey("simpleExternalWorkerWithError");

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/bpmnError", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker does not hold a lock on the requested job'"
                        + "}");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker2");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/bpmnError", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker2 does not hold a lock on the requested job'"
                        + "}");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/bpmnError", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        body = response.getBody();
        assertThat(body).isNull();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000, 300);

        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskAfterError");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJobWithBpmnError.bpmn20.xml")
    void bpmnErrorBpmnJobWithVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleExternalWorkerWithError");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        request.put("errorCode", "errorOne");
        ArrayNode variables = request.putArray("variables");

        variables.add(createVariableNode("errorReason", "string", "test"));

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/bpmnError", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        String body = response.getBody();
        assertThat(body).isNull();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000, 300);

        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskAfterError");

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("errorReason", "test")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void bpmmnErrorCmmnJob() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/bpmnError", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body).isEqualTo("{"
                + "  message: 'Bad request',"
                + "  exception: \"Can only complete BPMN external job with a BPMN error. Job with id '" + externalWorkerJob.getId()
                + "' is from scope 'cmmn'\""
                + "}");

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000, 300, true);

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();
        assertThat(managementService.createExternalWorkerJobQuery().singleResult()).isNotNull();
    }

    @Test
    void failJobsWithoutWorkerId() {
        ObjectNode request = objectMapper.createObjectNode();

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Bad request',"
                        + "  exception: 'workerId is required'"
                        + "}");
    }

    @Test
    void failJobsWithInvalidJobId() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NOT_FOUND);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Not found',"
                        + "  exception: \"Could not find external worker job with id 'invalid'.\""
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void failBpmnJob() {
        runtimeService.startProcessInstanceByKey("simpleExternalWorker");

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker does not hold a lock on the requested job'"
                        + "}");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker2");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker2 does not hold a lock on the requested job'"
                        + "}");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        body = response.getBody();
        assertThat(body).isNull();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000, 300);

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(2);
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isNull();
        assertThat(managementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId())).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void failBpmnJobWithCustomValues() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleExternalWorker");

        assertThat(taskService.createTaskQuery().list()).isEmpty();
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        Instant failTime = Instant.parse("2020-05-12T09:25:45.583Z");
        processEngineConfiguration.getClock().setCurrentTime(Date.from(failTime));

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        request.put("retries", 10);
        request.put("retryTimeout", "PT30M");
        request.put("errorMessage", "Error message");
        request.put("errorDetails", "Error details");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        String body = response.getBody();
        assertThat(body).isNull();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000, 300);

        assertThat(taskService.createTaskQuery().list()).isEmpty();

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(10);
        assertThat(externalWorkerJob.getLockExpirationTime()).isEqualTo(Date.from(Instant.parse("2020-05-12T09:55:45.583Z")));
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Error message");
        assertThat(managementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId())).isEqualTo("Error details");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void failCmmnJob() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker does not hold a lock on the requested job'"
                        + "}");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker2");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.FORBIDDEN);
        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  message: 'Forbidden',"
                        + "  exception: 'testWorker2 does not hold a lock on the requested job'"
                        + "}");

        request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        body = response.getBody();
        assertThat(body).isNull();

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000, 300, true);

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(2);
        assertThat(externalWorkerJob.getLockExpirationTime()).isNull();
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isNull();
        assertThat(managementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId())).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void failCmmnJobWithCustomValues() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        ExternalWorkerJob externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();
        assertThat(externalWorkerJob).isNotNull();

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1");

        Instant failTime = Instant.parse("2020-05-12T09:25:45.583Z");

        processEngineConfiguration.getClock().setCurrentTime(Date.from(failTime));
        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        request.put("retries", 7);
        request.put("retryTimeout", "PT1H");
        request.put("errorMessage", "Error case message");
        request.put("errorDetails", "Error case details");

        ResponseEntity<String> response = restTemplate
                .postForEntity("/service/acquire/jobs/{jobId}/fail", request, String.class, externalWorkerJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        String body = response.getBody();
        assertThat(body).isNull();

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000, 300, true);

        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        externalWorkerJob = managementService.createExternalWorkerJobQuery().singleResult();

        assertThat(externalWorkerJob).isNotNull();
        assertThat(externalWorkerJob.getRetries()).isEqualTo(7);
        assertThat(externalWorkerJob.getLockExpirationTime()).isEqualTo(Date.from(Instant.parse("2020-05-12T10:25:45.583Z")));
        assertThat(externalWorkerJob.getLockOwner()).isNull();
        assertThat(externalWorkerJob.getExceptionMessage()).isEqualTo("Error case message");
        assertThat(managementService.getExternalWorkerJobErrorDetails(externalWorkerJob.getId())).isEqualTo("Error case details");
    }

    protected ObjectNode createVariableNode(String name, String type, Object value) {
        ObjectNode variable = objectMapper.createObjectNode();
        variable.put("name", name);
        variable.put("type", type);
        if (value instanceof String) {
            variable.put("value", (String) value);
        } else if (value instanceof Double) {
            variable.put("value", ((Double) value).doubleValue());
        } else if (value instanceof Number) {
            variable.put("value", ((Number) value).longValue());
        } else if (value instanceof JsonNode) {
            variable.set("value", (JsonNode) value);
        } else if (value instanceof Boolean) {
            variable.put("value", (Boolean) value);
        } else {
            throw new RuntimeException("cannot create variable node for value " + value);
        }

        return variable;
    }
}
