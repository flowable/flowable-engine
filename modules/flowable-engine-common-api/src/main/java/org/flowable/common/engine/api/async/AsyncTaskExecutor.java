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
package org.flowable.common.engine.api.async;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Flowable task executor interface that abstracts the execution of a {@link Runnable} or {@link Callable}
 * asynchronously in a different thread.
 *
 * @author Filip Hrisafov
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ExecutorService
 */
public interface AsyncTaskExecutor {

    /**
     * Execute the given {@code task}
     *
     * @param task the {@link Runnable} task to execute
     * @throws java.util.concurrent.RejectedExecutionException if the given task was not accepted
     */
    void execute(Runnable task);

    /**
     * Submit a {@link Runnable} task for execution, receiving a Future representing the execution of the task.
     *
     * @param task the {@link Runnable} to execute
     * @return a {@link CompletableFuture} representing pending completion of the task
     * @throws java.util.concurrent.RejectedExecutionException if the given task was not submitted
     */
    CompletableFuture<?> submit(Runnable task);

    /**
     * Submit a {@link Callable} task for execution, receiving a Future representing the execution of the task.
     *
     * @param task the {@link Runnable} to execute
     * @return a {@link CompletableFuture} representing pending completion of the task
     * @throws java.util.concurrent.RejectedExecutionException if the given task was not submitted
     */
    <T> CompletableFuture<T> submit(Callable<T> task);

    /**
     * Potentially shutdown the resources used by the async task executor.
     */
    void shutdown();

    int getRemainingCapacity();
}
