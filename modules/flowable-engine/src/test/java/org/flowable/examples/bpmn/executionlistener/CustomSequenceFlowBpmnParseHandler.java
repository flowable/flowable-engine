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
package org.flowable.examples.bpmn.executionlistener;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.handler.SequenceFlowParseHandler;

/**
 * @author Tijs Rademakers
 */
public class CustomSequenceFlowBpmnParseHandler extends SequenceFlowParseHandler {

    @Override
    protected void executeParse(BpmnParse bpmnParse, SequenceFlow flow) {

        // Do the regular stuff
        super.executeParse(bpmnParse, flow);

        // Add extension element conditions
        Map<String, List<ExtensionElement>> extensionElements = flow.getExtensionElements();
        if (extensionElements.containsKey("activiti_custom_condition")) {
            List<ExtensionElement> conditionsElements = extensionElements.get("activiti_custom_condition");

            CustomSetConditionsExecutionListener customFlowListener = new CustomSetConditionsExecutionListener();
            customFlowListener.setFlowId(flow.getId());
            for (ExtensionElement conditionElement : conditionsElements) {
                customFlowListener.addCondition(conditionElement.getElementText());
            }

            FlowableListener activitiListener = new FlowableListener();
            activitiListener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_INSTANCE);
            activitiListener.setInstance(customFlowListener);
            activitiListener.setEvent("start");
            flow.getSourceFlowElement().getExecutionListeners().add(activitiListener);

        }
    }

}
