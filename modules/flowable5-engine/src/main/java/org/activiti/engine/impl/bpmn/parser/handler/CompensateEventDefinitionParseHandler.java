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
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.ThrowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CompensateEventDefinitionParseHandler extends AbstractBpmnParseHandler<CompensateEventDefinition> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompensateEventDefinitionParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return CompensateEventDefinition.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, CompensateEventDefinition eventDefinition) {

        ScopeImpl scope = bpmnParse.getCurrentScope();
        if (StringUtils.isNotEmpty(eventDefinition.getActivityRef())) {
            if (scope.findActivity(eventDefinition.getActivityRef()) == null) {
                LOGGER.warn("Invalid attribute value for 'activityRef': no activity with id '{}' in current scope {}", eventDefinition.getActivityRef(), scope.getId());
            }
        }

        org.activiti.engine.impl.bpmn.parser.CompensateEventDefinition compensateEventDefinition = new org.activiti.engine.impl.bpmn.parser.CompensateEventDefinition();
        compensateEventDefinition.setActivityRef(eventDefinition.getActivityRef());
        compensateEventDefinition.setWaitForCompletion(eventDefinition.isWaitForCompletion());

        ActivityImpl activity = bpmnParse.getCurrentActivity();
        if (bpmnParse.getCurrentFlowElement() instanceof ThrowEvent) {

            activity.setActivityBehavior(bpmnParse.getActivityBehaviorFactory().createIntermediateThrowCompensationEventActivityBehavior((ThrowEvent) bpmnParse.getCurrentFlowElement(), compensateEventDefinition));

        } else if (bpmnParse.getCurrentFlowElement() instanceof BoundaryEvent) {

            BoundaryEvent boundaryEvent = (BoundaryEvent) bpmnParse.getCurrentFlowElement();
            boolean interrupting = boundaryEvent.isCancelActivity();

            activity.setActivityBehavior(bpmnParse.getActivityBehaviorFactory().createBoundaryEventActivityBehavior(boundaryEvent, interrupting, activity));
            activity.setProperty("type", "compensationBoundaryCatch");

        } else {

            // What to do?

        }

    }

}
