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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class EventSubscriptionLockingTest extends AbstractCmmnEventRegistryConsumerTest {

    @Test
    @CmmnDeployment
    public void testEventSubscriptionCanBeLockedAndUnlocked() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("testCaseStartEvent")
                .singleResult();

        EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().caseDefinitionId(caseDefinition.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();

        assertThat(eventSubscription.getLockOwner()).isNull();
        assertThat(eventSubscription.getLockTime()).isNull();

        EventSubscription finalEventSubscription = eventSubscription;
        ((CmmnManagementServiceImpl) cmmnManagementService).executeCommand(commandContext -> {
            EventSubscriptionService eventSubscriptionService = cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            eventSubscriptionService.lockEventSubscription(finalEventSubscription.getId());
            return null;
        });

        eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().caseDefinitionId(caseDefinition.getId()).singleResult();
        assertThat(eventSubscription.getLockOwner()).isNotNull();
        assertThat(eventSubscription.getLockTime()).isNotNull();

        ((CmmnManagementServiceImpl) cmmnManagementService).executeCommand(commandContext -> {
            EventSubscriptionService eventSubscriptionService = cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            eventSubscriptionService.unlockEventSubscription(finalEventSubscription.getId());
            return null;
        });

        eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().caseDefinitionId(caseDefinition.getId()).singleResult();
        assertThat(eventSubscription.getLockOwner()).isNull();
        assertThat(eventSubscription.getLockTime()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testConcurrentStarts() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("testCustomer"));
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("testCaseStartEvent").count()).isEqualTo(1);

        executorService = Executors.newFixedThreadPool(32);

        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> inboundEventChannelAdapter.triggerTestEvent("otherCustomer"));
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("testCaseStartEvent").count()).isEqualTo(2);

    }

}
