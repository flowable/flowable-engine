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
package org.flowable.engine.impl.runtime;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;

/**
 * @author Bassam Al-Sarori
 * @author Joram Barrez
 */
public class ProcessInstanceBuilderImpl implements ProcessInstanceBuilder {

    protected RuntimeServiceImpl runtimeService;

    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected String messageName;
    protected String processInstanceName;
    protected String businessKey;
    protected String callbackId;
    protected String callbackType;
    protected String stageInstanceId;
    protected String tenantId;
    protected String overrideDefinitionTenantId;
    protected String predefinedProcessInstanceId;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    protected Map<String, Object> startFormVariables;
    protected String outcome;
    protected boolean fallbackToDefaultTenant;

    public ProcessInstanceBuilderImpl(RuntimeServiceImpl runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ProcessInstanceBuilder processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public ProcessInstanceBuilder processDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    @Override
    public ProcessInstanceBuilder messageName(String messageName) {
        this.messageName = messageName;
        return this;
    }

    @Override
    public ProcessInstanceBuilder name(String processInstanceName) {
        this.processInstanceName = processInstanceName;
        return this;
    }

    @Override
    public ProcessInstanceBuilder businessKey(String businessKey) {
        this.businessKey = businessKey;
        return this;
    }
    
    @Override
    public ProcessInstanceBuilder callbackId(String callbackId) {
        this.callbackId = callbackId;
        return this;
    }
    
    @Override
    public ProcessInstanceBuilder callbackType(String callbackType) {
        this.callbackType = callbackType;
        return this;
    }

    @Override
    public ProcessInstanceBuilder stageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
        return this;
    }

    @Override
    public ProcessInstanceBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }
    
    @Override
    public ProcessInstanceBuilder overrideProcessDefinitionTenantId(String tenantId) {
        this.overrideDefinitionTenantId = tenantId;
        return this;
    }

    @Override
    public ProcessInstanceBuilder predefineProcessInstanceId(String processInstanceId) {
        this.predefinedProcessInstanceId = processInstanceId;
        return this;
    }

    @Override
    public ProcessInstanceBuilder variables(Map<String, Object> variables) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        if (variables != null) {
            this.variables.putAll(variables);
        }
        return this;
    }

    @Override
    public ProcessInstanceBuilder variable(String variableName, Object value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(variableName, value);
        return this;
    }

    @Override
    public ProcessInstanceBuilder transientVariables(Map<String, Object> transientVariables) {
        if (this.transientVariables == null) {
            this.transientVariables = new HashMap<>();
        }
        if (transientVariables != null) {
            this.transientVariables.putAll(transientVariables);
        }
        return this;
    }

    @Override
    public ProcessInstanceBuilder transientVariable(String variableName, Object value) {
        if (this.transientVariables == null) {
            this.transientVariables = new HashMap<>();
        }
        this.transientVariables.put(variableName, value);
        return this;
    }

    @Override
    public ProcessInstanceBuilder startFormVariables(Map<String, Object> startFormVariables) {
        if (this.startFormVariables == null) {
            this.startFormVariables = new HashMap<>();
        }
        if (startFormVariables != null) {
            this.startFormVariables.putAll(startFormVariables);
        }
        return this;
    }

    @Override
    public ProcessInstanceBuilder startFormVariable(String variableName, Object value) {
        if (this.startFormVariables == null) {
            this.startFormVariables = new HashMap<>();
        }
        this.startFormVariables.put(variableName, value);
        return this;
    }

    @Override
    public ProcessInstanceBuilder outcome(String outcome) {
        this.outcome = outcome;
        return this;
    }

    @Override
    public ProcessInstanceBuilder fallbackToDefaultTenant() {
        this.fallbackToDefaultTenant = true;
        return this;
    }

    @Override
    public ProcessInstance start() {
        return runtimeService.startProcessInstance(this);
    }

    @Override
    public ProcessInstance startAsync() {
        return runtimeService.startProcessInstanceAsync(this);
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getMessageName() {
        return messageName;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public String getBusinessKey() {
        return businessKey;
    }
    
    public String getCallbackId() {
        return callbackId;
    }

    public String getCallbackType() {
        return callbackType;
    }

    public String getStageInstanceId() {
        return stageInstanceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getOverrideDefinitionTenantId() {
        return overrideDefinitionTenantId;
    }

    public String getPredefinedProcessInstanceId() {
        return predefinedProcessInstanceId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }

    public Map<String, Object> getStartFormVariables() {
        return startFormVariables;
    }
    public String getOutcome() {
        return outcome;
    }

    public boolean isFallbackToDefaultTenant() {
        return fallbackToDefaultTenant;
    }

}
