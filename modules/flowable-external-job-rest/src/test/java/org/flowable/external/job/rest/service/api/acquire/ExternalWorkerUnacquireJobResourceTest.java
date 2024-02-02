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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.Deployment;
import org.flowable.external.job.rest.service.ExternalJobRestSpringBootTest;
import org.flowable.job.api.ExternalWorkerJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExternalJobRestSpringBootTest
class ExternalWorkerUnacquireJobResourceTest {

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
    protected CmmnManagementService cmmnManagementService;

    @Autowired
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void unacquireBpmnJobs() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();
        
        ExternalWorkerJob job = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1").get(0);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(0);
        
        job = managementService.createExternalWorkerJobQuery().jobId(job.getId()).singleResult();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
    }
    
    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void unacquireBpmnJobsWithWrongWorkerId() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();
        
        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1").get(0);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "invalid");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);
    }
    
    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void unacquireBpmnJobsWithTenantId() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .overrideProcessDefinitionTenantId("tenant1")
                .start();
        
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .overrideProcessDefinitionTenantId("tenant1")
                .start();
        
        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(2, "testWorker1");
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(2);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        request.put("tenantId", "tenant1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(0);
    }
    
    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void unacquireBpmnJobsWithWrongTenantId() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .overrideProcessDefinitionTenantId("tenant1")
                .start();
        
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .overrideProcessDefinitionTenantId("tenant2")
                .start();
        
        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(2, "testWorker1");
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(2);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        request.put("tenantId", "tenant1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(2);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void unacquireCmmnJobs() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();
        
        ExternalWorkerJob job = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1").get(0);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(0);
        
        job = cmmnManagementService.createExternalWorkerJobQuery().jobId(job.getId()).singleResult();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void unacquireCmmnJobsWithWrongWorkerId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();
        
        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1").get(0);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "invalid");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void unacquireCmmnJobsWithTenantId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .overrideCaseDefinitionTenantId("tenant1")
                .start();
        
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .overrideCaseDefinitionTenantId("tenant1")
                .start();
        
        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(2, "testWorker1");
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(2);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        request.put("tenantId", "tenant1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(0);
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void unacquireCmmnJobsWithWrongTenantId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .overrideCaseDefinitionTenantId("tenant1")
                .start();
        
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .overrideCaseDefinitionTenantId("tenant2")
                .start();
        
        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(2, "testWorker1");
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(2);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");
        request.put("tenantId", "tenant1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(2);
    }
    
    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void unacquireBpmnJobById() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();
        
        ExternalWorkerJob job = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1").get(0);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs/" + job.getId(), request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(0);
        
        job = managementService.createExternalWorkerJobQuery().jobId(job.getId()).singleResult();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
    }
    
    @Test
    @Deployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.bpmn20.xml")
    void unacquireBpmnJobWithWrongId() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("simpleExternalWorker")
                .start();
        
        managementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1").get(0);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs/invalid", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NOT_FOUND);
        
        assertThat(managementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void unacquireCmmnJobById() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();
        
        ExternalWorkerJob job = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1").get(0);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs/" + job.getId(), request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NO_CONTENT);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(0);
        
        job = cmmnManagementService.createExternalWorkerJobQuery().jobId(job.getId()).singleResult();
        assertThat(job.getLockOwner()).isNull();
        assertThat(job.getLockExpirationTime()).isNull();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/external/job/rest/service/api/simpleExternalWorkerJob.cmmn")
    void unacquireCmmnJobWithWrongId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleExternalWorker")
                .start();
        
        cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("simple", Duration.ofMinutes(10))
                .acquireAndLock(1, "testWorker1").get(0);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("workerId", "testWorker1");

        ResponseEntity<String> response = restTemplate.postForEntity("/service/unacquire/jobs/invalid", request, String.class);

        assertThat(response.getStatusCode()).as(response.toString()).isEqualTo(HttpStatus.NOT_FOUND);
        
        assertThat(cmmnManagementService.createExternalWorkerJobQuery().lockOwner("testWorker1").count()).isEqualTo(1);
    }
}
