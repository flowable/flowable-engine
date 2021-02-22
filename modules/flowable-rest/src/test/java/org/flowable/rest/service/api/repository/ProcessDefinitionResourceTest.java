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
package org.flowable.rest.service.api.repository;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to single a Process Definition resource.
 * 
 * @author Frederik Heremans
 */
public class ProcessDefinitionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single process definition. GET repository/process-definitions/{processDefinitionResource}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetProcessDefinition() throws Exception {

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processDefinition.getId() + "',"
                        + "name: '" + processDefinition.getName() + "',"
                        + "key: '" + processDefinition.getKey() + "',"
                        + "category: '" + processDefinition.getCategory() + "',"
                        + "version: " + processDefinition.getVersion() + ","
                        + "description: '" + processDefinition.getDescription() + "',"
                        + "url: '" + httpGet.getURI().toString() + "',"
                        + "deploymentId: '" + processDefinition.getDeploymentId() + "',"
                        + "deploymentUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, processDefinition.getDeploymentId()) + "',"
                        + "resource: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, processDefinition.getDeploymentId(), processDefinition.getResourceName())
                        + "',"
                        + "graphicalNotationDefined: false,"
                        + "diagramResource: null"
                        + "}");
    }

    /**
     * Test getting a single process definition with a graphical notation defined. GET repository/process-definitions/{processDefinitionResource}
     */
    @Test
    @Deployment
    public void testGetProcessDefinitionWithGraphicalNotation() throws Exception {

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processDefinition.getId() + "',"
                        + "name: " + processDefinition.getName() + ","
                        + "key: '" + processDefinition.getKey() + "',"
                        + "category: '" + processDefinition.getCategory() + "',"
                        + "version: " + processDefinition.getVersion() + ","
                        + "description: " + processDefinition.getDescription() + ","
                        + "url: '" + httpGet.getURI().toString() + "',"
                        + "deploymentId: '" + processDefinition.getDeploymentId() + "',"
                        + "deploymentUrl: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, processDefinition.getDeploymentId()) + "',"
                        + "resource: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, processDefinition.getDeploymentId(), processDefinition.getResourceName())
                        + "',"
                        + "graphicalNotationDefined: true,"
                        + "diagramResource: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, processDefinition.getDeploymentId(),
                                processDefinition.getDiagramResourceName()) + "'"
                        + "}");
    }

    /**
     * Test getting an unexisting process-definition. GET repository/process-definitions/{processDefinitionId}
     */
    @Test
    public void testGetUnexistingProcessDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test suspending a process definition. POST repository/process-definitions/{processDefinitionId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testSuspendProcessDefinition() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isFalse();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "suspend");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "suspended: true"
                        + "}");

        // Check if process-definition is suspended
        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isTrue();
    }

    /**
     * Test suspending a process definition on a certain date. POST repository/process-definitions/{processDefinitionId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testSuspendProcessDefinitionDelayed() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isFalse();

        ObjectNode requestNode = objectMapper.createObjectNode();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 2);

        // Format the date using ISO date format
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        String dateString = formatter.print(cal.getTimeInMillis());

        requestNode.put("action", "suspend");
        requestNode.put("date", dateString);

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "suspended: true"
                        + "}");

        // Check if process-definition is not yet suspended
        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isFalse();

        // Force suspension by altering time
        cal.add(Calendar.HOUR, 1);
        processEngineConfiguration.getClock().setCurrentTime(cal.getTime());
        waitForJobExecutorToProcessAllJobs(7000, 100);

        // Check if process-definition is suspended
        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isTrue();
    }

    /**
     * Test suspending already suspended process definition. POST repository/process-definitions/{processDefinitionId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testSuspendAlreadySuspendedProcessDefinition() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        repositoryService.suspendProcessDefinitionById(processDefinition.getId());

        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isTrue();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "suspend");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_CONFLICT);
        closeResponse(response);
    }

    /**
     * Test activating a suspended process definition. POST repository/process-definitions/{processDefinitionId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testActivateProcessDefinition() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        repositoryService.suspendProcessDefinitionById(processDefinition.getId());

        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isTrue();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "activate");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "suspended: false"
                        + "}");

        // Check if process-definition is suspended
        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isFalse();
    }

    /**
     * Test activating a suspended process definition delayed. POST repository/process-definitions/{processDefinitionId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testActivateProcessDefinitionDelayed() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        repositoryService.suspendProcessDefinitionById(processDefinition.getId());

        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isTrue();

        ObjectNode requestNode = objectMapper.createObjectNode();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 2);

        // Format the date using ISO date format
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        String dateString = formatter.print(cal.getTimeInMillis());

        requestNode.put("action", "activate");
        requestNode.put("date", dateString);

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "suspended: false"
                        + "}");

        // Check if process-definition is not yet active
        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isTrue();

        // Force activation by altering time
        cal.add(Calendar.HOUR, 1);
        processEngineConfiguration.getClock().setCurrentTime(cal.getTime());
        waitForJobExecutorToProcessAllJobs(7000, 100);

        // Check if process-definition is activated
        processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isFalse();
    }

    /**
     * Test activating already active process definition. POST repository/process-definitions/{processDefinitionId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testActivateAlreadyActiveProcessDefinition() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isFalse();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "activate");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_CONFLICT);
        closeResponse(response);
    }

    /**
     * Test executing an unexisting action.
     * 
     * POST repository/process-definitions/{processDefinitionId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testIllegalAction() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.isSuspended()).isFalse();

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "unexistingaction");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_BAD_REQUEST);
        closeResponse(response);
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetProcessDefinitionResourceData() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_RESOURCE_CONTENT, processDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).contains("The One Task Process");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetProcessDefinitionModel() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_MODEL, processDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode resultNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(resultNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "processes : [ "
                        + "              {"
                        + "                id: 'oneTaskProcess'"
                        + "              }"
                        + "            ]"
                        + "}"
                );
    }

    /**
     * Test getting model for an unexisting process-definition .
     */
    @Test
    public void testGetModelForUnexistingProcessDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_MODEL, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test getting resource content for an unexisting process-definition .
     */
    @Test
    public void testGetResourceContentForUnexistingProcessDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_RESOURCE_CONTENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test activating a suspended process definition delayed. POST repository/process-definitions/{processDefinitionId}
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testUpdateProcessDefinitionCategory() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionCategory("OneTaskCategory").count()).isEqualTo(1);

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("category", "updatedcategory");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION, processDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "   category: 'updatedcategory'"
                        + "}"
                );

        // Check actual entry in DB
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionCategory("updatedcategory").count()).isEqualTo(1);

    }

}
