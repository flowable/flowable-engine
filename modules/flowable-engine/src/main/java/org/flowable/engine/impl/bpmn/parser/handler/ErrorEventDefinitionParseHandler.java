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
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ErrorEventDefinitionParseHandler extends AbstractBpmnParseHandler<ErrorEventDefinition> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorEventDefinitionParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return ErrorEventDefinition.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, ErrorEventDefinition eventDefinition) {
        if (bpmnParse.getCurrentFlowElement() instanceof BoundaryEvent boundaryEvent) {
            boundaryEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createBoundaryEventActivityBehavior(boundaryEvent, true));

        } else if (bpmnParse.getCurrentFlowElement() instanceof EndEvent endEvent) {
            if (bpmnParse.getBpmnModel().containsErrorRef(eventDefinition.getErrorCode())) {
                String errorCode = bpmnParse.getBpmnModel().getErrors().get(eventDefinition.getErrorCode());
                if (StringUtils.isEmpty(errorCode)) {
                    LOGGER.warn("errorCode is required for an error event {}", endEvent.getId());
                }
            }
            endEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createErrorEndEventActivityBehavior(endEvent, eventDefinition));

        } else if (bpmnParse.getCurrentFlowElement() instanceof StartEvent startEvent
                && startEvent.getSubProcess() instanceof EventSubProcess) {
            startEvent.setBehavior(bpmnParse.getActivityBehaviorFactory()
                    .createEventSubProcessErrorStartEventActivityBehavior(startEvent));
        }
    }
}
