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
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventDefinitionLocation;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class EndEventParseHandler extends AbstractActivityBpmnParseHandler<EndEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndEventParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return EndEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, EndEvent endEvent) {
        if (endEvent.getEventDefinitions().isEmpty()) {
            endEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createNoneEndEventActivityBehavior(endEvent));
            return;
        }

        EventDefinition eventDefinition = endEvent.getEventDefinitions().get(0);
        if (!eventDefinition.getSupportedLocations().contains(EventDefinitionLocation.END_EVENT)) {
            LOGGER.warn("EventDefinition {} is not supported on end event {}; falling back to none-end behavior",
                    eventDefinition.getClass().getSimpleName(), endEvent.getId());
            endEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createNoneEndEventActivityBehavior(endEvent));
            return;
        }

        bpmnParse.getBpmnParserHandlers().parseElement(bpmnParse, eventDefinition);
    }
}
