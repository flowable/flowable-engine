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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.HistoricActivityInstanceQueryProperty;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityImpl;
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
 * Note that a Parallel Gateway having one incoming and multiple outgoing sequence flow, is the same as having multiple outgoing sequence flow on a given activity. However, a parallel gateway does NOT
 * check conditions on the outgoing sequence flow.
 *
 * @author Joram Barrez
 * @author Tom Baeyens
 */
public class ParallelGatewayActivityBehavior extends GatewayActivityBehavior {

    private static final long serialVersionUID = 1840892471343975524L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelGatewayActivityBehavior.class);

    private HistoryService historyService = Context.getProcessEngineConfiguration().getHistoryService();

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
            throw new FlowableException(
                    "Programmatic error: parallel gateway behaviour can only be applied to a ParallelGateway instance, but got an instance of " + flowElement
                            + " for " + execution);
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

        Set<String> unmatchedIncomingFlowIds = new HashSet<>();

        // Add all incoming flows to the unmatchedIncomingFlowIds Set
        for (SequenceFlow incomingFlow : parallelGateway.getIncomingFlows()) {
            if (incomingFlow.getId() != null) {
                unmatchedIncomingFlowIds.add(incomingFlow.getId());
            } else {
                // If incoming flow has no id, then we build the string used as the flowable default id
                // See getArtificialSequenceFlowId() in org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityManagerImpl
                unmatchedIncomingFlowIds.add(new StringBuilder("_flow_").append(incomingFlow.getSourceRef()).append("__").append(incomingFlow.getTargetRef()).toString());
            }
        }

        // Collect HistoricActivity cache because historyService is not flushed between the sequenceFlow completing and reaching parallelGateway
        // Sort output so that the most recent activity is first in the list
        List<HistoricActivityInstance> historicActivitiesCache = CommandContextUtil.getEntityCache()
                .findInCache(HistoricActivityInstanceEntityImpl.class)
                .stream().map(historicActivity -> (HistoricActivityInstance) historicActivity)
                .sorted(Comparator.comparing(HistoricActivityInstance::getEndTime, Comparator.nullsFirst(Comparator.reverseOrder()))).toList();

        // For each execution, get the most recent sequenceflow activity and remove it from the Set of incomingFlow ids
        // If the Set becomes empty, then we know that we have all the incoming flows fulfilled in our joinedExecutions
        // Ignore the execution unless the most recent activity is the join gateway, and the previous activity was the relevant sequenceFlow
        joinedExecutions:
        for (ExecutionEntity joinedExecution : joinedExecutions) {
            List<HistoricActivityInstance> latestActivities = new ArrayList<>();

            // Add relevant latest activities (i.e. 2 most recent activities) to the latestActivities List & sort using most recent activity first
            latestActivities.addAll(historyService
                    .createHistoricActivityInstanceQuery()
                    .executionId(joinedExecution.getId())
                    .orderBy(HistoricActivityInstanceQueryProperty.END).desc()
                    .listPage(0, 2));

            // Add any relevant cached entities to the start of the list
            latestActivities.addAll(0, historicActivitiesCache.stream().filter(historicActivity -> Objects.equals(historicActivity.getExecutionId(), joinedExecution.getId())).toList());

            if (latestActivities.isEmpty()) {
                continue;
            }

            // Walk back through the latest activities for this execution & check if they include a sequenceFlow that leads to this gateway
            latestActivities:
            for (int i = 0; i < latestActivities.size(); i++) {
                HistoricActivityInstance latestActivity = latestActivities.get(i);

                // Latest activity is parallelGateway, but not this parallelGateway - i.e. its not the execution we are looking for
                if (Objects.equals(latestActivity.getActivityType(), "parallelGateway") && !Objects.equals(latestActivity.getActivityId(), parallelGateway.getId())) {
                    continue joinedExecutions;
                }
                // Latest activity is parallelGateway && id matches. Its the execution we are looking for. Our sequenceFlow should be the next activity
                if (Objects.equals(latestActivity.getActivityType(), "parallelGateway") && Objects.equals(latestActivity.getActivityId(), parallelGateway.getId())) {
                    continue;
                }
                // If we for some reason get to here and the activity is not a sequence flow, then we are in the wrong execution
                if (!Objects.equals(latestActivity.getActivityType(), "sequenceFlow")) {
                    continue joinedExecutions;
                }

                unmatchedIncomingFlowIds.remove(latestActivity.getActivityId());

                // Once the unmatchedIncomingFlowIds Set is empty, then break and avoid running unneeded historyService queries
                if (unmatchedIncomingFlowIds.isEmpty()) {
                    break joinedExecutions;
                }

                // if we get to here, then we have already completed analysing this execution. Continue to the next execution
                continue joinedExecutions;
            }
        }

        // Fork

        // Is needed to set the endTime for all historic activity joins
        CommandContextUtil.getActivityInstanceEntityManager().recordActivityEnd((ExecutionEntity) execution, null);

        // if all incoming flows were executed, then the gateway can continue
        if (unmatchedIncomingFlowIds.isEmpty()) {

            // Fork
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("parallel gateway '{}' ({}) activates: {} of {} joined", execution.getCurrentActivityId(),
                        execution.getId(), nbrOfExecutionsToJoin, nbrOfExecutionsToJoin);
            }

            if (parallelGateway.getIncomingFlows().size() > 1) {

                // All (now inactive) children are deleted.
                for (ExecutionEntity joinedExecution : joinedExecutions) {

                    // The current execution will be reused and not deleted
                    if (!joinedExecution.getId().equals(execution.getId())) {
                        executionEntityManager.deleteRelatedDataForExecution(joinedExecution, null, false);
                        executionEntityManager.delete(joinedExecution);
                    }

                }
            }

            // TODO: potential optimization here: reuse more then 1 execution, only 1 currently
            CommandContextUtil.getAgenda().planTakeOutgoingSequenceFlowsOperation((ExecutionEntity) execution, false); // false -> ignoring conditions on parallel gw

        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("parallel gateway '{}' ({}) does not activate: {} of {} incoming flows were not completed", execution.getCurrentActivityId(),
                    execution.getId(), unmatchedIncomingFlowIds.size(), nbrOfExecutionsToJoin);
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
            if (flowElement instanceof Activity activity) {
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
