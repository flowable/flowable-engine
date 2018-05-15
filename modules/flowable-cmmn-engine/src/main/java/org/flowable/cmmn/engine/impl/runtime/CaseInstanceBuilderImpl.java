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
    protected String name;
    protected String businessKey;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    protected String tenantId;
    protected String outcome;
    protected String callbackType;
    protected String callbackId;

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
    public CaseInstanceBuilder outcome(String outcome) {
        this.outcome = outcome;
        return this;
    }

    @Override
    public CaseInstanceBuilder callbackType(String callbackType) {
        this.callbackType = callbackType;
        return this;
    }

    @Override
    public CaseInstanceBuilder callbackId(String callbackId) {
        this.callbackId = callbackId;
        return this;
    }

    @Override
    public CaseInstance start() {
        return cmmnRuntimeService.startCaseInstance(this);
    }
    
    @Override
    public CaseInstance startWithForm() {
        return cmmnRuntimeService.startCaseInstanceWithForm(this);
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
    public String getName() {
        return name;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
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
    public String getOutcome() {
        return outcome;
    }

    @Override
    public String getCallbackType() {
        return this.callbackType;
    }
    @Override
    public String getCallbackId() {
        return this.callbackId;
    }

}
