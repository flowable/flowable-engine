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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Filip Hrisafov
 * @author Joram Barrez
 */
public class SpringAsyncTaskExecutor implements AsyncTaskExecutor {

    protected final AsyncListenableTaskExecutor asyncTaskExecutor;

    protected final boolean isAsyncTaskExecutorAopProxied;

    public SpringAsyncTaskExecutor(AsyncListenableTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.isAsyncTaskExecutorAopProxied = AopUtils.isAopProxy(asyncTaskExecutor); // no need to repeat this every time, done once in constructor
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

    @Override
    public int getRemainingCapacity() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = getThreadPoolTaskExecutor();
        if (threadPoolTaskExecutor != null) {
            return threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    protected ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
        Object executor = asyncTaskExecutor;
        if (isAsyncTaskExecutorAopProxied) {
            executor = AopProxyUtils.getSingletonTarget(asyncTaskExecutor);
        }
        if (executor instanceof ThreadPoolTaskExecutor) {
            return (ThreadPoolTaskExecutor) executor;
        }
    }

    @Override
    public double getPressure() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = getThreadPoolTaskExecutor();
        if (threadPoolTaskExecutor != null) {
            BlockingQueue<Runnable> queue = threadPoolTaskExecutor.getThreadPoolExecutor().getQueue();

            int waiting = queue.size();
            if (waiting == 0) {
                return 0;
            }

            int remainingCapacity = queue.remainingCapacity();
            int totalQueueSize = remainingCapacity + waiting;
            return BigDecimal.valueOf(remainingCapacity).divide(BigDecimal.valueOf(totalQueueSize), RoundingMode.HALF_UP).doubleValue();
        }
        return 0;
    }
}
