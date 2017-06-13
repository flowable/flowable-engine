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
package org.flowable.engine.impl.agenda;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * Operation that triggers a wait state and continues the process, leaving that activity.
 * 
 * The {@link ExecutionEntity} for this operations should be in a wait state (receive task for example) and have a {@link FlowElement} that has a behaviour that implements the
 * {@link TriggerableActivityBehavior}.
 * 
 * @author Joram Barrez
 */
public class TriggerExecutionOperation extends AbstractOperation {

    public TriggerExecutionOperation(CommandContext commandContext, ExecutionEntity execution) {
        super(commandContext, execution);
    }

    @Override
    public void run() {
        FlowElement currentFlowElement = getCurrentFlowElement(execution);
        if (currentFlowElement instanceof FlowNode) {

            ActivityBehavior activityBehavior = (ActivityBehavior) ((FlowNode) currentFlowElement).getBehavior();
            if (activityBehavior instanceof TriggerableActivityBehavior) {

                if (currentFlowElement instanceof BoundaryEvent) {
                    commandContext.getHistoryManager().recordActivityStart(execution);
                }
                
                ((TriggerableActivityBehavior) activityBehavior).trigger(execution, null, null);

                if (currentFlowElement instanceof BoundaryEvent) {
                    commandContext.getHistoryManager().recordActivityEnd(execution, null);
                }

            } else {
                throw new FlowableException("Cannot trigger execution with id " + execution.getId()
                    + " : the activityBehavior " + activityBehavior.getClass() + " does not implement the " 
                    + TriggerableActivityBehavior.class.getName() + " interface");
        
            }

        } else if (currentFlowElement == null) {
            throw new FlowableException("Cannot trigger execution with id " + execution.getId()
                    + " : no current flow element found. Check the execution id that is being passed "
                    + "(it should not be a process instance execution, but a child execution currently referencing a flow element).");
            
        } else {
            throw new FlowableException("Programmatic error: cannot trigger execution, invalid flowelement type found: " 
                    + currentFlowElement.getClass().getName() + ".");
            
        }
    }

}
