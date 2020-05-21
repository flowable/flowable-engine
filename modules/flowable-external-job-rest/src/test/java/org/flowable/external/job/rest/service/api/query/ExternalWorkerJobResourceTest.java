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
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
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
class ExternalWorkerJobResourceTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected ManagementService managementService;

    @Autowired
    protected ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Test
    void getJobWithInvalidId() {

        ResponseEntity<String> response = restTemplate.getForEntity("/service/jobs/{jobId}", String.class, "invalid");

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
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void getUnlockedJobWithId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        ExternalWorkerJob externalOrderJob = managementService.createExternalWorkerJobQuery().elementId("externalOrder").singleResult();

        assertThat(externalOrderJob).isNotNull();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs/{jobId}", String.class, externalOrderJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + externalOrderJob.getId() + "',"
                        + "  correlationId: '" + externalOrderJob.getCorrelationId() + "',"
                        + "  processInstanceId: '" + processInstance1.getId() + "',"
                        + "  executionId: '" + externalOrderJob.getExecutionId() + "',"
                        + "  processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "  elementId: 'externalOrder',"
                        + "  elementName: 'Order Service',"
                        + "  retries: 3,"
                        + "  lockOwner: null,"
                        + "  lockExpirationTime: null"
                        + "}");
    }

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.bpmn20.xml")
    void getLockedJobWithId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("externalWorkerJobQueryTest");

        Instant acquireTime = Instant.parse("2020-05-08T08:22:45.583Z");
        processEngineConfiguration.getClock().setCurrentTime(Date.from(acquireTime));

        List<AcquiredExternalWorkerJob> acquiredJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("orderService", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker");

        AcquiredExternalWorkerJob acquiredJob = acquiredJobs.get(0);

        assertThat(acquiredJob).isNotNull();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs/{jobId}", String.class, acquiredJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + acquiredJob.getId() + "',"
                        + "  processInstanceId: '" + processInstance1.getId() + "',"
                        + "  executionId: '" + acquiredJob.getExecutionId() + "',"
                        + "  processDefinitionId: '" + processInstance1.getProcessDefinitionId() + "',"
                        + "  elementId: 'externalOrder',"
                        + "  elementName: 'Order Service',"
                        + "  retries: 3,"
                        + "  lockOwner: 'testWorker',"
                        + "  lockExpirationTime: '2020-05-08T08:32:45.583Z'"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/parallelExternalWorkerJobs.cmmn")
    void getUnlockedCmmnJobWithId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("externalWorkerJobQueryTest")
                .start();

        ExternalWorkerJob externalOrderJob = managementService.createExternalWorkerJobQuery().elementId("externalOrder").singleResult();

        assertThat(externalOrderJob).isNotNull();

        ResponseEntity<String> response = restTemplate
                .getForEntity("/service/jobs/{jobId}", String.class, externalOrderJob.getId());

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThatJson(body)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + externalOrderJob.getId() + "',"
                        + "  scopeId: '" + caseInstance.getId() + "',"
                        + "  subScopeId: '" + externalOrderJob.getSubScopeId() + "',"
                        + "  scopeDefinitionId: '" + caseInstance.getCaseDefinitionId() + "',"
                        + "  scopeType: 'cmmn',"
                        + "  elementId: 'externalOrder',"
                        + "  elementName: 'Order Service',"
                        + "  retries: 3,"
                        + "  lockOwner: null,"
                        + "  lockExpirationTime: null"
                        + "}");
    }

}
