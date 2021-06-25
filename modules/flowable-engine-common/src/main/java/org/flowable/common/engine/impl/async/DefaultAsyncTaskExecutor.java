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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    /**
     * The executor service used for task execution.
     */
    protected ExecutorService executorService;

    /**
     * Whether the executor needs a shutdown.
     * This is true if the executor service has not been set from the outside.
     */
    protected boolean executorNeedsShutdown;

    // Configuration properties

    /**
     * The minimal number of threads that are kept alive in the threadpool for
     * job execution
     */
    protected int corePoolSize = 8;

    /**
     * The maximum number of threads that are kept alive in the threadpool for
     * job execution
     */
    protected int maxPoolSize = 8;

    /**
     * The time (in milliseconds) a thread used for job execution must be kept
     * alive before it is destroyed. Default setting is 0. Having a non-default
     * setting of 0 takes resources, but in the case of many job executions it
     * avoids creating new threads all the time.
     */
    protected long keepAliveTime = 5000L;

    /**
     * The size of the queue on which jobs to be executed are placed
     */
    protected int queueSize = 100;

    /**
     * Whether or not core threads can time out (which is needed to scale down the threads)
     */
    protected boolean allowCoreThreadTimeout = true;

    /**
     * The time (in seconds) that is waited to gracefully shut down the
     * threadpool used for job execution
     */
    protected long secondsToWaitOnShutdown = 60L;

    /**
     * The queue used for job execution work
     */
    protected BlockingQueue<Runnable> threadPoolQueue;

    protected String threadPoolNamingPattern = "flowable-async-job-executor-thread-%d";

    protected ThreadFactory threadFactory;

    protected RejectedExecutionHandler rejectedExecutionHandler;

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
            logger.info("Creating thread pool queue of size {}", queueSize);
            threadPoolQueue = new ArrayBlockingQueue<>(queueSize);
        }

        if (threadFactory == null) {
            logger.info("Creating thread factory with naming pattern {}", threadPoolNamingPattern);
            threadFactory = new BasicThreadFactory.Builder().namingPattern(threadPoolNamingPattern).build();

        }

        logger.info("Creating executor service with corePoolSize {}, maxPoolSize {} and keepAliveTime {}", corePoolSize, maxPoolSize, keepAliveTime);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime,
                TimeUnit.MILLISECONDS, threadPoolQueue, threadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeout);

        if (rejectedExecutionHandler != null) {
            logger.info("Using rejectedExecutionHandler {}", rejectedExecutionHandler);
            threadPoolExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);

        }

        return threadPoolExecutor;

    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public boolean isAllowCoreThreadTimeout() {
        return allowCoreThreadTimeout;
    }

    public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
        this.allowCoreThreadTimeout = allowCoreThreadTimeout;
    }

    public long getSecondsToWaitOnShutdown() {
        return secondsToWaitOnShutdown;
    }

    public void setSecondsToWaitOnShutdown(long secondsToWaitOnShutdown) {
        this.secondsToWaitOnShutdown = secondsToWaitOnShutdown;
    }

    public BlockingQueue<Runnable> getThreadPoolQueue() {
        return threadPoolQueue;
    }

    public void setThreadPoolQueue(BlockingQueue<Runnable> threadPoolQueue) {
        this.threadPoolQueue = threadPoolQueue;
    }

    public String getThreadPoolNamingPattern() {
        return threadPoolNamingPattern;
    }

    public void setThreadPoolNamingPattern(String threadPoolNamingPattern) {
        this.threadPoolNamingPattern = threadPoolNamingPattern;
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
}
