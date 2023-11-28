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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.event.ProcessStartEventSubscriptionBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;
import org.flowable.eventsubscription.service.EventSubscriptionService;

/**
 * This command creates and registers a new process start event subscription based on the provided builder information.
 *
 * @author Micha Kiener
 */
public class RegisterProcessStartEventSubscriptionCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected final ProcessStartEventSubscriptionBuilderImpl builder;

    public RegisterProcessStartEventSubscriptionCmd(ProcessStartEventSubscriptionBuilderImpl builder) {
        this.builder = builder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        ProcessDefinition processDefinition = getProcessDefinition(commandContext);
        Process process = getProcess(processDefinition.getId(), commandContext);

        boolean subscriptionCreated = false;
        List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class, false);
        for (StartEvent startEvent : startEvents) {
            // looking for a start event based on an event-registry event subscription
            List<ExtensionElement> eventTypeElements = startEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
            if (eventTypeElements != null && eventTypeElements.size() > 0) {
                // looking for a dynamic, manually subscribed behavior of the event-registry start event
                List<ExtensionElement> correlationConfiguration = startEvent.getExtensionElements().get(BpmnXMLConstants.START_EVENT_CORRELATION_CONFIGURATION);
                if (correlationConfiguration != null && correlationConfiguration.size() > 0 &&
                    "manualSubscriptions".equals(correlationConfiguration.get(0).getElementText())) {

                    String eventDefinitionKey = eventTypeElements.get(0).getElementText();
                    String correlationKey = generateCorrelationConfiguration(eventDefinitionKey, commandContext);

                    insertEventRegistryEvent(eventDefinitionKey, startEvent, processDefinition, correlationKey, commandContext);
                    subscriptionCreated = true;
                }
            }
        }

        if (!subscriptionCreated) {
            throw new FlowableIllegalArgumentException("The process definition with key '" + builder.getProcessDefinitionKey()
                + "' does not have an event-registry based start event with a manual subscription behavior.");
        }

        return null;
    }

    protected String generateCorrelationConfiguration(String eventDefinitionKey, CommandContext commandContext) {
        EventModel eventModel = getEventModel(eventDefinitionKey, commandContext);
        Map<String, Object> correlationParameters = new HashMap<>();
        for (Map.Entry<String, Object> correlationValue : builder.getCorrelationParameterValues().entrySet()) {
            // make sure the correlation parameter value is based on a valid, defined correlation parameter within the event model
            checkEventModelCorrelationParameter(eventModel, correlationValue.getKey());
            correlationParameters.put(correlationValue.getKey(), correlationValue.getValue());
        }

        return CommandContextUtil.getEventRegistry().generateKey(correlationParameters);
    }

    protected void checkEventModelCorrelationParameter(EventModel eventModel, String correlationParameterName) {
        Collection<EventPayload> correlationParameters = eventModel.getCorrelationParameters();
        for (EventPayload correlationParameter : correlationParameters) {
            if (correlationParameter.getName().equals(correlationParameterName)) {
                return;
            }
        }
        throw new FlowableIllegalArgumentException("There is no correlation parameter with name '" + correlationParameterName + "' defined in event model "
            + "with key '" + eventModel.getKey() + "'. You can only subscribe for an event with a combination of valid correlation parameters.");
    }

    protected void insertEventRegistryEvent(String eventDefinitionKey, StartEvent startEvent, ProcessDefinition processDefinition, String correlationKey,
        CommandContext commandContext) {
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

        EventSubscription eventSubscription = eventSubscriptionBuilder.create();
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
    }

    protected ProcessDefinition getProcessDefinition(CommandContext commandContext) {
        RepositoryService repositoryService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getRepositoryService();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(builder.getProcessDefinitionKey())
            .latestVersion()
            .singleResult();

        if (processDefinition == null) {
            throw new FlowableIllegalArgumentException("No deployed process definition found for key '" + builder.getProcessDefinitionKey() + "'.");
        }
        return processDefinition;
    }

    protected Process getProcess(String processDefinitionId, CommandContext commandContext) {
        RepositoryService repositoryService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getRepositoryService();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        return bpmnModel.getMainProcess();
    }

    protected EventModel getEventModel(String eventDefinitionKey, CommandContext commandContext) {
        EventModel eventModel = CommandContextUtil.getEventRepositoryService(commandContext).getEventModelByKey(eventDefinitionKey);
        if (eventModel == null) {
            throw new FlowableIllegalArgumentException("Could not find event model with key '" + eventDefinitionKey + "'.");
        }
        return eventModel;
    }
}
