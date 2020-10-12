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
 * Convenience class which always uses the {@link AsyncTaskInvoker} to execute the async data.
 * Provides intermediate methods to prepare the execution data before executing and do the
 * actual execution without the need to work with futures.
 *
 * @param <Input> the input of the execution
 * @param <Output> the output of the execution
 * @author Filip Hrisafov
 * @see MapBasedFlowablePlanItemFutureJavaDelegate
 * @see PlanItemFutureJavaDelegate
 */
public interface FlowablePlanItemFutureJavaDelegate<Input, Output> extends PlanItemFutureJavaDelegate<Output> {

    @Override
    default CompletableFuture<Output> execute(DelegatePlanItemInstance planItemInstance, AsyncTaskInvoker taskInvoker) {
        Input inputData = prepareExecutionData(planItemInstance);
        return taskInvoker.submit(() -> execute(inputData));
    }

    /**
     * Method invoked before doing the execution to extract needed that from the planItemInstance
     * on the main thread.
     * This should be used to prepare and extract data from the planItemInstance before doing the execution in a different thread.
     *
     * @param planItemInstance the planItemInstance from which to extract data
     * @return the data for the delegate
     */
    Input prepareExecutionData(DelegatePlanItemInstance planItemInstance);

    /**
     * Perform the actual execution of the delegate in another thread.
     * This uses {@link #prepareExecutionData(DelegatePlanItemInstance)} to get the needed data
     * from the {@link DelegatePlanItemInstance} and returns the output data that can is passed to {@link #afterExecution(DelegatePlanItemInstance, Object)}.
     *
     * <b>IMPORTANT:</b> This is a completely new thread which does not participate in the transaction of the case.
     *
     * @param inputData the input data for the execution created via {@link #prepareExecutionData(DelegatePlanItemInstance)}
     * @return the output data of the execution
     * @see #execute(DelegatePlanItemInstance, AsyncTaskInvoker)
     */
    Output execute(Input inputData);

    /**
     * Method invoked with the result from {@link #execute(Object)}.
     * This should be used to set data on the {@link DelegatePlanItemInstance}.
     * This is on the same thread as {@link #prepareExecutionData(DelegatePlanItemInstance)} and participates in the case transaction.
     *
     * @param planItemInstance the planItemInstance to which data can be set
     * @param executionData the execution data
     */
    @Override
    void afterExecution(DelegatePlanItemInstance planItemInstance, Output executionData);

}
