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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.lock.LockManager;
import org.flowable.common.engine.impl.lock.LockManagerImpl;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsCmd;
import org.flowable.job.service.impl.cmd.MoveTimerJobsToExecutableJobsCmd;
import org.flowable.job.service.impl.cmd.UnlockTimerJobsCmd;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class AcquireTimerJobsRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquireTimerJobsRunnable.class);

    private static final String ACQUIRE_TIMER_JOBS_GLOBAL_LOCK = "acquireTimerJobsLock";

    private static final AcquireTimerLifecycleListener NOOP_LIFECYCLE_LISTENER = new AcquireTimerLifecycleListener() {

        @Override
        public void startAcquiring(String engineName) {

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

    protected boolean globalAcquireLockEnabled;
    protected Duration lockWaitTime = Duration.ofMinutes(1);
    protected Duration lockPollRate = Duration.ofMillis(500);
    protected LockManager lockManager;

    protected volatile boolean isInterrupted;
    protected final Object MONITOR = new Object();
    protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

    protected ExecutorService moveJobsExecutorService = Executors.newFixedThreadPool(8);

    public AcquireTimerJobsRunnable(AsyncExecutor asyncExecutor, JobManager jobManager) {
        this(asyncExecutor, jobManager, null, false);
    }

    public AcquireTimerJobsRunnable(AsyncExecutor asyncExecutor, JobManager jobManager,
            AcquireTimerLifecycleListener lifecycleListener,  boolean globalAcquireLockEnabled) {
        this.asyncExecutor = asyncExecutor;
        this.jobManager = jobManager;
        this.lifecycleListener = lifecycleListener != null ? lifecycleListener : NOOP_LIFECYCLE_LISTENER;
        this.globalAcquireLockEnabled = globalAcquireLockEnabled;
    }

    @Override
    public synchronized void run() {

        if (lockManager == null) {
            this.lockManager = createLockManager(asyncExecutor.getJobServiceConfiguration().getCommandExecutor());
        }

        LOGGER.info("starting to acquire async jobs due");
        Thread.currentThread().setName("flowable-" + getEngineName() + "-acquire-timer-jobs");

        final CommandExecutor commandExecutor = asyncExecutor.getJobServiceConfiguration().getCommandExecutor();

        long millisToWait = 0L;
        while (!isInterrupted) {

            if (globalAcquireLockEnabled) {

                AcquiredTimerJobEntities acquiredTimerJobEntities = null;
                try {

                    acquiredTimerJobEntities = lockManager.waitForLockRunAndRelease(lockWaitTime, () -> {
                        return commandExecutor.execute(new AcquireTimerJobsCmd(asyncExecutor, globalAcquireLockEnabled));
                    });

                } catch (Exception e) {
                    // Don't do anything, lock will be tried again next time
                    millisToWait = asyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();

                    if (!(e instanceof FlowableException)) { // FlowableException doesn't need to be logged, could be regular lock logic
                        LOGGER.warn("Error while waiting for global acquire lock", e);
                    }
                }

                if (acquiredTimerJobEntities != null) {
                    millisToWait = executeAcquireCycle(commandExecutor, acquiredTimerJobEntities);
                }

                if (millisToWait == 0) {
                    // Always wait when running with global acquire lock, to let other nodes have the ability to fill the queue
                    // If 0 was returned, it means there is still work to do, but we want to give other nodes a chance.
                    millisToWait = lockPollRate.toMillis();
                }

            } else {
                millisToWait = executeAcquireCycle(commandExecutor, null);

            }

            if (millisToWait > 0) {
                sleep(millisToWait);
            }

        }

        LOGGER.info("stopped async job due acquisition");
    }

    protected LockManager createLockManager(CommandExecutor commandExecutor) {
        return new LockManagerImpl(commandExecutor, ACQUIRE_TIMER_JOBS_GLOBAL_LOCK, lockPollRate, getEngineName());
    }

    protected long executeAcquireCycle(CommandExecutor commandExecutor, AcquiredTimerJobEntities acquiredTimerJobEntities) {
        lifecycleListener.startAcquiring(getEngineName());

        Collection<TimerJobEntity> timerJobs = Collections.emptyList();
        long millisToWait = 0L;
        try {

            if (acquiredTimerJobEntities == null) {
                AcquiredTimerJobEntities acquiredJobs = commandExecutor.execute(new AcquireTimerJobsCmd(asyncExecutor, globalAcquireLockEnabled));
                timerJobs = acquiredJobs.getJobs();
            } else {
                timerJobs = acquiredTimerJobEntities.getJobs();
            }

            if (!timerJobs.isEmpty()) {

                final List<TimerJobEntity> finalListCopy = new ArrayList<>(timerJobs);
                moveJobsExecutorService.execute(() -> {
                    commandExecutor.execute(new MoveTimerJobsToExecutableJobsCmd(jobManager, finalListCopy));
                });

            }

            // if all jobs were executed
            millisToWait = asyncExecutor.getDefaultTimerJobAcquireWaitTimeInMillis();
            int jobsAcquired = timerJobs.size();
            lifecycleListener.acquiredJobs(getEngineName(), jobsAcquired, asyncExecutor.getMaxTimerJobsPerAcquisition());
            if (jobsAcquired >= asyncExecutor.getMaxTimerJobsPerAcquisition()) {
                millisToWait = 0;
            }

        } catch (FlowableOptimisticLockingException optimisticLockingException) {

            if (globalAcquireLockEnabled) {
                LOGGER.debug("Optimistic locking exception (using global acquire lock)", optimisticLockingException);

            } else {
                LOGGER.debug(
                    "Optimistic locking exception during async job acquisition. If you have multiple async executors running against the same database, " +
                        "this exception means that this thread tried to acquire a due async job, which already was acquired by another " +
                        "async executor acquisition thread.This is expected behavior in a clustered environment. " +
                        "You can ignore this message if you indeed have multiple async executor acquisition threads running against the same database. " +
                        "For engine {}. Exception message: {}",
                    getEngineName(), optimisticLockingException.getMessage());

            }

            unlockTimerJobs(commandExecutor, timerJobs);
        } catch (Throwable e) {
            LOGGER.error("exception during timer job acquisition: {}", e.getMessage(), e);
            millisToWait = asyncExecutor.getDefaultTimerJobAcquireWaitTimeInMillis();

            unlockTimerJobs(commandExecutor, timerJobs);
        }

        lifecycleListener.stopAcquiring(getEngineName());

        return millisToWait;
    }

    protected void sleep(long millisToWait) {
        if (millisToWait > 0) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("timer job acquisition thread sleeping for {} millis", millisToWait);
                }
                synchronized (MONITOR) {
                    if (!isInterrupted) {
                        isWaiting.set(true);
                        lifecycleListener.startWaiting(getEngineName(), millisToWait);
                        MONITOR.wait(millisToWait);
                    }
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("timer job acquisition thread woke up");
                }
            } catch (InterruptedException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("timer job acquisition wait interrupted");
                }
            } finally {
                isWaiting.set(false);
            }
        }
    }

    protected String getEngineName() {
        return asyncExecutor.getJobServiceConfiguration().getEngineName();
    }

    protected void unlockTimerJobs(CommandExecutor commandExecutor, Collection<TimerJobEntity> timerJobs) {
        try {
            if (!timerJobs.isEmpty()) {
                commandExecutor.execute(new UnlockTimerJobsCmd(timerJobs, asyncExecutor.getJobServiceConfiguration()));
            }
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to unlock timer jobs during acquiring. This is OK since they will be unlocked when the reset expired jobs thread runs", e);
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

    public boolean isGlobalAcquireLockEnabled() {
        return globalAcquireLockEnabled;
    }

    public void setGlobalAcquireLockEnabled(boolean globalAcquireLockEnabled) {
        this.globalAcquireLockEnabled = globalAcquireLockEnabled;
    }

    public Duration getLockWaitTime() {
        return lockWaitTime;
    }

    public void setLockWaitTime(Duration lockWaitTime) {
        this.lockWaitTime = lockWaitTime;
    }

    public Duration getLockPollRate() {
        return lockPollRate;
    }

    public void setLockPollRate(Duration lockPollRate) {
        this.lockPollRate = lockPollRate;
    }

}
