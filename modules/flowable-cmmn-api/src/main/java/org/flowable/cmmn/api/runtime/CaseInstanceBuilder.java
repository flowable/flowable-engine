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

    CaseInstanceBuilder name(String name);

    CaseInstanceBuilder businessKey(String businessKey);

    CaseInstanceBuilder variables(Map<String, Object> variables);

    CaseInstanceBuilder variable(String variableName, Object value);

    CaseInstanceBuilder transientVariables(Map<String, Object> transientVariables);

    CaseInstanceBuilder transientVariable(String variableName, Object value);

    CaseInstanceBuilder tenantId(String tenantId);
    
    CaseInstanceBuilder outcome(String outcome);

    /**
     * Set callback type of the newly created case instance.
     * @param callbackType type of the callback
     * @return case instance builder which creates case instance with defined callback type
     */
    CaseInstanceBuilder callbackType(String callbackType);

    /**
     * Set callback id of the newly created case instance.
     *
     * @param callbackId id of the callback
     * @return case instance builder which creates case instance with defined callback id
     */
    CaseInstanceBuilder callbackId(String callbackId);

    CaseInstance start();
    
    CaseInstance startWithForm();

    String getCaseDefinitionId();

    String getCaseDefinitionKey();

    String getName();

    String getBusinessKey();

    Map<String, Object> getVariables();

    Map<String, Object> getTransientVariables();

    String getTenantId();

    String getOutcome();

    String getCallbackType();

    String getCallbackId();
}
