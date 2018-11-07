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
package org.flowable.engine.impl.delegate.invocation;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

/**
 * Class handling invocations of ExecutionListeners
 * 
 * @author Daniel Meyer
 */
public class ExecutionListenerInvocation extends DelegateInvocation {

    protected final ExecutionListener executionListenerInstance;
    protected final DelegateExecution execution;

    public ExecutionListenerInvocation(ExecutionListener executionListenerInstance, DelegateExecution execution) {
        this.executionListenerInstance = executionListenerInstance;
        this.execution = execution;
    }

    @Override
    protected void invoke() {
        executionListenerInstance.notify(execution);
    }

    @Override
    public Object getTarget() {
        return executionListenerInstance;
    }

}
