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

/**
 * Convenience class that should be used when a Java delegation in a BPMN 2.0 process is required (for example, to call custom business logic).
 * When this interface is implemented then the execution of the logic can happen on a different thread then the process execution.
 * <p>
 * This class can be used for both service tasks and event listeners.
 * <p>
 * This class does not allow to influence the control flow. It follows the default BPMN 2.0 behavior of taking every outgoing sequence flow (which has a condition that evaluates to true if there is a
 * condition defined) If you are in need of influencing the flow in your process, use the class 'org.flowable.engine.impl.pvm.delegate.ActivityBehavior' instead.
 *
 * @param <Input> the input of the execution
 * @param <Output> the output of the execution
 * @author Filip Hrisafov
 */
public interface FutureJavaDelegate<Input, Output> {

    /**
     * Method invoked before doing the execution to extract needed that from the execution
     * on the main thread.
     * This should be used to prepare and extract data from the execution before doing the execution in a different thread.
     *
     * @param execution the execution from which to extract data
     * @return the data for the delegate
     */
    default Input beforeExecution(DelegateExecution execution) {
        return null;
    }

    /**
     * Perform the actual execution of the delegate in another thread.
     * This uses {@link #beforeExecution(DelegateExecution)} to get the needed data
     * from the {@link DelegateExecution} and returns the output data that can is passed to {@link #afterExecution(DelegateExecution, Object)}.
     *
     * <b>IMPORTANT:</b> This is a completely new thread which does not participate in the transaction of the process.
     *
     * @param inputData the input data for the execution created via {@link #beforeExecution(DelegateExecution)}
     * @return the output data of the execution
     */
    Output execute(Input inputData);

    /**
     * Method invoked with the result from {@link #execute(Object)}.
     * This should be used to set data on the {@link DelegateExecution}.
     * This is on the same thread as {@link #beforeExecution(DelegateExecution)} and participates in the process transaction.
     *
     * @param execution the execution to which data can be set
     * @param executionData the execution data
     */
    default void afterExecution(DelegateExecution execution, Output executionData) {

    }

}
