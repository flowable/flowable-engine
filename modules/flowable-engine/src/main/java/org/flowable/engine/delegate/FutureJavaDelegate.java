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
package org.flowable.engine.delegate;

import java.util.concurrent.CompletableFuture;

import org.flowable.common.engine.api.async.AsyncTaskInvoker;

/**
 * Convenience class that should be used when a Java delegation in a BPMN 2.0 process is required (for example, to call custom business logic).
 * When this interface is implemented then the execution of the logic can happen on a different thread then the process execution.
 * <p>
 * This class can be used only for service tasks.
 * <p>
 * This class does not allow to influence the control flow. It follows the default BPMN 2.0 behavior of taking every outgoing sequence flow (which has a condition that evaluates to true if there is a
 * condition defined) If you are in need of influencing the flow in your process, use the class 'org.flowable.engine.impl.pvm.delegate.ActivityBehavior' instead.
 * <p>
 * This interface allows fine grained control on how the future should be created.
 * It gives access to the {@link AsyncTaskInvoker} which can delegate execution to a shared task executor.
 * However, it doesn't have to be used.
 * In case you don't need custom task executor the {@link FlowableFutureJavaDelegate} can be used.
 * </p>
 *
 * @param <Output> the output of the execution
 * @author Filip Hrisafov
 * @see FlowableFutureJavaDelegate
 * @see MapBasedFlowableFutureJavaDelegate
 */
public interface FutureJavaDelegate<Output> {

    /**
     * Perform the execution of the delegate, potentially on another thread.
     * The result of the future is passed in the {@link #afterExecution(DelegateExecution, Object)} in order to store
     * the data on the execution on the same thread as the caller of this method.
     *
     * <b>IMPORTANT:</b> the execution should only be used to read data before creating the future.
     * The execution should not be used in the task that will be executed on a new thread.
     * <p>
     * The {@link AsyncTaskInvoker} is in order to schedule an execution on a different thread.
     * However, it is also possible to use a different scheduler, or return a future not created by the given {@code taskInvoker}.
     * </p>
     *
     * @param execution the execution that can be used to extract data
     * @param taskInvoker the task invoker that can be used to execute expensive operation on another thread
     * @return the output data of the execution
     */
    CompletableFuture<Output> execute(DelegateExecution execution, AsyncTaskInvoker taskInvoker);

    /**
     * Method invoked with the result from {@link #execute(DelegateExecution, AsyncTaskInvoker)}.
     * This should be used to set data on the {@link DelegateExecution}.
     * This is on the same thread as {@link #execute(DelegateExecution, AsyncTaskInvoker)} and participates in the process transaction.
     *
     * @param execution the execution to which data can be set
     * @param executionData the execution data
     */
    void afterExecution(DelegateExecution execution, Output executionData);

}
