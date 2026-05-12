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
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventDefinitionLocation;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class StartEventParseHandler extends AbstractActivityBpmnParseHandler<StartEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartEventParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return StartEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, StartEvent element) {
        if (element.getSubProcess() instanceof EventSubProcess) {
            if (CollectionUtil.isNotEmpty(element.getEventDefinitions())) {
                EventDefinition eventDefinition = element.getEventDefinitions().get(0);
                if (!eventDefinition.getSupportedLocations().contains(EventDefinitionLocation.EVENT_SUBPROCESS_START_EVENT)) {
                    LOGGER.warn("EventDefinition {} is not supported on event sub-process start event {}",
                            eventDefinition.getClass().getSimpleName(), element.getId());
                    return;
                }
                bpmnParse.getBpmnParserHandlers().parseElement(bpmnParse, eventDefinition);
            }

        } else if (CollectionUtil.isEmpty(element.getEventDefinitions())) {
            element.setBehavior(bpmnParse.getActivityBehaviorFactory().createNoneStartEventActivityBehavior(element));
        
        } else {
            EventDefinition eventDefinition = element.getEventDefinitions().get(0);
            if (!eventDefinition.getSupportedLocations().contains(EventDefinitionLocation.START_EVENT)) {
                LOGGER.warn("EventDefinition {} is not supported on process-level start event {}",
                        eventDefinition.getClass().getSimpleName(), element.getId());
            } else {
                bpmnParse.getBpmnParserHandlers().parseElement(bpmnParse, eventDefinition);
            }
        }

        if (element.getSubProcess() == null && (CollectionUtil.isEmpty(element.getEventDefinitions()) ||
                bpmnParse.getCurrentProcess().getInitialFlowElement() == null)) {
            
            bpmnParse.getCurrentProcess().setInitialFlowElement(element);
        }
    }
}
