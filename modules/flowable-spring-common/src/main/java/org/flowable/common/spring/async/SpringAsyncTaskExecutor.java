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
package org.flowable.common.spring.async;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.springframework.core.task.AsyncListenableTaskExecutor;

/**
 * @author Filip Hrisafov
 */
public class SpringAsyncTaskExecutor implements AsyncTaskExecutor {

    protected final AsyncListenableTaskExecutor asyncTaskExecutor;

    public SpringAsyncTaskExecutor(AsyncListenableTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    @Override
    public void execute(Runnable task) {
        asyncTaskExecutor.execute(task);
    }

    @Override
    public CompletableFuture<?> submit(Runnable task) {
        return asyncTaskExecutor.submitListenable(task).completable();
    }

    @Override
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return asyncTaskExecutor.submitListenable(task).completable();
    }

    @Override
    public void shutdown() {
        // This uses spring resources passed in the constructor, therefore there is nothing to shutdown here
    }

    public AsyncListenableTaskExecutor getAsyncTaskExecutor() {
        return asyncTaskExecutor;
    }
}
