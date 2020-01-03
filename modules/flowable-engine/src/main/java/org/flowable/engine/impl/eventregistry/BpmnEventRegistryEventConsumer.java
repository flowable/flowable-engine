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
package org.flowable.engine.impl.eventregistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.consumer.BaseEventRegistryEventConsumer;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.eventsubscription.service.impl.util.CommandContextUtil;

public class BpmnEventRegistryEventConsumer extends BaseEventRegistryEventConsumer  {

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected CommandExecutor commandExecutor;

    public BpmnEventRegistryEventConsumer(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
        
        this.processEngineConfiguration = processEngineConfiguration;
        this.commandExecutor = processEngineConfiguration.getCommandExecutor();
    }

    @Override
    public String getConsumerKey() {
        return "bpmnEventConsumer";
    }
    
    @Override
    protected void eventReceived(EventInstance eventInstance) {
        EventModel eventModel = eventInstance.getEventModel();

        // Fetching the event subscriptions happens in one transaction,
        // executing them one per subscription. There is no overarching transaction.
        // The reason for this is that the handling of one event subscription
        // should not influence (i.e. roll back) the handling of another.

        // Always execute the events without a correlation key
        List<EventSubscription> eventSubscriptions = findEventSubscriptionsByEventDefinitionKeyAndNoCorrelations(eventModel);
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        for (EventSubscription eventSubscription : eventSubscriptions) {
            handleEventSubscription(runtimeService, eventSubscription, eventInstance);
        }

        Collection<String> correlationKeys = generateCorrelationKeys(eventInstance.getCorrelationParameterInstances());
        if (!correlationKeys.isEmpty()) {
            // If there are correlation keys then look for all event subscriptions matching them
            eventSubscriptions = findEventSubscriptionsByEventDefinitionKeyAndCorrelationKeys(eventModel, correlationKeys);
            for (EventSubscription eventSubscription : eventSubscriptions) {
                handleEventSubscription(runtimeService, eventSubscription, eventInstance);
            }
        }
    }

    protected List<EventSubscription> findEventSubscriptionsByEventDefinitionKeyAndCorrelationKeys(EventModel eventDefinition, Collection<String> correlationKeys) {
        return commandExecutor.execute(commandContext ->
            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(
                new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()).configurations(correlationKeys).scopeType(ScopeTypes.BPMN)));
    }

    protected List<EventSubscription> findEventSubscriptionsByEventDefinitionKeyAndNoCorrelations(EventModel eventDefinition) {
        return commandExecutor.execute(commandContext ->
            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(
                new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()).withoutConfiguration().scopeType(ScopeTypes.BPMN)));
    }

    protected void handleEventSubscription(RuntimeService runtimeService, EventSubscription eventSubscription, EventInstance eventInstance) {
        if (eventSubscription.getExecutionId() != null) {
            Map<String, Object> transientVariableMap = new HashMap<>();
            transientVariableMap.put("eventInstance", eventInstance);
            runtimeService.trigger(eventSubscription.getExecutionId(), null, transientVariableMap);

        } else if (eventSubscription.getProcessDefinitionId() != null
                        && eventSubscription.getProcessInstanceId() == null && eventSubscription.getExecutionId() == null) {
            
            runtimeService.createProcessInstanceBuilder().processDefinitionId(eventSubscription.getProcessDefinitionId())
                    .transientVariable("eventInstance", eventInstance)
                    .startAsync();
        }
    }

}
