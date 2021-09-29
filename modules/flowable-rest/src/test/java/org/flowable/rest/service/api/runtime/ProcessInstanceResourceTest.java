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

package org.flowable.rest.service.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a single Process instance resource.
 *
 * @author Frederik Heremans
 */
public class ProcessInstanceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single process instance.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testGetProcessInstance() throws Exception {
        Authentication.setAuthenticatedUserId("testUser");
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("processOne")
                .businessKey("myBusinessKey")
                .callbackId("testCallbackId")
                .callbackType("testCallbackType")
                .referenceId("testReferenceId")
                .referenceType("testReferenceType")
                .stageInstanceId("testStageInstanceId")
                .start();
        runtimeService.updateBusinessStatus(processInstance.getId(), "myBusinessStatus");
        Authentication.setAuthenticatedUserId(null);

        String url = buildUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        // Check resulting instance
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processInstance.getId() + "',"
                        + "startTime: '${json-unit.any-string}',"
                        + "startUserId: '" + processInstance.getStartUserId() + "',"
                        + "processDefinitionName: '" + processInstance.getProcessDefinitionName() + "',"
                        + "businessKey: 'myBusinessKey',"
                        + "businessStatus: 'myBusinessStatus',"
                        + "callbackId: 'testCallbackId',"
                        + "callbackType: 'testCallbackType',"
                        + "referenceId: 'testReferenceId',"
                        + "referenceType: 'testReferenceType',"
                        + "propagatedStageInstanceId: 'testStageInstanceId',"
                        + "suspended: false,"
                        + "tenantId: '',"
                        + "url: '" + url + "',"
                        + "processDefinitionUrl: '" + buildUrl(RestUrls.URL_PROCESS_DEFINITION, processInstance.getProcessDefinitionId()) + "'"
                        + "}"
                );

        // Check result after tenant has been changed
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));
        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId())),
                HttpStatus.SC_OK);

        // Check resulting instance tenant id
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.get("tenantId").textValue()).isEqualTo("myTenant");
    }

    /**
     * Test getting an unexisting process instance.
     */
    @Test
    public void testGetUnexistingProcessInstance() {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, "unexistingpi")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single process instance.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testDeleteProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", "myBusinessKey");
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId())), HttpStatus.SC_NO_CONTENT));

        // Check if process-instance is gone
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    /**
     * Test deleting an unexisting process instance.
     */
    @Test
    public void testDeleteUnexistingProcessInstance() {
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, "unexistingpi")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test suspending a single process instance.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testSuspendProcessInstance() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", "myBusinessKey");

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "suspend");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check engine id instance is suspended
        assertThat(runtimeService.createProcessInstanceQuery().suspended().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // Check resulting instance is suspended
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processInstance.getId() + "',"
                        + "suspended: true"
                        + "}");

        // Suspending again should result in conflict
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_CONFLICT));
    }

    /**
     * Test suspending a single process instance.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testActivateProcessInstance() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", "myBusinessKey");
        runtimeService.suspendProcessInstanceById(processInstance.getId());

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("action", "activate");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check engine id instance is suspended
        assertThat(runtimeService.createProcessInstanceQuery().active().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // Check resulting instance is suspended
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + processInstance.getId() + "',"
                        + "suspended: false"
                        + "}");

        // Activating again should result in conflict
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_CONFLICT));
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceResourceTest.process-one.bpmn20.xml" })
    public void testUpdateProcessInstance() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne");

        String url = SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId());
        HttpPut httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity("{\"name\": \"name one\"}"));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult().getName()).isEqualTo("name one");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult().getBusinessKey()).isNull();

        httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity("{\"businessKey\": \"key one\"}"));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult().getName()).isEqualTo("name one");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult().getBusinessKey()).isEqualTo("key one");

        httpPut = new HttpPut(url);
        httpPut.setEntity(new StringEntity("{\"businessKey\": \"key two\"}"));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_OK));

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult().getName()).isEqualTo("name one");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult().getBusinessKey()).isEqualTo("key two");

    }
}
