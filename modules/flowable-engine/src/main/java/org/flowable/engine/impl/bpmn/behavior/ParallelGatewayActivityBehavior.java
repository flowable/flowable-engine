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

package org.flowable.engine.impl.bpmn.behavior;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Parallel Gateway/AND gateway as defined in the BPMN 2.0 specification.
 * 
 * The Parallel Gateway can be used for splitting a path of execution into multiple paths of executions (AND-split/fork behavior), one for every outgoing sequence flow.
 * 
 * The Parallel Gateway can also be used for merging or joining paths of execution (AND-join). In this case, on every incoming sequence flow an execution needs to arrive, before leaving the Parallel
 * Gateway (and potentially then doing the fork behavior in case of multiple outgoing sequence flow).
 * 
 * Note that there is a slight difference to spec (p. 436): "The parallel gateway is activated if there is at least one Token on each incoming sequence flow." We only check the number of incoming
 * tokens to the number of sequenceflow. So if two tokens would arrive through the same sequence flow, our implementation would activate the gateway.
 * 
 * Note that a Parallel Gateway having one incoming and multiple outgoing sequence flow, is the same as having multiple outgoing sequence flow on a given activity. However, a parallel gateway does NOT
 * check conditions on the outgoing sequence flow.
 * 
 * @author Joram Barrez
 * @author Tom Baeyens
 */
public class ParallelGatewayActivityBehavior extends GatewayActivityBehavior {

    private static final long serialVersionUID = 1840892471343975524L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelGatewayActivityBehavior.class);

    @Override
    public void execute(DelegateExecution execution) {

        // First off all, deactivate the execution
        execution.inactivate();

        // Join
        FlowElement flowElement = execution.getCurrentFlowElement();
        ParallelGateway parallelGateway = null;
        if (flowElement instanceof ParallelGateway) {
            parallelGateway = (ParallelGateway) flowElement;
        } else {
            throw new FlowableException("Programmatic error: parallel gateway behaviour can only be applied" + " to a ParallelGateway instance, but got an instance of " + flowElement);
        }

        lockFirstParentScope(execution);

        DelegateExecution multiInstanceExecution = null;
        if (hasMultiInstanceParent(parallelGateway)) {
            multiInstanceExecution = findMultiInstanceParentExecution(execution);
        }

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        Collection<ExecutionEntity> joinedExecutions = executionEntityManager.findInactiveExecutionsByActivityIdAndProcessInstanceId(execution.getCurrentActivityId(), execution.getProcessInstanceId());
        if (multiInstanceExecution != null) {
            joinedExecutions = cleanJoinedExecutions(joinedExecutions, multiInstanceExecution);
        }

        int nbrOfExecutionsToJoin = parallelGateway.getIncomingFlows().size();
        int nbrOfExecutionsCurrentlyJoined = joinedExecutions.size();

        // Fork

        // Is needed to set the endTime for all historic activity joins
        CommandContextUtil.getHistoryManager().recordActivityEnd((ExecutionEntity) execution, null);

        if (nbrOfExecutionsCurrentlyJoined == nbrOfExecutionsToJoin) {

            // Fork
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("parallel gateway '{}' activates: {} of {} joined", execution.getCurrentActivityId(), nbrOfExecutionsCurrentlyJoined, nbrOfExecutionsToJoin);
            }

            if (parallelGateway.getIncomingFlows().size() > 1) {

                // All (now inactive) children are deleted.
                for (ExecutionEntity joinedExecution : joinedExecutions) {

                    // The current execution will be reused and not deleted
                    if (!joinedExecution.getId().equals(execution.getId())) {
                        executionEntityManager.deleteRelatedDataForExecution(joinedExecution, null);
                        executionEntityManager.delete(joinedExecution);
                    }

                }
            }

            // TODO: potential optimization here: reuse more then 1 execution, only 1 currently
            CommandContextUtil.getAgenda().planTakeOutgoingSequenceFlowsOperation((ExecutionEntity) execution, false); // false -> ignoring conditions on parallel gw

        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("parallel gateway '{}' does not activate: {} of {} joined", execution.getCurrentActivityId(), nbrOfExecutionsCurrentlyJoined, nbrOfExecutionsToJoin);
        }

    }

    protected Collection<ExecutionEntity> cleanJoinedExecutions(Collection<ExecutionEntity> joinedExecutions, DelegateExecution multiInstanceExecution) {
        List<ExecutionEntity> cleanedExecutions = new ArrayList<>();
        for (ExecutionEntity executionEntity : joinedExecutions) {
            if (isChildOfMultiInstanceExecution(executionEntity, multiInstanceExecution)) {
                cleanedExecutions.add(executionEntity);
            }
        }
        return cleanedExecutions;
    }

    protected boolean isChildOfMultiInstanceExecution(DelegateExecution executionEntity, DelegateExecution multiInstanceExecution) {
        boolean isChild = false;
        DelegateExecution parentExecution = executionEntity.getParent();
        if (parentExecution != null) {
            if (parentExecution.getId().equals(multiInstanceExecution.getId())) {
                isChild = true;
            } else {
                boolean isNestedChild = isChildOfMultiInstanceExecution(parentExecution, multiInstanceExecution);
                if (isNestedChild) {
                    isChild = true;
                }
            }
        }

        return isChild;
    }

    protected boolean hasMultiInstanceParent(FlowNode flowNode) {
        boolean hasMultiInstanceParent = false;
        if (flowNode.getSubProcess() != null) {
            if (flowNode.getSubProcess().getLoopCharacteristics() != null) {
                hasMultiInstanceParent = true;
            } else {
                boolean hasNestedMultiInstanceParent = hasMultiInstanceParent(flowNode.getSubProcess());
                if (hasNestedMultiInstanceParent) {
                    hasMultiInstanceParent = true;
                }
            }
        }

        return hasMultiInstanceParent;
    }

    protected DelegateExecution findMultiInstanceParentExecution(DelegateExecution execution) {
        DelegateExecution multiInstanceExecution = null;
        DelegateExecution parentExecution = execution.getParent();
        if (parentExecution != null && parentExecution.getCurrentFlowElement() != null) {
            FlowElement flowElement = parentExecution.getCurrentFlowElement();
            if (flowElement instanceof Activity) {
                Activity activity = (Activity) flowElement;
                if (activity.getLoopCharacteristics() != null) {
                    multiInstanceExecution = parentExecution;
                }
            }

            if (multiInstanceExecution == null) {
                DelegateExecution potentialMultiInstanceExecution = findMultiInstanceParentExecution(parentExecution);
                if (potentialMultiInstanceExecution != null) {
                    multiInstanceExecution = potentialMultiInstanceExecution;
                }
            }
        }

        return multiInstanceExecution;
    }

}
