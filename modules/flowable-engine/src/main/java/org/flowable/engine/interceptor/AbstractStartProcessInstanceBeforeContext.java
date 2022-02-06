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

public class AbstractStartProcessInstanceBeforeContext {

    protected String businessKey;
    protected String businessStatus;
    protected String processInstanceName;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    protected String initialActivityId;
    protected FlowElement initialFlowElement;
    protected Process process;
    protected ProcessDefinition processDefinition;

    public AbstractStartProcessInstanceBeforeContext() {
        
    }

    public AbstractStartProcessInstanceBeforeContext(String businessKey, String businessStatus, String processInstanceName, Map<String, Object> variables,
            Map<String, Object> transientVariables, String initialActivityId, FlowElement initialFlowElement,
            Process process, ProcessDefinition processDefinition) {
        
        this.businessKey = businessKey;
        this.businessStatus = businessStatus;
        this.processInstanceName = processInstanceName;
        this.variables = variables;
        this.transientVariables = transientVariables;
        this.initialActivityId = initialActivityId;
        this.initialFlowElement = initialFlowElement;
        this.process = process;
        this.processDefinition = processDefinition;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public void setProcessInstanceName(String processInstanceName) {
        this.processInstanceName = processInstanceName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }

    public void setTransientVariables(Map<String, Object> transientVariables) {
        this.transientVariables = transientVariables;
    }

    public String getInitialActivityId() {
        return initialActivityId;
    }

    public void setInitialActivityId(String initialActivityId) {
        this.initialActivityId = initialActivityId;
    }

    public FlowElement getInitialFlowElement() {
        return initialFlowElement;
    }

    public void setInitialFlowElement(FlowElement initialFlowElement) {
        this.initialFlowElement = initialFlowElement;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }
}
