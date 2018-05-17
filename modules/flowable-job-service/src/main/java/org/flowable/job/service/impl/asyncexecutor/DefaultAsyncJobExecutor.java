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
package org.flowable.job.service.impl.asyncexecutor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class DefaultAsyncJobExecutor extends AbstractAsyncExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAsyncJobExecutor.class);
    
    /**
     * If true (default), the thread for acquiring async jobs will be started.
     */
    protected boolean isAsyncJobAcquisitionEnabled = true;
    
    /**
     * If true (default), the thread for acquiring timer jobs will be started.
     */
    protected boolean isTimerJobAcquisitionEnabled = true;
    
    /**
     * If true (default), the thread for acquiring expired jobs will be started.
     */
    protected boolean isResetExpiredJobEnabled = true;
    
    /**
     * Thread responsible for async job acquisition.
     */
    protected Thread asyncJobAcquisitionThread;

    /**
     * Thread responsible for timer job acquisition.
     */
    protected Thread timerJobAcquisitionThread;
    
    /**
     * Thread responsible for resetting the expired jobs.
     */
    protected Thread resetExpiredJobThread;

    /**
     * The minimal number of threads that are kept alive in the threadpool for job execution
     */
    protected int corePoolSize = 2;

    /**
     * The maximum number of threads that are kept alive in the threadpool for job execution
     */
    protected int maxPoolSize = 10;

    /**
     * The time (in milliseconds) a thread used for job execution must be kept alive before it is destroyed. Default setting is 0. Having a non-default setting of 0 takes resources, but in the case of
     * many job executions it avoids creating new threads all the time.
     */
    protected long keepAliveTime = 5000L;

    /** The size of the queue on which jobs to be executed are placed */
    protected int queueSize = 100;

    /** Whether to unlock jobs that are owned by this executor (have the same lockOwner) at startup */
    protected boolean unlockOwnedJobs;

    /** The queue used for job execution work */
    protected BlockingQueue<Runnable> threadPoolQueue;

    /** The executor service used for job execution */
    protected ExecutorService executorService;

    /**
     * The time (in seconds) that is waited to gracefully shut down the threadpool used for job execution
     */
    protected long secondsToWaitOnShutdown = 60L;
    
    protected String threadPoolNamingPattern = "flowable-async-job-executor-thread-%d";

    @Override
    protected boolean executeAsyncJob(final JobInfo job, Runnable runnable) {
        try {
            executorService.execute(runnable);
            return true;
        } catch (RejectedExecutionException e) {
            unacquireJobAfterRejection(job);

            // Job queue full, returning false so (if wanted) the acquiring can be throttled
            return false;
        }
    }

    protected void unacquireJobAfterRejection(final JobInfo job) {
        // When a RejectedExecutionException is caught, this means that the queue for holding the jobs
        // that are to be executed is full and can't store more.
        // The job is now 'unlocked', meaning that the lock owner/time is set to null,
        // so other executors can pick the job up (or this async executor, the next time the
        // acquire query is executed.

        // This can happen while already in a command context (for example in a transaction listener
        // after the async executor has been hinted that a new async job is created)
        // or not (when executed in the acquire thread runnable)

        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            CommandContextUtil.getJobManager(commandContext).unacquire(job);

        } else {
            jobServiceConfiguration.getCommandExecutor().execute(new Command<Void>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    CommandContextUtil.getJobManager(commandContext).unacquire(job);
                    return null;
                }
            });
        }
    }

    @Override
    protected void startAdditionalComponents() {
        if (!isMessageQueueMode) {
            initAsyncJobExecutionThreadPool();
            startJobAcquisitionThread();
        }
        
        if (unlockOwnedJobs) {
            unlockOwnedJobs();
        }

        if (timerRunnableNeeded) {
            startTimerAcquisitionThread();
        }
        startResetExpiredJobsThread();
    }

    @Override
    protected void shutdownAdditionalComponents() {
        stopResetExpiredJobsThread();
        stopTimerAcquisitionThread();
        stopJobAcquisitionThread();
        stopExecutingAsyncJobs();
    }

    protected void initAsyncJobExecutionThreadPool() {
        if (threadPoolQueue == null) {
            LOGGER.info("Creating thread pool queue of size {}", queueSize);
            threadPoolQueue = new ArrayBlockingQueue<>(queueSize);
        }

        if (executorService == null) {
            LOGGER.info("Creating executor service with corePoolSize {}, maxPoolSize {} and keepAliveTime {}", corePoolSize, maxPoolSize, keepAliveTime);

            BasicThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern(threadPoolNamingPattern).build();
            executorService = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, threadPoolQueue, threadFactory);
        }
    }

    protected void stopExecutingAsyncJobs() {
        if (executorService != null) {

            // Ask the thread pool to finish and exit
            executorService.shutdown();

            // Waits for 1 minute to finish all currently executing jobs
            try {
                if (!executorService.awaitTermination(secondsToWaitOnShutdown, TimeUnit.SECONDS)) {
                    LOGGER.warn("Timeout during shutdown of async job executor. The current running jobs could not end within {} seconds after shutdown operation.", secondsToWaitOnShutdown);
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while shutting down the async job executor. ", e);
            }

            executorService = null;
        }
    }

    /** Starts the acquisition thread */
    protected void startJobAcquisitionThread() {
        if (isAsyncJobAcquisitionEnabled) {
            if (asyncJobAcquisitionThread == null) {
                asyncJobAcquisitionThread = new Thread(asyncJobsDueRunnable);
            }
            asyncJobAcquisitionThread.start();
        }
    }

    protected void startTimerAcquisitionThread() {
        if (isTimerJobAcquisitionEnabled) {
            if (timerJobAcquisitionThread == null) {
                timerJobAcquisitionThread = new Thread(timerJobRunnable);
            }
            timerJobAcquisitionThread.start();
        }
    }

    /** Stops the acquisition thread */
    protected void stopJobAcquisitionThread() {
        if (asyncJobAcquisitionThread != null) {
            try {
                asyncJobAcquisitionThread.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for the async job acquisition thread to terminate", e);
            }
            asyncJobAcquisitionThread = null;
        }
    }

    protected void stopTimerAcquisitionThread() {
        if (timerJobAcquisitionThread != null) {
            try {
                timerJobAcquisitionThread.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for the timer job acquisition thread to terminate", e);
            }
            timerJobAcquisitionThread = null;
        }
    }

    /** Starts the reset expired jobs thread */
    protected void startResetExpiredJobsThread() {
        if (isResetExpiredJobEnabled) {
            if (resetExpiredJobThread == null) {
                resetExpiredJobThread = new Thread(resetExpiredJobsRunnable);
            }
            resetExpiredJobThread.start();
        }
    }

    /** Stops the reset expired jobs thread */
    protected void stopResetExpiredJobsThread() {
        if (resetExpiredJobThread != null) {
            try {
                resetExpiredJobThread.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for the reset expired jobs thread to terminate", e);
            }

            resetExpiredJobThread = null;
        }
    }
    
    public boolean isAsyncJobAcquisitionEnabled() {
        return isAsyncJobAcquisitionEnabled;
    }

    public void setAsyncJobAcquisitionEnabled(boolean isAsyncJobAcquisitionEnabled) {
        this.isAsyncJobAcquisitionEnabled = isAsyncJobAcquisitionEnabled;
    }

    public boolean isTimerJobAcquisitionEnabled() {
        return isTimerJobAcquisitionEnabled;
    }

    public void setTimerJobAcquisitionEnabled(boolean isTimerJobAcquisitionEnabled) {
        this.isTimerJobAcquisitionEnabled = isTimerJobAcquisitionEnabled;
    }

    public boolean isResetExpiredJobEnabled() {
        return isResetExpiredJobEnabled;
    }

    public void setResetExpiredJobEnabled(boolean isResetExpiredJobEnabled) {
        this.isResetExpiredJobEnabled = isResetExpiredJobEnabled;
    }

    public Thread getTimerJobAcquisitionThread() {
        return timerJobAcquisitionThread;
    }

    public void setTimerJobAcquisitionThread(Thread timerJobAcquisitionThread) {
        this.timerJobAcquisitionThread = timerJobAcquisitionThread;
    }

    public Thread getAsyncJobAcquisitionThread() {
        return asyncJobAcquisitionThread;
    }

    public void setAsyncJobAcquisitionThread(Thread asyncJobAcquisitionThread) {
        this.asyncJobAcquisitionThread = asyncJobAcquisitionThread;
    }

    public Thread getResetExpiredJobThread() {
        return resetExpiredJobThread;
    }

    public void setResetExpiredJobThread(Thread resetExpiredJobThread) {
        this.resetExpiredJobThread = resetExpiredJobThread;
    }

    public int getQueueSize() {
        return queueSize;
    }

    @Override
    public int getRemainingCapacity() {
        if (threadPoolQueue != null) {
            return threadPoolQueue.remainingCapacity();
        } else {
            // return plenty of remaining capacity if there's no thread pool queue
            return 99;
        }
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
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

    public long getSecondsToWaitOnShutdown() {
        return secondsToWaitOnShutdown;
    }

    public void setSecondsToWaitOnShutdown(long secondsToWaitOnShutdown) {
        this.secondsToWaitOnShutdown = secondsToWaitOnShutdown;
    }

    public boolean isUnlockOwnedJobs() {
        return unlockOwnedJobs;
    }

    public void setUnlockOwnedJobs(boolean unlockOwnedJobs) {
        this.unlockOwnedJobs = unlockOwnedJobs;
    }

    public BlockingQueue<Runnable> getThreadPoolQueue() {
        return threadPoolQueue;
    }

    public void setThreadPoolQueue(BlockingQueue<Runnable> threadPoolQueue) {
        this.threadPoolQueue = threadPoolQueue;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public String getThreadPoolNamingPattern() {
        return threadPoolNamingPattern;
    }

    public void setThreadPoolNamingPattern(String threadPoolNamingPattern) {
        this.threadPoolNamingPattern = threadPoolNamingPattern;
    }
    
}
