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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.lock.LockManager;
import org.flowable.common.engine.impl.lock.LockManagerImpl;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsCmd;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsWithGlobalAcquireLockCmd;
import org.flowable.job.service.impl.cmd.BulkMoveTimerJobsToExecutableJobsCmd;
import org.flowable.job.service.impl.cmd.MoveTimerJobsToExecutableJobsCmd;
import org.flowable.job.service.impl.cmd.UnlockTimerJobsCmd;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class AcquireTimerJobsRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquireTimerJobsRunnable.class);

    private static final String ACQUIRE_TIMER_JOBS_GLOBAL_LOCK = "acquireTimerJobsLock";

    private static final AcquireTimerLifecycleListener NOOP_LIFECYCLE_LISTENER = new AcquireTimerLifecycleListener() {

        @Override
        public void startAcquiring(String engineName, int maxTimerJobsPerAcquisition) {

        }

        @Override
        public void stopAcquiring(String engineName) {

        }

        @Override
        public void acquiredJobs(String engineName, int jobsAcquired, int maxTimerJobsPerAcquisition) {

        }

        @Override
        public void startWaiting(String engineName, long millisToWait) {

        }
    };

    protected final AsyncExecutor asyncExecutor;
    protected final JobManager jobManager;
    protected final AcquireTimerLifecycleListener lifecycleListener;

    protected AcquireJobsRunnableConfiguration configuration;
    protected LockManager lockManager;

    protected volatile boolean isInterrupted;
    protected final Object MONITOR = new Object();
    protected final AtomicBoolean isWaiting = new AtomicBoolean(false);
    protected final int moveExecutorPoolSize;

    protected ExecutorService moveTimerJobsExecutorService;

    protected CommandExecutor commandExecutor;

    public AcquireTimerJobsRunnable(AsyncExecutor asyncExecutor, JobManager jobManager, int moveExecutorPoolSize) {
        this(asyncExecutor, jobManager, null, AcquireJobsRunnableConfiguration.DEFAULT, moveExecutorPoolSize);
    }

    public AcquireTimerJobsRunnable(AsyncExecutor asyncExecutor, JobManager jobManager,
            AcquireTimerLifecycleListener lifecycleListener, AcquireJobsRunnableConfiguration configuration, int moveExecutorPoolSize) {
        this.asyncExecutor = asyncExecutor;
        this.jobManager = jobManager;
        this.lifecycleListener = lifecycleListener != null ? lifecycleListener : NOOP_LIFECYCLE_LISTENER;
        this.configuration = configuration;
        this.moveExecutorPoolSize = moveExecutorPoolSize;
    }

    @Override
    public synchronized void run() {

        // Always initialize the lock manager, allowing to switch execution modes if needed
        this.lockManager = createLockManager(asyncExecutor.getJobServiceConfiguration().getCommandExecutor());

        LOGGER.info("starting to acquire async jobs due for engine {}", getEngineName());
        String threadName = "flowable-" + getEngineName() + "-acquire-timer-jobs";
        Thread.currentThread().setName(threadName);

        createTimerMoveExecutorService(threadName);

        this.commandExecutor = asyncExecutor.getJobServiceConfiguration().getCommandExecutor();

        long millisToWait = 0L;
        while (!isInterrupted) {
            millisToWait = executeAcquireAndMoveCycle();

            if (millisToWait > 0) {
                sleep(millisToWait);
            }

        }

        if (moveTimerJobsExecutorService != null) {
            moveTimerJobsExecutorService.shutdown();
        }

        LOGGER.info("stopped async job due acquisition for engine {}", getEngineName());
    }

    protected LockManager createLockManager(CommandExecutor commandExecutor) {
        return new LockManagerImpl(commandExecutor, configuration.getGlobalAcquireLockPrefix() + ACQUIRE_TIMER_JOBS_GLOBAL_LOCK, configuration.getLockPollRate(), configuration.getLockForceAcquireAfter(), getEngineName());
    }

    protected void createTimerMoveExecutorService(String threadName) {
        BasicThreadFactory threadFactory = new BasicThreadFactory.Builder()
            .namingPattern(threadName + "-move")
            .build();
        // We are using really low queue size since if we have a lot of move operations
        // we need to complete some of them before acquiring again.
        // This should leave some time to other nodes to pick up and lock the timer jobs
        ThreadPoolExecutor executor = new ThreadPoolExecutor(moveExecutorPoolSize, moveExecutorPoolSize, 5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);

        this.moveTimerJobsExecutorService = executor;
    }

    protected long executeAcquireAndMoveCycle() {
        lifecycleListener.startAcquiring(getEngineName(), asyncExecutor.getMaxTimerJobsPerAcquisition());

        List<TimerJobEntity> timerJobs = Collections.emptyList();
        long millisToWait = 0L;

        try {

            boolean globalAcquireLockEnabled = configuration.isGlobalAcquireLockEnabled();
            if (globalAcquireLockEnabled) {

                // When running with global acquire lock, we only need to have the lock during the acquire.
                // In the move phase, other nodes can already acquire timer jobs themselves (as the lock is free).
                try {
                    timerJobs = lockManager.waitForLockRunAndRelease(configuration.getLockWaitTime(), () -> {
                        return commandExecutor.execute(new AcquireTimerJobsWithGlobalAcquireLockCmd(asyncExecutor));
                    });

                } catch (Exception e) {
                    // Don't do anything, lock will be tried again next time

                    if (!(e instanceof FlowableException)) { // FlowableException doesn't need to be logged, could be regular lock logic
                        LOGGER.warn("Error while waiting for global acquire lock for engine {}", getEngineName(), e);
                    }
                }

            } else {
                timerJobs = commandExecutor.execute(new AcquireTimerJobsCmd(asyncExecutor));

            }

            if (!timerJobs.isEmpty()) {
                List<TimerJobEntity> finalTimerJobs = timerJobs;
                moveTimerJobsExecutorService.execute(() -> {
                    executeMoveTimerJobsToExecutableJobs(finalTimerJobs);
                });
            }

            // if all jobs were executed
            millisToWait = asyncExecutor.getDefaultTimerJobAcquireWaitTimeInMillis();
            int nrOfJobsAcquired = timerJobs.size();
            lifecycleListener.acquiredJobs(getEngineName(), nrOfJobsAcquired, asyncExecutor.getMaxTimerJobsPerAcquisition());

            if (nrOfJobsAcquired >= asyncExecutor.getMaxTimerJobsPerAcquisition()) {

                if (globalAcquireLockEnabled) {
                    // Always wait when running with global acquire lock, to let other nodes have the ability to fill the queue
                    // If 0 was returned, it means there is still work to do, but we want to give other nodes a chance.
                    millisToWait = configuration.getLockPollRate().toMillis();

                } else {
                    // Otherwise (no global acquire lock),the node can retry immediately
                    millisToWait = 0;

                }

            }

        } catch (FlowableOptimisticLockingException optimisticLockingException) {
            logOptimisticLockingException(optimisticLockingException);

        } catch (Throwable e) {
            LOGGER.warn("exception during timer job acquisition for engine {}. Exception message: {}", getEngineName(), e.getMessage(), e);
            millisToWait = asyncExecutor.getDefaultTimerJobAcquireWaitTimeInMillis();

        }

        lifecycleListener.stopAcquiring(getEngineName());

        return millisToWait;
    }

    protected void executeMoveTimerJobsToExecutableJobs(List<TimerJobEntity> timerJobs) {
        try {
            if (configuration.isGlobalAcquireLockEnabled()) {
                commandExecutor.execute(new BulkMoveTimerJobsToExecutableJobsCmd(jobManager, timerJobs));
            } else {
                commandExecutor.execute(new MoveTimerJobsToExecutableJobsCmd(jobManager, timerJobs));
            }

        } catch (FlowableOptimisticLockingException optimisticLockingException) {
            logOptimisticLockingException(optimisticLockingException);
            unlockTimerJobs(timerJobs); // jobs have been acquired before, so need to unlock when exception happens here

        } catch (Throwable t) {
            LOGGER.warn("exception during timer job move for engine {}. Exception message: {}", getEngineName(), t.getMessage(), t);
            unlockTimerJobs(timerJobs); // jobs have been acquired before, so need to unlock when exception happens here

        }
    }

    protected void logOptimisticLockingException(FlowableOptimisticLockingException optimisticLockingException) {
        if (configuration.isGlobalAcquireLockEnabled()) {
            LOGGER.warn("Optimistic locking exception (using global acquire lock) for engine {}", getEngineName(), optimisticLockingException);

        } else {
            LOGGER.debug(
                "Optimistic locking exception during async job acquisition. If you have multiple async executors running against the same database, " +
                    "this exception means that this thread tried to acquire a due async job, which already was acquired by another " +
                    "async executor acquisition thread.This is expected behavior in a clustered environment. " +
                    "You can ignore this message if you indeed have multiple async executor acquisition threads running against the same database. " +
                    "For engine {}. Exception message: {}",
                getEngineName(), optimisticLockingException.getMessage());

        }
    }

    protected void sleep(long millisToWait) {
        if (millisToWait > 0) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("timer job acquisition thread for engine {} sleeping for {} millis", getEngineName(), millisToWait);
                }
                synchronized (MONITOR) {
                    if (!isInterrupted) {
                        isWaiting.set(true);
                        lifecycleListener.startWaiting(getEngineName(), millisToWait);
                        MONITOR.wait(millisToWait);
                    }
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("timer job acquisition thread for engine {} woke up", getEngineName());
                }
            } catch (InterruptedException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("timer job acquisition wait for engine {} interrupted", getEngineName());
                }
            } finally {
                isWaiting.set(false);
            }
        }
    }

    protected String getEngineName() {
        return asyncExecutor.getJobServiceConfiguration().getEngineName();
    }

    protected void unlockTimerJobs(Collection<TimerJobEntity> timerJobs) {
        try {
            if (!timerJobs.isEmpty()) {
                commandExecutor.execute(new UnlockTimerJobsCmd(timerJobs, asyncExecutor.getJobServiceConfiguration()));
            }
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to unlock timer jobs during acquiring for engine {}. This is OK since they will be unlocked when the reset expired jobs thread runs", getEngineName(), e);
            }
        }
    }

    public void stop() {
        synchronized (MONITOR) {
            isInterrupted = true;
            if (isWaiting.compareAndSet(true, false)) {
                MONITOR.notifyAll();
            }
        }
    }

    public void setConfiguration(AcquireJobsRunnableConfiguration configuration) {
        this.configuration = configuration;
    }

}
