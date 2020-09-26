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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to the Job collection and a single job resource.
 * 
 * @author Frederik Heremans
 */
public class JobResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single job.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void testGetJob() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        processEngineConfiguration.getClock().setCurrentTime(now.getTime());

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB, timerJob.getId())), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + timerJob.getId() + "',"
                        + "correlationId: '" + timerJob.getCorrelationId() + "',"
                        + "exceptionMessage: " + timerJob.getExceptionMessage() + ","
                        + "executionId: '" + timerJob.getExecutionId() + "',"
                        + "processDefinitionId: '" + timerJob.getProcessDefinitionId() + "',"
                        + "processInstanceId: '" + timerJob.getProcessInstanceId() + "',"
                        + "elementId: 'escalationTimer',"
                        + "elementName: 'Escalation',"
                        + "retries: " + timerJob.getRetries() + ","
                        + "dueDate: " + new TextNode(getISODateStringWithTZ(timerJob.getDuedate())) + ","
                        + "tenantId: ''"
                        + "}");

        // Set tenant on deployment
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));

        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB, timerJob.getId())),
                HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "tenantId: 'myTenant'"
                        + "}");
    }

    /**
     * Test getting an unexisting job.
     */
    @Test
    public void testGetUnexistingJob() throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB, "unexistingjob")), HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test executing a single job.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void testExecuteJob() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        managementService.moveTimerToExecutableJob(timerJob.getId());

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "execute");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB, timerJob.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        // Job should be executed
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult()).isNull();
    }

    /**
     * Test executing an unexisting job.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void testExecuteUnexistingJob() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "execute");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB, "unexistingjob"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test executing with bad action.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void testIllegalActionOnJob() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "unexistinAction");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB, timerJob.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST);
        closeResponse(response);
    }

    /**
     * Test deleting a single job.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void testDeleteJob() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB, timerJob.getId()));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        // Job should be deleted
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult()).isNull();
    }

    /**
     * Test deleting an unexisting job.
     */
    @Test
    public void testDeleteUnexistingJob() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB, "unexistingjob"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    public void getUnexistingTimerJob() throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB, "unexistingjob")), HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void executeTimerJobBadAction() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        managementService.moveTimerToExecutableJob(timerJob.getId());

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "badAction");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB, timerJob.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST);
        closeResponse(response);
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void executeUnexistingTimerJob() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "move");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB, "unexistingjob"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    public void deleteUnexistingTimerJob() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB, "unexistingjob"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    public void getUnexistingDeadLetterJob() throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB, "unexistingjob")), HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void executeDeadLetterJobBadAction() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        managementService.moveTimerToExecutableJob(timerJob.getId());

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "badAction");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB, timerJob.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST);
        closeResponse(response);
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobResourceTest.testTimerProcess.bpmn20.xml" })
    public void executeUnexistingDeadLetterJob() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "move");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB, "unexistingjob"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    public void deleteUnexistingDeadLetterJob() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEADLETTER_JOB, "unexistingjob"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    public void getUnexistingSuspendedJob() throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_SUSPENDED_JOB, "unexistingjob")), HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    public void deleteUnexistingSuspendedJob() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_SUSPENDED_JOB, "unexistingjob"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
