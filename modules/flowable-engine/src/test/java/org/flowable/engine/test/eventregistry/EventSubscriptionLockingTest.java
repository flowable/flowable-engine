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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class EventSubscriptionLockingTest extends AbstractBpmnEventRegistryConsumerTest {

    @Test
    @Deployment
    public void testEventSubscriptionCanBeLockedAndUnlocked() {
        ProcessDefinition processDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery()
                .processDefinitionKey("eventSubscriptionLocking").singleResult();

        EventSubscription eventSubscription = processEngine.getRuntimeService().createEventSubscriptionQuery()
                .processDefinitionId(processDefinition.getId())
                .singleResult();
        assertThat(eventSubscription).isNotNull();

        assertThat(eventSubscription.getLockOwner()).isNull();
        assertThat(eventSubscription.getLockTime()).isNull();

        EventSubscription finalEventSubscription = eventSubscription;
        processEngineConfiguration.getManagementService().executeCommand(commandContext -> {
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            eventSubscriptionService.lockEventSubscription(finalEventSubscription.getId());
            return null;
        });

        eventSubscription = processEngine.getRuntimeService().createEventSubscriptionQuery()
                .processDefinitionId(processDefinition.getId())
                .singleResult();
        assertThat(eventSubscription.getLockOwner()).isNotNull();
        assertThat(eventSubscription.getLockTime()).isNotNull();

        processEngineConfiguration.getManagementService().executeCommand(commandContext -> {
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            eventSubscriptionService.unlockEventSubscription(finalEventSubscription.getId());
            return null;
        });

        eventSubscription = processEngine.getRuntimeService().createEventSubscriptionQuery()
                .processDefinitionId(processDefinition.getId())
                .singleResult();
        assertThat(eventSubscription.getLockOwner()).isNull();
        assertThat(eventSubscription.getLockTime()).isNull();
    }

    @Test
    @Deployment
    public void testConcurrentStarts() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("testCustomer"));
        executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("testCustomer"));

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("eventSubscriptionLocking").includeProcessVariables().list())
                .extracting(instance -> instance.getProcessVariables().get("customerId"))
                .containsExactlyInAnyOrder("testCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("eventSubscriptionLocking").count()).isEqualTo(1);

        executorService = Executors.newFixedThreadPool(4);

        executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("otherCustomer"));
        executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("otherCustomer"));

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("eventSubscriptionLocking").includeProcessVariables().list())
                .extracting(instance -> instance.getProcessVariables().get("customerId"))
                .containsExactlyInAnyOrder("testCustomer", "otherCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("eventSubscriptionLocking").count()).isEqualTo(2);

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/EventSubscriptionLockingTest.testConcurrentStarts.bpmn20.xml")
    public void testConcurrentStartsWithoutLocking() throws Exception {
        // When locking is disabled then in a parallel scenario multi-thread scenario
        // multiple case instances can be started for the same event, since both threads will see the count as 0
        boolean originalEventRegistryUniqueProcessInstanceCheckWithLock = processEngineConfiguration.isEventRegistryUniqueProcessInstanceCheckWithLock();
        try {
            processEngineConfiguration.setEventRegistryUniqueProcessInstanceCheckWithLock(false);

            ExecutorService executorService = Executors.newFixedThreadPool(4);

            executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("testCustomer"));
            executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("testCustomer"));
            executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("otherCustomer"));
            executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("otherCustomer"));

            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.MINUTES);

            assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("eventSubscriptionLocking").includeProcessVariables().list())
                    .extracting(instance -> instance.getProcessVariables().get("customerId"))
                    .containsExactlyInAnyOrder("testCustomer", "testCustomer", "otherCustomer", "otherCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("eventSubscriptionLocking").count()).isEqualTo(4);
        } finally {
            processEngineConfiguration.setEventRegistryUniqueProcessInstanceCheckWithLock(originalEventRegistryUniqueProcessInstanceCheckWithLock);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/EventSubscriptionLockingTest.testConcurrentStarts.bpmn20.xml")
    public void testConcurrentStartsMultipleEvents() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("kermit"));
        executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("kermit"));
        executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("fozzie"));
        executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("fozzie"));
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("eventSubscriptionLocking")
                .includeProcessVariables()
                .list();

        assertThat(instances)
                .extracting(instance -> instance.getProcessVariables().get("customerId"))
                .containsExactlyInAnyOrder("kermit", "fozzie");
    }

}
