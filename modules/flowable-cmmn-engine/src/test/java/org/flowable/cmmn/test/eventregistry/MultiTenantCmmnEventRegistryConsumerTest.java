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
package org.flowable.cmmn.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventModelBuilder;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class MultiTenantCmmnEventRegistryConsumerTest extends FlowableEventRegistryCmmnTestCase {

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

    private Set<String> cleanupDeploymentIds = new HashSet<>();

    @Before
    public void setup() {
        getEventRegistryEngineConfiguration().setFallbackToDefaultTenant(true);
        Map<Object, Object> beans = getEventRegistryEngineConfiguration().getExpressionManager().getBeans();
        beans.put("inboundChannelAdapter", new TestInboundChannelAdapter());
        beans.put("sharedInboundChannelAdapter", new TestInboundChannelAdapter());
        beans.put("tenantAChannelAdapter", new TestInboundChannelAdapter());
        beans.put("tenantBChannelAdapter", new TestInboundChannelAdapter());

        // Shared channel and event in default tenant
        getEventRepositoryService().createInboundChannelModelBuilder()
                .key("sharedDefaultChannel")
                .resourceName("sharedDefault.channel")
                .channelAdapter("${inboundChannelAdapter}")
                .jsonDeserializer()
                .fixedEventKey("defaultTenantSameKey")
                .detectEventTenantUsingJsonPointerExpression("/tenantId")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();

        deployEventDefinition("sharedDefaultChannel", "defaultTenantSameKey", null);

        // Shared channel with 'sameKey' event
        getEventRepositoryService().createInboundChannelModelBuilder()
                .key("sharedChannel")
                .resourceName("shared.channel")
                .channelAdapter("${sharedInboundChannelAdapter}")
                .jsonDeserializer()
                .fixedEventKey("sameKey")
                .detectEventTenantUsingJsonPointerExpression("/tenantId")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();

        deployEventDefinition("sharedChannel", "sameKey", TENANT_A, "tenantAData");
        deployEventDefinition("sharedChannel", "sameKey", TENANT_B, "tenantBData", "someMoreTenantBData");

        // Tenant A specific events
        getEventRepositoryService().createInboundChannelModelBuilder()
                .key("tenantAChannel")
                .resourceName("tenantA.channel")
                .deploymentTenantId(TENANT_A)
                .channelAdapter("${tenantAChannelAdapter}")
                .jsonDeserializer()
                .fixedEventKey("tenantAKey")
                .fixedTenantId("tenantA")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();

        deployEventDefinition("tenantAChannel", "tenantAKey", TENANT_A);

        // Tenant B specific events
        getEventRepositoryService().createInboundChannelModelBuilder()
                .key("tenantBChannel")
                .resourceName("tenantB.channel")
                .deploymentTenantId(TENANT_B)
                .channelAdapter("${tenantBChannelAdapter}")
                .jsonDeserializer()
                .fixedEventKey("tenantBKey")
                .fixedTenantId("tenantB")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();

        deployEventDefinition("tenantBChannel", "tenantBKey", TENANT_B);
    }

    private void deployEventDefinition(String channelKey, String key, String tenantId, String... optionalExtraPayload) {
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

    @After
    public void cleanup() {
        getEventRepositoryService().createDeploymentQuery().list()
                .forEach(eventDeployment -> getEventRepositoryService().deleteDeployment(eventDeployment.getId()));

        for (String cleanupDeploymentId : cleanupDeploymentIds) {
            CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, cleanupDeploymentId);
        }
        cleanupDeploymentIds.clear();

        getEventRegistryEngineConfiguration().setFallbackToDefaultTenant(false);
    }

    @Test
    public void validateEventModelDeployments() {
        EventDefinition eventDefinitionDefaultTenant = getEventRepositoryService().createEventDefinitionQuery()
                .eventDefinitionKey("defaultTenantSameKey").singleResult();
        assertThat(eventDefinitionDefaultTenant.getTenantId()).isEqualTo(CmmnEngineConfiguration.NO_TENANT_ID);

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
        assertThat(tenantBEventDefinition.getId()).isEqualTo(getEventRepositoryService().createEventDefinitionQuery()
                .eventDefinitionKey("tenantBKey").tenantId(TENANT_B).singleResult().getId());
        assertThat(getEventRepositoryService().createEventDefinitionQuery()
                .eventDefinitionKey("tenantAKey").tenantId(TENANT_B).singleResult()).isNull();
    }

    private void deployCaseModel(String modelResource, String tenantId) {
        String resource = getClass().getPackage().toString().replace("package ", "").replace(".", "/");
        resource += "/MultiTenantCmmnEventRegistryConsumerTest." + modelResource;
        CmmnDeploymentBuilder cmmnDeploymentBuilder = cmmnRepositoryService.createDeployment().addClasspathResource(resource);
        if (tenantId != null) {
            cmmnDeploymentBuilder.tenantId(tenantId);
        }

        String deploymentId = cmmnDeploymentBuilder.deploy().getId();
        cleanupDeploymentIds.add(deploymentId);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId).singleResult()).isNotNull();
    }

    @Test
    public void testStartCaseInstanceWithTenantSpecificEvent() {
        deployCaseModel("startCaseInstanceTenantA.cmmn", TENANT_A);
        deployCaseModel("startCaseInstanceTenantB.cmmn", TENANT_B);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().tenantId(TENANT_A).list())
                .extracting(EventSubscription::getEventType, EventSubscription::getTenantId)
                .containsOnly(tuple("tenantAKey", "tenantA"));
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().tenantId(TENANT_B).list())
                .extracting(EventSubscription::getEventType, EventSubscription::getTenantId)
                .containsOnly(tuple("tenantBKey", "tenantB"));

        InboundChannelModel tenantAChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("tenantAChannel", TENANT_A);
        InboundChannelModel tenantBChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("tenantBChannel", TENANT_B);
        // Note that #triggerEventWithoutTenantId doesn't have a tenantId set, but the channel has it hardcoded

        ((TestInboundChannelAdapter) tenantAChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isZero();

        ((TestInboundChannelAdapter) tenantAChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        ((TestInboundChannelAdapter) tenantBChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerB");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isEqualTo(1);
    }

    @Test
    public void testStartUniqueCaseInstanceWithSpecificTenantEvent() {
        deployCaseModel("startUniqueCaseInstanceTenantA.cmmn", TENANT_A);
        deployCaseModel("startUniqueCaseInstanceTenantB.cmmn", TENANT_B);

        InboundChannelModel tenantAChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("tenantAChannel", TENANT_A);
        InboundChannelModel tenantBChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("tenantBChannel", TENANT_B);
        ((TestInboundChannelAdapter) tenantAChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isZero();

        ((TestInboundChannelAdapter) tenantAChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        ((TestInboundChannelAdapter) tenantBChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(1); // no new instance for A started
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count())
                .isEqualTo(1); // but a new instance for B (different tenant)
    }

    @Test
    public void testStartCaseInstanceWithSameEventKeyDeployedInDifferentTenants() {
        deployCaseModel("startCaseInstanceSameKeyA.cmmn", TENANT_A);
        deployCaseModel("startCaseInstanceSameKeyB.cmmn", TENANT_B);

        InboundChannelModel sharedInboundChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("sharedChannel");

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_A);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isZero();

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isEqualTo(1);

        // The event definitions have the same key, but different payload handling
        CaseInstance tenantAInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).singleResult();
        assertThat(cmmnRuntimeService.getVariable(tenantAInstance.getId(), "tenantSpecificVar")).isEqualTo("tenantAValue");
        assertThat(cmmnRuntimeService.getVariable(tenantAInstance.getId(), "tenantSpecificVar2")).isNull();

        CaseInstance tenantBInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).singleResult();
        assertThat(cmmnRuntimeService.getVariable(tenantBInstance.getId(), "tenantSpecificVar")).isEqualTo("tenantBValue");
        assertThat(cmmnRuntimeService.getVariable(tenantBInstance.getId(), "tenantSpecificVar2")).isEqualTo("someMoreTenantBValue");

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isEqualTo(2);
    }

    @Test
    public void testStartCaseInstanceWithEventFromDefaultTenant() {
        deployCaseModel("startCaseInstanceDefaultTenant.cmmn", null);

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().singleResult().getTenantId()).isNullOrEmpty();

        InboundChannelModel defaultSharedInboundChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("sharedDefaultChannel");
        // The channel has a tenant detector that will use the correct tenant to start the case instance

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_A);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isZero();

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_A);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isZero();

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_A).count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId(TENANT_B).count()).isEqualTo(1);
    }

    @Test
    public void testCaseDefinitionInDefaultTenantAndEventListenerSubscriptionInSpecificTenant() {
        // Case definition is in default tenant, event definition is in default tenant,
        // yet the event subscription needs to have the specific tenant
        deployCaseModel("eventListenerSameKey.cmmn", null);

        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").fallbackToDefaultTenant().overrideCaseDefinitionTenantId(TENANT_A)
                .tenantId(TENANT_A).start();
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").fallbackToDefaultTenant().overrideCaseDefinitionTenantId(TENANT_B)
                .tenantId(TENANT_B).start();

        InboundChannelModel defaultSharedInboundChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("sharedDefaultChannel");

        // Event subscription should be for specific tenants
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().list()).extracting(EventSubscription::getTenantId).containsOnly(TENANT_A, TENANT_B);

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_A);
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().list()).extracting(EventSubscription::getTenantId).containsOnly(TENANT_B);
        assertThat(cmmnTaskService.createTaskQuery().list()).extracting(Task::getName).containsOnly("Task tenantA");

        ((TestInboundChannelAdapter) defaultSharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().list()).isEmpty();
        assertThat(cmmnTaskService.createTaskQuery().list()).extracting(Task::getName).containsOnly("Task tenantA", "Task tenantB");

    }

    @Test
    public void testEventListenerForSpecificTenantEvent() {
        deployCaseModel("eventListenerTenantA.cmmn", TENANT_A);
        deployCaseModel("eventListenerTenantB.cmmn", TENANT_B);

        // Start a case instance in both tenants
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").tenantId(TENANT_A).start();
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").tenantId(TENANT_B).start();
        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        InboundChannelModel tenantAChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("tenantAChannel", TENANT_A);
        InboundChannelModel tenantBChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("tenantBChannel", TENANT_B);

        // Now trigger the event, which should only trigger tasks in the specific tenant
        ((TestInboundChannelAdapter) tenantAChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("TenantATask");

        ((TestInboundChannelAdapter) tenantBChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("TenantATask", "TenantBTask");

        ((TestInboundChannelAdapter) tenantBChannelModel.getInboundEventChannelAdapter()).triggerEventWithoutTenantId("customerA");
        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("TenantATask", "TenantBTask", "TenantBTask");
    }

    @Test
    public void testEventListenerSameEventKeyForDifferentTenants() {
        deployCaseModel("eventListenerSameKeyTenantA.cmmn", TENANT_A);
        deployCaseModel("eventListenerSameKeyTenantB.cmmn", TENANT_B);

        // Start a case instance in both tenants
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").tenantId(TENANT_A).start();
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").tenantId(TENANT_B).start();
        assertThat(cmmnTaskService.createTaskQuery().list()).isEmpty();

        InboundChannelModel sharedInboundChannelModel = (InboundChannelModel) getEventRepositoryService().getChannelModelByKey("sharedChannel");

        // Now trigger the event, which should only trigger tasks in the specific tenant
        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_A);
        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("TenantATask");

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("TenantATask", "TenantBTask");

        ((TestInboundChannelAdapter) sharedInboundChannelModel.getInboundEventChannelAdapter()).triggerEventForTenantId("customerA", TENANT_B);
        assertThat(cmmnTaskService.createTaskQuery().list())
                .extracting(Task::getName)
                .containsOnly("TenantATask", "TenantBTask", "TenantBTask");
    }

    private static class TestInboundChannelAdapter implements InboundEventChannelAdapter {

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

}
