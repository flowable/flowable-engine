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

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Micha Kiener
 */
public interface CaseInstanceBuilder {

    /**
     * Set the case definition to be used for creating a new case instance by its id. If both the case definition id and the key
     * are set, the id takes precedence and the key will be ignored. At least one of them needs to be specified within the builder.
     *
     * @param caseDefinitionId the id of the case definition the new case should be based on
     * @return the case instance builder for method chaining
     */
    CaseInstanceBuilder caseDefinitionId(String caseDefinitionId);

    /**
     * Set the case definition to be used for creating a new case instance by its key. If both the case definition id and the key
     * are set, the id takes precedence and the key will be ignored. At least one of them needs to be specified within the builder.
     *
     * @param caseDefinitionKey the key of the case definition the new case should be based on
     * @return the case instance builder for method chaining
     */
    CaseInstanceBuilder caseDefinitionKey(String caseDefinitionKey);
    
    /**
     * When looking up for a case definition by key it would first look up for a case definition
     * within the given parent deployment.
     * Then it would fall back to the latest case definition with the given key.
     * <p>
     * This is typically needed when the CaseInstanceBuilder is called for example
     * from the process engine to start a case instance and it needs to
     * look up the case definition in the same deployment as the process.
     * Or when starting a case via a case task from the cmmn engine
     */
    CaseInstanceBuilder caseDefinitionParentDeploymentId(String parentDeploymentId);

    /**
     * If the new case instance should have a predefined id, you can set it using this method.
     * If that predefined id is set, it will be used instead of creating a new one automatically.
     *
     * @param caseInstanceId the id of the new case instance to be used
     * @return the case instance builder for method chaining
     */
    CaseInstanceBuilder predefinedCaseInstanceId(String caseInstanceId);

    CaseInstanceBuilder name(String name);

    CaseInstanceBuilder businessKey(String businessKey);

    CaseInstanceBuilder businessStatus(String businessStatus);

    CaseInstanceBuilder variables(Map<String, Object> variables);

    CaseInstanceBuilder variable(String variableName, Object value);

    CaseInstanceBuilder transientVariables(Map<String, Object> transientVariables);

    CaseInstanceBuilder transientVariable(String variableName, Object value);

    CaseInstanceBuilder tenantId(String tenantId);

    /**
     * Set the owner of the case to be created to the given user id.
     * @param userId the id of the user to become the owner of the case
     * @return the case instance builder for method chaining
     */
    CaseInstanceBuilder owner(String userId);

    /**
     * Set the assignee of the case to be created to the given user id.
     * @param userId the id of the user to become the assignee of the case
     * @return the case instance builder for method chaining
     */
    CaseInstanceBuilder assignee(String userId);
    
    /**
     * Indicator to override the tenant id of the case definition with the provided value.
     * The tenantId to lookup the case definition should still be provided if needed.
     */
    CaseInstanceBuilder overrideCaseDefinitionTenantId(String tenantId);

    /**
     * Allows to pass any variables if they come from a form.
     * The difference with regular {@link #variables(Map)} is that the  start form will be fetched
     * and the variables matched with the {@link org.flowable.form.api.FormInfo}.
     */
    CaseInstanceBuilder startFormVariables(Map<String, Object> formVariables);

    /**
     * Saves the outcome of the start form for the case, if this case should be started out of a start form.
     * You can additionally save any form variables along with the outcome and start the case using {@link #startWithForm()}.
     *
     * @param outcome the outcome to be registered in the builder
     * @return the case instance builder for method chaining
     */
    CaseInstanceBuilder outcome(String outcome);

    /**
     * Set callback id of the newly created case instance.
     *
     * @param callbackId id of the callback
     * @return case instance builder which creates case instance with defined callback id
     */
    CaseInstanceBuilder callbackId(String callbackId);

    /**
     * Set callback type of the newly created case instance.
     * @param callbackType type of the callback
     * @return case instance builder which creates case instance with defined callback type
     */
    CaseInstanceBuilder callbackType(String callbackType);

    /**
     * Set the reference id on the newly create case instance.
     */
    CaseInstanceBuilder referenceId(String referenceId);

    /**
     * Set the reference type on the newly create case instance.
     */
    CaseInstanceBuilder referenceType(String referenceType);

    /**
     * Set parent case instanceId of the newly create case instance
     *
     * @param parentCaseInstanceId parent case instance identifier
     * @return modified case instance builder which creates case instance with the reference to parent
     */
    CaseInstanceBuilder parentId(String parentCaseInstanceId);

    /**
     * If case definition is not found by key in the specified tenant use default tenant search as a fall back
     *
     * @return modified case instance builder
     */
    CaseInstanceBuilder fallbackToDefaultTenant();

    /**
     * Once all the information is set using this builder API, the start method will create the case instance, initialize it according all
     * the data in the builder and then evaluate the case model to start the case. It will be initialized, evaluated and started in a single
     * transaction, synchronously, so this method returns once the case model will hit a wait state.
     *
     * @return the case instance
     */
    CaseInstance start();

    /**
     * Once all the information is set using this builder API, the startAsync method will create the case instance and initialize its data, but
     * the case model is not yet evaluated, but will be started and evaluated asynchronously in a different transaction.
     *
     * @return the case instance as being persisted, but not yet evaluated through the case model
     */
    CaseInstance startAsync();

    /**
     * Once all the information is set using this builder API, the startWithForm method will create the case instance and initialize its data by
     * additionally using the submitted form variables and handling them with the start form provided with the case model (e.g. validation).
     *
     * @return the case instance as being persisted, but not yet evaluated through the case model
     */
    CaseInstance startWithForm();

    String getCaseDefinitionId();

    String getCaseDefinitionKey();

    String getCaseDefinitionParentDeploymentId();
    
    String getPredefinedCaseInstanceId();

    String getName();

    String getBusinessKey();

    String getBusinessStatus();

    Map<String, Object> getVariables();

    Map<String, Object> getTransientVariables();

    String getTenantId();

    String getOwner();

    String getAssignee();
    
    String getOverrideDefinitionTenantId();

    Map<String, Object> getStartFormVariables();

    String getOutcome();

    String getCallbackId();

    String getCallbackType();

    String getReferenceId();

    String getReferenceType();

    String getParentId();

    boolean isFallbackToDefaultTenant();

    boolean isStartWithForm();
}
