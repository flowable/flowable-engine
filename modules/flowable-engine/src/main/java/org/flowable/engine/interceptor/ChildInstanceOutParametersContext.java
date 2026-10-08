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

import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * The completed child instance of a call activity or case task, see
 * {@link ChildInstanceParametersInterceptor#afterOutParameters(ChildInstanceOutParametersContext)}.
 */
public class ChildInstanceOutParametersContext {

    protected DelegateExecution execution;
    protected FlowElement flowElement;
    protected VariableContainer childInstance;

    public ChildInstanceOutParametersContext(DelegateExecution execution, FlowElement flowElement, VariableContainer childInstance) {
        this.execution = execution;
        this.flowElement = flowElement;
        this.childInstance = childInstance;
    }

    /** The execution of the call activity or case task, in the parent process instance: where the outcome is written. */
    public DelegateExecution getExecution() {
        return execution;
    }

    /** The call activity or case task. */
    public FlowElement getFlowElement() {
        return flowElement;
    }

    /** The completed child process or case instance, to read its variables from. */
    public VariableContainer getChildInstance() {
        return childInstance;
    }
}
