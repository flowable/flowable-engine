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

import static org.flowable.common.engine.impl.util.ExceptionUtil.sneakyThrow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class DefaultAsyncTaskExecutor implements AsyncTaskExecutor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final AsyncTaskExecutorConfiguration configuration;

    /**
     * The executor service used for task execution.
     */
    protected ExecutorService executorService;

    /**
     * Whether the executor needs a shutdown.
     * This is true if the executor service has not been set from the outside.
     */
    protected boolean executorNeedsShutdown;

    /**
     * The queue used for job execution work
     */
    protected BlockingQueue<Runnable> threadPoolQueue;

    protected ThreadFactory threadFactory;

    protected RejectedExecutionHandler rejectedExecutionHandler;

    public DefaultAsyncTaskExecutor() {
        this(new AsyncTaskExecutorConfiguration());
        this.configuration.setThreadPoolNamingPattern("flowable-async-job-executor-thread-%d");
    }

    public DefaultAsyncTaskExecutor(AsyncTaskExecutorConfiguration configuration) {
        this.configuration = configuration;
        if (StringUtils.isEmpty(this.configuration.getThreadPoolNamingPattern())) {
            this.configuration.setThreadPoolNamingPattern("flowable-async-job-executor-thread-%d");
        }
    }

    @Override
    public void execute(Runnable task) {
        executorService.execute(task);
    }

    @Override
    public CompletableFuture<?> submit(Runnable task) {
        return CompletableFuture.runAsync(task, executorService);
    }

    @Override
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception exception) {
                sneakyThrow(exception);
                return null;
            }
        }, executorService);
    }

    public void start() {
        if (executorService == null) {
            this.executorService = initializeExecutor();
            this.executorNeedsShutdown = true;
        }
    }

    @Override
    public void shutdown() {
        if (executorService != null && executorNeedsShutdown) {
            // Ask the thread pool to finish and exit
            executorService.shutdown();

            // Waits for the configured time to finish all currently executing jobs
            try {
                long secondsToWaitOnShutdown = configuration.getAwaitTerminationPeriod().getSeconds();
                if (!executorService.awaitTermination(secondsToWaitOnShutdown, TimeUnit.SECONDS)) {
                    logger.warn(
                            "Timeout during shutdown of async job executor. The current running jobs could not end within {} seconds after shutdown operation.",
                            secondsToWaitOnShutdown);
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while shutting down the async job executor. ", e);
                Thread.currentThread().interrupt();
            }

            executorService = null;
        }
    }

    protected ExecutorService initializeExecutor() {
        if (threadPoolQueue == null) {
            int queueSize = getQueueSize();
            logger.info("Creating thread pool queue of size {}", queueSize);
            threadPoolQueue = new ArrayBlockingQueue<>(queueSize);
        }

        if (threadFactory == null) {
            String threadPoolNamingPattern = getThreadPoolNamingPattern();
            logger.info("Creating thread factory with naming pattern {}", threadPoolNamingPattern);
            threadFactory = new BasicThreadFactory.Builder().namingPattern(threadPoolNamingPattern).build();

        }

        int corePoolSize = getCorePoolSize();
        int maxPoolSize = getMaxPoolSize();
        long keepAliveTime = getKeepAliveTime();
        logger.info("Creating executor service with corePoolSize {}, maxPoolSize {} and keepAliveTime {}", corePoolSize, maxPoolSize, keepAliveTime);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime,
                TimeUnit.MILLISECONDS, threadPoolQueue, threadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(isAllowCoreThreadTimeout());

        if (rejectedExecutionHandler != null) {
            logger.info("Using rejectedExecutionHandler {}", rejectedExecutionHandler);
            threadPoolExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);

        }

        return threadPoolExecutor;

    }

    public AsyncTaskExecutorConfiguration getConfiguration() {
        return configuration;
    }

    public int getCorePoolSize() {
        return getConfiguration().getCorePoolSize();
    }

    public void setCorePoolSize(int corePoolSize) {
        getConfiguration().setCorePoolSize(corePoolSize);
    }

    public int getMaxPoolSize() {
        return getConfiguration().getMaxPoolSize();
    }

    public void setMaxPoolSize(int maxPoolSize) {
        getConfiguration().setMaxPoolSize(maxPoolSize);
    }

    public long getKeepAliveTime() {
        return getConfiguration().getKeepAlive().toMillis();
    }

    public void setKeepAliveTime(long keepAliveTime) {
        getConfiguration().setKeepAlive(Duration.ofMillis(keepAliveTime));
    }

    public int getQueueSize() {
        return getConfiguration().getQueueSize();
    }

    public void setQueueSize(int queueSize) {
        getConfiguration().setQueueSize(queueSize);
    }

    public boolean isAllowCoreThreadTimeout() {
        return getConfiguration().isAllowCoreThreadTimeout();
    }

    public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
        getConfiguration().setAllowCoreThreadTimeout(allowCoreThreadTimeout);
    }

    public long getSecondsToWaitOnShutdown() {
        return getConfiguration().getAwaitTerminationPeriod().getSeconds();
    }

    public void setSecondsToWaitOnShutdown(long secondsToWaitOnShutdown) {
        getConfiguration().setAwaitTerminationPeriod(Duration.ofSeconds(secondsToWaitOnShutdown));
    }

    public BlockingQueue<Runnable> getThreadPoolQueue() {
        return threadPoolQueue;
    }

    public void setThreadPoolQueue(BlockingQueue<Runnable> threadPoolQueue) {
        this.threadPoolQueue = threadPoolQueue;
    }

    public String getThreadPoolNamingPattern() {
        return getConfiguration().getThreadPoolNamingPattern();
    }

    public void setThreadPoolNamingPattern(String threadPoolNamingPattern) {
        getConfiguration().setThreadPoolNamingPattern(threadPoolNamingPattern);
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    @Override
    public int getRemainingCapacity() {
        return threadPoolQueue.remainingCapacity();
    }

    @Override
    public double getPressure() {
        int waiting = threadPoolQueue.size();
        if (waiting == 0) {
            return 0;
        }

        int remainingCapacity = threadPoolQueue.remainingCapacity();
        int totalQueueSize = remainingCapacity + waiting;
        return BigDecimal.valueOf(remainingCapacity).divide(BigDecimal.valueOf(totalQueueSize), RoundingMode.HALF_UP).doubleValue();
    }
}
