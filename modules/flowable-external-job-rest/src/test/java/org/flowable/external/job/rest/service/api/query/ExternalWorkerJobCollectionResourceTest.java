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
package org.flowable.external.job.rest.service.api.query;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.external.job.rest.service.ExternalJobRestSpringBootTest;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Filip Hrisafov
 */
@ExternalJobRestSpringBootTest
class ExternalWorkerJobCollectionResourceTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected ManagementService managementService;

    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Test
    void queryNoJobs() {

        ResponseEntity<String> response = restTemplate.getForEntity("/service/jobs", String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");

    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByJobInstanceId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ExternalWorkerJob externalOrderJob = managementService.createExternalWorkerJobQuery().elementId("externalOrder").singleResult();
        assertThat(externalOrderJob).isNotNull();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?id={id}", String.class, externalOrderJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  size: 1,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      id: '" + externalOrderJob.getId() + "',"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      executionId: '" + externalOrderJob.getExecutionId() + "',"
                        + "      processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?id={id}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");

    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByProcessInstanceId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?processInstanceId={processInstanceId}", String.class, processInstance1.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 2,"
                        + "  start: 0,"
                        + "  size: 2,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalCustomer1',"
                        + "      elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?processInstanceId={processInstanceId}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");

    }
    
    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.cmmn")
    void testQueryWithoutProcessInstanceId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?withoutProcessInstanceId=true", String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 4,"
                        + "  start: 0,"
                        + "  size: 4,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      scopeId: '" + caseInstance1.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance1.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      scopeId: '" + caseInstance1.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance1.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalCustomer1',"
                        + "      elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      scopeId: '" + caseInstance2.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance2.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      scopeId: '" + caseInstance2.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance2.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalCustomer1',"
                        + "      elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    }"
                        + "  ]"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByProcessDefinitionId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?processDefinitionId={processInstanceId}", String.class, processInstance1.getProcessDefinitionId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 4,"
                        + "  start: 0,"
                        + "  size: 4,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service'"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalCustomer1', elementName: 'Customer Service'"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance2.getId() + "',"
                        + "      processDefinitionId: '" + processInstance2.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service'"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance2.getId() + "',"
                        + "      processDefinitionId: '" + processInstance2.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalCustomer1', elementName: 'Customer Service'"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?processDefinitionId={processDefinitionId}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByExecutionId() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        Execution orderExecution = runtimeService.createExecutionQuery().activityId("externalOrder").singleResult();
        assertThat(orderExecution).isNotNull();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?executionId={executionId}", String.class, orderExecution.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  size: 1,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + orderExecution.getProcessInstanceId() + "',"
                        + "      executionId: '" + orderExecution.getId() + "',"
                        + "      processDefinitionId: '" + processInstance.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service'"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?executionId={executionId}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByElementId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?elementId={elementId}", String.class, "externalOrder");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 2,"
                        + "  start: 0,"
                        + "  size: 2,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service'"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance2.getId() + "',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service'"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?elementId={elementId}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByElementName() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?elementName={elementName}", String.class, "Customer Service");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 2,"
                        + "  start: 0,"
                        + "  size: 2,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      elementId: 'externalCustomer1', elementName: 'Customer Service'"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance2.getId() + "',"
                        + "      elementId: 'externalCustomer1', elementName: 'Customer Service'"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?elementId={elementId}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByException() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        managementService.createExternalWorkerJobFailureBuilder(acquiredJobs.get(0).getId(), "testWorker")
                .errorMessage("Error message")
                .errorDetails("Error details")
                .fail();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs", String.class, "externalOrder");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 2,"
                        + "  start: 0,"
                        + "  size: 2,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + processInstance.getId() + "',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service',"
                        + "      retries: 2, exceptionMessage: 'Error message'"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance.getId() + "',"
                        + "      elementId: 'externalCustomer1', elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?withException={withException}", String.class, "true");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  size: 1,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + processInstance.getId() + "',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service',"
                        + "      retries: 2, exceptionMessage: 'Error message'"
                        + "    }"
                        + "  ]"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByExceptionMessage() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        managementService.createExternalWorkerJobFailureBuilder(acquiredJobs.get(0).getId(), "testWorker")
                .errorMessage("Error message")
                .errorDetails("Error details")
                .fail();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?exceptionMessage={exceptionMessage}", String.class, "Error message");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  size: 1,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + processInstance.getId() + "',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service',"
                        + "      retries: 2, exceptionMessage: 'Error message'"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?exceptionMessage={exceptionMessage}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: [ ]"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByLockedAndUnlocked() {
        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?locked={locked}", String.class, "true");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  size: 1,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      elementId: 'externalOrder', elementName: 'Order Service',"
                        + "      lockOwner: 'testWorker'"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?unlocked={locked}", String.class, "true");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  size: 1,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      elementId: 'externalCustomer1', elementName: 'Customer Service'"
                        + "    }"
                        + "  ]"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.cmmn")
    void testQueryByScopeId() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?scopeId={scopeId}", String.class, caseInstance1.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 2,"
                        + "  start: 0,"
                        + "  size: 2,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      scopeId: '" + caseInstance1.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance1.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      scopeId: '" + caseInstance1.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance1.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalCustomer1',"
                        + "      elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?scopeId={scopeId}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");
    }
    
    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.cmmn")
    void testQueryWithoutScopeId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");
        
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?withoutScopeId=true", String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 4,"
                        + "  start: 0,"
                        + "  size: 4,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance1.getId() + "',"
                        + "      processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalCustomer1',"
                        + "      elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance2.getId() + "',"
                        + "      processDefinitionId: '" + processInstance2.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      processInstanceId: '" + processInstance2.getId() + "',"
                        + "      processDefinitionId: '" + processInstance2.getProcessDefinitionId() + "',"
                        + "      elementId: 'externalCustomer1',"
                        + "      elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    }"
                        + "  ]"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.cmmn")
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByScopeType() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?scopeType={scopeType}", String.class, ScopeTypes.CMMN);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 2,"
                        + "  start: 0,"
                        + "  size: 2,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      scopeId: '" + caseInstance1.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance1.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      scopeId: '" + caseInstance1.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance1.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalCustomer1',"
                        + "      elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?scopeType={scopeType}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.cmmn")
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void testQueryByScopeDefinitionId() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?scopeDefinitionId={scopeDefinitionId}", String.class, caseInstance1.getCaseDefinitionId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 2,"
                        + "  start: 0,"
                        + "  size: 2,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      scopeId: '" + caseInstance1.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance1.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalOrder',"
                        + "      elementName: 'Order Service',"
                        + "      retries: 3"
                        + "    },"
                        + "    {"
                        + "      scopeId: '" + caseInstance1.getId() + "',"
                        + "      scopeDefinitionId: '" + caseInstance1.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalCustomer1',"
                        + "      elementName: 'Customer Service',"
                        + "      retries: 3"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?scopeDefinitionId={scopeDefinitionId}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.cmmn")
    void testQueryBySubScopeId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        PlanItemInstance orderExecution = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("externalOrder").singleResult();
        assertThat(orderExecution).isNotNull();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs?subScopeId={subScopeId}", String.class, orderExecution.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  size: 1,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: ["
                        + "    {"
                        + "      scopeId: '" + orderExecution.getCaseInstanceId() + "',"
                        + "      subScopeId: '" + orderExecution.getId() + "',"
                        + "      scopeDefinitionId: '" + orderExecution.getCaseDefinitionId() + "',"
                        + "      scopeType: 'cmmn',"
                        + "      elementId: 'externalOrder', elementName: 'Order Service'"
                        + "    }"
                        + "  ]"
                        + "}");

        response = restTemplate
                .getForEntity("/service/jobs?executionId={executionId}", String.class, "invalid");

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .isEqualTo("{"
                        + "  total: 0,"
                        + "  start: 0,"
                        + "  size: 0,"
                        + "  sort: 'id',"
                        + "  order: 'asc',"
                        + "  data: []"
                        + "}");
    }

}
