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

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Message;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.event.MessageEventHandler;
import org.flowable.engine.impl.event.SignalEventHandler;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * Manages event subscriptions for newly-deployed process definitions and their previous versions.
 */
public class EventSubscriptionManager {

    protected void removeObsoleteEventSubscriptionsImpl(ProcessDefinitionEntity processDefinition, String eventHandlerType) {
        // remove all subscriptions for the previous version
        EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager();
        List<EventSubscriptionEntity> subscriptionsToDelete = eventSubscriptionEntityManager.findEventSubscriptionsByTypeAndProcessDefinitionId(eventHandlerType, processDefinition.getId(), processDefinition.getTenantId());

        for (EventSubscriptionEntity eventSubscriptionEntity : subscriptionsToDelete) {
            eventSubscriptionEntityManager.delete(eventSubscriptionEntity);
        }
    }

    protected void removeObsoleteMessageEventSubscriptions(ProcessDefinitionEntity previousProcessDefinition) {
        // remove all subscriptions for the previous version
        if (previousProcessDefinition != null) {
            removeObsoleteEventSubscriptionsImpl(previousProcessDefinition, MessageEventHandler.EVENT_HANDLER_TYPE);
        }
    }

    protected void removeObsoleteSignalEventSubScription(ProcessDefinitionEntity previousProcessDefinition) {
        // remove all subscriptions for the previous version
        if (previousProcessDefinition != null) {
            removeObsoleteEventSubscriptionsImpl(previousProcessDefinition, SignalEventHandler.EVENT_HANDLER_TYPE);
        }
    }

    protected void addMessageEventSubscriptions(ProcessDefinitionEntity processDefinition, Process process, BpmnModel bpmnModel) {
        if (CollectionUtil.isNotEmpty(process.getFlowElements())) {
            for (FlowElement element : process.getFlowElements()) {
                if (element instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) element;
                    if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions())) {
                        EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                        if (eventDefinition instanceof MessageEventDefinition) {
                            MessageEventDefinition messageEventDefinition = (MessageEventDefinition) eventDefinition;
                            insertMessageEvent(messageEventDefinition, startEvent, processDefinition, bpmnModel);
                        }
                    }
                }
            }
        }
    }

    protected void insertMessageEvent(MessageEventDefinition messageEventDefinition, StartEvent startEvent, ProcessDefinitionEntity processDefinition, BpmnModel bpmnModel) {
        CommandContext commandContext = Context.getCommandContext();
        if (bpmnModel.containsMessageId(messageEventDefinition.getMessageRef())) {
            Message message = bpmnModel.getMessage(messageEventDefinition.getMessageRef());
            messageEventDefinition.setMessageRef(message.getName());
        }

        // look for subscriptions for the same name in db:
        List<EventSubscriptionEntity> subscriptionsForSameMessageName = CommandContextUtil.getEventSubscriptionEntityManager(commandContext)
                .findEventSubscriptionsByName(MessageEventHandler.EVENT_HANDLER_TYPE, messageEventDefinition.getMessageRef(), processDefinition.getTenantId());

        for (EventSubscriptionEntity eventSubscriptionEntity : subscriptionsForSameMessageName) {
            // throw exception only if there's already a subscription as start event
            if (eventSubscriptionEntity.getProcessInstanceId() == null || eventSubscriptionEntity.getProcessInstanceId().isEmpty()) { // processInstanceId != null or not empty -> it's a message
                                                                                                                                      // related to an execution
                // the event subscription has no instance-id, so it's a message start event
                throw new FlowableException("Cannot deploy process definition '" + processDefinition.getResourceName()
                        + "': there already is a message event subscription for the message with name '" + messageEventDefinition.getMessageRef() + "'.");
            }
        }

        MessageEventSubscriptionEntity newSubscription = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createMessageEventSubscription();
        newSubscription.setEventName(messageEventDefinition.getMessageRef());
        newSubscription.setActivityId(startEvent.getId());
        newSubscription.setConfiguration(processDefinition.getId());
        newSubscription.setProcessDefinitionId(processDefinition.getId());

        if (processDefinition.getTenantId() != null) {
            newSubscription.setTenantId(processDefinition.getTenantId());
        }

        CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(newSubscription);
    }

    protected void addSignalEventSubscriptions(CommandContext commandContext, ProcessDefinitionEntity processDefinition, org.flowable.bpmn.model.Process process, BpmnModel bpmnModel) {
        if (CollectionUtil.isNotEmpty(process.getFlowElements())) {
            for (FlowElement element : process.getFlowElements()) {
                if (element instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) element;
                    if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions())) {
                        EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                        if (eventDefinition instanceof SignalEventDefinition) {
                            SignalEventDefinition signalEventDefinition = (SignalEventDefinition) eventDefinition;
                            SignalEventSubscriptionEntity subscriptionEntity = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).createSignalEventSubscription();
                            Signal signal = bpmnModel.getSignal(signalEventDefinition.getSignalRef());
                            if (signal != null) {
                                subscriptionEntity.setEventName(signal.getName());
                            } else {
                                subscriptionEntity.setEventName(signalEventDefinition.getSignalRef());
                            }
                            subscriptionEntity.setActivityId(startEvent.getId());
                            subscriptionEntity.setProcessDefinitionId(processDefinition.getId());
                            if (processDefinition.getTenantId() != null) {
                                subscriptionEntity.setTenantId(processDefinition.getTenantId());
                            }

                            CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insert(subscriptionEntity);
                        }
                    }
                }
            }
        }
    }

}
