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
 */
public interface CaseInstanceBuilder {

    CaseInstanceBuilder caseDefinitionId(String caseDefinitionId);

    CaseInstanceBuilder caseDefinitionKey(String caseDefinitionKey);
    
    /**
     * When looking up for a case definition by key it would first lookup for a case definition
     * within the given parent deployment.
     * Then it would fallback to the latest case definition with the given key.
     * <p>
     * This is typically needed when the CaseInstanceBuilder is called for example
     * from the process engine to start a case instance and it needs to
     * look up the case definition in the same deployment as the process.
     * Or when starting a case via a case task from the cmmn engine
     */
    CaseInstanceBuilder caseDefinitionParentDeploymentId(String parentDeploymentId);

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

    CaseInstance start();

    CaseInstance startAsync();

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
