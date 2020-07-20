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
package org.flowable.cmmn.api.delegate;

import java.util.concurrent.CompletableFuture;

import org.flowable.common.engine.api.async.AsyncTaskInvoker;

/**
 * Convenience class to be used when needing to execute custom logic for a plan item by delegating to a Java class.
 * When this interface is implemented then the execution of the logic can happen on a different thread then the case execution.
 * The plan item is completed, after the future is done.
 * <p>
 * This interface allows fine grained control on how the future should be created.
 * It gives access to the {@link AsyncTaskInvoker} which can delegate execution to a shared task executor.
 * However, it doesn't have to be used.
 * In case you don't need custom task executor the {@link FlowablePlanItemFutureJavaDelegate} can be used.
 * </p>
 *
 * @param <Output> the output of the execution
 * @author Filip Hrisafov
 * @see FlowablePlanItemFutureJavaDelegate
 * @see MapBasedFlowablePlanItemFutureJavaDelegate
 */
public interface PlanItemFutureJavaDelegate<Output> {

    /**
     * Perform the execution of the delegate, potentially on another thread.
     * The result of the future is passed in the {@link #afterExecution(DelegatePlanItemInstance, Object)} in order to store
     * the data on the planItemInstance on the same thread as the caller of this method.
     *
     * <b>IMPORTANT:</b> the planItemInstance should only be used to read data before creating the future.
     * The planItemInstance should not be used in the task that will be executed on a new thread.
     * <p>
     * The {@link AsyncTaskInvoker} is in order to schedule an execution on a different thread.
     * However, it is also possible to use a different scheduler, or return a future not created by the given {@code taskInvoker}.
     * </p>
     *
     * @param planItemInstance the planItemInstance that can be used to extract data
     * @param taskInvoker the task invoker that can be used to execute expensive operation on another thread
     * @return the output data of the execution
     */
    CompletableFuture<Output> execute(DelegatePlanItemInstance planItemInstance, AsyncTaskInvoker taskInvoker);

    /**
     * Method invoked with the result from {@link #execute(DelegatePlanItemInstance, AsyncTaskInvoker)}.
     * This should be used to set data on the {@link DelegatePlanItemInstance}.
     * This is on the same thread as {@link #execute(DelegatePlanItemInstance, AsyncTaskInvoker)} and participates in the case transaction.
     *
     * @param planItemInstance the planItemInstance to which data can be set
     * @param executionData the execution data
     */
    void afterExecution(DelegatePlanItemInstance planItemInstance, Output executionData);

}
