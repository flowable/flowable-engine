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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.constant.EventConstants;
import org.flowable.eventregistry.impl.consumer.BaseEventRegistryEventConsumer;
import org.flowable.eventregistry.impl.consumer.CorrelationKey;
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

        Collection<CorrelationKey> correlationKeys = generateCorrelationKeys(eventInstance.getCorrelationParameterInstances());

        // Always execute the events without a correlation key (keys are passed in case they're stored)
        List<EventSubscription> eventSubscriptions = findEventSubscriptionsByEventDefinitionKeyAndNoCorrelations(eventModel);
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        for (EventSubscription eventSubscription : eventSubscriptions) {
            handleEventSubscription(runtimeService, eventSubscription, eventInstance, correlationKeys);
        }

        if (!correlationKeys.isEmpty()) {
            // If there are correlation keys then look for all event subscriptions matching them
            eventSubscriptions = findEventSubscriptionsByEventDefinitionKeyAndCorrelationKeys(eventModel, correlationKeys);
            for (EventSubscription eventSubscription : eventSubscriptions) {
                handleEventSubscription(runtimeService, eventSubscription, eventInstance, correlationKeys);
            }
        }
    }

    protected List<EventSubscription> findEventSubscriptionsByEventDefinitionKeyAndCorrelationKeys(EventModel eventDefinition, Collection<CorrelationKey> correlationKeys) {
        Set<String> allCorrelationKeyValues = correlationKeys.stream().map(CorrelationKey::getValue).collect(Collectors.toSet());

        return commandExecutor.execute(commandContext ->
            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(
                new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()).configurations(allCorrelationKeyValues).scopeType(ScopeTypes.BPMN)));
    }

    protected List<EventSubscription> findEventSubscriptionsByEventDefinitionKeyAndNoCorrelations(EventModel eventDefinition) {
        return commandExecutor.execute(commandContext ->
            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(
                new EventSubscriptionQueryImpl(commandContext).eventType(eventDefinition.getKey()).withoutConfiguration().scopeType(ScopeTypes.BPMN)));
    }

    protected void handleEventSubscription(RuntimeService runtimeService, EventSubscription eventSubscription,
            EventInstance eventInstance, Collection<CorrelationKey> correlationKeys) {

        if (eventSubscription.getExecutionId() != null) {

            // When an executionId is set, this means that the process instance is waiting at that step for an event

            Map<String, Object> transientVariableMap = new HashMap<>();
            transientVariableMap.put(EventConstants.EVENT_INSTANCE, eventInstance);
            runtimeService.trigger(eventSubscription.getExecutionId(), null, transientVariableMap);

        } else if (eventSubscription.getProcessDefinitionId() != null
                        && eventSubscription.getProcessInstanceId() == null && eventSubscription.getExecutionId() == null) {

            // If there is no execution/process instance set, but a definition id is set, this means that it's a start event

            ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(eventSubscription.getProcessDefinitionId())
                .transientVariable(EventConstants.EVENT_INSTANCE, eventInstance);

            if (correlationKeys != null) {
                String startCorrelationConfiguration = getStartCorrelationConfiguration(eventSubscription);

                CorrelationKey correlationKeyWithAllParameters = getCorrelationKeyWithAllParameters(correlationKeys);

                if (Objects.equals(startCorrelationConfiguration, BpmnXMLConstants.START_EVENT_CORRELATION_STORE_AS_BUSINESS_KEY)) {
                    processInstanceBuilder.businessKey(correlationKeyWithAllParameters.getValue());

                } else if (Objects.equals(startCorrelationConfiguration, BpmnXMLConstants.START_EVENT_CORRELATION_STORE_AS_UNIQUE_REFERENCE_ID)) {
                    long processInstanceCount = runtimeService.createProcessInstanceQuery()
                        .processInstanceReferenceId(correlationKeyWithAllParameters.getValue())
                        .processInstanceReferenceType(ReferenceTypes.EVENT_PROCESS)
                        .count();

                    if (processInstanceCount > 0) {
                        // Returning, no new instance should be started
                        return;
                    }

                    processInstanceBuilder.referenceId(correlationKeyWithAllParameters.getValue());
                    processInstanceBuilder.referenceType(ReferenceTypes.EVENT_PROCESS);

                }
            }

            processInstanceBuilder.start();
        }

    }

    protected String getStartCorrelationConfiguration(EventSubscription eventSubscription) {
        BpmnModel bpmnModel = processEngineConfiguration.getRepositoryService().getBpmnModel(eventSubscription.getProcessDefinitionId());
        if (bpmnModel != null) {

            // There are potentially multiple start events, with different configurations.
            // The one that has the matching eventType needs to be used

            List<StartEvent> startEvents = bpmnModel.getMainProcess().findFlowElementsOfType(StartEvent.class);
            for (StartEvent startEvent : startEvents) {
                List<ExtensionElement> eventTypes = startEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
                if (eventTypes != null && !eventTypes.isEmpty()
                        && Objects.equals(eventSubscription.getEventType(), eventTypes.get(0).getElementText())) {

                    List<ExtensionElement> correlationCfgExtensions = startEvent.getExtensionElements()
                        .getOrDefault(BpmnXMLConstants.START_EVENT_CORRELATION_CONFIGURATION, Collections.emptyList());
                    if (!correlationCfgExtensions.isEmpty()) {
                        return correlationCfgExtensions.get(0).getElementText();
                    }
                }
            }

        }

        return null;
    }

}
