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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.groups.Tuple;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.DeploymentId;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Various tests for event-registry based process starts, both static and dynamic and the handling of the event subscriptions.
 *
 * @author Micha Kiener
 */
public class DynamicProcessStartEventRegistryDeploymentTest extends FlowableEventRegistryBpmnTestCase {
    @AfterEach
    void tearDown(@DeploymentId String deploymentId) {
        if (deploymentId != null) {
            EventRepositoryService eventRepositoryService = getEventRepositoryService();
            List<EventDeployment> eventDeployments = eventRepositoryService.createDeploymentQuery()
                    .parentDeploymentId(deploymentId)
                    .list();

            for (EventDeployment eventDeployment : eventDeployments) {
                eventRepositoryService.deleteDeployment(eventDeployment.getId());
            }
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testStaticEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testStaticEventRegistryProcessStart() {
        sendEvent("kermit", "start");

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryStaticStartTestProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testStaticEventRegistryProcessWithCorrelationParameters.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testStaticEventRegistryProcessWithCorrelationParameters() {
        sendEvent("gonzo", "start");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        sendEvent("kermit", "start");
        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryStaticStartTestProcessWithCorrelationParameters");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartWithoutManualSubscription() {
        sendEvent("kermit", "start");

        // there must be no running process instance as we didn't create a manual subscription yet
        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testStaticEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartWithoutProcessDefinition() {
        FlowableIllegalArgumentException exception = Assertions.assertThrowsExactly(FlowableIllegalArgumentException.class, () -> {
            // manually register start subscription, but with different correlation than the actual event being sent
            runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
                .addCorrelationParameterValue("customer", "test")
                .addCorrelationParameterValue("action", "start")
                .subscribe();
        });

        assertThat(exception.getMessage()).isEqualTo("The process definition must be provided using the key for the subscription to be registered.");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testStaticEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartWithIllegalManualSubscriptionForWrongStartEvent() {
        FlowableIllegalArgumentException exception = Assertions.assertThrowsExactly(FlowableIllegalArgumentException.class, () -> {
            // manually register start subscription, but with different correlation than the actual event being sent
            runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
                .processDefinitionKey("eventRegistryStaticStartTestProcess")
                .addCorrelationParameterValue("customer", "test")
                .addCorrelationParameterValue("action", "start")
                .subscribe();
        });

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("eventRegistryStaticStartTestProcess").latestVersion().singleResult();

        assertThat(exception.getMessage()).isEqualTo("The process definition with id '" + processDefinition.getId() + "' does not have an event-registry based start event with a manual subscription behavior.");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartWithNonMatchingManualSubscription() {
        // manually register start subscription, but with different correlation than the actual event being sent
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "test")
            .addCorrelationParameterValue("action", "start")
            .subscribe();

        sendEvent("kermit", "start");

        // there must be no running process instance as we didn't create a manual subscription yet
        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartWithIllegalManualSubscriptionForWrongCorrelation() {
        FlowableIllegalArgumentException exception = Assertions.assertThrowsExactly(FlowableIllegalArgumentException.class, () -> {
            // manually register start subscription, but with different correlation than the actual event being sent
            runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
                .processDefinitionKey("eventRegistryDynamicStartTestProcess")
                .addCorrelationParameterValue("invalidCorrelationParameter", "test")
                .addCorrelationParameterValue("action", "start")
                .subscribe();
        });

        assertThat(exception.getMessage()).isEqualTo("There is no correlation parameter with name 'invalidCorrelationParameter' defined in event model with key 'simpleTest'. You can only subscribe for an event with a combination of valid correlation parameters.");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartWithMatchingManualSubscription() {
        // manually register start subscription, matching the event correlation sent later
        EventSubscription eventSubscription = runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .subscribe();

        assertThat(eventSubscription).isNotNull().extracting(EventSubscription::getScopeDefinitionKey).isEqualTo("eventRegistryDynamicStartTestProcess");

        sendEvent("kermit", "start");

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryDynamicStartTestProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartWithMatchingManualSubscriptionVersion2() {
        // manually register start subscription, matching the event correlation sent later
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValues(Map.of("customer", "kermit", "action", "start"))
            .subscribe();

        sendEvent("kermit", "start");

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryDynamicStartTestProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testStaticEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testStaticEventRegistryProcessStartAfterRedeployment() {
        sendEvent("kermit", "start");

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryStaticStartTestProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

        // redeploy the process definition (which removes and re-adds the static start subscription)
        ProcessDefinition processDefinition = deployProcessDefinition("testStaticEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testStaticEventRegistryProcessStart.bpmn20.xml");

        try {
            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(processDefinition.getId());

            task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(processDefinition.getDeploymentId());
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartAfterRedeployment() {
        // manually register start subscription, matching the event correlation sent later
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .subscribe();

        sendEvent("kermit", "start");

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryDynamicStartTestProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

        // the scope definition key must be present in the subscription
        assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionKey)
            .containsExactlyInAnyOrder("eventRegistryDynamicStartTestProcess");

        // redeploy the process definition (which must not remove the subscriptions, but rather update them to the newest version)
        ProcessDefinition processDefinition = deployProcessDefinition("eventRegistryDynamicStartTestProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml");

        try {
            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", processDefinition.getId()));

            task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(processDefinition.getDeploymentId());
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartAfterRedeploymentWithoutAutoUpdate() {
        // manually register start subscription, matching the event correlation sent later
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        sendEvent("kermit", "start");

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryDynamicStartTestProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

        // the scope definition key must not be present in the event subscription
        assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionKey).containsOnlyNulls();

        // search the first process definition as we don't have auto-update in the subscription
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess").latestVersion().singleResult();

        // redeploy the process definition (which must not remove the subscriptions, but rather update them to the newest version)
        ProcessDefinition latestProcessDefinition = deployProcessDefinition("eventRegistryDynamicStartTestProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml");

        try {
            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", processDefinition.getId()));

            task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(latestProcessDefinition.getDeploymentId());
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testSubscriptionDeletionAfterUndeploy() {
        ProcessDefinition processDefinition = deployProcessDefinition("eventRegistryDynamicStartTestProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml");

        try {
            // manually register start subscriptions
            runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
                .processDefinitionKey("eventRegistryDynamicStartTestProcess")
                .addCorrelationParameterValue("customer", "kermit")
                .addCorrelationParameterValue("action", "start")
                .subscribe();

            Map<String, Object> correlationParameters = new HashMap<>();
            correlationParameters.put("customer", "kermit");
            correlationParameters.put("action", "start");
            String correlationConfig1 = getEventRegistry().generateKey(correlationParameters);

            runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
                .processDefinitionKey("eventRegistryDynamicStartTestProcess")
                .addCorrelationParameterValue("customer", "frog")
                .addCorrelationParameterValue("action", "end")
                .subscribe();

            correlationParameters.put("customer", "frog");
            correlationParameters.put("action", "end");
            String correlationConfig2 = getEventRegistry().generateKey(correlationParameters);

            assertThat(runtimeService.createEventSubscriptionQuery().processDefinitionId(processDefinition.getId()).list())
                .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId, EventSubscription::getConfiguration)
                .containsExactlyInAnyOrder(
                    Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig1),
                    Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig2));
        } finally {
            deleteDeployment(processDefinition.getDeploymentId());
        }

        // now the subscriptions also need to be deleted, after the process definition was undeployed
        assertThat(runtimeService.createEventSubscriptionQuery().processDefinitionId(processDefinition.getId()).list()).isEmpty();
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartAfterSubscriptionMigration() {
        // manually register start subscription, matching the event correlation sent later
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "frog")
            .addCorrelationParameterValue("action", "end")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        // search the first process definition as we don't have auto-update in the subscriptions
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess").latestVersion().singleResult();

        assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId)
            .containsExactlyInAnyOrder(
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1"),
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1"));

        // redeploy the process definition (which must not remove or update the existing subscriptions)
        ProcessDefinition newProcessDefinition = deployProcessDefinition("eventRegistryDynamicStartTestProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml");

        try {
            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", processDefinition.getId()));

            Task task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

            // now migrate to the latest process definition manually
            runtimeService.createProcessInstanceStartEventSubscriptionModificationBuilder()
                .processDefinitionId(processDefinition.getId())
                .migrateToLatestProcessDefinition();

            assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
                .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId)
                .containsExactlyInAnyOrder(
                    Tuple.tuple(newProcessDefinition.getId(), "bpmnStartEvent_1"),
                    Tuple.tuple(newProcessDefinition.getId(), "bpmnStartEvent_1"));

            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", newProcessDefinition.getId()));

            task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newProcessDefinition.getDeploymentId());
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartAfterSingleSubscriptionMigration() {
        // manually register start subscription, matching the event correlation sent later
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "frog")
            .addCorrelationParameterValue("action", "end")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();


        Map<String, Object> correlationParameters = new HashMap<>();
        correlationParameters.put("customer", "kermit");
        correlationParameters.put("action", "start");
        String correlationConfig1 = getEventRegistry().generateKey(correlationParameters);

        correlationParameters.put("customer", "frog");
        correlationParameters.put("action", "end");
        String correlationConfig2 = getEventRegistry().generateKey(correlationParameters);

        // search the first process definition as we don't have auto-update in the subscriptions
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess").latestVersion().singleResult();

        assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId, EventSubscription::getConfiguration)
            .containsExactlyInAnyOrder(
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig1),
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig2));

        // redeploy the process definition (which must not remove or update the existing subscriptions)
        ProcessDefinition newProcessDefinition = deployProcessDefinition("eventRegistryDynamicStartTestProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml");

        try {
            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", processDefinition.getId()));

            Task task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

            // now migrate to the latest process definition manually
            runtimeService.createProcessInstanceStartEventSubscriptionModificationBuilder()
                .processDefinitionId(processDefinition.getId())
                .addCorrelationParameterValues(Map.of("customer", "kermit", "action", "start"))
                .migrateToLatestProcessDefinition();

            assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
                .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId, EventSubscription::getConfiguration)
                .containsExactlyInAnyOrder(
                    Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig2),
                    Tuple.tuple(newProcessDefinition.getId(), "bpmnStartEvent_1", correlationConfig1));

            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", newProcessDefinition.getId()));

            task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newProcessDefinition.getDeploymentId());
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartAfterSingleSubscriptionMigrationToSpecificVersion() {
        // manually register start subscription, matching the event correlation sent later
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "frog")
            .addCorrelationParameterValue("action", "end")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();


        Map<String, Object> correlationParameters = new HashMap<>();
        correlationParameters.put("customer", "kermit");
        correlationParameters.put("action", "start");
        String correlationConfig1 = getEventRegistry().generateKey(correlationParameters);

        correlationParameters.put("customer", "frog");
        correlationParameters.put("action", "end");
        String correlationConfig2 = getEventRegistry().generateKey(correlationParameters);

        // search the first process definition as we don't have auto-update in the subscriptions
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess").latestVersion().singleResult();

        assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId, EventSubscription::getConfiguration)
            .containsExactlyInAnyOrder(
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig1),
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig2));

        // redeploy the process definition (which must not remove or update the existing subscriptions)
        ProcessDefinition newProcessDefinition = deployProcessDefinition("eventRegistryDynamicStartTestProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml");

        try {
            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", processDefinition.getId()));

            Task task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

            // now migrate to the latest process definition manually
            runtimeService.createProcessInstanceStartEventSubscriptionModificationBuilder()
                .processDefinitionId(processDefinition.getId())
                .addCorrelationParameterValues(Map.of("customer", "kermit", "action", "start"))
                .migrateToProcessDefinition(newProcessDefinition.getId());

            assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
                .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId, EventSubscription::getConfiguration)
                .containsExactlyInAnyOrder(
                    Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig2),
                    Tuple.tuple(newProcessDefinition.getId(), "bpmnStartEvent_1", correlationConfig1));

            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", newProcessDefinition.getId()));

            task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newProcessDefinition.getDeploymentId());
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartAfterSingleSubscriptionDeletion() {
        // manually register start subscription, matching the event correlation sent later
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "frog")
            .addCorrelationParameterValue("action", "end")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();


        Map<String, Object> correlationParameters = new HashMap<>();
        correlationParameters.put("customer", "kermit");
        correlationParameters.put("action", "start");
        String correlationConfig1 = getEventRegistry().generateKey(correlationParameters);

        correlationParameters.put("customer", "frog");
        correlationParameters.put("action", "end");
        String correlationConfig2 = getEventRegistry().generateKey(correlationParameters);

        // search the first process definition as we don't have auto-update in the subscriptions
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess").latestVersion().singleResult();

        assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId, EventSubscription::getConfiguration)
            .containsExactlyInAnyOrder(
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig1),
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig2));

        // redeploy the process definition (which must not remove or update the existing subscriptions)
        ProcessDefinition newProcessDefinition = deployProcessDefinition("eventRegistryDynamicStartTestProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml");

        try {
            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", processDefinition.getId()));

            Task task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

            // now migrate to the latest process definition manually
            runtimeService.createProcessInstanceStartEventSubscriptionDeletionBuilder()
                .processDefinitionId(processDefinition.getId())
                .addCorrelationParameterValues(Map.of("customer", "kermit", "action", "start"))
                .deleteSubscriptions();

            assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
                .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId, EventSubscription::getConfiguration)
                .containsExactlyInAnyOrder(Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig2));

            sendEvent("kermit", "start");
            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

            sendEvent("frog", "end");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", processDefinition.getId()));

            task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newProcessDefinition.getDeploymentId());
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.sendTestEventProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testDynamicEventRegistryProcessStartAfterAllSubscriptionDeletion() {
        // manually register start subscription, matching the event correlation sent later
        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        runtimeService.createProcessInstanceStartEventSubscriptionBuilder()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess")
            .addCorrelationParameterValue("customer", "frog")
            .addCorrelationParameterValue("action", "end")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();


        Map<String, Object> correlationParameters = new HashMap<>();
        correlationParameters.put("customer", "kermit");
        correlationParameters.put("action", "start");
        String correlationConfig1 = getEventRegistry().generateKey(correlationParameters);

        correlationParameters.put("customer", "frog");
        correlationParameters.put("action", "end");
        String correlationConfig2 = getEventRegistry().generateKey(correlationParameters);

        // search the first process definition as we don't have auto-update in the subscriptions
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("eventRegistryDynamicStartTestProcess").latestVersion().singleResult();

        assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getProcessDefinitionId, EventSubscription::getActivityId, EventSubscription::getConfiguration)
            .containsExactlyInAnyOrder(
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig1),
                Tuple.tuple(processDefinition.getId(), "bpmnStartEvent_1", correlationConfig2));

        // redeploy the process definition (which must not remove or update the existing subscriptions)
        ProcessDefinition newProcessDefinition = deployProcessDefinition("eventRegistryDynamicStartTestProcess.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/DynamicProcessStartEventRegistryDeploymentTest.testDynamicEventRegistryProcessStart.bpmn20.xml");

        try {
            sendEvent("kermit", "start");

            assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestProcess", processDefinition.getId()));

            Task task = taskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            taskService.complete(task.getId());

            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

            // now migrate to the latest process definition manually
            runtimeService.createProcessInstanceStartEventSubscriptionDeletionBuilder()
                .processDefinitionId(processDefinition.getId())
                .deleteSubscriptions();

            assertThat(runtimeService.createEventSubscriptionQuery().eventType("simpleTest").list()).isEmpty();

            sendEvent("kermit", "start");
            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

            sendEvent("frog", "end");
            assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newProcessDefinition.getDeploymentId());
        }
    }

    protected ProcessInstance sendEvent(String customerId, String action) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("sendTestEventProcess")
            .variable("customerId", customerId)
            .variable("customerName", "Kermit the Frog")
            .variable("eventKey", "simpleTest")
            .variable("action", action)
            .start();

        return processInstance;
    }
}
