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
package org.flowable.cmmn.engine.impl.process;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.model.IOParameter;
import org.flowable.common.engine.api.delegate.BusinessError;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.form.api.FormInfo;

/**
 * @author Joram Barrez
 */
public interface ProcessInstanceService {

    /**
     * @return A new id that will be used when starting a process instance.
     *         This is for example needed to set the bidirectional relation
     *         when a case instance starts a process instance through a process task.
     */
    String generateNewProcessInstanceId();

    /**
     * Starts a process instance without a reference to a plan item instance (i.e. non-blocking behavior).
     */
    String startProcessInstance(String processDefinitionId, String predefinedProcessInstanceId, String stageInstanceId,
            String tenantId, Map<String, Object> inParametersMap, Map<String, Object> transientVariablesMap, String businessKey,
            Map<String, Object> variableFormVariables, FormInfo variableFormInfo, String variableFormOutcome);

    /**
     * Starts a process instance with a reference to a plan item instance (i.e. blocking behavior).
     */
    String startProcessInstance(String processDefinitionId, String predefinedProcessInstanceId, String planItemInstanceId, String stageInstanceId,
            String tenantId, Map<String, Object> inParametersMap, Map<String, Object> transientVariablesMap, String businessKey,
            Map<String, Object> variableFormVariables, FormInfo variableFormInfo, String variableFormOutcome);

    /**
     * Deletes the given process instance. Typically used to propagate termination.
     */
    void deleteProcessInstance(String processInstanceId);

    /**
     * Returns the variable value for a given variable.
     */
    Object getVariable(String executionId, String variableName);

    /**
     * Returns all variables for the given execution (or process instance).
     */
    Map<String, Object> getVariables(String executionId);

    /**
     * The variables of the given execution, read on demand. By default a read only view on {@link #getVariable(String, String)}
     * and {@link #getVariables(String)}.
     */
    default VariableContainer getVariableContainer(String executionId) {
        return new VariableContainer() {

            @Override
            public boolean hasVariable(String variableName) {
                return getVariables(executionId).containsKey(variableName);
            }

            @Override
            public Object getVariable(String variableName) {
                return ProcessInstanceService.this.getVariable(executionId, variableName);
            }

            @Override
            public void setVariable(String variableName, Object variableValue) {
                throw new UnsupportedOperationException("The variables of execution " + executionId + " are read only here");
            }

            @Override
            public void setTransientVariable(String variableName, Object variableValue) {
                throw new UnsupportedOperationException("The variables of execution " + executionId + " are read only here");
            }

            @Override
            public String getTenantId() {
                return null;
            }

            @Override
            public Set<String> getVariableNames() {
                return getVariables(executionId).keySet();
            }
        };
    }

    /**
     * Resolves the given expression within the context of the passed execution.
     */
    Object resolveExpression(String executionId, String expression);

    /**
     * Triggers a case instance that was started by a process instance.
     */
    void triggerCaseTask(String executionId, Map<String, Object> variables);

    /**
     * Triggers a case instance that was started by a process instance, passing the completed child case instance, so the
     * process engine can hand it to its child instance parameters interceptor.
     */
    default void triggerCaseTask(String executionId, Map<String, Object> variables, VariableContainer childCaseInstance) {
        triggerCaseTask(executionId, variables);
    }

    /**
     * Propagates an uncaught business error from a child case instance to the parent BPMN execution.
     * This triggers BPMN error propagation (boundary error events) on the CaseTask.
     */
    void handleCaseTaskError(String executionId, BusinessError error);

    /**
     * Retrieves the {@link IOParameter} out parameters of a case task currently being execution by the given execution.
     */
    List<IOParameter> getOutputParametersOfCaseTask(String executionId);

    /**
     * Resolves the process definition id from the given key and parameters.
     */
    String resolveProcessDefinitionId(String processDefinitionKey, String tenantId,
            Boolean fallbackToDefaultTenant, String parentDeploymentId);

    /**
     * Checks whether history is enabled for the given process definition id.
     */
    boolean isHistoryEnabledForProcessDefinitionId(String processDefinitionId);

    /**
     * Checks whether history is enabled for the given process instance id.
     */
    boolean isHistoryEnabledForProcessInstance(String processInstanceId);

    /**
     * Returns the id of the root process instance of the call hierarchy the given process instance belongs to.
     * For a top-level process instance this is the process instance id itself; for a process instance started
     * through one or more BPMN call activities it is the outermost process instance. Used to resolve the owning
     * case of a variable that belongs to a process nested under a case.
     */
    default String getRootProcessInstanceId(String processInstanceId) {
        return processInstanceId;
    }

}
