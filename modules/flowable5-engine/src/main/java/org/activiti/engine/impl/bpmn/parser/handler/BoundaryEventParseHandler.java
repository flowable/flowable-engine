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
package org.activiti.engine.impl.bpmn.parser.handler;

import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class BoundaryEventParseHandler extends AbstractFlowNodeBpmnParseHandler<BoundaryEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoundaryEventParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return BoundaryEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, BoundaryEvent boundaryEvent) {

        ActivityImpl parentActivity = findActivity(bpmnParse, boundaryEvent.getAttachedToRefId());
        if (parentActivity == null) {
            LOGGER.warn("Invalid reference in boundary event. Make sure that the referenced activity is defined in the same scope as the boundary event {}", boundaryEvent.getId());
            return;
        }

        ActivityImpl nestedActivity = createActivityOnScope(bpmnParse, boundaryEvent, BpmnXMLConstants.ELEMENT_EVENT_BOUNDARY, parentActivity);
        bpmnParse.setCurrentActivity(nestedActivity);

        EventDefinition eventDefinition = null;
        if (!boundaryEvent.getEventDefinitions().isEmpty()) {
            eventDefinition = boundaryEvent.getEventDefinitions().get(0);
        }

        if (eventDefinition instanceof TimerEventDefinition
                || eventDefinition instanceof org.flowable.bpmn.model.ErrorEventDefinition
                || eventDefinition instanceof SignalEventDefinition
                || eventDefinition instanceof CancelEventDefinition
                || eventDefinition instanceof MessageEventDefinition
                || eventDefinition instanceof org.flowable.bpmn.model.CompensateEventDefinition) {

            bpmnParse.getBpmnParserHandlers().parseElement(bpmnParse, eventDefinition);

        } else {
            LOGGER.warn("Unsupported boundary event type for boundary event {}", boundaryEvent.getId());
        }
    }

}
