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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processOne", "myBusinessKey");
        Authentication.setAuthenticatedUserId(null);

        String url = buildUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(url), HttpStatus.SC_OK);

        // Check resulting instance
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(processInstance.getId(), responseNode.get("id").textValue());
        assertTrue("has startTime", responseNode.has("startTime"));
        assertNotNull("startTime", responseNode.get("startTime").textValue());
        assertEquals(processInstance.getStartUserId(), responseNode.get("startUserId").textValue());
        assertEquals(processInstance.getProcessDefinitionName(), responseNode.get("processDefinitionName").textValue());
        assertEquals("myBusinessKey", responseNode.get("businessKey").textValue());
        assertFalse(responseNode.get("suspended").booleanValue());
        assertEquals("", responseNode.get("tenantId").textValue());

        assertEquals(responseNode.get("url").asText(), url);
        assertEquals(responseNode.get("processDefinitionUrl").asText(), buildUrl(RestUrls.URL_PROCESS_DEFINITION, processInstance.getProcessDefinitionId()));

        // Check result after tenant has been changed
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));
        response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE, processInstance.getId())), HttpStatus.SC_OK);

        // Check resulting instance tenant id
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals("myTenant", responseNode.get("tenantId").textValue());
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
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
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
        assertEquals(1, runtimeService.createProcessInstanceQuery().suspended().processInstanceId(processInstance.getId()).count());

        // Check resulting instance is suspended
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(processInstance.getId(), responseNode.get("id").textValue());
        assertTrue(responseNode.get("suspended").booleanValue());

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
        assertEquals(1, runtimeService.createProcessInstanceQuery().active().processInstanceId(processInstance.getId()).count());

        // Check resulting instance is suspended
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(processInstance.getId(), responseNode.get("id").textValue());
        assertFalse(responseNode.get("suspended").booleanValue());

        // Activating again should result in conflict
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_CONFLICT));
    }
}
