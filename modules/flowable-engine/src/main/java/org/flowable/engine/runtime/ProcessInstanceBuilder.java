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
package org.flowable.engine.runtime;

import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormInfo;

/**
 * Helper for starting new ProcessInstance.
 * 
 * An instance can be obtained through {@link org.flowable.engine.RuntimeService#createProcessInstanceBuilder()}.
 * 
 * processDefinitionId or processDefinitionKey should be set before calling {@link #start()} to start a process instance.
 * 
 * 
 * @author Bassam Al-Sarori
 * @author Joram Barrez
 */
public interface ProcessInstanceBuilder {

    /**
     * Set the id of the process definition
     **/
    ProcessInstanceBuilder processDefinitionId(String processDefinitionId);

    /**
     * Set the key of the process definition, latest version of the process definition with the given key. If processDefinitionId was set this will be ignored
     **/
    ProcessInstanceBuilder processDefinitionKey(String processDefinitionKey);

    /**
     * When looking up for a process definition by key it would first lookup for a process definition
     * within the given parent deployment.
     * Then it would fallback to the latest process definition with the given key.
     * <p>
     * This is typically needed when the ProcessInstanceBuilder is called for example
     * from the case engine to start a process instance and it needs to
     * look up the process definition in the same deployment as the case.
     */
    ProcessInstanceBuilder processDefinitionParentDeploymentId(String parentDeploymentId);

    /**
     * Set the message name that needs to be used to look up the process definition that needs to be used to start the process instance.
     */
    ProcessInstanceBuilder messageName(String messageName);

    /**
     * Set the name of process instance
     **/
    ProcessInstanceBuilder name(String processInstanceName);

    /**
     * Set the businessKey of process instance
     **/
    ProcessInstanceBuilder businessKey(String businessKey);

    /**
     * Set the businessStatus of process instance
     **/
    ProcessInstanceBuilder businessStatus(String businessStatus);

    /**
     * Sets the callback identifier of the process instance.
     */
    ProcessInstanceBuilder callbackId(String callbackId);
    
    /**
     * Sets the callback type of the process instance.
     */
    ProcessInstanceBuilder callbackType(String callbackType);

    /**
     * Sets the reference identifier of the process instance.
     */
    ProcessInstanceBuilder referenceId(String referenceId);

    /**
     * Sets the reference type of the process instance.
     */
    ProcessInstanceBuilder referenceType(String referenceType);

    /**
     * Set the optional instance id of the stage this process instance belongs to, if it runs in the context of a CMMN case.
     */
    ProcessInstanceBuilder stageInstanceId(String stageInstanceId);

    /**
     * Set the tenantId of to lookup the process definition
     **/
    ProcessInstanceBuilder tenantId(String tenantId);
    
    /**
     * Indicator to override the tenant id of the process definition with the provided value.
     * The tenantId to lookup the process definition should still be provided if needed.
     */
    ProcessInstanceBuilder overrideProcessDefinitionTenantId(String tenantId);
    
    /**
     * When starting a process instance from the CMMN engine process task, the process instance id needs to be known beforehand
     * to store entity links and callback references before the process instance is started.
     */
    ProcessInstanceBuilder predefineProcessInstanceId(String processInstanceId);

    /**
     * Sets the process variables
     */
    ProcessInstanceBuilder variables(Map<String, Object> variables);

    /**
     * Adds a variable to the process instance
     **/
    ProcessInstanceBuilder variable(String variableName, Object value);

    /**
     * Sets the transient variables
     */
    ProcessInstanceBuilder transientVariables(Map<String, Object> transientVariables);

    /**
     * Adds a transient variable to the process instance
     */
    ProcessInstanceBuilder transientVariable(String variableName, Object value);

    /**
     * Adds variables from a start form to the process instance.
     */
    ProcessInstanceBuilder startFormVariables(Map<String, Object> startFormVariables);

    /**
     * Adds one variable from a start form to the process instance.
     */
    ProcessInstanceBuilder startFormVariable(String variableName, Object value);

    /**
     * Allows to set an outcome for a start form.
     */
    ProcessInstanceBuilder outcome(String outcome);

    /**
     * Start the process instance with the given form variables from the given {@code formInfo}.
     * This is different than {@link #startFormVariables(Map)} and it can be used in addition to that.
     */
    ProcessInstanceBuilder formVariables(Map<String, Object> formVariables, FormInfo formInfo, String formOutcome);

    /**
     * Use default tenant as a fallback in the case when process definition was not found by key and tenant id
     */
    ProcessInstanceBuilder fallbackToDefaultTenant();

    /**
     * Start the process instance
     * 
     * @throws FlowableIllegalArgumentException
     *             if processDefinitionKey and processDefinitionId are null
     * @throws FlowableObjectNotFoundException
     *             when no process definition is deployed with the given processDefinitionKey or processDefinitionId
     **/
    ProcessInstance start();

    /**
     * Start the process instance asynchronously
     *
     * @throws FlowableIllegalArgumentException
     *             if processDefinitionKey and processDefinitionId are null
     * @throws FlowableObjectNotFoundException
     *             when no process definition is deployed with the given processDefinitionKey or processDefinitionId
     **/
    ProcessInstance startAsync();

}
