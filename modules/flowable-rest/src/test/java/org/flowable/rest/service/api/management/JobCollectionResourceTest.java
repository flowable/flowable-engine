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
package org.flowable.rest.service.api.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Calendar;
import java.util.Collections;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to the Job collection and a single job resource.
 * 
 * @author Frederik Heremans
 */
public class JobCollectionResourceTest extends BaseSpringRestTestCase {

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobCollectionResourceTest.testTimerProcess.bpmn20.xml" })
    public void testGetJobs() throws Exception {
        Calendar hourAgo = Calendar.getInstance();
        hourAgo.add(Calendar.HOUR, -1);

        Calendar inAnHour = Calendar.getInstance();
        inAnHour.add(Calendar.HOUR, 1);

        // Start process, forcing error on job-execution
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess", Collections.singletonMap("error", (Object) Boolean.TRUE));

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).timers().singleResult();
        assertThat(timerJob).isNotNull();

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?id=" + timerJob.getId()), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        JsonNode timerJobNode = responseNode.get("data").get(0);
        assertThat(timerJobNode.get("id").asText()).isEqualTo(timerJob.getId());
        assertThat(timerJobNode.get("url").asText()).contains("management/timer-jobs/" + timerJob.getId());
        
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION);
        assertResultsPresentInDataResponse(url, timerJob.getId());

        // Fetch using id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?id=" + timerJob.getId();
        assertResultsPresentInDataResponse(url, timerJob.getId());
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?id=" + timerJob.getId() + "xyzzy";
        assertResultsPresentInDataResponse(url);

        // Fetch using processInstanceId
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?processInstanceId=" + processInstance.getId();
        assertResultsPresentInDataResponse(url, timerJob.getId());
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?processInstanceId=" + processInstance.getId() + "xyzzy";
        assertResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?withoutProcessInstanceId=true";
        assertResultsPresentInDataResponse(url);

        // Fetch using executionId
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?executionId=" + timerJob.getExecutionId();
        assertResultsPresentInDataResponse(url, timerJob.getId());
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?executionId=" + timerJob.getExecutionId() + "xyzzy";
        assertResultsPresentInDataResponse(url);

        // Fetch using processDefinitionId
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?processDefinitionId=" + timerJob.getProcessDefinitionId();
        assertResultsPresentInDataResponse(url, timerJob.getId());
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?processDefinitionId=" + timerJob.getProcessDefinitionId() + "xyzzy";
        assertResultsPresentInDataResponse(url);
        // Fetch using dueBefore
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?dueBefore=" + getISODateString(inAnHour.getTime());
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?dueBefore=" + getISODateString(hourAgo.getTime());
        assertResultsPresentInDataResponse(url);

        // Fetch using dueAfter
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?dueAfter=" + getISODateString(hourAgo.getTime());
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?dueAfter=" + getISODateString(inAnHour.getTime());
        assertResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?elementId=escalationTimer";
        assertResultsPresentInDataResponse(url, timerJob.getId());
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?elementId=unknown";
        assertEmptyResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?elementName=Escalation";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?elementName=unknown";
        assertEmptyResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?executable";
        assertEmptyResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?tenantId=xyzzy";
        assertResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?tenantIdLike=xyzzy";
        assertResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?timersOnly=true";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?messagesOnly=true";
        assertResultsPresentInDataResponse(url);

        // Combining messagesOnly with timersOnly should result in exception
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?timersOnly=true&messagesOnly=true"), HttpStatus.SC_BAD_REQUEST));

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?withException=true";
        assertResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?exceptionMessage=";
        assertResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?exceptionMessage=FlowableException";
        assertResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_COLLECTION) + "?withoutScopeId=true";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).timers().singleResult();
        for (int i = 0; i < timerJob2.getRetries(); i++) {
            // Force execution of job until retries are exhausted
            assertThatThrownBy(() -> {
                managementService.moveTimerToExecutableJob(timerJob2.getId());
                managementService.executeJob(timerJob2.getId());
            })
            .isExactlyInstanceOf(FlowableException.class);
        }

        timerJob = managementService.createDeadLetterJobQuery().processInstanceId(processInstance.getId()).timers().singleResult();
        assertThat(timerJob.getRetries()).isZero();

        // Fetch the async-job (which has retries left)
        Job asyncJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

        // Test fetching all jobs
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION);
        assertResultsPresentInDataResponse(url, asyncJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION);
        
        response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION)), HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        JsonNode deadletterJobNode = responseNode.get("data").get(0);
        assertThat(deadletterJobNode.get("id").asText()).isEqualTo(timerJob.getId());
        assertThat(deadletterJobNode.get("url").asText()).contains("management/deadletter-jobs/" + timerJob.getId());
        
        assertResultsPresentInDataResponse(url, timerJob.getId());

        // Fetch using job-id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?id=" + asyncJob.getId();
        assertResultsPresentInDataResponse(url, asyncJob.getId());

        // Fetch using processInstanceId
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?processInstanceId=" + processInstance.getId();
        assertResultsPresentInDataResponse(url, asyncJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?processInstanceId=unexisting";
        assertResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?withoutProcessInstanceId=true";
        assertResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?withoutProcessInstanceId=true";
        assertResultsPresentInDataResponse(url);

        // Fetch using executionId
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?executionId=" + asyncJob.getExecutionId();
        assertResultsPresentInDataResponse(url, asyncJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?executionId=" + timerJob.getExecutionId();
        assertResultsPresentInDataResponse(url, timerJob.getId());

        // Fetch using processDefinitionId
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?processDefinitionId=" + processInstance.getProcessDefinitionId();
        assertResultsPresentInDataResponse(url, asyncJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?processDefinitionId=" + processInstance.getProcessDefinitionId();
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?processDefinitionId=unexisting";
        assertResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?processDefinitionId=unexisting";
        assertResultsPresentInDataResponse(url);
        
        // Fetch using element id and name
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?elementId=escalationTimer";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?elementId=unexisting";
        assertResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?elementName=Escalation";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?elementName=unexisting";
        assertResultsPresentInDataResponse(url);

        // Fetch using withRetriesLeft
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?withRetriesLeft=true";
        assertResultsPresentInDataResponse(url, asyncJob.getId());
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?withoutScopeId=true";
        assertResultsPresentInDataResponse(url, asyncJob.getId());
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?withoutScopeId=true";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        // Fetch using executable
        // url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION)
        // + "?executable=true";
        // assertResultsPresentInDataResponse(url, asyncJob.getId());

        // Fetch using timers only
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?timersOnly=true";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        // Combining messagesOnly with timersOnly should result in exception
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?timersOnly=true&messagesOnly=true"), HttpStatus.SC_BAD_REQUEST));

        // Fetch using withException
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?withException=true";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        // Fetch with exceptionMessage
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?exceptionMessage=" + encode(timerJob.getExceptionMessage());
        assertResultsPresentInDataResponse(url, timerJob.getId());

        // Fetch with empty exceptionMessage
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?exceptionMessage=";
        assertResultsPresentInDataResponse(url);

        // Without tenant id, before tenant update
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url, asyncJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        // Set tenant on deployment
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));

        // Without tenant id, after tenant update
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url);

        // Tenant id
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?tenantId=myTenant";
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?tenantId=anotherTenant";
        assertResultsPresentInDataResponse(url);

        // Tenant id like
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?tenantIdLike=" + encode("%enant");
        assertResultsPresentInDataResponse(url, asyncJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB_COLLECTION) + "?tenantIdLike=" + encode("%enant");
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_COLLECTION) + "?tenantIdLike=anotherTenant";
        assertResultsPresentInDataResponse(url);

    }
}
