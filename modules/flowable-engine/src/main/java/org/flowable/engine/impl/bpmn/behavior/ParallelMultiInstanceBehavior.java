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
import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
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
     * Handles the parallel case of spawning the instances. Will create child
     * executions accordingly for every instance needed.
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
        }

        // Before the activities are executed, all executions MUST be created up
        // front
        // Do not try to merge this loop with the previous one, as it will lead
        // to bugs, due to possible child execution pruning.
        for (int loopCounter = 0; loopCounter < nrOfInstances; loopCounter++) {
            ExecutionEntity concurrentExecution = concurrentExecutions.get(loopCounter);
            // executions can be inactive, if instances are all automatics
            // (no-waitstate) and completionCondition has been met in the
            // meantime
            if (concurrentExecution.isActive() && !concurrentExecution.isEnded() && !concurrentExecution.getParent().isEnded()) {
                executeOriginalBehavior(concurrentExecution, (ExecutionEntity) multiInstanceRootExecution, loopCounter);
            }
        }

        // See ACT-1586: ExecutionQuery returns wrong results when using multi
        // instance on a receive task The parent execution must be set to false,
        // so it wouldn't show up in
        // the execution query when using .activityId(something). Do not we
        // cannot nullify the
        // activityId (that would have been a better solution), as it would
        // break boundary event behavior.
        if (!concurrentExecutions.isEmpty()) {
            multiInstanceRootExecution.setActive(false);
        }

        scheduleMonitor(multiInstanceRootExecution, concurrentExecutions);

        return nrOfInstances;
    }

    private void scheduleMonitor(DelegateExecution multiInstanceRootExecution, List<ExecutionEntity> childConcurrentExecutions) {
        CommandContextUtil.getAgenda().planMonitorParallelMultiInstanceOperation((ExecutionEntity) multiInstanceRootExecution, childConcurrentExecutions);
    }

    /**
     * Called when the wrapped {@link ActivityBehavior} calls the
     * {@link AbstractBpmnActivityBehavior#leave(DelegateExecution)} method.
     * Handles the completion of one of the parallel instances
     */
    @Override
    public void leave(DelegateExecution execution) {
        boolean zeroNrOfInstances = false;
        if (resolveNrOfInstances(execution) == 0) {
            // Empty collection, just leave.
            zeroNrOfInstances = true;
            super.leave(execution); // Plan the default leave
        }

        DelegateExecution miRootExecution = getMultiInstanceRootExecution(execution);
        if (miRootExecution != null) { // will be null in case of empty
                                       // collection
            setLoopVariable(execution, NUMBER_OF_COMPLETED_INSTANCES, 1);
        }
        CommandContextUtil.getHistoryManager().recordActivityEnd((ExecutionEntity) execution, null);
        callActivityEndListeners(execution);

        if (zeroNrOfInstances) {
            return;
        }

        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        if (executionEntity.getParent() != null) {
            if (!execution.isActive()) {
                return;
            }
            executionEntity.inactivate();
        } else {
            sendCompletedEvent(execution);
            super.leave(execution);
        }
    }

}
