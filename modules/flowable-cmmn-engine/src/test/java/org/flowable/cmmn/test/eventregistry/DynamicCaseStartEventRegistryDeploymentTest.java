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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.groups.Tuple;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Various tests for event-registry based case starts, both static and dynamic and the handling of the event subscriptions.
 *
 * @author Micha Kiener
 */
public class DynamicCaseStartEventRegistryDeploymentTest extends FlowableEventRegistryCmmnTestCase {

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryStaticStartTestCase.cmmn"
    })
    public void testStaticEventRegistryCaseStart() {
        sendEvent("kermit", "start");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryStaticStartTestCase");

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartWithoutManualSubscription() {
        sendEvent("kermit", "start");

        // there must be no running case instance as we didn't create a manual subscription yet
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryStaticStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartWithoutCaseDefinition() {
        FlowableIllegalArgumentException exception = Assertions.assertThrowsExactly(FlowableIllegalArgumentException.class, () -> {
            // manually register start subscription, but with different correlation than the actual event being sent
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
                .addCorrelationParameterValue("customer", "test")
                .addCorrelationParameterValue("action", "start")
                .subscribe();
        });

        assertThat(exception.getMessage()).isEqualTo("The case definition must be provided using the key for the subscription to be registered.");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryStaticStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartWithIllegalManualSubscriptionForWrongStartEvent() {
        FlowableIllegalArgumentException exception = Assertions.assertThrowsExactly(FlowableIllegalArgumentException.class, () -> {
            // manually register start subscription, but with different correlation than the actual event being sent
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
                .caseDefinitionKey("eventRegistryStaticStartTestCase")
                .addCorrelationParameterValue("customer", "test")
                .addCorrelationParameterValue("action", "start")
                .subscribe();
        });

        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("eventRegistryStaticStartTestCase").latestVersion().singleResult();

        assertThat(exception.getMessage()).isEqualTo("The case definition with id '" + caseDefinition.getId() + "' does not have an event-registry based start event with a manual subscription behavior.");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartWithNonMatchingManualSubscription() {
        // manually register start subscription, but with different correlation than the actual event being sent
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "test")
            .addCorrelationParameterValue("action", "start")
            .subscribe();

        sendEvent("kermit", "start");

        // there must be no running case instance as we didn't create a manual subscription yet
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartWithIllegalManualSubscriptionForWrongCorrelation() {
        FlowableIllegalArgumentException exception = Assertions.assertThrowsExactly(FlowableIllegalArgumentException.class, () -> {
            // manually register start subscription, but with different correlation than the actual event being sent
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
                .caseDefinitionKey("eventRegistryDynamicStartTestCase")
                .addCorrelationParameterValue("invalidCorrelationParameter", "test")
                .addCorrelationParameterValue("action", "start")
                .subscribe();
        });

        assertThat(exception.getMessage()).isEqualTo("There is no correlation parameter with name 'invalidCorrelationParameter' defined in event model with key 'simpleTest'. You can only subscribe for an event with a combination of valid correlation parameters.");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartWithMatchingManualSubscription() {
        // manually register start subscription, matching the event correlation sent later
        EventSubscription eventSubscription = cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .subscribe();

        assertThat(eventSubscription).isNotNull().extracting(EventSubscription::getScopeDefinitionKey).isEqualTo("eventRegistryDynamicStartTestCase");

        sendEvent("kermit", "start");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryDynamicStartTestCase");

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartWithMatchingManualSubscriptionVersion2() {
        // manually register start subscription, matching the event correlation sent later
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValues(Map.of("customer", "kermit", "action", "start"))
            .subscribe();

        sendEvent("kermit", "start");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryDynamicStartTestCase");

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryStaticStartTestCase.cmmn"
    })
    public void testStaticEventRegistryCaseStartAfterRedeployment() {
        sendEvent("kermit", "start");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryStaticStartTestCase");

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

        // redeploy the case definition (which removes and re-adds the static start subscription)
        CaseDefinition caseDefinition = deployCaseDefinition("eventRegistryStaticStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryStaticStartTestCase.cmmn");

        try {
            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(caseDefinition.getId());

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(caseDefinition.getDeploymentId());
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartAfterRedeployment() {
        // manually register start subscription, matching the event correlation sent later
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .subscribe();

        sendEvent("kermit", "start");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryDynamicStartTestCase");

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

        // the scope definition key must be present in the subscription
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionKey)
            .containsExactlyInAnyOrder("eventRegistryDynamicStartTestCase");

        // redeploy the case definition (which must not remove the subscriptions, but rather update them to the newest version)
        CaseDefinition caseDefinition = deployCaseDefinition("eventRegistryDynamicStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn");

        try {
            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", caseDefinition.getId()));

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(caseDefinition.getDeploymentId());
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartAfterRedeploymentWithoutAutoUpdate() {
        // manually register start subscription, matching the event correlation sent later
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        sendEvent("kermit", "start");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey)
                .containsExactlyInAnyOrder("eventRegistryDynamicStartTestCase");

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

        // the scope definition key must not be present in the event subscription
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionKey).containsOnlyNulls();

        // search the first case definition as we don't have auto-update in the subscription
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase").latestVersion().singleResult();

        // redeploy the case definition (which must not remove the subscriptions, but rather update them to the newest version)
        CaseDefinition latestCaseDefinition = deployCaseDefinition("eventRegistryDynamicStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn");

        try {
            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", caseDefinition.getId()));

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(latestCaseDefinition.getDeploymentId());
        }
    }

    @Test
    public void testSubscriptionDeletionAfterUndeploy() {
        CaseDefinition caseDefinition = deployCaseDefinition("eventRegistryDynamicStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn");

        try {
            // manually register start subscriptions
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
                .caseDefinitionKey("eventRegistryDynamicStartTestCase")
                .addCorrelationParameterValue("customer", "kermit")
                .addCorrelationParameterValue("action", "start")
                .subscribe();

            Map<String, Object> correlationParameters = new HashMap<>();
            correlationParameters.put("customer", "kermit");
            correlationParameters.put("action", "start");
            String correlationConfig1 = getEventRegistry().generateKey(correlationParameters);

            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
                .caseDefinitionKey("eventRegistryDynamicStartTestCase")
                .addCorrelationParameterValue("customer", "frog")
                .addCorrelationParameterValue("action", "end")
                .subscribe();

            correlationParameters.put("customer", "frog");
            correlationParameters.put("action", "end");
            String correlationConfig2 = getEventRegistry().generateKey(correlationParameters);

            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().scopeDefinitionId(caseDefinition.getId()).list())
                .extracting(EventSubscription::getScopeDefinitionId, EventSubscription::getConfiguration)
                .containsExactlyInAnyOrder(
                    Tuple.tuple(caseDefinition.getId(), correlationConfig1),
                    Tuple.tuple(caseDefinition.getId(), correlationConfig2));
        } finally {
            deleteDeployment(caseDefinition.getDeploymentId());
        }

        // now the subscriptions also need to be deleted, after the case definition was undeployed
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().scopeDefinitionId(caseDefinition.getId()).list()).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartAfterSubscriptionMigration() {
        // manually register start subscription, matching the event correlation sent later
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "frog")
            .addCorrelationParameterValue("action", "end")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        // search the first case definition as we don't have auto-update in the subscriptions
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase").latestVersion().singleResult();

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionId)
            .containsExactlyInAnyOrder(caseDefinition.getId(), caseDefinition.getId());

        // redeploy the case definition (which must not remove or update the existing subscriptions)
        CaseDefinition newCaseDefinition = deployCaseDefinition("eventRegistryDynamicStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn");

        try {
            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", caseDefinition.getId()));

            Task task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

            // now migrate to the latest case definition manually
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionModificationBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .migrateToLatestCaseDefinition();

            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
                .extracting(EventSubscription::getScopeDefinitionId)
                .containsExactlyInAnyOrder(newCaseDefinition.getId(), newCaseDefinition.getId());

            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", newCaseDefinition.getId()));

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newCaseDefinition.getDeploymentId());
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartAfterSingleSubscriptionMigration() {
        // manually register start subscription, matching the event correlation sent later
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
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

        // search the first case definition as we don't have auto-update in the subscriptions
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase").latestVersion().singleResult();

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionId, EventSubscription::getConfiguration)
            .containsExactlyInAnyOrder(
                Tuple.tuple(caseDefinition.getId(), correlationConfig1),
                Tuple.tuple(caseDefinition.getId(), correlationConfig2));

        // redeploy the case definition (which must not remove or update the existing subscriptions)
        CaseDefinition newCaseDefinition = deployCaseDefinition("eventRegistryDynamicStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn");

        try {
            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", caseDefinition.getId()));

            Task task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

            // now migrate to the latest case definition manually
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionModificationBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .addCorrelationParameterValues(Map.of("customer", "kermit", "action", "start"))
                .migrateToLatestCaseDefinition();

            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
                .extracting(EventSubscription::getScopeDefinitionId, EventSubscription::getConfiguration)
                .containsExactlyInAnyOrder(
                    Tuple.tuple(caseDefinition.getId(), correlationConfig2),
                    Tuple.tuple(newCaseDefinition.getId(), correlationConfig1));

            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", newCaseDefinition.getId()));

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newCaseDefinition.getDeploymentId());
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartAfterSingleSubscriptionMigrationToSpecificVersion() {
        // manually register start subscription, matching the event correlation sent later
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
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

        // search the first case definition as we don't have auto-update in the subscriptions
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase").latestVersion().singleResult();

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionId, EventSubscription::getConfiguration)
            .containsExactlyInAnyOrder(
                Tuple.tuple(caseDefinition.getId(), correlationConfig1),
                Tuple.tuple(caseDefinition.getId(), correlationConfig2));

        // redeploy the case definition (which must not remove or update the existing subscriptions)
        CaseDefinition newCaseDefinition = deployCaseDefinition("eventRegistryDynamicStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn");

        try {
            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", caseDefinition.getId()));

            Task task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

            // now migrate to the latest case definition manually
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionModificationBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .addCorrelationParameterValues(Map.of("customer", "kermit", "action", "start"))
                .migrateToCaseDefinition(newCaseDefinition.getId());

            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
                .extracting(EventSubscription::getScopeDefinitionId, EventSubscription::getConfiguration)
                .containsExactlyInAnyOrder(
                    Tuple.tuple(caseDefinition.getId(), correlationConfig2),
                    Tuple.tuple(newCaseDefinition.getId(), correlationConfig1));

            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", newCaseDefinition.getId()));

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newCaseDefinition.getDeploymentId());
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartAfterSingleSubscriptionDeletion() {
        // manually register start subscription, matching the event correlation sent later
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
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

        // search the first case definition as we don't have auto-update in the subscriptions
        CaseDefinition caseDefinition =cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase").latestVersion().singleResult();

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionId, EventSubscription::getConfiguration)
            .containsExactlyInAnyOrder(
                Tuple.tuple(caseDefinition.getId(), correlationConfig1),
                Tuple.tuple(caseDefinition.getId(), correlationConfig2));

        // redeploy the case definition (which must not remove or update the existing subscriptions)
        CaseDefinition newCaseDefinition = deployCaseDefinition("eventRegistryDynamicStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn");

        try {
            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", caseDefinition.getId()));

            Task task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

            // now migrate to the latest case definition manually
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionDeletionBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .addCorrelationParameterValues(Map.of("customer", "kermit", "action", "start"))
                .deleteSubscriptions();

            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
                .extracting(EventSubscription::getScopeDefinitionId, EventSubscription::getConfiguration)
                .containsExactlyInAnyOrder(Tuple.tuple(caseDefinition.getId(), correlationConfig2));

            sendEvent("kermit", "start");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

            sendEvent("frog", "end");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", caseDefinition.getId()));

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newCaseDefinition.getDeploymentId());
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn"
    })
    public void testDynamicEventRegistryCaseStartAfterAllSubscriptionDeletion() {
        // manually register start subscription, matching the event correlation sent later
        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
            .addCorrelationParameterValue("customer", "kermit")
            .addCorrelationParameterValue("action", "start")
            .doNotUpdateToLatestVersionAutomatically()
            .subscribe();

        cmmnRuntimeService.createCaseInstanceStartEventSubscriptionBuilder()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase")
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

        // search the first case definition as we don't have auto-update in the subscriptions
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey("eventRegistryDynamicStartTestCase").latestVersion().singleResult();

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list())
            .extracting(EventSubscription::getScopeDefinitionId, EventSubscription::getConfiguration)
            .containsExactlyInAnyOrder(
                Tuple.tuple(caseDefinition.getId(), correlationConfig1),
                Tuple.tuple(caseDefinition.getId(), correlationConfig2));

        // redeploy the case definition (which must not remove or update the existing subscriptions)
        CaseDefinition newCaseDefinition = deployCaseDefinition("eventRegistryDynamicStartTestCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.eventRegistryDynamicStartTestCase.cmmn");

        try {
            sendEvent("kermit", "start");

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list())
                .extracting(CaseInstance::getCaseDefinitionKey, CaseInstance::getCaseDefinitionId)
                .containsExactlyInAnyOrder(Tuple.tuple("eventRegistryDynamicStartTestCase", caseDefinition.getId()));

            Task task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task).isNotNull();

            cmmnTaskService.complete(task.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

            // now migrate to the latest case definition manually
            cmmnRuntimeService.createCaseInstanceStartEventSubscriptionDeletionBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .deleteSubscriptions();

            assertThat(cmmnRuntimeService.createEventSubscriptionQuery().eventType("simpleTest").list()).isEmpty();

            sendEvent("kermit", "start");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

            sendEvent("frog", "end");
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();
        } finally {
            deleteDeployment(newCaseDefinition.getDeploymentId());
        }
    }

    protected CaseInstance sendEvent(String customerId, String action) {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("sendTestEventCase")
            .variable("customerId", customerId)
            .variable("customerName", "Kermit the Frog")
            .variable("eventKey", "simpleTest")
            .variable("action", action)
            .start();

        return caseInstance;
    }

    @Before
    public void deployEventDefinitionAndSendEventCaseDefinition() {
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        eventRepositoryService.createDeployment()
            .name("SimpleEvent")
            .addClasspathResource("org/flowable/cmmn/test/eventregistry/SendInternalEventTaskTest.simple.event")
            .deploy();

        deployCaseDefinition("sendTestEventCase.cmmn",
            "org/flowable/cmmn/test/eventregistry/DynamicCaseStartEventRegistryDeploymentTest.sendTestEventCase.cmmn");
    }

    @After
    public void deleteEventAndCaseDeployment() {
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }

        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("sendTestEventCase").singleResult();
        if (caseDefinition != null) {
            deleteDeployment(caseDefinition.getDeploymentId());
        }
    }
}
