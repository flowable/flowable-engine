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
import org.flowable.engine.delegate.DelegateExecution;

/**
 * The variables a call activity or case task starts its child instance with, see
 * {@link ChildInstanceParametersInterceptor#afterInParameters(ChildInstanceInParametersContext)}.
 */
public class ChildInstanceInParametersContext {

    protected DelegateExecution execution;
    protected FlowElement flowElement;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;

    public ChildInstanceInParametersContext(DelegateExecution execution, FlowElement flowElement, Map<String, Object> variables,
            Map<String, Object> transientVariables) {

        this.execution = execution;
        this.flowElement = flowElement;
        this.variables = variables;
        this.transientVariables = transientVariables;
    }

    /** The execution of the call activity or case task, in the parent process instance. */
    public DelegateExecution getExecution() {
        return execution;
    }

    /** The call activity or case task. */
    public FlowElement getFlowElement() {
        return flowElement;
    }

    /** The variables the child instance starts with. Changes to this map are passed to the child instance. */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /** The transient variables the child instance starts with. Changes to this map are passed to the child instance. */
    public Map<String, Object> getTransientVariables() {
        return transientVariables;
    }
}
