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
package org.flowable.engine.impl.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

public class ExecutionGraphUtil {

    /**
     * Verifies if the element with the given source identifier can reach the element with the target identifier through following sequence flow.
     */
    public static boolean isReachable(String processDefinitionId, String sourceElementId, String targetElementId) {

        // Fetch source and target elements
        Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);

        FlowElement sourceFlowElement = process.getFlowElement(sourceElementId, true);
        FlowNode sourceElement = null;
        if (sourceFlowElement instanceof FlowNode) {
            sourceElement = (FlowNode) sourceFlowElement;
        } else if (sourceFlowElement instanceof SequenceFlow) {
            sourceElement = (FlowNode) ((SequenceFlow) sourceFlowElement).getTargetFlowElement();
        }

        FlowElement targetFlowElement = process.getFlowElement(targetElementId, true);
        FlowNode targetElement = null;
        if (targetFlowElement instanceof FlowNode) {
            targetElement = (FlowNode) targetFlowElement;
        } else if (targetFlowElement instanceof SequenceFlow) {
            targetElement = (FlowNode) ((SequenceFlow) targetFlowElement).getTargetFlowElement();
        }

        if (sourceElement == null) {
            throw new FlowableException("Invalid sourceElementId '" + sourceElementId + "': no element found for this id n process definition '" + processDefinitionId + "'");
        }
        if (targetElement == null) {
            throw new FlowableException("Invalid targetElementId '" + targetElementId + "': no element found for this id n process definition '" + processDefinitionId + "'");
        }

        Set<String> visitedElements = new HashSet<>();
        return isReachable(process, sourceElement, targetElement, visitedElements);
    }

    public static boolean isReachable(Process process, FlowNode sourceElement, FlowNode targetElement, Set<String> visitedElements) {
        
        // Special case: start events in an event subprocess might exist as an execution and are most likely be able to reach the target
        // when the target is in the event subprocess, but should be ignored as they are not 'real' runtime executions (but rather waiting for a trigger)
        if (sourceElement instanceof StartEvent && isInEventSubprocess(sourceElement)) {
            return false;
        }

        // No outgoing seq flow: could be the end of eg . the process or an embedded subprocess
        if (sourceElement.getOutgoingFlows().size() == 0) {
            visitedElements.add(sourceElement.getId());

            FlowElementsContainer parentElement = process.findParent(sourceElement);
            if (parentElement instanceof SubProcess) {
                sourceElement = (SubProcess) parentElement;
            } else {
                return false;
            }
        }

        if (sourceElement.getId().equals(targetElement.getId())) {
            return true;
        }

        // To avoid infinite looping, we must capture every node we visit
        // and check before going further in the graph if we have already
        // visited the node.
        visitedElements.add(sourceElement.getId());

        List<SequenceFlow> sequenceFlows = sourceElement.getOutgoingFlows();
        if (sequenceFlows != null && sequenceFlows.size() > 0) {
            for (SequenceFlow sequenceFlow : sequenceFlows) {
                String targetRef = sequenceFlow.getTargetRef();
                FlowNode sequenceFlowTarget = (FlowNode) process.getFlowElement(targetRef, true);
                if (sequenceFlowTarget != null && !visitedElements.contains(sequenceFlowTarget.getId())) {
                    boolean reachable = isReachable(process, sequenceFlowTarget, targetElement, visitedElements);

                    if (reachable) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected static boolean isInEventSubprocess(FlowNode flowNode) {
        FlowElementsContainer flowElementsContainer = flowNode.getParentContainer();
        while (flowElementsContainer != null) {
            if (flowElementsContainer instanceof EventSubProcess) {
                return true;
            }
            
            if (flowElementsContainer instanceof FlowElement) {
                flowElementsContainer = ((FlowElement) flowElementsContainer).getParentContainer();
            } else {
                flowElementsContainer = null;
            }
        }
        return false;
    }

    public static DelegateExecution getMultiInstanceRootExecution(ExecutionEntity execution) {
        ExecutionEntity multiInstanceRootExecution = null;
        ExecutionEntity currentExecution = execution;
        while (currentExecution != null && multiInstanceRootExecution == null && currentExecution.getParentId() != null) {
            if (currentExecution.isMultiInstanceRoot()) {
                multiInstanceRootExecution = currentExecution;
            } else {
                currentExecution = currentExecution.getParent();
            }
        }
        return multiInstanceRootExecution;
    }

    public static DelegateExecution getParentInstanceExecutionInMultiInstance(ExecutionEntity execution) {
        ExecutionEntity instanceExecution = null;
        ExecutionEntity currentExecution = execution;
        while (currentExecution != null && instanceExecution == null && currentExecution.getParentId() != null) {
            if (currentExecution.getParent().isMultiInstanceRoot()) {
                instanceExecution = currentExecution;
            } else {
                currentExecution = currentExecution.getParent();
            }
        }
        return instanceExecution;
    }

    /**
     * Returns the list of boundary event activity ids that are in the the process model,
     * associated with the current activity of the passed execution.
     * Note that no check if made here whether this an active child execution for those boundary events.
     */
    public static List<String> getBoundaryEventActivityIds(DelegateExecution execution) {
        Process process = ProcessDefinitionUtil.getProcess(execution.getProcessDefinitionId());

        String activityId = execution.getCurrentActivityId();
        if (StringUtils.isNotEmpty(activityId)) {
            List<String> boundaryEventActivityIds = new ArrayList<>();
            List<BoundaryEvent> boundaryEvents = process.findFlowElementsOfType(BoundaryEvent.class, true);
            for (BoundaryEvent boundaryEvent : boundaryEvents) {
                if (activityId.equals(boundaryEvent.getAttachedToRefId())) {
                    boundaryEventActivityIds.add(boundaryEvent.getId());
                }
            }
            return boundaryEventActivityIds;

        } else {
            return Collections.emptyList();

        }
    }

}
