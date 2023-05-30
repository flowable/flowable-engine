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
package org.flowable.cmmn.rest.service.api.management;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single job resource.
 */
public class JobResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single job.
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/management/timerEventListenerCase.cmmn" })
    public void testGetTimerJob() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();
        Job timerJob = managementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TIMER_JOB, timerJob.getId())), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + timerJob.getId() + "',"
                        + "exceptionMessage: " + timerJob.getExceptionMessage() + ","
                        + "planItemInstanceId: '" + timerJob.getSubScopeId() + "',"
                        + "caseDefinitionId: '" + timerJob.getScopeDefinitionId() + "',"
                        + "caseInstanceId: '" + timerJob.getScopeId() + "',"
                        + "elementId: 'timerListener',"
                        + "elementName: 'Timer listener',"
                        + "retries: " + timerJob.getRetries() + ","
                        + "dueDate: " + new TextNode(getISODateStringWithTZ(timerJob.getDuedate())) + ","
                        + "tenantId: ''"
                        + "}");
        assertThat(responseNode.path("url").asText(null))
                .endsWith(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TIMER_JOB, timerJob.getId()));
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/management/timerEventListenerCase.cmmn" })
    public void testGetDeadLetterJob() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();
        Job timerJob = managementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        Job deadLetterJob = managementService.createDeadLetterJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(deadLetterJob).isNull();

        managementService.moveJobToDeadLetterJob(timerJob.getId());

        timerJob = managementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(timerJob).isNull();

        deadLetterJob = managementService.createDeadLetterJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(deadLetterJob).isNotNull();

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_DEADLETTER_JOB, deadLetterJob.getId())), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + deadLetterJob.getId() + "',"
                        + "exceptionMessage: " + deadLetterJob.getExceptionMessage() + ","
                        + "planItemInstanceId: '" + deadLetterJob.getSubScopeId() + "',"
                        + "caseDefinitionId: '" + deadLetterJob.getScopeDefinitionId() + "',"
                        + "caseInstanceId: '" + deadLetterJob.getScopeId() + "',"
                        + "elementId: 'timerListener',"
                        + "elementName: 'Timer listener',"
                        + "retries: " + deadLetterJob.getRetries() + ","
                        + "dueDate: " + new TextNode(getISODateStringWithTZ(deadLetterJob.getDuedate())) + ","
                        + "tenantId: ''"
                        + "}");
        assertThat(responseNode.path("url").asText(null))
                .endsWith(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_DEADLETTER_JOB, deadLetterJob.getId()));
    }

    /**
     * Test getting an unexisting job.
     */
    @Test
    public void testGetUnexistingJob() throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB, "unexistingjob")), HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
    
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/management/timerEventListenerCase.cmmn" })
    public void testGetLockedJob() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();
        Job timerJob = managementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        
        final Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        
        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                JobEntity jobEntity = (JobEntity) executableJob;
                jobEntity.setLockOwner("test");
                jobEntity.setLockExpirationTime(new Date());
                CommandContextUtil.getJobService(commandContext).updateJob(jobEntity);
                return null;
            }
            
        });
        
        JobEntity lockedJob = (JobEntity) managementService.createJobQuery().jobId(executableJob.getId()).singleResult();
        assertThat(lockedJob.getLockOwner()).isEqualTo("test");
        assertThat(lockedJob.getLockExpirationTime()).isNotNull();

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB, lockedJob.getId())), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + lockedJob.getId() + "',"
                        + "exceptionMessage: " + lockedJob.getExceptionMessage() + ","
                        + "planItemInstanceId: '" + lockedJob.getSubScopeId() + "',"
                        + "caseDefinitionId: '" + lockedJob.getScopeDefinitionId() + "',"
                        + "caseInstanceId: '" + lockedJob.getScopeId() + "',"
                        + "elementId: 'timerListener',"
                        + "elementName: 'Timer listener',"
                        + "retries: " + lockedJob.getRetries() + ","
                        + "dueDate: " + new TextNode(getISODateStringWithTZ(lockedJob.getDuedate())) + ","
                        + "lockOwner: 'test',"
                        + "lockExpirationTime: " + new TextNode(getISODateStringWithTZ(lockedJob.getLockExpirationTime())) + ","
                        + "tenantId: ''"
                        + "}");
        assertThat(responseNode.path("url").asText(null))
                .endsWith(CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB, lockedJob.getId()));
    }

    /**
     * Test executing a single job.
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/management/timerEventListenerCase.cmmn" })
    public void testExecuteJob() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();
        Job timerJob = managementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        managementService.moveTimerToExecutableJob(timerJob.getId());

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "execute");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB, timerJob.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        // Job should be executed
        assertThat(managementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNull();
    }
}
