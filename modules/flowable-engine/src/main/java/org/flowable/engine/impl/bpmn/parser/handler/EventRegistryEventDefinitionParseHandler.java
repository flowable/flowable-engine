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
package org.flowable.engine.impl.bpmn.parser.handler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.EventRegistryEventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;

public class EventRegistryEventDefinitionParseHandler extends AbstractBpmnParseHandler<EventRegistryEventDefinition> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return EventRegistryEventDefinition.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, EventRegistryEventDefinition eventDefinition) {
        FlowElement currentFlowElement = bpmnParse.getCurrentFlowElement();
        if (currentFlowElement instanceof IntermediateCatchEvent intermediateCatchEvent) {
            String key = requireEventDefinitionKey(eventDefinition, intermediateCatchEvent.getId());
            intermediateCatchEvent.setBehavior(bpmnParse.getActivityBehaviorFactory()
                    .createIntermediateCatchEventRegistryEventActivityBehavior(intermediateCatchEvent, key));

        } else if (currentFlowElement instanceof BoundaryEvent boundaryEvent) {
            String key = requireEventDefinitionKey(eventDefinition, boundaryEvent.getId());
            boundaryEvent.setBehavior(bpmnParse.getActivityBehaviorFactory()
                    .createBoundaryEventRegistryEventActivityBehavior(boundaryEvent, key, boundaryEvent.isCancelActivity()));

        } else if (currentFlowElement instanceof StartEvent startEvent) {
            String key = requireEventDefinitionKey(eventDefinition, startEvent.getId());
            if (startEvent.getSubProcess() instanceof EventSubProcess) {
                startEvent.setBehavior(bpmnParse.getActivityBehaviorFactory()
                        .createEventSubProcessEventRegistryStartEventActivityBehavior(startEvent, key));
            } else {
                startEvent.setBehavior(bpmnParse.getActivityBehaviorFactory()
                        .createEventRegistryStartEventActivityBehavior(startEvent, key, isManualCorrelation(startEvent)));
            }
        }
    }

    protected static boolean isManualCorrelation(StartEvent startEvent) {
        List<ExtensionElement> correlationConfiguration = startEvent.getExtensionElements().get(BpmnXMLConstants.START_EVENT_CORRELATION_CONFIGURATION);
        return correlationConfiguration != null && !correlationConfiguration.isEmpty()
                && BpmnXMLConstants.START_EVENT_CORRELATION_MANUAL.equals(correlationConfiguration.get(0).getElementText());
    }

    private static String requireEventDefinitionKey(EventRegistryEventDefinition eventRegistry, String elementId) {
        String key = eventRegistry.getEventDefinitionKey();
        if (StringUtils.isEmpty(key)) {
            throw new FlowableIllegalArgumentException("EventRegistryEventDefinition on '" + elementId
                    + "' has an empty eventDefinitionKey; the engine cannot register an event-registry subscription without a key.");
        }
        return key;
    }
}
