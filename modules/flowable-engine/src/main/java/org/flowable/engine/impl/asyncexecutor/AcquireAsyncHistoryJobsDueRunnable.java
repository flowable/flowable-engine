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
package org.flowable.engine.impl.asyncexecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.flowable.engine.common.api.FlowableOptimisticLockingException;
import org.flowable.engine.impl.cmd.AcquireHistoryJobsCmd;
import org.flowable.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Tijs Rademakers
 */
public class AcquireAsyncHistoryJobsDueRunnable implements Runnable {

    private static Logger log = LoggerFactory.getLogger(AcquireAsyncHistoryJobsDueRunnable.class);

    protected final AsyncHistoryExecutor asyncExecutor;

    protected volatile boolean isInterrupted;
    protected final Object MONITOR = new Object();
    protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

    public AcquireAsyncHistoryJobsDueRunnable(AsyncHistoryExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    public synchronized void run() {
        log.info("starting to acquire async history jobs due");
        Thread.currentThread().setName("flowable-acquire-async-history-jobs");

        CommandExecutor commandExecutor = asyncExecutor.getProcessEngineConfiguration().getCommandExecutor();

        while (!isInterrupted) {
            final long millisToWait;

            int remainingCapacity = asyncExecutor.getRemainingCapacity();
            if (remainingCapacity > 0) {
                millisToWait = acquireAndExecuteJobs(commandExecutor, remainingCapacity);

                if (log.isDebugEnabled()) {
                    log.debug("acquired and queued new history jobs; sleeping for {} ms", millisToWait);
                }
            } else {
                millisToWait = asyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();

                if (log.isDebugEnabled()) {
                    log.debug("queue is full; sleeping for {} ms", millisToWait);
                }
            }

            if (millisToWait > 0) {
                sleep(millisToWait);
            }
        }
        log.info("stopped async history job due acquisition");
    }

    protected long acquireAndExecuteJobs(CommandExecutor commandExecutor, int remainingCapacity) {
        try {
            AcquiredHistoryJobEntities acquiredJobs = commandExecutor.execute(new AcquireHistoryJobsCmd(asyncExecutor, remainingCapacity));

            List<HistoryJobEntity> rejectedJobs = offerJobs(acquiredJobs);

            log.debug("History jobs acquired: {}, rejected: {}", acquiredJobs.size(), rejectedJobs.size());
            if (rejectedJobs.size() > 0) {
                // some jobs were rejected, so the queue was full; wait until attempting to acquire more.
                return asyncExecutor.getDefaultQueueSizeFullWaitTimeInMillis();
            }
            if (acquiredJobs.size() >= asyncExecutor.getMaxAsyncJobsDuePerAcquisition()) {
                // the maximum amount of jobs were acquired, so we can expect more.
                return 0L;
            }

        } catch (FlowableOptimisticLockingException optimisticLockingException) {
            if (log.isDebugEnabled()) {
                log.debug("Optimistic locking exception during async job acquisition. If you have multiple async executors running against the same database, "
                        + "this exception means that this thread tried to acquire a due async job, which already was acquired by another async executor acquisition thread."
                        + "This is expected behavior in a clustered environment. "
                        + "You can ignore this message if you indeed have multiple async executor acquisition threads running against the same database. " + "Exception message: {}",
                        optimisticLockingException.getMessage());
            }
        } catch (Throwable e) {
            log.error("exception during async job acquisition: {}", e.getMessage(), e);
        }

        return asyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();
    }

    protected List<HistoryJobEntity> offerJobs(AcquiredHistoryJobEntities acquiredJobs) {
        List<HistoryJobEntity> rejected = new ArrayList<HistoryJobEntity>();
        for (HistoryJobEntity job : acquiredJobs.getJobs()) {
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
                if (log.isDebugEnabled()) {
                    log.debug("async history job acquisition thread sleeping for {} millis", millisToWait);
                }
                synchronized (MONITOR) {
                    if (!isInterrupted) {
                        isWaiting.set(true);
                        MONITOR.wait(millisToWait);
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("async history job acquisition thread woke up");
                }
            } catch (InterruptedException e) {
                if (log.isDebugEnabled()) {
                    log.debug("async history job acquisition wait interrupted");
                }
            } finally {
                isWaiting.set(false);
            }
        }
    }

}
