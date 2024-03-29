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

package org.flowable.engine.delegate;

import java.util.List;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * Execution used in {@link JavaDelegate}s and {@link ExecutionListener}s.
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface DelegateExecution extends VariableScope {

    /**
     * Unique id of this path of execution that can be used as a handle to provide external signals back into the engine after wait states.
     */
    String getId();

    /** Reference to the overall process instance */
    String getProcessInstanceId();

    /**
     * The 'root' process instance. When using call activity for example, the processInstance set will not always be the root. This method returns the topmost process instance.
     */
    String getRootProcessInstanceId();

    /**
     * Will contain the event name in case this execution is passed in for an {@link ExecutionListener}.
     */
    String getEventName();

    /**
     * Sets the current event (typically when execution an {@link ExecutionListener}).
     */
    void setEventName(String eventName);

    /**
     * The business key for the process instance this execution is associated with.
     */
    String getProcessInstanceBusinessKey();
    
    /**
     * The business status for the process instance this execution is associated with.
     */
    String getProcessInstanceBusinessStatus();

    /**
     * The process definition key for the process instance this execution is associated with.
     */
    String getProcessDefinitionId();

    /**
     * If this execution runs in the context of a case and stage, this method returns it's closest parent stage instance id (the stage plan item instance id to be
     * precise).
     *
     * @return the stage instance id this execution belongs to or null, if this execution is not part of a case at all or is not a child element of a stage
     */
    String getPropagatedStageInstanceId();

    /**
     * Gets the id of the parent of this execution. If null, the execution represents a process-instance.
     */
    String getParentId();

    /**
     * Gets the id of the calling execution. If not null, the execution is part of a subprocess.
     */
    String getSuperExecutionId();

    /**
     * Gets the id of the current activity.
     */
    String getCurrentActivityId();

    /**
     * Gets the name of the current activity.
     */
    String getCurrentActivityName();

    /**
     * Returns the tenant id, if any is set before on the process definition or process instance.
     */
    @Override
    String getTenantId();

    /**
     * The BPMN element where the execution currently is at.
     */
    FlowElement getCurrentFlowElement();

    /**
     * Change the current BPMN element the execution is at.
     */
    void setCurrentFlowElement(FlowElement flowElement);

    /**
     * Returns the {@link FlowableListener} instance matching an {@link ExecutionListener} if currently an execution listener is being execution. Returns null otherwise.
     */
    FlowableListener getCurrentFlowableListener();

    /**
     * Called when an {@link ExecutionListener} is being executed.
     */
    void setCurrentFlowableListener(FlowableListener currentListener);

    /**
     * Create a snapshot read only delegate execution of this delegate execution.
     *
     * @return a {@link ReadOnlyDelegateExecution}
     */
    ReadOnlyDelegateExecution snapshotReadOnly();

    /* Execution management */

    /**
     * returns the parent of this execution, or null if there no parent.
     */
    DelegateExecution getParent();

    /**
     * returns the list of execution of which this execution the parent of.
     */
    List<? extends DelegateExecution> getExecutions();

    /* State management */

    /**
     * makes this execution active or inactive.
     */
    void setActive(boolean isActive);

    /**
     * returns whether this execution is currently active.
     */
    boolean isActive();

    /**
     * returns whether this execution has ended or not.
     */
    boolean isEnded();

    /**
     * changes the concurrent indicator on this execution.
     */
    void setConcurrent(boolean isConcurrent);

    /**
     * returns whether this execution is concurrent or not.
     */
    boolean isConcurrent();

    /**
     * returns whether this execution is a process instance or not.
     */
    boolean isProcessInstanceType();

    /**
     * Inactivates this execution. This is useful for example in a join: the execution still exists, but it is not longer active.
     */
    void inactivate();

    /**
     * Returns whether this execution is a scope.
     */
    boolean isScope();

    /**
     * Changes whether this execution is a scope or not.
     */
    void setScope(boolean isScope);

    /**
     * Returns whether this execution is the root of a multi instance execution.
     */
    boolean isMultiInstanceRoot();

    /**
     * Changes whether this execution is a multi instance root or not.
     * 
     * @param isMultiInstanceRoot
     */
    void setMultiInstanceRoot(boolean isMultiInstanceRoot);

}
