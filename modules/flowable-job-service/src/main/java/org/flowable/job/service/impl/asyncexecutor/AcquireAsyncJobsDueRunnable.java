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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.lock.LockManager;
import org.flowable.common.engine.impl.lock.LockManagerImpl;
import org.flowable.job.service.impl.cmd.AcquireJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class AcquireAsyncJobsDueRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquireAsyncJobsDueRunnable.class);

    private static final String ACQUIRE_ASYNC_JOBS_GLOBAL_LOCK = "acquireAsyncJobsLock";

    private static final AcquireAsyncJobsDueLifecycleListener NOOP_LIFECYCLE_LISTENER = new AcquireAsyncJobsDueLifecycleListener() {

        @Override
        public void startAcquiring(String engineName) {

        }

        @Override
        public void stopAcquiring(String engineName) {

        }

        @Override
        public void acquiredJobs(String engineName, int jobsAcquired, int maxAsyncJobsDuePerAcquisition, int remainingQueueCapacity) {

        }

        @Override
        public void rejectedJobs(String engineName, int jobsRejected, int jobsAcquired, int maxAsyncJobsDuePerAcquisition, int remainingQueueCapacity) {

        }

        @Override
        public void optimistLockingException(String engineName, int maxAsyncJobsDuePerAcquisition, int remainingCapacity) {

        }

        @Override
        public void startWaiting(String engineName, long millisToWait) {

        }

    };

    protected String name;

    protected final AsyncExecutor asyncExecutor;
    protected final JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;

    protected AcquireAsyncJobsDueLifecycleListener lifecycleListener;

    protected boolean globalAcquireLockEnabled;
    protected Duration lockWaitTime = Duration.ofMinutes(1);
    protected Duration lockPollRate = Duration.ofMillis(500);
    protected LockManager lockManager;

    protected volatile boolean isInterrupted;
    protected final Object MONITOR = new Object();
    protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

    public AcquireAsyncJobsDueRunnable(String name, AsyncExecutor asyncExecutor, JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager,
            AcquireAsyncJobsDueLifecycleListener lifecycleListener, boolean globalAcquireLockEnabled) {
        this.name = name;
        this.asyncExecutor = asyncExecutor;
        this.jobEntityManager = jobEntityManager;
        this.lifecycleListener = lifecycleListener != null ? lifecycleListener : NOOP_LIFECYCLE_LISTENER;
        this.globalAcquireLockEnabled = globalAcquireLockEnabled;
    }

    @Override
    public synchronized void run() {

        if (lockManager == null) {
            this.lockManager = createLockManager(asyncExecutor.getJobServiceConfiguration().getCommandExecutor());
        }

        LOGGER.info("starting to acquire async jobs due");
        Thread.currentThread().setName(name);

        final CommandExecutor commandExecutor = asyncExecutor.getJobServiceConfiguration().getCommandExecutor();

        long millisToWait = 0L;
        while (!isInterrupted) {

            if (globalAcquireLockEnabled) {

                try {
                    millisToWait = lockManager.waitForLockRunAndRelease(lockWaitTime, () -> executeAcquireCycle(commandExecutor));
                } catch (Exception e) {
                    // Don't do anything, lock will be tried again next time
                    millisToWait = asyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();

                    if (!(e instanceof FlowableException)) { // FlowableExeption doesn't need to be logged, could be regular lock logic
                        LOGGER.warn("Error while waiting for global acquire lock", e);
                    }
                }

                if (millisToWait == 0) {
                    // Always wait when running with global acquire lock, to let other nodes have the ability to fill the queue
                    // If 0 was returned, it means there is still work to do, but we want to give other nodes a chance.
                    millisToWait = lockPollRate.toMillis();
                }

            } else {
                millisToWait = executeAcquireCycle(commandExecutor);

            }

            if (millisToWait > 0) {
                sleep(millisToWait);
            }

        }
        LOGGER.info("stopped async job due acquisition for engine {}", getEngineName());
    }

    protected LockManager createLockManager(CommandExecutor commandExecutor) {
        return new LockManagerImpl(commandExecutor, ACQUIRE_ASYNC_JOBS_GLOBAL_LOCK, lockPollRate, getEngineName());
    }

    protected long executeAcquireCycle(CommandExecutor commandExecutor) {
        lifecycleListener.startAcquiring(getEngineName());

        final long millisToWait;
        int remainingCapacity = asyncExecutor.getTaskExecutor().getRemainingCapacity();
        if (remainingCapacity > 0) {
            millisToWait = acquireAndExecuteJobs(commandExecutor, remainingCapacity);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("acquired and queued new jobs for engine {}; sleeping for {} ms", getEngineName(), millisToWait);
            }
        } else {
            millisToWait = asyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("queue is full for engine {}; sleeping for {} ms", getEngineName(), millisToWait);
            }
        }

        lifecycleListener.stopAcquiring(getEngineName());

        return millisToWait;
    }

    protected long acquireAndExecuteJobs(CommandExecutor commandExecutor, int remainingCapacity) {
        try {
            AcquiredJobEntities acquiredJobs = commandExecutor.execute(new AcquireJobsCmd(asyncExecutor, remainingCapacity, jobEntityManager));
            lifecycleListener.acquiredJobs(getEngineName(), acquiredJobs.size(), asyncExecutor.getMaxAsyncJobsDuePerAcquisition(), remainingCapacity);

            List<JobInfoEntity> rejectedJobs = offerJobs(acquiredJobs);

            LOGGER.debug("Jobs acquired: {}, rejected: {}, for engine {}", acquiredJobs.size(), rejectedJobs.size(), getEngineName());
            if (rejectedJobs.size() > 0) {

                lifecycleListener.rejectedJobs(getEngineName(), rejectedJobs.size(), acquiredJobs.size(),
                    asyncExecutor.getMaxAsyncJobsDuePerAcquisition(), remainingCapacity);

                // some jobs were rejected, so the queue was full; wait until attempting to acquire more.
                return asyncExecutor.getDefaultQueueSizeFullWaitTimeInMillis();
            }
            if (acquiredJobs.size() >= asyncExecutor.getMaxAsyncJobsDuePerAcquisition()) {
                return 0L; // the maximum amount of jobs were acquired, so we can expect more.
            }

        } catch (FlowableOptimisticLockingException optimisticLockingException) {

            lifecycleListener.optimistLockingException(getEngineName(), asyncExecutor.getMaxAsyncJobsDuePerAcquisition(), remainingCapacity);

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
        } catch (Throwable e) {
            LOGGER.error("exception for engine {} during async job acquisition: {}", getEngineName(), e.getMessage(), e);
        }

        return asyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();
    }

    protected List<JobInfoEntity> offerJobs(AcquiredJobEntities acquiredJobs) {
        List<JobInfoEntity> rejected = new ArrayList<>();
        for (JobInfoEntity job : acquiredJobs.getJobs()) {
            boolean jobSuccessFullyOffered = asyncExecutor.executeAsyncJob(job);
            if (!jobSuccessFullyOffered) {
                rejected.add(job);
            }
        }
        return rejected;
    }

    public void stop() {
        synchronized (MONITOR) {
            isInterrupted = true;
            if (isWaiting.compareAndSet(true, false)) {
                MONITOR.notifyAll();
            }
        }
    }

    protected void sleep(long millisToWait) {
        if (millisToWait > 0) {
            try {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("async job acquisition for engine {}, thread sleeping for {} millis", getEngineName(), millisToWait);
                }
                synchronized (MONITOR) {
                    if (!isInterrupted) {
                        isWaiting.set(true);
                        lifecycleListener.startWaiting(getEngineName(), millisToWait);
                        MONITOR.wait(millisToWait);
                    }
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("async job acquisition for engine {}, thread woke up", getEngineName());
                }
            } catch (InterruptedException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("async job acquisition for engine {}, wait interrupted", getEngineName());
                }
            } finally {
                isWaiting.set(false);
            }
        }
    }

    protected String getEngineName() {
        return asyncExecutor.getJobServiceConfiguration().getEngineName();
    }

    public AcquireAsyncJobsDueLifecycleListener getLifecycleListener() {
        return lifecycleListener;
    }

    public void setLifecycleListener(AcquireAsyncJobsDueLifecycleListener lifecycleListener) {
        this.lifecycleListener = lifecycleListener;
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
