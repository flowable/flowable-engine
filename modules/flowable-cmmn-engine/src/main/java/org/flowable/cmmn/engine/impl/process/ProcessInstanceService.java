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

import org.flowable.cmmn.model.IOParameter;
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
    String startProcessInstanceByKey(String processDefinitionKey, String predefinedProcessInstanceId, String stageInstanceId,
            String tenantId, Boolean fallbackToDefaultTenant, String parentDeploymentId, Map<String, Object> inParametersMap, String businessKey,
            Map<String, Object> variableFormVariables, FormInfo variableFormInfo, String variableFormOutcome);

    /**
     * Starts a process instance with a reference to a plan item instance (i.e. blocking behavior).
     */
    String startProcessInstanceByKey(String processDefinitionKey, String predefinedProcessInstanceId, String planItemInstanceId, String stageInstanceId,
            String tenantId, Boolean fallbackToDefaultTenant, String parentDeploymentId, Map<String, Object> inParametersMap, String businessKey,
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
     * Resolves the given expression within the context of the passed execution.
     */
    Object resolveExpression(String executionId, String expression);

    /**
     * Triggers a case instance that was started by a process instance.
     */
    void triggerCaseTask(String executionId, Map<String, Object> variables);

    /**
     * Retrieves the {@link IOParameter} out parameters of a case task currently being execution by the given execution.
     */
    List<IOParameter> getOutputParametersOfCaseTask(String executionId);

}
