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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.impl.DefaultInboundEvent;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Event;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CmmnEventRegistryConsumerTest extends AbstractCmmnEventRegistryConsumerTest {

    @Test
    @CmmnDeployment
    public void testGenericEventListenerNoCorrelation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);

        for (int i = 2; i < 10; i++) {
            inboundEventChannelAdapter.triggerTestEvent("test");
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(i);
        }
    }

    @Test
    @CmmnDeployment
    public void testGenericEventListenerWithCorrelation() {
        CaseInstance kermitCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("singleCorrelationCase")
                .variable("customerIdVar", "kermit")
                .start();
        CaseInstance gonzoCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("singleCorrelationCase")
                .variable("customerIdVar", "gonzo")
                .start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitCase.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);

        inboundEventChannelAdapter.triggerTestEvent("fozzie");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testGenericEventListenerNoCorrelation.cmmn",
            "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testGenericEventListenerWithCorrelation.cmmn",
            "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testGenericEventListenerWithOrderCorrelation.cmmn",
            "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testGenericEventListenerWithTwoCorrelations.cmmn"
    })
    public void testGenericEventListenerWithMultipleCorrelations() {
        CaseInstance noCorrelationCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
        CaseInstance kermitOrder1Case = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("twoCorrelationsCase")
                .variable("customerIdVar", "kermit")
                .variable("orderIdVar", "order1")
                .start();
        CaseInstance kermitOrder2Case = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("twoCorrelationsCase")
                .variable("customerIdVar", "kermit")
                .variable("orderIdVar", "order2")
                .start();
        CaseInstance gonzoOrder1Case = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("twoCorrelationsCase")
                .variable("customerIdVar", "gonzo")
                .variable("orderIdVar", "order1")
                .start();
        CaseInstance gonzoCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("singleCorrelationCase")
                .variable("customerIdVar", "gonzo")
                .start();
        CaseInstance order1Case = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("orderCorrelationCase")
                .variable("orderIdVar", "order1")
                .start();
        CaseInstance order2Case = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("orderCorrelationCase")
                .variable("orderIdVar", "order2")
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("fozzie");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(4);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit", "order1");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(5);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerTestEvent("gonzo", "order1");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(6);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(1);

        inboundEventChannelAdapter.triggerOrderTestEvent("order2");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(noCorrelationCase.getId()).list()).hasSize(7);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(kermitOrder2Case.getId()).list()).hasSize(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoOrder1Case.getId()).list()).hasSize(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(gonzoCase.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order1Case.getId()).list()).hasSize(3);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(order2Case.getId()).list()).hasSize(2);
    }

    @Test
    @CmmnDeployment
    public void testCaseStartNoCorrelationParameter() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("testCaseStartEvent").singleResult();
        assertThat(caseDefinition).isNotNull();

        EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery()
                .scopeDefinitionId(caseDefinition.getId())
                .scopeType(ScopeTypes.CMMN)
                .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

        for (int i = 1; i <= 5; i++) {
            inboundEventChannelAdapter.triggerTestEvent();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(i);
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseStartWithSimpleCorrelationParameter() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("testCaseStartEvent").singleResult();
        assertThat(caseDefinition).isNotNull();

        EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery()
                .scopeDefinitionId(caseDefinition.getId())
                .scopeType(ScopeTypes.CMMN)
                .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

        inboundEventChannelAdapter.triggerTestEvent("anotherCustomer");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty(); // shouldn't trigger, correlation doesn't match

        for (int i = 1; i <= 3; i++) {
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(i);
        }
    }

    @Test
    @CmmnDeployment
    public void testGenericEventListenerWithPayload() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCaseEventWithPayload")
                .start();

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();
        inboundEventChannelAdapter.triggerTestEvent("payloadCustomer");
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("customerIdVar", "payloadCustomer"),
                        entry("payload1", "Hello World")
                );
    }

    @Test
    public void testRedeployDefinitionWithRuntimeEventSubscriptions() {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testRedeploy.cmmn")
            .deploy();
        addDeploymentForAutoCleanup(deployment);
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        // After deploying, there should be one eventsubscription: to start the case instance
        List<EventSubscription> eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
            .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId, EventSubscription::getScopeId)
            .containsOnly(tuple("myEvent", caseDefinition.getId(), null));

        // After the case instance is started, there should be one additional eventsubscription
        inboundEventChannelAdapter.triggerTestEvent();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());

        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
            .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId,  EventSubscription::getScopeType, EventSubscription::getScopeId)
            .containsOnly(
                tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, null),
                tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, caseInstance.getId())
            );

        // Redeploying the same definition:
        // Event subscription to start should reflect new definition id
        // Existing subscription for event listener should remain
        org.flowable.cmmn.api.repository.CmmnDeployment redeployment = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testRedeploy.cmmn")
            .deploy();
        addDeploymentForAutoCleanup(redeployment);
        CaseDefinition caseDefinitionAfterRedeploy = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(redeployment.getId()).singleResult();

        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
            .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId,  EventSubscription::getScopeType, EventSubscription::getScopeId)
            .containsOnly(
                tuple("myEvent", caseDefinitionAfterRedeploy.getId(), ScopeTypes.CMMN, null), // Note the different definition id
                tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, caseInstance.getId()) // Note the old definition id
            );

        // Triggering the instance event subscription should continue the case instance like before
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
            .extracting(Task::getName)
            .containsOnly("My task 1");

        inboundEventChannelAdapter.triggerTestEvent();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
            .extracting(Task::getName, Task::getScopeId)
            .containsOnly(
                tuple("My task 1", caseInstance.getId()),
                tuple("My task 2", caseInstance.getId()));
    }

    @Test
    @CmmnDeployment
    public void testCaseStartWithPayload() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("testCaseStartEventWithPayload").singleResult();
        assertThat(caseDefinition).isNotNull();

        inboundEventChannelAdapter.triggerTestEvent("payloadStartCustomer");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(1);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("customerIdVar", "payloadStartCustomer"),
                        entry("anotherVarName", "Hello World")
                );
    }
    
    @Test
    @CmmnDeployment
    public void testCaseStartOnlyOneInstance() {
        for (int i = 1; i <= 9; i++) {
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(1);
            assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(2);
        }
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().singleResult().getReferenceId()).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().singleResult().getReferenceType()).isEqualTo(ReferenceTypes.EVENT_CASE);

        for (int i = 1; i <= 4; i++) {
            inboundEventChannelAdapter.triggerTestEvent("anotherCustomer");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(2);
            assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(4);
        }
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testCaseStartOnlyOneInstance.cmmn")
    public void testCaseStartOnlyOneInstanceWithAsyncStartInConfiguration() {
        boolean originalEventRegistryStartCaseInstanceAsync = cmmnEngineConfiguration.isEventRegistryStartCaseInstanceAsync();

        try {
            cmmnEngineConfiguration.setEventRegistryStartCaseInstanceAsync(true);
            for (int i = 1; i <= 9; i++) {
                inboundEventChannelAdapter.triggerTestEvent("testCustomer");
                assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(1);
                assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(0);
            }

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().singleResult().getReferenceId()).isNotNull();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().singleResult().getReferenceType()).isEqualTo(ReferenceTypes.EVENT_CASE);

            waitForJobExecutorToProcessAllJobs();
            assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(2);

            for (int i = 1; i <= 4; i++) {
                inboundEventChannelAdapter.triggerTestEvent("anotherCustomer");
                assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(2);
                assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(2);
            }

            waitForJobExecutorToProcessAllJobs();
            assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(4);
        } finally {
            cmmnEngineConfiguration.setEventRegistryStartCaseInstanceAsync(originalEventRegistryStartCaseInstanceAsync);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testCaseStartOnlyOneInstanceAsync.cmmn")
    public void testCaseStartOnlyOneInstanceWithAsyncStartInCaseModel() {
        for (int i = 1; i <= 9; i++) {
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(1);
            assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(0);
        }

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().singleResult().getReferenceId()).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().singleResult().getReferenceType()).isEqualTo(ReferenceTypes.EVENT_CASE);

        waitForJobExecutorToProcessAllJobs();
        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(2);

        for (int i = 1; i <= 4; i++) {
            inboundEventChannelAdapter.triggerTestEvent("anotherCustomer");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).hasSize(2);
            assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(2);
        }

        waitForJobExecutorToProcessAllJobs();
        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(4);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testCaseStartOnlyOneInstance.cmmn")
    public void testCaseStartOneInstanceWithMultipleCaseDefinitionVersions() {
        inboundEventChannelAdapter.triggerTestEvent("testCustomer");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("testCaseStartEvent").count()).isEqualTo(1);
        
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;
        try {
            deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testCaseStartOnlyOneInstance.cmmn")
                .deploy();
            
            assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("testCaseStartEvent").latestVersion().singleResult().getVersion()).isEqualTo(2);
            
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("testCaseStartEvent").count()).isEqualTo(1);
    
            inboundEventChannelAdapter.triggerTestEvent("anotherTestCustomer");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("testCaseStartEvent").count()).isEqualTo(2);
            inboundEventChannelAdapter.triggerTestEvent("anotherTestCustomer");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("testCaseStartEvent").count()).isEqualTo(2);
            
        } finally {
            cmmnRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testEventRegistrySubscriptionsRecreatedOnDeploymentDelete() {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testRedeploy.cmmn")
                .deploy();
        addDeploymentForAutoCleanup(deployment);
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        // After deploying, there should be one eventsubscription: to start the case instance
        List<EventSubscription> eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId, EventSubscription::getScopeId)
                .containsOnly(tuple("myEvent", caseDefinition.getId(), null));

        // After the case instance is started, there should be one additional eventsubscription
        inboundEventChannelAdapter.triggerTestEvent();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());

        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId,  EventSubscription::getScopeType, EventSubscription::getScopeId)
                .containsOnly(
                        tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, null),
                        tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, caseInstance.getId())
                );

        // Redeploying the same definition:
        // Event subscription to start should reflect new definition id
        // Existing subscription for event listener should remain
        org.flowable.cmmn.api.repository.CmmnDeployment redeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testRedeploy.cmmn")
                .deploy();
        addDeploymentForAutoCleanup(redeployment);
        CaseDefinition caseDefinitionAfterRedeploy = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(redeployment.getId()).singleResult();

        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId,  EventSubscription::getScopeType, EventSubscription::getScopeId)
                .containsOnly(
                        tuple("myEvent", caseDefinitionAfterRedeploy.getId(), ScopeTypes.CMMN, null), // Note the different definition id
                        tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, caseInstance.getId()) // Note the old definition id
                );

        // Triggering the instance event subscription should continue the case instance like before
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(Task::getName)
                .containsOnly("My task 1");

        inboundEventChannelAdapter.triggerTestEvent();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(Task::getName, Task::getScopeId)
                .containsOnly(
                        tuple("My task 1", caseInstance.getId()),
                        tuple("My task 2", caseInstance.getId()));

        autoCleanupDeploymentIds.remove(redeployment.getId());

        // Removing the second definition should recreate the one from the first one
        // There won't be a case instance since we will do cascaded delete of the deployment
        cmmnRepositoryService.deleteDeployment(redeployment.getId(), true);

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId, EventSubscription::getScopeType, EventSubscription::getScopeId)
                .containsOnly(
                        tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, null)
                );
    }

    @Test
    public void testEventRegistrySubscriptionsShouldNotBeRecreatedOnNonLatestDeploymentDelete() {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testRedeploy.cmmn")
                .deploy();
        addDeploymentForAutoCleanup(deployment);
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        // After deploying, there should be one eventsubscription: to start the case instance
        List<EventSubscription> eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId, EventSubscription::getScopeId)
                .containsOnly(tuple("myEvent", caseDefinition.getId(), null));

        // After the case instance is started, there should be one additional eventsubscription
        inboundEventChannelAdapter.triggerTestEvent();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());

        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId,  EventSubscription::getScopeType, EventSubscription::getScopeId)
                .containsOnly(
                        tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, null),
                        tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, caseInstance.getId())
                );

        // Redeploying the same definition:
        // Event subscription to start should reflect new definition id
        // Existing subscription for event listener should remain
        org.flowable.cmmn.api.repository.CmmnDeployment redeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testRedeploy.cmmn")
                .deploy();
        addDeploymentForAutoCleanup(redeployment);
        CaseDefinition caseDefinitionAfterRedeploy = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(redeployment.getId()).singleResult();

        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId,  EventSubscription::getScopeType, EventSubscription::getScopeId)
                .containsOnly(
                        tuple("myEvent", caseDefinitionAfterRedeploy.getId(), ScopeTypes.CMMN, null), // Note the different definition id
                        tuple("myEvent", caseDefinition.getId(), ScopeTypes.CMMN, caseInstance.getId()) // Note the old definition id
                );

        // Triggering the instance event subscription should continue the case instance like before
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(Task::getName)
                .containsOnly("My task 1");

        inboundEventChannelAdapter.triggerTestEvent();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(Task::getName, Task::getScopeId)
                .containsOnly(
                        tuple("My task 1", caseInstance.getId()),
                        tuple("My task 2", caseInstance.getId()));

        autoCleanupDeploymentIds.remove(deployment.getId());

        // Removing the second definition should recreate the one from the first one
        // There won't be a case instance since we will do cascaded delete of the deployment
        cmmnRepositoryService.deleteDeployment(deployment.getId(), true);

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().singleResult();
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionAfterRedeploy.getId());

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId, EventSubscription::getScopeType, EventSubscription::getScopeId)
                .containsOnly(
                        tuple("myEvent", caseDefinitionAfterRedeploy.getId(), ScopeTypes.CMMN, null),
                        tuple("myEvent", caseDefinitionAfterRedeploy.getId(), ScopeTypes.CMMN, caseInstance.getId())
                );
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/CmmnEventRegistryConsumerTest.testEventListenerInRepeatableStage.cmmn"
    })
    public void testEventListenerInRepeatableStage() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<Object, Object> beans = getEventRegistryEngineConfiguration().getExpressionManager().getBeans();


        getEventRepositoryService().createEventModelBuilder()
                .key("stopEvent")
                .resourceName("stopEvent.event")
                .correlationParameter("corr", EventPayloadTypes.STRING)
                .deploy();

        getEventRepositoryService().createEventModelBuilder()
                .key("startEvent")
                .resourceName("startEvent.event")
                .payload("eventField1", EventPayloadTypes.STRING)
                .deploy();

        CaseInstance myCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testEventListenerInRepeatableStage")
                .start();

        ObjectNode eventJson = objectMapper.createObjectNode();
        eventJson.put("type", "startEvent");
        eventJson.put("eventField1", "abc");

        inboundEventChannelAdapter.triggerTestEventWithJson(eventJson);

        eventJson = objectMapper.createObjectNode();
        eventJson.put("type", "startEvent");
        eventJson.put("eventField1", "def");

        inboundEventChannelAdapter.triggerTestEventWithJson(eventJson);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemeventListener2").list();
        List<EventSubscription> eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().eventType("stopEvent").list();

        assertThat(planItemInstances.size()).isEqualTo(2);
        assertThat(eventSubscriptions.size()).isEqualTo(2);

        eventJson = objectMapper.createObjectNode();
        eventJson.put("type", "stopEvent");
        eventJson.put("corr", "abc");

        inboundEventChannelAdapter.triggerTestEventWithJson(eventJson);

        planItemInstances =  cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemeventListener2").list();
        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().eventType("stopEvent").list();

        assertThat(planItemInstances.size()).isEqualTo(1);
        assertThat(eventSubscriptions.size()).isEqualTo(1);

        cmmnRuntimeService.terminateCaseInstance(myCase.getId());
    }

}
