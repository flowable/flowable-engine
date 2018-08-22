package org.flowable.engine.impl.bpmn.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Transaction;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCompletedEvent;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiInstanceRootCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiInstanceRootCleaner.class);

    private boolean isCompletionConditionSatisfied;

    public MultiInstanceRootCleaner(boolean isCompletionConditionSatisfied) {
        this.isCompletionConditionSatisfied = isCompletionConditionSatisfied;
    }

    public void finishMultiInstanceRootExecution(ExecutionEntity execution) {

        CommandContextUtil.getHistoryManager()
            .recordActivityEnd((ExecutionEntity) execution, null);

        Activity activity = (Activity) execution.getCurrentFlowElement();
        verifyCompensation(execution, execution, activity);
        verifyCallActivity(execution, execution.getActivityId());
        if (isCompletionConditionSatisfied) {
            LinkedList<DelegateExecution> toVerify = new LinkedList<>(execution.getExecutions());
            while (!toVerify.isEmpty()) {
                DelegateExecution childExecution = toVerify.pop();
                if (((ExecutionEntity) childExecution).isInserted()) {
                    childExecution.inactivate();
                }

                @SuppressWarnings("unchecked")
                List<DelegateExecution> childExecutions = (List<DelegateExecution>) childExecution.getExecutions();
                if (childExecutions != null && !childExecutions.isEmpty()) {
                    toVerify.addAll(childExecutions);
                }
            }
            sendCompletedWithConditionEvent(execution);
        } else {
            sendCompletedEvent(execution);
        }
        cleanupMiRoot(execution);
    }

    private Activity verifyCompensation(DelegateExecution execution, ExecutionEntity executionToUse, Activity activity) {
        boolean hasCompensation = false;
        if (activity instanceof Transaction) {
            hasCompensation = true;
        } else if (activity instanceof SubProcess) {
            SubProcess subProcess = (SubProcess) activity;
            for (FlowElement subElement : subProcess.getFlowElements()) {
                if (subElement instanceof Activity) {
                    Activity subActivity = (Activity) subElement;
                    if (CollectionUtil.isNotEmpty(subActivity.getBoundaryEvents())) {
                        for (BoundaryEvent boundaryEvent : subActivity.getBoundaryEvents()) {
                            if (CollectionUtil.isNotEmpty(boundaryEvent.getEventDefinitions()) && boundaryEvent.getEventDefinitions()
                                .get(0) instanceof CompensateEventDefinition) {

                                hasCompensation = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (hasCompensation) {
            ScopeUtil.createCopyOfSubProcessExecutionForCompensation(executionToUse);
        }
        return activity;
    }

    private void verifyCallActivity(ExecutionEntity execution, String activityId) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        if (execution != null) {
            List<String> callActivityExecutionIds = new ArrayList<>();

            // Find all execution entities that are at the call activity
            List<ExecutionEntity> childExecutions = executionEntityManager.collectChildren(execution);
            if (childExecutions != null) {
                for (ExecutionEntity childExecution : childExecutions) {
                    // LOGGER.info("Checking child execution {}", childExecution.getPersistentState());
                    if (activityId.equals(childExecution.getCurrentActivityId())) {
                        callActivityExecutionIds.add(childExecution.getId());
                    }
                }

                // Now all call activity executions have been collected, loop again and check which should be removed
                for (int i = childExecutions.size() - 1; i >= 0; i--) {
                    ExecutionEntity childExecution = childExecutions.get(i);
                    if (StringUtils.isNotEmpty(childExecution.getSuperExecutionId())
                        && callActivityExecutionIds.contains(childExecution.getSuperExecutionId())) {
                        if (!childExecution.isDeleted()) {
                            // LOGGER.info("deleted {}", childExecution);
                            executionEntityManager.deleteProcessInstanceExecutionEntity(childExecution.getId(), activityId,
                                "call activity completion condition met", true, false, true);
                        } else {
                            LOGGER.debug("The child execution {} is already deleted", childExecution.getId());
                        }
                    }
                }

            }
        }
    }

    protected void sendCompletedWithConditionEvent(DelegateExecution execution) {
        CommandContextUtil.getEventDispatcher(CommandContextUtil.getCommandContext())
            .dispatchEvent(buildCompletedEvent(execution, FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED_WITH_CONDITION));
    }

    protected FlowableMultiInstanceActivityCompletedEvent buildCompletedEvent(DelegateExecution execution,
        FlowableEngineEventType eventType) {
        FlowElement flowNode = execution.getCurrentFlowElement();

        // verify the variables
        return FlowableEventBuilder.createMultiInstanceActivityCompletedEvent(eventType, (int) execution.getVariable("nrOfInstances"),
            (int) execution.getVariable("nrOfActiveInstances"), (int) execution.getVariable("nrOfCompletedInstances"), flowNode.getId(),
            flowNode.getName(), execution.getId(), execution.getProcessInstanceId(), execution.getProcessDefinitionId(), flowNode);
    }

    private void sendCompletedEvent(ExecutionEntity execution) {
        CommandContextUtil.getEventDispatcher(CommandContextUtil.getCommandContext())
            .dispatchEvent(buildCompletedEvent(execution, FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED));
    }

    protected void cleanupMiRoot(DelegateExecution execution) {
        // Delete multi instance root and all child executions.
        // Create a fresh execution to continue
        ExecutionEntity multiInstanceRootExecution = (ExecutionEntity) getMultiInstanceRootExecution(execution);
        FlowElement flowElement = multiInstanceRootExecution.getCurrentFlowElement();
        ExecutionEntity parentExecution = multiInstanceRootExecution.getParent();

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        Collection<String> executionIdsNotToSendCancelledEventsFor = execution.isMultiInstanceRoot() ? null
            : Collections.singletonList(execution.getId());
        executionEntityManager.deleteChildExecutions(multiInstanceRootExecution, null, executionIdsNotToSendCancelledEventsFor, "MI_END",
            true, flowElement);
        executionEntityManager.deleteRelatedDataForExecution(multiInstanceRootExecution, "MI_END");
        executionEntityManager.delete(multiInstanceRootExecution);

        ExecutionEntity newExecution = executionEntityManager.createChildExecution(parentExecution);
        newExecution.setCurrentFlowElement(flowElement);
        takeNextTransition(newExecution);
    }

    protected DelegateExecution getMultiInstanceRootExecution(DelegateExecution executionEntity) {
        DelegateExecution multiInstanceRootExecution = null;
        DelegateExecution currentExecution = executionEntity;
        while (currentExecution != null && multiInstanceRootExecution == null && currentExecution.getParent() != null) {
            if (currentExecution.isMultiInstanceRoot()) {
                multiInstanceRootExecution = currentExecution;
            } else {
                currentExecution = currentExecution.getParent();
            }
        }
        return multiInstanceRootExecution;
    }

    private void takeNextTransition(ExecutionEntity newExecution) {
        CommandContextUtil.getAgenda()
            .planTakeOutgoingSequenceFlowsOperation(newExecution, true);
    }
}
