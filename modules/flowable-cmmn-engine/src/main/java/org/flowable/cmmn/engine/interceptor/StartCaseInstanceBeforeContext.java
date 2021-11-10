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
package org.flowable.cmmn.engine.interceptor;

import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;

public class StartCaseInstanceBeforeContext extends AbstractStartCaseInstanceBeforeContext {

    protected String callbackId;
    protected String callbackType;
    protected String referenceId;
    protected String referenceType;
    protected String parentId;
    protected Map<String, Object> transientVariables;
    protected String tenantId;
    protected String initiatorVariableName;
    protected String overrideDefinitionTenantId;
    protected String predefinedCaseInstanceId;
    
    public StartCaseInstanceBeforeContext() {
        
    }

    public StartCaseInstanceBeforeContext(String businessKey, String businessStatus, String caseInstanceName, String callbackId, String callbackType,
            String referenceId, String referenceType, String parentId, Map<String, Object> variables,
            Map<String, Object> transientVariables, String tenantId,
            String initiatorVariableName, Case caseModel, CaseDefinition caseDefinition, CmmnModel cmmnModel,
            String overrideDefinitionTenantId, String predefinedCaseInstanceId) {

        super(businessKey, businessStatus, caseInstanceName, variables, caseModel, caseDefinition, cmmnModel);

        this.callbackId = callbackId;
        this.callbackType = callbackType;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.parentId = parentId;
        this.transientVariables = transientVariables;
        this.tenantId = tenantId;
        this.initiatorVariableName = initiatorVariableName;
        this.overrideDefinitionTenantId = overrideDefinitionTenantId;
        this.predefinedCaseInstanceId = predefinedCaseInstanceId;
    }

    public String getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    public String getCallbackType() {
        return callbackType;
    }

    public void setCallbackType(String callbackType) {
        this.callbackType = callbackType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }

    public void setTransientVariables(Map<String, Object> transientVariables) {
        this.transientVariables = transientVariables;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getInitiatorVariableName() {
        return initiatorVariableName;
    }

    public void setInitiatorVariableName(String initiatorVariableName) {
        this.initiatorVariableName = initiatorVariableName;
    }

    public String getOverrideDefinitionTenantId() {
        return overrideDefinitionTenantId;
    }

    public void setOverrideDefinitionTenantId(String overrideDefinitionTenantId) {
        this.overrideDefinitionTenantId = overrideDefinitionTenantId;
    }

    public String getPredefinedCaseInstanceId() {
        return predefinedCaseInstanceId;
    }

    public void setPredefinedCaseInstanceId(String predefinedCaseInstanceId) {
        this.predefinedCaseInstanceId = predefinedCaseInstanceId;
    }
}
