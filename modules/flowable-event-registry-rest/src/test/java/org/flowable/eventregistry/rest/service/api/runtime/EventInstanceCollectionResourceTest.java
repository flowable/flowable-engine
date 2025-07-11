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

package org.flowable.eventregistry.rest.service.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;
import org.flowable.eventregistry.test.ChannelDeploymentAnnotation;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class EventInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    @Test
    @Deployment(resources = { "org/flowable/eventregistry/rest/service/api/repository/boundaryEvent.bpmn20.xml" })
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event" })
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleChannel.channel" })
    public void testSendEvent() throws Exception {
        ProcessInstance processInstance = processRuntimeService.startProcessInstanceByKey("process", Collections.singletonMap("customerIdVar", "123"));
        Task task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        
        // first send event that doesn't match process boundary event
        requestNode.put("eventDefinitionKey", "myEvent");
        requestNode.put("channelDefinitionKey", "myChannel");
        ObjectNode payloadNode = requestNode.putObject("eventPayload");
        payloadNode.put("customerId", "notExisting");
        payloadNode.put("productNumber", "p456");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);
        
        task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

        // now send event with matching correlation value
        requestNode.put("eventDefinitionKey", "myEvent");
        payloadNode = requestNode.putObject("eventPayload");
        payloadNode.put("customerId", "123");
        payloadNode.put("productNumber", "p456");

        httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);
        
        task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
    }

    @Test
    @Deployment(resources = { "org/flowable/eventregistry/rest/service/api/repository/boundaryEvent.bpmn20.xml" })
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent2.event" })
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleChannel.channel" })
    public void testSendEventWithMultipleEventDefinitions() throws Exception {
        EventDeployment deployment2 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event")
                .deploy();

        ProcessInstance processInstance = processRuntimeService.startProcessInstanceByKey("process", Collections.singletonMap("customerIdVar", "123"));
        Task task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

        ObjectNode requestNode = objectMapper.createObjectNode();

        // first send event that doesn't match process boundary event
        requestNode.put("eventDefinitionKey", "myEvent");
        requestNode.put("channelDefinitionKey", "myChannel");
        ObjectNode payloadNode = requestNode.putObject("eventPayload");
        payloadNode.put("customerId", "notExisting");
        payloadNode.put("productNumber", "p456");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

        // now send event with matching correlation value
        requestNode.put("eventDefinitionKey", "myEvent");
        payloadNode = requestNode.putObject("eventPayload");
        payloadNode.put("customerId", "123");
        payloadNode.put("productNumber", "p456");

        httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");

        repositoryService.deleteDeployment(deployment2.getId());
    }
    
    @Test
    @Deployment(resources = { "org/flowable/eventregistry/rest/service/api/repository/boundaryEvent.bpmn20.xml" }, tenantId = "tenant1")
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event" }, tenantId = "tenant1")
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleTenantChannel.channel" }, tenantId = "tenant1")
    public void testSendEventWithCustomAndDefaultTenant() throws Exception {
        boolean fallbackToDefaultTenant = eventRegistryEngineConfiguration.isFallbackToDefaultTenant();
        eventRegistryEngineConfiguration.setFallbackToDefaultTenant(true);
        
        EventDeployment defaultEventDeployment = null;
        org.flowable.engine.repository.Deployment defaultDeployment = null;
        try {
            defaultEventDeployment = repositoryService.createDeployment().tenantId("")
                .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event")
                .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleTenantChannel.channel")
                .deploy();
            
            defaultDeployment = processRepositoryService.createDeployment().tenantId("")
                .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/boundaryEvent.bpmn20.xml")
                .deploy();
            
            ProcessInstance processInstance = processRuntimeService.startProcessInstanceByKeyAndTenantId("process", Collections.singletonMap("customerIdVar", "123"), "tenant1");
            Task task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
            
            ProcessInstance defaultProcessInstance = processRuntimeService.startProcessInstanceByKeyAndTenantId("process", Collections.singletonMap("customerIdVar", "123"), "");
            Task defaultTask = processTaskService.createTaskQuery().processInstanceId(defaultProcessInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
            
            ObjectNode requestNode = objectMapper.createObjectNode();
            
            // first send event that doesn't match process boundary event
            requestNode.put("eventDefinitionKey", "myEvent");
            requestNode.put("channelDefinitionKey", "myChannel");
            requestNode.put("tenantId", "tenant1");
            ObjectNode payloadNode = requestNode.putObject("eventPayload");
            payloadNode.put("customerId", "notExisting");
            payloadNode.put("productNumber", "p456");
    
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
            closeResponse(response);
            
            task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
    
            // now send event with matching correlation value
            requestNode.put("eventDefinitionKey", "myEvent");
            payloadNode = requestNode.putObject("eventPayload");
            payloadNode.put("customerId", "123");
            payloadNode.put("productNumber", "p456");
    
            httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
            closeResponse(response);
            
            task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
            
            // default process instance task should not have been changed
            defaultTask = processTaskService.createTaskQuery().processInstanceId(defaultProcessInstance.getId()).singleResult();
            assertThat(defaultTask.getTaskDefinitionKey()).isEqualTo("task");
            
        } finally {
            if (defaultEventDeployment != null) {
                repositoryService.deleteDeployment(defaultEventDeployment.getId());
            }
            
            if (defaultDeployment != null) {
                processRepositoryService.deleteDeployment(defaultDeployment.getId(), true);
            }
            
            eventRegistryEngineConfiguration.setFallbackToDefaultTenant(fallbackToDefaultTenant);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/eventregistry/rest/service/api/repository/startEvent.bpmn20.xml" }, tenantId = "tenant1")
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event" }, tenantId = "tenant1")
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleTenantChannel.channel" }, tenantId = "tenant1")
    public void testStartEventWithCustomAndDefaultTenant() throws Exception {
        boolean fallbackToDefaultTenant = eventRegistryEngineConfiguration.isFallbackToDefaultTenant();
        eventRegistryEngineConfiguration.setFallbackToDefaultTenant(true);
        
        EventDeployment defaultEventDeployment = null;
        org.flowable.engine.repository.Deployment defaultDeployment = null;
        try {
            defaultEventDeployment = repositoryService.createDeployment().tenantId("")
                .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event")
                .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleTenantChannel.channel")
                .deploy();
            
            defaultDeployment = processRepositoryService.createDeployment().tenantId("")
                .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/startEvent.bpmn20.xml")
                .deploy();
            
            long tenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("tenant1").count();
            assertThat(tenantInstanceCount).isZero();
            
            long defaultTenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("").count();
            assertThat(defaultTenantInstanceCount).isZero();
            
            ObjectNode requestNode = objectMapper.createObjectNode();
            
            // first send event that doesn't match process boundary event
            requestNode.put("eventDefinitionKey", "myEvent");
            requestNode.put("channelDefinitionKey", "myChannel");
            requestNode.put("tenantId", "tenant1");
            ObjectNode payloadNode = requestNode.putObject("eventPayload");
            payloadNode.put("customerId", "123");
            payloadNode.put("productNumber", "p456");
    
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
            closeResponse(response);
            
            tenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("tenant1").count();
            assertThat(tenantInstanceCount).isEqualTo(1);
            
            defaultTenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("").count();
            assertThat(defaultTenantInstanceCount).isZero();
            
        } finally {
            if (defaultEventDeployment != null) {
                repositoryService.deleteDeployment(defaultEventDeployment.getId());
            }
            
            if (defaultDeployment != null) {
                processRepositoryService.deleteDeployment(defaultDeployment.getId(), true);
            }
            
            eventRegistryEngineConfiguration.setFallbackToDefaultTenant(fallbackToDefaultTenant);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/eventregistry/rest/service/api/repository/startEvent.bpmn20.xml" }, tenantId = "")
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event" }, tenantId = "")
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleTenantChannel.channel" }, tenantId = "")
    public void testStartEventWithDefaultTenant() throws Exception {
        boolean fallbackToDefaultTenant = eventRegistryEngineConfiguration.isFallbackToDefaultTenant();
        eventRegistryEngineConfiguration.setFallbackToDefaultTenant(true);
        
        try {
            long tenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("tenant1").count();
            assertThat(tenantInstanceCount).isZero();
            
            long defaultTenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("").count();
            assertThat(defaultTenantInstanceCount).isZero();
            
            ObjectNode requestNode = objectMapper.createObjectNode();
            
            // first send event that doesn't match process boundary event
            requestNode.put("eventDefinitionKey", "myEvent");
            requestNode.put("channelDefinitionKey", "myChannel");
            requestNode.put("tenantId", "tenant1");
            ObjectNode payloadNode = requestNode.putObject("eventPayload");
            payloadNode.put("customerId", "123");
            payloadNode.put("productNumber", "p456");
    
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
            closeResponse(response);
            
            tenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("tenant1").count();
            assertThat(tenantInstanceCount).isEqualTo(1);
            
            defaultTenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("").count();
            assertThat(defaultTenantInstanceCount).isZero();
            
        } finally {
            eventRegistryEngineConfiguration.setFallbackToDefaultTenant(fallbackToDefaultTenant);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/eventregistry/rest/service/api/repository/startEvent.bpmn20.xml" }, tenantId = "tenant1")
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event" }, tenantId = "tenant1")
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleTenantChannel.channel" }, tenantId = "tenant1")
    public void testStartEventWithCustomTenant() throws Exception {
        boolean fallbackToDefaultTenant = eventRegistryEngineConfiguration.isFallbackToDefaultTenant();
        eventRegistryEngineConfiguration.setFallbackToDefaultTenant(true);
        
        try {
            long tenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("tenant1").count();
            assertThat(tenantInstanceCount).isZero();
            
            long defaultTenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("").count();
            assertThat(defaultTenantInstanceCount).isZero();
            
            ObjectNode requestNode = objectMapper.createObjectNode();
            
            // first send event that doesn't match process boundary event
            requestNode.put("eventDefinitionKey", "myEvent");
            requestNode.put("channelDefinitionKey", "myChannel");
            requestNode.put("tenantId", "tenant1");
            ObjectNode payloadNode = requestNode.putObject("eventPayload");
            payloadNode.put("customerId", "123");
            payloadNode.put("productNumber", "p456");
    
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
            closeResponse(response);
            
            tenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("tenant1").count();
            assertThat(tenantInstanceCount).isEqualTo(1);
            
            defaultTenantInstanceCount = processRuntimeService.createProcessInstanceQuery().processDefinitionKey("process").processInstanceTenantId("").count();
            assertThat(defaultTenantInstanceCount).isZero();
            
        } finally {
            eventRegistryEngineConfiguration.setFallbackToDefaultTenant(fallbackToDefaultTenant);
        }
    }
}
