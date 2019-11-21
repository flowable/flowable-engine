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
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;

public class StartProcessInstanceAfterContext extends AbstractStartProcessInstanceAfterContext {

    protected Map<String, Object> transientVariables;
    
    public StartProcessInstanceAfterContext() {
        
    }
    
    public StartProcessInstanceAfterContext(ExecutionEntity processInstance, ExecutionEntity childExecution, Map<String, Object> variables, 
                    Map<String, Object> transientVariables, FlowElement initialFlowElement, Process process, ProcessDefinition processDefinition) {
        
        super(processInstance, childExecution, variables, initialFlowElement, process, processDefinition);
        
        this.transientVariables = transientVariables;
    }

    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }

    public void setTransientVariables(Map<String, Object> transientVariables) {
        this.transientVariables = transientVariables;
    }
}
