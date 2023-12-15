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
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.runtime.ProcessInstanceStartEventSubscriptionModificationBuilderImpl;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * This command either modifies event subscriptions with a process start event and optional correlation parameter values.
 *
 * @author Micha Kiener
 */
public class ModifyProcessInstanceStartEventSubscriptionCmd extends AbstractProcessStartEventSubscriptionCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected final ProcessInstanceStartEventSubscriptionModificationBuilderImpl builder;

    public ModifyProcessInstanceStartEventSubscriptionCmd(ProcessInstanceStartEventSubscriptionModificationBuilderImpl builder) {
        this.builder = builder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        ProcessDefinition newProcessDefinition;
        if (builder.hasNewProcessDefinitionId()) {
            newProcessDefinition = getProcessDefinitionById(builder.getNewProcessDefinitionId(), commandContext);
        } else {
            // no explicit process definition provided, so use latest one
            ProcessDefinition processDefinition = getProcessDefinitionById(builder.getProcessDefinitionId(), commandContext);
            newProcessDefinition = getLatestProcessDefinitionByKey(processDefinition.getKey(), processDefinition.getTenantId(), commandContext);
        }

        if (newProcessDefinition == null) {
            throw new FlowableIllegalArgumentException("Cannot find process definition with id " + (builder.hasNewProcessDefinitionId() ?
                builder.getNewProcessDefinitionId() :
                builder.getProcessDefinitionId()));
        }

        Process process = getProcess(newProcessDefinition.getId(), commandContext);

        List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class, false);
        for (StartEvent startEvent : startEvents) {
            // looking for a start event based on an event-registry event subscription
            List<ExtensionElement> eventTypeElements = startEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
            if (eventTypeElements != null && eventTypeElements.size() > 0) {
                // looking for a dynamic, manually subscribed behavior of the event-registry start event
                List<ExtensionElement> correlationConfiguration = startEvent.getExtensionElements().get(BpmnXMLConstants.START_EVENT_CORRELATION_CONFIGURATION);
                if (correlationConfiguration != null && correlationConfiguration.size() > 0 &&
                    BpmnXMLConstants.START_EVENT_CORRELATION_MANUAL.equals(correlationConfiguration.get(0).getElementText())) {

                    String eventDefinitionKey = eventTypeElements.get(0).getElementText();
                    String correlationKey = null;

                    if (builder.hasCorrelationParameterValues()) {
                        correlationKey = generateCorrelationConfiguration(eventDefinitionKey, builder.getTenantId(), 
                                builder.getCorrelationParameterValues(), commandContext);
                    }

                    getEventSubscriptionService(commandContext).updateEventSubscriptionProcessDefinitionId(builder.getProcessDefinitionId(), newProcessDefinition.getId(),
                        eventDefinitionKey, startEvent.getId(), null, correlationKey);
                }
            }
        }

        return null;
    }
}
