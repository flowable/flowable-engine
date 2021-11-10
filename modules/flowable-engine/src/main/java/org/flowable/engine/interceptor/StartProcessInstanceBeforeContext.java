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
package org.flowable.engine.interceptor;

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.repository.ProcessDefinition;

public class StartProcessInstanceBeforeContext extends AbstractStartProcessInstanceBeforeContext {

    protected String callbackId;
    protected String callbackType;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId;
    protected String initiatorVariableName;
    protected String overrideDefinitionTenantId;
    protected String predefinedProcessInstanceId;
    
    public StartProcessInstanceBeforeContext() {

    }
        
    public StartProcessInstanceBeforeContext(String businessKey, String businessStatus, String processInstanceName,
            String callbackId, String callbackType, String referenceId, String referenceType,
            Map<String, Object> variables, Map<String, Object> transientVariables, String tenantId,
            String initiatorVariableName, String initialActivityId, FlowElement initialFlowElement, Process process,
            ProcessDefinition processDefinition, String overrideDefinitionTenantId, String predefinedProcessInstanceId) {
        
        super(businessKey, businessStatus, processInstanceName, variables, transientVariables, initialActivityId, initialFlowElement, process,
                processDefinition);
        
        this.callbackId = callbackId;
        this.callbackType = callbackType;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.tenantId = tenantId;
        this.initiatorVariableName = initiatorVariableName;
        this.overrideDefinitionTenantId = overrideDefinitionTenantId;
        this.predefinedProcessInstanceId = predefinedProcessInstanceId;
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

    public String getPredefinedProcessInstanceId() {
        return predefinedProcessInstanceId;
    }

    public void setPredefinedProcessInstanceId(String predefinedProcessInstanceId) {
        this.predefinedProcessInstanceId = predefinedProcessInstanceId;
    }
}
