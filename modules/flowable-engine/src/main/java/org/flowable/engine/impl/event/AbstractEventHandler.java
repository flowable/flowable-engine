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

package org.flowable.engine.impl.event;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public abstract class AbstractEventHandler implements EventHandler {

    public void handleEvent(EventSubscriptionEntity eventSubscription, Object payload, CommandContext commandContext) {
        ExecutionEntity execution = eventSubscription.getExecution();
        FlowNode currentFlowElement = (FlowNode) execution.getCurrentFlowElement();

        if (currentFlowElement == null) {
            throw new FlowableException("Error while sending signal for event subscription '" + eventSubscription.getId() + "': " + "no activity associated with event subscription");
        }

        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> processVariables = (Map<String, Object>) payload;
            execution.setVariables(processVariables);
        }

        if (currentFlowElement instanceof EventSubProcess) {
            try {
                dispatchActivitiesCancelledIfNeeded(eventSubscription, execution, currentFlowElement, commandContext);

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new FlowableException("exception while sending signal for event subscription '" + eventSubscription + "':" + e.getMessage(), e);
            }
        }

        CommandContextUtil.getAgenda().planTriggerExecutionOperation(execution);
    }

    protected void dispatchActivitiesCancelledIfNeeded(EventSubscriptionEntity eventSubscription, ExecutionEntity execution, FlowElement currentFlowElement, CommandContext commandContext) {
        if (currentFlowElement instanceof BoundaryEvent) {
            BoundaryEvent boundaryEvent = (BoundaryEvent) currentFlowElement;
            if (boundaryEvent.isCancelActivity()) {
                dispatchExecutionCancelled(eventSubscription, execution, commandContext);
            }
        }
    }

    protected void dispatchExecutionCancelled(EventSubscriptionEntity eventSubscription, ExecutionEntity execution, CommandContext commandContext) {
        // subprocesses
        for (ExecutionEntity subExecution : execution.getExecutions()) {
            dispatchExecutionCancelled(eventSubscription, subExecution, commandContext);
        }

        // call activities
        ExecutionEntity subProcessInstance = CommandContextUtil.getExecutionEntityManager(commandContext).findSubProcessInstanceBySuperExecutionId(execution.getId());
        if (subProcessInstance != null) {
            dispatchExecutionCancelled(eventSubscription, subProcessInstance, commandContext);
        }
    }

    protected void dispatchActivityCancelled(EventSubscriptionEntity eventSubscription, ExecutionEntity boundaryEventExecution, FlowNode flowNode, CommandContext commandContext) {

        // Scope
        CommandContextUtil.getEventDispatcher().dispatchEvent(
                FlowableEventBuilder.createActivityCancelledEvent(flowNode.getId(), flowNode.getName(), boundaryEventExecution.getId(),
                        boundaryEventExecution.getProcessInstanceId(), boundaryEventExecution.getProcessDefinitionId(),
                        parseActivityType(flowNode), eventSubscription));

        if (flowNode instanceof SubProcess) {
            // The parent of the boundary event execution will be the one on which the boundary event is set
            ExecutionEntity parentExecutionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(boundaryEventExecution.getParentId());
            if (parentExecutionEntity != null) {
                dispatchActivityCancelledForChildExecution(eventSubscription, parentExecutionEntity, boundaryEventExecution, commandContext);
            }
        }
    }

    protected void dispatchActivityCancelledForChildExecution(EventSubscriptionEntity eventSubscription,
            ExecutionEntity parentExecutionEntity, ExecutionEntity boundaryEventExecution, CommandContext commandContext) {

        List<ExecutionEntity> executionEntities = CommandContextUtil.getExecutionEntityManager(commandContext).findChildExecutionsByParentExecutionId(parentExecutionEntity.getId());
        for (ExecutionEntity childExecution : executionEntities) {

            if (!boundaryEventExecution.getId().equals(childExecution.getId())
                    && childExecution.getCurrentFlowElement() != null
                    && childExecution.getCurrentFlowElement() instanceof FlowNode) {

                FlowNode flowNode = (FlowNode) childExecution.getCurrentFlowElement();
                CommandContextUtil.getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createActivityCancelledEvent(flowNode.getId(), flowNode.getName(), childExecution.getId(),
                                childExecution.getProcessInstanceId(), childExecution.getProcessDefinitionId(),
                                parseActivityType(flowNode), eventSubscription));

                if (childExecution.isScope()) {
                    dispatchActivityCancelledForChildExecution(eventSubscription, childExecution, boundaryEventExecution, commandContext);
                }

            }

        }

    }

    protected String parseActivityType(FlowNode flowNode) {
        String elementType = flowNode.getClass().getSimpleName();
        elementType = elementType.substring(0, 1).toLowerCase() + elementType.substring(1);
        return elementType;
    }

}
