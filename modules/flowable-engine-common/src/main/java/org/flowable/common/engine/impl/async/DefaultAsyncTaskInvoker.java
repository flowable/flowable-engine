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
package org.flowable.common.engine.impl.async;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.api.async.AsyncTaskInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class DefaultAsyncTaskInvoker implements AsyncTaskInvoker {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final AsyncTaskExecutor taskExecutor;

    public DefaultAsyncTaskInvoker(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        try {
            return taskExecutor.submit(task);
        } catch (RejectedExecutionException rejected) {
            logger.debug("Task {} was rejected. It will be executed on the current thread.", task, rejected);
            CompletableFuture<T> future = new CompletableFuture<>();
            try {
                future.complete(task.call());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
            return future;
        }
    }
}
