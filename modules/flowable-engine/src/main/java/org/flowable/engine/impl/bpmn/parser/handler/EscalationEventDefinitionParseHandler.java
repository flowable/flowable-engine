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
import org.flowable.bpmn.model.Escalation;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;

/**
 * @author Tijs Rademakers
 */
public class EscalationEventDefinitionParseHandler extends AbstractBpmnParseHandler<EscalationEventDefinition> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return EscalationEventDefinition.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, EscalationEventDefinition eventDefinition) {
        if (bpmnParse.getCurrentFlowElement() instanceof BoundaryEvent boundaryEvent) {

            Escalation escalation = null;
            if (bpmnParse.getBpmnModel().containsEscalationRef(eventDefinition.getEscalationCode())) {
                escalation = bpmnParse.getBpmnModel().getEscalation(eventDefinition.getEscalationCode());
            }
            
            boundaryEvent.setBehavior(bpmnParse.getActivityBehaviorFactory().createBoundaryEscalationEventActivityBehavior(boundaryEvent, 
                                eventDefinition, escalation, boundaryEvent.isCancelActivity()));
        }
    }
}
