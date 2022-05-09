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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
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
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("testCustomer"));
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("eventSubscriptionLocking").count()).isEqualTo(1);

        executorService = Executors.newFixedThreadPool(32);

        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("otherCustomer"));
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("eventSubscriptionLocking").count()).isEqualTo(2);

    }

}
