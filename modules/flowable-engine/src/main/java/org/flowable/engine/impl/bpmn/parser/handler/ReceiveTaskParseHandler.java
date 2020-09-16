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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.ReceiveTask;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;

/**
 * @author Joram Barrez
 */
public class ReceiveTaskParseHandler extends AbstractActivityBpmnParseHandler<ReceiveTask> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return ReceiveTask.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, ReceiveTask receiveTask) {

        // Check if it's a receive task for receiving an eventregistry event
        Map<String, List<ExtensionElement>> extensionElements = receiveTask.getExtensionElements();
        if (!extensionElements.isEmpty()) {
            List<ExtensionElement> eventTypeExtensionElements = receiveTask.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
            if (eventTypeExtensionElements != null && !eventTypeExtensionElements.isEmpty()) {
                String eventTypeValue = eventTypeExtensionElements.get(0).getElementText();
                if (StringUtils.isNotEmpty(eventTypeValue)) {
                    receiveTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createReceiveEventTaskActivityBehavior(receiveTask, eventTypeValue));
                    return;
                }
            }
        }

        receiveTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createReceiveTaskActivityBehavior(receiveTask));
    }

}
