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

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventDefinitionLocation;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class BoundaryEventParseHandler extends AbstractFlowNodeBpmnParseHandler<BoundaryEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoundaryEventParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return BoundaryEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, BoundaryEvent boundaryEvent) {

        if (boundaryEvent.getAttachedToRef() == null) {
            LOGGER.warn("Invalid reference in boundary event. Make sure that the referenced activity is defined in the same scope as the boundary event {}", boundaryEvent.getId());
            return;
        }

        if (boundaryEvent.getEventDefinitions().isEmpty()) {
            // Should already be picked up by process validator on deploy, so this is just to be sure
            LOGGER.warn("Unsupported boundary event type for boundary event {}", boundaryEvent.getId());
            return;
        }

        EventDefinition eventDefinition = boundaryEvent.getEventDefinitions().get(0);
        if (!eventDefinition.getSupportedLocations().contains(EventDefinitionLocation.BOUNDARY_EVENT)) {
            LOGGER.warn("EventDefinition {} is not supported on boundary event {}",
                    eventDefinition.getClass().getSimpleName(), boundaryEvent.getId());
            return;
        }

        bpmnParse.getBpmnParserHandlers().parseElement(bpmnParse, eventDefinition);
    }

}
