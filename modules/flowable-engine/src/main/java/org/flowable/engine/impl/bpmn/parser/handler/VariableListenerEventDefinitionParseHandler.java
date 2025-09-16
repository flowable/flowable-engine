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
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class VariableListenerEventDefinitionParseHandler extends AbstractBpmnParseHandler<VariableListenerEventDefinition> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return VariableListenerEventDefinition.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, VariableListenerEventDefinition variableListenerEventDefinition) {

        if (bpmnParse.getCurrentFlowElement() instanceof IntermediateCatchEvent intermediateCatchEvent) {
            intermediateCatchEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createIntermediateCatchVariableListenerEventActivityBehavior(intermediateCatchEvent, variableListenerEventDefinition));

        } else if (bpmnParse.getCurrentFlowElement() instanceof BoundaryEvent boundaryEvent) {
            boundaryEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createBoundaryVariableListenerEventActivityBehavior(boundaryEvent, variableListenerEventDefinition, boundaryEvent.isCancelActivity()));
        }
    }
}
