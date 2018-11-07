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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Transaction;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ScopeUtil;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ParallelMultiInstanceBehavior extends MultiInstanceActivityBehavior {

    private static final long serialVersionUID = 1L;

    public ParallelMultiInstanceBehavior(Activity activity, AbstractBpmnActivityBehavior originalActivityBehavior) {
        super(activity, originalActivityBehavior);
    }

    /**
     * Handles the parallel case of spawning the instances. Will create child executions accordingly for every instance needed.
     */
    @Override
    protected int createInstances(DelegateExecution multiInstanceRootExecution) {
        int nrOfInstances = resolveNrOfInstances(multiInstanceRootExecution);
        if (nrOfInstances < 0) {
            throw new FlowableIllegalArgumentException("Invalid number of instances: must be non-negative integer value" + ", but was " + nrOfInstances);
        }

        setLoopVariable(multiInstanceRootExecution, NUMBER_OF_INSTANCES, nrOfInstances);
        setLoopVariable(multiInstanceRootExecution, NUMBER_OF_COMPLETED_INSTANCES, 0);
        setLoopVariable(multiInstanceRootExecution, NUMBER_OF_ACTIVE_INSTANCES, nrOfInstances);

        List<ExecutionEntity> concurrentExecutions = new ArrayList<>();
        for (int loopCounter = 0; loopCounter < nrOfInstances; loopCounter++) {
            ExecutionEntity concurrentExecution = CommandContextUtil.getExecutionEntityManager()
                    .createChildExecution((ExecutionEntity) multiInstanceRootExecution);
            concurrentExecution.setCurrentFlowElement(activity);
            concurrentExecution.setActive(true);
            concurrentExecution.setScope(false);

            concurrentExecutions.add(concurrentExecution);
            logLoopDetails(concurrentExecution, "initialized", loopCounter, 0, nrOfInstances, nrOfInstances);
            
            //CommandContextUtil.getHistoryManager().recordActivityStart(concurrentExecution);
        }

        // Before the activities are executed, all executions MUST be created up front
        // Do not try to merge this loop with the previous one, as it will lead
        // to bugs, due to possible child execution pruning.
        for (int loopCounter = 0; loopCounter < nrOfInstances; loopCounter++) {
            ExecutionEntity concurrentExecution = concurrentExecutions.get(loopCounter);
            // executions can be inactive, if instances are all automatics
            // (no-waitstate) and completionCondition has been met in the meantime
            if (concurrentExecution.isActive() 
                    && !concurrentExecution.isEnded() 
                    && !concurrentExecution.getParent().isEnded()) {
                executeOriginalBehavior(concurrentExecution, (ExecutionEntity) multiInstanceRootExecution, loopCounter);
            } 
        }

        // See ACT-1586: ExecutionQuery returns wrong results when using multi
        // instance on a receive task The parent execution must be set to false, so it wouldn't show up in
        // the execution query when using .activityId(something). Do not we cannot nullify the
        // activityId (that would have been a better solution), as it would break boundary event behavior.
        if (!concurrentExecutions.isEmpty()) {
            multiInstanceRootExecution.setActive(false);
        }

        return nrOfInstances;
    }

    /**
     * Called when the wrapped {@link ActivityBehavior} calls the {@link AbstractBpmnActivityBehavior#leave(DelegateExecution)} method. Handles the completion of one of the parallel instances
     */
    @Override
    public void leave(DelegateExecution execution) {

        boolean zeroNrOfInstances = false;
        if (resolveNrOfInstances(execution) == 0) {
            // Empty collection, just leave.
            zeroNrOfInstances = true;
            super.leave(execution); // Plan the default leave
        }

        int loopCounter = getLoopVariable(execution, getCollectionElementIndexVariable());
        int nrOfInstances = getLoopVariable(execution, NUMBER_OF_INSTANCES);
        int nrOfCompletedInstances = getLoopVariable(execution, NUMBER_OF_COMPLETED_INSTANCES) + 1;
        int nrOfActiveInstances = getLoopVariable(execution, NUMBER_OF_ACTIVE_INSTANCES) - 1;
        
        DelegateExecution miRootExecution = getMultiInstanceRootExecution(execution);
        if (miRootExecution != null) { // will be null in case of empty collection
            setLoopVariable(miRootExecution, NUMBER_OF_COMPLETED_INSTANCES, nrOfCompletedInstances);
            setLoopVariable(miRootExecution, NUMBER_OF_ACTIVE_INSTANCES, nrOfActiveInstances);
        }

        CommandContextUtil.getHistoryManager().recordActivityEnd((ExecutionEntity) execution, null);
        callActivityEndListeners(execution);
        
        logLoopDetails(execution, "instance completed", loopCounter, nrOfCompletedInstances, nrOfActiveInstances, nrOfInstances);

        if (zeroNrOfInstances) {
            return;
        }

        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        if (executionEntity.getParent() != null) {

            executionEntity.inactivate();
            lockFirstParentScope(executionEntity);

            boolean isCompletionConditionSatisfied = completionConditionSatisfied(execution.getParent());
            if (nrOfCompletedInstances >= nrOfInstances || isCompletionConditionSatisfied) {

                ExecutionEntity leavingExecution = null;
                if (nrOfInstances > 0) {
                    leavingExecution = executionEntity.getParent();
                } else {
                    CommandContextUtil.getHistoryManager().recordActivityEnd((ExecutionEntity) execution, null);
                    leavingExecution = executionEntity;
                }

                Activity activity = (Activity) execution.getCurrentFlowElement();
                verifyCompensation(execution, leavingExecution, activity);
                verifyCallActivity(leavingExecution, activity);
                
                if (isCompletionConditionSatisfied) {
                    LinkedList<DelegateExecution> toVerify = new LinkedList<>(miRootExecution.getExecutions());
                    while (!toVerify.isEmpty()) {
                        DelegateExecution childExecution = toVerify.pop();
                        if (((ExecutionEntity) childExecution).isInserted()) {
                            childExecution.inactivate();
                        }
                        
                        List<DelegateExecution> childExecutions = (List<DelegateExecution>) childExecution.getExecutions();
                        if (childExecutions != null && !childExecutions.isEmpty()) {
                            toVerify.addAll(childExecutions);
                        }
                    }
                    sendCompletedWithConditionEvent(leavingExecution);
                }
                else {
                    sendCompletedEvent(leavingExecution);
                }

                super.leave(leavingExecution);
              }

        } else {
            sendCompletedEvent(execution);
            super.leave(execution);
        }
    }

    protected Activity verifyCompensation(DelegateExecution execution, ExecutionEntity executionToUse, Activity activity) {
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
                            if (CollectionUtil.isNotEmpty(boundaryEvent.getEventDefinitions()) &&
                                    boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition) {

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

    protected void verifyCallActivity(ExecutionEntity executionToUse, Activity activity) {
        if (activity instanceof CallActivity) {
            ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
            if (executionToUse != null) {
                List<String> callActivityExecutionIds = new ArrayList<>();

                // Find all execution entities that are at the call activity
                List<ExecutionEntity> childExecutions = executionEntityManager.collectChildren(executionToUse);
                if (childExecutions != null) {
                    for (ExecutionEntity childExecution : childExecutions) {
                        if (activity.getId().equals(childExecution.getCurrentActivityId())) {
                            callActivityExecutionIds.add(childExecution.getId());
                        }
                    }

                    // Now all call activity executions have been collected, loop again and check which should be removed
                    for (int i = childExecutions.size() - 1; i >= 0; i--) {
                        ExecutionEntity childExecution = childExecutions.get(i);
                        if (StringUtils.isNotEmpty(childExecution.getSuperExecutionId())
                                && callActivityExecutionIds.contains(childExecution.getSuperExecutionId())) {

                            executionEntityManager.deleteProcessInstanceExecutionEntity(childExecution.getId(), activity.getId(),
                                    "call activity completion condition met", true, false, true);
                        }
                    }

                }
            }
        }
    }


    protected void lockFirstParentScope(DelegateExecution execution) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();

        boolean found = false;
        ExecutionEntity parentScopeExecution = null;
        ExecutionEntity currentExecution = (ExecutionEntity) execution;
        while (!found && currentExecution != null && currentExecution.getParentId() != null) {
            parentScopeExecution = executionEntityManager.findById(currentExecution.getParentId());
            if (parentScopeExecution != null && parentScopeExecution.isScope()) {
                found = true;
            }
            currentExecution = parentScopeExecution;
        }

        parentScopeExecution.forceUpdate();
    }
}
