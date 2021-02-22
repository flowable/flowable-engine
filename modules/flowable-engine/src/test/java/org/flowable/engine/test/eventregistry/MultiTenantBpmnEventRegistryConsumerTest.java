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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventModelBuilder;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class MultiTenantBpmnEventRegistryConsumerTest extends FlowableEventRegistryBpmnTestCase {

    /**
     * Setup: two tenants: tenantA and tenantB.
     *
     * Default tenant: - event definition 'defaultTenantSameKey'
     *
     * TenantA: - event definition 'sameKey'
     *          - event definition 'tenantAKey'
     *
     * TenantB: - event definition 'sameKey'
     *          - event definition 'tenantBKey'
     *
     * The event with 'defaultTenantSameKey' comes in through a channel with a tenantId selector, but it's deployed to the default tenant.
     * The event with 'sameKey' comes in through a channel with a tenantId detector, but each tenant has a deployment for the event definition.
     * The events with tenant specific keys come in through dedicated channels with a static tenantId, each tenant has a specific deployment for the event definition.
     */

    private static final String TENANT_A = "tenantA";

    private static final String TENANT_B = "tenantB";

    private InboundChannelModel defaultSharedInboundChannelModel;
    private InboundChannelModel sharedInboundChannelModel;
    private InboundChannelModel tenantAChannelModel;
    private InboundChannelModel tenantBChannelModel;

    private Set<String> cleanupDeploymentIds = new HashSet<>();

    @BeforeEach
    public void setup() {
        getEventRegistryEngineConfiguration().setFallbackToDefaultTenant(true);

        Map<Object, Object> beans = getEventRegistryEngineConfiguration().getExpressionManager().getBeans();
        beans.put("testInboundChannelAdapter", new TestInboundChannelAdapter());
        beans.put("testInboundChannelAdapter2", new TestInboundChannelAdapter());
        beans.put("testInboundChannelAdapter3", new TestInboundChannelAdapter());
        beans.put("testInboundChannelAdapter4", new TestInboundChannelAdapter());
        
        // Shared channel and event in default tenant
        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("sharedDefaultChannel")
            .resourceName("sharedDefaultChannel.channel")
            .channelAdapter("${testInboundChannelAdapter}")
            .jsonDeserializer()
            .fixedEventKey("defaultTenantSameKey")
            .detectEventTenantUsingJsonPointerExpression("/tenantId")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();
        
        defaultSharedInboundChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("sharedDefaultChannel");

        deployEventDefinition(defaultSharedInboundChannelModel, "defaultTenantSameKey", null);

        // Shared channel with 'sameKey' event
        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("sharedChannel")
            .resourceName("sharedChannel.channel")
            .channelAdapter("${testInboundChannelAdapter2}")
            .jsonDeserializer()
            .fixedEventKey("sameKey")
            .detectEventTenantUsingJsonPointerExpression("/tenantId")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();
        
        sharedInboundChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("sharedChannel", TENANT_A);

        deployEventDefinition(sharedInboundChannelModel, "sameKey", TENANT_A, "tenantAData");
        deployEventDefinition(sharedInboundChannelModel, "sameKey", TENANT_B, "tenantBData", "someMoreTenantBData");

        // Tenant A specific events
        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("tenantAChannel")
            .resourceName("tenantAChannel.channel")
            .deploymentTenantId(TENANT_A)
            .channelAdapter("${testInboundChannelAdapter3}")
            .jsonDeserializer()
            .fixedEventKey("tenantAKey")
            .fixedTenantId("tenantA")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();
        
        tenantAChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("tenantAChannel", TENANT_A);
        
        deployEventDefinition(tenantAChannelModel, "tenantAKey", TENANT_A);

        // Tenant B specific events
        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("tenantBChannel")
            .resourceName("tenantBChannel.channel")
            .deploymentTenantId(TENANT_B)
            .channelAdapter("${testInboundChannelAdapter4}")
            .jsonDeserializer()
            .fixedEventKey("tenantBKey")
            .fixedTenantId("tenantB")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();
        
        tenantBChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("tenantBChannel", TENANT_B);

        deployEventDefinition(tenantBChannelModel, "tenantBKey", TENANT_B);
    }

    private void deployEventDefinition(ChannelModel channelModel, String key, String tenantId, String ... optionalExtraPayload) {
        EventModelBuilder eventModelBuilder = getEventRepositoryService().createEventModelBuilder()
            .key(key)
            .resourceName("myEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .payload("testPayload", EventPayloadTypes.STRING);

        if (tenantId != null) {
            eventModelBuilder.deploymentTenantId(tenantId);
        }

        if (optionalExtraPayload != null) {
            for (String payload : optionalExtraPayload) {
                eventModelBuilder.payload(payload, EventPayloadTypes.STRING);
            }
        }

        eventModelBuilder.deploy();
    }

    @AfterEach
    public void cleanup() {
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
        resource += "/MultiTenantBpmnEventRegistryConsumerTest." + modelResource;
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().addClasspathResource(resource);
        if (tenantId != null) {
            deploymentBuilder.tenantId(tenantId);
        }

        String deploymentId = deploymentBuilder.deploy().getId();
        cleanupDeploymentIds.add(deploymentId);

        assertThat(repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult()).isNotNull();
    }

    @Test
    public void validateEventModelDeployments() {
        EventDefinition eventDefinitionDefaultTenant = getEventRepositoryService().createEventDefinitionQuery()
            .eventDefinitionKey("defaultTenantSameKey").singleResult();
        assertThat(eventDefinitionDefaultTenant.getTenantId()).isEqualTo(ProcessEngineConfiguration.NO_TENANT_ID);

        List<EventDefinition> sameKeyEventDefinitions = getEventRepositoryService().createEventDefinitionQuery()
            .eventDefinitionKey("sameKey").orderByTenantId().asc().list();
        assertThat(sameKeyEventDefinitions)
            .extracting(EventDefinition::getTenantId)
            .containsExactly(TENANT_A, TENANT_B);

        EventDefinition tenantAEventDefinition = getEventRepositoryService().createEventDefinitionQuery()
            .eventDefinitionKey("tenantAKey").singleResult();
        assertThat(tenantAEventDefinition).isNotNull();
        assertThat(tenantAEventDefinition.getId()).isEqualTo(getEventRepositoryService().createEventDefinitionQuery()
            .eventDefinitionKey("tenantAKey").tenantId(TENANT_A).singleResult().getId());
        assertThat(getEventRepositoryService().createEventDefinitionQuery()
            .eventDefinitionKey("tenantBKey").tenantId(TENANT_A).singleResult()).isNull();

        EventDefinition tenantBEventDefinition = getEventRepositoryService().createEventDefinitionQuery()
            .eventDefinitionKey("tenantBKey").singleResult();
        assertThat(tenantBEventDefinition).isNotNull();
        assertThat(tenantBEventDefinition.getId()).isEqualTo( getEventRepositoryService().createEventDefinitionQuery()
            .eventDefinitionKey("tenantBKey").tenantId(TENANT_B).singleResult().getId());
        assertThat(getEventRepositoryService().createEventDefinitionQuery()
            .eventDefinitionKey("tenantAKey").tenantId(TENANT_B).singleResult()).isNull();
    }

    @Test
    public void testStartProcessInstanceWithTenantSpecificEvent() {
        deployProcessModel("startProcessInstanceTenantA.bpmn20.xml", TENANT_A);
        deployProcessModel("startProcessInstanceTenantB.bpmn20.xml", TENANT_B);

        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        assertThat(runtimeService.createEventSubscriptionQuery().tenantId(TENANT_A).list())
            .extracting(EventSubscription::getEventType, EventSubscription::getTenantId)
            .containsOnly(tuple("tenantAKey", "tenantA"));
        assertThat(runtimeService.createEventSubscriptionQuery().tenantId(TENANT_B).list())
            .extracting(EventSubscription::getEventType, EventSubscription::getTenantId)
            .containsOnly(tuple("tenantBKey", "tenantB"));

        // Note that #triggerEventWithoutTenantId doesn't have a tenantId set, but the channel has it hardcoded

        for (int i = 0; i < 5; i++) {
            ((TestInboundChannelAdapter) tenantAChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isEqualTo(i + 1);
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isZero();
        }

        ((TestInboundChannelAdapter) tenantBChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isEqualTo(5);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isEqualTo(1);

        waitForJobExecutorOnCondition(10000L, 100L, () -> taskService.createTaskQuery().count() == 6);
        assertThat(taskService.createTaskQuery().orderByTaskName().asc().list())
            .extracting(Task::getName)
            .containsExactly("task tenant A", "task tenant A", "task tenant A", "task tenant A", "task tenant A", "task tenant B");
    }

    @Test
    public void testStartProcessInstanceWithSameEventKeyDeployedInDifferentTenants() {
        deployProcessModel("startProcessInstanceSameKeyTenantA.bpmn20.xml", TENANT_A);
        deployProcessModel("startProcessInstanceSameKeyTenantB.bpmn20.xml", TENANT_B);

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_A);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isZero();

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isEqualTo(2);

        waitForJobExecutorOnCondition(10000L, 100L, () -> taskService.createTaskQuery().count() == 3);
        assertThat(taskService.createTaskQuery().orderByTaskName().asc().list())
            .extracting(Task::getName)
            .containsExactly("task tenant A", "task tenant B", "task tenant B");
    }

    @Test
    public void testUniqueStartProcessInstanceWithSameEventKeyDeployedInDifferentTenants() {
        deployProcessModel("startUniqueProcessInstanceSameKeyTenantA.bpmn20.xml", TENANT_A);
        deployProcessModel("startUniqueProcessInstanceSameKeyTenantB.bpmn20.xml", TENANT_B);

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_A);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isZero();

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isEqualTo(1); // unique instance for same correlation

        waitForJobExecutorOnCondition(10000L, 100L, () -> taskService.createTaskQuery().count() == 2);
        assertThat(taskService.createTaskQuery().orderByTaskName().asc().list())
            .extracting(Task::getName)
            .containsExactly("task tenant A", "task tenant B");
    }

    @Test
    public void testStartProcessInstanceWithProcessAndEventInDefaultTenant() {
        // Both the process model and the event definition are part of the default tenant
        deployProcessModel("startProcessInstanceDefaultTenant.bpmn20.xml", null);

        String tenantId = runtimeService.createEventSubscriptionQuery().singleResult().getTenantId();
        assertThat(tenantId).isNullOrEmpty();

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isZero();

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_A);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isZero();

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).singleResult();
        assertThat(processInstance.getTenantId()).isEqualTo(TENANT_A);

        for (int i = 0; i < 4; i++) {
            ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerB", TENANT_B);
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_A).count()).isEqualTo(1);
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceTenantId(TENANT_B).count()).isEqualTo(i + 1);
        }
    }

    @Test
    public void testProcessDefinitionInDefaultTenantAndBoundaryEventSubscriptionInSpecificTenant() {
        deployProcessModel("boundaryEventSameKey.bpmn20.xml", null);

        runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").fallbackToDefaultTenant().overrideProcessDefinitionTenantId(TENANT_A).start();
        runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").fallbackToDefaultTenant().overrideProcessDefinitionTenantId(TENANT_B).start();

        // Event subscription should be for specific tenants
        assertThat(runtimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getTenantId)
                .containsOnly(TENANT_A, TENANT_B);

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("no_correlation", TENANT_A);
        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("no_correlation", TENANT_B);
        assertThat(runtimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getTenantId)
                .containsOnly(TENANT_A, TENANT_B);
        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("Task with boundary event", "Task with boundary event");

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("abc", TENANT_A); // abc = correlation
        assertThat(runtimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getTenantId)
                .containsOnly(TENANT_B);
        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("Task with boundary event", "Task tenantA");

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("abc", TENANT_B);
        assertThat(runtimeService.createEventSubscriptionQuery().list()).isEmpty();
        assertThat(taskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("Task tenantA", "Task tenantB");
    }

    @Test
    public void testBoundaryEventWithSpecificTenantEvent() {
        // Note that both events correlate on 'customerId' being 'ABC'
        deployProcessModel("boundaryEventTenantA.bpmn20.xml", TENANT_A);
        deployProcessModel("boundaryEventTenantB.bpmn20.xml", TENANT_B);

        runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").tenantId(TENANT_A).start();
        runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").tenantId(TENANT_B).start();

        // Triggering through the tenant A channel should only correlate
        ((TestInboundChannelAdapter) tenantAChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("ABC");
        assertThat(taskService.createTaskQuery().taskName("Task from tenant A").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant B").count()).isZero();

        runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").tenantId(TENANT_A).start();
        ((TestInboundChannelAdapter) tenantAChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("Doesn't correlate");
        assertThat(taskService.createTaskQuery().taskName("Task from tenant A").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant B").count()).isZero();

        ((TestInboundChannelAdapter) tenantBChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("ABC");
        assertThat(taskService.createTaskQuery().taskName("Task from tenant A").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant B").count()).isEqualTo(1);
    }

    @Test
    public void testBoundaryEventWithSameEventKeyEvent() {
        // Note that both events correlate on 'customerId' being 'ABC'
        deployProcessModel("boundaryEventSameKeyTenantA.bpmn20.xml", TENANT_A);
        deployProcessModel("boundaryEventSameKeyTenantB.bpmn20.xml", TENANT_B);

        runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").tenantId(TENANT_A).start();
        runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").tenantId(TENANT_B).start();

        // Triggering through the tenant A channel should only correlate
        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("ABC", TENANT_A);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant A").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant B").count()).isZero();

        runtimeService.createProcessInstanceBuilder().processDefinitionKey("process").tenantId(TENANT_A).start();
        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("Doesn't correlate", TENANT_A);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant A").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant B").count()).isZero();

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("ABC", TENANT_A);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant A").count()).isEqualTo(2);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant B").count()).isZero();

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("ABC", TENANT_B);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant A").count()).isEqualTo(2);
        assertThat(taskService.createTaskQuery().taskName("Task from tenant B").count()).isEqualTo(1);
    }

    private static class TestInboundChannelAdapter implements InboundEventChannelAdapter {

        protected InboundChannelModel inboundChannelModel;
        protected EventRegistry eventRegistry;

        @Override
        public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
            this.inboundChannelModel = inboundChannelModel;
        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
            this.eventRegistry = eventRegistry;
        }

        public void triggerEventWithoutTenantId(String customerId) {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "tenantAKey");
            if (customerId != null) {
                json.put("customerId", customerId);
            }

            json.put("payload", "Hello World");

            try {
                eventRegistry.eventReceived(inboundChannelModel, objectMapper.writeValueAsString(json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public void triggerEventForTenantId(String customerId, String tenantId) {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "tenantAKey");
            if (customerId != null) {
                json.put("customerId", customerId);
            }

            json.put("tenantAData", "tenantAValue");
            json.put("tenantBData", "tenantBValue");
            json.put("someMoreTenantBData", "someMoreTenantBValue");

            json.put("payload", "Hello World");
            json.put("tenantId", tenantId);

            try {
                eventRegistry.eventReceived(inboundChannelModel, objectMapper.writeValueAsString(json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
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
