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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.runtime.ProcessInstanceStartEventSubscriptionBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * This command creates and registers a new process start event subscription based on the provided builder information.
 *
 * @author Micha Kiener
 */
public class RegisterProcessInstanceStartEventSubscriptionCmd extends AbstractProcessStartEventSubscriptionCmd implements Command<EventSubscription>, Serializable {

    private static final long serialVersionUID = 1L;

    protected final ProcessInstanceStartEventSubscriptionBuilderImpl builder;

    public RegisterProcessInstanceStartEventSubscriptionCmd(ProcessInstanceStartEventSubscriptionBuilderImpl builder) {
        this.builder = builder;
    }

    @Override
    public EventSubscription execute(CommandContext commandContext) {
        ProcessDefinition processDefinition = getLatestProcessDefinitionByKey(builder.getProcessDefinitionKey(), builder.getTenantId(), commandContext);
        Process process = getProcess(processDefinition.getId(), commandContext);

        EventSubscription eventSubscription = null;
        List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class, false);
        for (StartEvent startEvent : startEvents) {
            // looking for a start event based on an event-registry event subscription
            List<ExtensionElement> eventTypeElements = startEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
            if (eventTypeElements != null && eventTypeElements.size() > 0) {
                // looking for a dynamic, manually subscribed behavior of the event-registry start event
                List<ExtensionElement> correlationConfiguration = startEvent.getExtensionElements().get(BpmnXMLConstants.START_EVENT_CORRELATION_CONFIGURATION);
                if (correlationConfiguration != null && correlationConfiguration.size() > 0 &&
                    BpmnXMLConstants.START_EVENT_CORRELATION_MANUAL.equals(correlationConfiguration.get(0).getElementText())) {

                    // currently, only one event-registry start event is supported for manual subscriptions
                    if (eventSubscription != null) {
                        throw new FlowableIllegalArgumentException("The process definition with id " + processDefinition.getId()
                            + " has more than one event-registry start events based on manually registered subscriptions, which is currently not supported.");
                    }

                    String eventDefinitionKey = eventTypeElements.get(0).getElementText();
                    String correlationKey = generateCorrelationConfiguration(eventDefinitionKey, builder.getTenantId(), 
                            builder.getCorrelationParameterValues(), commandContext);

                    eventSubscription = insertEventRegistryEvent(eventDefinitionKey, builder.isDoNotUpdateToLatestVersionAutomatically(), startEvent, processDefinition,
                        correlationKey, commandContext);
                }
            }
        }

        if (eventSubscription == null) {
            throw new FlowableIllegalArgumentException("The process definition with id '" + processDefinition.getId()
                + "' does not have an event-registry based start event with a manual subscription behavior.");
        }

        return eventSubscription;
    }

    protected EventSubscription insertEventRegistryEvent(String eventDefinitionKey, boolean doNotUpdateToLatestVersionAutomatically, StartEvent startEvent,
            ProcessDefinition processDefinition, String correlationKey, CommandContext commandContext) {
        
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        EventSubscriptionBuilder eventSubscriptionBuilder = eventSubscriptionService.createEventSubscriptionBuilder()
                .eventType(eventDefinitionKey)
                .activityId(startEvent.getId())
                .processDefinitionId(processDefinition.getId())
                .scopeType(ScopeTypes.BPMN)
                .configuration(correlationKey);

        if (processDefinition.getTenantId() != null) {
            eventSubscriptionBuilder.tenantId(processDefinition.getTenantId());
        }

        // if we need to update the process definition to the latest version upon new deployment, also set the definition key, not just the process definition id
        if (!doNotUpdateToLatestVersionAutomatically) {
            eventSubscriptionBuilder.scopeDefinitionKey(processDefinition.getKey());
        }

        EventSubscription eventSubscription = eventSubscriptionBuilder.create();
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);

        return eventSubscription;
    }
}
