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
package org.flowable.engine.test.eventregistry;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.jobexecutor.AsyncSendEventJobHandler;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MultiTenantSendEventTaskTest extends FlowableEventRegistryBpmnTestCase {

    private static final String TENANT_A = "tenantA";

    private static final String TENANT_B = "tenantB";

    protected TestOutboundEventChannelAdapter outboundEventChannelAdapter;
    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    private Set<String> cleanupDeploymentIds = new HashSet<>();

    @BeforeEach
    protected void setUp() throws Exception {
        getEventRegistryEngineConfiguration().setFallbackToDefaultTenant(true);
        outboundEventChannelAdapter = setupTestChannel();
        inboundEventChannelAdapter = setupTestInboundChannel();

        getEventRepositoryService().createEventModelBuilder()
            .key("myEvent")
            .resourceName("myEvent.event")
            .payload("tenantAProperty", EventPayloadTypes.STRING)
            .payload("customerId", EventPayloadTypes.STRING)
            .payload("eventProperty", EventPayloadTypes.STRING)
            .deploymentTenantId(TENANT_A)
            .deploy();

        getEventRepositoryService().createEventModelBuilder()
            .key("myEvent")
            .resourceName("myEvent.event")
            .payload("tenantBProperty", EventPayloadTypes.STRING)
            .payload("customerId", EventPayloadTypes.STRING)
            .payload("eventProperty", EventPayloadTypes.STRING)
            .deploymentTenantId(TENANT_B)
            .deploy();

        getEventRepositoryService().createEventModelBuilder()
            .key("myTriggerEvent")
            .resourceName("myTriggerEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .deploymentTenantId(TENANT_A)
            .deploy();

        getEventRepositoryService().createEventModelBuilder()
            .key("myTriggerEvent")
            .resourceName("myTriggerEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .deploymentTenantId(TENANT_B)
            .deploy();
    }

    protected TestOutboundEventChannelAdapter setupTestChannel() {
        TestOutboundEventChannelAdapter outboundEventChannelAdapter = new TestOutboundEventChannelAdapter();

        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
            .put("outboundEventChannelAdapter", outboundEventChannelAdapter);

        getEventRepositoryService().createOutboundChannelModelBuilder()
            .key("out-channel")
            .resourceName("testOut.channel")
            .channelAdapter("${outboundEventChannelAdapter}")
            .jsonSerializer()
            .deploy();

        return outboundEventChannelAdapter;
    }

    protected TestInboundEventChannelAdapter setupTestInboundChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
            .put("inboundEventChannelAdapter", inboundEventChannelAdapter);

        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("test-channel")
            .resourceName("testIn.channel")
            .channelAdapter("${inboundEventChannelAdapter}")
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("type")
            .detectEventTenantUsingJsonPointerExpression("/tenantId")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        return inboundEventChannelAdapter;
    }

    @AfterEach
    protected void tearDown() throws Exception {
        getEventRepositoryService().createDeploymentQuery().list()
            .forEach(eventDeployment -> getEventRepositoryService().deleteDeployment(eventDeployment.getId()));

        for (String cleanupDeploymentId : cleanupDeploymentIds) {
            repositoryService.deleteDeployment(cleanupDeploymentId, true);
        }
        cleanupDeploymentIds.clear();

        getEventRegistryEngineConfiguration().setFallbackToDefaultTenant(false);
    }

    private void deployProcessModel(String modelResource, String tenantId) {
        String resource = getClass().getPackage().toString().replace("package ", "").replace(".", "/");
        resource += "/MultiTenantSendEventTaskTest." + modelResource;
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().addClasspathResource(resource);
        if (tenantId != null) {
            deploymentBuilder.tenantId(tenantId);
        }

        String deploymentId = deploymentBuilder.deploy().getId();
        cleanupDeploymentIds.add(deploymentId);

        assertThat(repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult()).isNotNull();
    }

    @Test
    public void testSendEvent() throws Exception {
        deployProcessModel("sendEventTenantA.bpmn20.xml", TENANT_A);
        deployProcessModel("sendEventTenantB.bpmn20.xml", TENANT_B);

        ProcessInstance instanceA = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").tenantId(TENANT_A).start();
        validateEventSent(instanceA, "tenantAProperty");

        ProcessInstance instanceB = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").tenantId(TENANT_B).start();
        validateEventSent(instanceB, "tenantBProperty");
    }

    private void validateEventSent(ProcessInstance processInstance, String property) throws JsonProcessingException {
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        Job job = managementService.createJobQuery().jobTenantId(processInstance.getTenantId()).singleResult();
        assertThat(job.getTenantId()).isEqualTo(processInstance.getTenantId());
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncSendEventJobHandler.TYPE);
        assertThat(job.getElementId()).isEqualTo("sendEventTask");

        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get(property).asText()).isEqualTo("test");
        outboundEventChannelAdapter.receivedEvents.clear();
    }

    @Test
    public void testTriggerableSendEventWithCorrelation() throws Exception {
        // Model deployed to the default tenant, but event model is in the specific tenants
        deployProcessModel("sendEventWithTrigger.bpmn20.xml", null);

        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();

        // One instance in tenant A, one in tenant B
        ProcessInstance instanceA = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("process")
            .fallbackToDefaultTenant()
            .variable("customerId", "customerForTenantA")
            .overrideProcessDefinitionTenantId(TENANT_A)
            .start();
        ProcessInstance instanceB = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("process")
            .fallbackToDefaultTenant()
            .variable("customerId", "customerForTenantB")
            .overrideProcessDefinitionTenantId(TENANT_B)
            .start();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(instanceA.getId()).singleResult();
        assertThat(eventSubscription.getEventType()).isEqualTo("myTriggerEvent");
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(instanceA.getId());
        assertThat(eventSubscription.getTenantId()).isEqualTo(TENANT_A);

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(instanceB.getId()).singleResult();
        assertThat(eventSubscription.getEventType()).isEqualTo("myTriggerEvent");
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(instanceB.getId());
        assertThat(eventSubscription.getTenantId()).isEqualTo(TENANT_B);

        // Sending
        assertThat(outboundEventChannelAdapter.receivedEvents).isEmpty();
        assertThat(managementService.createJobQuery().list())
                .extracting(Job::getTenantId)
                .containsOnly(TENANT_A, TENANT_B);
        managementService.createJobQuery().list().forEach(job -> managementService.executeJob(job.getId()));
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(2);

        JsonNode jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "   eventProperty: 'customerForTenantA'"
                        + " }");
        jsonNode = processEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(1));
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "   eventProperty: 'customerForTenantB'"
                        + " }");

        // Triggering
        InboundChannelModel inboundChannel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("test-channel");
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "myTriggerEvent");
        json.put("customerId", "customerForTenantA");
        json.put("tenantId", TENANT_A);
        getEventRegistry().eventReceived(inboundChannel, objectMapper.writeValueAsString(json));

        assertThat(taskService.createTaskQuery().processInstanceId(instanceA.getId()).singleResult().getName()).isEqualTo("tenantA Task");
        assertThat(taskService.createTaskQuery().processInstanceId(instanceB.getId()).singleResult()).isNull();

        json.put("customerId", "customerForTenantB");
        json.put("tenantId", TENANT_B);
        getEventRegistry().eventReceived(inboundChannel, objectMapper.writeValueAsString(json));

        assertThat(taskService.createTaskQuery().processInstanceId(instanceA.getId()).singleResult().getName()).isEqualTo("tenantA Task");
        assertThat(taskService.createTaskQuery().processInstanceId(instanceB.getId()).singleResult().getName()).isEqualTo("tenantB Task");

        // Sending the event again shouldn't do anything, as there are no eventsubscriptions anymore
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        getEventRegistry().eventReceived(inboundChannel, objectMapper.writeValueAsString(json));
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
    }

    public static class TestOutboundEventChannelAdapter implements OutboundEventChannelAdapter<String> {

        public List<String> receivedEvents = new ArrayList<>();

        @Override
        public void sendEvent(String rawEvent) {
            receivedEvents.add(rawEvent);
        }
    }
    
    public static class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

        public InboundChannelModel inboundChannelModel;
        public EventRegistry eventRegistry;

        @Override
        public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
            this.inboundChannelModel = inboundChannelModel;
        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
            this.eventRegistry = eventRegistry;
        }
        
    }

    @Override
    protected EventRepositoryService getEventRepositoryService() {
        return getEventRegistryEngineConfiguration().getEventRepositoryService();
    }

    @Override
    protected EventRegistry getEventRegistry() {
        return getEventRegistryEngineConfiguration().getEventRegistry();
    }

    @Override
    protected EventRegistryEngineConfiguration getEventRegistryEngineConfiguration() {
        return (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
            .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
    }
}
