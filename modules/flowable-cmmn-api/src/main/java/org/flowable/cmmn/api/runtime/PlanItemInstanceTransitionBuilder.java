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
package org.flowable.cmmn.api.runtime;

import java.util.Map;

import org.flowable.form.api.FormInfo;

/**
 * A builder that allows to execute a transition for a plan item instance from one state to another,
 * optionally passing variables before the transition.
 *
 * @author Joram Barrez
 */
public interface PlanItemInstanceTransitionBuilder {

    /**
     * Sets a variable before the transition is executed.
     * The variable will be stored on the case instance.
     */
    PlanItemInstanceTransitionBuilder variable(String variableName, Object variableValue);

    /**
     * See {@link #variable(String, Object)}.
     */
    PlanItemInstanceTransitionBuilder variables(Map<String, Object> variables);

    /**
     * The form variables that should be set before the transition is executed.
     */
    PlanItemInstanceTransitionBuilder formVariables(Map<String, Object> variables, FormInfo formInfo, String outcome);

    /**
     * Sets a local variable before the transition is executed.
     * The variable will be stored locally on the plan item instance.
     */
    PlanItemInstanceTransitionBuilder localVariable(String variableName, Object variableValue);

    /**
     * See {@link #localVariable(String, Object)}.
     */
    PlanItemInstanceTransitionBuilder localVariables(Map<String, Object> localVariables);

    /**
     * Sets a non-persisted variable before the transition is executed.
     * The transient variable will not be persisted at the end of the database transaction.
     */
    PlanItemInstanceTransitionBuilder transientVariable(String variableName, Object variableValue);

    /**
     * See {@link #transientVariable(String, Object)}.
     */
    PlanItemInstanceTransitionBuilder transientVariables(Map<String, Object> transientVariables);

    /**
     * Behaviors that create a new 'child entity' can take in specialized variables.
     * For example: the case task can pass variables that will be set on the child case instance and not on the parent case instance.
     *
     * Only allowed when starting a plan item instance.
     */
    PlanItemInstanceTransitionBuilder childTaskVariable(String variableName, Object variableValue);

    /**
     * See {@link #childTaskVariable(String, Object)}.
     */
    PlanItemInstanceTransitionBuilder childTaskVariables(Map<String, Object> childTaskVariables);

    /**
     * The form variables that should be used when creating a new 'child entity'.
     *
     * @see #childTaskVariable(String, Object)
     */
    PlanItemInstanceTransitionBuilder childTaskFormVariables(Map<String, Object> variables, FormInfo formInfo, String outcome);

    /**
     * Completes the plan item instance, which needs to be a stage instance.
     * The stage needs to be completable, otherwise an exception will be thrown.
     */
    void completeStage();

    /**
     * Completes the plan item instance, which needs to be a stage instance.
     * The stage is completed, irregardless whether it is completable or not.
     */
    void forceCompleteStage();

    /**
     * Triggers a plan item to continue, e.g. a human task completion, a service task wait state that continues, etc.
     */
    void trigger();

    /**
     * Enables a manually activated plan item instance.
     */
    void enable();

    /**
     * Disables a manually activated plan item instance.
     */
    void disable();

    /**
     * Starts a plan item instance, this typically will executes it associated behavior.
     */
    void start();

    /**
     * Manually terminates a plan item instance.
     */
    void terminate();

}
