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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;

/**
 * @author Joram Barrez
 */
public class CaseInstanceBuilderImpl implements CaseInstanceBuilder {

    protected CmmnRuntimeServiceImpl cmmnRuntimeService;

    protected String caseDefinitionId;
    protected String caseDefinitionKey;
    protected String caseDefinitionParentDeploymentId;
    protected String predefinedCaseInstanceId;
    protected String name;
    protected String businessKey;
    protected String businessStatus;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    protected String tenantId;
    protected String overrideDefinitionTenantId;
    protected String outcome;
    protected Map<String, Object> startFormVariables;
    protected String callbackType;
    protected String callbackId;
    protected String referenceId;
    protected String referenceType;
    protected String parentId;
    protected boolean fallbackToDefaultTenant;
    protected boolean startWithForm;

    public CaseInstanceBuilderImpl() {
        
    }
    
    public CaseInstanceBuilderImpl(CmmnRuntimeServiceImpl cmmnRuntimeService) {
        this.cmmnRuntimeService = cmmnRuntimeService;
    }

    @Override
    public CaseInstanceBuilder caseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public CaseInstanceBuilder caseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
        return this;
    }

    @Override
    public CaseInstanceBuilder caseDefinitionParentDeploymentId(String parentDeploymentId) {
        this.caseDefinitionParentDeploymentId = parentDeploymentId;
        return this;
    }

    @Override
    public CaseInstanceBuilder predefinedCaseInstanceId(String caseInstanceId) {
        this.predefinedCaseInstanceId = caseInstanceId;
        return this;
    }

    @Override
    public CaseInstanceBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
    public CaseInstanceBuilder businessKey(String businessKey) {
        this.businessKey = businessKey;
        return this;
    }

    @Override
    public CaseInstanceBuilder businessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
        return this;
    }

    @Override
    public CaseInstanceBuilder variables(Map<String, Object> variables) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        
        if (variables != null) {
            this.variables.putAll(variables);
        }
        
        return this;
    }

    @Override
    public CaseInstanceBuilder variable(String variableName, Object value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(variableName, value);
        return this;
    }

    @Override
    public CaseInstanceBuilder transientVariables(Map<String, Object> transientVariables) {
        if (this.transientVariables == null) {
            this.transientVariables = new HashMap<>();
        }
        this.transientVariables.putAll(transientVariables);
        return this;
    }

    @Override
    public CaseInstanceBuilder transientVariable(String variableName, Object value) {
        if (this.transientVariables == null) {
            this.transientVariables = new HashMap<>();
        }
        this.transientVariables.put(variableName, value);
        return this;
    }
    
    @Override
    public CaseInstanceBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }
    
    @Override
    public CaseInstanceBuilder overrideCaseDefinitionTenantId(String tenantId) {
        this.overrideDefinitionTenantId = tenantId;
        return this;
    }
    
    @Override
    public CaseInstanceBuilder outcome(String outcome) {
        this.outcome = outcome;
        return this;
    }

    @Override
    public CaseInstanceBuilder startFormVariables(Map<String, Object> formVariables) {
        this.startWithForm = true;
        this.startFormVariables = formVariables;
        return this;
    }

    @Override
    public CaseInstanceBuilder callbackId(String callbackId) {
        this.callbackId = callbackId;
        return this;
    }

    @Override
    public CaseInstanceBuilder callbackType(String callbackType) {
        this.callbackType = callbackType;
        return this;
    }

    @Override
    public CaseInstanceBuilder referenceId(String referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    @Override
    public CaseInstanceBuilder referenceType(String referenceType) {
        this.referenceType = referenceType;
        return this;
    }

    @Override
    public CaseInstanceBuilder parentId(String parentCaseInstanceId) {
        this.parentId = parentCaseInstanceId;
        return this;
    }

    @Override
    public CaseInstanceBuilder fallbackToDefaultTenant() {
        this.fallbackToDefaultTenant = true;
        return this;
    }

    @Override
    public CaseInstance start() {
        return cmmnRuntimeService.startCaseInstance(this);
    }
    
    @Override
    public CaseInstance startAsync() {
        return cmmnRuntimeService.startCaseInstanceAsync(this);
    }

    @Override
    public CaseInstance startWithForm() {
        this.startWithForm = true;
        return cmmnRuntimeService.startCaseInstance(this);
    }

    @Override
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    @Override
    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }
    
    @Override
    public String getCaseDefinitionParentDeploymentId() {
        return caseDefinitionParentDeploymentId;
    }

    @Override
    public String getPredefinedCaseInstanceId() {
        return predefinedCaseInstanceId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
    }
    @Override
    public String getBusinessStatus() {
        return businessStatus;
    }

    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }
    
    @Override
    public String getOverrideDefinitionTenantId() {
        return overrideDefinitionTenantId;
    }
    
    @Override
    public String getOutcome() {
        return outcome;
    }

    @Override
    public Map<String, Object> getStartFormVariables() {
        return startFormVariables;
    }

    @Override
    public String getCallbackId() {
        return this.callbackId;
    }

    @Override
    public String getCallbackType() {
        return this.callbackType;
    }

    @Override
    public String getReferenceId() {
        return referenceId;
    }

    @Override
    public String getReferenceType() {
        return referenceType;
    }

    @Override
    public String getParentId() {
        return this.parentId;
    }

    @Override
    public boolean isFallbackToDefaultTenant() {
        return this.fallbackToDefaultTenant;
    }

    @Override
    public boolean isStartWithForm() {
        return this.startWithForm;
    }

}
