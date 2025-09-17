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
package org.flowable.engine.impl.bpmn.deployer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.event.EventDefinitionExpressionUtil;
import org.flowable.engine.impl.event.MessageEventHandler;
import org.flowable.engine.impl.event.SignalEventHandler;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CorrelationUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * Manages event subscriptions for newly-deployed process definitions and their previous versions.
 */
public class EventSubscriptionManager {

    protected void removeObsoleteMessageEventSubscriptions(ProcessDefinitionEntity previousProcessDefinition) {
        // remove all subscriptions for the previous version
        if (previousProcessDefinition != null) {
            removeObsoleteEventSubscriptions(previousProcessDefinition, MessageEventHandler.EVENT_HANDLER_TYPE);
        }
    }

    protected void removeObsoleteSignalEventSubscription(ProcessDefinitionEntity previousProcessDefinition) {
        // remove all subscriptions for the previous version
        if (previousProcessDefinition != null) {
            removeObsoleteEventSubscriptions(previousProcessDefinition, SignalEventHandler.EVENT_HANDLER_TYPE);
        }
    }

    protected void removeOrUpdateObsoleteEventRegistryEventSubscription(ProcessDefinitionEntity previousProcessDefinition, ProcessDefinitionEntity processDefinition) {
        // remove all subscriptions for the previous version or update them for a dynamic, manual subscription behavior
        if (previousProcessDefinition != null) {
            List<StartEventInfo> eventRegistryStartEvents = getEventRegistryStartEventEventTypes(previousProcessDefinition);
            if (eventRegistryStartEvents != null) {
                for (StartEventInfo eventRegistryStartEvent : eventRegistryStartEvents) {
                    if (eventRegistryStartEvent.dynamic()) {
                        // for a dynamic, manual subscription behavior, we must not remove the old subscriptions, but rather update them
                        // to the newest process definition id, as they have been manually added before
                        updateOldEventSubscriptions(previousProcessDefinition, processDefinition, eventRegistryStartEvent.eventType(), eventRegistryStartEvent.activityId());
                    } else {
                        // for a static starting behavior, we always remove the old subscription and recreate it with the new definition
                        removeObsoleteEventSubscriptions(previousProcessDefinition, eventRegistryStartEvent.eventType());
                    }
                }
            }
        }
    }

    protected List<StartEventInfo> getEventRegistryStartEventEventTypes(ProcessDefinitionEntity previousProcessDefinition) {
        List<StartEventInfo> result = null;
        Process process = ProcessDefinitionUtil.getProcess(previousProcessDefinition.getId());
        List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class, true);
        if (!startEvents.isEmpty()) {
            for (StartEvent startEvent : startEvents) {
                if (CollectionUtil.isEmpty(startEvent.getEventDefinitions())) {
                    List<ExtensionElement> eventTypeElements = startEvent.getExtensionElements().get("eventType");
                    if (eventTypeElements != null && !eventTypeElements.isEmpty()) {
                        String eventType = eventTypeElements.get(0).getElementText();
                        if (StringUtils.isNotEmpty(eventType)) {
                            if (result == null) {
                                result = new ArrayList<>();
                            }

                            // check the starting behavior of the event-registry start event, if it is dynamic with manual subscriptions, add it to the
                            // result with a true boolean, otherwise with false
                            List<ExtensionElement> correlationConfiguration = startEvent.getExtensionElements().get(BpmnXMLConstants.START_EVENT_CORRELATION_CONFIGURATION);
                            if (correlationConfiguration != null && correlationConfiguration.size() > 0 && BpmnXMLConstants.START_EVENT_CORRELATION_MANUAL.equals(correlationConfiguration.get(0).getElementText())) {
                                result.add(new StartEventInfo(eventType, startEvent.getId(), true));
                            } else {
                                result.add(new StartEventInfo(eventType, startEvent.getId(), false));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    protected record StartEventInfo(String eventType, String activityId, boolean dynamic) {
    }

    protected void removeObsoleteEventSubscriptions(ProcessDefinitionEntity processDefinition, String eventHandlerType) {
        // remove all subscriptions for the previous version
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        List<EventSubscriptionEntity> subscriptionsToDelete = eventSubscriptionService
            .findEventSubscriptionsByTypeAndProcessDefinitionId(eventHandlerType, processDefinition.getId(), processDefinition.getTenantId());

        for (EventSubscriptionEntity eventSubscriptionEntity : subscriptionsToDelete) {
            eventSubscriptionService.deleteEventSubscription(eventSubscriptionEntity);
            CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscriptionEntity);
        }
    }

    protected void updateOldEventSubscriptions(ProcessDefinitionEntity previousProcessDefinition, ProcessDefinitionEntity processDefinition,
        String eventType, String activityId) {
        CommandContext commandContext = Context.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService().updateEventSubscriptionProcessDefinitionId(
            previousProcessDefinition.getId(), processDefinition.getId(), eventType, activityId, processDefinition.getKey(), null);
    }

    protected void addEventSubscriptions(ProcessDefinitionEntity processDefinition, org.flowable.bpmn.model.Process process, BpmnModel bpmnModel) {
        if (CollectionUtil.isNotEmpty(process.getFlowElements())) {
            for (FlowElement element : process.getFlowElements()) {
                if (element instanceof StartEvent startEvent) {
                    if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions())) {
                        EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                        if (eventDefinition instanceof SignalEventDefinition signalEventDefinition) {
                            insertSignalEvent(signalEventDefinition, startEvent, processDefinition, bpmnModel);
                        
                        } else if (eventDefinition instanceof MessageEventDefinition messageEventDefinition) {
                            insertMessageEvent(messageEventDefinition, startEvent, processDefinition, bpmnModel);
                        }
                        
                    } else {
                        if (startEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE) != null) {
                            List<ExtensionElement> eventTypeElements = startEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
                            if (!eventTypeElements.isEmpty()) {
                                String eventDefinitionKey = eventTypeElements.get(0).getElementText();
                                insertEventRegistryEvent(eventDefinitionKey, startEvent, processDefinition, bpmnModel);
                            }
                        }
                    }
                }
            }
        }
    }
    
    protected void insertSignalEvent(SignalEventDefinition signalEventDefinition, StartEvent startEvent, ProcessDefinitionEntity processDefinition, BpmnModel bpmnModel) {
        CommandContext commandContext = Context.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        SignalEventSubscriptionEntity subscriptionEntity = eventSubscriptionService.createSignalEventSubscription();

        String signalName = EventDefinitionExpressionUtil.determineSignalName(commandContext, signalEventDefinition, bpmnModel,null);
        subscriptionEntity.setEventName(signalName);

        subscriptionEntity.setActivityId(startEvent.getId());
        subscriptionEntity.setProcessDefinitionId(processDefinition.getId());
        if (processDefinition.getTenantId() != null) {
            subscriptionEntity.setTenantId(processDefinition.getTenantId());
        }

        eventSubscriptionService.insertEventSubscription(subscriptionEntity);
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(subscriptionEntity);
    }
    
    protected void insertMessageEvent(MessageEventDefinition messageEventDefinition, StartEvent startEvent, ProcessDefinitionEntity processDefinition, BpmnModel bpmnModel) {
        CommandContext commandContext = Context.getCommandContext();

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        // look for subscriptions for the same name in db:
        String messageName = EventDefinitionExpressionUtil.determineMessageName(commandContext, messageEventDefinition, null);
        List<EventSubscriptionEntity> subscriptionsForSameMessageName = eventSubscriptionService
                .findEventSubscriptionsByName(MessageEventHandler.EVENT_HANDLER_TYPE, messageName, processDefinition.getTenantId());

        for (EventSubscriptionEntity eventSubscriptionEntity : subscriptionsForSameMessageName) {
            // throw exception only if there's already a subscription as start event
            if (eventSubscriptionEntity.getProcessInstanceId() == null || eventSubscriptionEntity.getProcessInstanceId().isEmpty()) { // processInstanceId != null or not empty -> it's a message related to an execution
                // the event subscription has no instance-id, so it's a message start event
                throw new FlowableException("Cannot deploy process definition '" + processDefinition.getResourceName()
                        + "': there already is a message event subscription for the message with name '" + messageName + "'. For " + eventSubscriptionEntity);
            }
        }

        MessageEventSubscriptionEntity newSubscription = eventSubscriptionService.createMessageEventSubscription();
        newSubscription.setEventName(messageName);
        newSubscription.setActivityId(startEvent.getId());
        newSubscription.setConfiguration(processDefinition.getId());
        newSubscription.setProcessDefinitionId(processDefinition.getId());

        if (processDefinition.getTenantId() != null) {
            newSubscription.setTenantId(processDefinition.getTenantId());
        }

        eventSubscriptionService.insertEventSubscription(newSubscription);
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(newSubscription);
    }
    
    protected void insertEventRegistryEvent(String eventDefinitionKey, StartEvent startEvent, ProcessDefinitionEntity processDefinition, BpmnModel bpmnModel) {
         // check, if we have a dynamic event-based start for that process definition
        List<ExtensionElement> correlationConfiguration = startEvent.getExtensionElements().get(BpmnXMLConstants.START_EVENT_CORRELATION_CONFIGURATION);
        if (correlationConfiguration != null && correlationConfiguration.size() > 0) {
            if (BpmnXMLConstants.START_EVENT_CORRELATION_MANUAL.equals(correlationConfiguration.get(0).getElementText())) {
                return;
            }
        }

        CommandContext commandContext = Context.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        EventSubscriptionBuilder eventSubscriptionBuilder = eventSubscriptionService.createEventSubscriptionBuilder()
                .eventType(eventDefinitionKey)
                .activityId(startEvent.getId())
                .processDefinitionId(processDefinition.getId())
                .scopeType(ScopeTypes.BPMN)
                .configuration(CorrelationUtil.getCorrelationKey(BpmnXMLConstants.ELEMENT_EVENT_CORRELATION_PARAMETER, commandContext, startEvent, null));
                
        if (processDefinition.getTenantId() != null) {
            eventSubscriptionBuilder.tenantId(processDefinition.getTenantId());
        }

        EventSubscription eventSubscription = eventSubscriptionBuilder.create();
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
    }

}
