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

import org.flowable.common.engine.api.async.AsyncTaskInvoker;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.FutureJavaDelegate;

/**
 * Class handling invocations of FutureJavaDelegates
 * 
 * @author Filip Hrisafov
 */
public class FutureJavaDelegateInvocation extends DelegateInvocation {

    protected final FutureJavaDelegate<?> delegateInstance;
    protected final DelegateExecution execution;
    protected final AsyncTaskInvoker asyncTaskInvoker;

    public FutureJavaDelegateInvocation(FutureJavaDelegate<?> delegateInstance, DelegateExecution execution, AsyncTaskInvoker asyncTaskInvoker) {
        this.delegateInstance = delegateInstance;
        this.execution = execution;
        this.asyncTaskInvoker = asyncTaskInvoker;
    }

    @Override
    protected void invoke() {
        invocationResult = delegateInstance.execute(execution, asyncTaskInvoker);
    }

    @Override
    public Object getTarget() {
        return delegateInstance;
    }

}
