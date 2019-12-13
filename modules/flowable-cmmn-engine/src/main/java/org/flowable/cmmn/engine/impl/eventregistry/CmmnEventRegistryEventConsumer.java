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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.consumer.BaseEventRegistryEventConsumer;
import org.flowable.eventregistry.model.EventModel;
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

    public CmmnEventRegistryEventConsumer(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.commandExecutor = cmmnEngineConfiguration.getCommandExecutor();
    }

    @Override
    protected void eventReceived(EventInstance eventInstance) {
        EventModel eventModel = eventInstance.getEventModel();

        // Fetching the event subscriptions happens in one transaction,
        // executing them one per subscription. There is no overarching transaction.
        // The reason for this is that the handling of one event subscription
        // should not influence (i.e. roll back) the handling of another.

        Collection<String> correlationKeys = generateCorrelationKeys(eventInstance.getCorrelationParameterInstances());

        // Always execute the events without a correlation key
        List<EventSubscription> eventSubscriptions = findEventSubscriptionsByEventDefinitionKeyAndNoCorrelations(eventModel);
        CmmnRuntimeService cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        for (EventSubscription eventSubscription : eventSubscriptions) {
            handleEventSubscription(cmmnRuntimeService, eventSubscription, eventInstance, correlationKeys);
        }

        if (!correlationKeys.isEmpty()) {
            // If there are correlation keys then look for all event subscriptions matching them
            eventSubscriptions = findEventSubscriptionsByEventDefinitionKeyAndCorrelationKeys(eventModel, correlationKeys);
            for (EventSubscription eventSubscription : eventSubscriptions) {
                handleEventSubscription(cmmnRuntimeService, eventSubscription, eventInstance, correlationKeys);
            }
        }

    }

    protected List<EventSubscription> findEventSubscriptionsByEventDefinitionKeyAndCorrelationKeys(EventModel eventDefinition, Collection<String> correlationKeys) {
        return commandExecutor.execute(commandContext ->
            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(
                new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()).configurations(correlationKeys).scopeType(ScopeTypes.CMMN)));
    }

    protected List<EventSubscription> findEventSubscriptionsByEventDefinitionKeyAndNoCorrelations(EventModel eventDefinition) {
        return commandExecutor.execute(commandContext ->
            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(
                new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()).withoutConfiguration().scopeType(ScopeTypes.CMMN)));
    }

    protected void handleEventSubscription(CmmnRuntimeService cmmnRuntimeService, EventSubscription eventSubscription, EventInstance eventInstance, Collection<String> correlationKeys) {
        if (eventSubscription.getSubScopeId() != null) {

            cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(eventSubscription.getSubScopeId())
                .transientVariable("eventInstance", eventInstance)
                .trigger();

        } else if (eventSubscription.getScopeDefinitionId() != null
                && eventSubscription.getScopeId() == null && eventSubscription.getSubScopeId() == null) {

            CaseInstanceBuilder caseInstanceBuilder = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(eventSubscription.getScopeDefinitionId())
                .transientVariable("eventInstance", eventInstance);

            if (correlationKeys != null) {
                String startCorrelationConfiguration = getStartCorrelationConfiguration(eventSubscription);

                String lastCorrelationKey = null;
                for (String correlationKey : correlationKeys) {
                    lastCorrelationKey = correlationKey;
                }

                if (Objects.equals(startCorrelationConfiguration, "storeAsBusinessKey")) {
                    caseInstanceBuilder.businessKey(lastCorrelationKey);

                } else if (Objects.equals(startCorrelationConfiguration, "storeAsUniqueBusinessKey")) {
                    long caseInstanceCount = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey(lastCorrelationKey).count();
                    if (caseInstanceCount > 0) {
                        // Returning, no new instance should be started
                        return;
                    }
                    caseInstanceBuilder.businessKey(lastCorrelationKey);

                }
            }

            caseInstanceBuilder.start();

        }
    }

    protected String getStartCorrelationConfiguration(EventSubscription eventSubscription) {
        CmmnModel cmmnModel = cmmnEngineConfiguration.getCmmnRepositoryService().getCmmnModel(eventSubscription.getScopeDefinitionId());
        if (cmmnModel != null) {
            List<ExtensionElement> correlationCfgExtensions = cmmnModel.getPrimaryCase().getExtensionElements()
                .getOrDefault("startEventCorrelationConfiguration", Collections.emptyList());
            if (!correlationCfgExtensions.isEmpty()) {
                return correlationCfgExtensions.get(0).getElementText();
            }
        }
        return null;
    }

}
