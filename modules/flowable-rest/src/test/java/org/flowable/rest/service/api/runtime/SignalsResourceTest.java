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

import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.*;

/**
 * @author Frederik Heremans
 */
public class SignalsResourceTest extends BaseSpringRestTestCase {

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/SignalsResourceTest.process-signal-start.bpmn20.xml" })
    public void testSignalEventReceivedSync() throws Exception {

        org.flowable.engine.repository.Deployment tenantDeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/rest/service/api/runtime/SignalsResourceTest.process-signal-start.bpmn20.xml").tenantId("my tenant").deploy();

        try {

            // Signal without vars, without tenant
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("signalName", "The Signal");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_SIGNALS));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

            // Check if process is started as a result of the signal without tenant ID set
            assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceWithoutTenantId().processDefinitionKey("processWithSignalStart1").count());

            // Signal with tenant
            requestNode.put("tenantId", "my tenant");
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

            // Check if process is started as a result of the signal, in the right tenant
            assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceTenantId("my tenant").processDefinitionKey("processWithSignalStart1").count());

            // Signal with tenant AND variables
            ArrayNode vars = requestNode.putArray("variables");
            ObjectNode var = vars.addObject();
            var.put("name", "testVar");
            var.put("value", "test");

            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

            // Check if process is started as a result of the signal, in the right tenant and with var set
            assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceTenantId("my tenant").processDefinitionKey("processWithSignalStart1").variableValueEquals("testVar", "test").count());

            // Signal without tenant AND variables
            requestNode.remove("tenantId");

            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_NO_CONTENT));

            // Check if process is started as a result of the signal, without tenant and with var set
            assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceWithoutTenantId().processDefinitionKey("processWithSignalStart1").variableValueEquals("testVar", "test").count());

        } finally {
            // Clean up tenant-specific deployment
            if (tenantDeployment != null) {
                repositoryService.deleteDeployment(tenantDeployment.getId(), true);
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/SignalsResourceTest.process-signal-start.bpmn20.xml" })
    public void testSignalEventReceivedAsync() throws Exception {

        org.flowable.engine.repository.Deployment tenantDeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/rest/service/api/runtime/SignalsResourceTest.process-signal-start.bpmn20.xml").tenantId("my tenant").deploy();

        try {

            // Signal without vars, without tenant
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("signalName", "The Signal");
            requestNode.put("async", true);

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_SIGNALS));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_ACCEPTED));

            // Check if job is queued as a result of the signal without tenant ID set
            assertEquals(1, managementService.createJobQuery().jobWithoutTenantId().count());

            // Signal with tenant
            requestNode.put("tenantId", "my tenant");
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_ACCEPTED));

            // Check if job is queued as a result of the signal, in the right tenant
            assertEquals(1, managementService.createJobQuery().jobTenantId("my tenant").count());

            // Signal with variables and async, should fail as it's not supported
            ArrayNode vars = requestNode.putArray("variables");
            ObjectNode var = vars.addObject();
            var.put("name", "testVar");
            var.put("value", "test");

            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        } finally {
            // Clean up tenant-specific deployment
            if (tenantDeployment != null) {
                repositoryService.deleteDeployment(tenantDeployment.getId(), true);
            }

            // Clear jobs
            List<Job> jobs = managementService.createJobQuery().list();
            for (Job job : jobs) {
                managementService.deleteJob(job.getId());
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/SignalsResourceTest.process-signal-start.bpmn20.xml" })
    public void testQueryEventSubscriptions() throws Exception {
        Calendar hourAgo = Calendar.getInstance();
        hourAgo.add(Calendar.HOUR, -1);

        Calendar inAnHour = Calendar.getInstance();
        inAnHour.add(Calendar.HOUR, 1);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION);
        assertResultsPresentInDataResponse(url, eventSubscription.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION) + "?eventType=signal";
        assertResultsPresentInDataResponse(url, eventSubscription.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION) + "?eventName=" + encode("The Signal");
        assertResultsPresentInDataResponse(url, eventSubscription.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION) + "?activityId=theStart";
        assertResultsPresentInDataResponse(url, eventSubscription.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION) + "?activityId=nonexisting";
        assertEmptyResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION) + "?processDefinitionId=" + processDefinition.getId();
        assertResultsPresentInDataResponse(url, eventSubscription.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION) + "?processDefinitionId=nonexisting";
        assertEmptyResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION) + "?createdBefore=" + getISODateString(inAnHour.getTime());
        assertResultsPresentInDataResponse(url, eventSubscription.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION_COLLECTION) + "?createdAfter=" + getISODateString(hourAgo.getTime());
        assertResultsPresentInDataResponse(url, eventSubscription.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/SignalsResourceTest.process-signal-start.bpmn20.xml" })
    public void testGetEventSubscription() throws Exception {
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_SUBSCRIPTION, eventSubscription.getId());
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);

        assertEquals(eventSubscription.getId(), responseNode.get("id").textValue());
        assertEquals(eventSubscription.getEventType(), responseNode.get("eventType").textValue());
        assertEquals(eventSubscription.getEventName(), responseNode.get("eventName").textValue());
        assertEquals(eventSubscription.getActivityId(), responseNode.get("activityId").textValue());
        assertEquals(eventSubscription.getProcessDefinitionId(), responseNode.get("processDefinitionId").textValue());
        assertEquals(eventSubscription.getCreated(), getDateFromISOString(responseNode.get("created").textValue()));
        assertEquals("", responseNode.get("tenantId").asText());
    }
}
