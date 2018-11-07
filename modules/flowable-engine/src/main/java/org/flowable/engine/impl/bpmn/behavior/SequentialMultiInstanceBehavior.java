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

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class SequentialMultiInstanceBehavior extends MultiInstanceActivityBehavior {

    private static final long serialVersionUID = 1L;

    public SequentialMultiInstanceBehavior(Activity activity, AbstractBpmnActivityBehavior innerActivityBehavior) {
        super(activity, innerActivityBehavior);
    }

    /**
     * Handles the sequential case of spawning the instances. Will only create one instance, since at most one instance can be active.
     */
    @Override
    protected int createInstances(DelegateExecution multiInstanceRootExecution) {

        int nrOfInstances = resolveNrOfInstances(multiInstanceRootExecution);
        if (nrOfInstances == 0) {
            return nrOfInstances;
        } else if (nrOfInstances < 0) {
            throw new FlowableIllegalArgumentException("Invalid number of instances: must be a non-negative integer value" + ", but was " + nrOfInstances);
        }

        // Create child execution that will execute the inner behavior
        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager()
                .createChildExecution((ExecutionEntity) multiInstanceRootExecution);
        execution.setCurrentFlowElement(multiInstanceRootExecution.getCurrentFlowElement());

        // Set Multi-instance variables
        setLoopVariable(multiInstanceRootExecution, NUMBER_OF_INSTANCES, nrOfInstances);
        setLoopVariable(multiInstanceRootExecution, NUMBER_OF_COMPLETED_INSTANCES, 0);
        setLoopVariable(multiInstanceRootExecution, NUMBER_OF_ACTIVE_INSTANCES, 1);
        logLoopDetails(multiInstanceRootExecution, "initialized", 0, 0, 1, nrOfInstances);

        if (nrOfInstances > 0) {
            executeOriginalBehavior(execution, (ExecutionEntity) multiInstanceRootExecution, 0);
        }

        return nrOfInstances;
    }

    /**
     * Called when the wrapped {@link ActivityBehavior} calls the {@link AbstractBpmnActivityBehavior#leave(DelegateExecution)} method. Handles the completion of one instance, and executes the logic
     * for the sequential behavior.
     */
    @Override
    public void leave(DelegateExecution execution) {
        DelegateExecution multiInstanceRootExecution = getMultiInstanceRootExecution(execution);
        int loopCounter = getLoopVariable(execution, getCollectionElementIndexVariable()) + 1;
        int nrOfInstances = getLoopVariable(multiInstanceRootExecution, NUMBER_OF_INSTANCES);
        int nrOfCompletedInstances = getLoopVariable(multiInstanceRootExecution, NUMBER_OF_COMPLETED_INSTANCES) + 1;
        int nrOfActiveInstances = getLoopVariable(multiInstanceRootExecution, NUMBER_OF_ACTIVE_INSTANCES);

        setLoopVariable(multiInstanceRootExecution, NUMBER_OF_COMPLETED_INSTANCES, nrOfCompletedInstances);
        logLoopDetails(execution, "instance completed", loopCounter, nrOfCompletedInstances, nrOfActiveInstances, nrOfInstances);

        callActivityEndListeners(execution);

        boolean completeConditionSatisfied = completionConditionSatisfied(multiInstanceRootExecution);
        if (loopCounter >= nrOfInstances || completeConditionSatisfied) {
            if(completeConditionSatisfied) {
                sendCompletedWithConditionEvent(multiInstanceRootExecution);
            }
            else {
                sendCompletedEvent(multiInstanceRootExecution);
            }

            super.leave(execution);
        } else {
            continueSequentialMultiInstance(execution, loopCounter, (ExecutionEntity) multiInstanceRootExecution);
        }
    }
    
    public void continueSequentialMultiInstance(DelegateExecution execution, int loopCounter, ExecutionEntity multiInstanceRootExecution) {
        try {
            
            if (execution.getCurrentFlowElement() instanceof SubProcess) {
                ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
                ExecutionEntity executionToContinue = executionEntityManager.createChildExecution(multiInstanceRootExecution);
                executionToContinue.setCurrentFlowElement(execution.getCurrentFlowElement());
                executionToContinue.setScope(true);
                executeOriginalBehavior(executionToContinue, multiInstanceRootExecution, loopCounter);
            } else {
                CommandContextUtil.getHistoryManager().recordActivityEnd((ExecutionEntity) execution, null);
                executeOriginalBehavior(execution, multiInstanceRootExecution, loopCounter);
            }

        } catch (BpmnError error) {
            // re-throw business fault so that it can be caught by an Error
            // Intermediate Event or Error Event Sub-Process in the process
            throw error;
        } catch (Exception e) {
            throw new FlowableException("Could not execute inner activity behavior of multi instance behavior", e);
        }
    }
}
