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
package org.flowable.cmmn.engine.impl.eventregistry;

import java.util.Collection;
import java.util.List;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.eventregistry.consumer.BaseEventRegistryEventConsumer;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.eventsubscription.service.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CmmnEventRegistryEventConsumer extends BaseEventRegistryEventConsumer  {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CommandExecutor commandExecutor;

    public CmmnEventRegistryEventConsumer(CmmnEngineConfiguration cmmnEngineConfiguration, EventRegistry eventRegistry) {
        super(eventRegistry);
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.commandExecutor = cmmnEngineConfiguration.getCommandExecutor();
    }

    @Override
    protected void eventReceived(EventInstance eventInstance) {
        EventDefinition eventDefinition = eventInstance.getEventDefinition();

        // Fetching the event subscriptions happens in one transaction,
        // executing them one per subscription. There is no overarching transaction.
        // The reason for this is that the handling of one event subscription
        // should not influence (i.e. roll back) the handling of another.

        // Always execute the events without a correlation key
        List<EventSubscription> eventSubscriptions = findEventSubscriptionsByEventDefinitionKeyAndNoCorrelations(eventDefinition);
        CmmnRuntimeService cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        for (EventSubscription eventSubscription : eventSubscriptions) {
            handleEventSubscription(cmmnRuntimeService, eventSubscription, eventInstance);
        }

        Collection<String> correlationKeys = generateCorrelationKeys(eventInstance.getCorrelationParameterInstances());
        if (!correlationKeys.isEmpty()) {
            // If there are correlation keys then look for all event subscriptions matching them
            eventSubscriptions = findEventSubscriptionsByEventDefinitionKeyAndCorrelationKeys(eventDefinition, correlationKeys);
            for (EventSubscription eventSubscription : eventSubscriptions) {
                handleEventSubscription(cmmnRuntimeService, eventSubscription, eventInstance);
            }
        }

    }

    protected List<EventSubscription> findEventSubscriptionsByEventDefinitionKeyAndCorrelationKeys(EventDefinition eventDefinition, Collection<String> correlationKeys) {
        return commandExecutor.execute(commandContext ->
            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(
                new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()).configurations(correlationKeys).scopeType(ScopeTypes.CMMN)));
    }

    protected List<EventSubscription> findEventSubscriptionsByEventDefinitionKeyAndNoCorrelations(EventDefinition eventDefinition) {
        return commandExecutor.execute(commandContext ->
            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(
                new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()).withoutConfiguration().scopeType(ScopeTypes.CMMN)));
    }

    protected void handleEventSubscription(CmmnRuntimeService cmmnRuntimeService, EventSubscription eventSubscription,
        EventInstance eventInstance) {
        if (eventSubscription.getSubScopeId() != null) {
            cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(eventSubscription.getSubScopeId())
                .transientVariable("eventInstance", eventInstance)
                .trigger();

        } else if (eventSubscription.getScopeDefinitionId() != null
            && eventSubscription.getScopeId() == null && eventSubscription.getSubScopeId() == null) {
            cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(eventSubscription.getScopeDefinitionId())
                .transientVariable("eventInstance", eventInstance)
                .start();
        }
    }

}
