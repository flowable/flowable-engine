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

import java.util.List;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.definition.CorrelationDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.eventregistry.consumer.BaseEventRegistryEventConsumer;
import org.flowable.common.engine.impl.eventregistry.definition.AlwaysAppliesEventCorrelationDefinition;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.eventsubscription.service.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class CmmnEventRegistryEventConsumer extends BaseEventRegistryEventConsumer  {

    protected CommandExecutor commandExecutor;

    public CmmnEventRegistryEventConsumer(CommandExecutor commandExecutor, EventRegistry eventRegistry) {
        super(eventRegistry);
        this.commandExecutor = commandExecutor;
    }

    @Override
    protected void eventReceived(EventInstance eventInstance) {
        EventDefinition eventDefinition = eventInstance.getEventDefinition();
        CorrelationDefinition correlationDefinition = eventDefinition.getCorrelationDefinition();

        if (correlationDefinition instanceof AlwaysAppliesEventCorrelationDefinition) {

            // In this case, no correlation needs to happen.
            // This means that all event subscriptions can be fetched with the given type and they can be fired
            commandExecutor.execute((Command<Void>) commandContext -> {
                List<EventSubscription> eventSubscriptions = findEventsubScriptionsByEventDefinitionKey(eventDefinition);

                for (EventSubscription eventSubscription : eventSubscriptions) {
                    if (ScopeTypes.CMMN.equals(eventSubscription.getScopeType())) {

                        if (eventSubscription.getSubScopeId() != null) {
                            CmmnRuntimeService cmmnRuntimeService = org.flowable.cmmn.engine.impl.util.CommandContextUtil.getCmmnRuntimeService();
                            cmmnRuntimeService.triggerPlanItemInstance(eventSubscription.getSubScopeId());
                        }

                    }
                }

                return null;
            });

        }
    }

    protected List<EventSubscription> findEventsubScriptionsByEventDefinitionKey(EventDefinition eventDefinition) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager(commandContext);
        return eventSubscriptionEntityManager
            .findEventSubscriptionsByQueryCriteria(new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()));
    }

}
