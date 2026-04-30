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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.EventRegistryEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventRegistryEventDefinitionParseHandler extends AbstractBpmnParseHandler<EventRegistryEventDefinition> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryEventDefinitionParseHandler.class);

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
            // StartEventParseHandler dispatches EventRegistryEventDefinition inline (event-sub-process branch
            // creates the typed behavior; process-level branch is driven by the engine's process-start path
            // via subscription, not by behavior.execute()). Reaching this handler for a StartEvent only
            // happens if a custom dispatch path explicitly delegates here — warn so the misconfiguration
            // surfaces.
            LOGGER.warn("EventRegistryEventDefinition on StartEvent '{}' is dispatched by StartEventParseHandler; reaching this handler is unexpected. Ignoring.",
                    startEvent.getId());
        } else {
            LOGGER.warn("EventRegistryEventDefinition is only supported on IntermediateCatchEvent, BoundaryEvent, and event-sub-process StartEvent. " +
                    "Found on '{}' (type {}); ignoring",
                    currentFlowElement != null ? currentFlowElement.getId() : "unknown",
                    currentFlowElement != null ? currentFlowElement.getClass().getSimpleName() : "null");
        }
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
